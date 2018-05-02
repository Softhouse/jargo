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
package se.softhouse.common.guavaextensions;

import static com.google.common.base.Charsets.UTF_8;
import static org.fest.assertions.Assertions.assertThat;
import static org.fest.assertions.Fail.fail;
import static se.softhouse.common.guavaextensions.Functions2.unmodifiableList;
import static se.softhouse.common.guavaextensions.Functions2.unmodifiableMap;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.junit.Test;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.common.io.Files;
import com.google.common.testing.NullPointerTester;
import com.google.common.testing.NullPointerTester.Visibility;

/**
 * Tests for {@link Functions2}
 */
public class Functions2Test
{
	static final Function<Integer, Integer> ADD_ONE = input -> input + 1;

	@Test
	public void testRepeatTwoTimes()
	{
		assertThat(Functions2.repeat(ADD_ONE, 2).apply(0)).isEqualTo(2);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testThatNegativeRepitionsIsIllegal()
	{
		Functions2.repeat(ADD_ONE, -1);
	}

	@Test
	public void testThatZeroRepitionsReturnsTheSameObjectAsInput()
	{
		assertThat(Functions2.repeat(ADD_ONE, 0).apply(1)).isEqualTo(1);
	}

	@Test
	public void testCompoundFunction()
	{
		assertThat(Functions2.compound(ADD_ONE, ADD_ONE).apply(2)).isEqualTo(4);
		Function<Integer, Integer> noOp = Function.identity();
		assertThat(Functions2.compound(noOp, ADD_ONE).apply(2)).isEqualTo(3);
	}

	@Test
	public void testListTransformer()
	{
		List<Integer> transformedList = Functions2.listTransformer(ADD_ONE).apply(Arrays.asList(1, 2, 3));
		assertThat(transformedList).isEqualTo(Arrays.asList(2, 3, 4));
	}

	@Test(expected = UnsupportedOperationException.class)
	public void testThatListTransformerReturnsImmutableList()
	{
		Functions2.listTransformer(ADD_ONE).apply(Arrays.asList(1, 2, 3)).add(4);
	}

	@Test
	public void testThatListTransformerReturnsIdentityFunctionWhenGivenIdentityFunction()
	{
		assertThat(Functions2.listTransformer(Function.identity())).isSameAs(Function.identity());
	}

	@Test
	public void testMapTransformer()
	{
		Map<Integer, Integer> input = new ImmutableMap.Builder<Integer, Integer>().put(1, 2).build();
		Map<Integer, Integer> output = new ImmutableMap.Builder<Integer, Integer>().put(1, 3).build();

		Map<Integer, Integer> transformedMap = Functions2.<Integer, Integer>mapValueTransformer(ADD_ONE).apply(input);
		assertThat(transformedMap).isEqualTo(output);
	}

	@Test(expected = UnsupportedOperationException.class)
	public void testThatMapTransformerReturnsImmutableList()
	{
		Functions2.mapValueTransformer(ADD_ONE).apply(Maps.newHashMap()).clear();
	}

	@Test
	public void testThatMapTransformerReturnsIdentityFunctionWhenGivenIdentityFunction()
	{
		assertThat(Functions2.mapValueTransformer(Function.identity())).isSameAs(Function.identity());
	}

	@Test
	public void testThatFileToStringReturnsFileInUTF8() throws Exception
	{
		String text = "\u1234\u5678";
		File testFile = File.createTempFile("fileToStringTest", ".txt");
		Files.asCharSink(testFile, UTF_8).write(text);

		assertThat(Functions2.fileToString().apply(testFile)).isEqualTo(text);
	}

	@Test
	public void testThatReadingFromDirectoryIsIllegal() throws Exception
	{
		File directory = new File(".");
		try
		{
			assertThat(Functions2.fileToString().apply(directory));
			fail("It should not be possible to convert a directory to a string");
		}
		catch(IllegalArgumentException expected)
		{
			assertThat(expected).hasMessage(directory.getAbsolutePath() + " is a directory, not a file");
		}
	}

	@Test
	public void testThatNonExistingFileGivesGoodErrorMessage() throws Exception
	{
		File nonExistingFile = new File("file_that_does_not_exist");
		try
		{
			assertThat(Functions2.fileToString().apply(nonExistingFile));
			fail("A non existing file should lead to an error");
		}
		catch(IllegalArgumentException expected)
		{
			assertThat(expected).hasMessage("I/O error occured while reading: " + nonExistingFile.getAbsolutePath());
		}
	}

	@Test
	public void testThatNullInputGivesNullOutput() throws Exception
	{
		assertThat(Functions2.listTransformer(unmodifiableList()).apply(null)).isNull();
		assertThat(Functions2.mapValueTransformer(unmodifiableMap()).apply(null)).isNull();
		assertThat(Functions2.unmodifiableList().apply(null)).isNull();
		assertThat(Functions2.unmodifiableMap().apply(null)).isNull();
	}

	@Test(expected = UnsupportedOperationException.class)
	public void testUnmodifiableListMaker()
	{
		Functions2.unmodifiableList().apply(Arrays.asList(new Object())).clear();
	}

	@Test(expected = UnsupportedOperationException.class)
	public void testUnmodifiableMapMaker()
	{
		Functions2.unmodifiableMap().apply(Collections.emptyMap()).clear();
	}

	@Test
	public void testThatNullContractsAreFollowed() throws Exception
	{
		new NullPointerTester().testStaticMethods(Functions2.class, Visibility.PACKAGE);
	}
}
