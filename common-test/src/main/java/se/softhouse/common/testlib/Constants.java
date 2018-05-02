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

import static java.util.concurrent.TimeUnit.SECONDS;

import javax.annotation.concurrent.Immutable;

import com.google.common.base.Charsets;

/**
 * Useful constants to use in tests
 */
@Immutable
public final class Constants
{
	private Constants()
	{
	}

	/**
	 * One second in milliseconds
	 */
	public static final long ONE_SECOND = SECONDS.toMillis(1);

	/**
	 * One minute in milliseconds
	 */
	public static final long ONE_MINUTE = SECONDS.toMillis(60);

	/**
	 * A character that exists in {@link Charsets#UTF_8 UTF-8} but not in {@link Charsets#US_ASCII
	 * ascii}
	 */
	public static final char UTF_8_CHAR = '\uFF26';

	static final int EXPECTED_TEST_TIME_FOR_THIS_SUITE = 10000;
}
