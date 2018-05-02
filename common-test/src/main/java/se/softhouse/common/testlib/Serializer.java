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

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

import javax.annotation.concurrent.Immutable;

/**
 * Aids tests that needs to test how APIs behave when their objects are serialized
 */
@Immutable
public final class Serializer
{
	private Serializer()
	{
	}

	/**
	 * Serializes {@code objectToSerialize} and reads the serialized bytes into an object again.
	 * Requires that readReplace() isn't overridden to return a type not assignable as a {@code T},
	 * for default implementations of {@link Serializable} this is true.
	 */
	public static <T extends Serializable> T clone(T objectToSerialize)
	{
		checkNotNull(objectToSerialize);
		try
		{
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			ObjectOutputStream oos = new ObjectOutputStream(baos);
			oos.writeObject(objectToSerialize);
			ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
			ObjectInputStream ois = new ObjectInputStream(bais);

			@SuppressWarnings("unchecked")
			T deserializedObject = (T) ois.readObject();
			return deserializedObject;
		}
		catch(IOException e)
		{
			throw new IllegalArgumentException("Failed to clone: " + objectToSerialize, e);
		}
		catch(ClassNotFoundException e)
		{
			throw new IllegalArgumentException("Failed to clone: " + objectToSerialize, e);
		}
	}
}
