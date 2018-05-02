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

import static se.softhouse.common.strings.StringsUtil.NEWLINE;

import java.text.BreakIterator;
import java.util.Locale;

import javax.annotation.concurrent.Immutable;

/**
 * Utilities for working with {@link StringsUtil#NEWLINE new line} in texts.
 */
@Immutable
public final class Lines
{
	private Lines()
	{
	}

	/**
	 * Wraps lines where <a
	 * href="http://docs.oracle.com/javase/tutorial/i18n/text/line.html">appropriate</a> (as defined
	 * by {@code locale}).
	 * 
	 * @param value the value to separate with {@link StringsUtil#NEWLINE new lines}
	 * @param maxLineLength how long each line are allowed to be
	 */
	public static StringBuilder wrap(CharSequence value, int maxLineLength, Locale locale)
	{
		return wrap(value, 0, maxLineLength, locale);
	}

	/**
	 * Wraps lines where <a
	 * href="http://docs.oracle.com/javase/tutorial/i18n/text/line.html">appropriate</a> (as defined
	 * by {@code locale}).
	 * 
	 * @param value the value to separate with {@link StringsUtil#NEWLINE new lines}
	 * @param startingIndex the index where each line starts, useful for a fixed-size table for
	 *            instance
	 * @param maxLineLength how long each line are allowed to be
	 */
	public static StringBuilder wrap(CharSequence value, int startingIndex, int maxLineLength, Locale locale)
	{
		String textToSplit = value.toString();
		StringBuilder result = new StringBuilder(textToSplit.length());
		// TODO(jontejj): is this not thread safe?
		BreakIterator boundary = BreakIterator.getLineInstance(locale);
		boundary.setText(textToSplit);
		int start = boundary.first();
		int end = boundary.next();
		int lineLength = startingIndex;

		while(end != BreakIterator.DONE)
		{
			String word = textToSplit.substring(start, end);
			lineLength = lineLength + word.length();
			if(lineLength >= maxLineLength)
			{
				result.append(NEWLINE);
				lineLength = startingIndex;
			}
			result.append(word);
			start = end;
			end = boundary.next();
		}
		return result;
	}
}
