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
package se.softhouse.common.guavaextensions;

import static org.fest.assertions.Assertions.assertThat;
import static org.fest.assertions.Fail.fail;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import com.google.common.testing.NullPointerTester;
import com.google.common.testing.NullPointerTester.Visibility;

/**
 * Tests for {@link Preconditions2}
 */
public class Preconditions2Test
{
	@Test
	public void testThatCheckNullsAddsTheOffendingIndex() throws Exception
	{
		try
		{
			Iterable<String> foos = Arrays.asList("foo", null, "bar");
			Preconditions2.checkNulls(foos, "foos may not be null");
			fail("null was not detected by checkNulls");
		}
		catch(NullPointerException expected)
		{
			assertThat(expected).hasMessage("foos may not be null (discovered one at index 1)");
		}
	}

	@Test
	public void testThatCheckNullsReturnsModifiableCopyOfList() throws Exception
	{
		List<String> foos = Arrays.asList("foo", "bar");
		List<String> checkedFoos = Preconditions2.checkNulls(foos, "foos may not be null");
		checkedFoos.set(0, "new-value");
		assertThat(checkedFoos).containsExactly("new-value", "bar");
		assertThat(foos).excludes("new-value");
	}

	@Test
	public void testThatNullContractsAreFollowed() throws Exception
	{
		new NullPointerTester().testStaticMethods(Preconditions2.class, Visibility.PACKAGE);
	}
}
