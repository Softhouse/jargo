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
package se.softhouse.jargo.limiters;

import java.util.function.Predicate;

final class LowNumbers implements Predicate<Number>
{
	@Override
	public boolean test(Number input)
	{
		return input.byteValue() <= 4 && input.byteValue() >= 0;
	}

	@Override
	public String toString()
	{
		return "Any number between 0 and 4 (inclusive)";
	}
}
