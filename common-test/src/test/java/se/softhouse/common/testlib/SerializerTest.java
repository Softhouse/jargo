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

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;

import org.junit.Test;

import com.google.common.testing.NullPointerTester;
import com.google.common.testing.NullPointerTester.Visibility;

public class SerializerTest
{
	@Test
	public void testThatChangesInDeserializedObjectDoesNotAffectSource()
	{
		SerializableObject source = new SerializableObject();
		SerializableObject deserialized = Serializer.clone(source);

		assertThat(deserialized.number).isZero();
		deserialized.number = 1;
		assertThat(source.number).isZero();
	}

	private static final class SerializableObject implements Serializable
	{
		private static final long serialVersionUID = 1L;

		int number = 0;
	}

	@Test
	public void testThatMissingClassThrows()
	{
		MissingClassForObject source = new MissingClassForObject();

		try
		{
			Serializer.clone(source);
			fail("Class should be missing");
		}
		catch(IllegalArgumentException expected)
		{
			assertThat(expected).hasMessage("Failed to clone: foo class");
			assertThat(expected.getCause()).isSameAs(MissingClassForObject.EXCEPTION);
		}
	}

	private static final class MissingClassForObject implements Serializable
	{
		private static final long serialVersionUID = 1L;

		private static final ClassNotFoundException EXCEPTION = new ClassNotFoundException("Missing class for serialized object");

		/**
		 * @param stream unused
		 * @throws ClassNotFoundException always
		 */
		private void readObject(ObjectInputStream stream) throws ClassNotFoundException
		{
			throw EXCEPTION;
		}

		@Override
		public String toString()
		{
			return "foo class";
		}
	}

	@Test
	public void testThatMissingBytesThrows()
	{
		MissingBytesForObject source = new MissingBytesForObject();

		try
		{
			Serializer.clone(source);
			fail("Bytes should be missing");
		}
		catch(IllegalArgumentException expected)
		{
			assertThat(expected).hasMessage("Failed to clone: foo class");
			assertThat(expected.getCause()).isSameAs(MissingBytesForObject.EXCEPTION);
		}
	}

	private static final class MissingBytesForObject implements Serializable
	{
		private static final long serialVersionUID = 1L;

		private static final IOException EXCEPTION = new IOException("missing bytes");

		/**
		 * @param stream unused
		 * @throws IOException always
		 */
		private void readObject(ObjectInputStream stream) throws IOException
		{
			throw EXCEPTION;
		}

		@Override
		public String toString()
		{
			return "foo class";
		}
	}

	@Test
	public void testThatNullContractsAreFollowed() throws Exception
	{
		new NullPointerTester().testStaticMethods(Serializer.class, Visibility.PACKAGE);
	}
}
