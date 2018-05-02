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
import static org.fest.assertions.Fail.fail;
import static se.softhouse.common.testlib.ConcurrencyTester.NR_OF_CONCURRENT_RUNNERS;
import static se.softhouse.common.testlib.ConcurrencyTester.verify;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.junit.Test;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.testing.NullPointerTester;
import com.google.common.testing.NullPointerTester.Visibility;
import com.google.common.util.concurrent.Atomics;

import se.softhouse.common.testlib.ConcurrencyTester.RunnableFactory;

/**
 * Tests for {@link ConcurrencyTester}
 */
public class ConcurrencyTesterTest
{
	@Test
	public void testThatRunsAreInterleavedBetweenDifferentThreadsForEachIteration() throws Throwable
	{
		final Vector<Integer> invocationIdentifiers = new Vector<Integer>();
		final int iterationCount = 100;
		ConcurrencyTester.verify(new RunnableFactory(){
			@Override
			public int iterationCount()
			{
				return iterationCount;
			}

			@Override
			public Runnable create(final int identifierForThread)
			{
				return new Runnable(){
					@Override
					public void run()
					{
						invocationIdentifiers.add(identifierForThread);
					}
				};
			}
		}, Constants.EXPECTED_TEST_TIME_FOR_THIS_SUITE, TimeUnit.MILLISECONDS);

		int maxInARow = 0;
		int currentStreak = 0;
		int lastIdentifier = 0;
		for(int invocationIdentifier : invocationIdentifiers)
		{
			if(invocationIdentifier != lastIdentifier)
			{
				if(currentStreak > maxInARow)
				{
					maxInARow = currentStreak;
				}
				currentStreak = 0;
			}
			lastIdentifier = invocationIdentifier;
			currentStreak++;
		}
		// As there is a barrier in between each run, no thread should be able to run more than
		// two iterations without another thread executing in between.
		// 2 because when all threads have waited for each other the last one to execute could be
		// the first one after the startSignal have been sent to be executed.
		assertThat(maxInARow) //
				.as("At least two of " + NR_OF_CONCURRENT_RUNNERS + " threads should be interleaved "
						+ "with each other, otherwise the concurrency test harness is not acceptable") //
				.isLessThanOrEqualTo(2);
	}

	@Test
	public void testThatExceptionsFromRunnableIsPropagatedAsIs() throws Throwable
	{
		try
		{
			final SimulatedException error = new SimulatedException();
			ConcurrencyTester.verify(new RunnableFactory(){
				@Override
				public int iterationCount()
				{
					return 1;
				}

				@Override
				public Runnable create(int uniqueNumber)
				{
					return new Runnable(){
						@Override
						public void run()
						{
							throw error;
						}
					};
				}
			}, Constants.EXPECTED_TEST_TIME_FOR_THIS_SUITE, TimeUnit.MILLISECONDS);
			fail(error.getClass() + " was not propagated correctly, this could potentially hide AssertionErrors thrown when bugs are introduced");
		}
		catch(SimulatedException expected)
		{
		}
	}

	@Test
	public void testThatEachRunnableIsRunIterationCountTimes() throws Throwable
	{
		final Map<Integer, AtomicInteger> counters = Maps.newHashMap();
		final int iterationCount = 100;
		ConcurrencyTester.verify(new RunnableFactory(){
			@Override
			public int iterationCount()
			{
				return iterationCount;
			}

			@Override
			public Runnable create(int uniqueNumber)
			{
				final AtomicInteger counter = new AtomicInteger();
				counters.put(uniqueNumber, counter);
				return new Runnable(){
					@Override
					public void run()
					{
						counter.incrementAndGet();
					}
				};
			}
		}, Constants.EXPECTED_TEST_TIME_FOR_THIS_SUITE, TimeUnit.MILLISECONDS);

		for(AtomicInteger counter : counters.values())
		{
			assertThat(counter.get()).isEqualTo(iterationCount);
		}
		assertThat(counters).hasSize(ConcurrencyTester.NR_OF_CONCURRENT_RUNNERS);
	}

	@Test
	public void testThatNoUniqueNumberIsUsedTwice() throws Throwable
	{
		final Set<Integer> assignedNumbers = Collections.synchronizedSet(Sets.<Integer>newHashSet());
		ConcurrencyTester.verify(new RunnableFactory(){
			@Override
			public int iterationCount()
			{
				return 2;
			}

			@Override
			public Runnable create(final int uniqueNumber)
			{
				assignedNumbers.add(uniqueNumber);
				return new Runnable(){
					@Override
					public void run()
					{
					}
				};
			}
		}, Constants.EXPECTED_TEST_TIME_FOR_THIS_SUITE, TimeUnit.MILLISECONDS);
		assertThat(assignedNumbers).hasSize(ConcurrencyTester.NR_OF_CONCURRENT_RUNNERS);
	}

	@Test
	public void testThatInterruptedStatusIsClearedWhenInterruptedAndThatRemainingThreadsGetInterrupted() throws Throwable
	{
		final Thread originThread = Thread.currentThread();
		final AtomicReference<Throwable> failure = Atomics.newReference();
		final Lock infinitelyLocked = new ReentrantLock();
		infinitelyLocked.lock();
		// +1 so that the starter thread also can await the start
		final CyclicBarrier startSignal = new CyclicBarrier(ConcurrencyTester.NR_OF_CONCURRENT_RUNNERS + 1);
		final CountDownLatch doneSignal = new CountDownLatch(1);
		Thread interruptableThread = new Thread(){
			@Override
			public void run()
			{
				try
				{
					// Verify that all the Runnables gets an interrupt signal
					ConcurrencyTester.verify(new RunnableFactory(){
						@Override
						public int iterationCount()
						{
							return 1;
						}

						@Override
						public Runnable create(final int uniqueNumber)
						{
							return new Runnable(){
								@Override
								public void run()
								{
									try
									{
										startSignal.await();
										// Wait for the interrupt signal
										infinitelyLocked.lockInterruptibly();
										fail("Executor did not interrupt remaining threads during shutdown operation");
									}
									catch(InterruptedException expected)
									{
										Thread.interrupted();
									}
									catch(BrokenBarrierException e)
									{
										throw new RuntimeException(e);
									}
								}
							};
						}
					}, Constants.EXPECTED_TEST_TIME_FOR_THIS_SUITE, TimeUnit.MILLISECONDS);
					fail("verify completed without being interrupted");
				}
				catch(InterruptedException expected)
				{
				}
				catch(Throwable error)
				{
					failure.compareAndSet(null, error);
					originThread.interrupt();
					return;
				}
				try
				{
					// As the interrupt status of this thread should have been cleared, it
					// should be okay to reenter the verify method
					ConcurrencyTester.verify(new RunnableFactory(){
						@Override
						public int iterationCount()
						{
							return 1;
						}

						@Override
						public Runnable create(int uniqueNumber)
						{
							return new Runnable(){
								@Override
								public void run()
								{
								}
							};
						}
					}, Constants.EXPECTED_TEST_TIME_FOR_THIS_SUITE, TimeUnit.MILLISECONDS);
				}
				catch(Throwable error)
				{
					failure.compareAndSet(null, error);
					originThread.interrupt();
					return;
				}
				doneSignal.countDown();
			}
		};
		try
		{
			interruptableThread.start();
			startSignal.await();
			interruptableThread.interrupt();

			if(!doneSignal.await(Constants.EXPECTED_TEST_TIME_FOR_THIS_SUITE, TimeUnit.MILLISECONDS))
			{
				fail("Timeout while waiting for shutdown of remaining threads");
			}
		}
		catch(InterruptedException error)
		{
			Thread.interrupted();
		}
		if(failure.get() != null)
			throw failure.get();
	}

	@Test
	public void testThatVerifyTimeoutsWhenTasksTakesToLongToExecute() throws Throwable
	{
		final Lock infinitelyLocked = new ReentrantLock();
		infinitelyLocked.lock();
		try
		{
			verify(new RunnableFactory(){
				@Override
				public int iterationCount()
				{
					return 1;
				}

				@Override
				public Runnable create(final int uniqueNumber)
				{
					return new Runnable(){
						@Override
						public void run()
						{
							try
							{
								infinitelyLocked.lockInterruptibly();
								fail("Inifinite test was not interrupted");
							}
							catch(InterruptedException expected)
							{
							}
						}
					};
				}
			}, 0, TimeUnit.NANOSECONDS);
			fail("Infinitely running test did not throw AssertionError when timeout should have occured");
		}
		catch(AssertionError expected)
		{
			assertThat(expected.getMessage())
					.isEqualTo(NR_OF_CONCURRENT_RUNNERS + " of " + NR_OF_CONCURRENT_RUNNERS + " did not finish within 0 NANOSECONDS");
		}
	}

	@Test
	public void testThatNullContractsAreFollowed() throws Exception
	{
		new NullPointerTester().testStaticMethods(ConcurrencyTester.class, Visibility.PACKAGE);
	}
}
