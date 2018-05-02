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

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import java.util.ArrayList;
import java.util.List;

import static java.util.Objects.requireNonNull;

/**
 * Some pre-condition checks for fail-fast behaviour
 */
@Immutable
public final class Preconditions2
{
	private Preconditions2()
	{
	}

	/**
	 * Checks that an expression is true
	 *
	 * @param expression a boolean expression
	 * @param errorMessage the exception message to use if the check fails; will be converted to a
	 *            string using {@link String#valueOf(Object)}
	 * @throws IllegalStateException if {@code expression} is false
	 */
	public static void check(boolean expression, @Nullable String errorMessage, @Nullable Object ... args)
	{
		if(!expression)
		{
			throw new IllegalArgumentException(String.format(errorMessage, args));
		}
	}

	/**
	 * <pre>
	 * Returns {@code items} as a <i>modifiable</i> {@link List} that's guaranteed not to contain
	 * any nulls.
	 * 
	 * @throws NullPointerException with {@code message} (plus the index of the first
	 *             <code>null</code> element) if any element in {@code items} is <code>null</code>
	 * </pre>
	 */
	@Nonnull
	public static <T> List<T> checkNulls(Iterable<T> items, String message)
	{
		requireNonNull(message, "a message describing what it is that is containing the elements must be given");
		List<T> nullCheckedList = new ArrayList<T>();
		int index = 0;
		for(T element : items)
		{
			if(element == null)
				throw new NullPointerException(message + " (discovered one at index " + index + ")");
			nullCheckedList.add(element);
			index++;
		}
		return nullCheckedList;
	}

}
