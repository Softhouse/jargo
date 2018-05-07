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

import se.softhouse.jargo.ParsedArguments;

public class Merge extends Commit
{
	public Merge(final Repository repo)
	{
		super(repo);
	}

	@Override
	protected void execute(final ParsedArguments parsedArguments)
	{
		repository.commits.add(new Revision(parsedArguments.get(Git.MESSAGE), parsedArguments));
	}

	@Override
	public String description()
	{
		return "Merges to a repository";
	}
}
