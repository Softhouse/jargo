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
import static se.softhouse.jargo.Arguments.booleanArgument;
import static se.softhouse.jargo.Arguments.command;
import static se.softhouse.jargo.Arguments.integerArgument;

import java.util.List;

import se.softhouse.jargo.Argument;
import se.softhouse.jargo.Command;
import se.softhouse.jargo.ParsedArguments;

import com.google.common.collect.Lists;

public class CommandWithOneIndexedArgument extends Command
{
	private static final Argument<Integer> NUMBER = integerArgument().required().build();
	private static final Argument<Boolean> FOO = booleanArgument("--bool").build();
	private static final Argument<?> CMD = command(new Command(){

		@Override
		protected void execute(ParsedArguments parsedArguments)
		{
		}

		@Override
		protected String commandName()
		{
			return "cmd";
		}
	}).build();

	private final List<Command> executedCommands;

	CommandWithOneIndexedArgument(final List<Command> executedCommands)
	{
		super(NUMBER, FOO, CMD);
		this.executedCommands = executedCommands;
	}

	public CommandWithOneIndexedArgument()
	{
		this(Lists.<Command>newArrayList());
	}

	@Override
	protected String commandName()
	{
		return "one_arg";
	}

	@Override
	protected void execute(ParsedArguments args)
	{
		assertThat(args.get(NUMBER)).isEqualTo(1);
		executedCommands.add(this);
	}
}
