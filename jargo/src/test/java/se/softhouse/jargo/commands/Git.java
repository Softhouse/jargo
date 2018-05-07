/* Copyright 2018 jonatanjonsson
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */
package se.softhouse.jargo.commands;

import java.util.ArrayList;
import java.util.List;

import se.softhouse.jargo.Argument;
import se.softhouse.jargo.Arguments;
import se.softhouse.jargo.Command;
import se.softhouse.jargo.ParsedArguments;
import se.softhouse.jargo.commands.Commit.Repository;

public class Git extends Command
{
	static final Argument<String> MESSAGE = Arguments.stringArgument("--message", "-m").build();

	public Git(Repository repo)
	{
		super(argumentsAndCommands(repo));
	}

	private static List<Argument<?>> argumentsAndCommands(Repository repo)
	{
		List<Argument<?>> args = new ArrayList<>();
		args.addAll(subCommands(new Commit(repo), new Log(repo), new Merge(repo)));
		return args;
	}

	@Override
	protected void execute(ParsedArguments parsedArguments)
	{

	}

	@Override
	protected String commandName()
	{
		return "git";
	}
}
