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
package se.softhouse.jargo.exceptions;

import static org.fest.assertions.Assertions.assertThat;
import static org.junit.Assert.fail;
import static se.softhouse.jargo.Arguments.command;
import static se.softhouse.jargo.Arguments.integerArgument;

import java.io.IOException;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.junit.Test;

import se.softhouse.jargo.Argument;
import se.softhouse.jargo.ArgumentBuilder.DefaultArgumentBuilder;
import se.softhouse.jargo.ArgumentException;
import se.softhouse.jargo.CommandLineParser;
import se.softhouse.jargo.ParsedArguments;
import se.softhouse.jargo.commands.ProfilingExecuteCommand;

import com.google.common.collect.ImmutableSet;
import com.google.common.reflect.ClassPath;
import com.google.common.reflect.ClassPath.ClassInfo;
import com.google.common.testing.NullPointerTester;
import com.google.common.testing.NullPointerTester.Visibility;

/**
 * Tests for {@link Nonnull} and {@link Nullable} arguments
 */
public class NullPointerTest
{
	@Test
	public void testThatNullContractsAreCheckedEagerly() throws ArgumentException, IOException
	{
		String packageName = Argument.class.getPackage().getName();
		ImmutableSet<ClassInfo> classes = ClassPath.from(getClass().getClassLoader()).getTopLevelClasses(packageName);
		NullPointerTester npeTester = new NullPointerTester();
		for(ClassInfo klazz : classes)
		{
			npeTester.testStaticMethods(klazz.load(), Visibility.PACKAGE);
		}

		DefaultArgumentBuilder<Integer> builder = integerArgument("--name");
		npeTester.testInstanceMethods(builder, Visibility.PROTECTED);

		Argument<Integer> argument = builder.build();

		npeTester.testInstanceMethods(argument, Visibility.PROTECTED);

		CommandLineParser parser = CommandLineParser.withArguments(argument);
		npeTester.testInstanceMethods(parser, Visibility.PROTECTED);

		ParsedArguments result = parser.parse();
		npeTester.testInstanceMethods(result, Visibility.PROTECTED);

		try
		{
			parser.parse("--a");
			fail("--a should have been unhandled");
		}
		catch(ArgumentException expected)
		{
			npeTester.testAllPublicInstanceMethods(expected);
		}
		npeTester.testInstanceMethods(parser.usage(), Visibility.PACKAGE);
	}

	@Test
	public void testThatArgumentsAreCheckedForNullBeforeFirstParseTakesPlace() throws Exception
	{
		ProfilingExecuteCommand profiler = new ProfilingExecuteCommand();
		try
		{
			CommandLineParser.withArguments(command(profiler).repeated().build()).parse("profile", "profile", null);
			fail("null arguments should not be allowed");
		}
		catch(NullPointerException expected)
		{
			assertThat(profiler.numberOfCallsToExecute).as("null wasn't checked before commands were executed").isZero();
			assertThat(expected).hasMessage("Argument strings may not be null (discovered one at index 2)");
		}
	}
}
