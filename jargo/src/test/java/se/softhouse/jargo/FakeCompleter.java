/* Copyright 2018 jonatanjonsson
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */
package se.softhouse.jargo;

import static org.fest.assertions.Assertions.assertThat;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.SortedSet;
import java.util.concurrent.atomic.AtomicReference;

import se.softhouse.jargo.ArgumentException;
import se.softhouse.jargo.ArgumentExceptions;
import se.softhouse.jargo.CommandLineParser;
import se.softhouse.jargo.Completers;

public final class FakeCompleter
{
	private FakeCompleter()
	{
	}

	public static SortedSet<String> completeWithSeparator(CommandLineParser parser, String separator, String ... args)
	{
		String programName = "program";
		Map<String, String> fakeEnv = fakeEnv(programName, separator, args);
		AtomicReference<SortedSet<String>> suggestions = new AtomicReference<SortedSet<String>>(null);
		try
		{
			parser.completer(Completers.bashCompleter(() -> fakeEnv, suggestions::set, () -> {
				throw ArgumentExceptions.withMessage("Done with completions");
			})).parse();
		}
		catch(ArgumentException expected)
		{
			if(!"Done with completions".equals(expected.getMessage()))
				throw expected;
		}
		assertThat(suggestions.get()).describedAs("No suggestions set. Completer failure").isNotNull();
		return suggestions.get();
	}

	public static SortedSet<String> complete(CommandLineParser parser, String ... args)
	{
		return completeWithSeparator(parser, " ", args);
	}

	public static Map<String, String> fakeEnv(String programName, String separator, String ... args)
	{
		Objects.requireNonNull(programName);
		Map<String, String> fakeEnv = new HashMap<>();
		String compLine = programName + " " + String.join(separator, args);
		fakeEnv.put("COMP_LINE", compLine);
		fakeEnv.put("COMP_POINT", "" + compLine.length());
		return fakeEnv;
	}
}
