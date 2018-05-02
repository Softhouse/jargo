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

import static com.google.common.util.concurrent.MoreExecutors.directExecutor;
import static org.fest.assertions.Assertions.assertThat;

import java.util.concurrent.Callable;

import org.junit.Test;

import com.google.common.testing.NullPointerTester;
import com.google.common.testing.NullPointerTester.Visibility;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

/**
 * Tests for {@link Concurrently}.
 */
public class ConcurrentlyTest
{

	@Test
	public void testThatConcurrentlyRunsTheCallable() throws Exception
	{
		final Object value = new Object();
		ListenableFuture<Object> future = Concurrently.run(new Callable<Object>(){
			@Override
			public Object call() throws Exception
			{
				return value;
			}
		});

		Futures.addCallback(future, new FutureCallback<Object>(){
			@Override
			public void onSuccess(Object result)
			{
				assertThat(result).isSameAs(value);
			}

			@Override
			public void onFailure(Throwable t)
			{
				throw new RuntimeException(t);
			}
		}, directExecutor());
		assertThat(future.get()).isSameAs(value);
	}

	@Test
	public void testThatConcurrentlyShutsDownExecutorForFailures() throws Exception
	{
		final SimulatedException simulatedException = new SimulatedException();
		ListenableFuture<Object> future = Concurrently.run(new Callable<Object>(){
			@Override
			public Object call() throws Exception
			{
				throw simulatedException;
			}
		});
		Futures.addCallback(future, new FutureCallback<Object>(){
			@Override
			public void onSuccess(Object result)
			{
			}

			@Override
			public void onFailure(Throwable t)
			{
				assertThat(t).isSameAs(simulatedException);
			}
		}, directExecutor());
	}

	@Test
	public void testThatNullContractsAreFollowed() throws Exception
	{
		new NullPointerTester().testStaticMethods(Concurrently.class, Visibility.PACKAGE);
	}
}
