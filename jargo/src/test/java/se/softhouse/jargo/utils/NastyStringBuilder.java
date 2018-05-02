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
package se.softhouse.jargo.utils;

import static org.mockito.Matchers.anyString;
import static org.powermock.api.mockito.PowerMockito.mock;

import java.io.IOException;

import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.powermock.api.mockito.PowerMockito;

/**
 * {@link StringBuilder} calls. See <a
 * href="http://code.google.com/p/powermock/wiki/MockSystem">MockSystem</a> for more details.
 */
public final class NastyStringBuilder
{
	private NastyStringBuilder()
	{
	}

	public static StringBuilder thatThrows(final IOException exceptionToThrow)
	{
		StringBuilder nastyBuilder = mock(StringBuilder.class);
		Answer<StringBuilder> withThrow = new Answer<StringBuilder>(){
			@Override
			public StringBuilder answer(InvocationOnMock invocation) throws Throwable
			{
				throw exceptionToThrow;
			}
		};
		PowerMockito.when(nastyBuilder.append(anyString())).thenAnswer(withThrow);
		return nastyBuilder;
	}
}
