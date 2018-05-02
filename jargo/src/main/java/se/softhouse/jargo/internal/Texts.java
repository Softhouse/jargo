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
package se.softhouse.jargo.internal;

import se.softhouse.common.strings.StringsUtil;
import se.softhouse.jargo.Argument;
import se.softhouse.jargo.ArgumentBuilder;
import se.softhouse.jargo.ArgumentException;
import se.softhouse.jargo.Command;
import se.softhouse.jargo.CommandLineParser;
import se.softhouse.jargo.StringParser;
import se.softhouse.jargo.StringParsers;

import static se.softhouse.common.strings.StringsUtil.NEWLINE;
import static se.softhouse.common.strings.StringsUtil.TAB;

/**
 * Contains {@link String#format(String, Object...)} ready strings.
 */
public final class Texts
{
	private Texts()
	{
	}

	/**
	 * Texts visible in usage texts printed with {@link CommandLineParser#usage()}
	 */
	public static final class UsageTexts
	{
		private UsageTexts()
		{
		}

		public static final String USAGE_HEADER = "Usage: ";
		public static final String ARGUMENT_INDICATOR = " [Arguments]";
		public static final String ARGUMENT_HEADER = "Arguments";

		public static final String ALLOWS_REPETITIONS = " [Supports Multiple occurrences]";
		public static final String REQUIRED = " [Required]";
		/**
		 * The characters that, for arguments with several names, separates the different names
		 */
		public static final String NAME_SEPARATOR = ", ";
		public static final String DEFAULT_VALUE_START = "Default: ";

		/**
		 * <pre>
		 * Parameter %s = the argument name to refer to in the usage
		 * 
		 * For instance: ". See usage for --author for proper values."
		 * 
		 * Used by {@link ArgumentException#getMessageAndUsage()} to guide
		 * the user to the correct argument from an error message.
		 * </pre>
		 */
		public static final String USAGE_REFERENCE = ". See usage for %s for proper values.";

		/**
		 * Indicator from the user that all arguments after this should be treated as
		 * {@link ArgumentBuilder#names(String...) indexed arguments}.
		 */
		public static final String END_OF_OPTIONS = "--";

		/**
		 * Indicates that if a file called with what comes after the @ sign exists, arguments should
		 * be read from it (in {@link StringsUtil#UTF8}) before continuing on with the parsing.
		 */
		public static final String FILE_REFERENCE_PREFIX = "@";
	}

	/**
	 * Error texts for user errors
	 */
	public static final class UserErrors
	{
		private UserErrors()
		{
		}

		/**
		 * <pre>
		 * Parameters
		 * 1st %s = an unknown argument
		 * 2nd %s = the closest matches for the unknown argument from the valid arguments
		 * 
		 * For instance: "Didn't expect stats, did you mean one of these?
		 * 			status"
		 * 
		 * Used by {@link CommandLineParser#parse(String...)}.
		 * </pre>
		 */
		public static final String SUGGESTION = "Didn't expect %s, did you mean one of these?" + NEWLINE + TAB + "%s";

		/**
		 * <pre>
		 * Parameters
		 * 1st %s = positional string specifying which of the parameters that is the first that is missing
		 * 2nd %s = meta description for the parameter
		 * 3rd %s = argument name that is expecting the parameter
		 * 
		 * For instance: "Missing second &lt;integer&gt; parameter for -n"
		 * 
		 * Used by {@link ArgumentBuilder#arity(int)}.
		 * </pre>
		 */
		public static final String MISSING_NTH_PARAMETER = "Missing %s %s parameter for %s";

		/**
		 * <pre>
		 * Parameters
		 * 1nd %s = meta description for the parameter, typically {@link StringParser#metaDescription()}
		 * 2nd %s = argument name that is expecting the parameter
		 * 
		 * For instance: "Missing &lt;integer&gt; parameter for -n"
		 * 
		 * Used by {@link StringParser}.
		 * </pre>
		 */
		public static final String MISSING_PARAMETER = "Missing %s parameter for %s";

		/**
		 * <pre>
		 * Parameter %s = a list of all the arguments that is missing
		 * 
		 * For instance: "Missing required arguments: [-n, -a]
		 * 
		 * Used by {@link ArgumentBuilder#required()}.
		 * </pre>
		 */
		public static final String MISSING_REQUIRED_ARGUMENTS = "Missing required arguments: %s";

		/**
		 * <pre>
		 * Parameters
		 * 1st %s = the name of the command that is missing arguments
		 * 2nd %s = a list of all the arguments that the command is missing
		 * 
		 * For instance: "Missing required parameters for commit: [--author]"
		 * 
		 * Used by {@link Command}s that are missing {@link ArgumentBuilder#required()} {@link Argument}s.
		 * </pre>
		 */
		public static final String MISSING_COMMAND_ARGUMENTS = "Missing required arguments for %s: %s";

		/**
		 * <pre>
		 * Parameter %s = the argument name that was used the second time the argument was given
		 * 
		 * For instance: "Non-allowed repetition of the argument --author"
		 * 
		 * Used when {@link ArgumentBuilder#repeated()} hasn't been specified but the user repeats
		 * the argument anyway.
		 * </pre>
		 */
		public static final String DISALLOWED_REPETITION = "Non-allowed repetition of the argument %s";

		/**
		 * <pre>
		 * Parameters
		 * 1st %s the disallowed value
		 * 2nd %s description of valid values
		 * For instance: "'5' is not between 1 and 4" (where 1st %s = 5, 2nd %s = between 1 and 4)
		 */
		public static final String DISALLOWED_VALUE = "'%s' is not %s";

		public static final String DISALLOWED_PROPERTY_VALUE = "Invalid value for key '%s': '%s' is not %s";

		public static final String INVALID_CHAR = "'%s' is not a valid character";

		/**
		 * <pre>
		 * Parameters
		 * 1st %s = the received value
		 * 2nd %s = a list of the acceptable enum values
		 * 
		 * For instance: "'break' is not a valid Option, Expecting one of {start | stop | restart}"
		 * 
		 * Used by {@link StringParsers#enumParser(Class)}
		 * </pre>
		 */
		public static final String INVALID_ENUM_VALUE = "'%s' is not a valid Option, Expecting one of %s";

		/**
		 * <pre>
		 * Parameters
		 * 1st %s = the property identifier (-D) (or argument name as it's usually called)
		 * 2nd %s = the received value (property.name-property.value)
		 * 3rd %s = the expected {@link ArgumentBuilder#separator(String)} (=)
		 * 
		 * For instance: "'-Dproperty.name-property.value' is missing an assignment operator (=)"
		 * 
		 * Used by {@link ArgumentBuilder#asPropertyMap()}.
		 * </pre>
		 */
		public static final String MISSING_KEY_VALUE_SEPARATOR = "'%s%s' is missing an assignment operator(%s)";

		/**
		 * <pre>
		 * Parameters
		 * %s = the unknown argument
		 * </pre>
		 */
		public static final String UNKNOWN_ARGUMENT = "'%s' is not a supported argument/option/command";
	}

	/**
	 * Error texts for programmatic errors
	 */
	public static final class ProgrammaticErrors
	{
		private ProgrammaticErrors()
		{
		}

		/**
		 * Parameter %s = the non-unique meta description
		 */
		public static final String UNIQUE_METAS = "Several required & indexed arguments have the same meta description (%s). "
				+ "That's not allowed because if one of them were left out the user would be confused about which of them he forgot.";

		/**
		 * <pre>
		 * Parameters
		 * 1st %s = index of the first optional argument
		 * 2nd %s = index of the first required argument
		 */
		public static final String REQUIRED_ARGUMENTS_BEFORE_OPTIONAL = "Argument given at index %s is optional but argument at index %s"
				+ " is required. Required arguments must be given before any optional arguments, at least when they are indexed (without names)";

		/**
		 * Parameter %s = a list with all parameters that is configured with
		 * {@link ArgumentBuilder#variableArity()}
		 */
		public static final String SEVERAL_VARIABLE_ARITY_PARSERS = "Several unnamed arguments are configured to receive a variable arity of parameters: %s";
		/**
		 * Parameter %s = a name that would cause ambiguous parsing
		 */
		public static final String NAME_COLLISION = "%s is handled by several arguments";

		/**
		 * Parameter %s = a description of an argument that was passed twice to
		 * {@link CommandLineParser#withArguments(se.softhouse.jargo.Argument...)}
		 */
		public static final String UNIQUE_ARGUMENT = "%s handles the same argument twice";

		/**
		 * Parameter %s = why the default value is invalid
		 */
		public static final String INVALID_DEFAULT_VALUE = "Invalid default value: %s";

		/**
		 * Parameter %s = the requested arity
		 */
		public static final String TO_SMALL_ARITY = "Arity requires at least 2 parameters (got %s)";

		public static final String OPTIONS_REQUIRES_AT_LEAST_ONE_NAME = "An option requires at least one name, otherwise it wouldn't be useful";
		public static final String OPTION_DOES_NOT_ALLOW_NULL_AS_DEFAULT = "Null is not allowed as a default value for an option argument. An option is either given or it's not. ";

		public static final String NO_NAME_FOR_PROPERTY_MAP = "No leading identifier (otherwise called names), for example -D, specified for property map. Call names(...) to provide it.";

		public static final String EMPTY_SEPARATOR = "In a key=value pair a separator of at least one character is required";

		public static final String INVALID_META_DESCRIPTION = "a meta description can't be empty";

		public static final String DEFAULT_VALUE_AND_REQUIRED = "Having a requried argument and a default value makes no sense";
		public static final String NO_USAGE_AVAILABLE = "No originParser set for ArgumentException. No usage available.";

		/**
		 * Parameter %s = the illegal argument that the {@link CommandLineParser} wasn't
		 * configured to handle
		 */
		public static final String ILLEGAL_ARGUMENT = "%s was not found in this result at all. Did you perhaps forget to add it to withArguments(...)?";
	}
}
