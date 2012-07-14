package se.j4j.argumentparser;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import javax.annotation.concurrent.ThreadSafe;

/**
 * A {@link Cache} that once it has been initialized always returns the same value.
 * Can be used to lazily initialize a singleton value.
 * 
 * @param <T> the type that this cache contains
 */
@ThreadSafe
public abstract class Cache<T>
{
	@Nonnull private T cachedValue;
	private boolean hasInitializedCache = false;

	/**
	 * @return the instance this {@link Cache} manages.
	 */
	@Nonnull
	@CheckReturnValue
	public final T getCachedInstance()
	{
		synchronized(this)
		{
			if(!hasInitializedCache)
			{
				cachedValue = createInstance();
				if(cachedValue == null)
					throw new IllegalStateException("Cached values may not be null");
				hasInitializedCache = true;
			}
			return cachedValue;
		}
	}

	/**
	 * @return the instance this {@link Cache} should manage.
	 *         Only called once and only if an instance is requested.
	 */
	@Nonnull
	protected abstract T createInstance();
}