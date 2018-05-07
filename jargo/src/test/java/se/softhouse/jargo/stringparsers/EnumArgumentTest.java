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

import static java.util.Arrays.asList;
import static org.fest.assertions.Assertions.assertThat;
import static org.junit.Assert.fail;
import static se.softhouse.jargo.Arguments.enumArgument;
import static se.softhouse.jargo.utils.Assertions2.assertThat;

import java.util.List;

import org.junit.Test;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import se.softhouse.jargo.ArgumentException;
import se.softhouse.jargo.Arguments;
import se.softhouse.jargo.StringParsers;
import se.softhouse.jargo.Usage;
import se.softhouse.jargo.internal.Texts.UserErrors;

/**
 * Tests for {@link Arguments#enumArgument(Class, String...)} and
 * {@link StringParsers#enumParser(Class)}
 */
public class EnumArgumentTest
{
	public enum Action
	{
		start,
		stop,
		restart
	}

	public enum UpperCase
	{
		START,
		STOP,
		RESTART
	}

	@Test
	public void testEnumArgument() throws ArgumentException
	{
		Action action = enumArgument(Action.class).parse("stop");
		assertThat(action).isEqualTo(Action.stop);
	}

	@Test
	public void testEnumArgumentWithNames() throws ArgumentException
	{
		List<Action> action = enumArgument(Action.class, "-a", "--action").repeated().parse("-a", "stop", "--action", "start");
		assertThat(action).isEqualTo(asList(Action.stop, Action.start));
	}

	@Test
	public void testEnumArgumentUsage()
	{
		Usage usageText = enumArgument(Action.class).usage();
		assertThat(usageText).contains("<Action>: {start | stop | restart}");
		assertThat(usageText).contains("Default: null");
	}

	@Test(expected = ArgumentException.class)
	public void testThatEnumArgumentOnlyMatchesExactMatchOrUpperCase() throws ArgumentException
	{
		Action action = enumArgument(Action.class).parse("STOP");
		assertThat(action).isEqualTo(Action.stop);
	}

	@Test
	public void testThatEnumArgumentTriesUpperCase() throws ArgumentException
	{
		UpperCase upperCase = enumArgument(UpperCase.class).parse("stop");
		assertThat(upperCase).isEqualTo(UpperCase.STOP);
	}

	enum ShouldNotInitialize
	{
		VALUE;

		static
		{
			staticEnumCodeHaveBeenRun = true;
		}
	}

	static boolean staticEnumCodeHaveBeenRun = false;

	@Test
	@SuppressFBWarnings(value = "RV_RETURN_VALUE_IGNORED", justification = "Only testing that static code hasn't run")
	public void testThatEnumIsNotInitializedUntilParse()
	{
		enumArgument(ShouldNotInitialize.class, "-UselessParameter");
		assertThat(staticEnumCodeHaveBeenRun).isFalse();
	}

	@Test
	public void testInvalidEnumArgument()
	{
		try
		{
			enumArgument(Action.class).parse("break");
			fail("break should not be a valid action");
		}
		catch(ArgumentException e)
		{
			assertThat(e.getMessage()).isEqualTo(String.format(UserErrors.INVALID_ENUM_VALUE, "break", "{start | stop | restart}"));
		}
	}

	@Test
	public void testThatValidEnumOptionsAreNotConstructedIfNotNeeded()
	{
		try
		{
			enumArgument(NefariousToString.class).parse("TWO");
			fail("TWO should be an invalid enum value");
		}
		catch(ArgumentException invalidEnumValue)
		{
			try
			{
				invalidEnumValue.getMessage();
				fail("Nefarious toString not detected");
			}
			catch(IllegalStateException expectingNefariousBehavior)
			{

			}
		}
	}

	enum NefariousToString
	{
		ONE;

	@Override
	public String toString()
	{
		throw new IllegalStateException("Nefarious behavior not avoided");
	}
}}
