package se.j4j.argumentparser;

import static java.util.Arrays.asList;
import static org.fest.assertions.Assertions.assertThat;
import static se.j4j.argumentparser.ArgumentFactory.stringArgument;
import static se.j4j.argumentparser.limiters.FooLimiter.foos;

import java.util.Map;

import org.junit.Test;

import se.j4j.argumentparser.functions.AddBar;
import se.j4j.argumentparser.functions.AddFoo;
import se.j4j.argumentparser.limiters.FooLimiter;
import se.j4j.testlib.Explanation;

import com.google.common.base.Function;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Tests for {@link ArgumentBuilder#finalizeWith(Function)}
 */
public class FinalizerTest
{
	@Test
	public void testThatFinalizersAreCalledForParsedValues() throws ArgumentException
	{
		assertThat(stringArgument().finalizeWith(new AddBar()).parse("foo")).isEqualTo("foobar");
	}

	@Test
	public void testThatFinalizersAreCalledBeforeLimiters() throws ArgumentException
	{
		assertThat(stringArgument().limitTo(foos()).finalizeWith(new AddFoo()).parse("")).isEqualTo("foo");
	}

	@Test
	public void testThatFinalizersAreCalledForPropertyValues() throws ArgumentException
	{
		Map<String, String> map = stringArgument("-N").finalizeWith(new AddBar()).asPropertyMap().parse("-Nfoo=bar", "-Nbar=foo");

		assertThat(map.get("foo")).isEqualTo("barbar");
		assertThat(map.get("bar")).isEqualTo("foobar");
	}

	@Test
	public void testThatFinalizersAreCalledForValuesInLists() throws ArgumentException
	{
		assertThat(stringArgument().finalizeWith(new AddBar()).variableArity().parse("foo", "bar")).isEqualTo(asList("foobar", "barbar"));
	}

	@Test
	public void testThatFinalizersAreCalledForRepeatedValues() throws ArgumentException
	{
		assertThat(stringArgument("-n").finalizeWith(new AddBar()).repeated().parse("-n", "foo", "-n", "foo")).isEqualTo(asList("foobar", "foobar"));
	}

	@Test
	public void testThatFinalizersAreCalledForValuesFromSplit() throws ArgumentException
	{
		assertThat(stringArgument().finalizeWith(new AddBar()).splitWith(",").parse("foo,bar")).isEqualTo(asList("foobar", "barbar"));
	}

	@Test
	public void testThatDefaultValuesAreFinalized() throws ArgumentException
	{
		assertThat(stringArgument("-n").finalizeWith(new AddBar()).defaultValue("foo").parse()).isEqualTo("foobar");
	}

	@Test
	public void testThatDefaultValuesAreFinalizedForUsage()
	{
		assertThat(stringArgument("-n").finalizeWith(new AddFoo()).usage()).contains("Default: foo");
	}

	@Test(expected = IllegalStateException.class)
	@SuppressFBWarnings(value = "RV_RETURN_VALUE_IGNORED", justification = Explanation.FAIL_FAST)
	public void testThatDefaultValueIsFinalizedBeforeLimitIsChecked()
	{
		stringArgument("-n").defaultValue("foo").finalizeWith(new AddBar()).limitTo(new FooLimiter()).build();
	}
}
