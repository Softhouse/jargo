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

import org.fest.assertions.Fail;

/**
 * Used to simulate an exception. Commonly used to avoid throwing something like
 * {@link IllegalStateException} or {@link IllegalArgumentException} in tests (catching those could
 * mask other errors if the message on the exception isn't verified, and asserting that message
 * would only cause further confusion as that's not really what the test is about). Catching
 * {@link SimulatedException} should be safe in tests as that's the purpose of the test, to catch it
 * and verify behavior when it occurs. <br>
 * <b>Reminder:</b> don't forget to call {@link Fail#fail(String) fail} if the exception doesn't
 * happen (otherwise you won't notice any test failures and a kitten may die)
 */
public class SimulatedException extends RuntimeException
{
	private static final long serialVersionUID = 1L;

	/**
	 * @see RuntimeException#RuntimeException()
	 */
	public SimulatedException()
	{
	}

	/**
	 * @see RuntimeException#RuntimeException(String)
	 */
	public SimulatedException(String message)
	{
		super(message);
	}
}
