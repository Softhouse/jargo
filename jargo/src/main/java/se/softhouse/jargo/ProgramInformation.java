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
package se.softhouse.jargo;

import se.softhouse.common.classes.Classes;
import se.softhouse.common.strings.Describable;
import se.softhouse.common.strings.Describables;

import javax.annotation.CheckReturnValue;
import javax.annotation.concurrent.Immutable;

import static java.util.Objects.requireNonNull;
import static se.softhouse.common.strings.Describables.cache;
import static se.softhouse.common.strings.StringsUtil.NEWLINE;

/**
 * Information about a program, printed in {@link Usage} before any {@link Argument}s are described.
 */
@Immutable
final class ProgramInformation
{
	private final Describable programName;
	private final String programDescription;

	private ProgramInformation(Describable programName, String programDescription)
	{
		this.programName = programName;
		this.programDescription = programDescription;
	}

	/**
	 * <pre>
	 * Automatically tries to get the simple name of the main class by looking at stack traces.
	 * <b>Note:</b>If the main thread has died this method returns "ProgramName".
	 * <b>Note:</b>If there's a restrictive {@link SecurityManager} in place you need to set the
	 * "ProgramName" with {@link CommandLineParser#programName(String)}
	 * </pre>
	 */
	static final ProgramInformation AUTO = new ProgramInformation(cache(new Describable(){
		@Override
		public String description()
		{
			return Classes.mainClassName();
		}
	}), "");

	/**
	 * Creates a {@link ProgramInformation} descriptor object for a program with the name
	 * {@code programName}. {@code programName} is used by {@link CommandLineParser#usage()} to tell
	 * the user which program the usage applies to. By default, {@link CommandLineParser#usage()}
	 * uses {@link Classes#mainClassName()} to figure out this name.
	 * 
	 * @param programName is usually "ProgramName" in "java ProgramName --argument parameter".
	 */
	@CheckReturnValue
	static ProgramInformation withProgramName(String programName)
	{
		return new ProgramInformation(Describables.withString(programName), "");
	}

	/**
	 * Sets the optional {@code programDescription} that's used to describe what the program as a
	 * whole does.
	 * 
	 * @param aProgramDescription the description to print at the top of the usage texts, just below
	 *            the {@link #programName(String)}.
	 * @return a newly created {@link ProgramInformation}
	 */
	@CheckReturnValue
	ProgramInformation programDescription(String aProgramDescription)
	{
		requireNonNull(aProgramDescription);
		return new ProgramInformation(programName, NEWLINE + aProgramDescription + NEWLINE);
	}

	/**
	 * Resets {@link #withProgramName(String)} to {@code aProgramName} keeping any
	 * {@link #programDescription(String)} previously set
	 * 
	 * @return a newly created {@link ProgramInformation}
	 */
	@CheckReturnValue
	ProgramInformation programName(String aProgramName)
	{
		return new ProgramInformation(Describables.withString(aProgramName), programDescription);
	}

	String programName()
	{
		return programName.description();
	}

	String programDescription()
	{
		return programDescription;
	}

	@Override
	public String toString()
	{
		return programName() + ":" + programDescription();
	}
}
