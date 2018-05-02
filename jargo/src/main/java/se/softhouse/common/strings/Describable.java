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

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import javax.annotation.concurrent.Immutable;

/**
 * Allows for lazily created descriptions (i.e {@link String}s) and the possible performance
 * optimization that the string is not constructed if it's not used.
 * If you already have a created {@link String} it's recommended to just use that instead.
 * This interface is typically implemented using an anonymous class.
 * 
 * @see Describables
 */
@Immutable
public interface Describable
{
	/**
	 * @return a description
	 */
	@Nonnull
	@CheckReturnValue
	String description();
}
