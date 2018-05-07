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

import static java.util.Collections.emptyMap;
import static java.util.Collections.emptySet;
import static java.util.Objects.requireNonNull;
import static se.softhouse.common.guavaextensions.Preconditions2.checkNulls;
import static se.softhouse.jargo.ArgumentExceptions.withMessage;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import javax.annotation.concurrent.NotThreadSafe;
import javax.annotation.concurrent.ThreadSafe;

import se.softhouse.common.strings.StringsUtil;
import se.softhouse.jargo.CommandLineParserInstance.CommandInvocation;
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
	 * Corresponds to one of the {@link Argument#names()} that has been given from the command
	 * line. This is updated as soon as the parsing of a new argument begins.
	 * For indexed arguments this will be the meta description instead.
	 */
	private String currentArgumentName;
	private int currentArgumentIndex;
	private boolean endOfOptionsReceived;

	LinkedList<CommandInvocation> commandInvocations = new LinkedList<>();
	Optional<String> unfinishedCommand = Optional.empty();
	/**
	 * Allows commands to rejoin parsing of arguments if main arguments are specified in-between
	 */
	boolean temporaryRepitionAllowedForCommand;

	/**
	 * In case of {@link Command}s this may be the parser for a specific {@link Command} or just
	 * simply the main parser
	 */
	private CommandLineParserInstance currentParser;

	/**
	 * Stored here to allow help arguments to be preferred over indexed arguments
	 */
	private final Map<String, Argument<?>> helpArguments;

	/**
	 * @param actualArguments a list of arguments, will be modified
	 */
	private ArgumentIterator(Iterable<String> actualArguments, Map<String, Argument<?>> helpArguments)
	{
		this.arguments = checkNulls(actualArguments, "Argument strings may not be null");
		this.helpArguments = requireNonNull(helpArguments);
	}

	Argument<?> helpArgument(String currentArgument)
	{
		return helpArguments.get(currentArgument);
	}

	/**
	 * Returns <code>true</code> if {@link UsageTexts#END_OF_OPTIONS} hasn't been received yet.
	 */
	boolean allowsOptions()
	{
		return !endOfOptionsReceived;
	}

	void setCurrentParser(CommandLineParserInstance instance)
	{
		currentParser = instance;
	}

	void rememberAsCommand()
	{
		// The command has moved the index by 1 therefore the -1 to get the index of the
		// commandName
		unfinishedCommand = Optional.of(arguments.get(currentArgumentIndex - 1));
	}

	void rememberInvocationOfCommand(Command command, ParsedArguments argumentsToCommand, Argument<?> argumentSettingsForInvokedCommand,
			List<Argument<?>> commandArguments)
	{
		commandInvocations.add(new CommandInvocation(command, argumentsToCommand, argumentSettingsForInvokedCommand));
		unfinishedCommand = Optional.empty();

		for(Argument<?> possibleSubcommand : commandArguments)
		{
			for(CommandInvocation invocation : commandInvocations)
			{
				if(possibleSubcommand == invocation.argumentSettingsForInvokedCommand)
				{
					invocation.args.setRootArgs(argumentsToCommand);
				}
			}
		}
	}

	void executeAnyCommandsInTheOrderTheyWereReceived(ParsedArguments rootArgs)
	{
		for(CommandInvocation invocation : commandInvocations)
		{
			invocation.args.setRootArgs(rootArgs);
		}
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
		Iterator<CommandInvocation> commands = commandInvocations.descendingIterator();
		if(commands.hasNext())
			return commands.next().args.nonParsedArguments();
		return emptySet();
	}

	/**
	 * For indexed arguments in commands the used command name is returned so that when
	 * multiple commands (or multiple command names) are used it's clear which command the
	 * offending argument is part of
	 */
	String usedCommandName()
	{
		return unfinishedCommand.get();
	}

	static ArgumentIterator forArguments(Iterable<String> arguments, Map<String, Argument<?>> helpArguments)
	{
		return new ArgumentIterator(arguments, helpArguments);
	}

	static ArgumentIterator forArguments(Iterable<String> arguments)
	{
		return new ArgumentIterator(arguments, emptyMap());
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

		return nextArgument;
	}

	/**
	 * Skips {@link UsageTexts#END_OF_OPTIONS} if the parser hasn't received it yet.
	 * This is to allow the string {@link UsageTexts#END_OF_OPTIONS} as an indexed argument
	 * itself.
	 */
	private String skipAheadIfEndOfOptions(String nextArgument)
	{
		if(!endOfOptionsReceived && nextArgument.equals(UsageTexts.END_OF_OPTIONS))
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
					List<String> lines = Files.readAllLines(fileWithArguments.toPath(), StringsUtil.UTF8);
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

	private void appendArgumentsAtCurrentPosition(List<String> argumentsToAppend)
	{
		arguments.addAll(currentArgumentIndex, argumentsToAppend);
	}

	@Override
	public String toString()
	{
		return arguments.subList(currentArgumentIndex, arguments.size()).toString();
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

	CommandLineParserInstance currentParser()
	{
		return currentParser;
	}
}
