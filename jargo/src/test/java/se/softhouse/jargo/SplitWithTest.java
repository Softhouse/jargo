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

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static org.fest.assertions.Assertions.assertThat;
import static org.fest.assertions.Fail.fail;
import static se.softhouse.jargo.Arguments.integerArgument;
import static se.softhouse.jargo.utils.Assertions2.assertThat;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import se.softhouse.common.testlib.Explanation;
import se.softhouse.jargo.ArgumentExceptions.MissingParameterException;
import se.softhouse.jargo.internal.Texts.UserErrors;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Tests for {@link ArgumentBuilder#splitWith(String)}
 */
public class SplitWithTest
{
	@Test
	public void testSplittingWithComma() throws ArgumentException
	{
		List<Integer> numbers = integerArgument("-n").splitWith(",").parse("-n", "1,2");

		assertThat(numbers).isEqualTo(asList(1, 2));
	}

	@Test
	public void testThatUsageTextForSplittingWithCommaLooksGood()
	{
		Usage usage = integerArgument("-n").splitWith(",").usage();
		assertThat(usage).contains("-n <integer>,<integer>,... ");
	}

	@Test(expected = IllegalArgumentException.class)
	@SuppressFBWarnings(value = "RV_RETURN_VALUE_IGNORED", justification = Explanation.FAIL_FAST)
	public void testThatSplittingWithEmptyStringIsForbidden()
	{
		integerArgument("-n").splitWith("");
	}

	@Test
	public void testThatSeparatorCombinedWithSplitterLooksGoodInUsage()
	{
		Usage usage = integerArgument("-N").separator("=").splitWith(",").usage();
		assertThat(usage).contains("-N=<integer>,<integer>,... ");
	}

	@Test
	public void testSplitWithCombinedWithPropertyMap() throws ArgumentException
	{
		Map<String, List<Integer>> numbers = integerArgument("-n").splitWith(",").asPropertyMap().parse("-nsmall=1,2", "-nbig=3,4");

		Map<String, List<Integer>> expected = new HashMap<String, List<Integer>>();
		expected.put("small", asList(1, 2));
		expected.put("big", asList(3, 4));
		assertThat(numbers).isEqualTo(expected);
	}

	@Test
	public void testSplittingCombinedWithRepeating() throws ArgumentException
	{
		List<List<Integer>> numbers = integerArgument("-n").separator("=").splitWith(",").repeated().parse("-n=1,2", "-n=3,4");

		List<List<Integer>> expected = new ArrayList<List<Integer>>();
		expected.add(asList(1, 2));
		expected.add(asList(3, 4));
		assertThat(numbers).isEqualTo(expected);
	}

	@Test
	public void testSplittingWithNoArg() throws ArgumentException
	{
		try
		{
			integerArgument("-n").splitWith(",").parse("-n");
			fail("no parameter to splitted argument should not be allowed, user must use \"\" to specify an empty list of values");
		}
		catch(MissingParameterException expected)
		{
			assertThat(expected).hasMessage(String.format(UserErrors.MISSING_PARAMETER, "<integer>", "-n"));
		}
	}

	@Test
	public void testSplittingWithEmptyInputString() throws ArgumentException
	{
		assertThat(integerArgument("-n").splitWith(",").parse("-n", "")).isEqualTo(emptyList());
	}

	@Test
	public void testSplittingWithEmptyInputStringAndDefaultValue() throws ArgumentException
	{
		assertThat(integerArgument("-n").defaultValue(42).splitWith(",").parse("-n", "")).isEqualTo(emptyList());
	}

	@Test
	public void testSplittingWithOnlyWhitespaceInInputString() throws ArgumentException
	{
		assertThat(integerArgument("-n").splitWith(",").parse("-n", " 	\n\r")).isEqualTo(emptyList());
	}

	@Test
	public void testThatListsWithSplitValuesAreUnmodifiable() throws ArgumentException
	{
		List<Integer> numbers = integerArgument("-N").splitWith(",").parse("-N", "1,2");
		try
		{
			numbers.add(3);
			fail("a list of split values should be unmodifiable");
		}
		catch(UnsupportedOperationException expected)
		{

		}
	}

	// This is what's tested
	@SuppressWarnings("deprecation")
	@Test(expected = IllegalStateException.class)
	public void testArityCombinedWithPropertyMapWrongCallOrder()
	{
		// The intent is to guide the user of the API to how he should have used
		// it
		integerArgument("-n").asPropertyMap().splitWith(",");
	}

	/**
	 * <pre>
	 * "-n", "1,2", "3,4"
	 * isn't allowed
	 * </pre>
	 */
	// This is what's tested
	@SuppressWarnings("deprecation")
	@Test(expected = IllegalStateException.class)
	public void testArityCombinedWithSplitting()
	{
		integerArgument("-n").splitWith(",").arity(2);
	}

	@SuppressWarnings("deprecation")
	// This is what's tested
	@Test(expected = IllegalStateException.class)
	public void testVariableArityCombinedWithSplitting()
	{
		integerArgument("-n").splitWith(",").variableArity();
	}
}
