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
import static org.fest.assertions.Fail.fail;
import static org.fest.assertions.Fail.failure;
import static se.softhouse.common.strings.StringsUtil.NEWLINE;
import static se.softhouse.common.strings.StringsUtil.TAB;
import static se.softhouse.common.testlib.Thrower.asUnchecked;
import static se.softhouse.jargo.Arguments.integerArgument;
import static se.softhouse.jargo.Arguments.stringArgument;
import static se.softhouse.jargo.Arguments.withParser;
import static se.softhouse.jargo.utils.Assertions2.assertThat;
import static se.softhouse.jargo.utils.ExpectedTexts.expected;

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.StringWriter;
import java.util.List;
import java.util.Locale;

import org.junit.Test;

import se.softhouse.common.classes.Classes;
import se.softhouse.common.strings.Describable;
import se.softhouse.common.testlib.Serializer;
import se.softhouse.common.testlib.SimulatedException;
import se.softhouse.jargo.ArgumentExceptions.UnexpectedArgumentException;
import se.softhouse.jargo.ForwardingStringParser.SimpleForwardingStringParser;
import se.softhouse.jargo.internal.Texts.ProgrammaticErrors;
import se.softhouse.jargo.internal.Texts.UsageTexts;
import se.softhouse.jargo.internal.Texts.UserErrors;

/**
 * Tests for {@link CommandLineParser#usage()}, {@link Argument#usage()} and
 * {@link ArgumentBuilder#usage()}
 *
 * @formatter:off
 */
public class UsageTest
{
	@Test
	public void testThatProgramNameDefaultsToMainClassName()
	{
		Usage usage = integerArgument("-n").usage();
		assertThat(usage).startsWith(UsageTexts.USAGE_HEADER + Classes.mainClassName());
	}

	@Test
	public void testThatPrintOnFlushesInBetweenEachArgument()
	{
		ByteArrayOutputStream output = new ByteArrayOutputStream();
		PrintStream stream = new PrintStream(output, true);
		try
		{
			Argument<String> willFailWhenPrinted = stringArgument().description(new InvalidDescription()).build();
			CommandLineParser.withArguments(integerArgument().build(), willFailWhenPrinted).usage().printOn(stream);
			fail("Printing faulty descriptions should not work");
		}
		catch(SimulatedException expected)
		{
			String usageSoFar = output.toString();
			// Verifies that the flush was made
			assertThat(usageSoFar).contains("<integer>");
			// Verifies that the usage doesn't contain any arguments that can't be fully printed
			assertThat(usageSoFar).doesNotContain("<string>");
		}
	}

	private static class InvalidDescription implements Describable
	{
		@Override
		public String description()
		{
			throw new SimulatedException();
		}
	}

	@Test
	public void testUsageWithRequiredArguments()
	{
		Usage usage = integerArgument("-n").required().usage();
		assertThat(usage).contains(UsageTexts.REQUIRED);
	}

	@Test
	public void testUsageWithRepeatedArguments()
	{
		Usage usage = stringArgument("-s").repeated().metaDescription("\"greeting phrase\"")
				.description("A greeting phrase to greet new connections with").defaultValueDescription("Nothing").usage();

		assertThat(usage).isEqualTo(expected("repeatedArguments"));
	}

	@Test
	public void testUsageForNoArguments()
	{
		Usage usage = CommandLineParser.withArguments().programName("NoArguments").usage();
		assertThat(usage).isEqualTo("Usage: NoArguments" + NEWLINE);
	}

	@Test
	public void testUsageForNoVisibleArguments()
	{
		Usage usage = CommandLineParser.withArguments(integerArgument().hideFromUsage().build()).programName("NoVisibleArguments").usage();
		assertThat(usage).isEqualTo("Usage: NoVisibleArguments" + NEWLINE);
	}

	@Test
	public void testUsageWithArguments()
	{
		Usage usage = stringArgument().usage();
		assertThat(usage).startsWith(UsageTexts.USAGE_HEADER).contains(UsageTexts.ARGUMENT_INDICATOR).contains(UsageTexts.ARGUMENT_HEADER);
	}

	@Test
	public void testThatHiddenArgumentsAreHidden()
	{
		Argument<String> hiddenArgument = stringArgument("--hidden-argument").hideFromUsage().build();
		Argument<String> visibleArgument = stringArgument("--visible-argument").build();
		CommandLineParser parser = CommandLineParser.withArguments(hiddenArgument, visibleArgument);
		Usage usage = parser.usage();

		assertThat(usage).doesNotContain("--hidden-argument");
		assertThat(usage).contains("--visible-argument");
	}

	@Test
	public void testThatHiddenArgumentsAreParsable() throws ArgumentException
	{
		assertThat(stringArgument("--hidden").hideFromUsage().parse("--hidden", "hello")).isEqualTo("hello");
	}

	@Test
	public void testUsageTextForRepeatedArgumentWithDefaultValueSet()
	{
		Usage usage = integerArgument().defaultValue(1).repeated().usage();
		assertThat(usage).contains(UsageTexts.DEFAULT_VALUE_START + "1").contains(UsageTexts.ALLOWS_REPETITIONS);
	}

	@Test
	public void testArgumentNameSuggestions()
	{
		try
		{
			CommandLineParser
					.withArguments(integerArgument("--name").build(), integerArgument("--number").build(), integerArgument("--nothing").build())
					.parse("--namr");
			fail("--namr should not be a valid argument");
		}
		catch(ArgumentException expected)
		{
			assertThat(expected).hasMessage(String.format(UserErrors.SUGGESTION, "--namr", "--name" + NEWLINE + TAB + "--number"));
		}
	}

	@Test
	public void testThatUsageOnArgumentExceptionThrowsWhenNoUsageIsAvailable()
	{
		try
		{
			throw ArgumentExceptions.withMessage("");
		}
		catch(ArgumentException e)
		{
			try
			{
				e.getMessageAndUsage();
				fail("getMessageAndUsage should throw when not enough information is available to produce a sane usage text");
			}
			catch(NullPointerException expected)
			{
				assertThat(expected).hasMessage(ProgrammaticErrors.NO_USAGE_AVAILABLE);
			}
		}
	}

	@Test
	public void testSortingOrderForIndexedArguments()
	{
		Argument<String> indexOne = stringArgument().description("IndexOne").build();
		Argument<String> indexTwo = stringArgument().description("IndexTwo").build();
		Argument<String> indexThree = stringArgument().description("IndexThree").build();
		Argument<List<String>> variableArity = stringArgument().description("VariableArity").variableArity().build();
		Argument<List<String>> namedVariableArity = stringArgument("-n").description("NamedVariableArity").variableArity().build();
		Argument<String> namedOne = stringArgument("-S").build();
		Argument<String> namedTwo = stringArgument("-T").build();
		Usage usage = CommandLineParser.withArguments(namedOne, namedTwo)//
				.andArguments(variableArity, namedVariableArity) //
				.andArguments(indexOne, indexTwo, indexThree) //
				.usage();

		assertThat(usage).isEqualTo(expected("indexedArgumentsSortingOrder"));
	}

	@Test
	public void testUnexpectedArgument() throws ArgumentException
	{
		try
		{
			integerArgument("--number").parse("--number", "1", "foo");
			fail("foo should cause a throw as it's not handled");
		}
		catch(UnexpectedArgumentException e)
		{

			assertThat(e).hasMessage("Unexpected argument: foo, previous argument: 1");
		}
	}

	@Test
	public void testUnexpectedArgumentWithoutPreviousArgument() throws ArgumentException
	{
		try
		{
			integerArgument("--number").parse("foo");
			fail("foo should cause a throw as it's not handled");
		}
		catch(UnexpectedArgumentException e)
		{
			assertThat(e).hasMessage("Unexpected argument: foo");
		}
	}

	@Test
	public void testProgramDescriptionInUsage()
	{
		Usage usage = CommandLineParser.withArguments().programName("ProgramName").programDescription("Program description of ProgramName").usage();

		assertThat(usage).isEqualTo("Usage: ProgramName" + NEWLINE + NEWLINE + "Program description of ProgramName" + NEWLINE);
	}

	@Test
	public void testThatDescriptionsAreLazilyInitialized()
	{
		Usage usage = integerArgument("-n").description(new Describable(){
			@Override
			public String description()
			{
				return "foo";
			}
		}).usage();
		assertThat(usage).contains("foo");
	}

	@Test
	public void testThatUsageInformationIsLazilyInitialized() throws ArgumentException
	{
		Argument<String> argument = withParser(new FailingMetaDescription()).names("-n").build();
		CommandLineParser parser = CommandLineParser.withArguments(argument);
		parser.usage(); // Should not cause meta description to be called as the usage isn't printed

		assertThat(parser.parse("-n", "foo").get(argument)).isEqualTo("foo");
	}

	private static class FailingMetaDescription extends SimpleForwardingStringParser<String>
	{
		protected FailingMetaDescription()
		{
			super(StringParsers.stringParser());
		}

		@Override
		public String metaDescription()
		{
			throw failure("meta description should not be called unless needed");
		}
	}

	@Test
	public void testThatDescriptionsAreNotLazilyInitializedWhenNotNeeded()
	{
		try
		{
			integerArgument("-n").description(new FailingDescription()).parse("-n", "foo");
			fail("foo should cause a throw as it's an invalid integer");
		}
		catch(ArgumentException expected)
		{
		}
	}

	private static final class FailingDescription implements Describable
	{
		@Override
		public String description()
		{
			throw failure("Describable should not be called as no usage was printed");
		}
	}

	/**
	 * @see Usage
	 */
	@Test
	public void testThatArgumentsAreSortedInLinguisticOrder()
	{
		// Unicode sorts å,ä,ö as ä,å,ö but in swedish it's actually å,ä,ö
		Argument<String> first = stringArgument("-å").build();
		Argument<String> second = stringArgument("-ä").build();
		Argument<String> third = stringArgument("-ö").build();
		assertThat(CommandLineParser.withArguments(first, second, third).usage()).isEqualTo(expected("alphabeticalOrder"));
	}

	/**
	 * Tests long descriptions, names, meta descriptions, default value descriptions, valid value
	 * explanations
	 */
	@Test
	public void testThatLongStringsAreLineBreakedAndPaddedWithSpacesEvenWhenTheyDontHaveNewlines()
	{
		Argument<String> longWindedArgument = withParser(new LongWindedParser()) //
				.description(longTextWithoutNewlines) //
				.defaultValueDescription(longTextWithoutNewlines).build();
		Usage usage = CommandLineParser.withArguments(longWindedArgument).programDescription(longTextWithoutNewlines).usage();
		assertThat(usage).isEqualTo(expected("wordwrapping"));
	}

	@Test
	public void testThatMoreNameRowsThanDescriptionRowsWorks() throws Exception
	{
		String extremelyLongArgumentName = longTextWithoutNewlines.replace(" ", "-");
		Usage usage = integerArgument(extremelyLongArgumentName).usage();
		assertThat(usage).endsWith("-est-laborum. <integer>" + NEWLINE);
	}

	private static final String longTextWithoutNewlines = "Lorem ipsum dolor sit amet, consectetur adipisicing elit, "
			+ "sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. "
			+ "Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. "
			+ "Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. "
			+ "Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum.";

	private static class LongWindedParser extends SimpleForwardingStringParser<String>
	{
		protected LongWindedParser()
		{
			super(StringParsers.stringParser());
		}

		@Override
		public String descriptionOfValidValues(Locale locale)
		{
			return "Not wordwrapped as that could lead to invalid description strings. " + longTextWithoutNewlines;
		}
	}

	@Test
	public void testPrintOnPrintStream()
	{
		ByteArrayOutputStream output = new ByteArrayOutputStream();
		PrintStream stream = new PrintStream(output, true);
		CommandLineParser.withArguments(integerArgument().build()).usage().printOn(stream);
		assertThat(output.toString()).contains("<integer>");
		assertThat(stream.checkError()).isFalse();
	}

	@Test
	public void testPrintOnStringBuilder()
	{
		StringBuilder builder = new StringBuilder();
		CommandLineParser.withArguments(integerArgument().build()).usage().printOn(builder);
		assertThat(builder.toString()).contains("<integer>");
	}

	@Test
	public void testPrintOnAppendable() throws IOException
	{
		Appendable appendable = new StringWriter();
		CommandLineParser.withArguments(integerArgument().build()).usage().printOn(appendable);
		assertThat(appendable.toString()).contains("<integer>");
	}

	@Test
	public void testThatSerializedUsageIsPrintedOnAppendable() throws IOException
	{
		Usage before = CommandLineParser.withArguments(integerArgument().build()).usage();
		Usage after = Serializer.clone(before);

		Appendable appendable = new StringWriter();
		after.printOn(appendable);
		assertThat(appendable.toString()).contains("<integer>");
	}

	@Test
	public void testThatIOExceptionIsPropagatedWhenThrownFromNastyAppendable()
	{
		final IOException thrown = new IOException();
		Appendable nastyAppendable = new BufferedWriter(new StringWriter()){
			@Override
			public BufferedWriter append(CharSequence csq) throws IOException
			{
				throw thrown;
			}
		};
		try
		{
			CommandLineParser.withArguments(integerArgument().build()).usage().printOn(nastyAppendable);
			fail("IOException wasn't propagated correctly");
		}
		catch(IOException propagatedVersion)
		{
			assertThat(propagatedVersion).isSameAs(thrown);
		}
	}

	@Test
	public void testThatIOExceptionIsPropagatedAsAssertionErrorForImpossibleCase_PrintStream()
	{
		final IOException thrown = new IOException();
		PrintStream nastyStream = new PrintStream(new ByteArrayOutputStream()){
			@Override
			public PrintStream append(CharSequence csq)
			{
				throw asUnchecked(thrown);
			}
		};

		try
		{
			CommandLineParser.withArguments(integerArgument().build()).usage().printOn(nastyStream);
			fail("IOException wasn't propagated correctly");
		}
		catch(AssertionError expected)
		{
			assertThat(expected.getCause()).isSameAs(thrown);
		}
	}
}
