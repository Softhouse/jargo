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

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.junit.Test;
import se.softhouse.common.testlib.Explanation;
import se.softhouse.jargo.functions.AddBar;
import se.softhouse.jargo.functions.AddFoo;
import se.softhouse.jargo.limiters.FooLimiter;

import java.util.Map;

import static java.util.Arrays.asList;
import static org.fest.assertions.Assertions.assertThat;
import static se.softhouse.jargo.Arguments.stringArgument;
import static se.softhouse.jargo.limiters.FooLimiter.foos;
import static se.softhouse.jargo.utils.Assertions2.assertThat;

/**
 * Tests for {@link ArgumentBuilder#finalizeWith(java.util.function.Function)}
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

	@Test(expected = IllegalArgumentException.class)
	@SuppressFBWarnings(value = "RV_RETURN_VALUE_IGNORED", justification = Explanation.FAIL_FAST)
	public void testThatDefaultValueIsFinalizedBeforeLimitIsChecked()
	{
		stringArgument("-n").defaultValue("foo").finalizeWith(new AddBar()).limitTo(new FooLimiter()).build();
	}
}
