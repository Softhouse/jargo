package se.j4j.argumentparser.stringparsers;

import static org.fest.assertions.Assertions.assertThat;
import static se.j4j.argumentparser.ArgumentFactory.optionArgument;

import java.util.Collections;

import org.junit.Ignore;
import org.junit.Test;

import se.j4j.argumentparser.ArgumentException;
import se.j4j.argumentparser.ArgumentFactory;
import se.j4j.argumentparser.internal.Texts.ProgrammaticErrors;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Tests for {@link ArgumentFactory#optionArgument(String, String...)}
 */
public class OptionalArgumentTest
{
	@Test
	public void testThatOptionalArgumentsDefaultsToFalse() throws ArgumentException
	{
		assertThat(optionArgument("-l").parse()).isFalse();
	}

	@Test
	public void testThatOptionalIsTrueWhenArgumentIsGiven() throws ArgumentException
	{
		assertThat(optionArgument("--disable-logging").parse("--disable-logging")).isTrue();
	}

	@Test
	public void testThatOptionalIsFalseWhenArgumentIsGivenAndDefaultIsTrue() throws ArgumentException
	{
		assertThat(optionArgument("--disable-logging").defaultValue(true).parse("--disable-logging")).isFalse();
	}

	@Test
	public void testDescription()
	{
		String usage = optionArgument("--enable-logging").usage();
		assertThat(usage).contains("Default: disabled");
	}

	@Test
	public void testForDefaultTrue() throws ArgumentException
	{
		String usage = optionArgument("--disable-logging").defaultValue(true).usage();
		assertThat(usage).contains("Default: enabled");

		assertThat(optionArgument("--disable-logging").defaultValue(true).parse()).isTrue();
	}

	@Test
	@SuppressFBWarnings(value = "NP_NONNULL_PARAM_VIOLATION", justification = "Checks enforcement of the annotation")
	public void testThatNullIsNotAllowed()
	{
		try
		{
			optionArgument("--enable-logging").defaultValue(null);
		}
		catch(NullPointerException expected)
		{
			assertThat(expected).hasMessage(ProgrammaticErrors.OPTION_DOES_NOT_ALLOW_NULL_AS_DEFAULT);
		}
	}

	@Test(expected = IllegalArgumentException.class)
	public void testThatOptionalArgumentsEnforcesAtLeastOneName()
	{
		optionArgument("-l").names();
	}

	@Test
	public void testThatOptionalArgumentsCanUseAnotherName() throws ArgumentException
	{
		assertThat(optionArgument("-l").names("--logging").parse("--logging")).isTrue();
	}

	/**
	 * TODO: maybe this should be supported? KeyValueParser#defaultValue() would have to create a
	 * Map in that relayed key lookups against the defaultValue previously set.
	 */
	@Test
	@Ignore
	public void testThatOptionArgumentWorksInPropertyMap() throws ArgumentException
	{
		// new LinkedHashMap<K, V>()
		// {
		// @Override
		// public V get(Object key)
		// {
		// if(!super.containsKey(key))
		// {
		// return defaultValueForValues;
		// }
		// return super.get(key);
		// }
		// };
		assertThat(optionArgument("-n").asPropertyMap().parse("-nactived.property").get("actived.property")).isTrue();
	}

	@Test(expected = IllegalArgumentException.class)
	public void testThatOptionalArgumentsEnforcesAtLeastOneNameForIterable()
	{
		optionArgument("-l").names(Collections.<String>emptyList());
	}

	@SuppressWarnings("deprecation")
	@Test(expected = IllegalStateException.class)
	public void testThatOptionalArgumentsCantHaveSeparators()
	{
		optionArgument("-l").separator("=");
	}

	@SuppressWarnings("deprecation")
	@Test(expected = IllegalStateException.class)
	public void testThatOptionalArgumentsCantHaveArity()
	{
		optionArgument("-l").arity(2);
	}

	@SuppressWarnings("deprecation")
	@Test(expected = IllegalStateException.class)
	public void testThatOptionalArgumentsCantHaveVariableArity()
	{
		optionArgument("-l").variableArity();
	}

	@SuppressWarnings("deprecation")
	@Test(expected = IllegalStateException.class)
	public void testThatOptionalArgumentsCantBeSplit()
	{
		optionArgument("-l").splitWith(",");
	}
}
