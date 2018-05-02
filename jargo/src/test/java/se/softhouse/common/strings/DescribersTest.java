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
package se.softhouse.common.strings;

import com.google.common.testing.NullPointerTester;
import com.google.common.testing.NullPointerTester.Visibility;
import org.junit.Test;
import se.softhouse.common.strings.Describers.BooleanDescribers;
import se.softhouse.common.testlib.Locales;
import se.softhouse.common.testlib.ResourceLoader;

import java.io.File;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static com.google.common.collect.Maps.newLinkedHashMap;
import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;
import static org.fest.assertions.Assertions.*;
import static org.junit.Assert.fail;
import static se.softhouse.common.strings.Describers.asFunction;
import static se.softhouse.common.strings.Describers.booleanAsEnabledDisabled;
import static se.softhouse.common.strings.Describers.booleanAsOnOff;
import static se.softhouse.common.strings.Describers.characterDescriber;
import static se.softhouse.common.strings.Describers.fileDescriber;
import static se.softhouse.common.strings.Describers.mapDescriber;
import static se.softhouse.common.strings.Describers.numberDescriber;
import static se.softhouse.common.strings.Describers.toStringDescriber;
import static se.softhouse.common.strings.Describers.withConstantString;
import static se.softhouse.common.strings.StringsUtil.NEWLINE;
import static se.softhouse.common.testlib.Locales.TURKISH;

/**
 * Tests for {@link Describers}
 */
public class DescribersTest
{
	private static final Locale locale = Locale.getDefault();

	@Test
	public void testDescriberWithConstant()
	{
		Describer<Integer> constant = Describers.withConstantString("42");
		assertThat(constant.describe(1, locale)).isEqualTo("42");
		assertThat(constant.describe(2, locale)).isEqualTo("42");
		assertThat(constant.describe(42, locale)).isEqualTo("42");
	}

	@Test
	public void testThatDescribingNullYieldsNullString()
	{
		assertThat(toStringDescriber().describe(null, locale)).isEqualTo("null");
		assertThat(characterDescriber().describe(null, locale)).isEqualTo("null");
		assertThat(fileDescriber().describe(null, locale)).isEqualTo("null");
		assertThat(mapDescriber(toStringDescriber()).describe(null, locale)).isEqualTo("null");
	}

	@Test
	public void testThatToStringDescriberCallsToString()
	{
		assertThat(Describers.toStringDescriber().describe(42, locale)).isEqualTo("42");
	}

	@Test
	public void testCharacterDescriber()
	{
		assertThat(characterDescriber().describe((char) 0, locale)).isEqualTo("the Null character");
		assertThat(characterDescriber().describe('a', locale)).isEqualTo("a");
	}

	@Test
	public void testFileDescriber()
	{
		File file = new File("");
		assertThat(fileDescriber().describe(file, locale)).isEqualTo(file.getAbsolutePath());
	}

	@Test
	public void testThatListDescriberDescribesEachValueWithTheValueDescriber()
	{
		Describer<List<?>> describer = Describers.listDescriber(withConstantString("42"));

		assertThat(describer.describe(Arrays.asList(1, 2, 3), locale)).isEqualTo("42, 42, 42");
		assertThat(describer.describe(Collections.<Integer>emptyList(), locale)).isEqualTo("Empty list");
	}

	@Test
	public void testThatListDescriberSeparatesValuesWithValueSeparator()
	{
		Describer<List<?>> dotter = Describers.listDescriber(withConstantString("."), "");
		assertThat(dotter.describe(Arrays.asList(1, 2, 3), locale)).isEqualTo("...");
	}

	@Test
	public void testDescriberAsAFunction()
	{
		List<Boolean> booleans = asList(true, false);
		List<String> describedBooleans = booleans.stream().map(booleanAsEnabledDisabled()).collect(toList());
		assertThat(describedBooleans).isEqualTo(asList("enabled", "disabled"));
	}

	@Test
	public void testDescriberAsAFunctionWithSpecificLocale() throws InterruptedException
	{
		Locales.setDefault(Locales.SWEDISH);
		List<Integer> numbers = Arrays.asList(1000, 2000);

		List<String> describedNumbers = numbers.stream().map(asFunction(numberDescriber(), Locale.US)).collect(toList());
		assertThat(describedNumbers).isEqualTo(asList("1,000", "2,000"));
		Locales.resetDefaultLocale();
	}

	@Test
	public void testFunctionAsADescriber()
	{
		String describedBoolean = Describers.usingFunction(booleanAsEnabledDisabled()).describe(false, locale);
		assertThat(describedBoolean).isEqualTo("disabled");
	}

	@Test
	public void testBooleanAsOnOff()
	{
		List<Boolean> booleans = asList(true, false);
		List<String> describedBooleans = booleans.stream().map(booleanAsOnOff()).collect(toList());
		assertThat(describedBooleans).isEqualTo(asList("on", "off"));
	}

	@Test
	public void testThatNumberDescriberHandlesLocaleWell()
	{
		String shortInLocale = numberDescriber().describe(Short.MAX_VALUE, TURKISH);
		assertThat(shortInLocale).isEqualTo("32.767");
		String integerInLocale = numberDescriber().describe(Integer.MAX_VALUE, TURKISH);
		assertThat(integerInLocale).isEqualTo("2.147.483.647");
		String longInLocale = numberDescriber().describe(Long.MAX_VALUE, TURKISH);
		assertThat(longInLocale).isEqualTo("9.223.372.036.854.775.807");
		String bigIntegerInLocale = numberDescriber().describe(BigInteger.valueOf(1000), TURKISH);
		assertThat(bigIntegerInLocale).isEqualTo("1.000");
		String bigDecimalInLocale = numberDescriber().describe(BigDecimal.valueOf(1000), TURKISH);
		assertThat(bigDecimalInLocale).isEqualTo("1.000");
	}

	@Test
	public void testMapDescriber()
	{
		Map<String, Integer> defaults = newLinkedHashMap();
		defaults.put("population", 42);
		defaults.put("hello", 1);

		Map<String, String> descriptions = newLinkedHashMap();
		descriptions.put("population", "The number of citizens in the world");
		descriptions.put("hello", "The number of times to say hello");
		Describer<Map<String, Integer>> propertyDescriber = mapDescriber(descriptions);

		String describedMap = propertyDescriber.describe(defaults, locale);

		assertThat(describedMap).isEqualTo(ResourceLoader.get("/common/described-map.txt"));

		Map<Number, String> numberDescriptions = newLinkedHashMap();
		numberDescriptions.put(2000, "MM");
		Describer<Map<Number, String>> numberDescriber = mapDescriber(numberDescriptions, Describers.numberDescriber());
		assertThat(numberDescriber.describe(numberDescriptions, TURKISH)).contains("2.000=MM");
	}

	@Test
	public void testMapDescriberForValuesAndKeysWithCustomSeparator()
	{
		Map<String, Integer> values = newLinkedHashMap();
		values.put("anything", 73);
		values.put("everything", 42);

		// Test with custom valueDescriber, custom keyDescriber and custom separator
		Describer<Map<String, Integer>> customValueKeySeparator = mapDescriber(	Describers.<String>withConstantString("foo"),
																				Describers.<Integer>withConstantString("bar"), ":");
		String describedMap = customValueKeySeparator.describe(values, locale);
		assertThat(describedMap).isEqualTo("foo:bar" + NEWLINE + "foo:bar");

		assertThat(customValueKeySeparator.describe(Collections.<String, Integer>emptyMap(), locale)).isEqualTo("Empty map");

		// Test with custom valueDescriber and custom separator
		Describer<Map<String, String>> customValueAndSeparator = mapDescriber(Describers.<String>withConstantString("foo"), ":");
		Map<String, String> valuesCustomValueAndSeparator = newLinkedHashMap();
		valuesCustomValueAndSeparator.put("hello", "world");
		assertThat(customValueAndSeparator.describe(valuesCustomValueAndSeparator, locale)).contains("hello:foo");

		// Test with custom valueDescriber
		Describer<Map<String, String>> customValue = mapDescriber(Describers.<String>withConstantString("foo"));
		assertThat(customValue.describe(valuesCustomValueAndSeparator, locale)).contains("hello=foo");

		// Test with custom valueDescriber, custom keyDescriber
		Describer<Map<String, Integer>> customValueAndKey = mapDescriber(	Describers.<String>withConstantString("foo"),
																			Describers.<Integer>withConstantString("bar"));
		assertThat(customValueAndKey.describe(values, locale)).contains("foo=bar" + NEWLINE + "foo=bar");
	}

	@Test
	public void testThatMissingKeyThrows()
	{
		Map<String, Integer> map = newLinkedHashMap();
		map.put("population", 42);

		Map<String, String> noDescriptions = newLinkedHashMap(); // No description of population
		Describer<Map<String, Integer>> describer = mapDescriber(noDescriptions);
		try
		{
			describer.describe(map, locale);
			fail("population should have to be described");
		}
		catch(IllegalArgumentException expected)
		{
			assertThat(expected).hasMessage("Undescribed key: population");
		}
	}

	@Test
	public void testToStringStrings()
	{
		assertThat(BooleanDescribers.valueOf("ON_OFF").toString()).isEqualTo("ON_OFF");
		assertThat(numberDescriber().toString()).isEqualTo("NumberDescriber");
	}

	@Test
	public void testThatNullContractsAreFollowed() throws Exception
	{
		new NullPointerTester().testStaticMethods(Describers.class, Visibility.PACKAGE);
	}
}
