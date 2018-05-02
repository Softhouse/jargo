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
import static se.softhouse.common.strings.StringsUtil.NEWLINE;
import static se.softhouse.jargo.Arguments.bigIntegerArgument;
import static se.softhouse.jargo.utils.Assertions2.assertThat;

import java.math.BigInteger;

import org.junit.Test;

import se.softhouse.jargo.ArgumentException;
import se.softhouse.jargo.Arguments;
import se.softhouse.jargo.StringParsers;
import se.softhouse.jargo.Usage;

/**
 * Tests for {@link Arguments#bigIntegerArgument(String...)} and
 * {@link StringParsers#bigIntegerParser()}
 */
public class BigIntegerArgumentTest
{
	@Test
	public void testValidInteger() throws ArgumentException
	{
		BigInteger b = bigIntegerArgument("-n").parse("-n", "123456789123456789");

		assertThat(b).isEqualTo(new BigInteger("123456789123456789"));
	}

	@Test
	public void testInvalidInteger()
	{
		try
		{
			bigIntegerArgument("-n").parse("-n", "1a");
		}
		catch(ArgumentException e)
		{
			/**
			 * @formatter.off
			 */
			assertThat(e).hasMessage("'1a' is not a valid big-integer (Localization: English (United States))" + NEWLINE +
			                         "  ^");
			/**
			 * @formatter.on
			 */
		}
	}

	@Test
	public void testDescription()
	{
		Usage usage = bigIntegerArgument("-b").usage();
		assertThat(usage).contains("<big-integer>: an arbitrary integer number (practically no limits)");
	}

	@Test
	public void testThatBigIntegerDefaultsToZero() throws ArgumentException
	{
		BigInteger b = bigIntegerArgument("-b").parse();
		assertThat(b).isEqualTo(BigInteger.ZERO);
	}
}
