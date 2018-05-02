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

import static com.google.common.util.concurrent.MoreExecutors.listeningDecorator;
import static java.util.concurrent.Executors.newSingleThreadExecutor;

import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.concurrent.Future;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;

/**
 * Utility for running things concurrently.
 */
public final class Concurrently
{
	private Concurrently()
	{
	}

	/**
	 * Returns a {@link ListenableFuture} running {@code callable} asynchronously. When this method
	 * returns the {@link Callable#call()} may have been run or it may not. Call
	 * {@link Future#get()} on the returned {@link Future} to get the result from the calculation.
	 * The {@link Executor} running the returned {@link Future} is automatically shutdown when it's
	 * done. If threads should be reused and performance is critical you should roll your own and
	 * reuse the executor instead.
	 */
	public static <T> ListenableFuture<T> run(Callable<T> callable)
	{
		final ListeningExecutorService executor = listeningDecorator(newSingleThreadExecutor());
		ListenableFuture<T> futureForCallable = executor.submit(callable);
		Futures.addCallback(futureForCallable, new FutureCallback<T>(){
			@Override
			public void onSuccess(T result)
			{
				executor.shutdownNow();
			}

			@Override
			public void onFailure(Throwable t)
			{
				executor.shutdownNow();
			}
		}, executor);
		return futureForCallable;
	}
}
