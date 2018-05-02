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

import org.junit.Test;
import se.softhouse.jargo.ArgumentException;
import se.softhouse.jargo.Arguments;
import se.softhouse.jargo.StringParsers;
import se.softhouse.jargo.Usage;
import se.softhouse.jargo.internal.Texts.UserErrors;

import static org.fest.assertions.Assertions.assertThat;
import static org.fest.assertions.Fail.fail;
import static se.softhouse.jargo.Arguments.charArgument;
import static se.softhouse.jargo.utils.Assertions2.assertThat;

/**
 * Tests for {@link Arguments#charArgument(String...)} and {@link StringParsers#charParser()}
 */
public class CharArgumentTest
{
	@Test
	public void testValidCharacter() throws ArgumentException
	{
		Character z = charArgument("-c").parse("-c", "Z");

		assertThat(z).isEqualTo('Z');
	}

	@Test
	public void testInvalidLength()
	{
		try
		{
			charArgument("-c").parse("-c", "abc");
			fail("abc is three characters and should thus not be a valid character");
		}
		catch(ArgumentException e)
		{
			assertThat(e.getMessage()).isEqualTo(String.format(UserErrors.INVALID_CHAR, "abc"));
		}
	}

	@Test
	public void testDescription()
	{
		Usage usage = charArgument("-c").usage();
		assertThat(usage).contains("<character>: any unicode character");
		assertThat(usage).contains("Default: the Null character");

		assertThat(charArgument("-c").defaultValue(null).usage()).contains("Default: null");
		assertThat(charArgument("-c").defaultValue('A').usage()).contains("Default: A");
	}

	@Test
	public void testThatCharDefaultsToZero() throws ArgumentException
	{
		Character c = charArgument("-c").parse();
		assertThat(c.charValue()).isEqualTo((char) 0);
	}
}
