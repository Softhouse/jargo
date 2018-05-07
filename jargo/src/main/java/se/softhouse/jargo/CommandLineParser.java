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
import static java.util.Objects.requireNonNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import javax.annotation.concurrent.GuardedBy;
import javax.annotation.concurrent.ThreadSafe;

import se.softhouse.common.strings.Describable;
import se.softhouse.jargo.ArgumentBuilder.SimpleArgumentBuilder;
import se.softhouse.jargo.StringParsers.RunnableParser;

/**
 * Manages multiple {@link Argument}s and/or {@link Command}s. The brain of this API.
 * Its primary goal is to decide which {@link Argument} each input string belongs to.
 * A {@link CommandLineParser} can be reused to parse arguments over and over again (even
 * concurrently). Different {@link CommandLineParser}s can even share {@link Argument}
 * configurations as {@link Argument} instances are immutable as well. Documentation through
 * example:
 *
 * <pre class="prettyprint">
 * <code class="language-java">
 * import static se.softhouse.jargo.Arguments.*;
 * ...
 * String[] args = {"--enable-logging", "--listen-port", "8090", "Hello"};
 *
 * Argument&lt;?&gt; helpArgument = helpArgument("-h", "--help"); //Will throw when -h is encountered
 * Argument&lt;Boolean&gt; enableLogging = optionArgument("-l", "--enable-logging").description("Output debug information to standard out").build();
 * Argument&lt;String&gt; greetingPhrase = stringArgument().description("A greeting phrase to greet new connections with").build();
 * Argument&lt;List&lt;Integer&gt;&gt; ports = integerArgument("-p", "--listen-port")
 * 						.defaultValue(8080)
 * 						.description("The port clients should connect to.")
 * 						.metaDescription("&lt;port&gt;")
 * 						.limitTo(number -&gt; number &gt;= 0 &amp;&amp; number &lt;= 65536)
 * 						.repeated().build();
 *
 * try
 * {
 *   ParsedArguments arguments = CommandLineParser.withArguments(helpArgument, greetingPhrase, enableLogging, ports).parse(args);
 *   assertThat(arguments.get(enableLogging)).isTrue();
 *   assertThat(arguments.get(ports)).isEqualTo(Arrays.asList(8090));
 *   assertThat(arguments.get(greetingPhrase)).isEqualTo("Hello");
 * }
 * catch(ArgumentException exception)
 * {
 *   System.out.println(exception.getMessageAndUsage());
 *   System.exit(1);
 * }
 * </code>
 * </pre>
 *
 * <pre>
 * For this program the usage would look like ("YourProgramName" is fetched from stack traces by default):
 * <code>
 * Usage: YourProgramName [Arguments]
 *
 * Arguments:
 * &lt;string&gt;                       A greeting phrase to greet new connections with
 *                                &lt;string&gt;: any string
 *                                Default:
 * -l, --enable-logging           Output debug information to standard out
 *                                Default: disabled
 * -p, --listen-port &lt;port&gt;       The port clients should connect to [Supports Multiple occurrences]
 *                                port: [0‥65536]
 *                                Default: 8080
 * </code>
 * If something goes wrong during the parsing (Missing required arguments, Unexpected arguments, Invalid values),
 * it will be described by the ArgumentException. Use {@link ArgumentException#getMessageAndUsage()} if you
 * want to explain what went wrong to the user.
 * </pre>
 *
 * <b>Internationalization</b> By default {@link Locale#US} is used for parsing strings and printing
 * usages. To change this use {@link #locale(Locale)}.<br>
 * <b>Thread safety concerns:</b> If there is a parsing occurring while any modifying method is
 * invoked, such as {@link #programName(String)}, the changes won't be seen until the start of the
 * next parse invocation. Also, if the goal is to set {@link #programName(String)} and
 * {@link #programDescription(String)} atomically you'll have to synchronize such changes
 * externally.
 */
@ThreadSafe
// TODO(jontejj): make this Immutable
public final class CommandLineParser
{
	// Internally CommandLineParser is a builder for CommandLineParserInstance but the idea is to
	// keep this idea hidden in the API to lessen the API complexity

	static final Locale US_BY_DEFAULT = Locale.US;

	@GuardedBy("modifyGuard") private volatile CommandLineParserInstance cachedParser;
	/**
	 * Use of this lock makes sure that there's no race condition if several concurrent calls
	 * to addArguments are made at the same time, in such a race some argument definitions
	 * could be "lost" without this lock.
	 */
	private final Lock modifyGuard = new ReentrantLock();

	private CommandLineParser(Iterable<Argument<?>> argumentDefinitions)
	{
		this.cachedParser = new CommandLineParserInstance(argumentDefinitions);
	}

	CommandLineParserInstance parser()
	{
		return cachedParser;
	}

	/**
	 * Creates a {@link CommandLineParser} with support for the given {@code argumentDefinitions}.
	 * {@link Command}s can be added later with {@link #andCommands(Command...)}.
	 *
	 * @param argumentDefinitions {@link Argument}s produced with {@link Arguments} or
	 *            with your own disciples of {@link ArgumentBuilder}
	 * @return a CommandLineParser which you can call {@link CommandLineParser#parse(String...)} on
	 *         and get {@link ParsedArguments} out of.
	 * @throws IllegalArgumentException if the given {@code argumentDefinitions} would create an
	 *             erroneous CommandLineParser
	 * @see CommandLineParser
	 */
	@CheckReturnValue
	@Nonnull
	public static CommandLineParser withArguments(final Argument<?> ... argumentDefinitions)
	{
		return new CommandLineParser(Arrays.asList(argumentDefinitions));
	}

	/**
	 * {@link Iterable} version of {@link #withArguments(Argument...)}
	 */
	@CheckReturnValue
	@Nonnull
	public static CommandLineParser withArguments(final Iterable<Argument<?>> argumentDefinitions)
	{
		return new CommandLineParser(argumentDefinitions);
	}

	/**
	 * Creates a {@link CommandLineParser} with support for {@code commands}. To add additional
	 * {@link Argument}s or {@link Command}s there is {@link #andArguments(Argument...)} and
	 * {@link #andCommands(Command...)}.
	 *
	 * @param commands the commands to support initially
	 * @return a CommandLineParser which you can call {@link CommandLineParser#parse(String...)} on
	 *         and get {@link ParsedArguments} out of.
	 * @see CommandLineParser
	 */
	@CheckReturnValue
	@Nonnull
	public static CommandLineParser withCommands(final Command ... commands)
	{
		return new CommandLineParser(Command.subCommands(commands));
	}

	/**
	 * An alternative to {@link Command} that accepts an {@link Enum} following the <a
	 * href="http://en.wikipedia.org/wiki/Command_pattern">Command
	 * Pattern</a>. This alternative is viable if the commands don't accept any parameters.
	 * Example enum:
	 *
	 * <pre class="prettyprint">
	 * <code class="language-java">
	 * public enum Service implements Runnable, Describable
	 * {
	 *   START{
	 * 	   &#64;Override
	 * 	   public void run(){
	 * 	     //Start service here
	 * 	   }
	 *
	 * 	   &#64;Override
	 * 	   public String description(){
	 * 	     return "Starts the service";
	 * 	   }
	 *   };
	 * }
	 * </code>
	 * </pre>
	 *
	 * The {@link ArgumentBuilder#names(String...) name} for each command will be the enum constants
	 * {@link Enum#name() name} in {@link String#toLowerCase(Locale) lower case}.
	 *
	 * @param commandEnum the {@link Class} <i>literal</i> for {@code Service} in the example above
	 * @return a CommandLineParser which you can call {@link CommandLineParser#parse(String...)} on
	 */
	public static <E extends Enum<E> & Runnable & Describable> CommandLineParser withCommands(Class<E> commandEnum)
	{
		return new CommandLineParser(commandsToArguments(commandEnum));
	}

	/**
	 * Parses {@code actualArguments} (typically from the command line, i.e argv) and returns the
	 * parsed values in a {@link ParsedArguments} container. {@link Locale#US} is used to parse
	 * strings by default. Use {@link #locale(Locale)} to change it.
	 *
	 * @throws ArgumentException if an invalid argument is encountered during the parsing
	 */
	@Nonnull
	public ParsedArguments parse(final String ... actualArguments) throws ArgumentException
	{
		return parser().parse(asList(actualArguments));
	}

	/**
	 * {@link Iterable} version of {@link #parse(String...)}
	 */
	@Nonnull
	public ParsedArguments parse(final Iterable<String> actualArguments) throws ArgumentException
	{
		return parser().parse(actualArguments);
	}

	/**
	 * Returns a usage text describing all arguments this {@link CommandLineParser} handles.
	 * Suitable to print on {@link System#out}.
	 */
	@CheckReturnValue
	@Nonnull
	public Usage usage()
	{
		return new Usage(parser());
	}

	/**
	 * Adds support for {@code commandsToAlsoSupport} in this {@link CommandLineParser}. Typically
	 * used in a chained fashion when faced with many supported {@link Command}s.
	 *
	 * @param commandsToAlsoSupport the commands to add support for
	 * @return this {@link CommandLineParser} to allow for chained calls
	 */
	@CheckReturnValue
	@Nonnull
	public CommandLineParser andCommands(final Command ... commandsToAlsoSupport)
	{
		verifiedAdd(Command.subCommands(commandsToAlsoSupport));
		return this;
	}

	/**
	 * <pre>
	 * Adds support for {@code argumentsToAlsoSupport} in this {@link CommandLineParser}.
	 * Typically used in a chained fashion when faced with many supported arguments.
	 * Another usage is simply for readability: group arguments by logical groups
	 *
	 * &#64;param argumentsToAlsoSupport the arguments to add support for
	 * @return this {@link CommandLineParser} to allow for chained calls
	 * </pre>
	 */
	@CheckReturnValue
	@Nonnull
	public CommandLineParser andArguments(final Argument<?> ... argumentsToAlsoSupport)
	{
		verifiedAdd(asList(argumentsToAlsoSupport));
		return this;
	}

	/**
	 * Verify that the parser would be usable after adding {@code argumentsToAdd} to it.
	 * This allows future parse operations to still use the unaffected old parser.
	 */
	private void verifiedAdd(Collection<Argument<?>> argumentsToAdd)
	{
		try
		{
			modifyGuard.lock();
			List<Argument<?>> newDefinitions = new ArrayList<>(parser().allArguments());
			newDefinitions.addAll(argumentsToAdd);
			cachedParser = new CommandLineParserInstance(newDefinitions, parser().programInformation(), parser().locale(), false);
		}
		finally
		{
			modifyGuard.unlock();
		}
	}

	/**
	 * Sets the {@code programName} to print with {@link #usage()}
	 *
	 * @return this parser
	 */
	public CommandLineParser programName(String programName)
	{
		try
		{
			modifyGuard.lock();
			ProgramInformation programInformation = parser().programInformation().programName(programName);
			cachedParser = new CommandLineParserInstance(parser().allArguments(), programInformation, parser().locale(), false);
		}
		finally
		{
			modifyGuard.unlock();
		}
		return this;
	}

	/**
	 * Sets the {@code programDescription} to print with {@link #usage()}
	 *
	 * @return this parser
	 */
	public CommandLineParser programDescription(String programDescription)
	{
		try
		{
			modifyGuard.lock();
			ProgramInformation programInformation = parser().programInformation().programDescription(programDescription);
			cachedParser = new CommandLineParserInstance(parser().allArguments(), programInformation, parser().locale(), false);
		}
		finally
		{
			modifyGuard.unlock();
		}
		return this;
	}

	/**
	 * {@link Locale#getDefault()} is dangerous to use. It's a global and mutable variable
	 * which makes it hard to give any guarantees on what it will be at a certain time. This is why
	 * {@link Locale#US} is used by default instead.
	 * If {@link Locale#getDefault()} is wanted, use {@link #locale(Locale)
	 * locale(Locale.getDefault())}.
	 *
	 * @param localeToUse the {@link Locale} to parse input strings with (it will be passed to
	 *            {@link StringParser#parse(String, Locale)} and
	 *            {@link StringParser#descriptionOfValidValues(Locale)})
	 * @return this parser
	 */
	public CommandLineParser locale(Locale localeToUse)
	{
		try
		{
			modifyGuard.lock();
			cachedParser = new CommandLineParserInstance(parser().allArguments(), parser().programInformation(), requireNonNull(localeToUse), false);
		}
		finally
		{
			modifyGuard.unlock();
		}
		return this;
	}

	/**
	 * Returns the {@link #usage()} for this {@link CommandLineParser}
	 */
	@Override
	public String toString()
	{
		return usage().toString();
	}

	private static <E extends Enum<E> & Runnable & Describable> List<Argument<?>> commandsToArguments(Class<E> commandEnum)
	{
		List<Argument<?>> commandsAsArguments = new ArrayList<>();
		for(E command : commandEnum.getEnumConstants())
		{
			Argument<Object> commandAsArgument = new SimpleArgumentBuilder<Object>(new RunnableParser(command)) //
					.names(command.name().toLowerCase(Locale.US)) //
					.description(command) //
					.build();
			commandsAsArguments.add(commandAsArgument);
		}
		return commandsAsArguments;
	}
}
