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
import static se.softhouse.jargo.Arguments.charArgument;

import java.util.List;

import se.softhouse.jargo.Argument;
import se.softhouse.jargo.Arguments;
import se.softhouse.jargo.Command;
import se.softhouse.jargo.ParsedArguments;

public class ProfilingSubcommand extends Command
{
	private static final Argument<Character> CHAR = charArgument().required().build();
	final List<Command> executedCommands;
	static Command subCommand;

	public ProfilingSubcommand(final List<Command> executedCommands)
	{
		super(initSubcommand(executedCommands), CHAR);
		this.executedCommands = executedCommands;

	}

	static Argument<?> initSubcommand(final List<Command> executedCommandList)
	{
		subCommand = new CommandWithOneIndexedArgument(executedCommandList);
		return Arguments.command(subCommand).build();
	}

	@Override
	protected String commandName()
	{
		return "main";
	}

	@Override
	public String description()
	{
		return "A command with an argument and a subcommand that takes an argument as well";
	}

	@Override
	protected void execute(ParsedArguments args)
	{
		assertThat(args.get(CHAR)).isEqualTo('c');
		executedCommands.add(this);
	}
}
