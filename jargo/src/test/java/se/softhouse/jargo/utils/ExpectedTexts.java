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
package se.softhouse.jargo.utils;

import se.softhouse.common.classes.Classes;
import se.softhouse.common.testlib.ResourceLoader;
import se.softhouse.jargo.Usage;

/**
 * Helps to load saved {@link Usage} texts
 */
public final class ExpectedTexts
{
	private ExpectedTexts()
	{
	}

	/**
	 * Loads a usage text (from resources) named /usage_texts/{@code testName}.txt
	 * 
	 * @return the file as a string
	 */
	public static String expected(String testName)
	{

		String expectedUsage = ResourceLoader.get("/jargo/usage_texts/" + testName + ".txt");

		// Avoids having RemoteTestRunner or ForkedBooter in the .txt files. As the main class is
		// fetched by ProgramInformation.AUTO by default this avoids test runner specific code
		expectedUsage = expectedUsage.replaceFirst(testName, Classes.mainClassName());
		return expectedUsage;
	}
}
