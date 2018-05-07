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
package se.softhouse.jargo;

import static se.softhouse.common.guavaextensions.Lists2.size;
import static se.softhouse.common.guavaextensions.Preconditions2.check;
import static se.softhouse.common.guavaextensions.Sets2.union;
import static se.softhouse.common.strings.Describables.format;
import static se.softhouse.common.strings.StringsUtil.NEWLINE;
import static se.softhouse.common.strings.StringsUtil.TAB;
import static se.softhouse.common.strings.StringsUtil.startsWithAndHasMore;
import static se.softhouse.jargo.Argument.IS_OF_VARIABLE_ARITY;
import static se.softhouse.jargo.Argument.IS_REPEATED;
import static se.softhouse.jargo.Argument.IS_REQUIRED;
import static se.softhouse.jargo.Argument.ParameterArity.NO_ARGUMENTS;
import static se.softhouse.jargo.ArgumentBuilder.DEFAULT_SEPARATOR;
import static se.softhouse.jargo.ArgumentExceptions.forMissingArguments;
import static se.softhouse.jargo.ArgumentExceptions.forUnallowedRepetitionArgument;
import static se.softhouse.jargo.ArgumentExceptions.withMessage;
import static se.softhouse.jargo.ArgumentExceptions.wrapException;
import static se.softhouse.jargo.CommandLineParser.US_BY_DEFAULT;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;

import se.softhouse.common.collections.CharacterTrie;
import se.softhouse.common.guavaextensions.Lists2;
import se.softhouse.common.strings.StringsUtil;
import se.softhouse.jargo.ArgumentExceptions.MissingRequiredArgumentException;
import se.softhouse.jargo.ArgumentExceptions.UnexpectedArgumentException;
import se.softhouse.jargo.StringParsers.InternalStringParser;
import se.softhouse.jargo.internal.Texts.ProgrammaticErrors;
import se.softhouse.jargo.internal.Texts.UserErrors;

/**
 * A snapshot view of a {@link CommandLineParser} configuration.
 */
@Immutable
final class CommandLineParserInstance
{
	/**
	 * A list where arguments created without names is put
	 */
	@Nonnull private final List<Argument<?>> indexedArguments;

	/**
	 * A map containing short-named arguments, long-named arguments and named arguments that ignore
	 * case
	 */
	@Nonnull private final NamedArguments namedArguments;

	/**
	 * Stores arguments that either has a special {@link ArgumentBuilder#separator()}, or is a
	 * {@link ArgumentBuilder#asPropertyMap()}. Handles {@link ArgumentBuilder#ignoreCase()} as
	 * well.
	 */
	@Nonnull private final SpecialArguments specialArguments;

	/**
	 * Stored separately (as well) as {@link Command}s need access to them through the
	 * {@link ArgumentIterator}. This avoids traversing all named arguments for each parse
	 * checking for help argument definitions
	 */
	@Nonnull private final Map<String, Argument<?>> helpArguments;

	@Nonnull private final Set<Argument<?>> allArguments;

	/**
	 * Used by {@link Command} to indicate that this parser is part of a {@link Command}
	 */
	private final boolean isCommandParser;

	private final ProgramInformation programInformation;

	private final Locale locale;

	CommandLineParserInstance(Iterable<Argument<?>> argumentDefinitions, ProgramInformation information, Locale locale, boolean isCommandParser)
	{
		int nrOfArgumentsToHandle = size(argumentDefinitions);
		this.indexedArguments = new ArrayList<>(nrOfArgumentsToHandle);
		this.namedArguments = new NamedArguments(nrOfArgumentsToHandle);
		this.specialArguments = new SpecialArguments();
		this.helpArguments = new HashMap<>();
		this.allArguments = new LinkedHashSet<>(nrOfArgumentsToHandle);

		this.programInformation = information;
		this.locale = locale;
		this.isCommandParser = isCommandParser;
		for(Argument<?> definition : argumentDefinitions)
		{
			addArgumentDefinition(definition);
		}
		verifyThatIndexedAndRequiredArgumentsWasGivenBeforeAnyOptionalArguments();
		verifyUniqueMetasForRequiredAndIndexedArguments();
		verifyThatOnlyOneArgumentIsOfVariableArity();
		verifyThatNoIndexedArgumentIsRepeated();
	}

	CommandLineParserInstance(Iterable<Argument<?>> argumentDefinitions)
	{
		this(argumentDefinitions, ProgramInformation.AUTO, US_BY_DEFAULT, false);
	}

	private void addArgumentDefinition(final Argument<?> definition)
	{
		if(definition.isIndexed())
		{
			indexedArguments.add(definition);
		}
		else
		{
			for(String name : definition.names())
			{
				addNamedArgumentDefinition(name, definition);
			}
		}
		boolean added = allArguments().add(definition);
		check(added, ProgrammaticErrors.UNIQUE_ARGUMENT, definition);
	}

	private void addNamedArgumentDefinition(final String name, final Argument<?> definition)
	{
		Argument<?> oldDefinition = null;
		String separator = definition.separator();
		if(definition.isPropertyMap())
		{
			oldDefinition = specialArguments.put(name, definition);
		}
		else if(separator.equals(DEFAULT_SEPARATOR))
		{
			oldDefinition = namedArguments.put(name, definition);
		}
		else
		{
			oldDefinition = specialArguments.put(name + separator, definition);
		}
		if(definition.isHelpArgument())
		{
			helpArguments.put(name, definition);
		}
		check(oldDefinition == null, ProgrammaticErrors.NAME_COLLISION, name);
	}

	/**
	 * Specifying the optional argument before the required argument would make the optional
	 * argument required
	 */
	private void verifyThatIndexedAndRequiredArgumentsWasGivenBeforeAnyOptionalArguments()
	{
		int lastRequiredIndexedArgument = 0;
		int firstOptionalIndexedArgument = Integer.MAX_VALUE;
		for(int i = 0; i < indexedArguments.size(); i++)
		{
			Argument<?> indexedArgument = indexedArguments.get(i);
			if(indexedArgument.isRequired())
			{
				lastRequiredIndexedArgument = i;
			}
			else if(firstOptionalIndexedArgument == Integer.MAX_VALUE)
			{
				firstOptionalIndexedArgument = i;
			}
		}
		check(	lastRequiredIndexedArgument <= firstOptionalIndexedArgument, ProgrammaticErrors.REQUIRED_ARGUMENTS_BEFORE_OPTIONAL,
				firstOptionalIndexedArgument, lastRequiredIndexedArgument);
	}

	/**
	 * Otherwise the error texts becomes ambiguous
	 */
	private void verifyUniqueMetasForRequiredAndIndexedArguments()
	{
		Set<String> metasForRequiredAndIndexedArguments = new HashSet<>(indexedArguments.size());
		indexedArguments.stream().filter(IS_REQUIRED).forEach(indexedArgument -> {
			String meta = indexedArgument.metaDescriptionInRightColumn();
			boolean metaWasUnique = metasForRequiredAndIndexedArguments.add(meta);
			check(metaWasUnique, ProgrammaticErrors.UNIQUE_METAS, meta);
		});
	}

	/**
	 * How would one know when the first argument considers itself satisfied?
	 */
	private void verifyThatOnlyOneArgumentIsOfVariableArity()
	{
		Collection<Argument<?>> indexedVariableArityArguments = indexedArguments.stream().filter(IS_OF_VARIABLE_ARITY).collect(Collectors.toList());
		check(indexedVariableArityArguments.size() <= 1, ProgrammaticErrors.SEVERAL_VARIABLE_ARITY_PARSERS, indexedVariableArityArguments);
	}

	private void verifyThatNoIndexedArgumentIsRepeated()
	{
		Collection<Argument<?>> indexedRepeatedArguments = indexedArguments.stream().filter(IS_REPEATED).collect(Collectors.toList());
		check(indexedRepeatedArguments.isEmpty(), ProgrammaticErrors.INDEXED_AND_REPEATED_ARGUMENT, indexedRepeatedArguments);
	}

	@Nonnull
	ParsedArguments parse(final Iterable<String> actualArguments) throws ArgumentException
	{
		ArgumentIterator arguments = ArgumentIterator.forArguments(actualArguments, helpArguments);
		return parse(arguments, locale());
	}

	@Nonnull
	ParsedArguments parse(ArgumentIterator arguments, Locale inLocale) throws ArgumentException
	{
		ParsedArguments holder = parseArguments(arguments, inLocale);

		Collection<Argument<?>> missingArguments = holder.requiredArgumentsLeft();
		if(missingArguments.size() > 0)
			throw forMissingArguments(missingArguments).withUsage(usage(inLocale));

		for(Argument<?> arg : holder.parsedArguments())
		{
			holder.finalize(arg);
			limitArgument(arg, holder, inLocale);
		}
		if(!isCommandParser())
		{
			arguments.executeAnyCommandsInTheOrderTheyWereReceived(holder);
		}
		return holder;
	}

	private ParsedArguments parseArguments(final ArgumentIterator iterator, Locale inLocale) throws ArgumentException
	{
		ParsedArguments holder = new ParsedArguments(allArguments());
		return parseArguments(holder, iterator, inLocale);
	}

	ParsedArguments parseArguments(ParsedArguments holder, final ArgumentIterator iterator, Locale inLocale) throws ArgumentException
	{
		iterator.setCurrentParser(this);
		while(iterator.hasNext())
		{
			Argument<?> definition = null;
			try
			{
				iterator.setCurrentArgumentName(iterator.next());
				definition = getDefinitionForCurrentArgument(iterator, holder, true);
				if(definition == null)
				{
					break;
				}
				parseArgument(iterator, holder, definition, inLocale);
			}
			catch(ArgumentException e)
			{
				String descriptiveName = iterator.getCurrentArgumentName();
				if(e instanceof MissingRequiredArgumentException)
				{
					descriptiveName = iterator.unfinishedCommand.orElse(descriptiveName);
				}
				e.withUsedArgumentName(descriptiveName);
				if(definition != null)
				{
					e.withUsageReference(definition);
				}
				throw e.withUsage(usage(inLocale));
			}
		}
		return holder;
	}

	private <T> void parseArgument(final ArgumentIterator arguments, final ParsedArguments parsedArguments, final Argument<T> definition,
			Locale inLocale) throws ArgumentException
	{
		InternalStringParser<T> parser = definition.parser();

		boolean commandOverride = (parser instanceof Command) && arguments.temporaryRepitionAllowedForCommand;
		if(parsedArguments.wasGiven(definition) && !definition.isAllowedToRepeat() && !definition.isPropertyMap() && !commandOverride)
			throw forUnallowedRepetitionArgument(arguments.current());

		T oldValue = parsedArguments.getValue(definition);

		T parsedValue = parser.parse(arguments, oldValue, definition, inLocale);

		parsedArguments.put(definition, parsedValue);
	}

	/**
	 * @return a definition that defines how to handle the current argument
	 * @throws UnexpectedArgumentException if no definition could be found
	 *             for the current argument
	 */
	@Nullable
	private Argument<?> getDefinitionForCurrentArgument(final ArgumentIterator iterator, final ParsedArguments holder, boolean fallbackToParent)
			throws ArgumentException
	{
		if(iterator.allowsOptions())
		{
			Argument<?> byName = lookupByName(iterator);
			if(byName != null)
				return byName;

			Argument<?> option = batchOfShortNamedArguments(iterator, holder);
			if(option != null)
				return option;
		}

		Argument<?> indexedArgument = indexedArgument(iterator, holder);
		if(indexedArgument != null)
			return indexedArgument;

		// Rolling back here means that the parent parser/command will receive the argument
		// instead, maybe it can handle it
		if(isCommandParser())
		{
			if(fallbackToParent)
			{
				iterator.previous();
			}
			return null;
		}

		// If we have previously rollbacked(to parse main args in the middle of a command's arguments),
		// then let's also try to rejoin parsing of that command's arguments again
		Iterator<CommandInvocation> lastCommandInvocation = iterator.commandInvocations.descendingIterator();
		if(lastCommandInvocation.hasNext())
		{
			CommandInvocation invocation = lastCommandInvocation.next();
			Argument<?> subCommandHasSupport = invocation.command.parser().getDefinitionForCurrentArgument(iterator, invocation.args, false);
			if(subCommandHasSupport != null)
			{
				// Let the subcommand see the argument again
				iterator.previous();
				iterator.temporaryRepitionAllowedForCommand = true;
				return invocation.argumentSettingsForInvokedCommand;
			}
		}

		guessAndSuggestIfCloseMatch(iterator, holder);

		// We're out of order, tell the user what we didn't like
		throw ArgumentExceptions.forUnexpectedArgument(iterator);
	}

	/**
	 * Looks for {@link ArgumentBuilder#names(String...) named arguments}
	 */
	private Argument<?> lookupByName(ArgumentIterator arguments)
	{
		String currentArgument = arguments.current();
		// Ordinary, named, arguments that directly matches the argument
		Argument<?> definition = namedArguments.get(currentArgument);
		if(definition != null)
			return definition;

		// Property Maps,Special separator, ignore case arguments
		Entry<String, Argument<?>> entry = specialArguments.get(currentArgument);
		if(entry != null)
		{
			// Remove "-D" from "-Dkey=value"
			String valueAfterSeparator = currentArgument.substring(entry.getKey().length());
			arguments.setNextArgumentTo(valueAfterSeparator);
			return entry.getValue();
		}
		definition = arguments.helpArgument(currentArgument);
		return definition;
	}

	/**
	 * Batch of short-named optional arguments
	 * For instance, "-fs" was used instead of "-f -s"
	 */
	private Argument<?> batchOfShortNamedArguments(ArgumentIterator arguments, ParsedArguments holder) throws ArgumentException
	{
		String currentArgument = arguments.current();
		if(startsWithAndHasMore(currentArgument, "-"))
		{
			List<Character> givenCharacters = Lists2.charactersOf(currentArgument.substring(1));
			Set<Argument<?>> foundOptions = new LinkedHashSet<>(givenCharacters.size());
			Argument<?> lastOption = null;
			for(Character optionName : givenCharacters)
			{
				lastOption = namedArguments.get("-" + optionName);
				if(lastOption == null || lastOption.parser().parameterArity() != NO_ARGUMENTS || !foundOptions.add(lastOption))
				{
					// Abort as soon as an unexpected character is discovered
					break;
				}
			}
			// Only accept the argument if all characters could be matched and no duplicate
			// characters were found
			if(foundOptions.size() == givenCharacters.size())
			{
				// The last option is handled with the return
				foundOptions.remove(lastOption);

				// A little ugly that this get method has side-effects but the alternative is to
				// return a list of arguments to parse and as this should be a rare case it's not
				// worth it, the result is the same at least
				for(Argument<?> option : foundOptions)
				{
					parseArgument(arguments, holder, option, null);
				}
				return lastOption;
			}
		}
		return null;
	}

	/**
	 * Looks for {@link ArgumentBuilder#names(String...) indexed arguments}
	 */
	private Argument<?> indexedArgument(ArgumentIterator arguments, ParsedArguments holder)
	{
		if(holder.indexedArgumentsParsed() < indexedArguments.size())
		{
			Argument<?> definition = indexedArguments.get(holder.indexedArgumentsParsed());
			arguments.previous();
			// This helps the error messages explain which of the indexed arguments that failed
			if(isCommandParser())
			{
				arguments.setCurrentArgumentName(arguments.usedCommandName());
			}
			else
			{
				arguments.setCurrentArgumentName(definition.metaDescriptionInRightColumn());
			}
			return definition;
		}
		return null;
	}

	private static final int ONLY_REALLY_CLOSE_MATCHES = 4;

	/**
	 * Suggests probable, valid, alternatives for a faulty argument, based on the
	 * {@link StringsUtil#levenshteinDistance(String, String)}
	 */
	private void guessAndSuggestIfCloseMatch(ArgumentIterator arguments, ParsedArguments holder) throws ArgumentException
	{
		Set<String> availableArguments = union(holder.nonParsedArguments(), arguments.nonParsedArguments());

		if(!availableArguments.isEmpty())
		{
			List<String> suggestions = StringsUtil.closestMatches(arguments.getCurrentArgumentName(), availableArguments, ONLY_REALLY_CLOSE_MATCHES);
			if(!suggestions.isEmpty())
				throw withMessage(format(UserErrors.SUGGESTION, arguments.getCurrentArgumentName(), String.join(NEWLINE + TAB, suggestions)));
		}
	}

	private <T> void limitArgument(@Nonnull Argument<T> arg, ParsedArguments holder, Locale inLocale) throws ArgumentException
	{
		try
		{
			arg.checkLimit(holder.getValue(arg));
		}
		catch(IllegalArgumentException e)
		{
			throw wrapException(e).withUsageReference(arg).withUsedArgumentName(arg.toString()).withUsage(usage(inLocale));
		}
		catch(ArgumentException e)
		{
			throw e.withUsageReference(arg).withUsage(usage(inLocale));
		}
	}

	/**
	 * Returns <code>true</code> if this is a parser for a specific {@link Command}
	 */
	boolean isCommandParser()
	{
		return isCommandParser;
	}

	Set<Argument<?>> allArguments()
	{
		return allArguments;
	}

	ProgramInformation programInformation()
	{
		return programInformation;
	}

	Locale locale()
	{
		return locale;
	}

	@CheckReturnValue
	@Nonnull
	Usage usage(Locale inLocale)
	{
		return new Usage(allArguments(), inLocale, programInformation(), isCommandParser());
	}

	@CheckReturnValue
	@Nonnull
	ArgumentException helpFor(ArgumentIterator arguments, Locale inLocale) throws ArgumentException
	{
		ArgumentException e = withMessage("Help requested with " + arguments.current());
		Usage usage = null;
		if(arguments.hasNext())
		{
			arguments.next();
			Argument<?> argument = lookupByName(arguments);
			if(argument == null)
				throw withMessage(format(UserErrors.UNKNOWN_ARGUMENT, arguments.current()));
			usage = new Usage(Arrays.<Argument<?>>asList(argument), inLocale, programInformation(), isCommandParser());
			if(isCommandParser())
			{
				String withCommandReference = ". Usage for " + argument + " (argument to " + arguments.usedCommandName() + "):";
				e.withUsageReference(withCommandReference);
			}
			else
			{
				e.withUsageReference(argument);
			}
		}
		else
		{
			usage = usage(inLocale);
			if(isCommandParser())
			{
				e.withUsageReference(". See usage for " + arguments.usedCommandName() + " below:");
			}
			else
			{
				e.withUsageReference(". See usage below:");
			}
		}
		return e.withUsage(usage);
	}

	@Override
	public String toString()
	{
		return usage(locale()).toString();
	}

	static final class CommandInvocation
	{
		private final Command command;
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
			command.execute(args);
		}

		@Override
		public String toString()
		{
			return command.commandName() + "" + args;
		}
	}

	private static final class NamedArguments
	{
		private final Map<String, Argument<?>> namedArguments;

		NamedArguments(int expectedSize)
		{
			namedArguments = new HashMap<>(expectedSize);
		}

		Argument<?> put(String name, Argument<?> definition)
		{
			if(definition.isIgnoringCase())
				return namedArguments.put(name.toLowerCase(Locale.ENGLISH), definition);
			return namedArguments.put(name, definition);
		}

		Argument<?> get(String name)
		{
			Argument<?> definition = namedArguments.get(name);
			if(definition != null)
				return definition;

			String lowerCase = name.toLowerCase(Locale.ENGLISH);
			definition = namedArguments.get(lowerCase);
			if(definition != null && definition.isIgnoringCase())
				return definition;
			return null;
		}
	}

	private static final class SpecialArguments
	{
		private final CharacterTrie<Argument<?>> specialArguments;

		SpecialArguments()
		{
			specialArguments = CharacterTrie.newTrie();
		}

		Argument<?> put(String name, Argument<?> definition)
		{
			if(definition.isIgnoringCase())
				return specialArguments.put(name.toLowerCase(Locale.ENGLISH), definition);
			return specialArguments.put(name, definition);
		}

		Entry<String, Argument<?>> get(String name)
		{
			Entry<String, Argument<?>> entry = specialArguments.findLongestPrefix(name);
			if(entry != null)
				return entry;

			String lowerCase = name.toLowerCase(Locale.ENGLISH);
			entry = specialArguments.findLongestPrefix(lowerCase);
			if(entry != null && entry.getValue().isIgnoringCase())
				return entry;
			return null;
		}
	}
}
