package se.softhouse.jargo.addons;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.ISODateTimeFormat;

import se.softhouse.jargo.Argument;
import se.softhouse.jargo.ArgumentBuilder;
import se.softhouse.jargo.ArgumentBuilder.DefaultArgumentBuilder;

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

/**
 * Additional {@link se.softhouse.jargo.Arguments}.<br>
 * To avoid explicitly specifying <code>se.softhouse.jargo.addons.Arguments</code> when using
 * arguments from both {@link Argument jargo} and {@link se.softhouse.jargo.addons.Arguments
 * jargo-addons} you can use static imports for each method.
 */
public final class Arguments
{
	private Arguments()
	{
	}

	/**
	 * Creates an {@link Argument} that parses {@link DateTime dates} with the
	 * {@link DateTimeZone#getDefault() default timezone} and the
	 * {@link ISODateTimeFormat#dateOptionalTimeParser()}.
	 *
	 * @param names the {@link ArgumentBuilder#names(String...) names} to use
	 */
	public static DefaultArgumentBuilder<DateTime> dateArgument(String ... names)
	{
		return se.softhouse.jargo.Arguments.withParser(new DateTimeParser(DateTimeZone.getDefault())).defaultValueDescription("Current time")
				.names(names);
	}

	/**
	 * Creates an {@link Argument} that parses {@link DateTime dates} with the provided
	 * {@link DateTimeZone#getDefault() timezone}.
	 *
	 * @param names the {@link ArgumentBuilder#names(String...) names} to use
	 */
	public static DefaultArgumentBuilder<DateTime> dateArgument(DateTimeZone timeZone, String ... names)
	{
		return se.softhouse.jargo.Arguments.withParser(new DateTimeParser(timeZone)).defaultValueDescription("Current time").names(names);
	}
}
