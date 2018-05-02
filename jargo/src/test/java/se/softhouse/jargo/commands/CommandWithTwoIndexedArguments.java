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
package se.softhouse.jargo.commands;

import static org.fest.assertions.Assertions.assertThat;
import static se.softhouse.jargo.Arguments.integerArgument;
import static se.softhouse.jargo.Arguments.stringArgument;

import java.util.Arrays;
import java.util.List;

import se.softhouse.jargo.Argument;
import se.softhouse.jargo.Command;
import se.softhouse.jargo.ParsedArguments;

import com.google.common.collect.Lists;

public class CommandWithTwoIndexedArguments extends Command
{
	private static final Argument<List<Integer>> NUMBERS = integerArgument().arity(2).required().build();
	private static final Argument<String> FOO = stringArgument("--string").build();

	private final List<Command> executedCommands;

	CommandWithTwoIndexedArguments(final List<Command> executedCommands)
	{
		super(NUMBERS, FOO);
		this.executedCommands = executedCommands;
	}

	CommandWithTwoIndexedArguments()
	{
		this(Lists.<Command>newArrayList());
	}

	@Override
	protected String commandName()
	{
		return "two_args";
	}

	@Override
	protected void execute(ParsedArguments args)
	{
		assertThat(args.get(NUMBERS)).isEqualTo(Arrays.asList(1, 2));
		executedCommands.add(this);
	}
}
