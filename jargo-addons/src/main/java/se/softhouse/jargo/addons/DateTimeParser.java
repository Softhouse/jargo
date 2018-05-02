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
package se.softhouse.jargo.addons;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;
import se.softhouse.jargo.ArgumentException;
import se.softhouse.jargo.ArgumentExceptions;
import se.softhouse.jargo.StringParser;

import java.util.Locale;

import static java.util.Objects.requireNonNull;

/**
 * Parser for <a href="http://joda-time.sourceforge.net/">joda-time</a> {@link DateTime dates}
 */
final class DateTimeParser implements StringParser<DateTime>
{
	private final DateTimeZone timeZone;
	private final DateTimeFormatter formatter;

	DateTimeParser(DateTimeZone timeZone)
	{
		this.timeZone = requireNonNull(timeZone);
		this.formatter = ISODateTimeFormat.dateOptionalTimeParser().withZone(timeZone);
	}

	@Override
	public String descriptionOfValidValues(Locale locale)
	{
		requireNonNull(locale);
		String unmistakableDate = new DateTime("2011-02-28", timeZone).toString(ISODateTimeFormat.dateTime());
		return "an ISO8601 date, such as " + unmistakableDate;
	}

	@Override
	public DateTime parse(final String value, Locale locale) throws ArgumentException
	{
		requireNonNull(locale);
		try
		{
			return DateTime.parse(value, formatter);
		}
		catch(IllegalArgumentException wrongDateFormat)
		{
			throw ArgumentExceptions.withMessage(wrongDateFormat.getLocalizedMessage());
		}
	}

	@Override
	public DateTime defaultValue()
	{
		return DateTime.now(timeZone);
	}

	@Override
	public String metaDescription()
	{
		return "<date>";
	}
}
