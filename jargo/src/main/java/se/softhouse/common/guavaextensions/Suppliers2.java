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

import static java.lang.String.format;
import static java.util.Objects.requireNonNull;
import static se.softhouse.common.guavaextensions.Preconditions2.check;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;

import se.softhouse.jargo.internal.Texts.UserErrors;

/**
 * Additional implementations of the {@link Supplier} interface
 */
@Immutable
public final class Suppliers2
{
	private Suppliers2()
	{
	}

	/**
	 * Creates a {@link Supplier} that supplies {@code elementsToSupply} number of elements from
	 * {@code elementSupplier}
	 *
	 * @throws IllegalArgumentException if {@code elementsToSupply} is less than zero
	 */
	@CheckReturnValue
	public static <T> Supplier<List<T>> ofRepeatedElements(Supplier<? extends T> elementSupplier, int elementsToSupply)
	{
		requireNonNull(elementSupplier);
		check(elementsToSupply >= 0, "elementsToSupply may not be negative");
		return new ListSupplier<T>(elementSupplier, elementsToSupply);
	}

	private static final class ListSupplier<T> implements Supplier<List<T>>
	{
		private final Supplier<? extends T> elementSupplier;
		private final int elementsToSupply;

		private ListSupplier(Supplier<? extends T> elementSupplier, final int elementsToSupply)
		{
			this.elementSupplier = elementSupplier;
			this.elementsToSupply = elementsToSupply;
		}

		@Override
		public List<T> get()
		{
			List<T> result = new ArrayList<T>(elementsToSupply);
			for(int i = 0; i < elementsToSupply; i++)
			{
				result.add(elementSupplier.get());
			}
			return result;
		}
	}

	/**
	 * Creates a {@link Supplier} that always returns {@code instance}.
	 * 
	 * @param instance the instance to supply
	 * @param <T> the type
	 * @return the newly created supplier
	 */
	@CheckReturnValue
	public static <T> Supplier<T> ofInstance(@Nullable T instance)
	{
		return new InstanceSupplier<T>(instance);
	}

	private static final class InstanceSupplier<T> implements Supplier<T>
	{
		private final T instance;

		private InstanceSupplier(T instance)
		{
			this.instance = instance;
		}

		@Override
		public T get()
		{
			return instance;
		}
	}

	/**
	 * @param origin the supplier that provides the original value to check and transform
	 * @param transformer a function that will be used to transform the origin value to something
	 *            else
	 * @param predicate function that will be used to check for correctness
	 * @return a wrapping {@link Supplier}
	 */
	@CheckReturnValue
	public static <T, F> Supplier<F> wrapWithPredicateAndTransform(Supplier<? extends T> origin, Function<T, F> transformer,
			Predicate<? super T> predicate)
	{
		return new PredicatedAndTransformedSupplier<T, F>(origin, transformer, predicate);
	}

	private static final class PredicatedAndTransformedSupplier<T, F> implements Supplier<F>
	{
		private final Supplier<? extends T> origin;
		private final Function<T, F> transformer;
		private final Predicate<? super T> predicate;

		private PredicatedAndTransformedSupplier(Supplier<? extends T> origin, Function<T, F> transformer, Predicate<? super T> predicate)
		{
			this.origin = requireNonNull(origin);
			this.transformer = requireNonNull(transformer);
			this.predicate = requireNonNull(predicate);
		}

		@Override
		public F get()
		{
			T originValue = origin.get();
			if(!predicate.test(originValue))
				throw new IllegalArgumentException(format(UserErrors.DISALLOWED_VALUE, originValue, predicate));
			return transformer.apply(originValue);
		}

		//

	}

	/**
	 * Returns true if {@code Supplier} is likely to supply values very fast
	 */
	public static boolean isSuppliedAlready(Supplier<?> supplier)
	{
		if(supplier.getClass().equals(InstanceSupplier.class))
			return true;
		else if(supplier instanceof ListSupplier<?>)
		{
			ListSupplier<?> listSupplier = (ListSupplier<?>) supplier;
			if(listSupplier.elementSupplier.getClass().equals(InstanceSupplier.class))
				return true;
		}
		return false;
	}

	/**
	 * Returns a supplier which caches the instance retrieved during the first call to {@code get()}
	 * and returns that value on subsequent calls to {@code get()}. See:
	 * <a href="http://en.wikipedia.org/wiki/Memoization">memoization</a>
	 */
	public static <T> Supplier<T> memoize(Supplier<T> delegate)
	{
		return (delegate instanceof MemoizingSupplier) ? delegate : new MemoizingSupplier<T>(requireNonNull(delegate));
	}

	private static class MemoizingSupplier<T> implements Supplier<T>, Serializable
	{
		final Supplier<T> delegate;
		transient volatile boolean initialized;
		// "value" does not need to be volatile; visibility piggy-backs
		// on volatile read of "initialized".
		transient T value;

		MemoizingSupplier(Supplier<T> delegate)
		{
			this.delegate = delegate;
		}

		@Override
		public T get()
		{
			// A 2-field variant of Double Checked Locking.
			if(!initialized)
			{
				synchronized(this)
				{
					if(!initialized)
					{
						T t = delegate.get();
						value = t;
						initialized = true;
						return t;
					}
				}
			}
			return value;
		}

		private static final long serialVersionUID = 0;
	}
}
