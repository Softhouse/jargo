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

import java.util.Arrays;
import java.util.List;

import se.softhouse.jargo.Argument;
import se.softhouse.jargo.Command;
import se.softhouse.jargo.ParsedArguments;

public class CommandWithThreeIndexedArguments extends Command
{
	private static final Argument<List<Integer>> NUMBERS = integerArgument().arity(3).required().build();

	private final List<Command> executedCommands;

	CommandWithThreeIndexedArguments(final List<Command> executedCommands)
	{
		super(NUMBERS);
		this.executedCommands = executedCommands;
	}

	@Override
	protected String commandName()
	{
		return "three_args";
	}

	@Override
	protected void execute(ParsedArguments args)
	{
		assertThat(args.get(NUMBERS)).isEqualTo(Arrays.asList(1, 2, 3));
		executedCommands.add(this);
	}
}
