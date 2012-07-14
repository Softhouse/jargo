package se.j4j.argumentparser;

import static org.fest.assertions.Assertions.assertThat;
import static se.j4j.argumentparser.ArgumentFactory.integerArgument;
import static se.j4j.argumentparser.ArgumentFactory.stringArgument;
import static se.j4j.argumentparser.utils.UsageTexts.expected;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Test;

import se.j4j.argumentparser.ArgumentExceptions.MissingParameterException;
import se.j4j.argumentparser.CommandLineParser.Arguments;
import se.j4j.argumentparser.CommandLineParser.ParsedArguments;

public class TestArityArguments
{
	// This is what's being tested
	@SuppressWarnings("deprecation")
	@Test(expected = IllegalStateException.class)
	public void testThatArityAndSplitWithIncompabilityIsEnforced()
	{
		integerArgument().arity(2).splitWith(",");
	}

	@Test
	public void testTwoParametersForNamedArgument() throws ArgumentException
	{
		String[] args = {"--numbers", "5", "6", "Rest", "Of", "Arguments"};

		Argument<List<Integer>> numbers = integerArgument("--numbers").arity(2).build();
		Argument<List<String>> unhandledArguments = stringArgument().variableArity().build();

		ParsedArguments parsed = CommandLineParser.forArguments(numbers, unhandledArguments).parse(args);

		assertThat(parsed.get(numbers)).isEqualTo(Arrays.asList(5, 6));
		assertThat(parsed.get(unhandledArguments)).isEqualTo(Arrays.asList("Rest", "Of", "Arguments"));
	}

	@Test
	public void testThatErrorMessageForMissingParameterLooksGoodForFixedArityArguments() throws ArgumentException
	{
		try
		{
			integerArgument("--numbers").arity(2).parse("--numbers", "5");
		}
		catch(MissingParameterException expected)
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
		String usage = CommandLineParser.forArguments(foo, bar, zoo, boo).usage("Arity");
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
		Arguments args = Arguments.forSingleArgument("foo");
		assertThat(args.nrOfRemainingArguments()).isEqualTo(1);
	}

}