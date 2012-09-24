package se.j4j.argumentparser;

import static junit.framework.Assert.fail;
import static org.fest.assertions.Assertions.assertThat;
import static se.j4j.argumentparser.ArgumentFactory.integerArgument;
import static se.j4j.argumentparser.ArgumentFactory.optionArgument;

import org.junit.Test;

import se.j4j.testlib.Explanation;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Tests for {@link ArgumentBuilder#required()}
 */
public class RequiredArgumentTest
{
	@Test
	public void testMissingRequiredNamedArgument()
	{
		Argument<Integer> number = integerArgument("--number").required().build();
		Argument<Integer> number2 = integerArgument("--number2").required().build();

		try
		{
			CommandLineParser.withArguments(number, number2).parse();
			fail("Required argument silently ignored");
		}
		catch(ArgumentException e)
		{
			assertThat(e).hasMessage("Missing required arguments: [--number, --number2]");
		}
	}

	@Test
	public void testMissingRequiredIndexedArgument()
	{
		Argument<Integer> number = integerArgument().metaDescription("numberOne").required().build();
		Argument<Integer> number2 = integerArgument().metaDescription("numberTwo").required().build();

		try
		{
			CommandLineParser.withArguments(number, number2).parse();
			fail("Required argument silently ignored");
		}
		catch(ArgumentException e)
		{
			assertThat(e).hasMessage("Missing required arguments: [numberOne, numberTwo]");
		}
	}

	@Test(expected = ArgumentException.class)
	public void testThatRequiredArgumentsIsResetBetweenParsings() throws ArgumentException
	{
		Argument<Integer> required = integerArgument("-n").required().build();
		try
		{
			required.parse("-n", "1");
		}
		catch(ArgumentException e)
		{
			fail("Parser failed to handle required argument");
		}
		required.parse(); // The second time shouldn't be affected by the first
	}

	@SuppressWarnings("deprecation")
	// This is what's tested
	@Test(expected = IllegalStateException.class)
	public void testMakingAnOptionalArgumentRequired()
	{
		optionArgument("-l").required();
	}

	@Test(expected = IllegalStateException.class)
	@SuppressFBWarnings(value = "RV_RETURN_VALUE_IGNORED", justification = Explanation.FAIL_FAST)
	public void testSettingADefaultValueForARequiredArgument()
	{
		integerArgument("-l").required().defaultValue(42);
	}

	@Test(expected = IllegalStateException.class)
	@SuppressFBWarnings(value = "RV_RETURN_VALUE_IGNORED", justification = Explanation.FAIL_FAST)
	public void testMakingARequiredArgumentWithDefaultValue()
	{
		integerArgument("-l").defaultValue(42).required();
	}
}