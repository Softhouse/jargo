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
package se.softhouse.jargo.addons;

import static org.fest.assertions.Assertions.assertThat;
import static org.fest.assertions.Fail.fail;
import static se.softhouse.jargo.addons.Arguments.dateArgument;
import static se.softhouse.jargo.utils.Assertions2.assertThat;
import static se.softhouse.jargo.utils.ExpectedTexts.expected;

import java.util.Locale;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.Test;

import se.softhouse.common.testlib.Locales;
import se.softhouse.common.testlib.UtilityClassTester;
import se.softhouse.jargo.Argument;
import se.softhouse.jargo.ArgumentException;
import se.softhouse.jargo.CommandLineParser;
import se.softhouse.jargo.Usage;

import com.google.common.testing.NullPointerTester;

public class DateTimeParserTest
{
	@Test
	public void testDateArgument() throws ArgumentException
	{
		DateTime parsedDate = dateArgument("--start").parse("--start", "2011-03-30");
		assertThat(parsedDate).isEqualTo(new DateTime("2011-03-30"));

		Usage usage = dateArgument(DateTimeZone.forOffsetHours(2), "--start").usage();
		assertThat(usage).isEqualTo(expected("dateTime"));
	}

	@Test
	public void testThatTimezoneCanBeSpecified() throws ArgumentException
	{
		DateTime parsedDate = dateArgument(DateTimeZone.UTC).parse("2011-03-30T00:00:00.000");
		assertThat(parsedDate).isEqualTo(new DateTime("2011-03-30T00:00:00.000", DateTimeZone.UTC));
	}

	@Test
	public void testThatTimeCanBeGiven() throws Exception
	{
		DateTime parsedDate = dateArgument().parse("2011-02-28T00:00:00.000+01:00");
		assertThat(parsedDate).isEqualTo(new DateTime("2011-02-28T00:00:00.000+01:00"));
	}

	@Test
	public void testThatInvalidFormatErrorMessageLooksGood() throws Exception
	{
		try
		{
			dateArgument().parse("ABC");
			fail("Characters should be not be allowed");
		}
		catch(ArgumentException expected)
		{
			assertThat(expected).hasMessage("Invalid format: \"ABC\"");
		}
	}

	/**
	 * As it's a ISO8601 date parser, the locale should not matter
	 */
	@Test
	public void testThatDateArgumentIgnoresLocale() throws ArgumentException
	{
		Argument<DateTime> dateArgument = dateArgument().build();
		CommandLineParser parser = CommandLineParser.withArguments(dateArgument).locale(Locales.SWEDISH);
		DateTime swedishDate = parser.parse("2011-03-30").get(dateArgument);
		assertThat(swedishDate).isEqualTo(new DateTime("2011-03-30"));

		parser.locale(Locale.US);
		DateTime americanDate = parser.parse("2011-03-30").get(dateArgument);
		assertThat(americanDate).isEqualTo(new DateTime("2011-03-30"));
	}

	@Test
	public void testThatUtilityClassDesignIsCorrect() throws Exception
	{
		UtilityClassTester.testUtilityClassDesign(Arguments.class);
		NullPointerTester tester = new NullPointerTester();
		tester.setDefault(DateTimeZone.class, DateTimeZone.getDefault());
		tester.testAllPublicStaticMethods(Arguments.class);
		tester.testAllPublicInstanceMethods(new DateTimeParser(DateTimeZone.getDefault()));
	}
}
