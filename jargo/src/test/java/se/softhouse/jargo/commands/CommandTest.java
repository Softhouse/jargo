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
package se.softhouse.jargo.commands;

import static java.util.Collections.emptyList;
import static org.fest.assertions.Assertions.assertThat;
import static org.junit.Assert.fail;
import static se.softhouse.jargo.Arguments.command;
import static se.softhouse.jargo.Arguments.stringArgument;
import static se.softhouse.jargo.utils.Assertions2.assertThat;
import static se.softhouse.jargo.utils.ExpectedTexts.expected;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.SortedSet;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.Test;

import com.google.common.collect.Lists;

import se.softhouse.common.guavaextensions.Predicates2;
import se.softhouse.common.guavaextensions.Suppliers2;
import se.softhouse.common.strings.Describable;
import se.softhouse.common.strings.Describers;
import se.softhouse.jargo.Argument;
import se.softhouse.jargo.ArgumentBuilder.CommandBuilder;
import se.softhouse.jargo.ArgumentException;
import se.softhouse.jargo.ArgumentExceptions;
import se.softhouse.jargo.Arguments;
import se.softhouse.jargo.Command;
import se.softhouse.jargo.CommandLineParser;
import se.softhouse.jargo.FakeCompleter;
import se.softhouse.jargo.ParsedArguments;
import se.softhouse.jargo.Usage;
import se.softhouse.jargo.commands.Build.BuildTarget;
import se.softhouse.jargo.commands.Commit.Repository;
import se.softhouse.jargo.commands.Commit.Revision;
import se.softhouse.jargo.internal.Texts.ProgrammaticErrors;
import se.softhouse.jargo.internal.Texts.UserErrors;

/**
 * Tests for subclassing {@link Command}
 */
public class CommandTest
{
	/**
	 * Simulate the behavior of <a href="http://maven.apache.org/">maven</a>
	 * goals. Simple commands without any parameters.
	 */
	@Test
	public void testExecutingSeveralCommandsFromOneInvocation() throws ArgumentException
	{
		String[] arguments = {"clean", "build"};

		BuildTarget target = new BuildTarget();

		CommandLineParser.withCommands(new Build(target), new Clean(target)).parse(arguments);

		assertThat(target.isBuilt()).isTrue();
		assertThat(target.isClean()).isTrue();
	}

	@Test
	public void testMultipleCommandsEachWithSpecificArguments() throws ArgumentException
	{
		String[] logArgs = {"log", "--limit", "20"};
		String[] commitArgs = {"commit", "--amend", "--author=jjonsson", "A.java", "B.java"};

		Repository repo = new Repository();
		CommandLineParser parser = CommandLineParser.withCommands(new Commit(repo), new Log(repo));

		// Log
		parser.parse(logArgs);

		assertThat(repo.logLimit).isEqualTo(20);
		assertThat(repo.commits).isEmpty();

		repo.logLimit = 10;

		// Commit
		parser.parse(commitArgs);

		Revision commit = repo.commits.get(0);
		assertThat(commit.amend).isTrue();
		assertThat(commit.author).isEqualTo("jjonsson");
		assertThat(commit.files).isEqualTo(Arrays.asList(new File("A.java"), new File("B.java")));

		// Make sure that the parsing of the commit command didn't change the logLimit
		assertThat(repo.logLimit).isEqualTo(10);
	}

	/**
	 * Tests that the concept Maven has with multiple goals with the command concept from Git works
	 * in concert. Or simply put that multiple commands with command specific parameters can be
	 * given at the same time.
	 * As commit ends with a variable arity parameter (files to commit) that command has to be last.
	 */
	@Test
	public void testThatCombinedCommandsFromTheSameCommandLineBothAreExecuted() throws ArgumentException
	{
		String[] combinedInvocation = {"log", "--limit", "30", "commit", "--author=jjonsson"};
		Repository repo = new Repository();
		CommandLineParser parser = CommandLineParser.withCommands(new Commit(repo), new Log(repo));

		parser.parse(combinedInvocation);

		assertThat(repo.logLimit).isEqualTo(30);

		Revision commit = repo.commits.get(0);
		assertThat(commit.amend).isFalse();
		assertThat(commit.author).isEqualTo("jjonsson");
		assertThat(commit.files).isEqualTo(emptyList());
	}

	private static boolean didStart = false;

	/**
	 * An alternative to {@link Command} that is based on interfaces instead
	 */
	public enum Service implements Runnable,Describable
	{
		START
		{

	@Override
	public void run()
	{
		didStart = true;
	}

	@Override
	public String description()
	{
		return "Starts the service";
	}

	}}

	@Test
	public void testMapOfCommands() throws Exception
	{
		CommandLineParser parser = CommandLineParser.withCommands(Service.class);

		parser.parse("start");
		assertThat(didStart).isTrue();

		Usage usage = parser.usage();
		assertThat(usage).contains("start").contains("Starts the service");
	}

	static final Argument<ParsedArguments> COMMIT = command(new Commit(new Repository())).build();
	static final Argument<?> LOG = command(new Log(new Repository())).build();

	@Test
	public void testCommandWithMissingRequiredArgument()
	{
		try
		{
			COMMIT.parse("commit");// No author
			fail("--author=??? wasn't given in the input and it should have been required");
		}
		catch(ArgumentException expected)
		{
			assertThat(expected).hasMessage(String.format(UserErrors.MISSING_COMMAND_ARGUMENTS, "commit", "[--author]"));
		}
	}

	@Test
	public void testCommandWithRequiredArgumentAfterOtherArgs()
	{
		Repository repo = new Repository();
		CommandLineParser parser = CommandLineParser.withCommands(new Git(repo)).andArguments(Git.MESSAGE);
		ParsedArguments parsedArguments = parser.parse("git", "commit", "--amend", "--message", "b", "--author=a");

		assertThat(parsedArguments.get(Git.MESSAGE)).isEqualTo("b");
		Revision commit = repo.commits.get(0);
		assertThat(commit.amend).isTrue();
		assertThat(commit.author).isEqualTo("a");
		assertThat(commit.files).isEqualTo(emptyList());
	}

	@Test
	public void testThatUnhandledArgumentIsCaught()
	{
		String[] args = {"log", "-verbose", "commit"};
		try
		{
			CommandLineParser.withArguments(COMMIT, LOG).parse(args);
			fail("-verbose should have been reported as an unhandled argument");
		}
		catch(ArgumentException expected)
		{
			assertThat(expected).hasMessage("Unexpected argument: -verbose, previous argument: log");
		}
	}

	@Test
	public void testThatArgListWorksForCommand() throws Exception
	{
		CommandWithArguments command = new CommandWithArguments();
		CommandLineParser.withCommands(command).parse("main", "c");
		assertThat(CommandWithArguments.wasExecuted).isTrue();
	}

	@Test
	public void testThatRequiredArgumentsAreResetBetweenParsings() throws ArgumentException
	{
		String[] invalidArgs = {"commit"};
		String[] validArgs = {"commit", "--author=jjonsson"};

		// First make a successful parsing
		COMMIT.parse(validArgs);
		for(int i = 0; i < 2; i++)
		{
			try
			{
				// Then make sure that the previous --author didn't "stick"
				COMMIT.parse(invalidArgs);
				fail("--author=??? wasn't given in the input and it should have been required");
			}
			catch(ArgumentException expected)
			{
				assertThat(expected).hasMessage(String.format(UserErrors.MISSING_COMMAND_ARGUMENTS, "commit", "[--author]"));
			}
		}
	}

	@Test
	public void testThatRepeatedParsingsWithACommandParserDoesNotAffectEachOther() throws ArgumentException
	{
		String[] firstArgs = {"commit", "--author=jjonsson"};
		String[] secondArgs = {"commit", "--author=nobody"};

		Repository repo = new Repository();
		CommandLineParser parser = CommandLineParser.withCommands(new Commit(repo));

		parser.parse(firstArgs);
		parser.parse(secondArgs);

		assertThat(repo.commits.get(0).author).isEqualTo("jjonsson");
		assertThat(repo.commits.get(1).author).isEqualTo("nobody");
	}

	@Test
	public void testUsageForCommands()
	{
		BuildTarget target = new BuildTarget();
		CommandLineParser parser = CommandLineParser.withCommands(new Build(target), new Clean(target), new Commit(new Repository()));
		Usage usage = parser.usage();
		assertThat(usage).isEqualTo(expected("commandsWithArguments"));
	}

	@Test
	public void testCommandWithMissingIndexedArgument()
	{
		Argument<?> command = command(new CommandWithTwoIndexedArguments()).build();
		try
		{
			command.parse("two_args", "1");
			fail("two_args argument should require two parameters");
		}
		catch(ArgumentException missingSecondParameterForIndexedArgument)
		{
			assertThat(missingSecondParameterForIndexedArgument)
					.hasMessage(String.format(UserErrors.MISSING_NTH_PARAMETER, "second", "<integer>", "two_args"));
		}
	}

	@Test
	public void testThatTheInnerMostCommandIsPrintedInErrorMessage()
	{
		Argument<?> superCommand = command(new Command(command(new CommandWithTwoIndexedArguments()).build()){

			@Override
			protected String commandName()
			{
				return "superCommand";
			}

			@Override
			protected void execute(ParsedArguments parsedArguments)
			{
			}
		}).build();

		try
		{
			superCommand.parse("superCommand", "two_args", "1");
			fail("two_args argument should require two parameters");
		}
		catch(ArgumentException missingSecondParameterForIndexedArgument)
		{
			assertThat(missingSecondParameterForIndexedArgument)
					.hasMessage(String.format(UserErrors.MISSING_NTH_PARAMETER, "second", "<integer>", "two_args"));
		}
	}

	/**
	 * Tests several commands that each have their own indexed arguments
	 */
	@Test
	public void testMultipleCommandEachWithIndexedArguments() throws ArgumentException
	{
		List<Command> executedCommands = Lists.newLinkedList();
		CommandWithOneIndexedArgument first = new CommandWithOneIndexedArgument(executedCommands);
		CommandWithTwoIndexedArguments second = new CommandWithTwoIndexedArguments(executedCommands);
		CommandWithThreeIndexedArguments third = new CommandWithThreeIndexedArguments(executedCommands);

		CommandLineParser parser = CommandLineParser.withCommands(first, second, third);

		parser.parse("one_arg", "1", "two_args", "1", "2", "three_args", "1", "2", "3");
		assertThat(executedCommands).containsExactly(first, second, third);
	}

	@Test
	public void testThatCorrectCommandIsMentionedInErrorMessage()
	{
		List<Command> executedCommands = Lists.newLinkedList();
		CommandWithOneIndexedArgument first = new CommandWithOneIndexedArgument(executedCommands);
		CommandWithTwoIndexedArguments second = new CommandWithTwoIndexedArguments(executedCommands);
		CommandWithThreeIndexedArguments third = new CommandWithThreeIndexedArguments(executedCommands);

		CommandLineParser parser = CommandLineParser.withCommands(first, second, third);
		try
		{
			// Switched order of two_args and three_args for extra test harness
			parser.parse("one_arg", "1", "three_args", "1", "2", "3", "two_args", "1");
			fail("two_args should require two args");
		}
		catch(ArgumentException expected)
		{
			assertThat(expected).hasMessage("Missing second <integer> parameter for two_args");
			assertThat(executedCommands).isEmpty();
		}
	}

	@Test
	public void testThatParserForCommandArgumentsIsOnlyCreatedWhenCommandIsExecuted() throws ArgumentException
	{
		InvalidCommand profiler = new InvalidCommand();
		Argument<?> command = command(profiler).build();

		try
		{
			command.parse("profile");
			fail("Invalid arguments passed to super constructor in InvalidCommand should cause a lazy initialization error");
		}
		catch(IllegalArgumentException expected)
		{
			assertThat(expected).hasMessage("-n is handled by several arguments");
		}
	}

	@Test
	public void testThatCommandIsExecutedOnlyOnce() throws ArgumentException
	{
		ProfilingExecuteCommand profiler = new ProfilingExecuteCommand();
		Argument<?> command = command(profiler).build();

		assertThat(profiler.numberOfCallsToExecute).isZero();
		command.parse("profile");
		assertThat(profiler.numberOfCallsToExecute).isEqualTo(1);
	}

	@Test
	public void testRepeatedCommands() throws ArgumentException
	{
		Argument<List<ParsedArguments>> repeatedCommand = command(new Clean()).repeated().build();
		ParsedArguments parsedArguments = CommandLineParser.withArguments(repeatedCommand).parse("clean", "clean", "clean");
		assertThat(parsedArguments.get(repeatedCommand)).hasSize(3);
	}

	@Test
	public void testAddingCommandsInChainedFashion() throws ArgumentException
	{
		BuildTarget target = new BuildTarget();
		CommandLineParser.withArguments().andCommands(new Clean(target)).andCommands(new Build(target)).parse("clean", "build");
		assertThat(target.isClean()).isTrue();
		assertThat(target.isBuilt()).isTrue();
	}

	@Test
	public void testThatSuitableCommandArgumentAreSuggestedForMissspelling() throws Exception
	{
		try
		{
			CommandLineParser.withArguments(LOG).parse("log", "-limit");
			fail("-limit should be detected as being missspelled");
		}
		catch(ArgumentException expected)
		{
			assertThat(expected).hasMessage(String.format(UserErrors.SUGGESTION, "-limit", "--limit "));
		}
	}

	@Test
	public void testThatArgumentsToSubcommandsAreSuggested() throws Exception
	{
		try
		{
			CommandLineParser.withCommands(new CommandWithOneIndexedArgument()).parse("one_arg", "1", "cm");
			fail("cmd should be suggested for cm");
		}
		catch(ArgumentException expected)
		{
			assertThat(expected).hasMessage(String.format(UserErrors.SUGGESTION, "cm", "cmd "));
		}
	}

	@Test
	public void testThatMissspelledArgumentIsSuggestedForAlreadyParsedCommand() throws Exception
	{
		CommandLineParser parser = CommandLineParser.withCommands(new CommandWithOneIndexedArgument(), new CommandWithTwoIndexedArguments());
		try
		{
			// As one_arg already has been parsed, by the time two_args is seen,
			// suggesting --bool should be okay
			parser.parse("one_arg", "1", "two_args", "1", "2", "-boo");
			fail("-boo not detected as unhandled argument");
		}
		catch(ArgumentException expected)
		{
			assertThat(expected).hasMessage(String.format(UserErrors.SUGGESTION, "-boo", "--bool "));
		}

	}

	@Test
	public void testThatInvalidParameterStopsExecuteFromBeingCalled() throws Exception
	{
		ProfilingExecuteCommand profilingCommand = new ProfilingExecuteCommand();
		try
		{

			CommandLineParser.withCommands(profilingCommand).parse("profile", "-limit");
			fail("-limit should not be handled by profile command");
		}
		catch(ArgumentException expected)
		{
			assertThat(expected.getMessage()).startsWith("Unexpected argument: -limit");
			assertThat(profilingCommand.numberOfCallsToExecute).as("profile should not have been called as -limit should be an invalid argument: ")
					.isZero();
		}
	}

	@Test
	public void testThatMainCommandsAreExecutedBeforeSubcommands() throws Exception
	{
		List<Command> executedCommands = Lists.newLinkedList();
		ProfilingSubcommand profilingSubcommand = new ProfilingSubcommand(executedCommands);
		CommandLineParser parser = CommandLineParser.withCommands(profilingSubcommand);
		parser.parse("main", "c", "one_arg", "1");
		assertThat(executedCommands).containsExactly(profilingSubcommand, ProfilingSubcommand.subCommand);

		assertThat(parser.usage()).isEqualTo(expected("commandWithSubCommand"));
	}

	@Test
	public void testThatCommandArgumentsCanBeFetchedAfterExecution() throws Exception
	{
		ParsedArguments rootArgs = COMMIT.parse("commit", "--author=joj");

		assertThat(rootArgs.get(COMMIT).get(Commit.AUTHOR)).isEqualTo("joj");
	}

	@Test
	public void testThatDescriptionForCommandIsLazilyCreated() throws ArgumentException
	{
		FailInDescription failDescription = new FailInDescription();
		command(failDescription).parse("fail_description");
	}

	private static final class FailInDescription extends Command
	{
		@Override
		public String commandName()
		{
			return "fail_description";
		}

		@Override
		protected void execute(ParsedArguments parsedArguments)
		{
		}

		@Override
		public String description()
		{
			fail("Describable should only be called if usage is printed");
			return "Unreachable description";
		}
	}

	@Test
	public void testThatEndOfOptionsStopCommandsFromParsingArgumentNames() throws Exception
	{
		final Argument<String> option = stringArgument("--option").defaultValue("one").build();
		Command command = new Command(option){
			@Override
			protected String commandName()
			{
				return "command";
			}

			@Override
			protected void execute(ParsedArguments parsedArguments)
			{
				assertThat(parsedArguments.get(option)).isEqualTo("one");
			}
		};
		Argument<?> indexed = stringArgument().build();
		ParsedArguments result = CommandLineParser.withCommands(command).andArguments(indexed).parse("command", "--", "--option");
		assertThat(result.get(indexed)).isEqualTo("--option");
	}

	// This is what's tested
	@SuppressWarnings("deprecation")
	@Test
	public void testThatInvalidArgumentPropertiesOnCommandIsDeprecated()
	{
		CommandBuilder builder = command(new Build());
		try
		{
			builder.arity(2);
			fail("method should throw as it's deprecated");
		}
		catch(IllegalStateException expected)
		{
		}
		try
		{
			builder.defaultValue(null);
			fail("method should throw as it's deprecated");
		}
		catch(IllegalStateException expected)
		{
		}
		try
		{
			builder.defaultValueDescriber(Describers.toStringDescriber());
			fail("method should throw as it's deprecated");
		}
		catch(IllegalStateException expected)
		{
		}
		try
		{
			builder.defaultValueSupplier(Suppliers2.ofInstance(null));
			fail("method should throw as it's deprecated");
		}
		catch(IllegalStateException expected)
		{
		}
		try
		{
			builder.defaultValueDescription("");
			fail("method should throw as it's deprecated");
		}
		catch(IllegalStateException expected)
		{
		}
		try
		{
			builder.limitTo(Predicates2.alwaysFalse());
			fail("method should throw as it's deprecated");
		}
		catch(IllegalStateException expected)
		{
		}
		try
		{
			builder.required();
			fail("method should throw as it's deprecated");
		}
		catch(IllegalStateException expected)
		{
		}
		try
		{
			builder.splitWith("-");
			fail("method should throw as it's deprecated");
		}
		catch(IllegalStateException expected)
		{
		}
		try
		{
			builder.variableArity();
			fail("method should throw as it's deprecated");
		}
		catch(IllegalStateException expected)
		{
		}
	}

	public static Argument<String> MAIN_ARG = Arguments.stringArgument("-t").build();

	@Test
	public void testThatSubcommandsCanAccessArgumentsToMainCommandLineParser() throws Exception
	{
		SubcommandAccessingParentArgument subCommand = new SubcommandAccessingParentArgument();
		CommandLineParser.withArguments(MAIN_ARG).andCommands(subCommand).parse("-t", "test", "sub");
		assertThat(subCommand.mainArg).isEqualTo("test");
	}

	@Test
	public void testThatSubcommandsCanAccessArgumentsToParentCommand() throws Exception
	{
		SubcommandAccessingParentArgument subCommand = new SubcommandAccessingParentArgument();
		List<Argument<?>> subCommandsOrArgs = Command.subCommands(subCommand);
		subCommandsOrArgs.add(MAIN_ARG);
		Command mainCommand = new Command(subCommandsOrArgs){
			@Override
			protected void execute(ParsedArguments parsedArguments)
			{

			}

			@Override
			protected String commandName()
			{
				return "main";
			}
		};

		CommandLineParser.withCommands(mainCommand).parse("main", "-t", "test", "sub");
		assertThat(subCommand.mainArg).isEqualTo("test");
	}

	@Test
	public void testThatInvalidParameterStopsEarlierCommandsFromBeingExecuted() throws Exception
	{
		ProfilingExecuteCommand profilingCommand = new ProfilingExecuteCommand();
		try
		{
			CommandLineParser.withArguments(command(profilingCommand).repeated().build()).parse("profile", "profile", "-limit");
			fail("-limit should not be handled by profile command");
		}
		catch(ArgumentException expected)
		{
			assertThat(expected.getMessage()).startsWith("Unexpected argument: -limit");
			assertThat(profilingCommand.numberOfCallsToExecute)
					.as("profile should not have been called since either all commands should be executed or none").isZero();
		}
	}

	@Test
	public void testThatMissingArgsForMainStopsSubSubcommandFromBeingExecuted() throws Exception
	{
		Argument<String> mainArg = Arguments.stringArgument("-a").required().build();
		ProfilingExecuteCommand subcommand = new ProfilingExecuteCommand();
		ProfilingExecuteCommand command = new ProfilingExecuteCommand(Command.subCommands(subcommand));

		try
		{
			CommandLineParser.withArguments(mainArg).andCommands(command).parse("profile", "profile");
			fail("Missing arg not detected");
		}
		catch(ArgumentException expected)
		{
			assertThat(subcommand.numberOfCallsToExecute).isZero();
		}
	}

	@Test
	public void testArgumentVisibilityBetweenMainArgAndSubArgAndThatRepeatedValuesAreRejected() throws Exception
	{
		SubcommandAccessingParentArgument subCommand = new SubcommandAccessingParentArgument();
		List<Argument<?>> subCommandsOrArgs = Command.subCommands(subCommand);
		subCommandsOrArgs.add(MAIN_ARG);
		AtomicReference<ParsedArguments> mainCommandParsedArguments = new AtomicReference<ParsedArguments>(null);
		Command mainCommand = new Command(subCommandsOrArgs){
			@Override
			protected void execute(ParsedArguments parsedArguments)
			{
				mainCommandParsedArguments.set(parsedArguments);
			}

			@Override
			protected String commandName()
			{
				return "main";
			}
		};

		CommandLineParser parser = CommandLineParser.withArguments(MAIN_ARG).andCommands(mainCommand);
		ParsedArguments parsedArguments = parser.parse("main", "-t", "1", "sub");
		assertThat(subCommand.mainArg).isEqualTo("1");
		assertThat(mainCommandParsedArguments.get().get(MAIN_ARG)).isEqualTo("1");
		// The root command does not intersect with multiple levels of commands
		assertThat(parsedArguments.get(MAIN_ARG)).isEqualTo("");

		// Test that default values for main arguments work
		parsedArguments = parser.parse("main", "sub");
		assertThat(subCommand.mainArg).isEqualTo("");

		parsedArguments = parser.parse("main", "sub", "-t", "2");
		assertThat(subCommand.mainArg).isEqualTo("2");
		// argument needs to be repeated for each subcommand on the same level
		assertThat(parsedArguments.get(MAIN_ARG)).isEqualTo("");

		try
		{
			parsedArguments = parser.parse("main", "-t", "1", "sub", "-t", "2");
			fail("-t is not repeated. Should not be allowed");
		}
		catch(ArgumentException expected)
		{
			assertThat(expected).hasMessage(String.format(UserErrors.DISALLOWED_REPETITION, "-t"));
		}

		try
		{
			parser = CommandLineParser.withCommands(mainCommand);
			parser.parse("main", "sub", "-t", "2").get(MAIN_ARG);
			fail("MAIN_ARG is not part of the root args, so it shouldn't be visible there");
		}
		catch(IllegalArgumentException expected)
		{
			assertThat(expected).hasMessage(String.format(ProgrammaticErrors.ILLEGAL_ARGUMENT, "-t"));
		}
	}

	@Test
	public void testThatTwoSubcommandsCanUseTheSameArgumentNameAndGetDifferentValues() throws Exception
	{
		Argument<String> commonArg = Arguments.stringArgument("-t").build();
		CommandWithArgument<String> commandOne = new CommandWithArgument<>("command-one", commonArg);
		CommandWithArgument<String> commandTwo = new CommandWithArgument<String>("command-two", commonArg);
		CommandLineParser parser = CommandLineParser.withCommands(commandOne, commandTwo);
		parser.parse("command-one", "-t", "1", "command-two", "-t", "2");
		assertThat(commandOne.parsedObject).isEqualTo("1");
		assertThat(commandTwo.parsedObject).isEqualTo("2");
	}

	@Test
	public void testThatCorrectNameAppearsForMissingArgToCommand() throws Exception
	{
		Argument<String> firstArg = Arguments.stringArgument("-s").build();
		Argument<String> secondArg = Arguments.stringArgument("-t").required().build();
		CommandWithTwoArguments<String, String> command = new CommandWithTwoArguments<>("command", firstArg, secondArg);
		CommandLineParser parser = CommandLineParser.withCommands(command);
		try
		{
			parser.parse("command", "-s", "hello");
			fail("-t should be reported as missing");
		}
		catch(ArgumentException expected)
		{
			assertThat(expected).hasMessage(String.format(UserErrors.MISSING_COMMAND_ARGUMENTS, "command", "[-t]"));
		}
	}

	@Test
	public void testThatCorrectNameAppearsForMissingIndexedArgToCommand() throws Exception
	{
		Argument<String> firstArg = Arguments.stringArgument("-s").build();
		Argument<String> secondArg = Arguments.stringArgument().required().build();
		CommandWithTwoArguments<String, String> command = new CommandWithTwoArguments<>("command", firstArg, secondArg);
		CommandLineParser parser = CommandLineParser.withCommands(command);
		try
		{
			parser.parse("command", "-s", "hello");
			fail("secondArg should be reported as missing");
		}
		catch(ArgumentException expected)
		{
			assertThat(expected).hasMessage(String.format(UserErrors.MISSING_COMMAND_ARGUMENTS, "command", "[<string>]"));
		}
	}

	@Test
	public void testThatMainArgsCanBeSpecifiedInTheMiddleOfCommandArguments() throws Exception
	{
		Argument<String> mainFirstArg = Arguments.stringArgument("--main").build();
		Argument<String> mainSecondArg = Arguments.stringArgument("-m").build();

		Argument<String> firstCommandArg = Arguments.stringArgument("--sub").build();
		Argument<String> secondCommandArg = Arguments.stringArgument("-s").build();
		CommandWithTwoArguments<String, String> command = new CommandWithTwoArguments<>("command", firstCommandArg, secondCommandArg);
		CommandLineParser parser = CommandLineParser.withArguments(mainFirstArg, mainSecondArg).andCommands(command);
		ParsedArguments parsedArguments = parser.parse("command", "--main", "1", "--sub", "2", "-m", "3", "-s", "4");
		assertThat(command.parsedObject).isEqualTo("2");
		assertThat(command.parsedObjectTwo).isEqualTo("4");
		assertThat(parsedArguments.get(mainFirstArg)).isEqualTo("1");
		assertThat(parsedArguments.get(mainSecondArg)).isEqualTo("3");
	}

	@Test
	public void testThatSubcommandsAreSuggested() throws Exception
	{
		Repository repo = new Repository();
		CommandLineParser git = CommandLineParser.withCommands(new Git(repo));
		try
		{
			git.parse("git", "lo");
			fail("log should be suggested");
		}
		catch(ArgumentException expected)
		{
			assertThat(expected).hasMessage(String.format(UserErrors.SUGGESTION, "lo", "log "));
		}
	}

	@Test
	public void testThatCompletingWorksForIgnoreCaseArgumentsForCommands() throws Exception
	{
		Argument<String> name = stringArgument("--name").ignoreCase().build();
		CommandWithArgument<String> command = new CommandWithArgument<>("cmd", name);
		CommandLineParser parser = CommandLineParser.withCommands(command);

		SortedSet<String> suggestions = FakeCompleter.complete(parser, "cmd", "--Na");
		assertThat(suggestions).containsOnly("--Name ");
	}

	@Test
	public void testThatDefaultValuesForMainArgsCanBeAccessedFromSubSubcommand() throws Exception
	{
		Repository repo = new Repository();
		CommandLineParser git = CommandLineParser.withCommands(new Git(repo)).andArguments(Git.MESSAGE);
		git.parse("git", "merge", "--author=merge-person@company.org");
		assertThat(repo.commits).hasSize(1);
		assertThat(repo.commits.get(0).message).isEqualTo("");
	}

	@Test
	public void testThatLimiterErrorMessageLooksGoodForCommandArguments() throws Exception
	{
		Argument<String> likesP = stringArgument("-p").limitTo(s -> s.contains("p"), "something with p").defaultValue("pppp").build();
		CommandWithArgument<String> command = new CommandWithArgument<>("limit", likesP);
		CommandLineParser parser = CommandLineParser.withCommands(command);
		try
		{
			parser.parse("limit", "-p", "no");
			fail("no does not contain p, limiter did not detect it");
		}
		catch(ArgumentException expected)
		{
			assertThat(expected).hasMessage(String.format(UserErrors.DISALLOWED_VALUE, "no", "something with p"));
		}
	}

	@Test
	public void testThatRepeatableWorksWithCommands() throws Exception
	{
		ProfilingExecuteCommand repeatable = new ProfilingExecuteCommand();
		ProfilingExecuteCommand notRepeatable = new ProfilingExecuteCommand();
		CommandLineParser repeater = CommandLineParser.withArguments(	Arguments.command(repeatable).repeated().build(),
																		Arguments.command(notRepeatable).names("-p").build());
		repeater.parse("profile", "profile", "-p");
		assertThat(repeatable.numberOfCallsToExecute).isEqualTo(2);
		assertThat(notRepeatable.numberOfCallsToExecute).isEqualTo(1);

		repeatable.numberOfCallsToExecute = 0;
		notRepeatable.numberOfCallsToExecute = 0;
		try
		{
			repeater.parse("profile", "profile", "-p", "-p");
			fail("-p should not be allowed to be repeated");
		}
		catch(ArgumentException expected)
		{
			assertThat(expected).hasMessage(String.format(UserErrors.DISALLOWED_REPETITION, "-p"));
			assertThat(repeatable.numberOfCallsToExecute).isEqualTo(0);
			assertThat(notRepeatable.numberOfCallsToExecute).isEqualTo(0);

		}
	}

	@Test
	public void testThatThrownArgumentExceptionsFromExecuteHasUsageAttachedToThem() throws Exception
	{
		try
		{
			CommandLineParser.withCommands(new Command(){

				@Override
				protected void execute(ParsedArguments parsedArguments)
				{
					throw ArgumentExceptions.withMessage("Catch me");
				}

				@Override
				protected String commandName()
				{
					return "catcher";
				}
			}).parse("catcher");
			fail("Thrown exception was suppressed");
		}
		catch(ArgumentException expected)
		{
			assertThat(expected.getMessageAndUsage()).isEqualTo(expected("exceptionFromCommand"));
		}
	}
}
