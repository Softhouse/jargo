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
package se.softhouse.jargo.nonfunctional;

import java.io.FilePermission;
import java.net.NetPermission;
import java.security.Permission;
import java.util.PropertyPermission;
import java.util.Set;

import com.google.common.collect.Sets;

/**
 * Tests that argument parser works with an extremely restrictive {@link SecurityManager} installed.
 */
public final class SecurityTest
{
	private SecurityTest()
	{
	}

	public static void main(String[] args) throws Throwable
	{
		// TODO(jontejj): introduce this as a security test
		System.setSecurityManager(new SecurityManager(){
			@Override
			public void checkPermission(Permission perm)
			{
				if(perm instanceof FilePermission)
				{
					// To load the java class
					if(perm.getActions().equals("read"))
						return;
				}
				else if(perm instanceof NetPermission)
				{
					// To load the java class
					if(perm.getName().equals("specifyStreamHandler"))
						return;
				}
				else if(perm instanceof RuntimePermission)
				{
					// To shutdown the executor
					if(perm.getName().equals("modifyThread"))
						return;
				}
				else if(perm instanceof PropertyPermission)
				{
					if(READABLE_PROPERTIES.contains(perm.getName()) && perm.getActions().equals("read"))
						return;
				}
				throw new SecurityException("Permission: " + perm + " not granted");
			}
		});
		ConcurrencyTest test = new ConcurrencyTest();
		test.testThatDifferentArgumentsCanBeParsedConcurrently();
	}

	static final Set<String> READABLE_PROPERTIES = Sets.newHashSet(	"user.timezone", "user.country", "java.home",
																	"org.joda.time.DateTimeZone.Provider", "org.joda.time.DateTimeZone.NameProvider",
																	"sun.timezone.ids.oldmapping", "os.name");
}
