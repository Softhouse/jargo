package se.j4j.argumentparser.defaultvalues;

import com.google.common.base.Supplier;

public class BarSupplier implements Supplier<String>
{
	@Override
	public String get()
	{
		return "bar";
	}
}