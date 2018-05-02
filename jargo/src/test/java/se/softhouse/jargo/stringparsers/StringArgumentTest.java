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
import static se.softhouse.jargo.Arguments.stringArgument;

import org.junit.Test;

import se.softhouse.jargo.ArgumentException;
import se.softhouse.jargo.Arguments;
import se.softhouse.jargo.StringParsers;

/**
 * Tests for {@link Arguments#stringArgument(String...)} and
 * {@link StringParsers#stringParser()}
 */
public class StringArgumentTest
{
	@Test
	public void testThatTheSameStringIsReturned() throws ArgumentException
	{
		String argumentValue = "Test";
		String actual = stringArgument("-s").parse("-s", argumentValue);
		assertThat(actual).isSameAs(argumentValue);
	}
}
