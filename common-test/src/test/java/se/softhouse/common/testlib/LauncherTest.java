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
package se.softhouse.common.testlib;

import static org.fest.assertions.Assertions.assertThat;
import static org.junit.Assert.fail;

import java.io.IOException;

import org.junit.Test;

import se.softhouse.common.testlib.Launcher.LaunchedProgram;

import com.google.common.testing.NullPointerTester;
import com.google.common.testing.NullPointerTester.Visibility;

/**
 * Tests for {@link Launcher}
 */
public class LauncherTest
{
	@Test
	public void testThatOutputAndErrorIsCapturedFromLaunchedProgram() throws IOException, InterruptedException
	{
		LaunchedProgram program = Launcher.launch(HelloWorldProgram.class);
		String debugInformation = program.debugInformation();
		assertThat(program.errors()).as("Error stream must be slurped from parent process." + debugInformation).endsWith("FooBar");
		assertThat(program.output()).as("Standard out must be slurped from parent process." + debugInformation).isEqualTo("HelloWorld");
		assertThat(debugInformation).contains("java").contains("classpath");
		assertThat(program.toString()).contains("Output: HelloWorld");
	}

	private static class HelloWorldProgram
	{
		@SuppressWarnings("unused")
		public static void main(String[] args)
		{
			System.out.print("HelloWorld");
			System.err.print("FooBar");
		}
	}

	@Test
	public void testThatClassWithoutMainIsInvalidated() throws IOException, InterruptedException
	{
		try
		{
			Launcher.launch(InvalidMainClass.class);
			fail("InvalidMainClass should have to have a static main method to be launchable");
		}
		catch(IllegalArgumentException expected)
		{
			assertThat(expected).hasMessage("No main method found on: " + InvalidMainClass.class.getName());
		}
	}

	private static class InvalidMainClass
	{
		// No main(String[] args)
	}

	@Test
	public void testThatClassWithStaticMainButWithWrongParametersIsInvalidated() throws IOException, InterruptedException
	{
		try
		{
			Launcher.launch(WrongArgParametersMainClass.class);
			fail("InvalidModifierMainClass should have to have a static main method to be launchable");
		}
		catch(IllegalArgumentException expected)
		{
			assertThat(expected).hasMessage("No main method found on: " + WrongArgParametersMainClass.class.getName());
		}
	}

	private static class WrongArgParametersMainClass
	{
		@SuppressWarnings("unused")
		public void main(Object[] args) // Should be String[] args
		{

		}
	}

	@Test
	public void testThatClassesWithoutCorrectModifiersOnMainMethodAreInvalidated() throws IOException, InterruptedException
	{
		for(Class<?> clazz : new Class<?>[]{NotStaticMainClass.class, PrivateMainMethod.class})
		{
			try
			{
				Launcher.launch(clazz);
				fail(clazz.getSimpleName() + " should have to have a static & public main method to be launchable");
			}
			catch(IllegalArgumentException expected)
			{
				assertThat(expected).hasMessage(clazz.getName() + "'s main method needs to be static and public for it to be launchable");
			}
		}
	}

	private static class NotStaticMainClass
	{
		@SuppressWarnings("unused")
		public void main(String[] args) // No static modifier
		{

		}
	}

	private static class PrivateMainMethod
	{
		@SuppressWarnings("unused")
		static private void main(String[] args) // No public modifier
		{

		}
	}

	@Test
	public void testThatNullContractsAreFollowed() throws Exception
	{
		new NullPointerTester().testStaticMethods(Launcher.class, Visibility.PACKAGE);
	}
}
