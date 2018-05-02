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
package se.softhouse.jargo.stringparsers;

import static java.lang.String.format;
import static org.fest.assertions.Assertions.assertThat;
import static org.junit.Assert.fail;
import static se.softhouse.jargo.Arguments.helpArgument;
import static se.softhouse.jargo.Arguments.integerArgument;
import static se.softhouse.jargo.Arguments.stringArgument;
import static se.softhouse.jargo.utils.Assertions2.assertThat;
import static se.softhouse.jargo.utils.ExpectedTexts.expected;

import java.util.Arrays;

import org.junit.Test;

import se.softhouse.jargo.Argument;
import se.softhouse.jargo.ArgumentException;
import se.softhouse.jargo.Arguments;
import se.softhouse.jargo.CommandLineParser;
import se.softhouse.jargo.commands.Build;
import se.softhouse.jargo.commands.CommandWithOneIndexedArgument;
import se.softhouse.jargo.internal.Texts.UsageTexts;
import se.softhouse.jargo.internal.Texts.UserErrors;

/**
 * Tests for {@link Arguments#helpArgument(String, String...)}
 */
public class HelpArgumentTest
{
	private static final Argument<String> STRING = stringArgument().build();
	private static final Argument<Integer> NUMBER = integerArgument("--number").build();
	private static final Argument<?> HELP = helpArgument("-h");

	@Test
	public void testThatHelpPrintsUsageByThrowing()
	{
		try
		{
			CommandLineParser.withArguments(NUMBER, HELP).parse("-h");
			fail("help argument should trigger exception");
		}
		catch(ArgumentException expected)
		{
			assertThat(expected.getMessageAndUsage()).isEqualTo(expected("helpArgument"));
		}
	}

	@Test
	public void testThatUnknownArgumentToHelpThrows()
	{
		try
		{
			CommandLineParser.withArguments(HELP).parse("-h", "--foo-bar");
			fail("--foo-bar should trigger exception as it's unknown");
		}
		catch(ArgumentException expected)
		{
			assertThat(expected).hasMessage(format(UserErrors.UNKNOWN_ARGUMENT, "--foo-bar"));
		}
	}

	@Test
	public void testThatHelpOnlyPrintsUsageForRequestedArgument()
	{
		try
		{
			CommandLineParser.withCommands(new Build()).andArguments(HELP, NUMBER).parse("-h", "build");
			fail("help argument should trigger exception");
		}
		catch(ArgumentException expected)
		{
			String usageReference = "Help requested with -h" + String.format(UsageTexts.USAGE_REFERENCE, "build");
			assertThat(expected.getMessageAndUsage()) //
					.startsWith(usageReference) //
					.contains("Builds a target") //
					.doesNotContain("--number");
		}
	}

	@Test
	public void testThatHelpOnlyPrintsUsageForRequestedCommandArgument()
	{
		try
		{
			CommandLineParser.withCommands(new CommandWithOneIndexedArgument()).andArguments(HELP, STRING).parse("one_arg", "-h", "--bool");
			fail("help argument should trigger exception");
		}
		catch(ArgumentException expected)
		{
			String usageReference = "Help requested with -h. Usage for --bool (argument to one_arg):";
			assertThat(expected.getMessageAndUsage()) //
					.startsWith(usageReference) //
					.contains("--bool <boolean>") //
					.doesNotContain("<string>") //
					.doesNotContain("<integer>");
		}
	}

	@Test
	public void testThatHelpPrintsUsageForAllCommandArguments()
	{
		try
		{
			CommandLineParser.withCommands(new CommandWithOneIndexedArgument()).andArguments(HELP, STRING).parse("one_arg", "-h");
			fail("help argument should trigger exception");
		}
		catch(ArgumentException expected)
		{
			assertThat(expected.getMessageAndUsage()) //
					.startsWith("Help requested with -h. See usage for one_arg below:") //
					.contains("cmd") //
					.contains("--bool") //
					.contains("<integer>") //
					.doesNotContain("<string>");
		}
	}

	@Test
	public void testThatHelpCanPrintHelpAboutHelp()
	{
		try
		{
			CommandLineParser.withArguments(HELP, STRING).parse("-h", "-h");
			fail("help argument should trigger exception");
		}
		catch(ArgumentException expected)
		{
			assertThat(expected.getMessageAndUsage()).contains("-h <argument-to-print-help-for>");
		}
	}

	@Test
	public void testThatSeveralHelpArgumentsCanBeUsed()
	{
		CommandLineParser parser = CommandLineParser.withArguments(HELP, STRING, helpArgument("--help"));
		for(String helpName : Arrays.asList("-h", "--help"))
		{
			try
			{
				parser.parse(helpName);
				fail("help argument should trigger exception");
			}
			catch(ArgumentException expected)
			{
				assertThat(expected.getMessage()).startsWith("Help requested with " + helpName);
			}
		}
	}

	@Test
	public void testThatOneCommandParserCanProvideHelpWhileAnotherDoesNot()
	{
		CommandWithOneIndexedArgument command = new CommandWithOneIndexedArgument();
		CommandLineParser withoutHelp = CommandLineParser.withCommands(command);
		CommandLineParser withHelp = CommandLineParser.withCommands(command).andArguments(HELP);
		try
		{
			withHelp.parse("one_arg", "-h");
			fail("help argument should trigger exception with help message");
		}
		catch(ArgumentException expected)
		{
			assertThat(expected.getMessage()).startsWith("Help requested");
		}
		try
		{
			withoutHelp.parse("one_arg", "-h");
			fail("help argument should trigger an unknown argument exception");
		}
		catch(ArgumentException expected)
		{
			// Without -h as a help argument it should be treated as an indexed parameter to command
			assertThat(expected.getMessage()).contains("is not a valid integer");
		}
	}

	@Test
	public void testThatNameCollisionsForHelpArgumentAndOtherArgumentsAreDetected()
	{
		try
		{
			CommandLineParser.withArguments(HELP, integerArgument("-h").build());
			fail("Should not bind -h to two different arguments");
		}
		catch(IllegalArgumentException expected)
		{
			assertThat(expected.getMessage()).isEqualTo("-h is handled by several arguments");
		}
	}
}
