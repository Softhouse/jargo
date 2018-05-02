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
package se.softhouse.jargo.stringparsers.custom;

import static java.util.Collections.emptySet;
import static java.util.Collections.unmodifiableSet;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

import se.softhouse.jargo.StringParser;

public class UniqueLetters implements StringParser<Set<Character>>
{
	@Override
	public Set<Character> parse(final String argument, Locale locale)
	{
		Set<Character> unique = new HashSet<Character>();
		for(Character c : argument.toCharArray())
		{
			unique.add(c);
		}
		return unmodifiableSet(unique);
	}

	@Override
	public String descriptionOfValidValues(Locale locale)
	{
		return "any number of letters (duplicates will be filtered)";
	}

	@Override
	public Set<Character> defaultValue()
	{
		return emptySet();
	}

	@Override
	public String metaDescription()
	{
		return "<letters>";
	}

}
