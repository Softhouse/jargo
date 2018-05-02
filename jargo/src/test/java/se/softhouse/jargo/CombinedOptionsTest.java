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

import static org.fest.assertions.Assertions.assertThat;
import static se.softhouse.jargo.Arguments.command;
import static se.softhouse.jargo.Arguments.integerArgument;
import static se.softhouse.jargo.Arguments.optionArgument;

import org.junit.Test;

/**
 * Tests for a batch of short-named optional arguments.
 * For instance when "-fs" is used instead of "-f -s"
 */
public class CombinedOptionsTest
{
	@Test
	public void testSeveralOptionsInOneArgument() throws ArgumentException
	{
		Argument<Boolean> logging = optionArgument("-l").build();
		Argument<Boolean> promiscousMode = optionArgument("-p").build();

		ParsedArguments args = CommandLineParser.withArguments(logging, promiscousMode).parse("-pl");

		assertThat(args.get(logging)).isTrue();
		assertThat(args.get(promiscousMode)).isTrue();
	}

	@Test(expected = ArgumentException.class)
	public void testSeveralOptionsInOneArgumentWithOneNonOptionArgument() throws ArgumentException
	{
		Argument<Boolean> logging = optionArgument("-l").build();
		Argument<Integer> number = integerArgument("-n").build();

		CommandLineParser.withArguments(logging, number).parse("-ln");
	}

	@Test(expected = ArgumentException.class)
	public void testSeveralOptionsInOneArgumentWithOneDuplicate() throws ArgumentException
	{
		Argument<Boolean> logging = optionArgument("-l").build();
		Argument<Boolean> promiscousMode = optionArgument("-p").build();

		CommandLineParser.withArguments(logging, promiscousMode).parse("-ppl");
	}

	@Test(expected = ArgumentException.class)
	public void testSeveralOptionsInOneArgumentWithOneUnknownCharacter() throws ArgumentException
	{
		optionArgument("-l").parse("-pl");
	}

	@Test(expected = ArgumentException.class)
	public void testThatOnlyAHyphenDoesNotMatchAnyOptions() throws ArgumentException
	{
		optionArgument("-l").parse("-");
	}

	private static Argument<Boolean> subOption = optionArgument("-l").build();

	@Test
	public void testThatNoOptionArgumentIsParsedWhenOneCharacterIsUnhandled() throws ArgumentException
	{
		Argument<Integer> programArgument = integerArgument("-lp").build();
		Argument<?> command = command(new Command(subOption){

			@Override
			protected void execute(ParsedArguments args)
			{
				assertThat(args.get(subOption)).as("Invocation with '-pl' lead to '-l' being parsed").isFalse();
			}

			@Override
			protected String commandName()
			{
				return "test";
			}
		}).build();

		ParsedArguments args = CommandLineParser.withArguments(programArgument, command).parse("test", "-lp", "1");

		// Also verify that the argument is parsed by it's rightful recipient
		assertThat(args.get(programArgument)).isEqualTo(1);
	}
}
