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

import static org.fest.assertions.Assertions.assertThat;
import static se.softhouse.common.strings.Describables.cache;

import org.junit.Test;

import se.softhouse.common.testlib.Serializer;

import com.google.common.testing.NullPointerTester;
import com.google.common.testing.NullPointerTester.Visibility;

/**
 * Tests for {@link Describables}
 */
public class DescribablesTest
{
	@Test
	public void testStaticStringAsDescription()
	{
		assertThat(Describables.withString("foo").toString()).isEqualTo("foo");
	}

	@Test
	public void testThatEmptyStringEqualsEmptyString()
	{
		assertThat(Describables.EMPTY_STRING.description()).isEqualTo("");
	}

	@Test
	public void testFormatDescription()
	{
		assertThat(Describables.format("hello %s %s", "foo", "bar").toString()).isEqualTo("hello foo bar");
	}

	@Test
	public void testToStringDescription()
	{
		assertThat(Describables.toString(42).toString()).isEqualTo("42");
	}

	@Test
	public void testDescriptionAsSerializable()
	{
		Describable fortyTwo = Describables.toString(42);

		Describable deserialized = Serializer.clone(Describables.asSerializable(fortyTwo));

		assertThat(deserialized.toString()).isEqualTo(fortyTwo.toString());
	}

	@Test
	public void testDescriptionInException()
	{
		Describable fortyTwo = Describables.toString(42);
		assertThat(Describables.illegalArgument(fortyTwo)).hasMessage(fortyTwo.toString());
	}

	@Test
	public void testDescriptionInExceptionWithCause()
	{
		Describable fortyTwo = Describables.toString(42);
		Exception cause = new Exception();
		assertThat(Describables.illegalArgument(fortyTwo, cause).getCause()).isEqualTo(cause);
	}

	@Test
	public void testThatCachedDescriptionIsOnlyCreatedOnce()
	{
		ProfilingDescription description = new ProfilingDescription();
		Describable cachedDescription = cache(description);
		assertThat(cachedDescription.description()).isEqualTo("foo");
		assertThat(cachedDescription.description()).isEqualTo("foo");
		assertThat(cachedDescription.toString()).isEqualTo("foo");
		assertThat(description.timesCalled).isEqualTo(1);
	}

	private static class ProfilingDescription implements Describable
	{
		int timesCalled;

		@Override
		public String description()
		{
			timesCalled++;
			return "foo";
		}
	}

	@Test
	public void testThatNullContractsAreFollowed() throws Exception
	{
		new NullPointerTester().testStaticMethods(Describables.class, Visibility.PACKAGE);
	}
}
