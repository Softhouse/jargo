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

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.junit.Test;
import se.softhouse.common.testlib.Explanation;
import se.softhouse.jargo.internal.Texts.ProgrammaticErrors;
import se.softhouse.jargo.internal.Texts.UserErrors;

import static org.fest.assertions.Assertions.*;
import static org.junit.Assert.fail;
import static se.softhouse.jargo.Arguments.integerArgument;
import static se.softhouse.jargo.Arguments.optionArgument;

/**
 * Tests for {@link ArgumentBuilder#required()}
 */
public class RequiredArgumentTest
{
	@Test
	public void testMissingRequiredNamedArgument()
	{
		// Only --number should be displayed in usage
		Argument<Integer> number = integerArgument("--number", "-n").required().build();

		Argument<Integer> number2 = integerArgument("--number2").required().build();

		try
		{
			CommandLineParser.withArguments(number, number2).parse();
			fail("Required argument silently ignored");
		}
		catch(ArgumentException expected)
		{
			assertThat(expected).hasMessage(String.format(UserErrors.MISSING_REQUIRED_ARGUMENTS, "[--number, --number2]"));
		}
	}

	@Test
	public void testMissingRequiredIndexedArgument()
	{
		Argument<Integer> number = integerArgument().metaDescription("numberOne").required().build();
		Argument<Integer> number2 = integerArgument().metaDescription("numberTwo").required().build();

		try
		{
			CommandLineParser.withArguments(number, number2).parse();
			fail("Required argument silently ignored");
		}
		catch(ArgumentException e)
		{
			assertThat(e).hasMessage(String.format(UserErrors.MISSING_REQUIRED_ARGUMENTS, "[numberOne, numberTwo]"));
		}
	}

	@Test(expected = ArgumentException.class)
	public void testThatRequiredArgumentsIsResetBetweenParsings() throws ArgumentException
	{
		Argument<Integer> required = integerArgument("-n").required().build();
		try
		{
			required.parse("-n", "1");
		}
		catch(ArgumentException e)
		{
			fail("Parser failed to handle required argument");
		}
		required.parse(); // The second time shouldn't be affected by the first
	}

	@SuppressWarnings("deprecation")
	// This is what's tested
	@Test(expected = IllegalStateException.class)
	public void testMakingAnOptionalArgumentRequired()
	{
		optionArgument("-l").required();
	}

	@Test
	@SuppressFBWarnings(value = "RV_RETURN_VALUE_IGNORED", justification = Explanation.FAIL_FAST)
	public void testThatSettingADefaultValueForARequiredArgumentIsForbidden()
	{
		try
		{
			integerArgument("-l").required().defaultValue(42);
			fail("setting a default value on a required argument should be forbidden");
		}
		catch(IllegalArgumentException expected)
		{
			assertThat(expected).hasMessage(ProgrammaticErrors.DEFAULT_VALUE_AND_REQUIRED);
		}
	}

	@Test(expected = IllegalArgumentException.class)
	@SuppressFBWarnings(value = "RV_RETURN_VALUE_IGNORED", justification = Explanation.FAIL_FAST)
	public void testMakingARequiredArgumentWithDefaultValue()
	{
		integerArgument("-l").defaultValue(42).required();
	}
}
