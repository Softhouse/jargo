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

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableSet;
import com.google.common.reflect.ClassPath;
import com.google.common.reflect.ClassPath.ClassInfo;

import javax.annotation.concurrent.Immutable;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;

import static org.fest.assertions.Assertions.*;
import static se.softhouse.common.testlib.ReflectionUtil.hasInstanceFields;
import static se.softhouse.common.testlib.ReflectionUtil.isStatic;

/**
 * The reasoning behind testing code that doesn't do anything is to achieve 100%
 * code coverage and to notice when the code
 * coverage drops. Otherwise one could always think, hey I don't have 100% code
 * coverage anymore but it's PROBABLY because of my private constructors
 * (or any other code that's not used but preferred to have).
 * This makes it easy to spot untested methods without having to check that it
 * only was a private constructor etc.
 */
@Immutable
public final class UtilityClassTester
{
	private UtilityClassTester()
	{
	}

	/**
	 * Detects if a utility class isn't final or if its no-args constructor
	 * isn't private. Also calls the constructor to get code coverage for it.
	 * The constructor is allowed to throw exceptions.
	 */
	public static void testUtilityClassDesign(Class<?> ... utilityClasses)
	{
		testUtilityClassDesign(Arrays.asList(utilityClasses));
	}

	/**
	 * {@link Iterable} version of {@link #testUtilityClassDesign(Class...)}.
	 */
	public static void testUtilityClassDesign(Iterable<Class<?>> utilityClasses)
	{
		for(Class<?> clazz : utilityClasses)
		{
			try
			{
				Constructor<?> constructor = clazz.getDeclaredConstructor();
				assertThat(constructor.getModifiers() & Modifier.PRIVATE).as("Constructor for " + clazz + " should be private.")
						.isEqualTo(Modifier.PRIVATE);
				constructor.setAccessible(true);
				assertThat(constructor.newInstance()).isNotNull();
				assertThat(clazz.getModifiers() & Modifier.FINAL).as("Utility class " + clazz + " not final").isEqualTo(Modifier.FINAL);
			}
			catch(NoSuchMethodException e)
			{
				throw new IllegalArgumentException("No no-arg constructor found for " + clazz, e);
			}
			catch(InvocationTargetException e)
			{
				// Some utility classes like to throw from their private constructor just to be on
				// the safe side, let's accept that
			}
			catch(InstantiationException e)
			{
				throw new IllegalArgumentException("Utility " + clazz + " may not be abstract", e);
			}
			catch(IllegalAccessException e)
			{
				// Let's just say that if we're not able to create an instance it's fine, after all
				// that's what we're testing here
			}
		}
	}

	/**
	 * Goes through all {@link ClassLoader#loadClass(String) loadable} classes around {@code klazz}
	 * (and sub packages) and makes sure that the class is marked as final and that its constructor
	 * is private if all publicly available methods are static (by using
	 * {@link #testUtilityClassDesign(Iterable)}).<br>
	 * <b>Note:</b> Nested classes are not tested (yet)
	 * 
	 * @throws IOException if the attempt to read class path resources (jar files or directories)
	 *             failed.
	 * @throws IllegalArgumentException if no utility classes were found
	 */
	public static void testUtilityClassDesignForAllClassesAround(Class<?> klazz) throws IOException
	{
		String packageName = klazz.getPackage().getName();
		ImmutableSet<ClassInfo> classes = ClassPath.from(klazz.getClassLoader()).getTopLevelClassesRecursive(packageName);
		Iterable<Class<?>> utilityClasses = FluentIterable.from(classes).transform(loadClasses()).filter(lookingLikeAUtilityClass()).toList();

		assertThat(utilityClasses).as("No utitlity classes exists in " + packageName).isNotEmpty();
		testUtilityClassDesign(utilityClasses);
	}

	private static Predicate<Class<?>> lookingLikeAUtilityClass()
	{
		return input -> {
			for(Method method : input.getDeclaredMethods())
			{
				if(method.isSynthetic())// Don't count injected code
				{
					continue;
				}
				if(!isStatic(method))
					return false;
			}
			if(input.isInterface())
				return false;
			if(hasInstanceFields(input))
				return false;
			return true;
		};
	}

	private static Function<ClassInfo, Class<?>> loadClasses()
	{
		return ClassInfo::load;
	}
}
