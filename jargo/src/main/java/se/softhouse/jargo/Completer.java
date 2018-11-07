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

import java.util.SortedSet;

/**
 * Provides a way to support <a href="https://en.wikipedia.org/wiki/Command-line_completion">tab-completions</a> for terminals.
 */
interface Completer
{
	/**
	 * The first trigger of the completion
	 */
	void completeIfApplicable(CommandLineParserInstance parser);

	/**
	 * A continuation of a completion invocation
	 */
	SortedSet<String> complete(CommandLineParserInstance parser, String partOfWord, SortedSet<String> suggestions, ArgumentIterator iterator);
}
