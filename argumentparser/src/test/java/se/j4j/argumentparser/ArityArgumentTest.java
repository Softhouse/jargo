package se.j4j.argumentparser;

import static java.util.Arrays.asList;
import static junit.framework.Assert.fail;
import static org.fest.assertions.Assertions.assertThat;
import static se.j4j.argumentparser.ArgumentFactory.integerArgument;
import static se.j4j.argumentparser.ArgumentFactory.stringArgument;
import static se.j4j.argumentparser.utils.ExpectedTexts.expected;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Test;

import se.j4j.argumentparser.CommandLineParser.ArgumentIterator;
import se.j4j.argumentparser.CommandLineParser.ParsedArguments;
import se.j4j.testlib.Explanation;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

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
			assertThat(expected).hasMessage("Arity requires at least 2 parameters (got 1)");
		}
	}

	@Test
	public void testThatErrorMessageForMissingParameterLooksGoodForFixedArityArguments()
	{
		try
		{
			integerArgument("--numbers").arity(2).parse("--numbers", "5");
		}
		catch(ArgumentException expected)
		{
			assertThat(expected.getMessageAndUsage("MissingSecondParameter")).isEqualTo(expected("missingSecondParameter"));
		}
	}

	@Test
	public void testThatUsageTextForArityLooksGood()
	{
		Argument<List<String>> foo = stringArgument("--foo").arity(3).description("MetaDescShouldBeDisplayedThreeTimes").build();
		Argument<List<Integer>> bar = integerArgument("--bar").arity(2).description("MetaDescShouldBeDisplayedTwoTimes").build();
		Argument<List<Integer>> zoo = integerArgument("--zoo").variableArity().description("MetaDescShouldIndicateVariableAmount").build();
		Argument<List<Integer>> boo = integerArgument().variableArity().description("MetaDescShouldIndicateVariableAmount").build();
		String usage = CommandLineParser.withArguments(foo, bar, zoo, boo).usage("Arity");
		assertThat(usage).isEqualTo(expected("metaDescriptionsForArityArgument"));
	}

	@Test
	public void testUsageTextForEmptyList()
	{
		String usage = stringArgument().arity(2).defaultValue(Collections.<String>emptyList()).usage("DefaultEmptyList");
		assertThat(usage).contains("Default: Empty list");
	}

	@Test
	public void testThatNrOfRemainingArgumentsGivesTheCorrectCapacity()
	{
		ArgumentIterator args = ArgumentIterator.forSingleArgument("foo");
		assertThat(args.nrOfRemainingArguments()).isEqualTo(1);
	}

	@Test(expected = IllegalArgumentException.class)
	@SuppressFBWarnings(value = "RV_RETURN_VALUE_IGNORED", justification = Explanation.FAIL_FAST)
	public void testThatTwoUnnamedVariableArityArgumentsIsIllegal()
	{
		// This is illegal because the parser couldn't possibly know when the integerArgument ends
		// and the stringArgument begins
		Argument<List<Integer>> numbers = integerArgument().variableArity().build();
		Argument<List<String>> strings = stringArgument().variableArity().build();

		CommandLineParser.withArguments(numbers, strings);
	}
}