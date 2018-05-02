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
package se.softhouse.jargo.stringparsers.custom;

import static org.fest.assertions.Assertions.assertThat;
import static se.softhouse.jargo.Arguments.withParser;
import static se.softhouse.jargo.StringParsers.stringParser;
import static se.softhouse.jargo.utils.Assertions2.assertThat;

import java.util.Locale;

import org.junit.Test;

import se.softhouse.jargo.ArgumentException;
import se.softhouse.jargo.ForwardingStringParser;
import se.softhouse.jargo.StringParser;
import se.softhouse.jargo.StringParsers;
import se.softhouse.jargo.Usage;

/**
 * Tests for {@link ForwardingStringParser}
 */
public class ForwardingStringParserTest
{
	@Test
	public void testOverridingEachMethodForStringParser() throws ArgumentException
	{
		String argumentValue = "bar";
		String result = withParser(new InterningStringParser()).parse(argumentValue);
		assertThat(result).isSameAs(argumentValue.intern());

		Usage usage = withParser(new InterningStringParser()).names("-f").usage();
		assertThat(usage).contains("-f <interned_string>    <interned_string>: some string");
		assertThat(usage).contains("Default: foo");
	}

	@Test
	public void testThatForwaringStringParserForwardsToDelegate() throws ArgumentException
	{
		StringParser<Integer> delegatedParser = new ForwardingStringParser<Integer>(){
			@Override
			protected StringParser<Integer> delegate()
			{
				return StringParsers.integerParser();
			}
		};

		StringParser<Integer> regularParser = StringParsers.integerParser();
		Locale locale = Locale.ENGLISH;

		assertThat(delegatedParser.parse("1", locale)).isEqualTo(regularParser.parse("1", locale));
		assertThat(delegatedParser.descriptionOfValidValues(locale)).isEqualTo(regularParser.descriptionOfValidValues(locale));
		assertThat(delegatedParser.defaultValue()).isEqualTo(regularParser.defaultValue());
		assertThat(delegatedParser.metaDescription()).isEqualTo(regularParser.metaDescription());
		assertThat(delegatedParser.toString()).isEqualTo(regularParser.toString());
	}

	private static final class InterningStringParser extends ForwardingStringParser<String>
	{
		@Override
		public String parse(String value, Locale locale) throws ArgumentException
		{
			return value.intern();
		}

		@Override
		public String defaultValue()
		{
			return "foo";
		}

		@Override
		public String metaDescription()
		{
			return "<interned_string>";
		}

		@Override
		public String descriptionOfValidValues(Locale locale)
		{
			return "some string";
		}

		@Override
		protected StringParser<String> delegate()
		{
			return stringParser();
		}
	}
}
