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
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static java.util.Objects.requireNonNull;
import static java.util.stream.IntStream.of;
import static se.softhouse.common.guavaextensions.Lists2.isEmpty;
import static se.softhouse.common.guavaextensions.Preconditions2.check;

/**
 * Utilities for working with {@link String}s
 */
@Immutable
public final class StringsUtil
{
	private StringsUtil()
	{
	}

	/**
	 * A suitable string to represent newlines on this specific platform
	 */
	public static final String NEWLINE = System.getProperty("line.separator");

	/**
	 * A {@link Charset} for <a href="https://en.wikipedia.org/wiki/UTF-8">UTF8</a>
	 */
	public static final Charset UTF8 = Charset.forName("UTF-8");

	/**
	 * The <a href="http://en.wikipedia.org/wiki/ASCII_tab">ASCII tab</a> (\t) character
	 */
	public static final char TAB = '\t';

	/**
	 * @param numberOfSpaces to put in the created string
	 * @return a string with numberOfSpaces in it
	 * @deprecated use {@link #repeat(String, int)} instead
	 */
	@Nonnull
	@CheckReturnValue
	@Deprecated
	public static String spaces(final int numberOfSpaces)
	{
		return repeat(" ", numberOfSpaces);
	}

	/**
	 * Returns a " ^" string pointing at the position indicated by {@code indexToPointAt}
	 */
	public static String pointingAtIndex(int indexToPointAt)
	{
		return spaces(indexToPointAt) + "^";
	}

	/**
	 * Returns <code>true</code> iff {@code input} {@link String#startsWith(String) starts with}
	 * {@code toStartWith} and has more characters after that match
	 */
	public static boolean startsWithAndHasMore(String input, String toStartWith)
	{
		return input.startsWith(toStartWith) && input.length() > toStartWith.length();
	}

	/**
	 * <pre>
	 * Returns the {@link String} in {@code validOptions} that {@code input} has the shortest
	 * <a href="http://en.wikipedia.org/wiki/Levenshtein_distance">levenshtein distance</a> to.
	 *
	 * For example when given "stats" as input and "status", "help",
	 * "action" as validOptions, "status" is returned.
	 *
	 * Current performance characteristics:
	 * n = length of {@code input}
	 * m = average string length of the strings in {@code validOptions}
	 * s = amount of validOptions
	 *
	 * complexity = n * m * s = O(n^3)
	 *
	 * So try to limit the number of valid options...
	 *
	 * @throws IllegalArgumentException if {@code validOptions} is empty
	 * </pre>
	 *
	 * @see #closestMatches(String, Iterable, int)
	 */
	@Nonnull
	@CheckReturnValue
	public static String closestMatch(final String input, final Iterable<String> validOptions)
	{
		requireNonNull(input);
		check(!isEmpty(validOptions), "No valid options to match the input against");

		int shortestDistance = Integer.MAX_VALUE;
		String bestGuess = null;
		for(String validOption : validOptions)
		{
			int distance = levenshteinDistance(input, validOption);
			if(distance < shortestDistance)
			{
				shortestDistance = distance;
				bestGuess = validOption;
			}
		}
		return bestGuess;
	}

	/**
	 * <pre>
	 * Returns a sorted {@link List} where the first entry is the {@link String} in {@code validOptions} that's closest in terms of
	 * <a href=
	"http://en.wikipedia.org/wiki/Levenshtein_distance">levenshtein distance</a> to {@code input}, or an empty list if no options within distance can be found.
	 *
	 * For example when given "stats" as input and "status", "staging",
	 * "stage" as validOptions, and 4 as maximumDistance, "status", "stage", "staging" is returned.
	 *
	 * Only values with a distance less than or equal to {@code maximumDistance} will be included in the result.
	 *
	 * The returned list is <i>modifiable</i>.
	 * </pre>
	 */
	@Nonnull
	@CheckReturnValue
	public static List<String> closestMatches(final String input, final Iterable<String> validOptions, int maximumDistance)
	{
		requireNonNull(input);
		if(isEmpty(validOptions))
			return Collections.emptyList();

		List<CloseMatch> closeMatches = new ArrayList<>();
		for(String validOption : validOptions)
		{
			int distance = levenshteinDistance(input, validOption, maximumDistance + 1);
			if(distance <= maximumDistance)
			{
				closeMatches.add(new CloseMatch(validOption, distance));
			}
		}
		return closeMatches.stream().sorted((l, r) -> l.measuredDistance - r.measuredDistance) //
				.map((i) -> i.value) //
				.collect(Collectors.toList());
	}

	static final class CloseMatch
	{
		private final int measuredDistance;
		private final String value;

		private CloseMatch(String validOption, int distance)
		{
			measuredDistance = distance;
			value = validOption;
		}
	}

	/**
	 * Returns the <a href="http://en.wikipedia.org/wiki/Levenshtein_distance">levenshtein
	 * distance</a> between {@code left} and {@code right}.
	 *
	 * @see #closestMatch(String, Iterable)
	 */
	public static int levenshteinDistance(final String left, final String right)
	{
		return levenshteinDistance(left, right, Integer.MAX_VALUE);
	}

	/**
	 * Returns the <a href="http://en.wikipedia.org/wiki/Levenshtein_distance">levenshtein
	 * distance</a> between {@code left} and {@code right}. If it's greater than maxDistance,
	 * maxDistance will be returned.
	 *
	 * @see #closestMatch(String, Iterable)
	 */
	public static int levenshteinDistance(final String left, final String right, final int maxDistance)
	{
		requireNonNull(left);
		requireNonNull(right);
		check(maxDistance >= 0, "only zero or positive distance supported. Not ", maxDistance);

		// a "cleaner" version of the org.apache.commons-lang algorithm which in
		// turn was inspired by http://www.merriampark.com/ldjava.htm
		int leftLength = left.length();
		int rightLength = right.length();

		if(leftLength == 0)
			return rightLength;
		else if(rightLength == 0)
			return leftLength;
		else if(Math.abs(leftLength - rightLength) > maxDistance)
			return maxDistance;

		int previousDistances[] = new int[leftLength + 1]; // 'previous' cost array, horizontally
		int distances[] = new int[leftLength + 1]; // cost array, horizontally

		int leftIndex;
		int rightIndex;

		char rightChar;

		for(leftIndex = 0; leftIndex <= leftLength; leftIndex++)
		{
			previousDistances[leftIndex] = leftIndex;
		}

		for(rightIndex = 1; rightIndex <= rightLength; rightIndex++)
		{
			rightChar = right.charAt(rightIndex - 1);
			distances[0] = rightIndex;

			for(leftIndex = 1; leftIndex <= leftLength; leftIndex++)
			{
				int insertionCost = distances[leftIndex - 1] + 1;
				int editCost = previousDistances[leftIndex] + 1;
				int deletionCost = previousDistances[leftIndex - 1];
				if(left.charAt(leftIndex - 1) != rightChar)
				{
					deletionCost++;
				}
				distances[leftIndex] = of(insertionCost, editCost, deletionCost).min().getAsInt();
			}

			// Swap current distance counts to 'previous row' distance counts
			int[] temp = previousDistances;
			previousDistances = distances;
			distances = temp;
		}

		// our last action in the above loop was to switch distances and
		// previousDistances, so
		// previousDistances now actually has the most recent cost counts
		return previousDistances[leftLength];
	}

	/**
	 * Returns {@code number} expressed as a position. For example 0 returns
	 * "zeroth", 1 returns "first" and so forth up to "fifth". Higher positions
	 * are described as "6th", "7th" and so on.
	 *
	 * @throws IllegalArgumentException if {@code number} is negative
	 */
	@Nonnull
	@CheckReturnValue
	public static String numberToPositionalString(int number)
	{
		check(number >= 0, "Negative numbers don't have positions");
		switch(number)
		{
		case 0:
			return "zeroth";
		case 1:
			return "first";
		case 2:
			return "second";
		case 3:
			return "third";
		case 4:
			return "fourth";
		case 5:
			return "fifth";

		}
		return Integer.toString(number) + "th";
	}

	/**
	 * Finds the {@code nth} occurrence of {@code needle} in {@code haystack}
	 *
	 * @param nth how many occurrences of {@code needle} that should occur before the returned index
	 * @param needle the string to search for
	 * @param haystack the string to search within
	 * @return the starting index of the {@code nth} occurrence of {@code needle} within
	 *         {@code haystack}, -1 if {@code nth} occurrences couldn't be found
	 */
	public static int indexOfNth(int nth, String needle, String haystack)
	{
		requireNonNull(haystack);
		requireNonNull(needle);
		check(nth > 0, "nth must be at least 1 (was %s)", nth);
		int occurencesFound = 0;
		int index = -1;
		while(occurencesFound < nth)
		{
			index = haystack.indexOf(needle, index + 1);
			occurencesFound++;
			if(index == -1)
			{
				break;
			}
		}
		return index;
	}

	/**
	 * @param part the string to repeat
	 * @param times how many times to repeat
	 * @return part, repeated {@code times} times
	 */
	@Nonnull
	@CheckReturnValue
	public static String repeat(String part, int times)
	{
		check(times >= 0, "Negative repitions is not supported. Was: ", times);
		StringBuilder builder = new StringBuilder(part.length() * times);
		for(int i = 0; i < times; i++)
			builder.append(part);
		return builder.toString();
	}
}
