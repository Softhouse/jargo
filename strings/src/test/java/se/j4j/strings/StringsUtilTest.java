package se.j4j.strings;

import static java.util.Arrays.asList;
import static org.fest.assertions.Assertions.assertThat;
import static se.j4j.strings.StringsUtil.closestMatch;
import static se.j4j.strings.StringsUtil.numberToPositionalString;
import static se.j4j.strings.StringsUtil.spaces;
import static se.j4j.strings.StringsUtil.toLowerCase;

import java.util.List;

import org.junit.Test;

import se.j4j.testlib.Explanation;
import se.j4j.testlib.UtilityClassTester;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

public class StringsUtilTest
{
	@Test
	public void testThatSpacesCreatesFiveSpaces()
	{
		assertThat(spaces(5)).isEqualTo("     ");
	}

	@Test
	public void testFuzzyMatching()
	{
		List<String> strings = asList("logging", "help", "status");
		assertThat(closestMatch("stats", strings)).isEqualTo("status");
	}

	@Test
	public void testThatShortestStringIsReturnedForEmptyInput()
	{
		List<String> strings = asList("logging", "help", "status");
		assertThat(closestMatch("", strings)).isEqualTo("help");
	}

	@Test
	public void testThatEmptyStringIsReturnedForSmallInput()
	{
		List<String> strings = asList("--logging", "--help", "");
		assertThat(closestMatch("s", strings)).isEqualTo("");
	}

	@Test(expected = IllegalArgumentException.class)
	@SuppressFBWarnings(value = "RV_RETURN_VALUE_IGNORED", justification = Explanation.FAIL_FAST)
	public void testThatNoValidOptionsIsIllegal()
	{
		List<String> strings = asList();
		closestMatch("", strings);
	}

	@Test
	public void testToLowerCase()
	{
		List<String> strings = asList("ABC", "Def");
		assertThat(toLowerCase(strings)).isEqualTo(asList("abc", "def"));
	}

	@Test
	public void testTextsForPositionalNumbers()
	{
		assertThat(numberToPositionalString(0)).isEqualTo("zeroth");
		assertThat(numberToPositionalString(1)).isEqualTo("first");
		assertThat(numberToPositionalString(2)).isEqualTo("second");
		assertThat(numberToPositionalString(3)).isEqualTo("third");
		assertThat(numberToPositionalString(4)).isEqualTo("fourth");
		assertThat(numberToPositionalString(5)).isEqualTo("fifth");
		assertThat(numberToPositionalString(6)).isEqualTo("6th");
	}

	@Test(expected = IllegalArgumentException.class)
	public void testThatNegativeNumberCantBePositional()
	{
		numberToPositionalString(-1);
	}

	@Test
	public void testUtilityClassDesign()
	{
		UtilityClassTester.testUtilityClassDesign(StringsUtil.class);
	}
}