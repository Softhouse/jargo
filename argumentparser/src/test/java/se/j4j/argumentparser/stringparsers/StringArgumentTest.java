package se.j4j.argumentparser.stringparsers;

import static org.fest.assertions.Assertions.assertThat;
import static se.j4j.argumentparser.ArgumentFactory.stringArgument;

import org.junit.Test;

import se.j4j.argumentparser.ArgumentException;
import se.j4j.argumentparser.ArgumentFactory;
import se.j4j.argumentparser.StringParsers;

/**
 * Tests for {@link ArgumentFactory#stringArgument(String...)} and
 * {@link StringParsers#stringParser()}
 */
public class StringArgumentTest
{
	@Test
	public void testThatTheSameStringIsReturned() throws ArgumentException
	{
		String argumentValue = "Test";
		String actual = stringArgument("-s").parse("-s", argumentValue);
		assertThat(actual).isSameAs(argumentValue);
	}
}