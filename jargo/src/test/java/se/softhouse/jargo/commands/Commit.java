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

import static se.softhouse.jargo.Arguments.fileArgument;
import static se.softhouse.jargo.Arguments.optionArgument;
import static se.softhouse.jargo.Arguments.stringArgument;

import java.io.File;
import java.util.List;

import se.softhouse.jargo.Argument;
import se.softhouse.jargo.Command;
import se.softhouse.jargo.ParsedArguments;

import com.google.common.collect.Lists;

public class Commit extends Command
{
	public final Repository repository;

	private static final Argument<Boolean> AMEND = optionArgument("--amend").build();
	static final Argument<String> AUTHOR = stringArgument("--author").required().separator("=").build();
	private static final Argument<List<File>> FILES = fileArgument().variableArity().build();

	public Commit(final Repository repo)
	{
		super(AMEND, AUTHOR, FILES);
		repository = repo;
	}

	@Override
	protected void execute(final ParsedArguments parsedArguments)
	{
		repository.commits.add(new Revision(parsedArguments));
	}

	@Override
	public String description()
	{
		return "Commits to a repository";
	}

	public static class Repository
	{
		List<Revision> commits = Lists.newArrayList();

		int logLimit = 10;
	}

	public static class Revision
	{
		final List<File> files;
		final boolean amend;
		final String author;

		public Revision(final ParsedArguments arguments)
		{
			amend = arguments.get(AMEND);
			files = arguments.get(FILES);
			author = arguments.get(AUTHOR);
		}
	}
}
