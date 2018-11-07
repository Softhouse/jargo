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
import static org.fest.assertions.Fail.fail;
import static se.softhouse.jargo.Arguments.booleanArgument;
import static se.softhouse.jargo.Arguments.enumArgument;
import static se.softhouse.jargo.Arguments.integerArgument;
import static se.softhouse.jargo.Arguments.stringArgument;
import static se.softhouse.jargo.Arguments.withParser;
import static se.softhouse.jargo.StringParsers.stringParser;

import java.util.List;
import java.util.Locale;
import java.util.SortedSet;

import org.junit.Test;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import se.softhouse.common.testlib.Explanation;
import se.softhouse.jargo.internal.Texts.ProgrammaticErrors;
import se.softhouse.jargo.stringparsers.EnumArgumentTest.Action;

/**
 * <pre>
 * An example of how to create a <b>hard to understand</b> command line invocation:
 * java program true 8090 Hello
 * Note that the order of the arguments matter and who knows what true
 * means? These are called indexed arguments in argumentparser.
 * 
 * This feature is allowed only because there exists some use cases
 * where indexed arguments makes sense, one example is:<br>
 * echo "Hello World"
 * </pre>
 */
public class IndexedArgumentTest
{
	Argument<Boolean> enableLogging = booleanArgument().description("Output debug information to standard out").build();

	Argument<Integer> port = integerArgument().defaultValue(8080).description("The port to start the server on.").build();

	Argument<String> greetingPhrase = stringArgument().description("A greeting phrase to greet new connections with").build();

	@Test
	public void testIndexedArguments() throws ArgumentException
	{
		String[] args = {"true", "8090", "Hello"};

		ParsedArguments arguments = CommandLineParser.withArguments(enableLogging, port, greetingPhrase).parse(args);

		assertThat(arguments.get(enableLogging)).isTrue();
		assertThat(arguments.get(port)).isEqualTo(8090);
		assertThat(arguments.get(greetingPhrase)).isEqualTo("Hello");
	}

	@Test
	public void testThatOnlyCurrentlyIndexedArgumentIsSuggested() throws Exception
	{
		Argument<Action> action = enumArgument(Action.class).description("Output debug information to standard out").build();

		CommandLineParser parser = CommandLineParser.withArguments(port, greetingPhrase, enableLogging, action);

		SortedSet<String> suggestions = FakeCompleter.complete(parser, "8080", "Hello!", "");
		assertThat(suggestions).containsOnly("false", "true");

		suggestions = FakeCompleter.complete(parser, "8080", "Hello!", "true", "");
		assertThat(suggestions).containsOnly("start", "stop", "restart");
	}

	@Test
	public void testThatSeveralIndexedArgumentsCanBeFinalized() throws Exception
	{
		Argument<List<Action>> firstActions = enumArgument(Action.class).arity(2).description("first").build();
		Argument<List<Action>> secondActions = enumArgument(Action.class).arity(2).description("second").build();

		CommandLineParser parser = CommandLineParser.withArguments(firstActions, secondActions);

		ParsedArguments parsedArguments = parser.parse("start", "stop", "stop", "start");
		assertThat(parsedArguments.get(firstActions)).containsExactly(Action.start, Action.stop);
		assertThat(parsedArguments.get(secondActions)).containsExactly(Action.stop, Action.start);
		SortedSet<String> suggestions = FakeCompleter.complete(parser, "start", "stop", "stop", "sta");
		assertThat(suggestions).containsOnly("start");
	}

	@Test
	@SuppressFBWarnings(value = "RV_RETURN_VALUE_IGNORED", justification = Explanation.FAIL_FAST)
	public void testThatIndexedArgumentThatIsRequiredIsGivenFirstBeforeAnyOptionalIndexedArguments()
	{
		// string is required but the integer before it is optional,
		// this is a fault because indexed & required arguments should be given first
		// and lastly indexed & optional arguments can be given
		Argument<Integer> integer = integerArgument().build();
		Argument<String> string = stringArgument().required().build();
		Argument<Integer> integerTwo = integerArgument().build();
		try
		{
			CommandLineParser.withArguments(integer, string, integerTwo);
			fail("string should have been forced to be placed before integer");
		}
		catch(IllegalArgumentException e)
		{
			assertThat(e).hasMessage(String.format(ProgrammaticErrors.REQUIRED_ARGUMENTS_BEFORE_OPTIONAL, 0, 1));
		}
	}

	@Test
	@SuppressFBWarnings(value = "RV_RETURN_VALUE_IGNORED", justification = Explanation.FAIL_FAST)
	public void testThatRequiredIndexedArgumentsHaveUniqueMetaDescriptions()
	{
		Argument<Integer> portArg = integerArgument().required().build();
		Argument<Integer> number = integerArgument().required().build();

		try
		{
			CommandLineParser.withArguments(portArg, number);
			fail("Non-unique meta description not detected");
		}
		catch(IllegalArgumentException expected)
		{
			assertThat(expected).hasMessage(String.format(ProgrammaticErrors.UNIQUE_METAS, portArg.metaDescriptionInRightColumn()));
		}
	}

	@Test
	public void testThatMetaDescriptionIsGivenForIndexedArgumentInExceptions()
	{
		try
		{
			withParser(new ForwardingStringParser.SimpleForwardingStringParser<String>(stringParser()){
				@Override
				public String parse(String value, Locale locale) throws ArgumentException
				{
					throw new UsedArgumentAsMessageException();
				}
			}).parse("");
			fail("exception should have been thrown from parse");
		}
		catch(ArgumentException expected)
		{
			// As indexed argument doesn't have names their
			// meta description is the best string the exception has to offer
			assertThat(expected.getMessage()).isEqualTo("<string>");
		}
	}

	private static final class UsedArgumentAsMessageException extends ArgumentException
	{
		private static final long serialVersionUID = 1L;

		@Override
		protected String getMessage(String referenceName)
		{
			return referenceName;
		}
	}
}
