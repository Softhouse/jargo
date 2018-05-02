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
package se.softhouse.jargo.argumentbuilder;

import static org.fest.assertions.Assertions.assertThat;

import org.junit.Test;

import se.softhouse.jargo.ArgumentBuilder;
import se.softhouse.jargo.ArgumentException;

/**
 * Tests for subclassing {@link ArgumentBuilder}
 */
public class CustomArgumentBuilderTest
{
	/**
	 * The idea here is that all foos created from parsed arguments should use bar=5
	 */
	@Test
	public void testThatBarIsSettableOnFooBuilder() throws ArgumentException
	{
		// Note that .bar(5) can be called after description which is a method in ArgumentBuilder
		Foo foo = new FooBuilder().description("bar should even be callable after calls to ArgumentBuilder defined methods").bar(5).parse("foo");

		assertThat(foo.bar).isEqualTo(5);
	}
}
