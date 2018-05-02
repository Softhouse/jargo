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

import org.junit.Test;

/**
 * Tests for {@link SimulatedException}
 */
public class SimulatedExceptionTest
{
	/**
	 * Doesn't really test something but this is how common usage of SimulatedException looks like.
	 * Note how something like {@link IllegalArgumentException} isn't expected making the test more
	 * explicit and avoiding the pitfall that something may throw an
	 * {@link IllegalArgumentException} for the wrong reasons.
	 */
	@Test(expected = SimulatedException.class)
	public void testSimulatedException()
	{
		Object nastyObject = new Object(){
			@Override
			public String toString()
			{
				throw new SimulatedException();
			}
		};
		nastyObject.toString();
	}

	@Test(expected = SimulatedException.class)
	public void testSimulatedExceptionWithMessage()
	{
		Object nastyObject = new Object(){
			@Override
			public String toString()
			{
				throw new SimulatedException("Some message that says what's being simulated");
			}
		};
		nastyObject.toString();
	}
}
