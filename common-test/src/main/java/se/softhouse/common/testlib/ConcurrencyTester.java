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
import static org.fest.assertions.Assertions.assertThat;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import javax.annotation.concurrent.Immutable;

import com.google.common.util.concurrent.Atomics;

/**
 * Helps to test how code works when it's concurrently accessed.
 */
@Immutable
public final class ConcurrencyTester
{
	private ConcurrencyTester()
	{
	}

	/**
	 * Can {@link #create(int) create} unique {@link Runnable}s based on a number. Used as input to
	 * {@link ConcurrencyTester#verify(RunnableFactory, long, TimeUnit)}.
	 */
	public interface RunnableFactory
	{
		/**
		 * Number of times to run each of the {@link #create(int) created} {@link Runnable}s.
		 */
		int iterationCount();

		/**
		 * Creates a Runnable that should be run {@link #iterationCount()} number of times.
		 * This method is called {@link ConcurrencyTester#NR_OF_CONCURRENT_RUNNERS} times so the
		 * total number of executions will be {@code iteratationCount * NR_OF_CONCURRENT_RUNNERS}.
		 * Any {@link RuntimeException} or {@link Error} thrown from the created runnable will be
		 * caught and propagated through
		 * {@link ConcurrencyTester#verify(RunnableFactory, long, TimeUnit)}.
		 * For performance and test-harness reasons the variables created based on
		 * {@code uniqueNumber} should be saved and reused by the repeated runs.
		 * 
		 * @param uniqueNumber
		 *            should be used to differentiate results made from different threads to
		 *            increase the odds of detecting thread-safety issues.
		 */
		Runnable create(int uniqueNumber);
	}

	/**
	 * The threads should have to fight for CPU time
	 */
	private static final int RUNNERS_PER_PROCESSOR = 3;

	/**
	 * A suitable thread count to have alive at the same time to cause some intended contention.
	 */
	public static final int NR_OF_CONCURRENT_RUNNERS = Runtime.getRuntime().availableProcessors() * RUNNERS_PER_PROCESSOR;

	/**
	 * Verifies that {@link Runnable}s created with {@code factory} can be run concurrently.
	 * Waits for the whole execution to finish for {@code timeout} in {@code unit} time.
	 * 
	 * @throws Throwable if any errors occurs during the concurrent executions
	 */
	public static void verify(RunnableFactory factory, long timeout, TimeUnit unit) throws Throwable
	{
		/**
		 * Makes sure that all threads are alive at the same time
		 */
		CyclicBarrier startSignal = new CyclicBarrier(NR_OF_CONCURRENT_RUNNERS);

		CountDownLatch activeWorkers = new CountDownLatch(NR_OF_CONCURRENT_RUNNERS);

		/**
		 * Used by other threads to report failure
		 */
		AtomicReference<Throwable> failureReporter = Atomics.newReference();

		ExecutorService executor = Executors.newFixedThreadPool(NR_OF_CONCURRENT_RUNNERS);
		int iterationCount = factory.iterationCount();
		for(int i = 0; i < NR_OF_CONCURRENT_RUNNERS; i++)
		{
			Runnable codeToTest = checkNotNull(factory.create(i));
			executor.execute(new BarrieredRunnable(codeToTest, iterationCount, startSignal, activeWorkers, failureReporter));
		}

		InterruptedException interrupted = null;
		try
		{
			if(!activeWorkers.await(timeout, unit))
				throw new AssertionError(
						activeWorkers.getCount() + " of " + NR_OF_CONCURRENT_RUNNERS + " did not finish within " + timeout + " " + unit);
		}
		catch(InterruptedException e)
		{
			// Makes this method reentrant from the same thread
			// Otherwise there could be a risk that await would throw
			// InterruptedException again without actually running any code again
			Thread.interrupted();
			interrupted = e;
		}
		List<Runnable> leftoverTasks = executor.shutdownNow();
		if(failureReporter.get() != null)
			throw failureReporter.get();
		if(interrupted != null)
			// We were interrupted while verifying, propagate so that tests finish up quickly
			throw interrupted;
		assertThat(leftoverTasks).as("Tasks remained even though activeWorkers reached zero").isEmpty();
	}

	/**
	 * Runs a {@link Runnable} ({@code iterationCount} times) after a {@code startSignal} has been
	 * given. When errors are encountered they are reported to {@code failureReporter} and the
	 * thread that created this instance is then {@link Thread#interrupt() interrupted}.
	 */
	private static final class BarrieredRunnable implements Runnable
	{
		private final Thread originThread;
		private final Runnable target;
		private final int iterationCount;
		private final CyclicBarrier startSignal;
		private final AtomicReference<Throwable> failureReporter;
		private final CountDownLatch activeWorkers;

		private BarrieredRunnable(Runnable target, int iterationCount, CyclicBarrier startSignal, CountDownLatch activeWorkers,
				AtomicReference<Throwable> failureReporter)
		{
			this.originThread = Thread.currentThread();
			this.target = target;
			this.iterationCount = iterationCount;
			this.startSignal = startSignal;
			this.activeWorkers = activeWorkers;
			this.failureReporter = failureReporter;
		}

		@Override
		public void run()
		{
			try
			{
				// Give all threads at most 10 seconds to come alive
				startSignal.await(10, TimeUnit.SECONDS);
				for(int i = 0; i < iterationCount; i++)
				{
					target.run();
					startSignal.await();
				}
			}
			catch(Throwable e)
			{
				// Don't report secondary failures
				if(failureReporter.compareAndSet(null, e))
				{
					originThread.interrupt();
				}
				return;
			}
			activeWorkers.countDown();
		}
	}
}
