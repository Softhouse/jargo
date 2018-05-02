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
package se.softhouse.jargo.argumentbuilder;

import java.util.Locale;

import se.softhouse.jargo.ArgumentException;
import se.softhouse.jargo.StringParser;

public class FooParser implements StringParser<Foo>
{
	private final int bar;

	public FooParser(final int aBar)
	{
		bar = aBar;
	}

	@Override
	public Foo parse(String argument, Locale locale) throws ArgumentException
	{
		return new Foo(argument, bar);
	}

	@Override
	public String descriptionOfValidValues(Locale locale)
	{
		return "foos";
	}

	@Override
	public Foo defaultValue()
	{
		return new Foo("", bar);
	}

	@Override
	public String metaDescription()
	{
		return "<foo>";
	}

}
