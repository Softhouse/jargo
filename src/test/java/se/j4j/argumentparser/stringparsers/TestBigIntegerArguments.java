package se.j4j.argumentparser.stringparsers;

import static org.fest.assertions.Assertions.assertThat;
import static se.j4j.argumentparser.ArgumentFactory.bigIntegerArgument;

import java.math.BigInteger;

import org.junit.Test;

import se.j4j.argumentparser.ArgumentException;
import se.j4j.argumentparser.ArgumentExceptions.InvalidArgument;

public class TestBigIntegerArguments
{

	@Test
	public void testInvalidInteger() throws ArgumentException
	{
		try
		{
			bigIntegerArgument("-n").parse("-n", "1a");
		}
		catch(InvalidArgument e)
		{
			assertThat(e.getMessage()).isEqualTo("'1a' is not a valid big-integer");
		}
	}

	@Test
	public void testValidInteger() throws ArgumentException
	{
		BigInteger b = bigIntegerArgument("-n").parse("-n", "123456789123456789");

		assertThat(b).isEqualTo(new BigInteger("123456789123456789"));
	}

	@Test
	public void testDescription()
	{
		String usage = bigIntegerArgument("-b").usage("BigIntegerArgument");
		assertThat(usage).contains("<big-integer>: any integer");
	}

	@Test
	public void testThatBigIntegerDefaultsToZero() throws ArgumentException
	{
		BigInteger b = bigIntegerArgument("-b").parse();
		assertThat(b).isEqualTo(BigInteger.ZERO);
	}
}