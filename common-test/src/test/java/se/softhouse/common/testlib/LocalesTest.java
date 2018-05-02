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

import static java.util.Locale.CANADA;
import static org.fest.assertions.Assertions.assertThat;
import static org.fest.assertions.Fail.fail;
import static se.softhouse.common.testlib.Constants.EXPECTED_TEST_TIME_FOR_THIS_SUITE;
import static se.softhouse.common.testlib.Locales.SWEDISH;
import static se.softhouse.common.testlib.Locales.TURKISH;

import java.lang.Thread.UncaughtExceptionHandler;
import java.util.Locale;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.Test;
import org.junit.runner.RunWith;

import com.google.code.tempusfugit.concurrency.ConcurrentTestRunner;
import com.google.common.testing.NullPointerTester;
import com.google.common.testing.NullPointerTester.Visibility;
import com.google.common.util.concurrent.Atomics;

@RunWith(ConcurrentTestRunner.class)
public class LocalesTest
{
	@Test
	public void testThatResetDefaultLocaleResetsToTheOneBeforeSetDefaultWasCalled() throws Exception
	{
		Locale before = Locales.setDefault(Locales.SWEDISH);
		Locale after = Locales.resetDefaultLocale();
		assertThat(after).isEqualTo(before);
	}

	@Test
	public void testThatInterruptedExceptionIsThrownForTimeoutsWhenWaitingForThreadToBeDoneWithLocale() throws Throwable
	{
		final String ownerThread = Thread.currentThread().getName();
		final Semaphore greenSignal = new Semaphore(1);

		final AtomicReference<Throwable> failure = Atomics.newReference();
		Thread thread = new Thread(){
			@Override
			public void run()
			{
				try
				{
					greenSignal.release();
					Locales.setDefault(TURKISH);

					fail("Should timeout as other thread isn't finished with using the default locale");
				}
				catch(InterruptedException expectedException)
				{
					String currentThreadDescription = Thread.currentThread().getName();
					String startOfMessage = currentThreadDescription + " waited on";
					String endOfMessage = "[Locked by thread " + ownerThread + "] to finish using " + CANADA + " but got interrupted";
					assertThat(expectedException.getMessage()).startsWith(startOfMessage).endsWith(endOfMessage);
					// Verify that this thread can't reset the locale
					try
					{
						Locales.resetDefaultLocale();
						fail("Current thread: " + Thread.currentThread() + " should not own lock and therefore be forbidden to release it");
					}
					catch(IllegalStateException expected)
					{
						startOfMessage = "Current thread: " + currentThreadDescription + " may not unlock: ";
						endOfMessage = "[Locked by thread " + ownerThread + "]";
						assertThat(expected.getMessage()).startsWith(startOfMessage).endsWith(endOfMessage);
					}
					assertThat(Locale.getDefault()).isEqualTo(CANADA);
				}
			}
		};
		thread.setUncaughtExceptionHandler(new UncaughtExceptionHandler(){
			@Override
			public void uncaughtException(Thread t, Throwable e)
			{
				failure.set(e);
			}
		});

		Locales.setDefault(SWEDISH);
		// This thread should be allowed to set the locale several times during its "period"
		Locales.setDefault(CANADA);

		thread.start();
		greenSignal.acquireUninterruptibly();
		thread.interrupt();
		thread.join(EXPECTED_TEST_TIME_FOR_THIS_SUITE);
		if(failure.get() != null)
			throw failure.get();
		Locales.resetDefaultLocale();
	}

	@Test
	public void testThatNullContractsAreFollowed() throws Exception
	{
		new NullPointerTester().testStaticMethods(Locales.class, Visibility.PACKAGE);
	}
}
