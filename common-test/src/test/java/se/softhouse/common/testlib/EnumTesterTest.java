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
package se.softhouse.common.testlib;

import static org.fest.assertions.Assertions.assertThat;
import static org.junit.Assert.fail;

import java.lang.reflect.InvocationTargetException;

import org.junit.Test;

import com.google.common.testing.NullPointerTester;
import com.google.common.testing.NullPointerTester.Visibility;

public class EnumTesterTest
{
	@Test
	public void testThatValidEnumIsAccepted()
	{
		EnumTester.testThatToStringIsCompatibleWithValueOf(NiceEnum.class);
		EnumTester.testThatToStringIsCompatibleWithValueOfRegardlessOfVisibility(NiceEnum.class);
	}

	private enum NiceEnum
	{
		ONE,
		TWO;
	}

	@Test
	public void testThatInvalidEnumIsDetected()
	{
		try
		{
			EnumTester.testThatToStringIsCompatibleWithValueOf(InvalidEnum.class);
			fail("invalid toString of InvalidEnum should have been detected");
		}
		catch(AssertionError expected)
		{
		}
	}

	private enum InvalidEnum
	{
		ONE,
		TWO;

	@Override
		public String toString()
		{
			return TWO.name();
		}

	}

	@Test
	public void testThatInvalidToStringIsDetected()
	{
		try
		{
			EnumTester.testThatToStringIsCompatibleWithValueOf(InvalidValueOfEnum.class);
			fail("toString may not return null as it stops valueOf from working");
		}
		catch(IllegalArgumentException expected)
		{
			assertThat(expected).hasMessage(InvalidValueOfEnum.class.getName()
					+ "#TWO#valueOf() threw java.lang.NullPointerException: Name is null for input: null");
			assertThat(expected.getCause()).isInstanceOf(InvocationTargetException.class);
		}
	}

	private enum InvalidValueOfEnum{ONE,TWO
	{

	@Override
	public String toString()
	{
		return null;
	}

	};}

	@Test
	public void testThatPackageProtectedValueOfIsNotCallable() throws ClassNotFoundException
	{
		try
		{
			Class<InvalidEnum> insideEnum = loadInvalidEnum("enums.", "NestedEnum$InsideEnum");
			EnumTester.testThatToStringIsCompatibleWithValueOf(insideEnum);
			fail("valueOf should not work reflectively for package private enums");
		}
		catch(IllegalArgumentException expected)
		{
			assertThat(expected.getCause()).isInstanceOf(IllegalAccessException.class);
		}
	}

	@Test
	public void testThatNonEnumThrows() throws ClassNotFoundException
	{
		Class<InvalidEnum> insideEnum = loadInvalidEnum("", "EnumTesterTest");
		try
		{
			EnumTester.testThatToStringIsCompatibleWithValueOf(insideEnum);
			fail("class that's not an Enum wasn't detected");
		}
		catch(IllegalArgumentException expected)
		{
			assertThat(expected).hasMessage("EnumTesterTest is not an Enum");
			assertThat(expected.getCause()).isInstanceOf(NoSuchMethodException.class);
		}
	}

	@SuppressWarnings("unchecked")
	private Class<InvalidEnum> loadInvalidEnum(String subPackageName, String className) throws ClassNotFoundException
	{
		String packageName = getClass().getPackage().getName() + "." + subPackageName;
		// Yeah, here we fool the type system
		return (Class<InvalidEnum>) Class.forName(packageName + className);
	}

	@Test
	public void testThatNullContractsAreFollowed() throws Exception
	{
		new NullPointerTester().testStaticMethods(EnumTester.class, Visibility.PACKAGE);
	}
}
