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

import static java.util.Collections.emptyList;
import static org.fest.assertions.Assertions.assertThat;

import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;

import org.junit.Test;

import com.google.common.testing.NullPointerTester;
import com.google.common.testing.NullPointerTester.Visibility;

/**
 * Tests for {@link Suppliers2}
 */
public class Suppliers2Test
{
	@Test
	public void testThatInstanceOfIsSuppliedAlready()
	{
		assertThat(Suppliers2.isSuppliedAlready(Suppliers2.ofInstance("foo"))).isTrue();
	}

	@Test
	public void testThatRepeatedMemoizeReturnsSameMemoizeInstance()
	{
		Supplier<String> first = Suppliers2.memoize(Suppliers2.ofInstance("foo"));
		Supplier<String> second = Suppliers2.memoize(first);
		assertThat(second).isSameAs(first);
	}

	@Test
	public void testThatInstanceOfIsSuppliedAlreadyForList()
	{
		Supplier<List<String>> listSupplier = Suppliers2.ofRepeatedElements(Suppliers2.ofInstance("foo"), 3);
		assertThat(Suppliers2.isSuppliedAlready(listSupplier)).isTrue();
	}

	@Test
	public void testThatInstanceOfIsNotSuppliedAlreadyForOtherSuppliers()
	{
		assertThat(Suppliers2.isSuppliedAlready(FOO_SUPPLIER)).isFalse();
	}

	private static final Supplier<String> FOO_SUPPLIER = () -> "foo";

	@Test
	public void testThatInstanceOfIsNotSuppliedForArbitrarySupplierInList()
	{
		Supplier<List<String>> listSupplier = Suppliers2.ofRepeatedElements(FOO_SUPPLIER, 3);
		assertThat(Suppliers2.isSuppliedAlready(listSupplier)).isFalse();
	}

	@Test
	public void testThatRepeatedElementsReturnCorrectNumberOfInstances()
	{
		Supplier<List<String>> listSupplier = Suppliers2.ofRepeatedElements(FOO_SUPPLIER, 3);
		assertThat(listSupplier.get()).isEqualTo(Arrays.asList("foo", "foo", "foo"));
	}

	@Test(expected = IllegalArgumentException.class)
	public void testThatNegativeRepeatsIsIllegal()
	{
		Suppliers2.ofRepeatedElements(FOO_SUPPLIER, -1);
	}

	@Test
	public void testThatZeroRepeatsReturnsEmptyList()
	{
		assertThat(Suppliers2.ofRepeatedElements(FOO_SUPPLIER, 0).get()).isEqualTo(emptyList());
	}

	@Test
	public void testThatNullContractsAreFollowed() throws Exception
	{
		new NullPointerTester().testStaticMethods(Suppliers2.class, Visibility.PACKAGE);
	}
}
