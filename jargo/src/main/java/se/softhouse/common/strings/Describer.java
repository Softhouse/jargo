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

import java.util.Locale;
import java.util.function.Function;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;

/**
 * Describes values of the type {@code T} (or subclasses of {@code T}). Useful
 * when {@link Object#toString()} doesn't give you what you want.
 *
 * @param <T> the type to describe
 * @see Describers
 */
@Immutable
public interface Describer<T> extends Function<T, String>
{
	/**
	 * @param value the value to describe
	 * @param inLocale the {@link Locale} to use when formatting the resulting {@link String}
	 * @return a {@link String} describing {@code value}
	 */
	@CheckReturnValue
	@Nonnull
	String describe(@Nullable T value, Locale inLocale);

	/**
	 * Uses {@link Locale#getDefault()} to describe values as a function
	 * 
	 * @param value the value to describe
	 * @return the description of the value
	 * @see Describers#asFunction(Describer, Locale) for specifying a specific {@link Locale}
	 */
	@Override
	@CheckReturnValue
	@Nonnull
	default String apply(T value)
	{
		return describe(value, Locale.getDefault());
	}
}
