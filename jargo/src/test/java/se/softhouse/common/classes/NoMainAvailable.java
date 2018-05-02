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
package se.softhouse.common.classes;

public final class NoMainAvailable
{
	private NoMainAvailable()
	{
	}

	public static void main(String[] args)
	{
		final Thread mainThread = Thread.currentThread();
		Thread thread = new Thread(){
			@Override
			public void run()
			{
				while(mainThread.isAlive())
				{
					try
					{
						Thread.sleep(50);
					}
					catch(InterruptedException e)
					{
						Thread.currentThread().interrupt();
					}
				}
				try
				{
					Classes.mainClassName();
					System.err.print("Requesting name of mainClass after main thread has died should trigger an IllegalStateException");
				}
				catch(IllegalStateException expected)
				{
					System.err.print(expected.getMessage());
				}
			}
		};
		thread.start();
	}
}
