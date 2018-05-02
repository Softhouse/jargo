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
import se.softhouse.jargo.Argument;
import se.softhouse.jargo.Command;
import se.softhouse.jargo.ParsedArguments;
import se.softhouse.jargo.commands.Commit.Repository;

public class Log extends Command
{
	public final Repository repository;

	private static final Argument<Integer> LIMIT = integerArgument("--limit", "-l").build();

	public Log(final Repository repo)
	{
		super(LIMIT);
		repository = repo;
	}

	@Override
	protected void execute(ParsedArguments parsedArguments)
	{
		repository.logLimit = parsedArguments.get(LIMIT);
	}
}
