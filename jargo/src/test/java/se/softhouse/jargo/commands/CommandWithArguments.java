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

import com.google.common.collect.Lists;

import se.softhouse.jargo.Argument;
import se.softhouse.jargo.Command;
import se.softhouse.jargo.ParsedArguments;

public class CommandWithArguments extends Command
{
	private static final Argument<Character> CHAR = charArgument().required().build();

	public static boolean wasExecuted;

	public CommandWithArguments()
	{
		super(Lists.newArrayList(CHAR));
	}

	@Override
	protected String commandName()
	{
		return "main";
	}

	@Override
	public String description()
	{
		return "A command that passes a list with arguments";
	}

	@Override
	protected void execute(ParsedArguments args)
	{
		assertThat(args.get(CHAR)).isEqualTo('c');
		wasExecuted = true;
	}
}
