/*
 * Copyright 2013 Jonatan Jönsson
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package se.softhouse.common.strings;

import se.softhouse.common.guavaextensions.Suppliers2;

import java.io.Serializable;
import java.util.function.Supplier;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import javax.annotation.concurrent.Immutable;

import static java.util.Objects.requireNonNull;

/**
 * Gives you static access to implementations of the {@link Describable} interface.
 */
@Immutable
public final class Describables
{
	private Describables()
	{
	}

	/**
	 * Returns an empty string as a describable.
	 */
	@Nonnull public static final SerializableDescription EMPTY_STRING = asSerializable(withString(""));

	/**
	 * Supplies an already created {@link String} as a {@link Describable}.
	 * Also useful for caching {@link Describable}s that won't change.
	 */
	@Nonnull
	@CheckReturnValue
	public static Describable withString(String description)
	{
		return new NonLazyDescription(description);
	}

	private static final class NonLazyDescription implements Describable
	{
		private final String description;

		private NonLazyDescription(String description)
		{
			this.description = requireNonNull(description);
		}

		@Override
		public String description()
		{
			return description;
		}

		@Override
		public String toString()
		{
			return description();
		}
	}

	/**
	 * Lazily calls {@link String#format(String, Object...)}
	 */
	@Nonnull
	@CheckReturnValue
	public static Describable format(String formatTemplate, Object ... args)
	{
		return new FormatDescription(formatTemplate, args);
	}

	private static final class FormatDescription implements Describable
	{
		private final String formattingTemplate;
		private final Object[] args;

		private FormatDescription(String formattingTemplate, Object ... args)
		{
			this.formattingTemplate = requireNonNull(formattingTemplate);
			this.args = requireNonNull(args);
		}

		@Override
		public String description()
		{
			return String.format(formattingTemplate, args);
		}

		@Override
		public String toString()
		{
			return description();
		}
	}

	/**
	 * Lazily caches the result of running {@link Describable#description()} on {@code describable}
	 * so that it's only run once.
	 */
	public static Describable cache(Describable describable)
	{
		return new CachingDescription(requireNonNull(describable));
	}

	private static final class CachingDescription implements Describable
	{
		private final Supplier<String> description;

		private CachingDescription(final Describable describable)
		{
			this.description = Suppliers2.memoize(describable::description);
		}

		@Override
		public String description()
		{
			return description.get();
		}

		@Override
		public String toString()
		{
			return description();
		}
	}

	/**
	 * Lazily calls the {@link #toString()} of {@code value} as a describable
	 * 
	 * @param value the object to call {@link #toString()} on
	 */
	@Nonnull
	@CheckReturnValue
	public static Describable toString(Object value)
	{
		return new ToStringDescription(value);
	}

	private static final class ToStringDescription implements Describable
	{
		private final Object value;

		private ToStringDescription(Object value)
		{
			this.value = requireNonNull(value);
		}

		@Override
		public String description()
		{
			return value.toString();
		}

		@Override
		public String toString()
		{
			return description();
		}
	}

	/**
	 * Creates an {@link IllegalArgumentException} where the {@link Describable#description()} of
	 * {@code message} is used as the detail message.
	 */
	@Nonnull
	@CheckReturnValue
	public static IllegalArgumentException illegalArgument(Describable message)
	{
		return new IllegalArgument(message);
	}

	/**
	 * Creates an {@link IllegalArgumentException} where the {@link Describable#description()} of
	 * {@code message} is used as the detail message. {@code cause} is set as the cause.
	 */
	@Nonnull
	@CheckReturnValue
	public static IllegalArgumentException illegalArgument(Describable message, Throwable cause)
	{
		return new IllegalArgument(message, cause);
	}

	private static final class IllegalArgument extends IllegalArgumentException
	{
		private final SerializableDescription message;

		private IllegalArgument(final Describable message)
		{
			this.message = asSerializable(message);
		}

		private IllegalArgument(final Describable message, Throwable cause)
		{
			this(message);
			initCause(requireNonNull(cause));
		}

		@Override
		public String getMessage()
		{
			return message.description();
		}

		/**
		 * For {@link Serializable}
		 */
		private static final long serialVersionUID = 1L;
	}

	/**
	 * Returns a version of {@code describable} that is serializable. Note that after serialization
	 * the describable is fixed, that is {@link Describable#description()} won't be called on
	 * {@code describable} any more.
	 */
	@Nonnull
	@CheckReturnValue
	public static SerializableDescription asSerializable(Describable describable)
	{
		return new SerializableDescription(describable);
	}

	/**
	 * A {@link Serializable} wrapper for {@link Describable}s
	 */
	public static final class SerializableDescription implements Serializable, Describable
	{
		private final transient Describable describable;

		private SerializableDescription(Describable descriptionToSerialize)
		{
			describable = requireNonNull(descriptionToSerialize);
		}

		private static final class SerializationProxy implements Serializable
		{
			/**
			 * @serial the detail message for this describable. Constructed lazily when serialized.
			 */
			private final String message;

			private static final long serialVersionUID = 1L;

			private SerializationProxy(Describable descriptionToSerialize)
			{
				message = descriptionToSerialize.description();
			}

			private Object readResolve()
			{
				return new SerializableDescription(Describables.withString(message));
			}
		}

		Object writeReplace()
		{
			return new SerializationProxy(this);
		}

		@Override
		public String description()
		{
			return describable.description();
		}

		@Override
		public String toString()
		{
			return description();
		}

		/**
		 * For {@link Serializable}
		 */
		private static final long serialVersionUID = 1L;
	}
}
