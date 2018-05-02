/*
 * Copyright 2017 Jonatan Jönsson
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

import java.util.AbstractSet;
import java.util.Iterator;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static java.util.Objects.requireNonNull;
import static se.softhouse.common.guavaextensions.Lists2.unmodifiableIterator;

public final class Sets2
{
	private Sets2()
	{
	}

	/**
	 * Returns an unmodifiable <b>view</b> of the difference of two sets. The
	 * returned set contains all elements that are contained by {@code set1} and
	 * not contained by {@code set2}.
	 * <b>Note:</b> {@code set2} may also contain elements not
	 * present in {@code set1}; these are simply ignored!
	 */
	public static <E> Set<E> difference(final Set<E> set1, final Set<?> set2)
	{
		requireNonNull(set1, "set1");
		requireNonNull(set2, "set2");

		final Predicate<Object> notInSet2 = Predicates2.<Object>in(set2).negate();
		return new AbstractSet<E>(){
			@Override
			public Iterator<E> iterator()
			{
				return unmodifiableIterator(set1.stream().filter(notInSet2).iterator());
			}

			@Override
			public int size()
			{
				return Lists2.size(iterator());
			}

			@Override
			public boolean isEmpty()
			{
				return set2.containsAll(set1);
			}

			@Override
			public boolean contains(Object e)
			{
				return set1.contains(e) && !set2.contains(e);
			}
		};
	}

	/**
	 * Returns an unmodifiable view of the union of two sets. The returned
	 * set contains all elements that are contained in either backing set.
	 * Iterating over the returned set iterates first over all the elements of
	 * {@code set1}, then over each element of {@code set2}, in order, that is not
	 * contained in {@code set1}.
	 * <b>Note:</b> for stable results, this method requires that both sets are based on the same
	 * equivalences
	 */
	public static <E> Set<E> union(Set<? extends E> one, Set<? extends E> two)
	{
		return new AbstractSet<E>(){
			private final Set<? extends E> set2minus1 = difference(two, one);

			@Override
			public Iterator<E> iterator()
			{
				return unmodifiableIterator(Stream.concat(one.stream(), set2minus1.stream()).iterator());
			}

			@Override
			public int size()
			{
				return one.size() + set2minus1.size();
			}

			@Override
			public boolean contains(Object e)
			{
				return one.contains(e) || two.contains(e);
			}

			@Override
			public boolean isEmpty()
			{
				return one.isEmpty() && two.isEmpty();
			}
		};
	}
}
