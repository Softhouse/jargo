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

import org.junit.Test;

import com.google.common.testing.NullPointerTester;
import com.google.common.testing.NullPointerTester.Visibility;

public class ResourceLoaderTest
{
	@Test
	public void testResourceLoader()
	{
		assertThat(ResourceLoader.get("/resource-loader-test.txt")).isEqualTo("some text");
	}

	@Test
	public void testResourceLoaderForNonExistingResource()
	{
		String resourceName = "foo.txt";
		try
		{
			ResourceLoader.get("foo.txt");
			fail("foo.txt should not exist");
		}
		catch(IllegalArgumentException expected)
		{
			assertThat(expected).hasMessage(String.format("resource %s relative to %s not found.", resourceName, ResourceLoader.class.getName()));
		}
	}

	@Test
	public void testThatNullContractsAreFollowed() throws Exception
	{
		new NullPointerTester().testStaticMethods(ResourceLoader.class, Visibility.PACKAGE);
	}
}
