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

import static java.lang.Math.max;
import static java.util.Collections.unmodifiableList;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toList;
import static se.softhouse.common.strings.StringsUtil.NEWLINE;
import static se.softhouse.common.strings.StringsUtil.repeat;
import static se.softhouse.jargo.Argument.IS_INDEXED;
import static se.softhouse.jargo.Argument.IS_OF_VARIABLE_ARITY;
import static se.softhouse.jargo.Argument.IS_VISIBLE;

import java.io.IOException;
import java.io.PrintStream;
import java.io.Serializable;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import javax.annotation.concurrent.NotThreadSafe;

import se.softhouse.common.strings.Lines;
import se.softhouse.common.strings.StringBuilders;
import se.softhouse.jargo.internal.Texts.UsageTexts;

/**
 * Responsible for formatting usage texts for {@link CommandLineParser#usage()} and
 * {@link Arguments#helpArgument(String, String...)}.
 * Using it is often as simple as:
 *
 * <pre class="prettyprint">
 * <code class="language-java">
 * System.out.print(CommandLineParser.withArguments(someArguments).usage());
 * </code>
 * </pre>
 *
 * Sorts {@link Argument}s in the following order:
 * <ol>
 * <li>{@link ArgumentBuilder#names(String...) indexed arguments} without a
 * {@link ArgumentBuilder#variableArity() variable arity}</li>
 * <li>By their {@link ArgumentBuilder#names(String...) first name} in an alphabetical order</li>
 * <li>The remaining {@link ArgumentBuilder#names(String...) indexed arguments} that are of
 * {@link ArgumentBuilder#variableArity() variable arity}</li>
 * </ol>
 */
@NotThreadSafe
public final class Usage implements Serializable
{
	private static final long serialVersionUID = 1L;

	/**
	 * Corresponds to usage for a single {@link Argument}
	 */
	static final class Row
	{
		private final StringBuilder nameColumn = new StringBuilder();
		private final StringBuilder descriptionColumn = new StringBuilder();

		@Override
		public String toString()
		{
			return nameColumn.toString() + " " + descriptionColumn.toString();
		}
	}

	private final transient Collection<Argument<?>> unfilteredArguments;
	private transient String errorMessage = "";
	private final transient Locale locale;
	private final transient ProgramInformation program;
	private final transient boolean forCommand;
	// TODO(jontejj): try getting the correct value automatically, if not possible fall back to 80
	private transient int columnWidth = 80;

	private transient String fromSerializedUsage = null;

	/**
	 * <pre>
	 * For:
	 * -l, --enable-logging Output debug information to standard out
	 * -p, --listen-port    The port clients should connect to.
	 *
	 * This would be 20.
	 * </pre>
	 */
	private transient int indexOfDescriptionColumn;
	private transient List<Argument<?>> argumentsToPrint;

	Usage(Collection<Argument<?>> arguments, Locale locale, ProgramInformation program, boolean forCommand)
	{
		this.unfilteredArguments = arguments;
		this.locale = locale;
		this.program = program;
		this.forCommand = forCommand;
	}

	Usage(CommandLineParserInstance parser)
	{
		this.unfilteredArguments = parser.allArguments();
		this.locale = parser.locale();
		this.program = parser.programInformation();
		this.forCommand = parser.isCommandParser();
	}

	private Usage(String fromSerializedUsage)
	{
		// All these are unused as the usage is already constructed
		this(null, null, null, false);
		this.fromSerializedUsage = fromSerializedUsage;
	}

	/**
	 * An optional errorMessage to print before any usage
	 */
	Usage withErrorMessage(String message)
	{
		this.errorMessage = requireNonNull(message);
		return this;
	}

	/**
	 * Returns the usage text that's suitable to print on {@link System#out}.
	 */
	@Override
	public String toString()
	{
		return usage();
	}

	private String usage()
	{
		if(fromSerializedUsage != null)
			return fromSerializedUsage;

		init();
		StringBuilder builder = newStringBuilder();

		printOn(builder);

		return builder.toString();
	}

	/**
	 * Appends usage to {@code target}. An alternative to {@link #toString()} when the usage is very
	 * large and needs to be flushed from time to time.
	 *
	 * @throws IOException If an I/O error occurs while appending usage
	 */
	public void printOn(Appendable target) throws IOException
	{
		if(fromSerializedUsage != null)
		{
			target.append(fromSerializedUsage);
		}
		else
		{
			init();
			appendUsageTo(target);
		}
	}

	/**
	 * Appends usage to {@code target}, just like {@link #printOn(Appendable)} but for a
	 * {@link PrintStream} instead. The reason for this overload is that {@link Appendable} declares
	 * {@link IOException} to be thrown while {@link PrintStream} handles it internally through the
	 * use of the {@link PrintStream#checkError()} method.
	 *
	 * @param target a {@link PrintStream} such as {@link System#out} or {@link System#err}.
	 */
	public void printOn(PrintStream target)
	{
		try
		{
			printOn((Appendable) target);
		}
		catch(IOException impossible)
		{
			throw new AssertionError(impossible);
		}
	}

	/**
	 * Appends usage to {@code target}, just like {@link #printOn(Appendable)} but for a
	 * {@link StringBuilder} instead
	 */
	public void printOn(StringBuilder target)
	{
		try
		{
			printOn((Appendable) target);
		}
		catch(IOException impossible)
		{
			throw new AssertionError(impossible);
		}
	}

	private void appendUsageTo(Appendable builder) throws IOException
	{
		builder.append(errorMessage);
		builder.append(header());

		for(Argument<?> arg : argumentsToPrint)
		{
			Row forArgument = usageForArgument(arg);
			appendRowTo(forArgument, builder);
		}
	}

	private String header()
	{
		if(forCommand) // Commands get their header from their meta description
			return hasArguments() ? NEWLINE : "";

		String mainUsage = UsageTexts.USAGE_HEADER + program.programName();

		if(hasArguments())
		{
			mainUsage += UsageTexts.ARGUMENT_INDICATOR;
		}

		mainUsage += NEWLINE + Lines.wrap(program.programDescription(), columnWidth, locale);

		if(hasArguments())
		{
			mainUsage += NEWLINE + UsageTexts.ARGUMENT_HEADER + ":" + NEWLINE;
		}

		return mainUsage;
	}

	private static final int SPACES_BETWEEN_COLUMNS = 4;

	private void init()
	{
		// The lack of synchronization is deliberate, repeated invocations will result in the
		// same variables anyway
		if(argumentsToPrint == null)
		{
			this.argumentsToPrint = sortedArguments();
			this.indexOfDescriptionColumn = determineLongestNameColumn() + SPACES_BETWEEN_COLUMNS;
		}
	}

	private List<Argument<?>> sortedArguments()
	{
		Stream<Argument<?>> indexedWithoutVariableArity = unfilteredArguments.stream()
				.filter(IS_VISIBLE.and(IS_INDEXED).and(IS_OF_VARIABLE_ARITY.negate()));
		Stream<Argument<?>> indexedWithVariableArity = unfilteredArguments.stream().filter(IS_VISIBLE.and(IS_INDEXED).and(IS_OF_VARIABLE_ARITY));

		Stream<Argument<?>> sortedArgumentsByName = unfilteredArguments.stream().filter(IS_VISIBLE.and(IS_INDEXED.negate()))
				.sorted(Argument.NAME_COMPARATOR);

		Stream<Argument<?>> result = Stream.of(indexedWithoutVariableArity, sortedArgumentsByName, indexedWithVariableArity).flatMap(x -> x);
		return unmodifiableList(result.collect(toList()));
	}

	private int determineLongestNameColumn()
	{
		int longestNameSoFar = 0;
		for(Argument<?> arg : argumentsToPrint)
		{
			longestNameSoFar = max(longestNameSoFar, lengthOfNameColumn(arg));
		}
		return Math.min(longestNameSoFar, maxNameColumnWidth());
	}

	private int lengthOfNameColumn(final Argument<?> argument)
	{
		int namesLength = 0;

		for(String name : argument.names())
		{
			namesLength += name.length();
		}
		int separatorLength = max(0, UsageTexts.NAME_SEPARATOR.length() * (argument.names().size() - 1));

		int metaLength = argument.metaDescriptionInLeftColumn().length();

		return namesLength + separatorLength + metaLength;
	}

	/**
	 * A minimum of 1 third must be available to print descriptions on
	 */
	private int maxNameColumnWidth()
	{
		return columnWidth / 3 * 2;
	}

	private StringBuilder newStringBuilder()
	{
		// Two lines for each argument
		return StringBuilders.withExpectedSize(2 * argumentsToPrint.size() * columnWidth);
	}

	private boolean hasArguments()
	{
		return !argumentsToPrint.isEmpty();
	}

	/**
	 * <pre>
	 * Left column  | Right column:
	 * name &lt;meta&gt; 	description of what the argument means [indicators]
	 *                &lt;meta&gt;: description of valid values
	 *                Default: default value
	 *
	 * For instance:
	 * -foo &lt;integer&gt;		Foo something [Required]
	 *                      	&lt;integer&gt;: 1 to 5
	 *
	 * -bar			Bar something
	 * 			Default: 0
	 * </pre>
	 */
	private Row usageForArgument(final Argument<?> arg)
	{
		Row row = new Row();

		row.nameColumn.append(String.join(UsageTexts.NAME_SEPARATOR, arg.names()));
		row.nameColumn.append(arg.metaDescriptionInLeftColumn());

		String description = arg.description();
		if(!description.isEmpty())
		{
			row.descriptionColumn.append(Lines.wrap(description, indexOfDescriptionColumn, columnWidth, locale));
			addIndicators(arg, row.descriptionColumn);
			row.descriptionColumn.append(NEWLINE);
			valueExplanation(arg, row.descriptionColumn);
		}
		else
		{
			valueExplanation(arg, row.descriptionColumn);
			addIndicators(arg, row.descriptionColumn);
		}
		return row;
	}

	private <T> void addIndicators(final Argument<T> arg, StringBuilder target)
	{
		if(arg.isRequired())
		{
			target.append(UsageTexts.REQUIRED);
		}
		if(arg.isAllowedToRepeat())
		{
			target.append(UsageTexts.ALLOWS_REPETITIONS);
		}
	}

	private <T> void valueExplanation(final Argument<T> arg, StringBuilder target)
	{
		String validValuesDescription = arg.descriptionOfValidValues(locale);
		if(!validValuesDescription.isEmpty())
		{
			String meta = arg.metaDescriptionInRightColumn();
			target.append(meta + ": ").append(validValuesDescription);
		}
		if(arg.isRequired())
			return;

		String descriptionOfDefaultValue = arg.defaultValueDescription(locale);
		if(descriptionOfDefaultValue != null)
		{
			if(!validValuesDescription.isEmpty())
			{
				target.append(NEWLINE);
			}
			String spaces = repeat(" ", UsageTexts.DEFAULT_VALUE_START.length());
			descriptionOfDefaultValue = descriptionOfDefaultValue.replace(NEWLINE, NEWLINE + spaces);
			target.append(UsageTexts.DEFAULT_VALUE_START).append(descriptionOfDefaultValue);
		}
	}

	private static final Pattern BY_NEWLINE = Pattern.compile(NEWLINE);

	private void appendRowTo(Row row, Appendable target) throws IOException
	{

		StringBuilder nameColumn = Lines.wrap(row.nameColumn, indexOfDescriptionColumn, locale);
		String descriptionColumn = row.descriptionColumn.toString();
		String[] nameLines = BY_NEWLINE.split(nameColumn);
		Iterator<String> descriptionLines = BY_NEWLINE.splitAsStream(descriptionColumn).iterator();
		for(String nameLine : nameLines)
		{
			target.append(nameLine);
			if(descriptionLines.hasNext())
			{
				int lengthOfNameColumn = nameLine.length();
				int paddingWidth = Math.max(1, indexOfDescriptionColumn - lengthOfNameColumn);
				target.append(repeat(" ", paddingWidth));
				target.append(descriptionLines.next());
			}
			target.append(NEWLINE);
		}
		while(descriptionLines.hasNext())
		{
			target.append(repeat(" ", indexOfDescriptionColumn));
			target.append(descriptionLines.next());
			target.append(NEWLINE);
		}
	}

	private static final class SerializationProxy implements Serializable
	{
		/**
		 * @serial all arguments described. Constructed lazily when serialized.
		 */
		private final String serializedUsage;

		private static final long serialVersionUID = 1L;

		private SerializationProxy(Usage usage)
		{
			// TODO(jontejj): how to support calling withConsoleWidth() after serialization? Some
			// kind of marker where the break iterator did something?
			serializedUsage = usage.usage();
		}

		private Object readResolve()
		{
			return new Usage(serializedUsage);
		}
	}

	private Object writeReplace()
	{
		return new SerializationProxy(this);
	}
}
