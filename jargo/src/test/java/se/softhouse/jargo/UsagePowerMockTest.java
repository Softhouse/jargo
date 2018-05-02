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
package se.softhouse.jargo;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import se.softhouse.jargo.utils.NastyStringBuilder;

import java.io.IOException;
import java.util.Collections;
import java.util.Locale;

import static org.fest.assertions.Assertions.*;
import static org.fest.assertions.Fail.fail;

@RunWith(PowerMockRunner.class)
@PrepareForTest({NastyStringBuilder.class, Usage.class})
public class UsagePowerMockTest
{

	@Test
	@Ignore("Can be enabled when http://code.google.com/p/powermock/issues/list?q=label:Milestone-Release1.6 has been released")
	public void testThatIOExceptionIsPropagatedAsAssertionErrorForImpossibleCase_StringBuilder()
	{
		final IOException thrown = new IOException("Can't happen");
		Appendable nastyBuilder = NastyStringBuilder.thatThrows(thrown);
		Usage usage = new Usage(Collections.<Argument<?>>emptyList(), Locale.ENGLISH, ProgramInformation.AUTO, false);

		try
		{
			usage.printOn((StringBuilder) nastyBuilder);
			fail("IOException wasn't propagated correctly");
		}
		catch(AssertionError expected)
		{
			assertThat(expected).hasMessage("Can't happen");
		}
	}
}
