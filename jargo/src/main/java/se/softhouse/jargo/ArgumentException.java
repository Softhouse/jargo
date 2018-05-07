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

import static java.util.Objects.requireNonNull;
import static se.softhouse.common.strings.StringsUtil.NEWLINE;

import java.io.Serializable;

import javax.annotation.concurrent.NotThreadSafe;

import se.softhouse.common.strings.StringsUtil;
import se.softhouse.jargo.internal.Texts.ProgrammaticErrors;
import se.softhouse.jargo.internal.Texts.UsageTexts;

/**
 * Indicates that something went wrong in a {@link CommandLineParser#parse(String...) parsing}.
 * Typical causes include:
 * <ul>
 * <li>Missing parameters</li>
 * <li>Unknown arguments, if it's {@link StringsUtil#closestMatches(String, Iterable, int) close
 * enough} to a known argument the error message will contain suggestions</li>
 * <li>Missing required arguments</li>
 * <li>Invalid arguments, thrown from {@link StringParser#parse(String, java.util.Locale)
 * parse}</li>
 * <li>Repetition of argument that hasn't specified {@link ArgumentBuilder#repeated() repeated}</li>
 * </ul>
 * The typical remedy action is to present {@link #getMessageAndUsage()} to the user so he is
 * informed about what he did wrong. <br>
 * As all {@link Exception}s should be, {@link ArgumentException} is {@link Serializable} so usage
 * is available after deserialization.
 */
@NotThreadSafe
public abstract class ArgumentException extends RuntimeException
{
	/**
	 * The {@link Usage} explaining how to avoid this exception
	 */
	private Usage usage = null;

	/**
	 * The used name, one of the strings passed to {@link ArgumentBuilder#names(String...)}
	 */
	private String usedArgumentName = null;

	/**
	 * The first name passed to {@link ArgumentBuilder#names(String...)}. Used to print a reference
	 * to the explanation of the erroneous argument. Used instead of usedArgumentName as the first
	 * name is the leftmost in the listings.
	 */
	private String usageReferenceName = null;

	protected ArgumentException()
	{
	}

	/**
	 * Alias for {@link #initCause(Throwable)}.<br>
	 * Added simply because {@code withMessage("Message").andCause(exception)} flows better.
	 */
	public final ArgumentException andCause(Throwable cause)
	{
		initCause(requireNonNull(cause));
		return this;
	}

	/**
	 * Returns usage detailing how to use the {@link CommandLineParser} that caused this
	 * {@link ArgumentException exception}, prepended with an error message detailing the erroneous
	 * argument and, if applicable, a reference to the usage where the user can read about
	 * acceptable input.
	 */
	public final Usage getMessageAndUsage()
	{
		// TODO(jontejj): jack into the uncaughtExceptionHandler and remove stacktraces? Potentially
		// very annoying feature...
		String message = getMessage(usedArgumentName) + usageReference() + NEWLINE + NEWLINE;
		return getUsage().withErrorMessage(message);
	}

	/**
	 * Returns why this exception occurred.
	 * 
	 * @param referenceName the argument name that was used on the command line.
	 *            If indexed the {@link ArgumentBuilder#metaDescription(String)} is given.
	 *            Furthermore if the argument is both indexed and part of a {@link Command} the
	 *            command name used to trigger the command is given. Alas never null.
	 */
	protected abstract String getMessage(String referenceName);

	/**
	 * <pre>
	 * {@inheritDoc}
	 * 
	 * Marked as final as the {@link #getMessage(String)} should be implemented instead
	 * </pre>
	 * 
	 * @see #getMessageAndUsage()
	 */
	@Override
	public final String getMessage()
	{
		return getMessage(usedArgumentName);
	}

	/**
	 * Returns the usage explaining how to use the {@link CommandLineParser} that
	 * caused this exception.
	 */
	private Usage getUsage()
	{
		requireNonNull(usage, ProgrammaticErrors.NO_USAGE_AVAILABLE);
		return usage;
	}

	final ArgumentException withUsage(final Usage theUsage)
	{
		if(usage != null)
			return this; // Don't overwrite if the user has specified a custom Usage
		usage = theUsage;
		return this;
	}

	final ArgumentException withUsedArgumentName(String argumentNameThatTriggeredMe)
	{
		usedArgumentName = argumentNameThatTriggeredMe;
		return this;
	}

	/**
	 * By default ". See usage for --author for proper values." is appended to the end of the error
	 * message if {@link #getMessageAndUsage()} is used. This method overrides that to be
	 * {@code usageReference} instead.
	 */
	void withUsageReference(final String usageReference)
	{
		usageReferenceName = requireNonNull(usageReference);
	}

	ArgumentException withUsageReference(final Argument<?> usageReference)
	{
		if(usageReferenceName != null)
			return this; // Don't overwrite if the user has specified a custom reference
		usageReferenceName = String.format(UsageTexts.USAGE_REFERENCE, usageReference);
		return this;
	}

	private String usageReference()
	{
		return usageReferenceName != null ? usageReferenceName : "";
	}

	/**
	 * For {@link Serializable}
	 */
	private static final long serialVersionUID = 1L;
}
