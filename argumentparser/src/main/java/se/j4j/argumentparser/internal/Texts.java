package se.j4j.argumentparser.internal;

import se.j4j.argumentparser.Argument;
import se.j4j.argumentparser.ArgumentBuilder;
import se.j4j.argumentparser.ArgumentFactory;
import se.j4j.argumentparser.Command;
import se.j4j.argumentparser.CommandLineParser;
import se.j4j.argumentparser.StringParser;

/**
 * Contains {@link String#format(String, Object...)} ready strings.
 */
public final class Texts
{
	private Texts()
	{
	}

	/**
	 * Texts visible in usage texts printed with {@link CommandLineParser#usage(String)}
	 */
	public static final class UsageTexts
	{
		public static final String USAGE_HEADER = "Usage: ";
		public static final String ALLOWS_REPETITIONS = " [Supports Multiple occurrences]";
		public static final String REQUIRED = " [Required]";
		public static final String OPTIONS = " [Options]";
		/**
		 * The characters that, for arguments with several names, separates the different names
		 */
		public static final String NAME_SEPARATOR = ", ";
		public static final String DEFAULT_VALUE_START = "Default: ";
	}

	/**
	 * Error texts for user errors
	 */
	public static final class UserErrors
	{
		// TODO: specify which argument that failed

		/**
		 * <pre>
		 * Parameters
		 * 1st %s = positional string specifying which of the parameters that is the first that is missing
		 * 2nd %s = meta description for the parameter
		 * 3rd %s = argument name that is expecting the parameter
		 * 
		 * For instance: "Missing second &lt;integer&gt; parameter for -n"
		 * 
		 * Used by {@link ArgumentBuilder#arity(int)}
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
		 * Used with {@link StringParser}.
		 * </pre>
		 */
		public static final String MISSING_PARAMETER = "Missing %s parameter for %s";

		/**
		 * <pre>
		 * Parameter %s = a list of all the arguments that is missing
		 * 
		 * For instance: "Missing required arguments: [-n, -a]
		 * 
		 * Used by {@link ArgumentBuilder#required()}
		 * </pre>
		 */
		public static final String MISSING_REQUIRED_ARGUMENTS = "Missing required arguments: %s";

		/**
		 * <pre>
		 * Parameters
		 * 1st %s = the name of the command that is missing arguments
		 * 2nd %s = a list of all the arguments that the command is missing
		 * 
		 * For instance: "Missing required arguments for commit: [--author]"
		 * 
		 * Used by {@link Command}s that's missing {@link ArgumentBuilder#required()} {@link Argument}s
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
		public static final String UNALLOWED_REPETITION = "Non-allowed repetition of the argument %s";

		/**
		 * <pre>
		 * Parameters
		 * 1st %s = the argument name
		 * 2nd %s = the second occurrence of the key=value pair
		 * 
		 * For instance: "'-Dsystem.property.name' was found as a key several times in the input"
		 * 
		 * Used by {@link ArgumentBuilder#asPropertyMap()}
		 * </pre>
		 */
		public static final String UNALLOWED_REPETITION_OF_KEY = "'%s%s' was found as a key several times in the input";

		/**
		 * <pre>
		 * Parameters
		 * 1st %s the unallowed value
		 * 2nd %s description of valid values
		 */
		public static final String UNALLOWED_VALUE = "'%s' is not %s";

		public static final String INVALID_BIG_INTEGER = "'%s' is not a valid big-integer";

		public static final String INVALID_DOUBLE = "'%s' is not a valid double (64-bit IEEE 754 floating point)";

		public static final String INVALID_FLOAT = "'%s' is not a valid float (32-bit IEEE 754 floating point)";

		public static final String INVALID_CHAR = "'%s' is not a valid character";

		/**
		 * <pre>
		 * Parameters
		 * 1st %s = the received value
		 * 2nd %s = a list of the acceptable enum values
		 * 
		 * For instance: "'break' is not a valid Option, Expecting one of [start | stop | restart]"
		 * 
		 * Used by {@link ArgumentFactory#enumArgument(Class, String...)}.
		 * </pre>
		 */
		public static final String INVALID_ENUM_VALUE = "'%s' is not a valid Option, Expecting one of %s";

		/**
		 * <pre>
		 * Parameters
		 * 1st %s = the property identifier (or argument name as it's usually called)
		 * 2nd %s = the received value
		 * 3rd %s = the expected {@link ArgumentBuilder#separator(String)}
		 * 
		 * For instance: "'-Dsystem.property.name-property_value' is missing an assignment operator (=)"
		 * 
		 * Used by {@link ArgumentBuilder#asPropertyMap()}
		 * </pre>
		 */
		public static final String MISSING_KEY_VALUE_SEPARATOR = "'%s%s' is missing an assignment operator(%s)";
	}

	/**
	 * Error texts for programmatic errors
	 */
	public static final class ProgrammaticErrors
	{
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
		 * {@link CommandLineParser#withArguments(se.j4j.argumentparser.Argument...)}
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

		public static final String INVALID_META_DESCRIPTION = "a meta description can't be null/empty";

		public static final String DEFAULT_VALUE_AND_REQUIRED = "Having a requried argument and a default value makes no sense";
		public static final String INVALID_CALL_ORDER = "The %s needs to be set after the %s invocation";
		/**
		 * Parameter %s = the program name that is requesting a usage text
		 */
		public static final String NO_USAGE_AVAILABLE = "No originParser set for ArgumentException. No usage available for %s";

		/**
		 * Parameter %s = the illegal argument that the {@link CommandLineParser} wasn't configured
		 * to
		 * handle
		 */
		public static final String ILLEGAL_ARGUMENT = "%s was not found in this result at all. Did you perhaps forget to add it to withArguments(...)?";
	}
}