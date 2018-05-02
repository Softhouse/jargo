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

import java.lang.ref.PhantomReference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;

import javax.annotation.concurrent.Immutable;
import javax.annotation.concurrent.ThreadSafe;

import com.google.common.testing.GcFinalization;

/**
 * Helps to test for memory leaks
 */
@Immutable
public final class MemoryTester
{
	private MemoryTester()
	{
	}

	/**
	 * A simple {@link PhantomReference} that can be used to assert that all references to it is
	 * gone.
	 */
	@ThreadSafe
	public static final class FinalizationAwareObject extends PhantomReference<Object>
	{
		private final WeakReference<Object> weakReference;

		private FinalizationAwareObject(Object referent, ReferenceQueue<Object> referenceQueue)
		{
			super(checkNotNull(referent), referenceQueue);
			weakReference = new WeakReference<Object>(referent, referenceQueue);
		}

		/**
		 * Runs a full {@link System#gc() GC} and asserts that the reference has been released
		 * afterwards
		 */
		public void assertThatNoMoreReferencesToReferentIsKept()
		{
			String leakedObjectDescription = String.valueOf(weakReference.get());
			GcFinalization.awaitFullGc();
			assertThat(isEnqueued()).as("Object: " + leakedObjectDescription + " was leaked").isTrue();
		}
	}

	/**
	 * Creates a {@link FinalizationAwareObject} that will know if {@code referenceToKeepTrackOff}
	 * has been garbage collected. Call
	 * {@link FinalizationAwareObject#assertThatNoMoreReferencesToReferentIsKept()} when you expect
	 * all references to {@code referenceToKeepTrackOff} be gone.
	 */
	public static FinalizationAwareObject createFinalizationAwareObject(Object referenceToKeepTrackOff)
	{
		return new FinalizationAwareObject(referenceToKeepTrackOff, new ReferenceQueue<Object>());
	}
}
