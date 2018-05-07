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

import static java.util.Collections.emptyList;
import static java.util.Objects.requireNonNull;
import static se.softhouse.common.guavaextensions.Preconditions2.check;
import static se.softhouse.common.strings.Describables.format;
import static se.softhouse.common.strings.StringsUtil.repeat;
import static se.softhouse.jargo.ArgumentExceptions.forMissingNthParameter;
import static se.softhouse.jargo.ArgumentExceptions.forMissingParameter;
import static se.softhouse.jargo.ArgumentExceptions.withMessage;
import static se.softhouse.jargo.ArgumentExceptions.wrapException;

import java.io.File;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.StringJoiner;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.regex.Pattern;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;

import se.softhouse.common.guavaextensions.Predicates2;
import se.softhouse.common.numbers.NumberType;
import se.softhouse.common.strings.StringBuilders;
import se.softhouse.jargo.Argument.ParameterArity;
import se.softhouse.jargo.ArgumentExceptions.MissingParameterException;
import se.softhouse.jargo.internal.Texts.UserErrors;

/**
 * <pre>
 * Gives you static access to implementations of the {@link StringParser} interface.
 * All methods return {@link Immutable} parsers.
 * Most methods here even return the same instance for every call.
 * If you want to customize one of these parsers you can use a {@link ForwardingStringParser}.
 *
 * By {@link StringParser#defaultValue() default} most parsers return <code>zero</code> or otherwise
 * sane defaults. The only exception being {@link #enumParser(Class)} which returns null instead
 * of the first enum constant available.
 * </pre>
 */
@Immutable
public final class StringParsers
{
	private StringParsers()
	{
	}

	/**
	 * The simplest possible parser, it simply returns the strings it's given to parse.
	 */
	@CheckReturnValue
	@Nonnull
	public static StringParser<String> stringParser()
	{
		return StringStringParser.STRING;
	}

	enum StringStringParser implements StringParser<String>
	{
		/**
		 * Simply returns the strings it's given to parse
		 */
		STRING
		{

	@Override
	public String parse(String value, Locale locale) throws ArgumentException
	{
		return value;
	}

	};

	// Put other StringParser<String> parsers here

	@Override
	public String descriptionOfValidValues(Locale locale)
	{
		return "any string";
	}

	@Override
	public String defaultValue()
	{
		return "";
	}

	@Override
	public String metaDescription()
	{
		return "<string>";
	}

	}

	/**
	 * <pre>
	 * A parser that parse strings with {@link Boolean#valueOf(String)}
	 * The {@link StringParser#defaultValue() default value} is <code>false</code>
	 * </pre>
	 */
	@CheckReturnValue
	@Nonnull
	public static StringParser<Boolean> booleanParser()
	{
		return BooleanParser.INSTANCE;
	}

	private static final class BooleanParser implements StringParser<Boolean>
	{
		private static final BooleanParser INSTANCE = new BooleanParser();

		@Override
		public Boolean parse(final String value, Locale locale)
		{
			return Boolean.valueOf(value);
		}

		@Override
		public String descriptionOfValidValues(Locale locale)
		{
			return "true or false";
		}

		@Override
		public Boolean defaultValue()
		{
			return false;
		}

		@Override
		public String metaDescription()
		{
			return "<boolean>";
		}
	}

	/**
	 * A parser that creates {@link File}s using {@link File#File(String)} for input strings.<br>
	 * The {@link StringParser#defaultValue() default value} is a {@link File} representing the
	 * current working directory (cwd).
	 */
	@CheckReturnValue
	@Nonnull
	public static StringParser<File> fileParser()
	{
		return FileParser.INSTANCE;
	}

	private static final class FileParser implements StringParser<File>
	{
		private static final FileParser INSTANCE = new FileParser();

		@Override
		public File parse(final String value, Locale locale)
		{
			return new File(value);
		}

		@Override
		public String descriptionOfValidValues(Locale locale)
		{
			return "a file path";
		}

		@Override
		public File defaultValue()
		{
			return new File(".");
		}

		@Override
		public String metaDescription()
		{
			return "<path>";
		}
	}

	/**
	 * <pre>
	 * A parser that returns the first character in a {@link String} as a {@link Character} and that
	 * throws {@link ArgumentException} for any given {@link String} with more than
	 * one {@link Character}.
	 * The {@link StringParser#defaultValue()} is the <a href=
	"https://en.wikipedia.org/wiki/Null_character">NUL</a> character.
	 * </pre>
	 */
	@CheckReturnValue
	@Nonnull
	public static StringParser<Character> charParser()
	{
		return CharParser.INSTANCE;
	}

	private static final class CharParser implements StringParser<Character>
	{
		private static final CharParser INSTANCE = new CharParser();

		@Override
		public Character parse(final String value, Locale locale) throws ArgumentException
		{
			if(value.length() != 1)
				throw withMessage(format(UserErrors.INVALID_CHAR, value));
			return value.charAt(0);
		}

		@Override
		public String descriptionOfValidValues(Locale locale)
		{
			return "any unicode character";
		}

		@Override
		public Character defaultValue()
		{
			return 0;
		}

		@Override
		public String metaDescription()
		{
			return "<character>";
		}
	}

	/**
	 * <pre>
	 * A parser that uses {@link Enum#valueOf(Class, String)} to {@link StringParser#parse(String, Locale) parse} input strings.
	 *
	 * <b>Case sensitivity note:</b> First a direct match with the input value in upper case is
	 * made, if that fails a direct match without converting the case is made,
	 * if that also fails an {@link ArgumentException} is thrown. This order of execution is
	 * based on the fact that users typically don't upper case their input while
	 * <a href=
	"http://www.oracle.com/technetwork/java/javase/documentation/codeconventions-135099.html#367">java naming conventions</a> recommends upper case for enum constants.
	 * </pre>
	 *
	 * <b>Default value:</b> <code>null</code> is used as {@link StringParser#defaultValue()} and
	 * not any of the enum values in {@code enumToHandle}
	 *
	 * @param enumToHandle the {@link Class} literal for the {@link Enum} to parse strings into
	 * @return a {@link StringParser} that parses strings into enum values of the type {@code T}
	 */
	@CheckReturnValue
	@Nonnull
	public static <T extends Enum<T>> StringParser<T> enumParser(final Class<T> enumToHandle)
	{
		return new EnumParser<T>(enumToHandle);
	}

	private static final class EnumParser<E extends Enum<E>> implements StringParser<E>
	{
		private final Class<E> enumType;

		private EnumParser(final Class<E> enumToHandle)
		{
			enumType = requireNonNull(enumToHandle);
		}

		@Override
		public E parse(final String value, final Locale locale) throws ArgumentException
		{
			try
			{
				return Enum.valueOf(enumType, value.toUpperCase(Locale.US));
			}
			catch(IllegalArgumentException noEnumFoundWithUpperCase)
			{
				try
				{
					// Try an exact match just in case an enum value doesn't follow java naming
					// guidelines
					return Enum.valueOf(enumType, value);
				}
				catch(IllegalArgumentException noEnumFoundWithExactMatch)
				{
					throw withMessage(format(UserErrors.INVALID_ENUM_VALUE, value, new Object(){
						@Override
						public String toString()
						{
							// Lazily call this as it's going over all enum values and converting
							// them to strings
							return descriptionOfValidValues(locale);
						}
					}), noEnumFoundWithExactMatch);
				}
			}
		}

		@Override
		public String descriptionOfValidValues(Locale locale)
		{
			E[] enumValues = enumType.getEnumConstants();
			StringJoiner joiner = new StringJoiner(" | ", "{", "}");
			Arrays.stream(enumValues).forEach(e -> joiner.add(e.toString()));
			return joiner.toString();
		}

		@Override
		public E defaultValue()
		{
			return null;
		}

		@Override
		public String metaDescription()
		{
			return "<" + enumType.getSimpleName() + ">";
		}
	}

	/**
	 * Parses {@link String}s into {@link Byte}s using {@link NumberType#parse(String, Locale)}
	 */
	@CheckReturnValue
	@Nonnull
	public static StringParser<Byte> byteParser()
	{
		return NumberParser.BYTE;
	}

	/**
	 * Parses {@link String}s into {@link Short}s using {@link NumberType#parse(String, Locale)}
	 */
	@CheckReturnValue
	@Nonnull
	public static StringParser<Short> shortParser()
	{
		return NumberParser.SHORT;
	}

	/**
	 * Parses {@link String}s into {@link Integer}s using {@link NumberType#parse(String, Locale)}
	 */
	@CheckReturnValue
	@Nonnull
	public static StringParser<Integer> integerParser()
	{
		return NumberParser.INTEGER;
	}

	/**
	 * Parses {@link String}s into {@link Long}s using {@link NumberType#parse(String, Locale)}
	 */
	@CheckReturnValue
	@Nonnull
	public static StringParser<Long> longParser()
	{
		return NumberParser.LONG;
	}

	/**
	 * Parses {@link String}s into {@link BigInteger}s using
	 * {@link NumberType#parse(String, Locale)}
	 */
	@CheckReturnValue
	@Nonnull
	public static StringParser<BigInteger> bigIntegerParser()
	{
		return NumberParser.BIG_INTEGER;
	}

	/**
	 * Parses {@link String}s into {@link BigDecimal}s using
	 * {@link NumberType#parse(String, Locale)}
	 */
	@CheckReturnValue
	@Nonnull
	public static StringParser<BigDecimal> bigDecimalParser()
	{
		return NumberParser.BIG_DECIMAL;
	}

	private static final class NumberParser<N extends Number & Comparable<N>> implements StringParser<N>
	{
		private static final StringParser<Byte> BYTE = new NumberParser<Byte>(NumberType.BYTE);
		private static final StringParser<Short> SHORT = new NumberParser<Short>(NumberType.SHORT);
		private static final StringParser<Integer> INTEGER = new NumberParser<Integer>(NumberType.INTEGER);
		private static final StringParser<Long> LONG = new NumberParser<Long>(NumberType.LONG);
		private static final StringParser<BigInteger> BIG_INTEGER = new NumberParser<BigInteger>(NumberType.BIG_INTEGER);
		private static final StringParser<BigDecimal> BIG_DECIMAL = new NumberParser<BigDecimal>(NumberType.BIG_DECIMAL);

		private final NumberType<N> type;

		private NumberParser(NumberType<N> type)
		{
			this.type = type;
		}

		@Override
		public N parse(String argument, Locale locale) throws ArgumentException
		{
			try
			{
				return type.parse(argument, locale);
			}
			catch(IllegalArgumentException invalidNumber)
			{
				throw wrapException(invalidNumber);
			}
		}

		@Override
		public String descriptionOfValidValues(Locale locale)
		{
			return type.descriptionOfValidValues(locale);
		}

		@Override
		public N defaultValue()
		{
			return type.defaultValue();
		}

		@Override
		public String metaDescription()
		{
			return '<' + type.name() + '>';
		}

		@Override
		public String toString()
		{
			return descriptionOfValidValues(Locale.US);
		}
	}

	static final class TransformingParser<T, F> extends InternalStringParser<F>
	{
		private final Function<T, F> transformer;
		private final InternalStringParser<T> firstParser;
		private final Predicate<? super T> limiter;

		TransformingParser(InternalStringParser<T> firstParser, Function<T, F> transformer, Predicate<? super T> limiter)
		{
			this.firstParser = requireNonNull(firstParser);
			this.transformer = requireNonNull(transformer);
			this.limiter = requireNonNull(limiter);
		}

		@SuppressWarnings("unchecked")
		@Override
		F parse(ArgumentIterator arguments, F previousOccurance, Argument<?> argumentSettings, Locale locale) throws ArgumentException
		{
			T first = firstParser.parse(arguments, null, argumentSettings, locale);
			if(!limiter.test(first))
				throw withMessage(format(UserErrors.DISALLOWED_VALUE, first, argumentSettings.descriptionOfValidValues(locale)));

			F result = transformer.apply(first);
			if(previousOccurance instanceof Collection && result instanceof Collection && argumentSettings.isAllowedToRepeat())
			{
				((Collection<Object>) previousOccurance).addAll((Collection<Object>) result);
				return previousOccurance;
			}
			return result;
		}

		@Override
		String descriptionOfValidValues(Argument<?> argumentSettings, Locale locale)
		{
			if(limiter != Predicates2.alwaysTrue())
			{
				String attemptAtGoodDescription = limiter.toString();
				if(!attemptAtGoodDescription.contains("$$Lambda$"))
					return attemptAtGoodDescription;
			}
			return firstParser.descriptionOfValidValues(argumentSettings, locale);
		}

		@Override
		String metaDescriptionInLeftColumn(Argument<?> argumentSettings)
		{
			return firstParser.metaDescriptionInLeftColumn(argumentSettings);
		}

		@Override
		String metaDescriptionInRightColumn(Argument<?> argumentSettings)
		{
			return firstParser.metaDescriptionInRightColumn(argumentSettings);
		}

		@Override
		String metaDescription(Argument<?> argumentSettings)
		{
			return firstParser.metaDescription(argumentSettings);
		}

		@Override
		F defaultValue()
		{
			return transformer.apply(firstParser.defaultValue());
		}

		@Override
		ParameterArity parameterArity()
		{
			return firstParser.parameterArity();
		}
	}

	/**
	 * <pre>
	 * Makes it possible to convert several (or zero) {@link String}s into a single {@code T} value.
	 * For a simpler one use {@link StringParser}.
	 *
	 * {@link Argument} is passed to the functions that produces text for the usage,
	 * it can't be a member of this class because one parser can be referenced
	 * from multiple different {@link Argument}s so this is extrinsic state.
	 * </pre>
	 *
	 * @param <T> the type this parser parses strings into
	 */
	abstract static class InternalStringParser<T>
	{
		/**
		 * @param arguments the arguments given from the command line where
		 *            {@link ArgumentIterator#next()} points to the parameter
		 *            for a named argument, for an indexed argument it points to the single unnamed
		 *            argument
		 * @param previousOccurance the previously parsed value for this
		 *            argument if it appears several times, otherwise null
		 * @param argumentSettings argument settings for this parser,
		 *            can be used for providing good exception messages.
		 * @param locale the locale to parse strings with, may matter when parsing numbers, dates
		 *            etc
		 * @return the parsed value
		 * @throws ArgumentException if an error occurred while parsing the value
		 */
		abstract T parse(final ArgumentIterator arguments, @Nullable final T previousOccurance, final Argument<?> argumentSettings, Locale locale)
				throws ArgumentException;

		/**
		 * Describes the values this parser accepts
		 *
		 * @return a description string to show in usage texts
		 */
		@Nonnull
		abstract String descriptionOfValidValues(Argument<?> argumentSettings, Locale locale);

		/**
		 * If you can provide a suitable value do so, it will look much better
		 * in the usage texts and providing sane defaults makes your program/code easier to use,
		 * otherwise return {@code null}
		 */
		@Nullable
		abstract T defaultValue();

		/**
		 * Returns a description of {@code value}, or null if no description is
		 * needed
		 */
		@Nullable
		String describeValue(@Nullable T value)
		{
			return String.valueOf(value);
		}

		@Nonnull
		abstract String metaDescription(Argument<?> argumentSettings);

		String metaDescriptionInLeftColumn(Argument<?> argumentSettings)
		{
			return metaDescription(argumentSettings);
		}

		String metaDescriptionInRightColumn(Argument<?> argumentSettings)
		{
			return metaDescription(argumentSettings);
		}

		/**
		 * Returns the {@link ParameterArity}
		 * {@link #parse(ArgumentIterator, Object, Argument, Locale) parse} expects.
		 */
		ParameterArity parameterArity()
		{
			return ParameterArity.AT_LEAST_ONE_ARGUMENT;
		}
	}

	@CheckReturnValue
	@Nonnull
	static InternalStringParser<Boolean> optionParser(final boolean defaultValue)
	{
		if(defaultValue)
			return OptionParser.DEFAULT_TRUE;
		return OptionParser.DEFAULT_FALSE;
	}

	/**
	 * Inherits from InternalStringParser because implementing StringParser
	 * would make it require a parameter which an Option doesn't.
	 */
	static final class OptionParser extends InternalStringParser<Boolean>
	{
		private static final OptionParser DEFAULT_FALSE = new OptionParser(false);
		private static final OptionParser DEFAULT_TRUE = new OptionParser(true);

		private final Boolean defaultValue;

		private OptionParser(final boolean defaultValue)
		{
			this.defaultValue = defaultValue;
		}

		@Override
		Boolean parse(ArgumentIterator arguments, Boolean previousOccurance, Argument<?> argumentSettings, Locale locale) throws ArgumentException
		{
			return !defaultValue;
		}

		@Override
		public Boolean defaultValue()
		{
			return defaultValue;
		}

		/**
		 * Only the existence of the flag matters, no specific value.
		 * Use {@link ArgumentBuilder#description(String)} to describe this
		 * argument.
		 */
		@Override
		public String descriptionOfValidValues(Argument<?> argumentSettings, Locale locale)
		{
			return "";
		}

		@Override
		String metaDescription(Argument<?> argumentSettings)
		{
			// Options don't have parameters so there's no parameter to explain
			return "";
		}

		@Override
		ParameterArity parameterArity()
		{
			return ParameterArity.NO_ARGUMENTS;
		}
	}

	static final class HelpParser extends InternalStringParser<String>
	{
		static final HelpParser INSTANCE = new HelpParser();

		@Override
		String parse(ArgumentIterator arguments, String previousOccurance, Argument<?> argumentSettings, Locale locale) throws ArgumentException
		{
			throw arguments.currentParser().helpFor(arguments, locale);
		}

		@Override
		String descriptionOfValidValues(Argument<?> argumentSettings, Locale locale)
		{
			return "an argument to print help for";
		}

		@Override
		String defaultValue()
		{
			return "If no specific parameter is given the whole usage text is given";
		}

		@Override
		String metaDescription(Argument<?> argumentSettings)
		{
			return "<argument-to-print-help-for>";
		}
	}

	/**
	 * Runs a {@link Runnable} when {@link StringParser#parse(String, Locale) parse} is invoked.
	 */
	static final class RunnableParser extends InternalStringParser<Object>
	{
		final Runnable target;

		RunnableParser(Runnable target)
		{
			this.target = target;
		}

		@Override
		Object parse(ArgumentIterator arguments, Object previousOccurance, Argument<?> argumentSettings, Locale locale) throws ArgumentException
		{
			target.run();
			return null;
		}

		@Override
		String descriptionOfValidValues(Argument<?> argumentSettings, Locale locale)
		{
			return "";
		}

		@Override
		Object defaultValue()
		{
			return null;
		}

		@Override
		String metaDescription(Argument<?> argumentSettings)
		{
			return "";
		}
	}

	/**
	 * Base class for {@link StringParser}s that uses a sub parser to parse element values and puts
	 * them into a {@link List}
	 */
	abstract static class ListParser<T> extends InternalStringParser<List<T>>
	{
		private final InternalStringParser<T> elementParser;

		private ListParser(final InternalStringParser<T> elementParser)
		{
			this.elementParser = elementParser;
		}

		protected final InternalStringParser<T> elementParser()
		{
			return elementParser;
		}

		@Override
		public String descriptionOfValidValues(Argument<?> argumentSettings, Locale locale)
		{
			return elementParser.descriptionOfValidValues(argumentSettings, locale);
		}

		@Override
		String metaDescription(Argument<?> argumentSettings)
		{
			return elementParser.metaDescription(argumentSettings);
		}

		@Override
		public List<T> defaultValue()
		{
			return emptyList();
		}

		@Override
		String describeValue(List<T> value)
		{
			if(value == null)
				return "null";
			if(value.isEmpty())
				return "Empty list";

			Iterator<T> values = value.iterator();
			String firstValue = String.valueOf(values.next());

			StringBuilder sb = StringBuilders.withExpectedSize(value.size() * firstValue.length());

			sb.append(firstValue);
			while(values.hasNext())
			{
				sb.append(", ").append(String.valueOf(values.next()));
			}
			return sb.toString();
		}
	}

	/**
	 * Implements {@link ArgumentBuilder#arity(int)}
	 */
	static final class FixedArityParser<T> extends ListParser<T>
	{
		private final int arity;

		FixedArityParser(final InternalStringParser<T> parser, final int arity)
		{
			super(parser);
			this.arity = arity;
		}

		@Override
		List<T> parse(final ArgumentIterator arguments, final List<T> list, final Argument<?> argumentSettings, Locale locale)
				throws ArgumentException
		{
			List<T> parsedArguments = new ArrayList<>(arity);
			for(int i = 0; i < arity; i++)
			{
				try
				{
					T parsedValue = elementParser().parse(arguments, null, argumentSettings, locale);
					parsedArguments.add(parsedValue);
				}
				catch(MissingParameterException exception)
				{
					// Wrap exception to more clearly specify which parameter that is missing
					throw forMissingNthParameter(exception, i);
				}
			}
			return parsedArguments;
		}

		@Override
		public List<T> defaultValue()
		{
			T defaultValue = elementParser().defaultValue();
			List<T> listFilledWithDefaultValues = new ArrayList<T>(arity);
			for(int i = 0; i < arity; i++)
			{
				listFilledWithDefaultValues.add(defaultValue);
			}
			return listFilledWithDefaultValues;
		}

		@Override
		String metaDescriptionInLeftColumn(Argument<?> argumentSettings)
		{
			String metaDescriptionForValue = metaDescription(argumentSettings);
			return metaDescriptionForValue + repeat(" " + metaDescriptionForValue, arity - 1);
		}
	}

	/**
	 * Implements {@link ArgumentBuilder#variableArity()}
	 */
	static final class VariableArityParser<T> extends ListParser<T>
	{
		VariableArityParser(final InternalStringParser<T> parser)
		{
			super(parser);
		}

		@Override
		List<T> parse(final ArgumentIterator arguments, final List<T> list, final Argument<?> argumentSettings, Locale locale)
				throws ArgumentException
		{
			List<T> parsedArguments = new ArrayList<>(arguments.nrOfRemainingArguments());
			while(arguments.hasNext())
			{
				T parsedValue = elementParser().parse(arguments, null, argumentSettings, locale);
				parsedArguments.add(parsedValue);
			}
			return parsedArguments;
		}

		@Override
		String metaDescriptionInLeftColumn(Argument<?> argumentSettings)
		{
			String metaDescriptionForValue = metaDescription(argumentSettings);
			return metaDescriptionForValue + " ...";
		}

		@Override
		ParameterArity parameterArity()
		{
			return ParameterArity.VARIABLE_AMOUNT;
		}
	}

	/**
	 * Implements {@link ArgumentBuilder#splitWith(String)}.
	 *
	 * @param <T> the type that's separated by the {@code valueSeparator}
	 */
	static final class StringSplitterParser<T> extends ListParser<T>
	{
		@Nonnull private final String valueSeparator;
		@Nonnull private final Pattern splitter;

		StringSplitterParser(final String valueSeparator, final InternalStringParser<T> parser)
		{
			super(parser);
			check(!valueSeparator.isEmpty(), "Splitting with an empty string is not supported");
			this.valueSeparator = valueSeparator;
			this.splitter = Pattern.compile(valueSeparator);
		}

		@Override
		List<T> parse(final ArgumentIterator arguments, final List<T> oldValue, final Argument<?> argumentSettings, Locale locale)
				throws ArgumentException
		{
			if(!arguments.hasNext())
				throw forMissingParameter(argumentSettings);

			String values = arguments.next();
			List<T> result = new ArrayList<T>();

			for(String value : splitter.split(values))
			{
				value = value.trim();
				if(value.isEmpty())
				{
					continue;
				}

				ArgumentIterator argument = ArgumentIterator.forArguments(Arrays.asList(value));
				T parsedValue = elementParser().parse(argument, null, argumentSettings, locale);
				result.add(parsedValue);
			}
			return result;
		}

		@Override
		String metaDescriptionInLeftColumn(Argument<?> argumentSettings)
		{
			String metaDescriptionForValue = metaDescription(argumentSettings);
			return metaDescriptionForValue + valueSeparator + metaDescriptionForValue + valueSeparator + "...";
		}
	}

	/**
	 * Implements {@link ArgumentBuilder#repeated()}.
	 *
	 * @param <T> type of the repeated values (such as {@link Integer} for {@link #integerParser()}
	 */
	static final class RepeatedArgumentParser<T> extends ListParser<T>
	{
		RepeatedArgumentParser(final InternalStringParser<T> parser)
		{
			super(parser);
		}

		@Override
		List<T> parse(final ArgumentIterator arguments, List<T> previouslyCreatedList, final Argument<?> argumentSettings, Locale locale)
				throws ArgumentException
		{
			T parsedValue = elementParser().parse(arguments, null, argumentSettings, locale);

			List<T> listToStoreRepeatedValuesIn = previouslyCreatedList;
			if(listToStoreRepeatedValuesIn == null)
			{
				listToStoreRepeatedValuesIn = new LinkedList<>();
			}

			listToStoreRepeatedValuesIn.add(parsedValue);
			return listToStoreRepeatedValuesIn;
		}
	}

	/**
	 * Implements {@link ArgumentBuilder#asPropertyMap()} and
	 * {@link ArgumentBuilder#asKeyValuesWithKeyParser(StringParser)}.
	 *
	 * @param <K> the type of key in the resulting map
	 * @param <V> the type of values in the resulting map
	 */
	static final class KeyValueParser<K, V> extends InternalStringParser<Map<K, V>>
	{
		@Nonnull private final InternalStringParser<V> valueParser;
		@Nonnull private final StringParser<K> keyParser;
		@Nonnull private final Predicate<? super V> valueLimiter;
		@Nonnull private final Supplier<? extends Map<K, V>> defaultMap;

		KeyValueParser(StringParser<K> keyParser, InternalStringParser<V> valueParser, Predicate<? super V> valueLimiter,
				@Nullable Supplier<? extends Map<K, V>> defaultMap, @Nullable final Supplier<? extends V> defaultValue)
		{
			this.valueParser = valueParser;
			this.keyParser = keyParser;
			this.valueLimiter = valueLimiter;
			if(defaultMap == null)
			{
				this.defaultMap = new Supplier<Map<K, V>>(){
					@Override
					public Map<K, V> get()
					{
						if(defaultValue != null)
							return new LinkedHashMap<K, V>(){
								private static final long serialVersionUID = 1L;

								@Override
								public V get(Object key)
								{
									if(super.containsKey(key))
										return super.get(key);
									return defaultValue.get();
								}
							};
						return new LinkedHashMap<>();
					}
				};
			}
			else
			{
				this.defaultMap = defaultMap;
			}
		}

		@Override
		Map<K, V> parse(final ArgumentIterator arguments, Map<K, V> previousMap, final Argument<?> argumentSettings, Locale locale)
				throws ArgumentException
		{
			Map<K, V> map = previousMap;
			if(map == null)
			{
				map = defaultValue();
			}

			String keyValue = arguments.next();
			String key = getKey(keyValue, argumentSettings);
			K parsedKey = keyParser.parse(key, locale);
			V oldValue = map.get(parsedKey);

			// Hide what we just did to the parser that handles the "value"
			if(valueParser.parameterArity() != ParameterArity.NO_ARGUMENTS)
			{
				arguments.setNextArgumentTo(getValue(key, keyValue, argumentSettings));
			}
			V parsedValue = valueParser.parse(arguments, oldValue, argumentSettings, locale);

			try
			{
				if(!valueLimiter.test(parsedValue))
					throw withMessage(format(UserErrors.DISALLOWED_PROPERTY_VALUE, parsedKey, parsedValue, valueLimiter));
			}
			catch(IllegalArgumentException e)
			{
				throw wrapException(e);
			}

			map.put(parsedKey, parsedValue);
			return map;
		}

		/**
		 * Fetch "key" from "key=value"
		 */
		private String getKey(String keyValue, Argument<?> argumentSettings) throws ArgumentException
		{
			if(valueParser.parameterArity() == ParameterArity.NO_ARGUMENTS)
				return keyValue;// Consume the whole string as the key as there's no value to parse
			String separator = argumentSettings.separator();
			int keyEndIndex = keyValue.indexOf(separator);
			if(keyEndIndex == -1)
				throw withMessage(format(UserErrors.MISSING_KEY_VALUE_SEPARATOR, argumentSettings, keyValue, separator));
			return keyValue.substring(0, keyEndIndex);
		}

		/**
		 * Fetch "value" from "key=value"
		 */
		private String getValue(String key, String keyValue, Argument<?> argumentSettings)
		{
			return keyValue.substring(key.length() + argumentSettings.separator().length());
		}

		@Override
		public String descriptionOfValidValues(Argument<?> argumentSettings, Locale locale)
		{
			String keyMeta = '"' + keyParser.metaDescription() + '"';
			String valueMeta = '"' + valueParser.metaDescription(argumentSettings) + '"';
			String keyDescription = keyParser.descriptionOfValidValues(locale);
			String valueDescription;
			if(valueLimiter != Predicates2.alwaysTrue())
			{
				valueDescription = valueLimiter.toString();
			}
			else
			{
				valueDescription = valueParser.descriptionOfValidValues(argumentSettings, locale);
			}

			return "where " + keyMeta + " is " + keyDescription + " and " + valueMeta + " is " + valueDescription;
		}

		@Override
		public Map<K, V> defaultValue()
		{
			return defaultMap.get();
		}

		@Override
		String metaDescription(Argument<?> argumentSettings)
		{
			String keyMeta = keyParser.metaDescription();
			String separator = argumentSettings.separator();
			String valueMeta = valueParser.metaDescription(argumentSettings);
			return keyMeta + separator + valueMeta;
		}
	}

	static final class StringParserBridge<T> extends InternalStringParser<T> implements StringParser<T>
	{
		private final StringParser<T> stringParser;

		/**
		 * A bridge between the {@link StringParser} & {@link InternalStringParser} interfaces.
		 *
		 * @param parserToBridge the {@link StringParser} to expose as a
		 *            {@link InternalStringParser}
		 */
		StringParserBridge(StringParser<T> parserToBridge)
		{
			stringParser = parserToBridge;
		}

		@Override
		T parse(ArgumentIterator arguments, T previousOccurance, Argument<?> argumentSettings, Locale locale) throws ArgumentException
		{
			if(!arguments.hasNext())
				throw forMissingParameter(argumentSettings);
			return parse(arguments.next(), locale);
		}

		@Override
		String descriptionOfValidValues(Argument<?> argumentSettings, Locale locale)
		{
			return descriptionOfValidValues(locale);
		}

		@Override
		String metaDescription(Argument<?> argumentSettings)
		{
			return metaDescription();
		}

		@Override
		public T parse(String argument, Locale locale) throws ArgumentException
		{
			return stringParser.parse(argument, locale);
		}

		@Override
		public String descriptionOfValidValues(Locale locale)
		{
			return stringParser.descriptionOfValidValues(locale);
		}

		@Override
		public String metaDescription()
		{
			return stringParser.metaDescription();
		}

		@Override
		public T defaultValue()
		{
			return stringParser.defaultValue();
		}

		@Override
		public String toString()
		{
			return descriptionOfValidValues(Locale.US);
		}
	}
}
