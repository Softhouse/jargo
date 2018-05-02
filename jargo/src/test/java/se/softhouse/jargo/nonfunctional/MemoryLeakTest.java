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
package se.softhouse.jargo.nonfunctional;

import static org.fest.assertions.Assertions.assertThat;
import static se.softhouse.jargo.utils.Assertions2.assertThat;

import java.util.Locale;

import org.junit.Test;

import se.softhouse.common.testlib.MemoryTester;
import se.softhouse.common.testlib.MemoryTester.FinalizationAwareObject;
import se.softhouse.jargo.Argument;
import se.softhouse.jargo.ArgumentException;
import se.softhouse.jargo.Arguments;
import se.softhouse.jargo.CommandLineParser;
import se.softhouse.jargo.StringParser;

public class MemoryLeakTest
{

	@Test
	public void testThatCommandLineParserDoesNotHoldOntoParsedValuesTooLong() throws Exception
	{
		parseAndUseReference().assertThatNoMoreReferencesToReferentIsKept();
	}

	/**
	 * Use shouldNotBeLeaked in various ways and make sure no references to it is kept
	 * afterwards
	 */
	private FinalizationAwareObject parseAndUseReference() throws ArgumentException
	{
		final Object shouldNotBeLeaked = new Object();
		FinalizationAwareObject finalizationAwareObject = MemoryTester.createFinalizationAwareObject(shouldNotBeLeaked);
		Argument<Object> leakTester = Arguments.withParser(new StringParser<Object>(){

			@Override
			public Object parse(String argument, Locale locale) throws ArgumentException
			{
				return shouldNotBeLeaked;
			}

			@Override
			public String descriptionOfValidValues(Locale locale)
			{
				return shouldNotBeLeaked.toString();
			}

			@Override
			public Object defaultValue()
			{
				return shouldNotBeLeaked;
			}

			@Override
			public String metaDescription()
			{
				return "leak-test";
			}
		}).build();
		CommandLineParser parser = CommandLineParser.withArguments(leakTester);
		assertThat(parser.parse().get(leakTester)).isEqualTo(shouldNotBeLeaked);
		assertThat(parser.parse("").get(leakTester)).isEqualTo(shouldNotBeLeaked);
		assertThat(parser.usage()).contains(shouldNotBeLeaked.toString());
		return finalizationAwareObject;
	}
}
