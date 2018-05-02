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

import javax.annotation.concurrent.Immutable;

/**
 * Sometimes tests makes things that the tested API tells it not to,
 * these strings explain some motivations about why certain test code looks like it does.
 */
@Immutable
public final class Explanation
{
	private Explanation()
	{
	}

	/**
	 * Often used as a justification of RV_RETURN_VALUE_IGNORED. It's ignored simply because it
	 * verifies that invalid arguments is handled directly instead of passively and thus doing
	 * something with the returned value would only be confusing and it could even
	 * make the test fail for the wrong reasons.
	 */
	public static final String FAIL_FAST = "fail-fast during configuration phase";
}
