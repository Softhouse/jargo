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
package se.softhouse.jargo.exceptions;

import static org.fest.assertions.Assertions.assertThat;
import static org.junit.Assert.fail;
import static se.softhouse.jargo.ArgumentExceptions.withMessage;
import static se.softhouse.jargo.Arguments.integerArgument;
import static se.softhouse.jargo.Arguments.stringArgument;
import static se.softhouse.jargo.utils.Assertions2.assertThat;

import org.junit.Test;

import se.softhouse.common.strings.Describable;
import se.softhouse.common.strings.Describables;
import se.softhouse.common.testlib.Serializer;
import se.softhouse.jargo.Argument;
import se.softhouse.jargo.ArgumentException;
import se.softhouse.jargo.ArgumentExceptions;
import se.softhouse.jargo.CommandLineParser;
import se.softhouse.jargo.internal.Texts.UsageTexts;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Tests for {@link ArgumentException} and {@link ArgumentExceptions}
 */
public class ArgumentExceptionTest
{
	@Test
	public void testThatErrorMessagePointsOutWhereToReadAboutProperValuesWhenMessageIsPrintedTogetherWithUsage() throws Exception
	{
		try
		{
			integerArgument("-n").parse("-n", "foo");
			fail("foo should not be a valid integer");
		}
		catch(ArgumentException expected)
		{
			assertThat(expected.getMessageAndUsage()).contains(String.format(UsageTexts.USAGE_REFERENCE, "-n"));
			assertThat(expected.getMessage()).doesNotContain(String.format(UsageTexts.USAGE_REFERENCE, "-n"));
		}
	}

	@Test
	@SuppressFBWarnings(value = "RV_RETURN_VALUE_IGNORED", justification = "Testing that invocation doesn't call description")
	public void testThatDescriptionIsNotCalledWhenNotNeeded()
	{
		withMessage(new Describable(){
			@Override
			public String description()
			{
				fail("description Should not be called as no getMessage is called");
				return "";
			}
		});
	}

	@Test
	@SuppressFBWarnings(value = "RV_RETURN_VALUE_IGNORED", justification = "Testing that argument exceptions lazily loads the description")
	public void testThatToStringIsNotRunWhenItIsNotNeeded()
	{
		withMessage(Describables.format("%s", new FailingToString()));
	}

	@Test
	public void testThatCauseIsSetWithMessage()
	{
		Throwable cause = new Error();
		assertThat(withMessage("", cause).getCause()).isEqualTo(cause);
	}

	@Test
	public void testThatToStringIsUsedByGetMessage()
	{
		assertThat(withMessage(new Object(){
			@Override
			public String toString()
			{
				return "foobar";
			}
		})).hasMessage("foobar");
	}

	@Test
	public void testThatUsageIsAvailableAfterSerialization()
	{
		Argument<Integer> number = integerArgument("-n").build();
		Argument<String> string = stringArgument("-s").build();

		try
		{
			CommandLineParser.withArguments(number, string).parse("-n");
			fail("-n argument should require an integer parameter");
		}
		catch(ArgumentException expected)
		{
			String usageBeforeSerialization = expected.getMessageAndUsage().toString();
			ArgumentException revivedException = Serializer.clone(expected);
			assertThat(revivedException.getMessageAndUsage()).isEqualTo(usageBeforeSerialization);
		}
	}

	@Test
	public void testThatProgramNameIsSerialized()
	{
		Argument<Integer> number = integerArgument("-n").build();
		try
		{
			CommandLineParser.withArguments(number).programName("MyProgram").parse("-n");
			fail("-n argument should require an integer parameter");
		}
		catch(ArgumentException expected)
		{
			ArgumentException revivedException = Serializer.clone(expected);
			assertThat(revivedException.getMessageAndUsage()).contains("MyProgram");
		}
	}

	@Test
	public void testThatProgramDescriptionIsSerialized()
	{
		Argument<Integer> number = integerArgument("-n").build();
		try
		{
			CommandLineParser.withArguments(number).programDescription("MyDescription").parse("-n");
			fail("-n argument should require an integer parameter");
		}
		catch(ArgumentException expected)
		{
			ArgumentException revivedException = Serializer.clone(expected);
			assertThat(revivedException.getMessageAndUsage()).contains("MyDescription");
		}
	}

	@Test
	@SuppressFBWarnings(value = "RV_RETURN_VALUE_IGNORED", justification = "Testing that argument exceptions lazily loads the toString")
	public void testThatWithMessageDoesNotRunToStringWhenItIsNotNeeded()
	{
		withMessage(new FailingToString());
	}

	private static final class FailingToString
	{
		@Override
		public String toString()
		{
			fail("toString Should not be called as no getMessage is called");
			return "";
		}
	}
}
