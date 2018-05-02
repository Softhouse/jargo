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
import se.softhouse.jargo.Command;
import se.softhouse.jargo.ParsedArguments;

public class Build extends Command
{
	final BuildTarget target;

	public Build(BuildTarget target)
	{
		this.target = target;
	}

	public Build()
	{
		this.target = new BuildTarget();
	}

	@Override
	public String description()
	{
		return "Builds a target";
	}

	@Override
	protected void execute(ParsedArguments parsedArguments)
	{
		target.build();
	}

	public static class BuildTarget
	{
		private boolean cleaned;
		private boolean built;

		void build()
		{
			// The clean command should be run before the build command, this verifies that commands
			// are executed in the order they are given on the command line
			assertThat(isClean()).as("Target should be clean before being built, hasn't the clean command been executed?").isTrue();
			built = true;
		}

		void clean()
		{
			cleaned = true;
		}

		boolean isClean()
		{
			return cleaned;
		}

		boolean isBuilt()
		{
			return built;
		}

		void reset()
		{
			cleaned = false;
			built = false;
		}
	}
}
