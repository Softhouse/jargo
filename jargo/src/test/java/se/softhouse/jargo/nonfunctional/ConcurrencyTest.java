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
package se.softhouse.jargo.nonfunctional;

import static org.fest.assertions.Assertions.assertThat;
import static se.softhouse.jargo.Arguments.stringArgument;
import static se.softhouse.jargo.utils.Assertions2.assertThat;

import java.util.concurrent.TimeUnit;

import org.junit.Test;

import se.softhouse.common.testlib.ConcurrencyTester;
import se.softhouse.common.testlib.ConcurrencyTester.RunnableFactory;
import se.softhouse.jargo.Argument;
import se.softhouse.jargo.CommandLineParser;
import se.softhouse.jargo.ParsedArguments;
import se.softhouse.jargo.utils.ExpectedTexts;

/**
 * Stress tests that verifies that a {@link CommandLineParser} can be used from several
 * {@link Thread}s concurrently
 */
public class ConcurrencyTest extends ExhaustiveProgram
{
	final String expectedUsageText = ExpectedTexts.expected("allFeaturesInUsage");

	private static final int timeoutInSeconds = 60;
	private static final int cleanupTime = 5;

	@Test
	public void testThatUsageCanBeCalledConcurrently() throws Throwable
	{
		ConcurrencyTester.verify(new RunnableFactory(){
			@Override
			public int iterationCount()
			{
				return 20;
			}

			@Override
			public Runnable create(int uniqueNumber)
			{
				return new Runnable(){
					@Override
					public void run()
					{
						// TODO(jontejj): share Usage instance once it's thread safe
						assertThat(parser.usage()).isEqualTo(expectedUsageText);
					}
				};
			}
		}, timeoutInSeconds, TimeUnit.SECONDS);
	}

	@Test(timeout = (timeoutInSeconds + cleanupTime) * 1000)
	public void testThatDifferentArgumentsCanBeParsedConcurrently() throws Throwable
	{
		ConcurrencyTester.verify(new RunnableFactory(){
			@Override
			public int iterationCount()
			{
				return 300;
			}

			@Override
			public Runnable create(int uniqueNumber)
			{
				return new ArgumentParseRunner(uniqueNumber);
			}
		}, timeoutInSeconds, TimeUnit.SECONDS);
	}

	@Test(timeout = (timeoutInSeconds + cleanupTime) * 1000)
	public void testThatEndOfOptionsIsNotSharedBetweenParsers() throws Throwable
	{
		final Argument<String> option = stringArgument("--option").build();
		final Argument<String> indexed = stringArgument().build();
		final CommandLineParser cli = CommandLineParser.withArguments(option, indexed);
		ConcurrencyTester.verify(new RunnableFactory(){
			@Override
			public int iterationCount()
			{
				return 300;
			}

			@Override
			public Runnable create(final int uniqueNumber)
			{
				return new Runnable(){
					@Override
					public void run()
					{
						ParsedArguments result = cli.parse("--option", "one" + uniqueNumber, "--", "indexed" + uniqueNumber);
						assertThat(result.get(option)).isEqualTo("one" + uniqueNumber);
						assertThat(result.get(indexed)).isEqualTo("indexed" + uniqueNumber);
					}
				};
			}
		}, timeoutInSeconds, TimeUnit.SECONDS);
	}
}
