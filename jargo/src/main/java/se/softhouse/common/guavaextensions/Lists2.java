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

import static java.util.Objects.requireNonNull;

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public final class Lists2
{
	private Lists2()
	{
	}

	public static <E> ArrayList<E> newArrayList(Iterable<? extends E> elements)
	{
		requireNonNull(elements);
		return (elements instanceof Collection) ? new ArrayList<E>(cast(elements)) : newArrayList(elements.iterator());
	}

	public static <E> ArrayList<E> newArrayList(Iterator<? extends E> elements)
	{
		ArrayList<E> list = new ArrayList<E>();
		elements.forEachRemaining((e) -> list.add(e));
		return list;
	}

	public static <E> ArrayList<E> asList(@Nonnull E firstElement, E[] rest)
	{
		final ArrayList<E> objects = new ArrayList<>();
		objects.add(requireNonNull(firstElement));
		objects.addAll(Arrays.asList(rest));
		return objects;
	}

	/**
	 * @return {@code true} if the iterable contains no elements
	 */
	public static boolean isEmpty(Iterable<?> iterable)
	{
		if(iterable instanceof Collection)
			return ((Collection<?>) iterable).isEmpty();
		return !iterable.iterator().hasNext();
	}

	/**
	 * Returns the given {@code CharSequence} as a {@code
	 * List<Character>}
	 *
	 * @param sequence the character sequence to view as a {@code List} of
	 *            characters
	 * @return an {@code List<Character>} view of the character sequence
	 */
	public static List<Character> charactersOf(CharSequence sequence)
	{
		return new CharSequenceAsList(requireNonNull(sequence));
	}

	private static final class CharSequenceAsList extends AbstractList<Character>
	{
		private final CharSequence sequence;

		CharSequenceAsList(CharSequence sequence)
		{
			this.sequence = sequence;
		}

		@Override
		public Character get(int index)
		{
			return sequence.charAt(index);
		}

		@Override
		public int size()
		{
			return sequence.length();
		}
	}

	public static int size(Iterator<?> iterator)
	{
		int i = 0;
		while(iterator.hasNext())
		{
			i++;
			iterator.next();
		}
		return i;
	}

	public static int size(Iterable<?> iterable)
	{
		if(iterable instanceof Collection)
			return ((Collection<?>) iterable).size();
		return size(iterable.iterator());
	}

	public static <T> Iterator<T> unmodifiableIterator(final Iterator<? extends T> iterator)
	{
		requireNonNull(iterator);
		return new Iterator<T>(){
			@Override
			public boolean hasNext()
			{
				return iterator.hasNext();
			}

			@Override
			public T next()
			{
				return iterator.next();
			}
		};
	}

	static <T> Collection<T> cast(@Nullable Iterable<T> iterable)
	{
		return (Collection<T>) iterable;
	}

}
