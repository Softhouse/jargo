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
import static java.util.Collections.unmodifiableList;
import static se.softhouse.jargo.Arguments.command;
import static se.softhouse.jargo.CommandLineParser.US_BY_DEFAULT;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.function.Supplier;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import javax.annotation.concurrent.Immutable;

import se.softhouse.common.guavaextensions.Suppliers2;
import se.softhouse.common.strings.Describable;
import se.softhouse.jargo.StringParsers.InternalStringParser;
import se.softhouse.jargo.internal.Texts.UsageTexts;

/**
 * <pre>
 * {@link Command}s automatically gets an invocation of execute when given on the command line.
 * This is particularly useful to avoid a never ending switch statement.
 *
 * {@link Command}s have a {@link CommandLineParser} themselves (and thereby sub-commands are allowed as well), that is,
 * they execute a command and may support contextual arguments as specified by the constructor {@link Command#Command(Argument...)}.
 *
 * Sub-commands are executed before their parent {@link Command}.
 *
 * To integrate your {@link Command} into an {@link Argument} use {@link Arguments#command(Command)}
 * or {@link CommandLineParser#withCommands(Command...)} if you have several commands.
 *
 * If you support several commands and a user enters several of them at the same
 * time they will be executed in the order given to {@link CommandLineParser#parse(String...)}.
 * If any {@link StringParser#parse(String, Locale) parse} errors occurs (for {@link Command#Command(Argument...) command arguments}) no {@link Command}
 * will be executed. So all arguments, for all commands given, are parsed before any execution occurs.
 *
 * <b>Mutability note:</b> although a {@link Command} should be {@link Immutable}
 * the objects it handles doesn't have to be. So repeated invocations of execute
 * is allowed to yield different results or to affect external state.
 * </pre>
 *
 * Example subclass:
 *
 * <pre class="prettyprint">
 * <code class="language-java">
 * public class Build extends Command
 * {
 *   private static final Argument&lt;File&gt; PATH = Arguments.fileArgument().description("the directory to build").build();
 *
 *   public Build()
 *   {
 *     super(PATH);
 *   }
 *
 *   public String description()
 *   {
 *     return "Builds a target";
 *   }
 *
 *   protected void execute(ParsedArguments parsedArguments)
 *   {
 *     File pathToBuild = parsedArguments.get(PATH);
 *     //Build pathToBuild here
 *   }
 * }
 * </code>
 * </pre>
 *
 * And the glue needed to integrate the Build {@link Command} with a {@link CommandLineParser}:
 *
 * <pre class="prettyprint">
 * <code class="language-java">
 * CommandLineParser.withCommands(new Build()).parse("build", "some_directory_that_needs_building");
 * </code>
 * </pre>
 *
 * As can be seen in the example the command name is the class name in lower case by default. This
 * may be overridden with {@link #commandName()}.<br>
 * If your commands don't require any arguments you can use
 * {@link CommandLineParser#withCommands(Class)} instead.
 */
@Immutable
public abstract class Command extends InternalStringParser<ParsedArguments> implements Describable
{
	@Nonnull private final List<Argument<?>> commandArguments;

	@Nonnull private final Supplier<CommandLineParserInstance> commandArgumentParser = Suppliers2.memoize(new Supplier<CommandLineParserInstance>(){
		@Override
		public CommandLineParserInstance get()
		{
			return new CommandLineParserInstance(commandArguments, ProgramInformation.AUTO, US_BY_DEFAULT, true);
		}
	});

	/**
	 * @param commandArguments the arguments that this command supports.
	 */
	protected Command(Argument<?> ... commandArguments)
	{
		this.commandArguments = unmodifiableList(asList(commandArguments));
	}

	/**
	 * @param commandArguments the arguments that this command supports.
	 */
	protected Command(List<Argument<?>> commandArguments)
	{
		this.commandArguments = Collections.unmodifiableList(commandArguments);
	}

	/**
	 * Useful if your {@link Command} has subcommands that you'll want to pass into the
	 * {@link Command#Command(List)} constructor
	 * 
	 * @param commands the subcommands
	 * @return the subcommands as an argument list
	 */
	public static List<Argument<?>> subCommands(final Command ... commands)
	{
		List<Argument<?>> commandsAsArguments = new ArrayList<>(commands.length);
		for(Command c : commands)
		{
			commandsAsArguments.add(command(c).build());
		}
		return commandsAsArguments;
	}

	/**
	 * The name that triggers this command. Defaults to {@link Class#getSimpleName()} in lower
	 * case. For several names override this with {@link ArgumentBuilder#names(String...)}
	 */
	@Nonnull
	@CheckReturnValue
	protected String commandName()
	{
		return getClass().getSimpleName().toLowerCase(Locale.US);
	}

	/**
	 * Called when this command is encountered on the command line
	 *
	 * @param parsedArguments a container with parsed values for the command arguments,
	 *            as specified by {@link Command#Command(Argument...)}
	 */
	protected abstract void execute(ParsedArguments parsedArguments);

	/**
	 * Override to provide a description to print in the usage text for this command.
	 * This is essentially an alternative to {@link ArgumentBuilder#description(Describable)}
	 *
	 * @return the description to use in the usage text
	 */
	@Override
	@Nonnull
	public String description()
	{
		return "";
	}

	@Override
	public String toString()
	{
		return commandName();
	}

	@Override
	final ParsedArguments parse(final ArgumentIterator arguments, final ParsedArguments previousOccurance, final Argument<?> argumentSettings,
			Locale locale) throws ArgumentException
	{
		arguments.rememberAsCommand();
		if(previousOccurance != null)
			return resumeParsing(arguments, previousOccurance, locale);
		ParsedArguments parsedArguments = parser().parse(arguments, locale);

		arguments.rememberInvocationOfCommand(this, parsedArguments, argumentSettings, commandArguments);

		return parsedArguments;
	}

	final ParsedArguments resumeParsing(final ArgumentIterator arguments, final ParsedArguments previousOccurance, Locale locale)
			throws ArgumentException
	{
		ParsedArguments result = parser().parseArguments(previousOccurance, arguments, locale);
		arguments.temporaryRepitionAllowedForCommand = false;
		return result;
	}

	/**
	 * The parser for parsing the {@link Argument}s passed to {@link Command#Command(Argument...)}
	 */
	CommandLineParserInstance parser()
	{
		return commandArgumentParser.get();
	}

	@Override
	final ParsedArguments defaultValue()
	{
		return null;
	}

	@Override
	final String descriptionOfValidValues(Argument<?> argumentSettings, Locale locale)
	{
		// For commands the validValues is a usage text itself for the command arguments
		return parser().usage(locale).toString();
	}

	@Override
	final String describeValue(ParsedArguments value)
	{
		return null;
	}

	@Override
	String metaDescription(Argument<?> argumentSettings)
	{
		return "";
	}

	@Override
	String metaDescriptionInRightColumn(Argument<?> argumentSettings)
	{
		return UsageTexts.ARGUMENT_HEADER;
	}
}
