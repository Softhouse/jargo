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

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Collections.emptyMap;
import static java.util.Objects.requireNonNull;
import static se.softhouse.common.guavaextensions.Preconditions2.checkNulls;
import static se.softhouse.jargo.ArgumentExceptions.withMessage;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import javax.annotation.concurrent.NotThreadSafe;
import javax.annotation.concurrent.ThreadSafe;

import se.softhouse.jargo.CommandLineParserInstance.FoundArgumentHandler;
import se.softhouse.jargo.internal.Texts.UsageTexts;

/**
 * Wraps a list of given arguments and remembers
 * which argument that is currently being parsed. Plays a key role in making
 * {@link CommandLineParserInstance} {@link ThreadSafe} as it holds the current state of a parse
 * invocation. Internal class.
 */
@NotThreadSafe
final class ArgumentIterator implements Iterator<String>
{
	private final List<String> arguments;

	/**
	 * Stored here to allow help arguments to be preferred over indexed arguments
	 */
	private final Map<String, Argument<?>> helpArguments;

	/**
	 * Corresponds to one of the {@link Argument#names()} that has been given from the command
	 * line. This is updated as soon as the parsing of a new argument begins.
	 * For indexed arguments this will be the meta description instead.
	 */
	private String currentArgumentName;
	private int currentArgumentIndex;
	private boolean endOfOptionsReceived;

	private final LinkedList<CommandInvocation> commandInvocations = new LinkedList<>();

	/**
	 * Allows commands to rejoin parsing of arguments if main arguments are specified in-between
	 */
	boolean temporaryRepitionAllowedForCommand;

	/**
	 * Hints that the word to complete was not actually given yet
	 */
	boolean isCompletingGeneratedSuggestion;

	private ParsedArguments currentHolder;

	private FoundArgumentHandler handler;

	/**
	 * Arguments have been manipulated and not yet consumed
	 */
	private boolean dirty;

	/**
	 * @param actualArguments a list of arguments, will be modified
	 */
	private ArgumentIterator(Iterable<String> actualArguments, Map<String, Argument<?>> helpArguments, FoundArgumentHandler handler)
	{
		this.arguments = checkNulls(actualArguments, "Argument strings may not be null");
		this.helpArguments = requireNonNull(helpArguments);
		this.handler = requireNonNull(handler);
	}

	Argument<?> helpArgument(String currentArgument)
	{
		return helpArguments.get(currentArgument);
	}

	ParsedArguments currentHolder()
	{
		return currentHolder;
	}

	/**
	 * Returns <code>true</code> if {@link UsageTexts#END_OF_OPTIONS} hasn't been received yet.
	 */
	boolean allowsOptions()
	{
		return !endOfOptionsReceived;
	}

	void setCurrentHolder(ParsedArguments currentHolder)
	{
		this.currentHolder = currentHolder;
	}

	static final class CommandInvocation
	{
		final Command command;
		final ParsedArguments args;
		final Argument<?> argumentSettingsForInvokedCommand;

		CommandInvocation(Command command, ParsedArguments args, Argument<?> argumentSettingsForInvokedCommand)
		{
			this.command = command;
			this.args = args;
			this.argumentSettingsForInvokedCommand = argumentSettingsForInvokedCommand;
		}

		void execute()
		{
			try
			{
				command.execute(args);
			}
			catch(ArgumentException exception)
			{
				exception.withUsage(argumentSettingsForInvokedCommand.usage());
				throw exception;
			}
		}

		@Override
		public String toString()
		{
			return command.commandName() + "" + args;
		}
	}

	void rememberInvocationOfCommand(Command command, ParsedArguments argumentsToCommand, Argument<?> argumentSettingsForInvokedCommand)
	{
		commandInvocations.add(new CommandInvocation(command, argumentsToCommand, argumentSettingsForInvokedCommand));
	}

	Optional<CommandInvocation> lastCommand()
	{
		Iterator<CommandInvocation> lastCommandInvocation = commandInvocations.descendingIterator();
		if(lastCommandInvocation.hasNext())
			return Optional.of(lastCommandInvocation.next());
		return Optional.empty();
	}

	void validateAndFinalize(Locale locale)
	{
		for(CommandInvocation invocation : commandInvocations)
		{
			invocation.command.parser().validateAndFinalize(this, invocation.argumentSettingsForInvokedCommand, invocation.args, locale);
		}
	}

	void executeAnyCommandsInTheOrderTheyWereReceived()
	{
		for(CommandInvocation invocation : commandInvocations)
		{
			invocation.execute();
		}
	}

	/**
	 * Returns any non-parsed arguments to the last command that was to be executed
	 */
	Set<String> nonParsedArguments()
	{
		HashSet<String> result = commandInvocations.stream().map(ci -> ci.args.nonParsedArguments()).collect(	HashSet::new, (l, r) -> l.addAll(r),
																												(l, r) -> l.addAll(r));
		result.addAll(currentHolder.nonParsedArguments());
		return result;
	}

	/**
	 * For indexed arguments in commands the used command name is returned so that when
	 * multiple commands (or multiple command names) are used it's clear which command the
	 * offending argument is part of
	 */
	Optional<String> usedCommandName()
	{
		return lastCommand().map(invocation -> invocation.argumentSettingsForInvokedCommand.toString());
	}

	static ArgumentIterator forArguments(Iterable<String> arguments, Map<String, Argument<?>> helpArguments, FoundArgumentHandler handler)
	{
		return new ArgumentIterator(arguments, helpArguments, handler);
	}

	static ArgumentIterator forArguments(Iterable<String> arguments)
	{
		FoundArgumentHandler noOpHandler = (d, p, a, l) -> {
		};
		return new ArgumentIterator(arguments, emptyMap(), noOpHandler);
	}

	/**
	 * Returns the string that was given by the previous {@link #next()} invocation.
	 */
	String current()
	{
		return arguments.get(currentArgumentIndex - 1);
	}

	@Override
	public boolean hasNext()
	{
		return currentArgumentIndex < arguments.size();
	}

	@Override
	public String next()
	{
		String nextArgument = arguments.get(currentArgumentIndex++);
		nextArgument = skipAheadIfEndOfOptions(nextArgument);
		nextArgument = readArgumentsFromFile(nextArgument);
		dirty = false;

		return nextArgument;
	}

	/**
	 * Skips {@link UsageTexts#END_OF_OPTIONS} if the parser hasn't received it yet.
	 * This is to allow the string {@link UsageTexts#END_OF_OPTIONS} as an indexed argument
	 * itself.
	 */
	private String skipAheadIfEndOfOptions(String nextArgument)
	{
		if(!endOfOptionsReceived && nextArgument.equals(UsageTexts.END_OF_OPTIONS) && hasNext())
		{
			endOfOptionsReceived = true;
			return next();
		}
		return nextArgument;
	}

	/**
	 * Reads arguments from files if the argument starts with a
	 * {@link UsageTexts#FILE_REFERENCE_PREFIX}.
	 */
	private String readArgumentsFromFile(String nextArgument)
	{
		// TODO(jontejj): add possibility to disable this feature? It has some security
		// implications as the caller can input any files and if this parser was exposed from a
		// server...
		if(nextArgument.startsWith(UsageTexts.FILE_REFERENCE_PREFIX))
		{
			String filename = nextArgument.substring(1);
			File fileWithArguments = new File(filename);
			if(fileWithArguments.exists())
			{
				try
				{
					List<String> lines = Files.readAllLines(fileWithArguments.toPath(), UTF_8);
					appendArgumentsAtCurrentPosition(lines);
				}
				catch(IOException errorWhileReadingFile)
				{
					throw withMessage("Failed while reading arguments from: " + filename, errorWhileReadingFile);
				}
				// Recursive call adds support for file references from within the file itself
				return next();
			}
		}
		return nextArgument;
	}

	void appendArgumentsAtCurrentPosition(List<String> argumentsToAppend)
	{
		arguments.addAll(currentArgumentIndex, argumentsToAppend);
	}

	@Override
	public String toString()
	{
		List<String> parsed = arguments.subList(0, currentArgumentIndex);
		List<String> remaining = arguments.subList(currentArgumentIndex, arguments.size());
		return "Parsed: " + parsed + ", Current: " + currentArgumentName + ", Remaining: " + remaining;
	}

	/**
	 * The opposite of {@link #next()}. In short, it makes this iterator return what
	 * {@link #next()} returned last time once again.
	 * 
	 * @return the {@link #current()} argument
	 */
	String previous()
	{
		return arguments.get(--currentArgumentIndex);
	}

	int nrOfRemainingArguments()
	{
		return arguments.size() - currentArgumentIndex;
	}

	void setNextArgumentTo(String newNextArgumentString)
	{
		arguments.set(--currentArgumentIndex, newNextArgumentString);
		dirty = true;
	}

	boolean hasPrevious()
	{
		return currentArgumentIndex > 0;
	}

	void setCurrentArgumentName(String argumentName)
	{
		currentArgumentName = argumentName;
	}

	String getCurrentArgumentName()
	{
		return currentArgumentName;
	}

	FoundArgumentHandler handler()
	{
		return handler;
	}

	void setHandler(FoundArgumentHandler handler)
	{
		this.handler = handler;
	}

	/**
	 * Because {@link CommandLineParserInstance#lookupByName} might use
	 * {@link #setNextArgumentTo(String)} and leave the iterator behind in a bad state
	 */
	void removeCurrentIfDirty()
	{
		if(dirty)
		{
			arguments.remove(currentArgumentIndex);
			dirty = false;
		}
	}

	ParsedArguments findParentHolderFor(Argument<ParsedArguments> argument)
	{
		for(CommandInvocation invocation : commandInvocations)
		{
			Optional<ParsedArguments> parentHolder = invocation.args.findParentHolderFor(argument);
			if(parentHolder.isPresent())
				return parentHolder.get();
		}
		return currentHolder().findParentHolderFor(argument).get();
	}
}
