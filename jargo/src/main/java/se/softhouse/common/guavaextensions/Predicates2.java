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

import static java.util.Objects.requireNonNull;

import java.util.Collection;
import java.util.List;
import java.util.function.Predicate;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.Immutable;

import se.softhouse.common.strings.Describables;

/**
 * Additional implementations of the {@link Predicate} interface
 */
@Immutable
public final class Predicates2
{
	private Predicates2()
	{
	}

	public static <T> Predicate<T> alwaysTrue()
	{
		return ObjectPredicates.ALWAYS_TRUE.withNarrowedType();
	}

	public static <T> Predicate<T> alwaysFalse()
	{
		return ObjectPredicates.ALWAYS_FALSE.withNarrowedType();
	}

	// Visible for testing
	enum ObjectPredicates implements Predicate<Object>
	{
		ALWAYS_TRUE((i) -> true),
		ALWAYS_FALSE((i) -> false);

	private Predicate<Object> predicate;

	ObjectPredicates(Predicate<Object> predicate)
		{
			this.predicate = predicate;
		}

	@Override
		public boolean test(Object o)
		{
			return predicate.test(o);
		}

	@SuppressWarnings("unchecked") // safe contravariant cast as all ObjectPredicates
		<T> Predicate<T> withNarrowedType()
		{
			return (Predicate<T>) this;
		}

	}

	/**
	 * Returns a predicate that throws an {@link IllegalArgumentException} if any element in a list
	 * doesn't return true for {@code elementLimiter.apply(Object)}. Useful for input validation
	 * where you already have a {@link Predicate} for the element type and want to check a list of
	 * such elements.
	 * Without {@link #listPredicate(Predicate)} it would look something like:
	 *
	 * <pre class="prettyprint">
	 * <code class="language-java">
	 * List&lt;Integer&gt; numbers = Arrays.asList(1, -1, -2);
	 * Optional&lt;Integer&gt; invalidNumber = Iterables.tryFind(numbers, Predicates.not(ABOVE_ZERO));
	 * if(invalidNumber.isPresent())
	 *   throw new IllegalArgumentException("'" + invalidNumber.get() + "' is not " + ABOVE_ZERO);
	 * </code>
	 * </pre>
	 *
	 * With {@link #listPredicate(Predicate)}:
	 *
	 * <pre class="prettyprint">
	 * <code class="language-java">
	 * Predicates2.listPredicate(ABOVE_ZERO).apply(numbers);
	 * </code>
	 * </pre>
	 *
	 * As this breaks the general contract of {@link Predicate#test(Object)} it should be
	 * considered useful for validating arguments only.
	 * The detail message only mentions the first element that didn't return true.
	 */
	public static <E> Predicate<List<? extends E>> listPredicate(Predicate<E> elementLimiter)
	{
		requireNonNull(elementLimiter);
		if(elementLimiter == alwaysTrue())
			return alwaysTrue();
		return new ListPredicate<E>(elementLimiter);
	}

	private static final class ListPredicate<E> implements Predicate<List<? extends E>>
	{
		private final Predicate<E> elementLimiter;

		private ListPredicate(Predicate<E> elementLimiter)
		{
			this.elementLimiter = elementLimiter;
		}

		@Override
		public boolean test(@Nonnull List<? extends E> values)
		{
			for(E value : values)
			{
				if(!elementLimiter.test(value))
					throw Describables.illegalArgument(Describables.format("'%s' is not %s", value, elementLimiter));
			}
			return true;
		}

		@Override
		public String toString()
		{
			return elementLimiter.toString();
		}
	}

	/**
	 * Works just like {@link Predicate#and(Predicate)} except that if {@code first} is
	 * {@link Predicates2#alwaysTrue()} {@code second} is returned directly (or vice versa). This
	 * has
	 * the potential to make {@link Object#toString()} look a bit nicer for the resulting
	 * {@link Predicate}.
	 */
	public static <T> Predicate<? super T> and(Predicate<? super T> first, Predicate<? super T> second)
	{
		requireNonNull(first);
		requireNonNull(second);
		if(first == alwaysTrue())
			return second;
		else if(second == alwaysTrue())
			return first;

		return new Predicate<T>(){
			@Override
			public boolean test(T o)
			{
				return first.test(o) && second.test(o);
			}

			@Override
			public String toString()
			{
				return "AND(" + first + ", " + second + ")";
			}
		};
	}

	/**
	 * Creates a {@link Predicate} which mandates that {@code target} contains the tested object
	 *
	 * @param target the collection that contains the "ok" objects
	 * @param <T> type of objects in {@code target}
	 * @return the newly created {@link Predicate}
	 */
	public static <T> Predicate<T> in(Collection<? extends T> target)
	{
		requireNonNull(target);
		return (e) -> target.contains(e);
	}
}
