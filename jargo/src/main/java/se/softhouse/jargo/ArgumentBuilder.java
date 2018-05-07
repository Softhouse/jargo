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
import static java.util.Collections.emptyList;
import static java.util.Collections.unmodifiableList;
import static java.util.Objects.requireNonNull;
import static se.softhouse.common.guavaextensions.Functions2.listTransformer;
import static se.softhouse.common.guavaextensions.Lists2.isEmpty;
import static se.softhouse.common.guavaextensions.Lists2.newArrayList;
import static se.softhouse.common.guavaextensions.Preconditions2.check;
import static se.softhouse.common.guavaextensions.Predicates2.listPredicate;
import static se.softhouse.common.strings.Describables.EMPTY_STRING;
import static se.softhouse.common.strings.Describables.withString;
import static se.softhouse.jargo.StringParsers.optionParser;
import static se.softhouse.jargo.StringParsers.stringParser;

import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.StringJoiner;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.OverridingMethodsMustInvokeSuper;
import javax.annotation.concurrent.Immutable;
import javax.annotation.concurrent.NotThreadSafe;

import se.softhouse.common.guavaextensions.Functions2;
import se.softhouse.common.guavaextensions.Predicates2;
import se.softhouse.common.guavaextensions.Suppliers2;
import se.softhouse.common.strings.Describable;
import se.softhouse.common.strings.Describer;
import se.softhouse.common.strings.Describers;
import se.softhouse.jargo.ForwardingStringParser.SimpleForwardingStringParser;
import se.softhouse.jargo.StringParsers.FixedArityParser;
import se.softhouse.jargo.StringParsers.InternalStringParser;
import se.softhouse.jargo.StringParsers.KeyValueParser;
import se.softhouse.jargo.StringParsers.RepeatedArgumentParser;
import se.softhouse.jargo.StringParsers.StringParserBridge;
import se.softhouse.jargo.StringParsers.StringSplitterParser;
import se.softhouse.jargo.StringParsers.TransformingParser;
import se.softhouse.jargo.StringParsers.VariableArityParser;
import se.softhouse.jargo.internal.Texts.ProgrammaticErrors;
import se.softhouse.jargo.internal.Texts.UserErrors;

/**
 * <pre>
 * Responsible for configuring and building {@link Argument} instances.
 * Example builders can be created with {@link Arguments}.
 *
 * <b>Note:</b>The code examples (for each method) assumes that all methods in {@link Arguments} have been statically imported.
 *
 * <b>Note:</b>Some methods needs to be called in a specific order
 * (to make the generic type system produce the correct type) and to guide the
 * caller, such invalid orders are documented with {@link Deprecated}. If those warnings
 * are ignored {@link IllegalStateException} will be thrown at the offending call.
 * </pre>
 *
 * @param <SELF> the type of the subclass extending this class.
 *            Concept borrowed from: <a href=
 *            "http://passion.forco.de/content/emulating-self-types-using-java-generics-simplify-fluent-api-implementation">Ansgar.Konermann's
 *            blog</a> The pattern also resembles the
 *            <a href="http://en.wikipedia.org/wiki/Curiously_recurring_template_pattern">Curiously
 *            recurring template pattern</a>
 * @param <T> the type of arguments the built {@link Argument} instance should handle,
 *            such as {@link Integer} in the case of {@link Arguments#integerArgument(String...)}
 */
@NotThreadSafe
public abstract class ArgumentBuilder<SELF extends ArgumentBuilder<SELF, T>, T>
{
	static final String DEFAULT_SEPARATOR = " ";
	// ArgumentSetting variables, think about #copy() when adding new ones
	@Nonnull private List<String> names = emptyList();
	@Nonnull private Describable description = EMPTY_STRING;
	private boolean required = false;
	@Nonnull private String separator = DEFAULT_SEPARATOR;
	private boolean ignoreCase = false;
	private boolean isAllowedToRepeat = false;
	@Nonnull private Optional<String> metaDescription = Optional.empty();
	private boolean hideFromUsage = false;

	private boolean isPropertyMap = false;

	// Members that uses the T type, think about
	// ListArgumentBuilder#copyAsListBuilder() when adding new ones
	@Nullable private Supplier<? extends T> defaultValueSupplier = null;
	@Nullable private Describer<? super T> defaultValueDescriber = null;
	@Nonnull private Function<T, T> finalizer = Function.identity();
	@Nonnull private Predicate<? super T> limiter = Predicates2.alwaysTrue();

	@Nullable private final InternalStringParser<T> internalStringParser;

	@Nonnull private final SELF myself;

	/**
	 * Creates a default {@link ArgumentBuilder} that produces arguments that:
	 * <ul>
	 * <li>doesn't have any {@link #names(String...)}</li>
	 * <li>has an empty {@link #description(String)}</li>
	 * <li>isn't {@link #required()}</li>
	 * <li>uses {@link StringParser#defaultValue()} on {@link #parser()} to produce default
	 * values</li>
	 * <li>uses {@link Object#toString()} to describe the default value</li>
	 * <li>uses {@link StringParser#metaDescription()} on {@link #parser()} to produce meta
	 * descriptions</li>
	 * <li>doesn't have any {@link #limitTo(Predicate) limits}</li>
	 * </ul>
	 * Typically invoked implicitly by subclasses.
	 */
	protected ArgumentBuilder()
	{
		this(null);
	}

	ArgumentBuilder(final InternalStringParser<T> stringParser)
	{
		this.internalStringParser = stringParser;
		myself = self();
	}

	@SuppressWarnings("unchecked")
	private SELF self()
	{
		// SELF is passed in by subclasses as a type-variable, so type-safety
		// is up to them
		return (SELF) this;
	}

	/**
	 * Returns an {@link Immutable} {@link Argument} which can be passed to
	 * {@link CommandLineParser#withArguments(Argument...)} <br>
	 * When the parsing is done the parsed value for this
	 * argument can be fetched with {@link ParsedArguments#get(Argument)}.
	 *
	 * @throws IllegalStateException if it's not possible to construct an {@link Argument} with the
	 *             current settings
	 */
	@CheckReturnValue
	@Nonnull
	public final Argument<T> build()
	{
		return new Argument<T>(this);
	}

	/**
	 * <pre>
	 * Parses command line arguments and returns the value of the argument built
	 * by {@link #build()}, providing a simple one liner shortcut when faced with only one argument.
	 * </pre>
	 *
	 * <pre class="prettyprint">
	 * <code class="language-java">
	 * String[] args = {"--listen-port", "8090"};
	 *
	 * int port = integerArgument("-p", "--listen-port").defaultValue(8080).description("The port clients should connect to.").parse(args);
	 * assertThat(port).isEqualTo(8090);
	 * </code>
	 * </pre>
	 *
	 * This is a shorthand method that should be used if only one {@link Argument} is expected as it
	 * will throw if unexpected arguments are encountered. If several arguments are expected use
	 * {@link CommandLineParser#withArguments(Argument...)} instead.
	 *
	 * @param actualArguments the arguments from the command line
	 * @return the parsed value from the {@code actualArguments}
	 * @throws ArgumentException if actualArguments isn't compatible with this
	 *             argument
	 */
	@Nullable
	public final T parse(String ... actualArguments) throws ArgumentException
	{
		return build().parse(actualArguments);
	}

	/**
	 * Returns a usage string for the argument that this builder builds.
	 * Should only be used if one argument is supported,
	 * otherwise the {@link CommandLineParser#usage()} method should be used instead.
	 */
	public final Usage usage()
	{
		return build().usage();
	}

	/**
	 * <pre>
	 * Returns a customized parser that the {@link Argument} will use to parse values.
	 *
	 * This is a suitable place to verify the configuration of your parser.
	 *
	 * If your {@link StringParser} doesn't support any configuration you can use
	 * {@link Arguments#withParser(StringParser)} directly instead of subclassing
	 * {@link ArgumentBuilder}
	 *
	 * &#64;return the {@link StringParser} that performs the actual parsing of an argument value
	 * &#64;throws IllegalStateException if the parser have been configured wrongly
	 * </pre>
	 */
	@Nonnull
	protected abstract StringParser<T> parser();

	@Nonnull
	InternalStringParser<T> internalParser()
	{
		StringParser<T> parser = parser();
		if(parser != InternalArgumentBuilder.MARKER)
			return new StringParserBridge<T>(parser);
		return internalStringParser;
	}

	/**
	 * Specifies the argument names that triggers the argument being built. As commands sometimes
	 * gets long and hard to understand it's recommended to also support long named arguments,
	 * making the commands even longer but more readable instead <br>
	 *
	 * @param argumentNames
	 *            <ul>
	 *            <li>"-o" for a short named option/argument</li>
	 *            <li>"--option-name" for a long named option/argument</li>
	 *            <li>"-o", "--option-name" to give the user both choices</li>
	 *            <li>zero elements: makes this an indexed argument, meaning that the argument must
	 *            be given at the same position on the command line as it is given to
	 *            {@link CommandLineParser#withArguments(Argument...)} (not counting named
	 *            arguments).</li>
	 *            </ul>
	 * @return this builder
	 */
	public SELF names(final String ... argumentNames)
	{
		checkNoSpaces(asList(argumentNames));
		names = Arrays.asList(argumentNames);
		return myself;
	}

	/**
	 * Works just like {@link #names(String...)} but it takes an {@link Iterable} instead.
	 *
	 * @param argumentNames the list to use as argument names
	 * @return this builder
	 */
	public SELF names(final Iterable<String> argumentNames)
	{
		checkNoSpaces(argumentNames);
		names = unmodifiableList(newArrayList(argumentNames));
		return myself;
	}

	/**
	 * Ignores the case of the argument names set by {@link ArgumentBuilder#names(String...)}
	 *
	 * @return this builder
	 */
	public final SELF ignoreCase()
	{
		ignoreCase = true;
		return myself;
	}

	/**
	 * <pre>
	 * Sets {@code theDescription} of what this argument does/means. Printed with
	 * {@link CommandLineParser#usage()}.
	 *
	 * For instance, in:
	 * -l, --enable-logging      Output debug information to standard out
	 *                           Default: disabled
	 * "Output debug information to standard out" is {@code theDescription}
	 * &#64;return this builder
	 * </pre>
	 */
	public final SELF description(final String theDescription)
	{
		description = withString(theDescription);
		return myself;
	}

	/**
	 * <pre>
	 * {@link Describable} version of {@link #description(String)}.
	 * &#64;return this builder
	 * </pre>
	 */
	public final SELF description(final Describable theDescription)
	{
		description = requireNonNull(theDescription);
		return myself;
	}

	/**
	 * <pre>
	 * Makes {@link CommandLineParser#parse(String...)} throw
	 * {@link ArgumentException} if this argument isn't given.
	 * If possible, it's preferred to use a {@link #defaultValue(Object) default value} instead.
	 *
	 * The {@link Argument#toString()} will be used to print each missing argument.
	 *
	 * <b>Note</b>: If you choose to use multiple {@link #required()} indexed arguments all of them
	 * must have unique {@link #metaDescription(String)}s. This ensures that error messages
	 * can point out erroneous arguments better
	 *
	 * &#64;return this builder
	 * @throws IllegalStateException if {@link #defaultValue(Object)} (or
	 *             {@link #defaultValueSupplier(Supplier)}) has been
	 *             called, because these are mutually exclusive with {@link #required()}
	 * </pre>
	 */
	public SELF required()
	{
		check(defaultValueSupplier == null, ProgrammaticErrors.DEFAULT_VALUE_AND_REQUIRED);
		required = true;
		return myself;
	}

	/**
	 * <pre>
	 * Sets a default value to use for this argument. Overrides {@link StringParser#defaultValue()} which is used by default.
	 * Returned by {@link ParsedArguments#get(Argument)} when no argument {@link ParsedArguments#wasGiven(Argument) was given}.
	 * To create default values lazily see {@link ArgumentBuilder#defaultValueSupplier(Supplier)}.
	 * </pre>
	 *
	 * <b>Mutability</b>:Remember that as {@link Argument} is {@link Immutable}
	 * this value should be so too if multiple argument parsings is going to take place.
	 * If mutability is wanted {@link ArgumentBuilder#defaultValueSupplier(Supplier)} should be used
	 * instead.
	 *
	 * @return this builder
	 * @throws IllegalStateException if {@link #required()} has been called,
	 *             because these two methods are mutually exclusive
	 */
	public SELF defaultValue(@Nullable final T value)
	{
		check(!required, ProgrammaticErrors.DEFAULT_VALUE_AND_REQUIRED);
		defaultValueSupplier = Suppliers2.ofInstance(value);
		return myself;
	}

	/**
	 * <pre>
	 * Sets a {@link Supplier} that can supply default values in the absence of this argument
	 * </pre>
	 *
	 * <b>Note:</b> Even if {@link #limitTo(Predicate)} is used, the {@link Supplier#get()} isn't
	 * called
	 * until the default value is actually needed ({@link ParsedArguments#get(Argument)}. If the
	 * default value is deemed non-allowed at that point an {@link IllegalStateException} is thrown.
	 * Wrap your supplier with {@link Suppliers2#memoize(Supplier)} if you want to cache created
	 * values.
	 *
	 * @return this builder
	 * @throws IllegalStateException if {@link #required()} has been called,
	 *             because these two methods are mutually exclusive
	 */
	public SELF defaultValueSupplier(final Supplier<? extends T> aDefaultValueSupplier)
	{
		check(!required, ProgrammaticErrors.DEFAULT_VALUE_AND_REQUIRED);
		defaultValueSupplier = requireNonNull(aDefaultValueSupplier);
		return myself;
	}

	/**
	 * Provides a way to give the usage texts a better explanation of a default
	 * value than {@link Object#toString()} provides. Always prints {@code aDescription} regardless
	 * of what the default value is.
	 *
	 * @param aDescription the description
	 * @return this builder
	 * @see Describers#withConstantString(String)
	 */
	public SELF defaultValueDescription(final String aDescription)
	{
		this.defaultValueDescriber = Describers.withConstantString(aDescription);
		return myself;
	}

	/**
	 * {@link Describer} version of {@link #defaultValueDescription(String)}
	 *
	 * @param describer a describer
	 * @return this builder
	 */
	public SELF defaultValueDescriber(final Describer<? super T> describer)
	{
		this.defaultValueDescriber = requireNonNull(describer);
		return myself;
	}

	/**
	 * <pre>
	 * By default {@link StringParser}s provides a meta description (by implementing {@link StringParser#metaDescription()}
	 * that describes the type of data they expect. For instance, if you're writing a music player,
	 * a user of your application would (most probably) rather see:
	 *
	 * --track-nr &lt;track nr&gt;    The track number to play (using {@code  metaDescription("<track nr>") })
	 *
	 * instead of
	 *
	 * --track-nr &lt;integer&gt;     The track number to play (the default provided by {@link StringParser#metaDescription()})
	 *
	 * So when using general data type parsers such as {@link StringParsers#integerParser()} you're better of if
	 * you provide a meta description that explains what the {@code integer} represents in the context of this argument.
	 *
	 * <b>Note:</b> empty meta descriptions aren't allowed
	 * <b>Note:</b> the surrounding &lt; and &gt; aren't enforced or added automatically but it's preferred
	 * to have them because it makes a clear distinction between {@link #names(String...) argument names} and their parameters.
	 *
	 * &#64;param aMetaDescription "&lt;track nr&gt;" in the above example
	 * &#64;return this builder
	 * &#64;throws IllegalArgumentException if aMetaDescription is empty
	 * </pre>
	 */
	public final SELF metaDescription(final String aMetaDescription)
	{
		check(aMetaDescription.length() > 0, ProgrammaticErrors.INVALID_META_DESCRIPTION);
		this.metaDescription = Optional.of(aMetaDescription);
		return myself;
	}

	/**
	 * Hides this argument so that it's not displayed in the usage texts.<br>
	 * It's recommended that hidden arguments have a reasonable {@link #defaultValue(Object)} and
	 * aren't {@link #required()}, in fact this is recommended for all arguments.
	 *
	 * @return this builder
	 */
	public final SELF hideFromUsage()
	{
		this.hideFromUsage = true;
		return myself;
	}

	/**
	 * For instance, "=" in --author=jjonsson
	 *
	 * @param aSeparator the character that separates the argument name and
	 *            argument value, defaults to a space
	 * @return this builder
	 */
	public SELF separator(final String aSeparator)
	{
		separator = requireNonNull(aSeparator);
		return myself;
	}

	/**
	 * <pre>
	 * Limits values parsed so that they conform to some specific rule.
	 * Only values for which {@link Predicate#test(Object)} returns true, will be accepted.
	 * Other values will cause an {@link ArgumentException}.
	 *
	 * To override the default error message that is generated with {@link UserErrors#DISALLOWED_VALUE}
	 * you can throw {@link IllegalArgumentException} from {@link Predicate#test(Object)}. The detail
	 * message of that exception will be used by {@link ArgumentException#getMessageAndUsage()}.
	 * When this is needed it's generally recommended to write a parser of its own instead.
	 *
	 * <b>Note:</b>{@link Object#toString() toString()} on {@code aLimiter} will replace {@link StringParser#descriptionOfValidValues(Locale)} in the usage
	 *
	 * <b>Note:</b>The validity of any {@link #defaultValueSupplier(Supplier) default value} isn't checked until
	 * it's actually needed when {@link ParsedArguments#get(Argument)} is called. This is so
	 * because {@link Supplier#get()} (or {@link StringParser#defaultValue()}) could take an arbitrary long time.
	 *
	 * <b>Note:</b>Any previously set limiter will be {@link Predicates2#and(Predicate, Predicate)
	 * and'ed} together with {@code aLimiter}.
	 * </pre>
	 *
	 * @param aLimiter a limiter
	 * @return this builder
	 */
	public SELF limitTo(Predicate<? super T> aLimiter)
	{
		limiter = Predicates2.<T>and(limiter, aLimiter);
		return myself;
	}

	/**
	 * <pre>
	 * Makes this argument handle "property like" arguments:
	 * -Dproperty_name=value
	 * where
	 * "-D" is the string supplied to {@link #names(String...)},
	 * "property_name" is the {@link String} key in the resulting {@link Map},
	 * "=" is the {@link #separator(String)} (set to "=" if it hasn't been overridden already),
	 * "value" is decoded by the previously set {@link StringParser}.
	 *
	 *
	 * Tip: You can pass {@link #defaultValueDescriber(Describer)} a
	 * {@link Describers#mapDescriber(Map)} to describe each property that you
	 * support (given that you've used {@link ArgumentBuilder#defaultValue(Object)}
	 * to use sane defaults for your properties).
	 *
	 * &#64;return a new (more specific) builder
	 *
	 * @see #asKeyValuesWithKeyParser(StringParser) to use other types as keys than {@link String}
	 * </pre>
	 */
	@CheckReturnValue
	public final MapArgumentBuilder<String, T> asPropertyMap()
	{
		return new MapArgumentBuilder<String, T>(this, stringParser());
	}

	/**
	 * <pre>
	 * Makes this argument handle properties like arguments:
	 * -Dproperty_name=value
	 * where "-D" is one of the strings supplied to {@link #names(String...)},
	 * "property_name" is decoded by {@code keyParser} and
	 * "value" is decoded by the {@link StringParser} previously passed to the constructor.
	 *
	 * For example:
	 * </pre>
	 *
	 * <pre class="prettyprint">
	 * <code class="language-java">
	 * Map&lt;Integer, Integer&gt; numberMap = Arguments.integerArgument("-N")
	 * 						.asKeyValuesWithKeyParser(StringParsers.integerParser())
	 * 						.parse("-N1=5", "-N2=10");
	 * assertThat(numberMap.get(1)).isEqualTo(5);
	 * </code>
	 * </pre>
	 *
	 * For this to work correctly it's paramount that {@code K} implements a
	 * proper {@link Object#hashCode()} because it's going to be used a key in a {@link Map}.
	 *
	 * @return a new (more specific) builder
	 */
	@CheckReturnValue
	public final <K> MapArgumentBuilder<K, T> asKeyValuesWithKeyParser(StringParser<K> keyParser)
	{
		return new MapArgumentBuilder<K, T>(this, requireNonNull(keyParser));
	}

	/**
	 * <pre>
	 * When given a "," this allows for
	 * arguments such as:
	 * -numbers 1,2,3
	 * where the resulting {@code List&lt;Integer&gt;} would contain 1, 2 and 3.
	 *
	 * <b>Note:</b> Doesn't allow empty lists. Leading and trailing whitespace will be trimmed for each element.
	 * </pre>
	 *
	 * @param valueSeparator the string to split the input with
	 * @return a new (more specific) builder
	 */
	@CheckReturnValue
	public SplitterArgumentBuilder<T> splitWith(final String valueSeparator)
	{
		return new SplitterArgumentBuilder<T>(this, valueSeparator);
	}

	/**
	 * Useful for handling a variable amount of parameters in the end of a
	 * command. Uses this argument to parse values but assumes that all the following
	 * parameters are of the same type, integer in the following example:
	 *
	 * <pre class="prettyprint">
	 * <code class="language-java">
	 * String[] threeArgs = {"--numbers", "1", "2", "3"};
	 * List&lt;Integer&gt; numbers = integerArgument("--numbers").variableArity().parse(threeArgs);
	 * assertThat(numbers).isEqualTo(asList(1, 2, 3));
	 *
	 * String[] twoArgs = {"--numbers", "1", "2"};
	 * List&lt;Integer&gt; numbers = integerArgument("--numbers").variableArity().parse(twoArgs);
	 * assertThat(numbers).isEqualTo(asList(1, 2));
	 * </code>
	 * </pre>
	 *
	 * <b>Note:</b> if {@link #defaultValue(Object) default value} has been set before
	 * {@link #variableArity()} is called the {@link #defaultValue(Object) default value} will be a
	 * list with that one element in it, otherwise it will be empty.
	 *
	 * @return a new (more specific) builder
	 */
	@CheckReturnValue
	public ArityArgumentBuilder<T> variableArity()
	{
		return new ArityArgumentBuilder<T>(this);
	}

	/**
	 * <pre>
	 * Uses this argument to parse values but assumes that {@code numberOfParameters} of the
	 * following parameters are of the same type.
	 *
	 * {@link Integer} in the following example:
	 * </pre>
	 *
	 * <pre class="prettyprint">
	 * <code class="language-java">
	 * List&lt;Integer&gt; numbers = integerArgument("--numbers").arity(2).parse("--numbers", "1", "2");
	 * assertThat(numbers).isEqualTo(asList(1, 2));
	 * </code>
	 * </pre>
	 *
	 * <b>Note:</b>If the argument isn't {@link #required()} the default value
	 * will be a list that contains {@code numberOfParameters} elements
	 * of {@link StringParser#defaultValue()}, in the above example that would be two zeros.
	 * If this isn't wanted use {@link #defaultValue(Object)} to override it.
	 *
	 * @return a new (more specific) builder
	 * @throws IllegalArgumentException if {@code numberOfParameters} is less than 2
	 */
	@CheckReturnValue
	public ArityArgumentBuilder<T> arity(final int numberOfParameters)
	{
		check(numberOfParameters > 1, ProgrammaticErrors.TO_SMALL_ARITY, numberOfParameters);
		return new ArityArgumentBuilder<T>(this, numberOfParameters);
	}

	// TODO: add minimumArguments(int)?

	/**
	 * Makes it possible to enter several values for the same argument. Such as this:
	 *
	 * <pre class="prettyprint">
	 * <code class="language-java">
	 * String[] arguments = {"--number", "1", "--number", "2"};
	 * List&lt;Integer&gt; numbers = integerArgument("--number").repeated().parse(arguments);
	 * assertThat(numbers).isEqualTo(asList(1, 2));
	 * </code>
	 * </pre>
	 *
	 * If you want to combine {@link #repeated()} with a specific {@link #arity(int)} then call
	 * {@link #arity(int)} before calling {@link #repeated()}:
	 *
	 * <pre class="prettyprint">
	 * <code class="language-java">
	 * String[] arguments = {"--numbers", "1", "2", "--numbers", "3", "4"};
	 * List&lt;List&lt;Integer&gt;&gt; numberLists = integerArgument("--numbers").arity(2).repeated().parse(arguments);
	 * assertThat(numberLists).isEqualTo(asList(asList(1, 2), asList(3, 4)));
	 * </code>
	 * </pre>
	 *
	 * For repeated values in a property map such as this:
	 *
	 * <pre class="prettyprint">
	 * <code class="language-java">
	 * String[] arguments = {"-Nnumber=1", "-Nnumber=2"};
	 * Map&lt;String, List&lt;Integer&gt;&gt; numberMap = integerArgument("-N").repeated().asPropertyMap().parse(arguments);
	 * assertThat(numberMap.get("number")).isEqualTo(asList(1, 2));
	 * </code>
	 * </pre>
	 *
	 * {@link #repeated()} should be called before {@link #asPropertyMap()}.
	 * For arguments without a name (indexed arguments) use {@link #variableArity()} instead.
	 *
	 * @return a new (more specific) builder
	 */
	@CheckReturnValue
	public RepeatedArgumentBuilder<T> repeated()
	{
		return new RepeatedArgumentBuilder<T>(this);
	}

	/**
	 * Makes it possible to chain together different transformation / map / conversion operations
	 * 
	 * <pre class="prettyprint">
	 * <code class="language-java">
	 * int size = Arguments.stringArgument("--foo").transform(String::length).parse("--foo", "abcd");
	 * assertThat(size).isEqualTo(4);
	 * </code>
	 * </pre>
	 * 
	 * @param transformer the function that takes a value of the previous type (like String in the
	 *            example), and converts it into another type of value
	 * @return a new (more specific) builder
	 */
	@CheckReturnValue
	public <F> TransformingArgumentBuilder<F> transform(Function<T, F> transformer)
	{
		return new TransformingArgumentBuilder<F>(this, transformer);
	}

	@Override
	public String toString()
	{
		return new StringJoiner(", ", ArgumentBuilder.class.getSimpleName() + "{", "}").add("names=" + names).add("description=" + description)
				.add("metaDescription=" + metaDescription).add("hideFromUsage=" + hideFromUsage).add("ignoreCase=" + ignoreCase)
				.add("limiter=" + limiter).add("required=" + required).add("separator=" + separator)
				.add("defaultValueDescriber=" + defaultValueDescriber).add("defaultValueSupplier=" + defaultValueSupplier)
				.add("internalStringParser=" + internalStringParser).toString();
	}

	/**
	 * <pre>
	 * Copies all values from the given copy into this one, except for:
	 * {@link #parser()}, {@link #defaultValueSupplier()} and {@link #defaultValueDescriber()}
	 * as they may change between different builders
	 * (e.g the default value for Argument&lt;Boolean&gt; and Argument&lt;List&lt;Boolean&gt; are not compatible)
	 * </pre>
	 *
	 * @param copy the ArgumentBuilder to copy from
	 */
	@OverridingMethodsMustInvokeSuper
	void copy(final ArgumentBuilder<?, ?> copy)
	{
		this.names = copy.names;
		this.description = copy.description;
		this.required = copy.required;
		this.separator = copy.separator;
		this.ignoreCase = copy.ignoreCase;
		this.isAllowedToRepeat = copy.isAllowedToRepeat;
		this.metaDescription = copy.metaDescription;
		this.hideFromUsage = copy.hideFromUsage;
	}

	/**
	 * <pre>
	 * {@code aFinalizer} is called after {@link StringParser#parse(String, Locale)}
	 * but before any predicates given to {@link #limitTo(Predicate)} are tested.
	 *
	 * Is used internally to finalize values produced by {@link StringParser#parse(String, Locale)}.
	 *
	 * For example {@link RepeatedArgumentParser} uses this to make the resulting {@link List} {@link Immutable}.
	 *
	 * For regular {@link StringParser}s it's recommended to use {@link SimpleForwardingStringParser}
	 * and decorate your {@link StringParser} with any finalization there instead.
	 *
	 * <b>Note:</b> If {@link #finalizeWith(Function)} have been called before,
	 * the given {@code aFinalizer} will be run after that finalizer.
	 * </pre>
	 *
	 * @param aFinalizer a finalizer
	 * @return this builder
	 */
	final SELF finalizeWith(Function<T, T> aFinalizer)
	{
		finalizer = Functions2.compound(finalizer, aFinalizer);
		return myself;
	}

	final void allowRepeatedArguments()
	{
		isAllowedToRepeat = true;
	}

	final void setAsPropertyMap()
	{
		isPropertyMap = true;
	}

	/**
	 * @formatter.off
	 */
	@Nonnull final List<String> names(){ return names; }

	@Nullable Describer<? super T> defaultValueDescriber(){ return defaultValueDescriber; }

	@Nonnull Describable description(){ return description; }

	final boolean isRequired(){ return required; }

	@Nullable final String separator(){ return separator; }

	final boolean isIgnoringCase(){ return ignoreCase; }

	final boolean isPropertyMap(){ return isPropertyMap; }

	final boolean isAllowedToRepeat(){ return isAllowedToRepeat; }

	@Nullable final Optional<String> metaDescription(){ return metaDescription; }
	final boolean isHiddenFromUsage(){ return hideFromUsage; }

	@Nullable final Supplier<? extends T> defaultValueSupplier(){ return defaultValueSupplier; }

	@Nonnull final Function<T, T> finalizer(){ return finalizer; }
	@Nonnull final Predicate<? super T> limiter(){ return limiter; }

	private void checkNoSpaces(Iterable<String> argumentNames)
	{
		for(String name : argumentNames)
		{
			check(!name.contains(" "), "Detected a space in %s, argument names must not have spaces in them", name);
		}
	}
	
	@Nonnull Supplier<? extends T> defaultValueSupplierOrFromParser()
	{
		if(defaultValueSupplier != null)
			return defaultValueSupplier;
		return (Supplier<T>) internalParser()::defaultValue;
	}

	/**
	 * @formatter.on
	 */

	/**
	 * A very simple version of an {@link ArgumentBuilder}, it's exposed mainly to
	 * lessen the exposure of the {@code SELF_TYPE} argument of the {@link ArgumentBuilder}.
	 */
	@NotThreadSafe
	public static final class DefaultArgumentBuilder<T> extends ArgumentBuilder<DefaultArgumentBuilder<T>, T>
	{
		private final StringParser<T> parser;

		DefaultArgumentBuilder(final StringParser<T> aParser)
		{
			parser = requireNonNull(aParser);
		}

		@Override
		protected StringParser<T> parser()
		{
			return parser;
		}
	}

	// Non-Interesting builders below, most declarations under here handles
	// (by deprecating) invalid invariants between different argument properties

	private abstract static class InternalArgumentBuilder<BUILDER extends InternalArgumentBuilder<BUILDER, T>, T> extends ArgumentBuilder<BUILDER, T>
	{
		/**
		 * Only used to flag that this builder is an internal one, not used for parsing
		 */
		static final StringParser<?> MARKER = new StringParserBridge<String>(StringParsers.stringParser());

		InternalArgumentBuilder()
		{

		}

		InternalArgumentBuilder(InternalStringParser<T> parser)
		{
			super(parser);
		}

		@SuppressWarnings("unchecked")
		@Override
		protected StringParser<T> parser()
		{
			return (StringParser<T>) MARKER;
		}
	}

	/**
	 * The {@link InternalStringParser} equivalent to
	 * {@link se.softhouse.jargo.ArgumentBuilder.DefaultArgumentBuilder}
	 */
	static final class SimpleArgumentBuilder<T> extends InternalArgumentBuilder<SimpleArgumentBuilder<T>, T>
	{
		SimpleArgumentBuilder(InternalStringParser<T> parser)
		{
			super(parser);
		}
	}

	/**
	 * Builder for {@link Command}s. Created with {@link Arguments#command(Command)}.
	 */
	@NotThreadSafe
	public static final class CommandBuilder extends InternalArgumentBuilder<CommandBuilder, ParsedArguments>
	{
		CommandBuilder(final Command command)
		{
			super(command);
		}

		/**
		 * @deprecated Commands shouldn't be required
		 */
		@Deprecated
		@Override
		public CommandBuilder required()
		{
			throw new IllegalStateException();
		}

		/**
		 * @deprecated Commands shouldn't have default values
		 */
		@Deprecated
		@Override
		public CommandBuilder defaultValue(ParsedArguments defaultValue)
		{
			throw new IllegalStateException();
		}

		/**
		 * @deprecated Commands shouldn't have default values
		 */
		@Deprecated
		@Override
		public CommandBuilder defaultValueSupplier(Supplier<? extends ParsedArguments> defaultValueSupplier)
		{
			throw new IllegalStateException();
		}

		/**
		 * @deprecated Commands can't have default values, so no description can be useful
		 */
		@Deprecated
		@Override
		public CommandBuilder defaultValueDescription(String defaultValueDescription)
		{
			throw new IllegalStateException();
		}

		/**
		 * @deprecated Commands can't have default values, so no description can be useful
		 */
		@Deprecated
		@Override
		public CommandBuilder defaultValueDescriber(Describer<? super ParsedArguments> defaultValueDescriber)
		{
			throw new IllegalStateException();
		}

		/**
		 * @deprecated Commands can't be limited
		 */
		@Deprecated
		@Override
		public CommandBuilder limitTo(Predicate<? super ParsedArguments> limiter)
		{
			throw new IllegalStateException();
		}

		/**
		 * @deprecated Commands can't be split
		 */
		@Deprecated
		@Override
		public SplitterArgumentBuilder<ParsedArguments> splitWith(String valueSeparator)
		{
			throw new IllegalStateException();
		}

		/**
		 * @deprecated pass any {@link Argument}s to your {@link Command} to
		 *             {@link Command#Command(Argument...)}
		 */
		@Deprecated
		@Override
		public ArityArgumentBuilder<ParsedArguments> arity(int arity)
		{
			throw new IllegalStateException();
		}

		/**
		 * @deprecated pass any {@link Argument}s to your {@link Command} to
		 *             {@link Command#Command(Argument...)}
		 */
		@Deprecated
		@Override
		public ArityArgumentBuilder<ParsedArguments> variableArity()
		{
			throw new IllegalStateException();
		}
	}

	public static class ListArgumentBuilder<BUILDER extends ListArgumentBuilder<BUILDER, T>, T> extends InternalArgumentBuilder<BUILDER, List<T>>
	{
		private ListArgumentBuilder(InternalStringParser<List<T>> parser)
		{
			super(parser);
		}

		void copyAsListBuilder(ArgumentBuilder<?, T> builder, int nrOfElementsInDefaultValue)
		{
			finalizeWith(listTransformer(builder.finalizer));
			finalizeWith(Functions2.<T>unmodifiableList());
			limitTo(listPredicate(builder.limiter));
			if(builder.defaultValueSupplier != null)
			{
				defaultValueSupplier(Suppliers2.ofRepeatedElements(builder.defaultValueSupplier, nrOfElementsInDefaultValue));
			}
			if(builder.defaultValueDescriber != null)
			{
				defaultValueDescriber(Describers.listDescriber(builder.defaultValueDescriber));
			}
		}

		/**
		 * Transforms this argument from a {@link List} to a {@link Set}. Thereby removing any duplicate values, given that
		 * {@link Object#equals(Object)} and {@link Object#hashCode()} has been implemented correctly by the element type.
		 * 
		 * @return a {@link se.softhouse.jargo.ArgumentBuilder.TransformingArgumentBuilder} that you can continue to configure
		 */
		public TransformingArgumentBuilder<Set<T>> unique()
		{
			return transform(list -> (Set<T>) new HashSet<>(list)).finalizeWith(Functions2.<T>unmodifiableSet());
		}
	}

	/**
	 * An intermediate builder used by {@link #arity(int)} and {@link #variableArity()}. It's mainly
	 * used to switch the T argument of the previous builder to List&lt;T&gt; and to indicate
	 * invalid call
	 * orders.
	 */
	@NotThreadSafe
	public static final class ArityArgumentBuilder<T> extends ListArgumentBuilder<ArityArgumentBuilder<T>, T>
	{
		private ArityArgumentBuilder(final ArgumentBuilder<? extends ArgumentBuilder<?, T>, T> builder, final int arity)
		{
			super(new FixedArityParser<T>(builder.internalParser(), arity));
			init(builder, arity);
		}

		private ArityArgumentBuilder(final ArgumentBuilder<? extends ArgumentBuilder<?, T>, T> builder)
		{
			super(new VariableArityParser<T>(builder.internalParser()));
			init(builder, 1);
		}

		private void init(final ArgumentBuilder<? extends ArgumentBuilder<?, T>, T> builder, int nrOfElementsInDefaultValue)
		{
			copy(builder);
			copyAsListBuilder(builder, nrOfElementsInDefaultValue);
		}

		/**
		 * @deprecated
		 *
		 *             <pre>
		 * This doesn't work with {@link ArgumentBuilder#arity(int)} or {@link ArgumentBuilder#variableArity()}
		 * I.e --foo 1,2 3,4
		 * is currently unsupported
		 *             </pre>
		 */
		@Deprecated
		@Override
		public SplitterArgumentBuilder<List<T>> splitWith(final String valueSeparator)
		{
			throw new IllegalStateException("splitWith(...) doesn't work with arity/variableArity()");
		}
	}

	/**
	 * An intermediate builder used by {@link #repeated()}. It's mainly used to switch the T
	 * argument of the previous builder to List&lt;T&gt; and to indicate invalid call orders.
	 */
	@NotThreadSafe
	public static final class RepeatedArgumentBuilder<T> extends ListArgumentBuilder<RepeatedArgumentBuilder<T>, T>
	{
		private RepeatedArgumentBuilder(final ArgumentBuilder<? extends ArgumentBuilder<?, T>, T> builder)
		{
			super(new RepeatedArgumentParser<T>(builder.internalParser()));
			copy(builder);
			copyAsListBuilder(builder, 1);
			allowRepeatedArguments();
		}

		/**
		 * @deprecated this method should be called before {@link #repeated()}
		 */
		@Deprecated
		@Override
		public ArityArgumentBuilder<List<T>> arity(final int numberOfParameters)
		{
			throw new IllegalStateException("Programmer Error. Call arity(...) before repeated()");
		}

		/**
		 * @deprecated this method should be called before {@link #repeated()}
		 */
		@Override
		@Deprecated
		public ArityArgumentBuilder<List<T>> variableArity()
		{
			throw new IllegalStateException("Programmer Error. Call variableArity(...) before repeated()");
		}

		/**
		 * @deprecated call {@link #splitWith(String)} before {@link #repeated()}
		 */
		@Deprecated
		@Override
		public SplitterArgumentBuilder<List<T>> splitWith(final String valueSeparator)
		{
			throw new IllegalStateException("call splitWith(String) before repeated()");
		}
	}

	/**
	 * An intermediate builder used by {@link Arguments#optionArgument(String, String...)}
	 */
	@NotThreadSafe
	public static final class OptionArgumentBuilder extends InternalArgumentBuilder<OptionArgumentBuilder, Boolean>
	{
		OptionArgumentBuilder()
		{
			defaultValue(false);
		}

		@Override
		InternalStringParser<Boolean> internalParser()
		{
			return optionParser(defaultValueSupplier().get());
		}

		@Override
		public OptionArgumentBuilder defaultValue(@Nonnull Boolean value)
		{
			requireNonNull(value, ProgrammaticErrors.OPTION_DOES_NOT_ALLOW_NULL_AS_DEFAULT);
			return super.defaultValue(value);
		}

		@Override
		public OptionArgumentBuilder names(Iterable<String> argumentNames)
		{
			check(!isEmpty(argumentNames), ProgrammaticErrors.OPTIONS_REQUIRES_AT_LEAST_ONE_NAME);
			return super.names(argumentNames);
		}

		@Override
		public OptionArgumentBuilder names(String ... argumentNames)
		{
			check(argumentNames.length >= 1, ProgrammaticErrors.OPTIONS_REQUIRES_AT_LEAST_ONE_NAME);
			return super.names(argumentNames);
		}

		/**
		 * @deprecated an optional flag can't be required
		 */
		@Deprecated
		@Override
		public OptionArgumentBuilder required()
		{
			throw new IllegalStateException("An optional flag can't be requried");
		}

		/**
		 * @deprecated a separator is useless since an optional flag can't be
		 *             assigned a value
		 */
		@Deprecated
		@Override
		public OptionArgumentBuilder separator(final String aSeparator)
		{
			throw new IllegalStateException("A seperator for an optional flag isn't supported as an optional flag can't be assigned a value");
		}

		/**
		 * @deprecated an optional flag can only have an arity of zero
		 */
		@Deprecated
		@Override
		public ArityArgumentBuilder<Boolean> arity(final int numberOfParameters)
		{
			throw new IllegalStateException("An optional flag can't have any other arity than zero");
		}

		/**
		 * @deprecated an optional flag can only have an arity of zero
		 */
		@Deprecated
		@Override
		public ArityArgumentBuilder<Boolean> variableArity()
		{
			throw new IllegalStateException("An optional flag can't have any other arity than zero");
		}

		/**
		 * @deprecated an optional flag can't be split by anything
		 */
		@Deprecated
		@Override
		public SplitterArgumentBuilder<Boolean> splitWith(final String valueSeparator)
		{
			throw new IllegalStateException("An optional flag can't be split as it has no value that is parsed");
		}
	}

	/**
	 * An intermediate builder used by {@link #asPropertyMap()}. It's mainly used to switch the T
	 * argument of the previous builder to Map&lt;K, T&gt; and to indicate invalid call orders.
	 */
	@NotThreadSafe
	public static final class MapArgumentBuilder<K, V> extends InternalArgumentBuilder<MapArgumentBuilder<K, V>, Map<K, V>>
	{
		private final ArgumentBuilder<?, V> valueBuilder;
		private final StringParser<K> keyParser;

		private MapArgumentBuilder(final ArgumentBuilder<?, V> builder, StringParser<K> keyParser)
		{
			this.valueBuilder = builder;
			this.keyParser = keyParser;
			copy(builder);

			finalizeWith(Functions2.<K, V>mapValueTransformer(builder.finalizer));
			finalizeWith(Functions2.<K, V>unmodifiableMap());

			setAsPropertyMap();
		}

		@Override
		InternalStringParser<Map<K, V>> internalParser()
		{
			check(names().size() > 0, ProgrammaticErrors.NO_NAME_FOR_PROPERTY_MAP);
			if(separator().equals(DEFAULT_SEPARATOR))
			{
				separator("=");
			}
			else
			{
				check(separator().length() > 0, ProgrammaticErrors.EMPTY_SEPARATOR);
			}
			return new KeyValueParser<K, V>(keyParser,
					valueBuilder.internalParser(),
					valueBuilder.limiter,
					defaultValueSupplier(),
					valueBuilder.defaultValueSupplier);
		}

		// A Describer<? super V> is also a describer for a V
		@SuppressWarnings("unchecked")
		@Override
		Describer<? super Map<K, V>> defaultValueDescriber()
		{
			Describer<? super Map<K, V>> overriddenDescriber = super.defaultValueDescriber();
			if(overriddenDescriber != null)
				return overriddenDescriber;

			Describer<? super V> valueDescriber = valueBuilder.defaultValueDescriber();
			if(valueDescriber != null)
			{
				Describer<?> mapDescriber = Describers.mapDescriber(valueDescriber, separator());
				return (Describer<? super Map<K, V>>) mapDescriber;
			}
			return Describers.mapDescriber(Describers.<V>toStringDescriber(), separator());
		}

		/**
		 * @deprecated because {@link #repeated()} should be called before {@link #asPropertyMap()}
		 */
		@Deprecated
		@Override
		public RepeatedArgumentBuilder<Map<K, V>> repeated()
		{
			throw new IllegalStateException("You'll need to call repeated before asPropertyMap");
		}

		/**
		 * @deprecated because {@link #splitWith(String)} should be
		 *             called before {@link #asPropertyMap()}.
		 *             This is to make generic work its magic and produce the
		 *             correct type, for example {@code Map<String, List<Integer>>}.
		 */
		@Deprecated
		@Override
		public SplitterArgumentBuilder<Map<K, V>> splitWith(final String valueSeparator)
		{
			throw new IllegalStateException("You'll need to call splitWith before asPropertyMap");
		}

		/**
		 * @deprecated because {@link #arity(int)} should be
		 *             called before {@link #asPropertyMap()}.
		 */
		@Deprecated
		@Override
		public ArityArgumentBuilder<Map<K, V>> arity(final int nrOfParameters)
		{
			throw new IllegalStateException("You'll need to call arity before asPropertyMap");
		}

		/**
		 * @deprecated because {@link #asPropertyMap()} is already of variable arity
		 */
		@Deprecated
		@Override
		public ArityArgumentBuilder<Map<K, V>> variableArity()
		{
			throw new IllegalStateException("asPropertyMap is already of variable arity");
		}

		@Override
		public MapArgumentBuilder<K, V> defaultValue(Map<K, V> defaultKeyValues)
		{
			return super.defaultValue(new LinkedHashMap<>(defaultKeyValues));
		}
	}

	/**
	 * An intermediate builder used by {@link #splitWith(String)}. It's mainly used to switch the T
	 * argument of the previous builder to List&lt;T&gt; and to indicate invalid call orders.
	 */
	@NotThreadSafe
	public static final class SplitterArgumentBuilder<T> extends ListArgumentBuilder<SplitterArgumentBuilder<T>, T>
	{
		private SplitterArgumentBuilder(final ArgumentBuilder<?, T> builder, final String valueSeparator)
		{
			super(new StringSplitterParser<T>(valueSeparator, builder.internalParser()));
			copy(builder);
			copyAsListBuilder(builder, 1);
		}

		/**
		 * @deprecated you can't use both {@link #splitWith(String)} and {@link #arity(int)}
		 */
		@Deprecated
		@Override
		public ArityArgumentBuilder<List<T>> arity(final int numberOfParameters)
		{
			throw new IllegalStateException("You can't use both splitWith and arity");
		}

		/**
		 * @deprecated you can't use both {@link #splitWith(String)} and {@link #variableArity()}
		 */
		@Deprecated
		@Override
		public ArityArgumentBuilder<List<T>> variableArity()
		{
			throw new IllegalStateException("You can't use both splitWith and variableArity");
		}
	}

	/**
	 * An intermediate builder used by {@link #transform(Function)}. It's used to switch the
	 * {@code <T>}
	 * argument of the previous builder to {@code <F>} and to indicate invalid call orders.
	 * 
	 * @param <F> The new type
	 */
	@NotThreadSafe
	public static final class TransformingArgumentBuilder<F> extends InternalArgumentBuilder<TransformingArgumentBuilder<F>, F>
	{
		private <T> TransformingArgumentBuilder(final ArgumentBuilder<?, T> builder, final Function<T, F> transformer)
		{
			super(new TransformingParser<T, F>(builder.internalParser(), transformer, builder.limiter()));
			copy(builder);

			Supplier<? extends T> defaultValueSupplier = builder.defaultValueSupplierOrFromParser();
			defaultValueSupplier(Suppliers2.wrapWithPredicateAndTransform(defaultValueSupplier, transformer, builder.limiter()));

			if(builder.defaultValueDescriber() != null)
			{
				defaultValueDescriber(new BeforeTransformationDescriber<>(defaultValueSupplier, builder.defaultValueDescriber()));
			}
		}
	}

	private static final class BeforeTransformationDescriber<F> implements Describer<Object>
	{
		private final Supplier<? extends F> valueProvider;
		private final Describer<F> beforeDescriber;

		BeforeTransformationDescriber(Supplier<? extends F> valueProvider, Describer<F> beforeDescriber)
		{
			this.valueProvider = requireNonNull(valueProvider);
			this.beforeDescriber = requireNonNull(beforeDescriber);
		}

		@Override
		public String describe(Object value, Locale inLocale)
		{
			F beforeValue = valueProvider.get();
			return beforeDescriber.apply(beforeValue);
		}
	}
}
