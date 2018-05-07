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
import static org.fest.assertions.Assertions.assertThat;
import static org.junit.Assert.fail;
import static se.softhouse.jargo.Arguments.integerArgument;
import static se.softhouse.jargo.Arguments.stringArgument;
import static se.softhouse.jargo.utils.Assertions2.assertThat;
import static se.softhouse.jargo.utils.ExpectedTexts.expected;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.junit.Test;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import se.softhouse.common.testlib.Explanation;
import se.softhouse.jargo.internal.Texts.ProgrammaticErrors;
import se.softhouse.jargo.internal.Texts.UserErrors;

/**
 * Tests for {@link ArgumentBuilder#arity(int)} and {@link ArgumentBuilder#variableArity()}
 */
public class ArityArgumentTest
{
	@Test
	public void testTwoParametersForNamedArgument() throws ArgumentException
	{
		String[] args = {"--numbers", "5", "6", "Rest", "Of", "Arguments"};

		Argument<List<Integer>> numbers = integerArgument("--numbers").arity(2).build();
		Argument<List<String>> unhandledArguments = stringArgument().variableArity().build();

		ParsedArguments parsed = CommandLineParser.withArguments(numbers, unhandledArguments).parse(args);

		assertThat(parsed.get(numbers)).isEqualTo(Arrays.asList(5, 6));
		assertThat(parsed.get(unhandledArguments)).isEqualTo(Arrays.asList("Rest", "Of", "Arguments"));
	}

	@Test
	public void testVariableArityForNamedArgument() throws ArgumentException
	{
		assertThat(integerArgument("--numbers").variableArity().parse("--numbers", "5", "6")).isEqualTo(asList(5, 6));
	}

	@Test(expected = UnsupportedOperationException.class)
	public void testThatParsedValuesIsWrappedInAnUnmodifiableList() throws ArgumentException
	{
		integerArgument("--numbers").variableArity().parse("--numbers", "2", "3").add(1);
	}

	// This is what's being tested
	@SuppressWarnings("deprecation")
	@Test(expected = IllegalStateException.class)
	public void testThatArityAndSplitWithIncompabilityIsEnforced()
	{
		integerArgument().arity(2).splitWith(",");
	}

	@Test
	public void testThatArityOfOneIsForbidden()
	{
		try
		{
			integerArgument().arity(1);
			fail("Arity should require at least 2 parameters");
		}
		catch(IllegalArgumentException expected)
		{
			assertThat(expected).hasMessage(String.format(ProgrammaticErrors.TO_SMALL_ARITY, 1));
		}
	}

	@Test
	public void testThatErrorMessageForMissingParameterLooksGoodForFixedArityArguments()
	{
		try
		{
			integerArgument("--numbers").arity(2).parse("--numbers", "5");
			fail("Missing integer not detected");
		}
		catch(ArgumentException expected)
		{
			assertThat(expected).hasMessage(String.format(UserErrors.MISSING_NTH_PARAMETER, "second", "<integer>", "--numbers"));
		}
	}

	@Test
	public void testThatUsageTextForArityLooksGood()
	{
		Argument<List<String>> foo = stringArgument("--foo").arity(3).description("MetaDescShouldBeDisplayedThreeTimes").build();
		Argument<List<Integer>> bar = integerArgument("--bar").arity(2).description("MetaDescShouldBeDisplayedTwoTimes").build();
		Argument<List<Integer>> zoo = integerArgument("--zoo").variableArity().description("MetaDescShouldIndicateVariableAmount").build();
		Argument<Integer> trans = integerArgument("--trans").variableArity().transform(l -> l.size())
				.description("MetaDescShouldIndicateVariableAmount").build();
		Argument<Integer> transTwo = integerArgument("--trans-two").arity(2).transform(l -> l.size()).description("MetaDescShouldBeDisplayedTwoTimes")
				.build();
		Argument<List<Integer>> boo = integerArgument().variableArity().description("MetaDescShouldIndicateVariableAmount").build();
		Usage usage = CommandLineParser.withArguments(foo, bar, zoo, trans, transTwo, boo).usage();
		assertThat(usage).isEqualTo(expected("metaDescriptionsForArityArgument"));
	}

	@Test
	public void testUsageTextForEmptyList()
	{
		Usage usage = stringArgument().arity(2).defaultValue(Collections.<String>emptyList()).usage();
		assertThat(usage).contains("Default: Empty list");
	}

	@Test
	public void testThatNrOfRemainingArgumentsGivesTheCorrectCapacity()
	{
		ArgumentIterator args = ArgumentIterator.forArguments(Arrays.asList("foo"), Collections.<String, Argument<?>>emptyMap());
		assertThat(args.nrOfRemainingArguments()).isEqualTo(1);
		args.next(); // Consume one argument
		assertThat(args.nrOfRemainingArguments()).isEqualTo(0);
	}

	@Test
	@SuppressFBWarnings(value = "RV_RETURN_VALUE_IGNORED", justification = Explanation.FAIL_FAST)
	public void testThatTwoUnnamedVariableArityArgumentsIsIllegal()
	{
		// This is illegal because the parser couldn't possibly know when the integerArgument ends
		// and the stringArgument begins
		Argument<List<Integer>> numbers = integerArgument().variableArity().build();
		Argument<List<String>> strings = stringArgument().variableArity().build();

		try
		{
			CommandLineParser.withArguments(numbers, strings);
			fail("several variable arity parsers should be forbidden");
		}
		catch(IllegalArgumentException expected)
		{
			assertThat(expected).hasMessage(String.format(ProgrammaticErrors.SEVERAL_VARIABLE_ARITY_PARSERS, "[<integer>, <string>]"));
		}
	}

	@Test
	public void testThatArityCanBeTransformedToUniqueValues()
	{
		Set<Integer> numbers = integerArgument().variableArity().unique().parse("123", "24", "123");
		assertThat(numbers).containsOnly(123, 24);
	}
}
