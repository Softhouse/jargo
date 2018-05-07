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
package se.softhouse.jargo;

import static com.google.common.collect.ImmutableList.of;
import static org.fest.assertions.Assertions.assertThat;
import static org.fest.assertions.Fail.fail;
import static se.softhouse.jargo.Arguments.integerArgument;
import static se.softhouse.jargo.Arguments.stringArgument;
import static se.softhouse.jargo.limiters.FooLimiter.foos;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Test;

import com.google.common.collect.ImmutableList;

import se.softhouse.jargo.internal.Texts.UserErrors;

/**
 * Tests for {@link ArgumentBuilder#repeated()}
 */
public class RepeatedArgumentTest
{
	@Test
	public void testRepeatedIntegerArgument() throws ArgumentException
	{
		String[] args = {"--number", "1", "--number", "2"};

		List<Integer> numbers = integerArgument("--number").repeated().parse(args);

		assertThat(numbers).isEqualTo(Arrays.asList(1, 2));
	}

	@Test
	public void testRepeatedArityArgument() throws ArgumentException
	{
		String[] args = {"--numbers", "5", "6", "--numbers", "3", "4"};
		List<List<Integer>> numbers = integerArgument("--numbers").arity(2).repeated().parse(args);
		assertThat(numbers).isEqualTo(ImmutableList.of(of(5, 6), of(3, 4)));
	}

	@Test(expected = ArgumentException.class)
	public void testUnhandledRepition() throws ArgumentException
	{
		integerArgument("-number").parse("-number", "5", "-number", "3");
	}

	@Test(expected = ArgumentException.class)
	public void testUnhandledRepitionForArityArgument() throws ArgumentException
	{
		integerArgument("--numbers").arity(2).parse("--numbers", "5", "6", "--numbers", "3", "4");
	}

	@Test
	public void testRepeatedPropertyValues() throws ArgumentException
	{
		Map<String, List<Integer>> numberMap = integerArgument("-N").repeated().asPropertyMap().parse("-Nnumber=1", "-Nnumber=2");
		assertThat(numberMap.get("number")).isEqualTo(Arrays.asList(1, 2));
	}

	@Test
	public void testRepeatedAndSplitPropertyValues() throws ArgumentException
	{
		Map<String, List<List<Integer>>> numberMap = integerArgument("-N").splitWith(",").repeated().asPropertyMap() //
				.parse("-Nnumber=1,2", "-Nnumber=3,4");

		List<ImmutableList<Integer>> expected = ImmutableList.of(of(1, 2), of(3, 4));

		assertThat(numberMap.get("number")).isEqualTo(expected);
	}

	@Test
	public void testThatRepeatedCanBeTransformedToUniqueValues()
	{
		Set<Integer> numbers = integerArgument("-n").arity(3).unique().parse("-n", "123", "24", "123");
		assertThat(numbers).containsOnly(123, 24);
		numbers = integerArgument("-n").variableArity().unique().parse("-n", "123", "24", "123", "1");
		assertThat(numbers).containsOnly(123, 24, 1);
		Set<Integer> repeatedNumbers = integerArgument("-n").repeated().unique().parse("-n", "123", "-n", "24", "-n", "123");
		assertThat(repeatedNumbers).containsOnly(123, 24);
		try
		{
			repeatedNumbers.add(4);
			fail("Returned data types should be immutable/unmodifiable");
		}
		catch(UnsupportedOperationException expected)
		{
		}
	}

	@Test
	public void testRepeatedValuesWithoutHandling()
	{
		try
		{
			integerArgument("--number", "-n").parse("--number", "1", "-n", "2");
			fail("-n should have been detected as --number was given already");
		}
		catch(ArgumentException expected)
		{
			assertThat(expected).hasMessage(String.format(UserErrors.DISALLOWED_REPETITION, "-n"));
		}
	}

	@Test
	public void testInvalidValuesShouldNotBeParsedIfRepeatedArgumentsAreNotAllowed()
	{
		try
		{
			stringArgument("-n").limitTo(foos()).defaultValue("foo").parse("-n", "foo", "-n", "bar");
			fail("disallowed repition (bar) not detected");
		}
		catch(ArgumentException expected)
		{
			assertThat(expected).hasMessage(String.format(UserErrors.DISALLOWED_REPETITION, "-n"));
		}
	}

	@Test
	public void testThatListsWithRepeatedValuesAreUnmodifiable() throws ArgumentException
	{
		List<Integer> numberList = integerArgument("-N").repeated().parse("-N", "1", "-N", "-2");
		try
		{
			numberList.add(3);
			fail("a list of repeated values should be unmodifiable");
		}
		catch(UnsupportedOperationException expected)
		{
		}
	}

	@SuppressWarnings("deprecation")
	// This is what's tested
	@Test(expected = IllegalStateException.class)
	public void testCallingRepeatedBeforeArity()
	{
		integerArgument("--number").repeated().arity(2);
	}

	@SuppressWarnings("deprecation")
	// This is what's tested
	@Test(expected = IllegalStateException.class)
	public void testCallingRepeatedBeforeVariableArity()
	{
		integerArgument("--number").repeated().variableArity();
	}

	@SuppressWarnings("deprecation")
	// This is what's tested
	@Test(expected = IllegalStateException.class)
	public void testCallingSplitWithAfterRepeated()
	{
		integerArgument().repeated().splitWith(",");
	}
}
