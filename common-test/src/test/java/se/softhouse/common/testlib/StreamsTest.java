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
import static se.softhouse.common.testlib.Constants.UTF_8_CHAR;

import java.io.ByteArrayInputStream;

import org.junit.Test;

import com.google.common.base.Charsets;
import com.google.common.base.Strings;
import com.google.common.testing.NullPointerTester;
import com.google.common.testing.NullPointerTester.Visibility;

/**
 * Tests for {@link Streams}
 */
public class StreamsTest
{
	@Test
	public void testThatStreamIsFullyRead() throws Exception
	{
		String largeUtf8String = Strings.repeat("hello" + UTF_8_CHAR, 1000);
		ByteArrayInputStream stream = new ByteArrayInputStream(largeUtf8String.getBytes(Charsets.UTF_8));
		String result = Streams.readAsynchronously(stream).get();
		assertThat(result).isEqualTo(largeUtf8String);
	}

	@Test
	public void testThatNullContractsAreFollowed() throws Exception
	{
		new NullPointerTester().testStaticMethods(Streams.class, Visibility.PACKAGE);
	}
}
