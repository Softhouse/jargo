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

import static com.google.common.base.Charsets.UTF_8;

import java.io.IOException;
import java.net.URL;

import javax.annotation.concurrent.Immutable;

import com.google.common.io.Resources;

/**
 * Aids tests with loading resource texts. These texts are often the expected result from some
 * operation.
 */
@Immutable
public final class ResourceLoader
{
	private ResourceLoader()
	{
	}

	/**
	 * Loads a file and reads it as an {@link com.google.common.base.Charsets#UTF_8 UTF_8} file.
	 * {@code resourceName} must start with a "/"
	 * 
	 * @return the file as a string
	 * @throws IllegalArgumentException if {@code resourceName} can't be found/loaded
	 */
	public static String get(String resourceName)
	{
		URL resourcePath = Resources.getResource(ResourceLoader.class, resourceName);
		String fileContent = null;
		try
		{
			fileContent = Resources.toString(resourcePath, UTF_8);
		}
		catch(IOException e)
		{
			throw new IllegalArgumentException("Failed to load: " + resourcePath, e);
		}
		return fileContent;
	}
}
