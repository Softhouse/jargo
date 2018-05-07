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
package se.softhouse.jargo;

import static org.fest.assertions.Assertions.assertThat;
import static se.softhouse.common.strings.StringsUtil.NEWLINE;
import static se.softhouse.common.testlib.UtilityClassTester.testUtilityClassDesign;
import static se.softhouse.common.testlib.UtilityClassTester.testUtilityClassDesignForAllClassesAround;
import static se.softhouse.jargo.Arguments.integerArgument;
import static se.softhouse.jargo.Arguments.stringArgument;
import static se.softhouse.jargo.ProgramInformation.withProgramName;
import static se.softhouse.jargo.StringParsers.optionParser;
import static se.softhouse.jargo.utils.Assertions2.assertThat;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Locale;
import java.util.function.Function;

import org.junit.Test;

import se.softhouse.common.guavaextensions.Predicates2;
import se.softhouse.common.numbers.NumberType;
import se.softhouse.common.testlib.EnumTester;
import se.softhouse.jargo.Argument.ParameterArity;
import se.softhouse.jargo.StringParsers.StringParserBridge;
import se.softhouse.jargo.StringParsers.StringStringParser;
import se.softhouse.jargo.StringParsers.TransformingParser;
import se.softhouse.jargo.Usage.Row;
import se.softhouse.jargo.commands.Build;
import se.softhouse.jargo.internal.Texts.ProgrammaticErrors;
import se.softhouse.jargo.internal.Texts.UsageTexts;
import se.softhouse.jargo.internal.Texts.UserErrors;

/**
 * Tests implementation details that has no meaning in the public API but can serve other purposes
 * such as to ease debugging. These tests can't reside in the internal package for (obvious)
 * visibility problems. They are mostly for code coverage.
 */
public class PackagePrivateTest
{
	@Test
	public void testArgumentToString()
	{
		assertThat(integerArgument().build().toString()).isEqualTo("<integer>");
		assertThat(integerArgument("-n").build().toString()).isEqualTo("-n");
	}

	@Test
	public void testArgumentBuilderToString()
	{
		assertThat(integerArgument("-i").description("foo").metaDescription("bar").toString())
				.isEqualTo("ArgumentBuilder{names=[-i], description=foo, metaDescription=Optional[bar], hideFromUsage=false"
						+ ", ignoreCase=false, limiter=ALWAYS_TRUE, required=false, separator= , defaultValueDescriber=NumberDescriber, defaultValueSupplier=null, internalStringParser=null}");
	}

	@Test
	public void testParserToString()
	{
		assertThat(integerArgument().internalParser().toString()).isEqualTo("-2,147,483,648 to 2,147,483,647");
	}

	@Test
	public void testProgramInformationToString()
	{
		assertThat(withProgramName("name").programDescription("description").toString()).isEqualTo("name:" + NEWLINE + "description" + NEWLINE);
	}

	@Test
	public void testCommandLineParserToString() throws ArgumentException
	{
		CommandLineParser parser = CommandLineParser.withArguments(integerArgument().build());
		assertThat(parser.toString()).startsWith("Usage: ");
		assertThat(parser.parse().toString()).isEqualTo("{}");
	}

	@Test
	public void testCommandLineParserInstanceToString()
	{
		CommandLineParserInstance parser = CommandLineParser.withArguments(integerArgument().build()).parser();
		assertThat(parser.toString()).startsWith("Usage: ");
	}

	@Test
	public void testArgumentIteratorToString()
	{
		assertThat(ArgumentIterator.forArguments(Arrays.asList("foobar"), Collections.<String, Argument<?>>emptyMap()).toString())
				.isEqualTo("[foobar]");
	}

	@Test
	public void testNumberTypeToString()
	{
		assertThat(NumberType.INTEGER.toString()).isEqualTo("integer");
	}

	@Test
	public void testCommandToString()
	{
		Build command = new Build();
		assertThat(command.toString()).isEqualTo(command.commandName());
	}

	@Test
	public void testUsageToString()
	{
		assertThat(new Usage(Collections.<Argument<?>>emptySet(), Locale.getDefault(), withProgramName("Program"), false).toString())
				.isEqualTo("Usage: Program" + NEWLINE);
	}

	@Test
	public void testThatOptionalArgumentDefaultsToTheGivenValue()
	{
		assertThat(optionParser(true).defaultValue()).isTrue();
		assertThat(optionParser(false).defaultValue()).isFalse();
	}

	@Test
	public void testOtherwiseUnusedMethodsForTransformerParser() throws Exception
	{
		Argument<String> arg = stringArgument("-s").build();
		StringParserBridge<String> parser = new StringParserBridge<String>(StringParsers.stringParser());
		TransformingParser<String, String> transformingParser = new TransformingParser<>(parser, Function.identity(), Predicates2.alwaysTrue());
		assertThat(transformingParser.defaultValue()).isEqualTo("");
		assertThat(transformingParser.metaDescription(arg)).isEqualTo("<string>");
	}

	@Test
	public void testThatUsageReferenceCanOnlyBeSetOneTime()
	{
		ArgumentException e = ArgumentExceptions.withMessage("");
		e = e.withUsageReference(integerArgument().build());
		e = e.withUsageReference(stringArgument().build());
		e.withUsage(new Usage(Collections.<Argument<?>>emptyList(), Locale.US, ProgramInformation.AUTO, false));
		assertThat(e.getMessageAndUsage()).as("string should not be able to override integer")
				.startsWith(String.format(UsageTexts.USAGE_REFERENCE, "<integer>"));
	}

	@Test
	public void testThatUtilityClassDesignIsCorrect() throws IOException
	{
		testUtilityClassDesignForAllClassesAround(Argument.class);
		testUtilityClassDesign(UserErrors.class, UsageTexts.class, ProgrammaticErrors.class);
	}

	@Test
	public void testValueOfAndToStringForEnums()
	{
		EnumTester.testThatToStringIsCompatibleWithValueOfRegardlessOfVisibility(StringStringParser.class);
		EnumTester.testThatToStringIsCompatibleWithValueOfRegardlessOfVisibility(ParameterArity.class);
	}

	@Test
	public void testRowToString() throws Exception
	{
		assertThat(new Row().toString()).isEqualTo(" ");
	}
}
