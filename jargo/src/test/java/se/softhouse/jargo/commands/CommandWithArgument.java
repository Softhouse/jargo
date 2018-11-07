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

import se.softhouse.jargo.Argument;
import se.softhouse.jargo.Command;
import se.softhouse.jargo.ParsedArguments;

public class CommandWithArgument<T> extends Command
{
	private final Argument<T> arg;

	public T parsedObject;

	private final String name;

	public CommandWithArgument(String name, Argument<T> arg)
	{
		super(arg);
		this.name = name;
		this.arg = arg;
	}

	@Override
	protected String commandName()
	{
		return name;
	}

	@Override
	public String description()
	{
		return "A command that parses a single argument";
	}

	@Override
	protected void execute(ParsedArguments args)
	{
		parsedObject = args.get(arg);
	}
}
