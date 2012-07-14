package se.j4j.argumentparser;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * <pre>
 * Allows for only allowing a subset of values parsed by {@link StringParser#parse(String)} implementations.
 * 
 * As an example, look at {@link Limiters#range(Comparable, Comparable)}.
 * 
 * Values have been passed through any {@link Finalizer#finalizeValue(Object)} before {@link #withinLimits(Object)} is called.
 * 
 * Integrate your limiter with an {@link Argument} by calling {@link ArgumentBuilder#limitTo(Limiter)}.
 * 
 * @param <T> the type to limit
 * </pre>
 */
public interface Limiter<T>
{
	/**
	 * <pre>
	 * Limits a value parsed by {@link StringParser#parse(String)} to be within some arbitrary limits.
	 * 
	 * @param value the value to check if it's within the limits of this limiter
	 * @return {@link Limit#OK} if the value is within the limits, i.e if it's an allowed value.
	 * If it's not within the limits {@link Limit#notOk(String)} or
	 * {@link Limit#notOk(Description)} should be used to describe why it wasn't.
	 */
	@CheckReturnValue
	@Nonnull
	Limit withinLimits(@Nullable T value);

	// TODO: print this pro active in usage somehow:
	// Error message: "'" + valueDescriber.describe(value) / valueFromCommandLine + "'" is not " +
	// limiter.validValuesDescription();
	//
	// boolean isWithinLimits(T value)
	// String validValuesDescription();
	// TODO: or remove Limiter all together and refer to ForwardingStringParser instead,
	// Finalizer and Callback could also be replaced by ForwardingStringParser
}
