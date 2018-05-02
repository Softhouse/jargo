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
package se.softhouse.jargo.stringparsers;

import static org.fest.assertions.Assertions.assertThat;
import static se.softhouse.jargo.Arguments.fileArgument;
import static se.softhouse.jargo.utils.Assertions2.assertThat;

import java.io.File;

import org.junit.Test;

import se.softhouse.jargo.ArgumentException;
import se.softhouse.jargo.Arguments;
import se.softhouse.jargo.StringParsers;
import se.softhouse.jargo.Usage;

/**
 * Tests for {@link Arguments#fileArgument(String...)} and {@link StringParsers#fileParser()}
 */
public class FileArgumentTest
{
	@Test
	public void testDescription()
	{
		Usage usage = fileArgument("-f").usage();
		assertThat(usage).contains("<path>: a file path");
	}

	@Test
	public void testThatFilesAreDescribedByAbsolutePath()
	{
		Usage usage = fileArgument("-f").usage();
		assertThat(usage).contains("Default: " + new File("").getAbsolutePath());
	}

	@Test
	public void testThatFileArgumentsDefaultsToCurrentDirectory() throws ArgumentException
	{
		File defaultFile = fileArgument("-f").parse();
		assertThat(defaultFile).exists().isDirectory().isEqualTo(new File("."));

	}
}
