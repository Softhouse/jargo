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

import static java.util.Objects.requireNonNull;

import java.util.Collections;
import java.util.LinkedList;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import se.softhouse.common.strings.StringsUtil;
import se.softhouse.jargo.Argument.ParameterArity;
import se.softhouse.jargo.ArgumentExceptions.MissingParameterException;
import se.softhouse.jargo.ArgumentExceptions.MissingRequiredArgumentException;
import se.softhouse.jargo.ArgumentExceptions.SuggestiveArgumentException;
import se.softhouse.jargo.ArgumentExceptions.UnexpectedArgumentException;
import se.softhouse.jargo.CommandLineParserInstance.FoundArgumentHandler;

/**
 * Implementations of the {@link Completer} interface.
 */
final class Completers
{
	private Completers()
	{
	}

	/**
	 * @return a {@link Completer} doing nothing
	 */
	static Completer noCompleter()
	{
		return new Completer(){
			@Override
			public void completeIfApplicable(CommandLineParserInstance parser)
			{
			}

			@Override
			public SortedSet<String> complete(CommandLineParserInstance parser, String partOfWord, SortedSet<String> suggestions,
					ArgumentIterator iterator)
			{
				return null;
			}
		};
	}

	static Completer bashCompleter(Supplier<Map<String, String>> environmentVariables, Consumer<SortedSet<String>> output, Runnable exitMethod)
	{
		return new BashCompleter(environmentVariables, output, exitMethod);
	}

	static final class BashCompleter implements Completer
	{
		private final Supplier<Map<String, String>> environmentVariables;

		private final Runnable exitMethod;

		private final Consumer<SortedSet<String>> output;

		private BashCompleter(Supplier<Map<String, String>> environmentVariables, Consumer<SortedSet<String>> output, Runnable exitMethod)
		{
			this.environmentVariables = requireNonNull(environmentVariables);
			this.output = requireNonNull(output);
			this.exitMethod = requireNonNull(exitMethod);
		}

		@Override
		public void completeIfApplicable(CommandLineParserInstance parser)
		{
			Map<String, String> environment = environmentVariables.get();
			if(environment.containsKey("COMP_LINE"))
			{
				String lineToComplete = environment.get("COMP_LINE");
				int editingPosition = Integer.parseInt(environment.get("COMP_POINT"));
				// TODO: complete when editing in the middle of a line as well
				if(editingPosition == lineToComplete.length())
				{
					// TODO handle " and ' as bash does
					Stream<String> splits = Pattern.compile(" ").splitAsStream(lineToComplete);
					LinkedList<String> list = new LinkedList<>(splits.skip(1).collect(Collectors.toList()));
					if(lineToComplete.endsWith(" "))
					{
						list.add("");
					}
					String partOfWord = list.removeLast();
					SortedSet<String> suggestions = complete(parser, list, partOfWord);
					output.accept(suggestions);
				}
				exitMethod.run();
			}
		}

		// ThreadLocal so that tests can run concurrently, would have been a simple boolean otherwise
		static ThreadLocal<Boolean> hasSuggestedForLastArg = ThreadLocal.withInitial(() -> false);

		SortedSet<String> complete(CommandLineParserInstance parser, Iterable<String> args, String partOfWord)
		{
			hasSuggestedForLastArg.set(false);

			SortedSet<String> suggestions = new TreeSet<>();

			ParsedArguments holder = new ParsedArguments(parser);

			FoundArgumentHandler handler = new FoundArgumentHandler(){
				@Override
				public void handle(Argument<?> definition, ParsedArguments parsedArguments, ArgumentIterator iter, Locale inLocale)
				{
					if(iter.hasNext() || definition.parser().parameterArity() == ParameterArity.NO_ARGUMENTS)
					{
						parser.handleArgument(definition, parsedArguments, iter, inLocale);
					}
					else
					{
						iter.currentHolder().put(definition, null);
						definition.complete(partOfWord, iter).forEach(suggestions::add);
						hasSuggestedForLastArg.set(true);
						iter.removeCurrentIfDirty();
					}
				}
			};

			ArgumentIterator iterator = ArgumentIterator.forArguments(args, parser.helpArguments(), handler);
			iterator.setCurrentHolder(holder);

			return complete(parser, partOfWord, suggestions, iterator);
		}

		public SortedSet<String> complete(CommandLineParserInstance parser, String partOfWord, SortedSet<String> suggestions,
				ArgumentIterator iterator)
		{
			try
			{
				while(iterator.hasNext() && !hasSuggestedForLastArg.get())
				{
					// TODO complete if arg starts with UsageTexts.FILE_REFERENCE_PREFIX and it's the last char
					iterator.setCurrentArgumentName(iterator.next());
					parser.parseArgument(iterator, iterator.currentHolder());
				}
				iterator.setCurrentArgumentName(partOfWord);
			}
			catch(SuggestiveArgumentException exception)
			{
				suggestions.addAll(exception.suggestions());
			}
			catch(MissingRequiredArgumentException exception)
			{
				if(hasSuggestedForLastArg.get())
					return suggestions;

				// We are completing, so this is acceptable, suggest the missing args
				for(Argument<?> arg : exception.missingArguments())
				{
					for(String name : arg.names())
					{
						String namePlusSeparator = name + arg.separator();
						if(partOfWord.startsWith(namePlusSeparator))
						{
							// The required argument is currently being completed, complete it
							iterator.setCurrentArgumentName(namePlusSeparator);
							arg.complete(partOfWord, iterator).forEach(suggestions::add);
							iterator.currentHolder().put(arg, null);
							hasSuggestedForLastArg.set(true);
						}
						else if(namePlusSeparator.startsWith(iterator.getCurrentArgumentName()))
						{
							suggestions.add(namePlusSeparator);
							break;
						}
					}
				}
			}
			catch(MissingParameterException exception)
			{
				suggestions.clear();
				exception.argumentWithMissingParameter().complete(partOfWord, iterator).forEach(suggestions::add);
				return suggestions;
			}
			catch(ArgumentException exception)
			{
				throw exception;
			}

			if(hasSuggestedForLastArg.get())
				return suggestions;

			iterator.lastCommand().ifPresent(command -> {
				command.command.parser().indexedArgument(command.args).ifPresent(indexedArg -> {
					boolean wasGivenBefore = command.args.wasGiven(indexedArg);
					indexedArg.complete(partOfWord, iterator).forEach(suggestions::add);
					if(indexedArg.parser().parameterArity() == ParameterArity.VARIABLE_AMOUNT && wasGivenBefore)
					{
						// No other args are allowed from here on
						hasSuggestedForLastArg.set(true);
					}
				});
			});
			if(hasSuggestedForLastArg.get())
				return suggestions;

			// When all the previous arguments been parsed, it's time to complete the current arg
			completeLastArg(parser, iterator, partOfWord, iterator.currentHolder(), suggestions);
			Set<String> options = iterator.nonParsedArguments();
			SortedSet<String> matchingPrefix = StringsUtil.prefixesIgnoringCase(partOfWord, options, parser.locale());
			suggestions.addAll(matchingPrefix);

			if(suggestions.size() == 1 && !suggestions.first().equals(partOfWord))
			{
				iterator.isCompletingGeneratedSuggestion = true;
				// Try to be smart and predict the next suggestions as if the user would have pressed tab
				completeLastArg(parser, iterator, suggestions.first(), iterator.currentHolder(), suggestions);
			}
			return suggestions;
		}

		private void completeLastArg(CommandLineParserInstance parser, ArgumentIterator iterator, String partOfWord, ParsedArguments holder,
				SortedSet<String> suggestions)
		{
			iterator.setHandler(new FoundArgumentHandler(){
				@Override
				public void handle(Argument<?> definition, ParsedArguments parsedArguments, ArgumentIterator arguments, Locale inLocale)
				{
					// Avoid recursive completion if the arg to complete is a "finished" command
					if(!(definition.parser() instanceof Command))
					{
						definition.complete(partOfWord, iterator).forEach(suggestions::add);
					}

					// Simulate that it was parsed to exclude the arg from further being suggested
					holder.put(definition, null);

					arguments.removeCurrentIfDirty();
				}
			});

			iterator.appendArgumentsAtCurrentPosition(Collections.singletonList(partOfWord));
			iterator.setCurrentArgumentName(iterator.next());
			try
			{
				parser.parseArgument(iterator, holder);
			}
			catch(SuggestiveArgumentException | UnexpectedArgumentException exception)
			{
			}
		}
	}
}
