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

import static org.fest.assertions.Assertions.assertThat;
import static org.fest.assertions.Fail.fail;
import static se.softhouse.jargo.Arguments.optionArgument;
import static se.softhouse.jargo.utils.Assertions2.assertThat;

import java.util.Collections;

import org.junit.Test;

import se.softhouse.jargo.ArgumentException;
import se.softhouse.jargo.Arguments;
import se.softhouse.jargo.Usage;
import se.softhouse.jargo.internal.Texts.ProgrammaticErrors;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Tests for {@link Arguments#optionArgument(String, String...)}
 */
public class OptionalArgumentTest
{
	@Test
	public void testThatOptionalArgumentsDefaultsToFalse() throws ArgumentException
	{
		assertThat(optionArgument("-l").parse()).isFalse();
	}

	@Test
	public void testThatOptionalIsTrueWhenArgumentIsGiven() throws ArgumentException
	{
		assertThat(optionArgument("--disable-logging").parse("--disable-logging")).isTrue();
	}

	@Test
	public void testThatOptionalIsFalseWhenArgumentIsGivenAndDefaultIsTrue() throws ArgumentException
	{
		assertThat(optionArgument("--disable-logging").defaultValue(true).parse("--disable-logging")).isFalse();
	}

	@Test
	public void testDescription()
	{
		Usage usage = optionArgument("--enable-logging").usage();
		assertThat(usage).contains("Default: disabled");
	}

	@Test
	public void testForDefaultTrue() throws ArgumentException
	{
		Usage usage = optionArgument("--disable-logging").defaultValue(true).usage();
		assertThat(usage).contains("Default: enabled");

		assertThat(optionArgument("--disable-logging").defaultValue(true).parse()).isTrue();
	}

	@Test
	@SuppressFBWarnings(value = "NP_NONNULL_PARAM_VIOLATION", justification = "Checks enforcement of the annotation")
	public void testThatNullIsNotAllowed()
	{
		try
		{
			optionArgument("--enable-logging").defaultValue(null);
			fail("tri-state booleans are evil and should thus be forbidden");
		}
		catch(NullPointerException expected)
		{
			assertThat(expected).hasMessage(ProgrammaticErrors.OPTION_DOES_NOT_ALLOW_NULL_AS_DEFAULT);
		}
	}

	@Test
	public void testThatOptionalArgumentsEnforcesAtLeastOneName()
	{
		try
		{
			optionArgument("-l").names();
			fail("Useless optionArgument not detected");
		}
		catch(IllegalArgumentException expected)
		{
			assertThat(expected).hasMessage(ProgrammaticErrors.OPTIONS_REQUIRES_AT_LEAST_ONE_NAME);
		}
	}

	@Test
	public void testThatOptionalArgumentsCanUseAnotherName() throws ArgumentException
	{
		assertThat(optionArgument("-l").names("--logging").parse("--logging")).isTrue();
	}

	@Test
	public void testThatOptionArgumentWorksInPropertyMap() throws ArgumentException
	{
		assertThat(optionArgument("-n").asPropertyMap().parse("-nactived.property").get("actived.property")).isTrue();
	}

	@Test
	public void testThatOptionDefaultValueForKeysInPropertyMapWorks() throws ArgumentException
	{
		assertThat(optionArgument("-n").defaultValue(true).asPropertyMap().parse().get("by.default.activated.property")).isTrue();
	}

	@Test(expected = IllegalArgumentException.class)
	public void testThatOptionalArgumentsEnforcesAtLeastOneNameForIterable()
	{
		optionArgument("-l").names(Collections.<String>emptyList());
	}

	@SuppressWarnings("deprecation")
	@Test(expected = IllegalStateException.class)
	public void testThatOptionalArgumentsCantHaveSeparators()
	{
		optionArgument("-l").separator("=");
	}

	@SuppressWarnings("deprecation")
	@Test(expected = IllegalStateException.class)
	public void testThatOptionalArgumentsCantHaveArity()
	{
		optionArgument("-l").arity(2);
	}

	@SuppressWarnings("deprecation")
	@Test(expected = IllegalStateException.class)
	public void testThatOptionalArgumentsCantHaveVariableArity()
	{
		optionArgument("-l").variableArity();
	}

	@SuppressWarnings("deprecation")
	@Test(expected = IllegalStateException.class)
	public void testThatOptionalArgumentsCantBeSplit()
	{
		optionArgument("-l").splitWith(",");
	}
}
