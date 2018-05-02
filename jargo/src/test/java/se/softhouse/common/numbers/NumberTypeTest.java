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
package se.softhouse.common.numbers;

import static java.lang.String.format;
import static org.fest.assertions.Assertions.assertThat;
import static org.fest.assertions.Fail.fail;
import static se.softhouse.common.numbers.NumberType.BIG_DECIMAL;
import static se.softhouse.common.numbers.NumberType.BIG_INTEGER;
import static se.softhouse.common.numbers.NumberType.BYTE;
import static se.softhouse.common.numbers.NumberType.INTEGER;
import static se.softhouse.common.numbers.NumberType.LONG;
import static se.softhouse.common.numbers.NumberType.OUT_OF_RANGE;
import static se.softhouse.common.numbers.NumberType.SHORT;
import static se.softhouse.common.strings.StringsUtil.NEWLINE;
import static se.softhouse.common.testlib.Locales.SWEDISH;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import org.junit.Test;

import com.google.common.testing.NullPointerTester;
import com.google.common.testing.NullPointerTester.Visibility;

/**
 * Tests for {@link NumberType}
 */
public class NumberTypeTest
{
	static final BigInteger biggerThanLong = BigInteger.valueOf(Long.MAX_VALUE).add(BigInteger.ONE);

	@Test
	public void testMappingOfByteFields()
	{
		assertThat(BYTE.maxValue()).isEqualTo(Byte.MAX_VALUE);
		assertThat(BYTE.minValue()).isEqualTo(Byte.MIN_VALUE);
	}

	@Test
	public void testMappingOfShortFields()
	{
		assertThat(SHORT.maxValue()).isEqualTo(Short.MAX_VALUE);
		assertThat(SHORT.minValue()).isEqualTo(Short.MIN_VALUE);
	}

	@Test
	public void testMappingOfIntegerFields()
	{
		assertThat(INTEGER.maxValue()).isEqualTo(Integer.MAX_VALUE);
		assertThat(INTEGER.minValue()).isEqualTo(Integer.MIN_VALUE);
	}

	@Test
	public void testMappingOfLongFields()
	{
		assertThat(LONG.maxValue()).isEqualTo(Long.MAX_VALUE);
		assertThat(LONG.minValue()).isEqualTo(Long.MIN_VALUE);
	}

	@Test
	public void testNumbersAtBounds()
	{
		for(NumberType<?> type : NumberType.TYPES)
		{
			assertThat(type.parse("0").intValue()).isZero();
			assertThat(type.parse("1").intValue()).isEqualTo(1);
			assertThat(type.parse("-1").intValue()).isEqualTo(-1);

			if(NumberType.UNLIMITED_TYPES.contains(type))
			{
				testUnlimitedType(type);
			}
			else
			{
				// Numbers at the bounds
				assertThat(type.parse(type.minValue().toString())).isEqualTo(type.minValue());
				assertThat(type.parse(type.maxValue().toString())).isEqualTo(type.maxValue());
			}

			// Default values
			assertThat(type.defaultValue().intValue()).isZero();
		}
	}

	private void testUnlimitedType(NumberType<?> type)
	{
		try
		{
			type.minValue();
			fail("Unlimited types should not support minValue");
		}
		catch(UnsupportedOperationException expected)
		{

		}
		try
		{
			type.maxValue();
			fail("Unlimited types should not support maxValue");
		}
		catch(UnsupportedOperationException expected)
		{

		}
	}

	@Test
	public void testThatParseCanHandleLargeNumbers() throws Exception
	{
		BigInteger bigInteger = BIG_INTEGER.parse(biggerThanLong.toString());
		assertThat(bigInteger).isEqualTo(biggerThanLong);

		BigDecimal biggerThanLongWithFraction = new BigDecimal(biggerThanLong).add(new BigDecimal(2.5));

		BigDecimal bigDecimal = BIG_DECIMAL.parse(biggerThanLongWithFraction.toString());
		assertThat(bigDecimal).isEqualTo(biggerThanLongWithFraction);
	}

	@Test
	public void testThatDescriptionOfValidValuesReturnsHumanReadableStrings() throws Exception
	{
		assertThat(BYTE.descriptionOfValidValues(SWEDISH)).isEqualTo("-128 to 127");
		assertThat(SHORT.descriptionOfValidValues(SWEDISH)).isEqualTo("-32 768 to 32 767");
		assertThat(INTEGER.descriptionOfValidValues(SWEDISH)).isEqualTo("-2 147 483 648 to 2 147 483 647");
		assertThat(LONG.descriptionOfValidValues(SWEDISH)).isEqualTo("-9 223 372 036 854 775 808 to 9 223 372 036 854 775 807");
		assertThat(BIG_INTEGER.descriptionOfValidValues(SWEDISH)).isEqualTo("an arbitrary integer number (practically no limits)");
		assertThat(BIG_DECIMAL.descriptionOfValidValues(SWEDISH)).isEqualTo("an arbitrary decimal number (practically no limits)");
	}

	@Test(expected = IllegalArgumentException.class)
	public void testThatEmptyInputThrows() throws Exception
	{
		BYTE.parse("");
	}

	@Test
	public void testThatNumbersTruncatesFractions() throws Exception
	{
		assertThat(BYTE.from(2.5)).isEqualTo((byte) 2);
		assertThat(SHORT.from(2.5)).isEqualTo((short) 2);
		assertThat(INTEGER.from(2.5)).isEqualTo(2);
		assertThat(LONG.from(2.5)).isEqualTo(2);
		assertThat(BIG_INTEGER.from(BigDecimal.valueOf(2.5))).isEqualTo(BigInteger.valueOf(2));
	}

	@Test
	public void testThatBigDecimalUsesAsHighPrecisionAsPossible() throws Exception
	{
		assertThat(BIG_DECIMAL.from(Double.MAX_VALUE)).isEqualTo(BigDecimal.valueOf(Double.MAX_VALUE));
		assertThat(BIG_DECIMAL.from(2.5)).isEqualTo(BigDecimal.valueOf(2.5));
		assertThat(BIG_DECIMAL.from(Long.MAX_VALUE)).isEqualTo(BigDecimal.valueOf(((Long) Long.MAX_VALUE).doubleValue()));
		assertThat(BIG_DECIMAL.from(biggerThanLong)).isEqualTo(new BigDecimal(biggerThanLong));
	}

	@Test
	public void testThatBigIntegerUsesLongValueOnNumber() throws Exception
	{
		assertThat(BIG_INTEGER.from(Long.MAX_VALUE)).isEqualTo(BigInteger.valueOf(Long.MAX_VALUE));
		assertThat(BIG_INTEGER.from(Double.MAX_VALUE)).isEqualTo(BigInteger.valueOf(((Double) Double.MAX_VALUE).longValue()));
	}

	@Test
	public void testThatFromReturnsTheArgumentIfItsOfTheSameType() throws Exception
	{
		BigInteger integer = new BigInteger("1");
		assertThat(BIG_INTEGER.from(integer)).isSameAs(integer);
		BigDecimal decimal = new BigDecimal("2.5");
		assertThat(BIG_DECIMAL.from(decimal)).isSameAs(decimal);
	}

	@Test
	public void testThatToStringReturnsName()
	{
		for(NumberType<?> type : NumberType.TYPES)
		{
			assertThat(type.toString()).isEqualTo(type.name());
		}
	}

	@Test
	public void testInvalidShortNumbers()
	{
		List<Integer> invalidInput = Arrays.asList(Short.MIN_VALUE - 1, Short.MAX_VALUE + 1);
		for(Integer input : invalidInput)
		{
			try
			{
				SHORT.parse(input.toString());
				fail("Invalid short input not detected: " + input);
			}
			catch(IllegalArgumentException expected)
			{
				assertThat(expected).hasMessage(format(OUT_OF_RANGE, input, "-32,768", "32,767"));
			}
		}
	}

	@Test
	public void testInvalidIntegerNumbers()
	{
		List<Long> invalidInput = Arrays.asList((long) Integer.MIN_VALUE - 1, (long) Integer.MAX_VALUE + 1);
		for(Long input : invalidInput)
		{
			try
			{
				INTEGER.parse(input.toString());
				fail("Invalid integer input not detected: " + input);
			}
			catch(IllegalArgumentException expected)
			{
				assertThat(expected).hasMessage(format(OUT_OF_RANGE, input, "-2,147,483,648", "2,147,483,647"));
			}
		}
	}

	@Test
	public void testInvalidLongNumbers()
	{
		List<BigInteger> invalidInput = Arrays.asList(BigInteger.valueOf(Long.MIN_VALUE).subtract(BigInteger.ONE), biggerThanLong);
		for(BigInteger input : invalidInput)
		{
			try
			{
				LONG.parse(input.toString());
				fail("Invalid long input not detected: " + input);
			}
			catch(IllegalArgumentException expected)
			{
				assertThat(expected).hasMessage(format(OUT_OF_RANGE, input, "-9,223,372,036,854,775,808", "9,223,372,036,854,775,807"));
			}
		}
	}

	@Test
	public void testThatNullContractsAreFollowed()
	{
		for(NumberType<?> numberType : NumberType.TYPES)
		{
			new NullPointerTester().testInstanceMethods(numberType, Visibility.PUBLIC);
		}

		new NullPointerTester().testStaticMethods(NumberType.class, Visibility.PACKAGE);
	}

	@Test
	public void testThatUnparsableIntegerGeneratesProperErrorMessage() throws Exception
	{
		try
		{
			INTEGER.parse("123a", Locale.ENGLISH);
			fail("a should not be a parsable number");
		}
		catch(IllegalArgumentException e)
		{
			/**
			 * @formatter.off
			 */
			assertThat(e).hasMessage("'123a' is not a valid integer (Localization: English)" + NEWLINE +
			                         "    ^");
			/**
			 * @formatter.on
			 */
		}
	}

	@Test
	public void testThatUnparsableBigIntegerGeneratesProperErrorMessage() throws Exception
	{
		try
		{
			BIG_INTEGER.parse("12.3", Locale.ENGLISH);
			fail("12.3 should not be a parsable big-integer");
		}
		catch(IllegalArgumentException e)
		{
			/**
			 * @formatter.off
			 */
			assertThat(e).hasMessage("'12.3' is not a valid big-integer (Localization: English)" + NEWLINE +
			                         "   ^");
			/**
			 * @formatter.on
			 */
		}
	}

	@Test
	public void testThatDecimalSeparatorCausesParseErrorForDiscreetTypes() throws Exception
	{
		List<NumberType<?>> discreetTypes = Arrays.<NumberType<?>>asList(BYTE, SHORT, INTEGER, LONG, BIG_INTEGER);
		for(NumberType<?> discreetType : discreetTypes)
		{
			try
			{
				discreetType.parse("12.3", Locale.ENGLISH);
				fail("12.3 should not be a parsable " + discreetType);
			}
			catch(IllegalArgumentException e)
			{
				assertThat(e.getMessage()).contains("is not a valid " + discreetType);
			}
		}
	}

	@Test
	public void testThatSmallEnoughDataTypesAreInLongRange() throws Exception
	{
		assertThat(LONG.inRange((byte) 42)).isTrue();
		assertThat(LONG.inRange((short) 42)).isTrue();
		assertThat(LONG.inRange(42)).isTrue();
		assertThat(LONG.inRange(BigDecimal.valueOf(Long.MAX_VALUE))).isTrue();
		assertThat(LONG.inRange(BigInteger.valueOf(Long.MIN_VALUE))).isTrue();

		assertThat(LONG.inRange(biggerThanLong)).isFalse();
	}

	@Test
	public void testThatBigDecimalMustBeDiscreetForItToFitInABigInteger() throws Exception
	{
		assertThat(BIG_INTEGER.inRange(BigDecimal.valueOf(1.23))).isFalse();
		assertThat(BIG_INTEGER.inRange(BigDecimal.valueOf(123))).isTrue();
		assertThat(BIG_INTEGER.inRange(biggerThanLong)).isTrue();
	}
}
