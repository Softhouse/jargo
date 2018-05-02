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

import javax.annotation.concurrent.Immutable;

/**
 * Utilities for working with {@link StringBuilder}s
 */
@Immutable
public final class StringBuilders
{
	private StringBuilders()
	{
	}

	/**
	 * <pre>
	 * Creates a {@link StringBuilder} with a pre-allocated array of {@code expectedSize}.
	 * 
	 * This method is suitable to exclude when running mutation tests as it's rather hard to test
	 * the calculation of expectedSize in unit tests without it affecting run time to much.
	 * </pre>
	 */
	public static StringBuilder withExpectedSize(int expectedSize)
	{
		return new StringBuilder(expectedSize);
	}
}
