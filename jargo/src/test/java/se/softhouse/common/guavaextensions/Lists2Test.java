/*
 * Copyright 2018 jonatan.jonsson
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
package se.softhouse.common.guavaextensions;

import static org.fest.assertions.Assertions.assertThat;

import org.junit.Test;

import com.google.common.base.Splitter;
import com.google.common.testing.NullPointerTester;
import com.google.common.testing.NullPointerTester.Visibility;

/**
 * Tests for {@link Lists2}
 */
public class Lists2Test
{
	@Test
	public void testThatListFunctionsWorkForABasicIterable() throws Exception
	{
		Iterable<String> iter = Splitter.on(',').split("hi,bye");
		assertThat(Lists2.newArrayList(iter)).containsExactly("hi", "bye");
		assertThat(Lists2.size(iter)).isEqualTo(2);
		assertThat(Lists2.isEmpty(iter)).isFalse();
		assertThat(Lists2.isEmpty(Splitter.on(',').omitEmptyStrings().split(""))).isTrue();
	}

	@Test
	public void testThatNullContractsAreFollowed() throws Exception
	{
		new NullPointerTester().testStaticMethods(Lists2.class, Visibility.PACKAGE);
	}
}
