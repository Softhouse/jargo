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

import static org.fest.assertions.Fail.fail;
import static se.softhouse.common.strings.StringsUtil.NEWLINE;
import static se.softhouse.common.testlib.Locales.TURKISH;
import static se.softhouse.jargo.Arguments.bigDecimalArgument;
import static se.softhouse.jargo.Arguments.bigIntegerArgument;
import static se.softhouse.jargo.Arguments.byteArgument;
import static se.softhouse.jargo.Arguments.integerArgument;
import static se.softhouse.jargo.Arguments.longArgument;
import static se.softhouse.jargo.Arguments.shortArgument;
import static se.softhouse.jargo.utils.Assertions2.assertThat;
import static se.softhouse.jargo.utils.ExpectedTexts.expected;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import se.softhouse.jargo.Argument;
import se.softhouse.jargo.ArgumentException;
import se.softhouse.jargo.Arguments;
import se.softhouse.jargo.CommandLineParser;
import se.softhouse.jargo.StringParsers;
import se.softhouse.jargo.Usage;

/**
 * <pre>
 * Tests for
 * {@link Arguments#byteArgument(String...)},
 * {@link Arguments#shortArgument(String...)},
 * {@link Arguments#integerArgument(String...)},
 * {@link Arguments#longArgument(String...)}
 * and
 * {@link StringParsers#byteParser()},
 * {@link StringParsers#shortParser()},
 * {@link StringParsers#integerParser()},
 * {@link StringParsers#longParser()}
 */
public class NumberArgumentTest
{
	@Test
	public void testUsage()
	{
		String validIntegers = "<integer>: -2,147,483,648 to 2,147,483,647" + NEWLINE;
		Usage usage = integerArgument().usage();
		assertThat(usage).contains(validIntegers);
		assertThat(usage).contains("Default: 0" + NEWLINE);
	}

	@Test
	public void testInvalidNumberArguments()
	{
		List<Integer> invalidInput = Arrays.asList(Byte.MIN_VALUE - 1, Byte.MAX_VALUE + 1);
		for(Integer input : invalidInput)
		{
			try
			{
				byteArgument("-b").parse("-b", input.toString());
				fail("Invalid byte input not detected: " + input);
			}
			catch(ArgumentException e)
			{
				assertThat(e.getMessageAndUsage()).isEqualTo(expected("InvalidByte" + input));
			}
		}

		try
		{
			byteArgument("-b").parse("-b", "NaN");
			fail("Not a number not detected");
		}
		catch(ArgumentException e)
		{
			assertThat(e.getMessageAndUsage()).isEqualTo(expected("ByteNaN"));
		}
	}

	@Test
	public void testThatDefaultValueForShortIsFormattedInTheChosenLocale()
	{
		Argument<Short> localeDependentArgument = shortArgument().defaultValue(Short.MAX_VALUE).build();
		Usage usage = CommandLineParser.withArguments(localeDependentArgument).locale(TURKISH).usage();
		assertThat(usage).contains("Default: 32.767" + NEWLINE);
	}

	@Test
	public void testThatDefaultValueForIntegerIsFormattedInTheChosenLocale()
	{
		Argument<Integer> localeDependentArgument = integerArgument().defaultValue(Integer.MAX_VALUE).build();
		Usage usage = CommandLineParser.withArguments(localeDependentArgument).locale(TURKISH).usage();
		assertThat(usage).contains("Default: 2.147.483.647" + NEWLINE);
	}

	@Test
	public void testThatDefaultValueForLongIsFormattedInTheChosenLocale()
	{
		Argument<Long> localeDependentArgument = longArgument().defaultValue(Long.MAX_VALUE).build();
		Usage usage = CommandLineParser.withArguments(localeDependentArgument).locale(TURKISH).usage();
		assertThat(usage).contains("Default: 9.223.372.036.854.775.807" + NEWLINE);
	}

	@Test
	public void testThatDefaultValueForBigDecimalIsFormattedInTheChosenLocale()
	{
		Argument<BigDecimal> localeDependentArgument = bigDecimalArgument().defaultValue(BigDecimal.valueOf(Long.MAX_VALUE)).build();
		Usage usage = CommandLineParser.withArguments(localeDependentArgument).locale(TURKISH).usage();
		assertThat(usage).contains("Default: 9.223.372.036.854.775.807" + NEWLINE);
	}

	@Test
	public void testThatDefaultValueForBigIntegerIsFormattedInTheChosenLocale()
	{
		Argument<BigInteger> localeDependentArgument = bigIntegerArgument().defaultValue(BigInteger.valueOf(Long.MAX_VALUE)).build();
		Usage usage = CommandLineParser.withArguments(localeDependentArgument).locale(TURKISH).usage();
		assertThat(usage).contains("Default: 9.223.372.036.854.775.807" + NEWLINE);
	}
}
