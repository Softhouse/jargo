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

import static se.softhouse.jargo.Arguments.integerArgument;
import se.softhouse.jargo.Command;
import se.softhouse.jargo.ParsedArguments;

final class InvalidCommand extends Command
{
	public InvalidCommand()
	{
		// Two arguments with the same name ("-n" here) should trigger an exception when building a
		// command line parser, but as that's only done when the command is encountered it shouldn't
		// trigger here
		super(integerArgument("-n").build(), integerArgument("-n").build());
	}

	@Override
	public String commandName()
	{
		return "profile";
	}

	@Override
	protected void execute(ParsedArguments parsedArguments)
	{
	}
}
