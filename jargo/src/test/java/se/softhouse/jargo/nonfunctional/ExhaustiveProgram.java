/* Copyright 2018 jonatanjonsson
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */
package se.softhouse.jargo.nonfunctional;

import static java.util.Arrays.asList;
import static org.fest.assertions.Assertions.assertThat;
import static se.softhouse.jargo.Arguments.bigDecimalArgument;
import static se.softhouse.jargo.Arguments.bigIntegerArgument;
import static se.softhouse.jargo.Arguments.booleanArgument;
import static se.softhouse.jargo.Arguments.byteArgument;
import static se.softhouse.jargo.Arguments.charArgument;
import static se.softhouse.jargo.Arguments.enumArgument;
import static se.softhouse.jargo.Arguments.fileArgument;
import static se.softhouse.jargo.Arguments.integerArgument;
import static se.softhouse.jargo.Arguments.longArgument;
import static se.softhouse.jargo.Arguments.optionArgument;
import static se.softhouse.jargo.Arguments.shortArgument;
import static se.softhouse.jargo.Arguments.stringArgument;

import java.io.File;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.fest.assertions.Description;

import com.google.common.base.Strings;

import se.softhouse.jargo.Argument;
import se.softhouse.jargo.CommandLineParser;
import se.softhouse.jargo.ParsedArguments;
import se.softhouse.jargo.stringparsers.EnumArgumentTest.Action;

/**
 * Gathers most of jargos features to be able to run orthogonal tests around every feature
 */
public class ExhaustiveProgram
{
	final Argument<Boolean> enableLoggingArgument = optionArgument("-l", "--enable-logging").description("Output debug information to standard out")
			.build();

	final Argument<Integer> port = integerArgument("-p", "--listen-port").required().description("The port to start the server on.").build();

	final Argument<String> greetingPhraseArgument = stringArgument().required().description("A greeting phrase to greet new connections with")
			.build();

	final Argument<Long> longArgument = longArgument("--long").build();

	final Argument<Short> shortArgument = shortArgument("--short").build();

	final Argument<Byte> byteArgument = byteArgument("--byte").build();

	final Argument<File> fileArgument = fileArgument("--file").defaultValueDescription("The current directory").build();

	final Argument<String> string = stringArgument("--string").build();

	final Argument<Character> charArgument = charArgument("--char").build();

	final Argument<Boolean> boolArgument = booleanArgument("--bool").build();

	final Argument<Map<String, Boolean>> propertyArgument = booleanArgument("-B").asPropertyMap().build();

	final Argument<List<Boolean>> arityArgument = booleanArgument("--arity").arity(6).build();

	final Argument<List<Integer>> repeatedArgument = integerArgument("--repeated").repeated().build();

	final Argument<List<Integer>> splittedArgument = integerArgument("--split").separator("=").splitWith(",").build();

	final Argument<Integer> transformedArgument = stringArgument("--transformed").transform(String::length).build();

	final Argument<Action> enumArgument = enumArgument(Action.class, "--enum").build();

	final Argument<List<Integer>> variableArityArgument = integerArgument("--variableArity").variableArity().build();

	final Argument<BigInteger> bigIntegerArgument = bigIntegerArgument("--big-integer").build();

	final Argument<BigDecimal> bigDecimalArgument = bigDecimalArgument("--big-decimal").build();

	/**
	 * The shared instance that the different threads will use
	 */
	final CommandLineParser parser = CommandLineParser
			.withArguments(	greetingPhraseArgument, enableLoggingArgument, port, longArgument, shortArgument, byteArgument, fileArgument, string,
							charArgument, boolArgument, propertyArgument, arityArgument, repeatedArgument, splittedArgument, transformedArgument,
							enumArgument, variableArityArgument, bigIntegerArgument, bigDecimalArgument)
			.programDescription("Example of most argument types that jargo can handle by default").locale(Locale.US);

	final class ArgumentParseRunner implements Runnable
	{
		/**
		 * The unique number each thread is assigned, used to differentiate results
		 */
		private final int offset;
		private ParsedArguments parsedArguments;
		private final String[] inputArgs;
		private final int portNumber;
		private final String greetingPhrase;
		private final char c;
		private final boolean bool;
		private final String enableLogging;
		private final short shortNumber;
		private final byte byteNumber;
		private final long longNumber;
		private final BigInteger bigInteger;
		private final String str;
		private final String action;
		private final Map<String, Boolean> propertyMap = new HashMap<String, Boolean>();
		private final int amountOfVariableArity;
		private final String variableArityIntegers;
		private final List<Boolean> arityBooleans;
		private final String arityString;
		private final String filename;
		private final File file;
		private final BigDecimal bigDecimal;
		private final String transformString;

		ArgumentParseRunner(int offset)
		{
			this.offset = offset;
			portNumber = 8090 + offset;

			greetingPhrase = "Hello" + offset;
			c = charFor(offset);
			bool = offset % 2 == 0;
			enableLogging = bool ? "-l " : "";
			shortNumber = (short) (1232 + offset);
			byteNumber = (byte) (123 + offset);
			longNumber = 1234567890L + offset;
			bigInteger = BigInteger.valueOf(12312313212323L + offset);
			str = "FooBar" + offset;
			action = Action.values()[offset % Action.values().length].toString();

			propertyMap.put("foo" + offset, true);
			propertyMap.put("bar", false);

			amountOfVariableArity = offset % 10;
			variableArityIntegers = Strings.repeat(" " + portNumber, amountOfVariableArity);
			arityBooleans = asList(bool, bool, bool, bool, bool, bool);
			arityString = Strings.repeat(" " + bool, 6);

			filename = "user_" + offset;
			file = new File(filename);
			bigDecimal = BigDecimal.valueOf(Long.MAX_VALUE);
			transformString = Strings.repeat("a", offset % 50);
			String inputArguments = enableLogging + "-p " + portNumber + " " + greetingPhrase + " --long " + longNumber + " --big-integer "
					+ bigInteger + " --short " + shortNumber + " --byte " + byteNumber + " --file " + filename + " --string " + str + " --char " + c
					+ " --bool " + bool + " -Bfoo" + offset + "=true -Bbar=false" + " --arity" + arityString + " --repeated 1 --repeated " + offset
					+ " --split=1," + (2 + offset) + ",3" + " --transformed " + transformString + " --enum " + action + " --big-decimal " + bigDecimal
					+ " --variableArity" + variableArityIntegers;
			inputArgs = inputArguments.split(" ");
		}

		@Override
		public void run()
		{
			parsedArguments = parser.parse(inputArgs);

			checkThat(enableLoggingArgument).received(bool);
			checkThat(port).received(portNumber);
			checkThat(greetingPhraseArgument).received(greetingPhrase);
			checkThat(longArgument).received(longNumber);
			checkThat(bigIntegerArgument).received(bigInteger);
			checkThat(shortArgument).received(shortNumber);
			checkThat(byteArgument).received(byteNumber);
			checkThat(fileArgument).received(file);
			checkThat(string).received(str);
			checkThat(charArgument).received(c);
			checkThat(boolArgument).received(bool);
			checkThat(arityArgument).received(arityBooleans);
			checkThat(repeatedArgument).received(asList(1, offset));
			checkThat(splittedArgument).received(asList(1, 2 + offset, 3));
			checkThat(propertyArgument).received(propertyMap);
			checkThat(enumArgument).received(Action.valueOf(action));
			checkThat(bigDecimalArgument).received(bigDecimal);
			assertThat(parsedArguments.get(variableArityArgument)).hasSize(amountOfVariableArity);
			checkThat(transformedArgument).received(transformString.length());
		}

		private char charFor(int o)
		{
			char result = (char) (o % Character.MAX_VALUE);
			return result == ' ' ? '.' : result; // A space would be trimmed to nothing
		}

		/**
		 * Verifies that an argument received an expected value
		 */
		public <T> Checker<T> checkThat(Argument<T> argument)
		{
			return new Checker<T>(argument);
		}

		private class Checker<T>
		{
			Argument<T> arg;

			public Checker(Argument<T> argument)
			{
				arg = argument;
			}

			public void received(final T expectation)
			{
				final T parsedValue = parsedArguments.get(arg);
				Description description = new Description(){
					// In a concurrency test it makes a big performance difference
					// with lazily created descriptions
					@Override
					public String value()
					{
						return "Failed to match: " + arg + ", actual: " + parsedValue + ", expected: " + expectation;
					}
				};
				assertThat(parsedValue).as(description).isEqualTo(expectation);
			}
		}
	}
}
