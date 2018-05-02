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

import static se.softhouse.jargo.StringParsers.stringParser;

import java.util.Locale;
import java.util.Set;

import se.softhouse.jargo.ArgumentException;
import se.softhouse.jargo.ArgumentExceptions;
import se.softhouse.jargo.ForwardingStringParser.SimpleForwardingStringParser;

import com.google.common.collect.ImmutableSet;

public class LimitedKeyParser extends SimpleForwardingStringParser<String>
{
	private final Set<String> validKeys;

	public LimitedKeyParser(String ... validKeys)
	{
		super(stringParser());
		this.validKeys = ImmutableSet.copyOf(validKeys);
	}

	@Override
	public String parse(String value, Locale locale) throws ArgumentException
	{
		String result = super.parse(value, locale);
		if(!validKeys.contains(result))
			throw ArgumentExceptions.withMessage("'" + value + "' didn't match any of: " + validKeys);
		return result;
	}

	@Override
	public String descriptionOfValidValues(Locale locale)
	{
		return "any of " + validKeys;
	}
}
