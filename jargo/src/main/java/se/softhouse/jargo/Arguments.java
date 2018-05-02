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

import static se.softhouse.common.guavaextensions.Lists2.asList;
import static se.softhouse.common.strings.Describers.booleanAsEnabledDisabled;
import static se.softhouse.common.strings.Describers.characterDescriber;
import static se.softhouse.common.strings.Describers.fileDescriber;
import static se.softhouse.common.strings.Describers.numberDescriber;
import static se.softhouse.jargo.StringParsers.bigDecimalParser;
import static se.softhouse.jargo.StringParsers.bigIntegerParser;
import static se.softhouse.jargo.StringParsers.booleanParser;
import static se.softhouse.jargo.StringParsers.byteParser;
import static se.softhouse.jargo.StringParsers.charParser;
import static se.softhouse.jargo.StringParsers.enumParser;
import static se.softhouse.jargo.StringParsers.fileParser;
import static se.softhouse.jargo.StringParsers.integerParser;
import static se.softhouse.jargo.StringParsers.longParser;
import static se.softhouse.jargo.StringParsers.shortParser;
import static se.softhouse.jargo.StringParsers.stringParser;

import java.io.File;
import java.math.BigDecimal;
import java.math.BigInteger;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import javax.annotation.concurrent.Immutable;

import se.softhouse.common.strings.Describer;
import se.softhouse.common.strings.Describers;
import se.softhouse.jargo.ArgumentBuilder.CommandBuilder;
import se.softhouse.jargo.ArgumentBuilder.DefaultArgumentBuilder;
import se.softhouse.jargo.ArgumentBuilder.OptionArgumentBuilder;
import se.softhouse.jargo.ArgumentBuilder.SimpleArgumentBuilder;
import se.softhouse.jargo.StringParsers.HelpParser;

/**
 * <pre>
 * Used as a starting point to create {@link Argument} instances.
 *
 * The produced arguments can be passed to
 * {@link CommandLineParser#withArguments(Argument...)} to group several
 * {@link Argument}s together. If only one argument should be
 * parsed, {@link ArgumentBuilder#parse(String...)} can be used instead.
 * </pre>
 */
@Immutable
public final class Arguments
{
	private Arguments()
	{
	}

	/**
	 * <pre>
	 * Makes the {@link CommandLineParser} it's passed to throw a helpful {@link ArgumentException}
	 * with usage explaining either the full program or a specific {@link Argument}/{@link Command}.
	 * The resulting {@link Usage} is currently only accessible through the {@link ArgumentException#getMessageAndUsage()} method.
	 * This suits most use-cases as there's usually a catch block for {@link ArgumentException} anyway where the {@link Usage} is printed
	 * </pre>
	 * 
	 * Supported usages:
	 * <ul>
	 * <li>"program -h" - Gives {@link Usage} for the whole program</li>
	 * <li>"program -h --number" Gives {@link Usage} for the "--number"{@link Argument}</li>
	 * <li>"program commit -h" Gives {@link Usage} for the <code>commit</code> {@link Command}</li>
	 * <li>"program -h commit" Also gives {@link Usage} for the <code>commit</code>
	 * {@link Command}</li>
	 * <li>"program commit -h --amend" Gives {@link Usage} for the <code>--amend</code>
	 * {@link Argument}
	 * for the <code>commit</code> {@link Command}</li>
	 * </ul>
	 * If given an unknown argument name, an {@link ArgumentException} is thrown.
	 *
	 * @param mandatoryName "-h" in the above example
	 * @param optionalNames for example "--help"
	 * @return an {@link Argument} that can be passed to
	 *         {@link CommandLineParser#withArguments(Argument...)}
	 */
	public static Argument<?> helpArgument(final String mandatoryName, final String ... optionalNames)
	{
		return new SimpleArgumentBuilder<String>(HelpParser.INSTANCE).names(asList(mandatoryName, optionalNames)).build();
	}

	/**
	 * Creates an {@link Argument} that uses {@link StringParsers#booleanParser()}
	 *
	 * @param names the {@link ArgumentBuilder#names(String...)} to use
	 */
	@CheckReturnValue
	@Nonnull
	public static DefaultArgumentBuilder<Boolean> booleanArgument(final String ... names)
	{
		return withParser(booleanParser()).names(names);
	}

	/**
	 * Creates an {@link Argument} that uses {@link StringParsers#byteParser()}
	 *
	 * @param names the {@link ArgumentBuilder#names(String...)} to use
	 */
	@CheckReturnValue
	@Nonnull
	public static DefaultArgumentBuilder<Byte> byteArgument(final String ... names)
	{
		return withParser(byteParser()).defaultValueDescriber(numberDescriber()).names(names);
	}

	/**
	 * Creates an {@link Argument} that uses {@link StringParsers#shortParser()}
	 *
	 * @param names the {@link ArgumentBuilder#names(String...)} to use
	 */
	@CheckReturnValue
	@Nonnull
	public static DefaultArgumentBuilder<Short> shortArgument(final String ... names)
	{
		return withParser(shortParser()).defaultValueDescriber(numberDescriber()).names(names);
	}

	/**
	 * Creates an {@link Argument} that uses {@link StringParsers#integerParser()}
	 *
	 * @param names the {@link ArgumentBuilder#names(String...)} to use
	 */
	@CheckReturnValue
	@Nonnull
	public static DefaultArgumentBuilder<Integer> integerArgument(final String ... names)
	{
		return withParser(integerParser()).defaultValueDescriber(numberDescriber()).names(names);
	}

	/**
	 * Creates an {@link Argument} that uses {@link StringParsers#longParser()}
	 *
	 * @param names the {@link ArgumentBuilder#names(String...)} to use
	 */
	@CheckReturnValue
	@Nonnull
	public static DefaultArgumentBuilder<Long> longArgument(final String ... names)
	{
		return withParser(longParser()).defaultValueDescriber(numberDescriber()).names(names);
	}

	/**
	 * Creates an {@link Argument} that uses {@link StringParsers#bigIntegerParser()}
	 *
	 * @param names the {@link ArgumentBuilder#names(String...)} to use
	 */
	@CheckReturnValue
	@Nonnull
	public static DefaultArgumentBuilder<BigInteger> bigIntegerArgument(final String ... names)
	{
		return withParser(bigIntegerParser()).defaultValueDescriber(numberDescriber()).names(names);
	}

	/**
	 * Creates an {@link Argument} that uses {@link StringParsers#bigIntegerParser()}
	 *
	 * @param names the {@link ArgumentBuilder#names(String...)} to use
	 */
	@CheckReturnValue
	@Nonnull
	public static DefaultArgumentBuilder<BigDecimal> bigDecimalArgument(final String ... names)
	{
		return withParser(bigDecimalParser()).defaultValueDescriber(numberDescriber()).names(names);
	}

	/**
	 * Creates an {@link Argument} that uses {@link StringParsers#charParser()}.<br>
	 * Describes default {@link Character}s with {@link Describers#characterDescriber()} in the
	 * usage.
	 *
	 * @param names the {@link ArgumentBuilder#names(String...)} to use
	 */
	@CheckReturnValue
	@Nonnull
	public static DefaultArgumentBuilder<Character> charArgument(final String ... names)
	{
		return withParser(charParser()).defaultValueDescriber(characterDescriber()).names(names);
	}

	/**
	 * Creates an {@link Argument} that uses {@link StringParsers#stringParser()}
	 *
	 * @param names the {@link ArgumentBuilder#names(String...)} to use
	 */
	@CheckReturnValue
	@Nonnull
	public static DefaultArgumentBuilder<String> stringArgument(final String ... names)
	{
		return withParser(stringParser()).names(names);
	}

	/**
	 * Creates an {@link Argument} that uses {@link StringParsers#fileParser()}.<br>
	 * Describes default {@link File}s with {@link Describers#fileDescriber()} in the usage.
	 *
	 * @param names the {@link ArgumentBuilder#names(String...)} to use
	 */
	@CheckReturnValue
	@Nonnull
	public static DefaultArgumentBuilder<File> fileArgument(final String ... names)
	{
		return withParser(fileParser()).defaultValueDescriber(fileDescriber()).names(names);
	}

	/**
	 * <pre>
	 * Creates an optional (flag) {@link Argument} where the sole existence of the name on the command
	 * line matters. This is the only place to have an {@link ArgumentBuilder#arity(int)} of zero.
	 *
	 * The default value is printed with {@link Describers#booleanAsEnabledDisabled()}.
	 * This can be changed with {@link ArgumentBuilder#defaultValueDescriber(Describer)}.
	 *
	 * &#64;see Describers#booleanAsOnOff()
	 *
	 * &#64;param mandatoryName the first name that enables this option
	 * &#64;param optionalNames aliases that also enables this option
	 * </pre>
	 */
	@CheckReturnValue
	@Nonnull
	public static OptionArgumentBuilder optionArgument(final String mandatoryName, final String ... optionalNames)
	{
		return new OptionArgumentBuilder().defaultValueDescriber(booleanAsEnabledDisabled()).names(asList(mandatoryName, optionalNames));
	}

	/**
	 * <pre>
	 * Creates an {@link Argument} that uses an {@link StringParsers#enumParser(Class)} to parse
	 * arguments.
	 *
	 * If you end up with a big switch statement for your enum consider using {@link Command}s
	 * instead.
	 * </pre>
	 *
	 * @param enumToHandle the {@link Enum} to retrieve enum values from
	 * @param names the {@link ArgumentBuilder#names(String...)} to use
	 */
	@CheckReturnValue
	@Nonnull
	public static <T extends Enum<T>> DefaultArgumentBuilder<T> enumArgument(final Class<T> enumToHandle, final String ... names)
	{
		return withParser(enumParser(enumToHandle)).names(names);
	}

	/**
	 * <pre>
	 * Creates a {@link CommandBuilder} for {@code command}. Used for setting up custom things like
	 * {@link ArgumentBuilder#ignoreCase()} for a {@link Command#commandName()}. Typically not needed
	 * as {@link CommandLineParser#andCommands(Command...)} or {@link CommandLineParser#withCommands(Command...)}
	 * can be used.
	 * </pre>
	 */
	@CheckReturnValue
	@Nonnull
	public static CommandBuilder command(final Command command)
	{
		return new CommandBuilder(command).names(command.commandName()).description(command);
	}

	/**
	 * Used to create {@link Argument} instances with a custom {@link StringParser}.
	 * A custom {@link StringParser} is one which isn't available through {@link StringParsers}.
	 * Consider setting {@link ArgumentBuilder#names(String...) names} on the returned
	 * {@link ArgumentBuilder builder}.
	 */
	@CheckReturnValue
	@Nonnull
	public static <T> DefaultArgumentBuilder<T> withParser(final StringParser<T> parser)
	{
		return new DefaultArgumentBuilder<T>(parser);
	}
}
