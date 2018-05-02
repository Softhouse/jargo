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

import static com.google.common.collect.ImmutableSet.of;
import static org.fest.assertions.Assertions.assertThat;

import java.util.Set;

import org.junit.Test;

import com.google.common.testing.NullPointerTester;
import com.google.common.testing.NullPointerTester.Visibility;

/**
 * Tests for {@link Sets2}
 */
public class Sets2Test
{
	@Test
	public void testThatUnionReturnsCombinationOfTwoSets() throws Exception
	{
		Set<String> union = Sets2.union(of("hi"), of("bye"));
		assertThat(union).hasSize(2);
		assertThat(union).containsOnly("hi", "bye");
		assertThat(union.contains("foo")).isFalse();
		assertThat(union.contains("bye")).isTrue();
		assertThat(union.contains("hi")).isTrue();
	}

	@Test
	public void testThatDifferenceReturnsDifferenceBetweenTwoSets() throws Exception
	{
		Set<String> difference = Sets2.difference(of("hi", "bye"), of("bye", "foo"));
		assertThat(difference).hasSize(1);
		assertThat(difference).containsOnly("hi");
		assertThat(difference.contains("bye")).isFalse();
		assertThat(difference.contains("hi")).isTrue();
		assertThat(difference.contains("foo")).isFalse();
		assertThat(difference.isEmpty()).isFalse();
	}

	@Test
	public void testThatNullContractsAreFollowed() throws Exception
	{
		new NullPointerTester().testStaticMethods(Sets2.class, Visibility.PACKAGE);
	}
}
