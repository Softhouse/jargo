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

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Range;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.junit.Test;
import se.softhouse.common.testlib.Explanation;
import se.softhouse.jargo.internal.Texts.ProgrammaticErrors;
import se.softhouse.jargo.internal.Texts.UserErrors;
import se.softhouse.jargo.limiters.LimiterTest;
import se.softhouse.jargo.stringparsers.custom.LimitedKeyParser;
import se.softhouse.jargo.stringparsers.custom.ObjectParser;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Maps.newLinkedHashMap;
import static java.lang.String.format;
import static java.util.Arrays.asList;
import static org.fest.assertions.Assertions.assertThat;
import static org.fest.assertions.Fail.fail;
import static se.softhouse.common.strings.Describers.withConstantString;
import static se.softhouse.jargo.Arguments.byteArgument;
import static se.softhouse.jargo.Arguments.integerArgument;
import static se.softhouse.jargo.Arguments.stringArgument;
import static se.softhouse.jargo.StringParsers.byteParser;
import static se.softhouse.jargo.StringParsers.integerParser;
import static se.softhouse.jargo.internal.Texts.UserErrors.DISALLOWED_PROPERTY_VALUE;
import static se.softhouse.jargo.limiters.FooLimiter.foos;
import static se.softhouse.jargo.utils.ExpectedTexts.expected;
import static se.softhouse.jargo.utils.Assertions2.assertThat;

/**
 * Tests for {@link ArgumentBuilder#asPropertyMap()} and
 * {@link ArgumentBuilder#asKeyValuesWithKeyParser(StringParser)}
 */
public class PropertyMapTest
{
	@Test
	public void testSeveralValues() throws ArgumentException
	{
		Map<String, Integer> numberMap = integerArgument("-N").asPropertyMap().parse("-None=1", "-Ntwo=2");

		assertThat(numberMap.get("one")).isEqualTo(1);
		assertThat(numberMap.get("two")).isEqualTo(2);
	}

	@Test
	public void testStartsWithCollisionsForTwoSimiliarIdentifiers() throws ArgumentException
	{
		Map<String, Integer> numberMap = integerArgument("-NS", "-N").asPropertyMap().parse("-None=1", "-NStwo=2");

		assertThat(numberMap.get("one")).isEqualTo(1);
		assertThat(numberMap.get("two")).isEqualTo(2);

		numberMap = integerArgument("-N", "-NS").asPropertyMap().parse("-None=1", "-NStwo=2");

		assertThat(numberMap.get("one")).isEqualTo(1);
		assertThat(numberMap.get("two")).isEqualTo(2);

		numberMap = integerArgument("-N", "-D").asPropertyMap().parse("-Done=1", "-Ntwo=2");

		assertThat(numberMap.get("one")).isEqualTo(1);
		assertThat(numberMap.get("two")).isEqualTo(2);
	}

	@Test
	public void testNameCollisionWithOrdinaryArgument() throws ArgumentException
	{
		Argument<Map<String, Integer>> numberMap = integerArgument("-N").asPropertyMap().build();
		Argument<Integer> number = integerArgument("-N").ignoreCase().build();

		ParsedArguments parsed = CommandLineParser.withArguments(numberMap, number).parse("-None=1", "-Ntwo=2", "-N", "3");

		assertThat(parsed.get(numberMap).get("one")).isEqualTo(1);
		assertThat(parsed.get(numberMap).get("two")).isEqualTo(2);
		assertThat(parsed.get(number)).isEqualTo(3);
	}

	@Test
	@SuppressFBWarnings(value = "RV_RETURN_VALUE_IGNORED", justification = Explanation.FAIL_FAST)
	public void testPropertyMapWithoutLeadingIdentifier()
	{
		try
		{
			integerArgument().asPropertyMap().build();
			fail("a property map must be prefixed with an identifier otherwise it would consume all arguments");
		}
		catch(IllegalArgumentException expected)
		{
			assertThat(expected).hasMessage(ProgrammaticErrors.NO_NAME_FOR_PROPERTY_MAP);
		}
	}

	@Test
	public void testInvalidationOfWrongSeparator()
	{
		try
		{
			integerArgument("-N").asPropertyMap().parse("-N3");
			fail("-N3 should be missing a =");
		}
		catch(ArgumentException expected)
		{
			assertThat(expected).hasMessage(String.format(UserErrors.MISSING_KEY_VALUE_SEPARATOR, "-N", "3", "="));
		}
	}

	@Test
	public void testCustomSeparator() throws ArgumentException
	{
		Map<String, Integer> numberMap = integerArgument("-N").separator("/").asPropertyMap().parse("-Nkey/3");

		assertThat(numberMap.get("key")).isEqualTo(3);
	}

	@Test
	public void testLimitationOfPropertyValues() throws ArgumentException
	{
		Argument<Map<String, String>> fooArgument = stringArgument("-N").limitTo(foos()).asPropertyMap().build();

		CommandLineParser parser = CommandLineParser.withArguments(fooArgument);
		ParsedArguments parsed = parser.parse("-Nbar=foo");

		assertThat(parsed.get(fooArgument).get("bar")).isEqualTo("foo");

		try
		{
			parser.parse("-Nbar=bar");
			fail("bar should not be a valid value");
		}
		catch(ArgumentException expected)
		{
			assertThat(expected).hasMessage(format(DISALLOWED_PROPERTY_VALUE, "bar", "bar", "foo"));
		}
	}

	@Test(expected = ArgumentException.class)
	public void testLimitationOfPropertyMapKeys() throws ArgumentException
	{
		integerArgument("-I").asKeyValuesWithKeyParser(new LimitedKeyParser("foo", "bar")).parse("-Ifoo=10", "-Ibar=5", "-Izoo=9");
	}

	@Test
	public void testLimitationOfPropertyMapKeysAndValues()
	{
		Predicate<Integer> zeroToTen = LimiterTest.java(Range.closed(0, 10));
		Argument<Map<String, Integer>> argument = integerArgument("-I").limitTo(zeroToTen)
				.asKeyValuesWithKeyParser(new LimitedKeyParser("foo", "bar")).build();

		CommandLineParser parser = CommandLineParser.withArguments(argument);

		try
		{
			parser.parse("-Ifoo=1", "-Ibar=2", "-Izoo=3");
			fail("Didn't invalidate zoo key");
		}
		catch(ArgumentException expected)
		{
			Usage usage = expected.getMessageAndUsage();
			assertThat(usage).isEqualTo(expected("limiterUsageForBothValueLimiterAndKeyLimiter"));
		}

		try
		{
			parser.parse("-Ifoo=1", "-Ibar=-1");
			fail("Didn't invalidate bar with negative value");
		}
		catch(ArgumentException invalidBar)
		{
			assertThat(invalidBar).hasMessage(format(DISALLOWED_PROPERTY_VALUE, "bar", -1, zeroToTen));
		}
	}

	@Test
	public void testThatPropertyValuesDefaultToAnUnmodifiableEmptyMap() throws ArgumentException
	{
		Map<String, Integer> defaultMap = integerArgument("-N").asPropertyMap().parse();

		assertThat(defaultMap).isEmpty();
		try
		{
			defaultMap.put("a", 42);
			fail("the defaultMap should be unmodifiable");
		}
		catch(UnsupportedOperationException expected)
		{
		}
	}

	@Test
	public void testCustomKeyParser() throws ArgumentException
	{
		assertThat(integerArgument("-N").asKeyValuesWithKeyParser(integerParser()).parse("-N1=42").get(1)).isEqualTo(42);
	}

	@Test
	public void testThatPropertyMapsAreUnmodifiable() throws ArgumentException
	{
		Map<String, Integer> numberMap = integerArgument("-N").asPropertyMap().parse("-None=1");
		try
		{
			numberMap.put("two", 2);
			fail("a propertyMap should be unmodifiable");
		}
		catch(UnsupportedOperationException expected)
		{
		}
	}

	@Test
	public void testThatPropertyMapsWithRepeatedValuesAreUnmodifiable() throws ArgumentException
	{
		Map<String, List<Integer>> numberMap = integerArgument("-N").repeated().asPropertyMap().parse("-Nfoo=1", "-Nfoo=2");
		try
		{
			numberMap.put("bar", Arrays.asList(1, 2));
			fail("a propertyMap should be unmodifiable");
		}
		catch(UnsupportedOperationException expected)
		{
		}
		try
		{
			numberMap.get("foo").add(3);
			fail("a list inside a propertyMap should be unmodifiable");
		}
		catch(UnsupportedOperationException expected)
		{
		}
	}

	@SuppressWarnings("deprecation")
	@Test(expected = IllegalStateException.class)
	public void testThatRepeatedMustBeCalledBeforeAsPropertyMap()
	{
		integerArgument("-N").asPropertyMap().repeated();
	}

	@SuppressWarnings("deprecation")
	@Test(expected = IllegalStateException.class)
	public void testThatArityMustBeCalledBeforeAsPropertyMap()
	{
		integerArgument("-N").asPropertyMap().arity(2);
	}

	@SuppressWarnings("deprecation")
	@Test(expected = IllegalStateException.class)
	public void testThatVariableArityAndAsPropertyMapIsIncompatible()
	{
		integerArgument("-N").asPropertyMap().variableArity();
	}

	@Test
	public void testThatIterationOrderForPropertyKeysIsTheSameAsFromTheCommandLine() throws ArgumentException
	{
		Map<String, Integer> map = integerArgument("-I").asPropertyMap().parse("-Itldr=7", "-Ifoo=10", "-Ibar=5");
		List<String> keys = newArrayList();
		for(String key : map.keySet())
		{
			keys.add(key);
		}
		assertThat(keys).isEqualTo(asList("tldr", "foo", "bar"));
	}

	@Test
	public void testDefaultValuesInUsageForPropertyMap()
	{
		Map<Byte, Byte> defaults = newLinkedHashMap();
		defaults.put((byte) 3, (byte) 4);
		defaults.put((byte) 1, (byte) 2);

		Usage usage = byteArgument("-N").asKeyValuesWithKeyParser(byteParser()).separator(":").defaultValue(defaults).usage();
		assertThat(usage).isEqualTo(expected("defaultValuePropertyMap"));
	}

	@Test
	public void testThatSeparatorsWithSeveralCharactersArePossible() throws ArgumentException
	{
		String value = stringArgument("-N").asPropertyMap().separator("==").parse("-Nkey==value").get("key");
		assertThat(value).isEqualTo("value");
	}

	@Test
	@SuppressFBWarnings(value = "RV_RETURN_VALUE_IGNORED", justification = Explanation.FAIL_FAST)
	public void testThatZeroCharacterSeparatorIsForbidden()
	{
		try
		{
			stringArgument("-N").separator("").asPropertyMap().build();
			fail("an empty separator must be forbidden, otherwise it would be impossible to distinguish where the key ends and the value starts");
		}
		catch(IllegalArgumentException expected)
		{
			assertThat(expected).hasMessage(ProgrammaticErrors.EMPTY_SEPARATOR);
		}
	}

	@Test
	public void testThatUsageTextForRepeatedPropertyValuesLooksGood()
	{
		Usage usage = integerArgument("-N").repeated().asPropertyMap().description("Some measurement values").usage();
		assertThat(usage).isEqualTo(expected("repeatedPropertyValues"));
	}

	@Test
	public void testSeparatorInName() throws ArgumentException
	{
		Integer ten = integerArgument("-N;").separator(";").asPropertyMap().parse("-N;foo;10").get("foo");
		assertThat(ten).isEqualTo(10);
	}

	@Test
	public void testLimitWithForRepeatedPropertyValues()
	{
		try
		{
			stringArgument("-N").limitTo(foos()).repeated().asPropertyMap().parse("-Nkey=foo", "-Nkey=bar");
		}
		catch(ArgumentException expected)
		{
			assertThat(expected).hasMessage("'bar' is not foo");
		}
	}

	@Test
	public void testArityAndPropertyMap() throws ArgumentException
	{
		// A bit crazy but it is supported
		Map<String, List<Integer>> value = integerArgument("-D").arity(2).asPropertyMap().parse("-Dt=1", "2");
		assertThat(value.get("t")).isEqualTo(Arrays.asList(1, 2));
	}

	@Test
	public void testDefaultValueForOnePropertyAndCommandLineValueForOneProperty() throws ArgumentException
	{
		Map<String, Integer> defaults = ImmutableMap.<String, Integer>builder().put("n", 10).build();

		Map<String, Integer> values = integerArgument("-D").asPropertyMap().defaultValue(defaults).parse("-Ds=20");

		assertThat(values.get("s")).isEqualTo(20);
		assertThat(values.get("n")).isEqualTo(10);
	}

	@Test
	public void testOverridingDefaultValue() throws ArgumentException
	{
		Map<String, Integer> defaults = ImmutableMap.<String, Integer>builder().put("n", 1).build();

		int n = integerArgument("-D").asPropertyMap().defaultValue(defaults).parse("-Dn=2").get("n");

		assertThat(n).isEqualTo(2);
	}

	@Test
	public void testThatCustomDescriberCanBeUsedForPropertyMaps()
	{
		Usage usage = integerArgument("-D").asPropertyMap().defaultValueDescriber(withConstantString("a map")).usage();
		assertThat(usage).contains("Default: a map");
	}

	@Test
	public void testThatSeparatorIsNotInSuggestions() throws Exception
	{
		try
		{
			integerArgument("-n").asPropertyMap().parse("-a");
			fail("-a should be suggested to -n");
		}
		catch(ArgumentException expected)
		{
			assertThat(expected).hasMessage(String.format(UserErrors.SUGGESTION, "-a", "-n"));
		}
	}

	@Test
	public void testThatSystemPropertiesCanBeUsedAsTargetMap() throws Exception
	{
		Map<Object, Object> map = Arguments.withParser(new ObjectParser()).names("-D").asKeyValuesWithKeyParser(new ObjectParser())
				.defaultValueSupplier(() -> System.getProperties()).parse("-Dsys.prop.test=foo");

		assertThat(map.get("sys.prop.test")).isEqualTo("foo");
		assertThat(System.getProperty("sys.prop.test")).isEqualTo("foo");
		assertThat(map.get("os.name")).as("Should delegate to system properties when not specified") //
				.isEqualTo(System.getProperty("os.name"));
	}
}
