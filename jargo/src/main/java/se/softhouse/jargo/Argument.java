/*
 * Copyright 2013 Jonatan Jönsson
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package se.softhouse.jargo;

import static java.util.Arrays.asList;
import static se.softhouse.common.guavaextensions.Predicates2.alwaysTrue;
import static se.softhouse.common.strings.Describables.format;
import static se.softhouse.jargo.ArgumentExceptions.withMessage;

import java.text.CollationKey;
import java.text.Collator;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;

import se.softhouse.common.guavaextensions.Suppliers2;
import se.softhouse.common.strings.Describable;
import se.softhouse.common.strings.Describer;
import se.softhouse.jargo.StringParsers.HelpParser;
import se.softhouse.jargo.StringParsers.InternalStringParser;
import se.softhouse.jargo.internal.Texts.ProgrammaticErrors;
import se.softhouse.jargo.internal.Texts.UserErrors;

/**
 * <pre>
 * Represents a supported {@link Argument} for a command line invocation.
 * 
 * {@link Argument}s are created with the static methods in {@link Arguments} or with a custom {@link ArgumentBuilder} and then
 * used by the {@link CommandLineParser} to parse strings (typically from the command line).
 * 
 * @param <T> the type of values this {@link Argument} is configured to parse
 * </pre>
 */
@Immutable
public final class Argument<T>
{
	@Nonnull private final List<String> names;
	@Nonnull private final Describable description;
	@Nonnull private final Optional<String> metaDescription;
	private final boolean hideFromUsage;

	@Nonnull private final String separator;
	private final boolean required;
	private final boolean ignoreCase;
	private final boolean isAllowedToRepeat;

	@Nonnull private final InternalStringParser<T> parser;
	@Nonnull private final Supplier<? extends T> defaultValue;
	@Nullable private final Describer<? super T> defaultValueDescriber;
	@Nonnull private final Predicate<? super T> limiter;

	// Internal bookkeeping
	@Nonnull private final Function<T, T> finalizer;
	private final boolean isPropertyMap;

	private final CollationKey sortingKey;
	private static final Collator LINGUISTIC_ORDER = Collator.getInstance(Locale.ROOT);

	/**
	 * <pre>
	 * Creates a basic object for handling {@link Argument}s taken from a command line invocation.
	 * For practical uses of this constructor see {@link Arguments#optionArgument(String, String...)} (and friends)
	 * and the {@link ArgumentBuilder}.
	 * </pre>
	 */
	Argument(final ArgumentBuilder<?, T> builder)
	{
		this.parser = builder.internalParser();
		this.defaultValueDescriber = builder.defaultValueDescriber();
		this.description = builder.description();
		this.required = builder.isRequired();
		this.separator = builder.separator();
		this.ignoreCase = builder.isIgnoringCase();
		this.names = builder.names();
		this.isPropertyMap = builder.isPropertyMap();
		this.isAllowedToRepeat = builder.isAllowedToRepeat();
		this.hideFromUsage = builder.isHiddenFromUsage();
		this.metaDescription = builder.metaDescription();

		this.finalizer = builder.finalizer();
		this.limiter = builder.limiter();
		this.defaultValue = builder.defaultValueSupplierOrFromParser();

		// Fail-fast for invalid default values that are created already
		if(Suppliers2.isSuppliedAlready(defaultValue))
		{
			// Calling this makes sure that the default value is within the limits of any limiter
			defaultValue();
		}

		// Better to take the hit up front than to lazy load and experience latency when usage is
		// requested for the first time
		this.sortingKey = LINGUISTIC_ORDER.getCollationKey(toString());
	}

	/**
	 * Parses command line arguments and returns the value of this argument.<br>
	 * This is a shorthand method that should be used if only one {@link Argument} is expected.
	 * If several arguments are expected use {@link CommandLineParser#withArguments(Argument...)}
	 * instead. <br>
	 * <b>Locale:</b> {@link Locale#US} is used to parse strings / print usage. To use another
	 * {@link Locale} you'll need to use {@link CommandLineParser#withArguments(Argument...)}
	 * instead.
	 * 
	 * @param actualArguments the arguments from the command line
	 * @return the parsed value from the {@code actualArguments}
	 * @throws ArgumentException if actualArguments isn't what this
	 *             argument expects
	 */
	@Nullable
	public T parse(String ... actualArguments) throws ArgumentException
	{
		return commandLineParser().parse(asList(actualArguments)).get(this);
	}

	/**
	 * Returns the {@link Usage} for this argument. Should only be used if one argument is
	 * supported, otherwise the {@link CommandLineParser#usage()} method should be used instead.
	 */
	@Nonnull
	@CheckReturnValue
	public Usage usage()
	{
		return new Usage(commandLineParser());
	}

	/**
	 * Describes {@link Argument}s by their first name. If they are indexed and have no name the
	 * {@link StringParser#metaDescription() meta description} is used instead.
	 */
	@Override
	public String toString()
	{
		if(isIndexed())
			return metaDescriptionInRightColumn();
		return names().get(0);
	}

	/**
	 * @return the default value for this argument, defaults to {@link StringParser#defaultValue()}.
	 *         Could also be set by {@link ArgumentBuilder#defaultValue(Object)} or
	 *         {@link ArgumentBuilder#defaultValueSupplier(Supplier)}
	 */
	@Nullable
	T defaultValue()
	{
		T value = finalizer.apply(defaultValue.get());

		// If this throws it indicates that a bad combination of Provider/Limiter
		// is used or simply put that it's an invalid default value
		checkLimitForDefaultValue(value);

		return value;
	}

	String descriptionOfValidValues(Locale inLocale)
	{
		if(limiter != alwaysTrue())
			return limiter.toString();

		return parser().descriptionOfValidValues(this, inLocale);
	}

	@Nullable
	String defaultValueDescription(Locale inLocale)
	{
		T value = defaultValue();
		if(defaultValueDescriber != null)
			return defaultValueDescriber.describe(value, inLocale);
		return parser().describeValue(value);
	}

	@Nonnull
	String metaDescriptionInLeftColumn()
	{
		String meta = metaDescription.orElseGet(() -> parser.metaDescriptionInLeftColumn(this));
		if(!isPropertyMap() && !isIndexed())
		{
			meta = separator + meta;
		}
		return meta;
	}

	@Nonnull
	String metaDescriptionInRightColumn()
	{
		return metaDescription.orElseGet(() -> parser.metaDescriptionInRightColumn(this));
	}

	void checkLimit(@Nullable final T value) throws ArgumentException
	{
		if(!limiter.test(value))
			throw withMessage(format(UserErrors.DISALLOWED_VALUE, value, limiter));
	}

	private void checkLimitForDefaultValue(@Nullable final T value)
	{
		boolean error = false;
		try
		{
			if(!limiter.test(value))
			{
				error = true;
			}
		}
		catch(IllegalArgumentException invalidDefaultValue)
		{
			throw new IllegalArgumentException(String.format(ProgrammaticErrors.INVALID_DEFAULT_VALUE, invalidDefaultValue.getMessage()),
					invalidDefaultValue);
		}
		if(error)
		{
			String disallowedValue = String.format(UserErrors.DISALLOWED_VALUE, value, limiter);
			throw new IllegalArgumentException(String.format(ProgrammaticErrors.INVALID_DEFAULT_VALUE, disallowedValue));
		}
	}

	@Nonnull
	InternalStringParser<T> parser()
	{
		return parser;
	}

	boolean isRequired()
	{
		return required;
	}

	@Nullable
	String separator()
	{
		return separator;
	}

	@Nonnull
	String description()
	{
		return description.description();
	}

	@Nonnull
	List<String> names()
	{
		return names;
	}

	boolean isPropertyMap()
	{
		return isPropertyMap;
	}

	boolean isAllowedToRepeat()
	{
		return isAllowedToRepeat;
	}

	boolean isIgnoringCase()
	{
		return ignoreCase;
	}

	Function<T, T> finalizer()
	{
		return finalizer;
	}

	boolean isIndexed()
	{
		return names().isEmpty();
	}

	boolean isHelpArgument()
	{
		return parser().equals(HelpParser.INSTANCE);
	}

	private CommandLineParserInstance commandLineParser()
	{
		// Not cached to save memory, users should use CommandLineParser.withArguments if they are
		// concerned about reuse
		return new CommandLineParserInstance(Arrays.<Argument<?>>asList(Argument.this));
	}

	/**
	 * Classes of how many arguments that can be handled by an {@link Argument}.
	 */
	enum ParameterArity
	{
		/**
		 * Indicates {@link Arguments#optionArgument(String, String...)}
		 */
		NO_ARGUMENTS,
		/**
		 * Indicates {@link ArgumentBuilder#variableArity()}
		 */
		VARIABLE_AMOUNT,
		/**
		 * {@link ArgumentBuilder#arity(int)} or any other {@link Argument}
		 */
		AT_LEAST_ONE_ARGUMENT
	}

	static final Predicate<Argument<?>> IS_INDEXED = Argument::isIndexed;

	static final Predicate<Argument<?>> IS_REQUIRED = Argument::isRequired;

	static final Predicate<Argument<?>> IS_VISIBLE = input -> !input.hideFromUsage;

	static final Predicate<Argument<?>> IS_REPEATED = input -> input.isAllowedToRepeat;

	static final Predicate<Argument<?>> IS_OF_VARIABLE_ARITY = input -> input.parser().parameterArity() == ParameterArity.VARIABLE_AMOUNT;

	// TODO(jontejj): replace this with a comparator that uses the Usage.locale instead of
	// Locale.ROOT?
	static final Comparator<Argument<?>> NAME_COMPARATOR = (lhs, rhs) -> lhs.sortingKey.compareTo(rhs.sortingKey);
}
