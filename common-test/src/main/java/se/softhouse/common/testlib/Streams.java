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

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

import javax.annotation.concurrent.Immutable;

import com.google.common.base.Charsets;
import com.google.common.io.CharStreams;
import com.google.common.util.concurrent.ListenableFuture;

/**
 * Utility for working with {@link Readable} and {@link InputStream stream} instances.
 */
@Immutable
public final class Streams
{
	private Streams()
	{
	}

	/**
	 * Reads from {@code source} asynchronously. Use {@link Future#get()} to get the
	 * characters read. Characters will be read using {@link Charsets#UTF_8 UTF-8}. A typical use
	 * case is to avoid <a
	 * href="http://www.javaworld.com/javaworld/jw-12-2000/jw-1229-traps.html?page=4">deadlock
	 * pitfalls</a> when launching {@link Process processes}.
	 */
	public static ListenableFuture<String> readAsynchronously(InputStream source)
	{
		final Readable sourceInUTF8 = new InputStreamReader(source, Charsets.UTF_8);
		Callable<String> reader = new Callable<String>(){
			@Override
			public String call() throws Exception
			{
				StringBuilder result = new StringBuilder();
				CharStreams.copy(sourceInUTF8, result);
				return result.toString();
			}
		};
		return Concurrently.run(reader);
	}
}
