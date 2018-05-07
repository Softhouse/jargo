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

import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toList;
import static se.softhouse.common.guavaextensions.Preconditions2.check;
import static se.softhouse.common.guavaextensions.Predicates2.in;
import static se.softhouse.jargo.Argument.IS_REQUIRED;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;

import se.softhouse.jargo.internal.Texts.ProgrammaticErrors;

/**
 * Holds parsed arguments for a {@link CommandLineParser#parse(String...)} invocation.
 * Use {@link #get(Argument)} to fetch a parsed command line value.
 */
@Immutable
public final class ParsedArguments
{
	/**
	 * Stores results from {@link StringParser#parse(String, Locale)}
	 */
	@Nonnull private final Map<Argument<?>, Object> parsedArguments = new LinkedHashMap<>();
	@Nonnull private final Set<Argument<?>> allArguments;

	/**
	 * This gives commands access to any args to the root/parent command arguments
	 */
	private Optional<ParsedArguments> rootArgs = Optional.empty();

	/**
	 * Keeps a running total of how many indexed arguments that have been parsed
	 */
	private int indexedArgumentsParsed = 0;

	ParsedArguments(Set<Argument<?>> arguments)
	{
		allArguments = arguments;
	}

	/**
	 * Returns the parsed value for the given {@code argumentToFetch},
	 * if no value was given on the command line the default value is
	 * returned.
	 * 
	 * @see StringParser#defaultValue()
	 */
	@Nullable
	@CheckReturnValue
	public <T> T get(final Argument<T> argumentToFetch)
	{
		if(!wasGiven(argumentToFetch))
		{
			check(handlesArgument(argumentToFetch), ProgrammaticErrors.ILLEGAL_ARGUMENT, argumentToFetch);
			return argumentToFetch.defaultValue();
		}
		return getValue(argumentToFetch);
	}

	/**
	 * Checks if an {@link Argument} was given in the command line arguments
	 * 
	 * @param argument the {@link Argument} to check
	 * @return true if {@code argument} was given, that is, the default value will not be used
	 */
	public boolean wasGiven(Argument<?> argument)
	{
		return parsedArguments.containsKey(requireNonNull(argument)) || rootArgs.map(args -> args.wasGiven(argument)).orElse(false);
	}

	private boolean handlesArgument(Argument<?> arg)
	{
		return allArguments.contains(arg) || rootArgs.map(args -> args.handlesArgument(arg)).orElse(false);
	}

	@Override
	public String toString()
	{
		return parsedArguments.toString();
	}

	@Override
	public boolean equals(@Nullable Object other)
	{
		if(this == other)
			return true;
		if(!(other instanceof ParsedArguments))
			return false;

		return parsedArguments.equals(((ParsedArguments) other).parsedArguments);
	}

	@Override
	public int hashCode()
	{
		return parsedArguments.hashCode();
	}

	// Publicly this class is Immutable, CommandLineParserInstance is only allowed to modify it
	// during parsing

	<T> void put(final Argument<T> definition, @Nullable final T value)
	{
		if(definition.isIndexed())
		{
			indexedArgumentsParsed++;
		}
		parsedArguments.put(definition, value);
	}

	/**
	 * Makes the parsed value for {@code definition} unmodifiable.
	 */
	<T> void finalize(final Argument<T> definition)
	{
		T value = getValue(definition);
		T finalizedValue = definition.finalizer().apply(value);
		put(definition, finalizedValue);
	}

	<T> T getValue(final Argument<T> definition)
	{
		if(parsedArguments.containsKey(definition))
		{
			// Safe because put guarantees that the map is heterogeneous
			@SuppressWarnings("unchecked")
			T value = (T) parsedArguments.get(definition);
			return value;
		}
		return rootArgs.map(args -> args.getValue(definition)).orElse(null);
	}

	Collection<Argument<?>> requiredArgumentsLeft()
	{
		return allArguments.stream().filter(IS_REQUIRED.and(in(parsedArguments.keySet()).negate())).collect(toList());
	}

	int indexedArgumentsParsed()
	{
		return indexedArgumentsParsed;
	}

	Set<Argument<?>> parsedArguments()
	{
		return parsedArguments.keySet();
	}

	Set<String> nonParsedArguments()
	{
		Set<String> validArguments = new HashSet<>(allArguments.size());
		for(Argument<?> argument : allArguments)
		{
			boolean wasGiven = wasGiven(argument);
			if(!wasGiven || argument.isAllowedToRepeat())
			{
				for(String name : argument.names())
				{
					if(argument.separator().equals(ArgumentBuilder.DEFAULT_SEPARATOR) || argument.isPropertyMap())
					{
						validArguments.add(name);
					}
					else
					{
						validArguments.add(name + argument.separator());
					}

				}
			}
		}
		return validArguments;
	}

	void setRootArgs(ParsedArguments rootArgs)
	{
		// If this has been set by a parent command it should not be cleared, this makes it possible to have
		// access to arguments to a chain of parent commands
		if(!this.rootArgs.isPresent())
		{
			this.rootArgs = Optional.of(rootArgs);
		}
	}
}
