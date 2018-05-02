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

import java.util.Locale;

import se.softhouse.jargo.ArgumentException;
import se.softhouse.jargo.StringParser;

public class NullReturningParser implements StringParser<Object>
{

	@Override
	public Object parse(String argument, Locale locale) throws ArgumentException
	{
		return null;
	}

	@Override
	public String descriptionOfValidValues(Locale locale)
	{
		return "";
	}

	@Override
	public Object defaultValue()
	{
		return "foo";
	}

	@Override
	public String metaDescription()
	{
		return "";
	}

}
