package se.j4j.argumentparser;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;

/**
 * <pre>
 * Parses {@link String}s into values of the type {@code T}.
 * 
 * Pass your created {@link StringParser}s to {@link ArgumentFactory#withParser(StringParser)}
 * to create an {@link Argument} that uses it to parse values.
 * 
 * @param <T> the type to parse {@link String}s into
 * </pre>
 */
@Immutable
public interface StringParser<T>
{
	/**
	 * Parses the given {@link String} into the type {@code T}
	 * 
	 * @param argument the string as given from the command line
	 * @return the parsed value
	 * @throws ArgumentException if the string isn't valid according to
	 *             {@link #descriptionOfValidValues()}
	 */
	@Nullable
	// TODO: maybe this shouldn't throw a checked exception?
	T parse(String argument) throws ArgumentException;

	/**
	 * Describes what values this {@link StringParser} accepts
	 * 
	 * @return a description string to show in usage texts
	 */
	@Nonnull
	String descriptionOfValidValues();

	/**
	 * Used when the {@link Argument} this parser is connected to wasn't given.
	 * Used as a fall-back mechanism when {@link ArgumentBuilder#defaultValue(Object)} isn't used.
	 */
	@Nullable
	T defaultValue();

	/**
	 * <pre>
	 * The meta description is the text displayed after the argument names.
	 * Typically surrounded by < and >
	 * Sort of like the default value of {@link ArgumentBuilder#metaDescription(String)}.
	 * 
	 * @return a meta description that very briefly explains what value this parser expects
	 */
	@Nonnull
	String metaDescription();
}