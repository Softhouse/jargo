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

import org.junit.Test;

import com.google.common.collect.ImmutableList;

import se.softhouse.common.testlib.Launcher;
import se.softhouse.common.testlib.Launcher.LaunchedProgram;

/**
 * Tests that argument parser works with the default {@link SecurityManager} activated.
 */
public final class SecurityTest extends ExhaustiveProgram
{
	public static void main(String[] args) throws Throwable
	{
		new SecurityTest().run();
	}

	private void run()
	{
		ArgumentParseRunner runner = new ArgumentParseRunner(1);
		runner.run();
	}

	@Test
	public void testThatProgramCanBeRunWithSecuritManagerActivated() throws Exception
	{
		ImmutableList<String> secureVmArgs = ImmutableList.of(	"-Djava.security.manager",
																"-Djava.security.policy=src/test/resources/jargo/security.policy");
		LaunchedProgram launchedProgram = Launcher.launch(secureVmArgs, SecurityTest.class, "");
		assertThat(launchedProgram.errors()).isEmpty();
		assertThat(launchedProgram.output()).isEmpty();
	}
}
