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
package se.softhouse.jargo.stringparsers.custom;

/**
 * A value object representing a port number
 */
public class Port implements Comparable<Port>
{
	public final Integer port;
	public int toStringCallCount = 0;

	public Port(final int port)
	{
		this.port = port;
	}

	public static Port parse(String portNumber)
	{
		return new Port(Integer.parseInt(portNumber));
	}

	public static final Port MIN = new Port(0);
	public static final Port MAX = new Port(Short.MAX_VALUE * 2);
	public static final Port DEFAULT = new Port(8080);

	@Override
	public boolean equals(Object obj)
	{
		if(!(obj instanceof Port))
			return false;

		Port that = (Port) obj;
		return this.port.equals(that.port);
	}

	@Override
	public int hashCode()
	{
		return port.hashCode();
	}

	@Override
	public int compareTo(Port that)
	{
		return port.compareTo(that.port);
	}

	@Override
	public String toString()
	{
		toStringCallCount++;
		return Integer.toString(port);
	}

}
