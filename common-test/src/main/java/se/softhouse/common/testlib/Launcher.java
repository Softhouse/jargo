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

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.reflect.Modifier.isPublic;
import static java.lang.reflect.Modifier.isStatic;

import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import javax.annotation.concurrent.Immutable;

import com.google.common.base.Charsets;
import com.google.common.collect.Lists;

/**
 * Can launch java programs for a {@link Class} with a main method. Output is captured in the
 * returned {@link LaunchedProgram}.
 */
@Immutable
public final class Launcher
{
	/**
	 * Result from a {@link Launcher#launch(Class, String...) launched program}
	 */
	@Immutable
	public static final class LaunchedProgram
	{
		private final String errors;
		private final String output;
		private final String debugInformation;

		private LaunchedProgram(String errors, String output, String debugInformation)
		{
			this.errors = errors;
			this.output = output;
			this.debugInformation = debugInformation;
		}

		/**
		 * The {@link System#out} in {@link Charsets#UTF_8 UTF-8} from the launched program
		 */
		public String output()
		{
			return output;
		}

		/**
		 * The {@link System#err} in {@link Charsets#UTF_8 UTF-8} from the launched program
		 */
		public String errors()
		{
			return errors;
		}

		/**
		 * Returns information suitable to print in case of errors on a debug level
		 */
		public String debugInformation()
		{
			return debugInformation;
		}

		@Override
		public String toString()
		{
			return "Output: " + output() + "\nErrors: " + errors();
		}
	}

	private Launcher()
	{
	}

	/**
	 * Runs {@code classWithMainMethod} in a separate process using the system property
	 * {@code java.home} to find java and {@code java.class.path} for the class path. This method
	 * will wait until program execution has finished.
	 * 
	 * @param classWithMainMethod a class with a static "main" method
	 * @param programArguments optional arguments to pass to the program
	 * @return output/errors from the executed program
	 * @throws IOException if an I/O error occurs while starting {@code classWithMainMethod} as a
	 *             process
	 * @throws InterruptedException if the thread starting the program is
	 *             {@link Thread#interrupted()} while waiting for the program to finish
	 * @throws IllegalArgumentException if {@code classWithMainMethod} doesn't have a public static
	 *             main method
	 * @throws SecurityException if it's not possible to validate the existence of a main method in
	 *             {@code classWithMainMethod} (or if {@link SecurityManager#checkExec checkExec}
	 *             fails)
	 */
	public static LaunchedProgram launch(Class<?> classWithMainMethod, String ... programArguments) throws IOException, InterruptedException
	{
		checkNotNull(programArguments);
		try
		{

			int modifiers = classWithMainMethod.getDeclaredMethod("main", String[].class).getModifiers();
			boolean validModifiers = isStatic(modifiers) && isPublic(modifiers);
			checkArgument(validModifiers, "%s's main method needs to be static and public for it to be launchable", classWithMainMethod.getName());
		}
		catch(NoSuchMethodException e)
		{
			throw new IllegalArgumentException("No main method found on: " + classWithMainMethod.getName(), e);
		}
		RuntimeMXBean runtimeInformation = ManagementFactory.getRuntimeMXBean();
		String jvm = new File(new File(System.getProperty("java.home"), "bin"), "java").getAbsolutePath();
		String classPath = runtimeInformation.getClassPath();

		List<String> vmArgs = runtimeInformation.getInputArguments();

		List<String> args = Lists.newArrayList(jvm, "-cp", classPath);
		args.addAll(vmArgs);
		args.add(classWithMainMethod.getName());
		args.addAll(Arrays.asList(programArguments));

		String debugInformation = "\njvm: " + jvm + "\nvm args: " + vmArgs + "\nclasspath: " + classPath;
		return execute(args, debugInformation);
	}

	/**
	 * Runs {@code program} with {@code programArguments} as arguments. This method
	 * will wait until program execution has finished.
	 * 
	 * @param program the executable to run
	 * @param programArguments optional arguments to pass to the program
	 * @return output/errors from the executed program
	 * @throws IOException if an I/O error occurs while starting {@code program}
	 * @throws InterruptedException if the thread starting the program is
	 *             {@link Thread#interrupted()} while waiting for the program to finish
	 * @throws SecurityException if {@link SecurityManager#checkExec checkExec} fails
	 */
	public static LaunchedProgram launch(String program, String ... programArguments) throws IOException, InterruptedException
	{
		return execute(Lists.asList(program, programArguments), "Failed to launch " + program);
	}

	private static LaunchedProgram execute(List<String> args, String debugInformation) throws IOException, InterruptedException
	{
		Process program = new ProcessBuilder().command(args).start();
		Future<String> stdout = Streams.readAsynchronously(program.getInputStream());
		Future<String> stderr = Streams.readAsynchronously(program.getErrorStream());
		program.waitFor();
		try
		{
			String output = stdout.get();
			String errors = stderr.get();
			return new LaunchedProgram(errors, output, debugInformation);
		}
		catch(ExecutionException e)
		{
			if(e.getCause() instanceof IOException)
				throw (IOException) e.getCause();
			throw new RuntimeException("Failed to read stdout/stderr", e);
		}
	}
}
