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
import static com.google.common.base.Preconditions.checkState;
import static java.lang.String.format;
import static java.lang.Thread.currentThread;

import java.util.Locale;
import java.util.concurrent.locks.ReentrantLock;

import javax.annotation.concurrent.ThreadSafe;

/**
 * <pre>
 * Utilities for testing different {@link Locale}s.
 * </pre>
 */
@ThreadSafe
public final class Locales
{
	private Locales()
	{
	}

	/**
	 * The {@link Locale} representing Sweden
	 */
	public static final Locale SWEDISH = new Locale("sv", "SE");

	/**
	 * See <a href="http://java.sys-con.com/node/46241">Turkish Java Needs Special Brewing</a> and
	 * <a href="http://www.codinghorror.com/blog/2008/03/whats-wrong-with-turkey.html">What's Wrong
	 * With Turkey?</a> for more details about why the Turkish {@link Locale} is suitable for
	 * testing purposes.
	 */
	public static final Locale TURKISH = new Locale("tr", "TR");

	private static final ReentrantLock LOCK = new ReentrantLock();

	private static volatile Locale defaultLocale = Locale.getDefault();

	/**
	 * <pre>
	 *  Makes it possible to test invalid usages of {@link Locale#getDefault()} in a synchronized
	 *  manner making sure that test cases don't fail suddenly when they are run concurrently.
	 *  I.e a pipelined version of {@link Locale#getDefault()}.
	 *
	 * <b>Note:</b> Don't forget to call {@link #resetDefaultLocale()} when you're done with using
	 * the {@link Locale#getDefault()} method. Otherwise other threads waiting to use it may starve.
	 * </pre>
	 * 
	 * @param locale the locale {@link Locale#getDefault()} should return
	 * @throws InterruptedException if the current thread is interrupted
	 * @return the previous default locale
	 */
	public static Locale setDefault(Locale locale) throws InterruptedException
	{
		checkNotNull(locale);
		// Allow the same thread to change the locale during its "period" without locking
		if(!LOCK.isHeldByCurrentThread())
		{
			try
			{
				LOCK.lockInterruptibly();
				defaultLocale = Locale.getDefault();
			}
			catch(InterruptedException interrupt)
			{
				throw new InterruptedException(
						format("%s waited on %s to finish using %s but got interrupted", currentThread().getName(), LOCK, Locale.getDefault()));
			}
		}

		try
		{
			Locale.setDefault(locale);
		}
		catch(SecurityException e)
		{
			LOCK.unlock(); // Let someone else try again
			throw e;
		}
		return defaultLocale;
	}

	/**
	 * Resets {@link Locale#getDefault()} to what it was when {@link #setDefault(Locale)} was
	 * called. Should be called after you're done with using {@link Locale#getDefault()}.
	 *
	 * @return the default {@link Locale} that was set
	 * @throws IllegalStateException if this thread wasn't the last one to successfully call
	 *             {@link #setDefault(Locale)}
	 */
	public static Locale resetDefaultLocale()
	{
		checkState(LOCK.isHeldByCurrentThread(), "Current thread: %s may not unlock: %s", Thread.currentThread().getName(), LOCK);
		try
		{
			Locale.setDefault(defaultLocale);
			return defaultLocale;
		}
		finally
		{
			LOCK.unlock();
		}
	}
}
