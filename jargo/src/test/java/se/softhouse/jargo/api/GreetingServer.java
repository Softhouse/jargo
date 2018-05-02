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
package se.softhouse.jargo.api;

import static se.softhouse.jargo.Arguments.integerArgument;
import static se.softhouse.jargo.Arguments.optionArgument;
import static se.softhouse.jargo.Arguments.stringArgument;
import static se.softhouse.jargo.CommandLineParser.withArguments;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;

import com.google.common.base.Ascii;
import com.google.common.base.Charsets;

import se.softhouse.common.strings.Describers;
import se.softhouse.jargo.Argument;
import se.softhouse.jargo.ArgumentException;
import se.softhouse.jargo.CommandLineParser;
import se.softhouse.jargo.ParsedArguments;

public class GreetingServer
{
	// Statically configured arguments, immutable and therefore also reusable by other parsers
	static final Argument<Boolean> ENABLE_LOGGING = optionArgument("-l", "--enable-logging").description("Output debug information to standard out")
			.build();

	static final Argument<List<Integer>> PORTS = integerArgument("-p", "--listen-port").limitTo(n -> n >= 0 && n <= 65536).defaultValue(10000)
			.defaultValueDescriber(Describers.toStringDescriber()).repeated().description("The port clients should connect to")
			.metaDescription("port").build();

	static final Argument<List<String>> GREETING_PHRASES = stringArgument().variableArity()
			.description("A greeting phrase to greet new connections with").build();

	// Immutable and therefore also reusable parser
	static final CommandLineParser PARSER = withArguments(GREETING_PHRASES, ENABLE_LOGGING, PORTS);

	public static void main(String[] args)
	{
		System.out.println(PARSER.usage());

		GreetingServer server = new GreetingServer();
		server.startWithArgs("--enable-logging", "--listen-port", "8090", "Hello world", "Habla Senor");
		server.startWithArgs("-l");
	}

	void startWithArgs(String ... arguments)
	{
		try
		{
			ParsedArguments args = PARSER.parse(arguments);

			start(args.get(ENABLE_LOGGING), args.get(PORTS), args.get(GREETING_PHRASES));
		}
		catch(ArgumentException exception)
		{
			System.out.println(exception.getMessageAndUsage());
			System.exit(1);
		}
	}

	void start(boolean loggingIsEnabled, List<Integer> ports, List<String> greetingPhrases)
	{
		if(loggingIsEnabled)
		{
			System.out.println("Starting server on port " + ports + ", will greet new connections with " + greetingPhrases);
		}
		try
		{
			for(Integer port : ports)
			{
				ServerSocket server = new ServerSocket(port, 10000);
				Socket client = server.accept();
				Writer writer = new OutputStreamWriter(client.getOutputStream(), Charsets.UTF_8);
				for(String greetingPhrase : greetingPhrases)
				{
					writer.append(greetingPhrase).append((char) Ascii.LF);
				}
				writer.close();
				server.close();
			}
		}
		catch(IOException e)
		{
			throw new RuntimeException("Bad shit happened", e);
		}
	}
}
