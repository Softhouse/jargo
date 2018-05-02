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
package se.softhouse.jargo.utils;

import static org.fest.assertions.Assertions.assertThat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import se.softhouse.jargo.Argument;
import se.softhouse.jargo.ArgumentException;
import se.softhouse.jargo.CommandLineParser;
import se.softhouse.jargo.ParsedArguments;

import com.google.common.collect.Lists;

/**
 * Can be used for testing purposes.
 */
public final class ArgumentExpector
{
	List<Expectation<?>> expectations = Lists.newArrayList();
	List<String> actualArguments = Lists.newArrayList();

	public <T> Expectation<T> expectThat(Argument<T> definition)
	{
		return new Expectation<T>(definition);
	}

	public class Expectation<T>
	{
		Argument<T> definition;
		T expect;

		public Expectation(Argument<T> definition)
		{
			this.definition = definition;
		}

		public Assertion receives(T expectation)
		{
			this.expect = expectation;
			expectations.add(this);
			return new Assertion();
		}

		@Override
		public String toString()
		{
			return definition.toString() + ", expecting: " + expect;
		}
	}

	public class Assertion
	{
		/**
		 * Creates a parser for all the definitions previously given to this
		 * expectation and asserts that they are all correct.
		 */
		public void given(String arguments) throws ArgumentException
		{
			actualArguments.addAll(Arrays.asList(arguments.split(" ")));
			CommandLineParser parser = CommandLineParser.withArguments(arguments());
			ParsedArguments parsedArguments = parser.parse(actualArguments);
			for(Expectation<?> expectation : expectations)
			{
				assertExpectation(expectation, parsedArguments);
			}
		}
	}

	private <T> void assertExpectation(Expectation<T> expectation, ParsedArguments arguments)
	{
		assertThat(arguments.get(expectation.definition)).isEqualTo(expectation.expect);
	}

	private List<Argument<?>> arguments()
	{
		List<Argument<?>> argumentDefinitions = new ArrayList<Argument<?>>(expectations.size());
		for(Expectation<?> expectation : expectations)
		{
			argumentDefinitions.add(expectation.definition);
		}
		return argumentDefinitions;
	}
}
