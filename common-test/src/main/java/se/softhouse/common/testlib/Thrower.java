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
package se.softhouse.common.testlib;

import javax.annotation.concurrent.Immutable;

import com.google.common.annotations.VisibleForTesting;

/**
 * <pre>
 * Makes it possible to throw checked exceptions from methods that don't declare them. See
 * <a href=
"http://java.dzone.com/articles/throwing-undeclared-checked">throwing-undeclared-checked</a> for a more thorough discussion.
 * Used in tests to reach places where 3rd party code forces you to catch exceptions that
 * <a href=
"https://code.google.com/p/guava-libraries/wiki/StringsExplained#Charsets">can't happen</a>.
 * </pre>
 */
@Immutable
public final class Thrower<E extends Exception>
{
	/**
	 * Re-throws {@code checkedException} without declaring that it throws it
	 * 
	 * @return doesn't actually return the exception to throw but removes the need to have
	 *         {@code return null;} if the calling code have a return type
	 */
	public static RuntimeException asUnchecked(final Exception checkedException)
	{
		return new Thrower<RuntimeException>().sneakyThrow(checkedException);
	}

	private Thrower()
	{
	}

	@SuppressWarnings("unchecked")
	// Takes advantage of the fact that the JVM allows any exceptions to be thrown
	@VisibleForTesting
	E sneakyThrow(Exception exception) throws E
	{
		throw (E) exception;
	}
}
