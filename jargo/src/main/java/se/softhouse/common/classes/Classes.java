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
package se.softhouse.common.classes;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import javax.annotation.concurrent.Immutable;

import se.softhouse.common.guavaextensions.Suppliers2;

import java.util.function.Supplier;

/**
 * Utilities for working with {@link Class} instances
 */
@Immutable
public final class Classes
{
	private Classes()
	{
	}

	/**
	 * Tries to find the class of the main method by looking at {@link Thread#getAllStackTraces()}.
	 * In simpler terms, a best effort attempt at figuring out the name of the invoked program.
	 * 
	 * @return the simple name of the class containing the "main" method
	 * @throws IllegalStateException when no main method could be found
	 */
	@CheckReturnValue
	@Nonnull
	public static String mainClassName()
	{
		return MAIN_CLASS_NAME.get();
	}

	private static final Supplier<String> MAIN_CLASS_NAME = Suppliers2.memoize(() -> {
		Iterable<StackTraceElement[]> stacks = Thread.getAllStackTraces().values();
		for(StackTraceElement[] currentStack : stacks)
		{
			if(currentStack.length == 0)
			{
				continue;
			}
			StackTraceElement startMethod = currentStack[currentStack.length - 1];
			if(startMethod.getMethodName().equals("main"))
				return classNameFor(startMethod);
		}
		throw new IllegalStateException("No main method found in the stack traces, could it be that the main thread has been terminated?");
	});

	private static String classNameFor(StackTraceElement element)
	{
		String fullyQualifiedClassName = element.getClassName();
		int classNameStart = fullyQualifiedClassName.lastIndexOf('.') + 1;
		String simpleClassName = fullyQualifiedClassName.substring(classNameStart);
		return simpleClassName;
	}
}
