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
import static se.softhouse.common.testlib.Thrower.asUnchecked;

import java.io.IOException;

import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mock;
import org.powermock.api.mockito.PowerMockito;

import com.google.common.testing.NullPointerTester;
import com.google.common.testing.NullPointerTester.Visibility;

/**
 * Tests {@link Thrower}
 */
public class ThrowerTest
{
	@Test(expected = IOException.class)
	public void testThatThrowerCanThrowCheckedException()
	{
		throw asUnchecked(new IOException());
	}

	@Mock Thrower<RuntimeException> mockedThrower;

	@Test
	@Ignore("enable when https://github.com/jacoco/jacoco/issues/51 has been fixed")
	public void testThatThrowerSpecifiesExceptionAsReturnedToKeepTheCompilerHappyWhenReturnValuesAreRequired() throws Exception
	{
		RuntimeException e = new RuntimeException();
		PowerMockito.when(mockedThrower.sneakyThrow(e)).thenReturn(e);

		PowerMockito.whenNew(Thrower.class).withAnyArguments().thenReturn(mockedThrower);
		Object asUnchecked = Thrower.asUnchecked(e);
		assertThat(asUnchecked).isSameAs(e);
	}

	@Test
	public void testThatNullContractsAreFollowed() throws Exception
	{
		new NullPointerTester().testStaticMethods(Thrower.class, Visibility.PACKAGE);
	}
}
