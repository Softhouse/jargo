package se.j4j.argumentparser;

import static junit.framework.Assert.assertTrue;
import se.j4j.argumentparser.ArgumentParser.ParsedArguments;
import se.j4j.argumentparser.handlers.CommandArgument;

public class InitCommand extends CommandArgument
{

	@Override
	public ArgumentParser getParserInstance()
	{
		return ArgumentParser.forArguments().throwOnUnexpectedArgument();
	}

	@Override
	public void handle(final ParsedArguments parsedArguments)
	{
		//Here would the init code be
		assertTrue(true);
	}

}
