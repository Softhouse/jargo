/* Copyright 2013 Jonatan Jönsson
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
*/
package se.j4j.argumentparser;

import static org.fest.assertions.Assertions.assertThat;
import static se.j4j.argumentparser.ArgumentFactory.integerArgument;
import static se.j4j.argumentparser.ArgumentFactory.stringArgument;
import static se.j4j.argumentparser.utils.ExpectedTexts.expected;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;

/**
 * Tests for {@link ArgumentBuilder#separator(String)}
 */
public class SeparatorTest
{
	@Test
	public void testIgnoringCaseCombinedWithSeparator() throws ArgumentException
	{
		Argument<String> logLevel = stringArgument("-log").ignoreCase().separator("=").build();

		assertThat(logLevel.parse("-Log=debug")).isEqualTo("debug");
		assertThat(logLevel.parse("-log=debug")).isEqualTo("debug");
	}

	@Test
	public void testIgnoringCaseCombinedWithAlphaSeparator() throws ArgumentException
	{
		Argument<String> logLevel = stringArgument("-log").ignoreCase().separator("A").build();

		assertThat(logLevel.parse("-LogAdebug")).isEqualTo("debug");
		assertThat(logLevel.parse("-logAdebug")).isEqualTo("debug");
	}

	@Test
	public void testArityCombinedWithSeparator() throws ArgumentException
	{
		String usage = integerArgument("-numbers").arity(3).separator("=").usage();
		assertThat(usage).isEqualTo(expected("separatorCombinedWithArity"));

		List<Integer> numbers = integerArgument("-numbers").arity(3).separator("=").parse("-numbers=1", "2", "3");
		assertThat(numbers).isEqualTo(Arrays.asList(1, 2, 3));
	}

	@Test
	public void testEmptySeparator() throws ArgumentException
	{
		Integer number = integerArgument("-N").separator("").parse("-N10");

		assertThat(number).isEqualTo(10);
	}

	@Test
	public void testEmptySeparatorWithSeveralNames() throws ArgumentException
	{
		Integer number = integerArgument("-N", "--name").separator("").parse("--name10");

		assertThat(number).isEqualTo(10);
	}

	@Test
	public void testEmptySeparatorWithSeveralNamesAndIgnoreCase() throws ArgumentException
	{
		Integer number = integerArgument("-N", "--name").separator("").ignoreCase().parse("--Name10");

		assertThat(number).isEqualTo(10);
	}

	@Test
	public void testTwoLetterSeperator() throws ArgumentException
	{
		Integer number = integerArgument("-N").separator("==").parse("-N==10");

		assertThat(number).isEqualTo(10);
	}

	@Test
	public void testTwoLetterSeperatorWithIgnoreCase() throws ArgumentException
	{
		Integer number = integerArgument("-N").separator("Fo").ignoreCase().parse("-Nfo10");

		assertThat(number).isEqualTo(10);
	}

	@Test
	public void testThatSeparatorIsPrintedBetweenArgumentNameAndMetaDescription()
	{
		String usage = integerArgument("-N").separator("=").usage();

		assertThat(usage).isEqualTo(expected("separatorBetweenNameAndMeta"));
	}

	@Test
	public void testMultipleNamesWithTheSameLengthAndTheSameStart() throws ArgumentException
	{
		Argument<Integer> number = integerArgument("--number").separator("").build();
		Argument<Integer> numberTwo = integerArgument("--numberTwo").separator("").build();

		assertThat(CommandLineParser.withArguments(number, numberTwo).parse("--number5").get(number)).isEqualTo(5);
	}
}
