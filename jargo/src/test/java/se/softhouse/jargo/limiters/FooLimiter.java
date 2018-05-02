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

public class FooLimiter implements Predicate<String>
{
	public static Predicate<String> foos()
	{
		return new FooLimiter();
	}

	@Override
	public boolean test(String value)
	{
		return "foo".equals(value);
	}

	@Override
	public String toString()
	{
		return "foo";
	}
}
