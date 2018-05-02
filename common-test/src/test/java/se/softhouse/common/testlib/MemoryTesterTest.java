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

import se.softhouse.common.testlib.MemoryTester.FinalizationAwareObject;

import com.google.common.testing.NullPointerTester;
import com.google.common.testing.NullPointerTester.Visibility;

/**
 * Tests for {@link MemoryTester}
 */
public class MemoryTesterTest
{
	@Test
	public void testThatHoldingOnToAnObjectIsTreatedAsALeak() throws Exception
	{
		Object holdMeTight = new String("Hold-me-tight");
		FinalizationAwareObject finalizationAwareObject = MemoryTester.createFinalizationAwareObject(holdMeTight);
		try
		{
			finalizationAwareObject.assertThatNoMoreReferencesToReferentIsKept();
			fail("holdMeTight was held but memory leak tester did not discover it");
		}
		catch(AssertionError expected)
		{
			assertThat(expected).hasMessage("[Object: Hold-me-tight was leaked] expected:<[tru]e> but was:<[fals]e>");
		}
	}

	@Test
	public void testThatUnreferencedObjectIsOkay() throws Exception
	{
		Object releaseMe = new Object();
		FinalizationAwareObject finalizationAwareObject = MemoryTester.createFinalizationAwareObject(releaseMe);
		releaseMe = null;
		finalizationAwareObject.assertThatNoMoreReferencesToReferentIsKept();
	}

	@Test
	public void testThatNullContractsAreFollowed() throws Exception
	{
		new NullPointerTester().testStaticMethods(MemoryTester.class, Visibility.PACKAGE);
		new NullPointerTester().testInstanceMethods(MemoryTester.createFinalizationAwareObject(""), Visibility.PACKAGE);
	}
}
