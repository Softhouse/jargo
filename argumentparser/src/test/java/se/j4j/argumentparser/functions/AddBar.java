package se.j4j.argumentparser.functions;

import com.google.common.base.Function;

public class AddBar implements Function<String, String>
{
	@Override
	public String apply(String value)
	{
		return value + "bar";
	}

}
