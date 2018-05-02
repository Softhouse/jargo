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
package se.softhouse.common.collections;

import static java.util.Objects.requireNonNull;

import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.Collections;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.TreeMap;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;

/**
 * <pre>
 * Stores {@link String}s in a <a href="http://en.wikipedia.org/wiki/Trie">trie</a>.
 * The main purpose when using a structure like this is the methods
 * {@link #findLongestPrefix(CharSequence)} and {@link #getEntriesWithPrefix(CharSequence)}.
 *
 * Neither <code>null</code> keys or <code>null</code> values are allowed because just like the
 * devil, they are evil.
 *
 * If you're iterating over the whole trie more often than you do {@link #getEntriesWithPrefix(CharSequence) simple lookups}
 * you're probably better off using a {@link TreeMap}.
 *
 * TODO(jontejj): Implement SortedMap instead of Map
 * </pre>
 * 
 * @param <V> the type of values stored in the trie
 */
@NotThreadSafe
public final class CharacterTrie<V> extends AbstractMap<String, V>
{
	private int size = 0;
	private final Entry<V> root;
	private int modCount = 0;

	/**
	 * An entry represents a node in the tree.
	 */
	private static final class Entry<V> implements Map.Entry<String, V>
	{
		/**
		 * The char the parent node will use to reference this child with
		 */
		private final Character index;

		/**
		 * If true this node represents a value.
		 * TODO(jontejj): This could have been optimized so that all entries have values. I.e
		 * there's not a level for each character if there's no other string that shares the prefix.
		 */
		private boolean isValue;

		/**
		 * the value of this node
		 */
		private V value;

		/**
		 * The nodes that belongs to this Node. Having them in a {@link TreeMap} ensures that
		 * iterating over them is done in a consistent(sorted) manner.
		 */
		private TreeMap<Character, Entry<V>> children;

		@Nullable private final Entry<V> parent;

		private Entry(final Character index, @Nullable final Entry<V> parent)
		{
			this.index = index;
			this.parent = parent;
		}

		@Override
		public String getKey()
		{
			StringBuilder sb = new StringBuilder();
			Entry<V> current = this;
			while(!current.isRoot())
			{
				sb.append(current.index);
				current = current.parent;
			}
			return sb.reverse().toString();
		}

		@Override
		public V getValue()
		{
			return value;
		}

		@Override
		public V setValue(final V value)
		{
			V oldValue = this.value;

			isValue = true;
			this.value = value;

			return oldValue;
		}

		@Override
		public boolean equals(Object obj)
		{
			if(!(obj instanceof Map.Entry<?, ?>))
				return false;

			Map.Entry<?, ?> entry = (Map.Entry<?, ?>) obj;
			return getKey().equals(entry.getKey()) && getValue().equals(entry.getValue());
		}

		@Override
		public int hashCode()
		{
			return getKey().hashCode() ^ getValue().hashCode();
		}

		@Override
		public String toString()
		{
			return getKey() + "=" + getValue();
		}

		private Map.Entry<String, V> findLongestPrefix(final CharSequence prefix)
		{
			Entry<V> child = findLastChild(prefix);
			if(child.isValue)
				return child;
			return null;
		}

		/**
		 * Clear this entry from being a value
		 *
		 * @return true if this call had any effect
		 */
		private boolean unset()
		{
			boolean wasValue = isValue;
			isValue = false;
			value = null;
			return wasValue;
		}

		private boolean isRoot()
		{
			return parent == null;
		}

		private boolean hasChildren()
		{
			return children != null ? children.size() > 0 : false;
		}

		private Entry<V> getChild(final Character c)
		{
			return children != null ? children.get(c) : null;
		}

		/**
		 * @param keyToFetch the key to find the child/leaf for
		 * @return the leaf in the tree that is reached for the given key,
		 *         or null if no such leaf could be found
		 */
		private Entry<V> findChild(final CharSequence keyToFetch)
		{
			// Start at the root and search the tree for the entry matching the
			// given key
			Entry<V> current = this;
			for(int i = 0, len = keyToFetch.length(); i < len && current != null; i++)
			{
				Character c = keyToFetch.charAt(i);
				current = current.getChild(c);
			}
			return current;
		}

		/**
		 * @param prefix the key to find the child/leaf for
		 * @return the leaf in the tree that is reached for the given key,
		 *         or null if no such leaf could be found
		 */
		private Entry<V> findLastChild(final CharSequence prefix)
		{
			// Start at the root and search the tree for an entry starting with
			// key, return the last possible match so that matches with more matching chars will be
			// prioritized
			Entry<V> current = this;
			for(int i = 0, len = prefix.length(); i < len; i++)
			{
				Character c = prefix.charAt(i);
				Entry<V> next = current.getChild(c);
				if(next == null)
					return current;
				current = next;
			}
			return current;
		}

		/**
		 * @param keyToFetch the key to find the child/leaf for
		 * @return the value for the leaf in the tree that is reached for the
		 *         given key,
		 *         or null if no such value could be found
		 */
		private V get(final CharSequence keyToFetch)
		{
			Entry<V> child = findChild(keyToFetch);
			if(child == null)
				return null;
			if(child.isValue)
				return child.value;
			return null;
		}

		/**
		 * @param c the Character index to remove
		 * @throws NullPointerException if this Entry doesn't have had any
		 *             children before
		 */
		private void deleteChild(final Character c)
		{
			children.remove(c);
		}

		/**
		 * Makes sure that a child that represents the given {@code childChar} is found in this
		 * entry.
		 *
		 * @param childChar the character to create/get a child for
		 * @return either the already existing child or a newly created one
		 */
		private Entry<V> ensureChild(final Character childChar)
		{
			if(children == null)
			{
				children = new TreeMap<Character, Entry<V>>();
				Entry<V> child = new Entry<V>(childChar, this);
				children.put(childChar, child);
				return child;
			}
			Entry<V> existing = children.get(childChar);
			if(existing != null)
				return existing;
			Entry<V> child = new Entry<V>(childChar, this);
			children.put(childChar, child);
			return child;
		}

		/**
		 * Removes all key-value pairs in this trie
		 */
		private void clear()
		{
			children = new TreeMap<Character, CharacterTrie.Entry<V>>();
			unset();
		}

		/**
		 * Finds the successor entry for predecessor,
		 * It starts by looking if it's a value itself, then it checks
		 * the children and if nothing there then it walks back up and checks siblings.
		 * (essentially a pre-order tree traversal)
		 *
		 * @param level the current level we're in (in the current stack)
		 */
		private Entry<V> successor(Entry<V> predecessor, CharSequence predecessorKey, int level, boolean isGoingDown)
		{
			if(isValue && predecessor != this && isGoingDown)
				return this;

			if(hasChildren())
			{
				Map.Entry<Character, Entry<V>> next = null;
				if(predecessor != null && predecessor.commonDescent(this) && level < predecessorKey.length())
				{
					// Go through each sibling one after the other
					char charAtLevel = predecessorKey.charAt(level);
					next = children.higherEntry(charAtLevel);
				}
				else
				{
					next = children.firstEntry();
				}

				// Visit the next child
				if(next != null)
					return next.getValue().successor(predecessor, predecessorKey, level + 1, true);
			}

			if(!isRoot()) // Go back up and enter the sibling
				return parent.successor(predecessor, predecessorKey, level - 1, false);

			return null;
		}

		/**
		 * Returns <code>true</code> if this node is an ancestor for {@code entry}.
		 */
		private boolean ancestorFor(Entry<V> entry)
		{
			if(isRoot())
				return true;

			Entry<V> entryAncestor = entry.parent;
			while(entryAncestor != null && this != entryAncestor)
			{
				entryAncestor = entryAncestor.parent;
			}
			return this == entryAncestor;
		}

		private boolean commonDescent(Entry<V> entry)
		{
			if(this.ancestorFor(entry))
				return true;
			else if(entry.ancestorFor(this))
				return true;
			return false;
		}

		private int size()
		{
			int size = isValue ? 1 : 0;
			if(hasChildren())
			{
				for(Entry<V> child : children.values())
				{
					size += child.size();
				}
			}
			return size;
		}
	}

	/**
	 * Creates a new, empty, {@link CharacterTrie}
	 */
	@CheckReturnValue
	public static <V> CharacterTrie<V> newTrie()
	{
		return new CharacterTrie<V>();
	}

	/**
	 * Creates a new {@link CharacterTrie} with the entries from {@code map}
	 */
	@CheckReturnValue
	public static <V> CharacterTrie<V> newTrie(Map<String, V> map)
	{
		CharacterTrie<V> trie = newTrie();
		trie.putAll(map);
		return trie;
	}

	private CharacterTrie()
	{
		root = createRoot();
	}

	/**
	 * @throws NullPointerException if {@code key} or {@code value} is null
	 */
	@Override
	public V put(final String key, final V value)
	{
		requireNonNull(key, "Null key given, CharacterTrie does not support null keys as they are error-prone");
		requireNonNull(value, "Null value given, CharacterTrie does not support null values as they are error-prone. "
				+ "Use the Null Object Pattern instead.");

		// Start at the root and search the tree for the entry to insert the
		// final character into
		Entry<V> current = root;
		for(int i = 0, len = key.length(); i < len; i++)
		{
			Character c = key.charAt(i);
			// Traverses the tree down to the end where we put in our child
			current = current.ensureChild(c);
		}
		V oldValue = current.setValue(value);
		if(oldValue == null)
		{
			size++;
			modCount++;
		}
		return oldValue;
	}

	@Override
	@CheckReturnValue
	public int size()
	{
		return size;
	}

	@Override
	public V remove(final Object keyToRemove)
	{
		CharSequence key = (CharSequence) keyToRemove;
		// Start at the root and search the tree for the entry to delete
		Entry<V> current = root;
		for(int i = 0, len = key.length(); i < len; i++)
		{
			Character c = key.charAt(i);
			current = current.getChild(c);
			if(current == null)
				return null;
		}
		return removeEntry(current);
	}

	private V removeEntry(Entry<V> entryToRemove)
	{
		V oldValue = entryToRemove.getValue();
		if(entryToRemove.unset())
		{
			size--;
			modCount++;
			if(entryToRemove.hasChildren())
				// We have children so we are important and can't be removed
				return oldValue;

			Entry<V> parent = entryToRemove.parent;

			// Remove ourselves from the parent
			parent.deleteChild(entryToRemove.index);

			// Clean up unused entries
			while(!parent.hasChildren() && !parent.isValue)
			{
				Entry<V> grandParent = parent.parent;
				if(grandParent == null)
				{
					break; // we reached root
				}
				// Ask the grandParent to remove our parent
				grandParent.deleteChild(parent.index);

				// Walk up the tree and remove entries without children
				parent = grandParent;
			}
			return oldValue;
		}
		return null;
	}

	@Override
	@CheckReturnValue
	public boolean containsKey(@Nullable final Object key)
	{
		if(key == null)
			return false;
		String keyToCheckContainMentFor = (String) key;
		return root.get(keyToCheckContainMentFor) != null;
	}

	@Override
	@CheckReturnValue
	public V get(final Object key)
	{
		CharSequence keyToFetch = (CharSequence) key;
		return root.get(keyToFetch);
	}

	/**
	 * Returns the entry that shares the longest prefix with {@code key}, or null
	 * if no such entry exists
	 *
	 * @see <a href="http://en.wikipedia.org/wiki/Longest_prefix_match">Longest_prefix_match</a>
	 */
	@CheckReturnValue
	public Map.Entry<String, V> findLongestPrefix(final CharSequence prefix)
	{
		return root.findLongestPrefix(prefix);
	}

	/**
	 * Returns all entries whose key starts with {@code prefix}. The returned {@link Set} is a view
	 * so removed elements from it are also removed in this structure.
	 */
	public Set<Map.Entry<String, V>> getEntriesWithPrefix(final CharSequence prefix)
	{
		Entry<V> startingPoint = root.findChild(prefix);
		if(startingPoint == null)
			return Collections.emptySet();
		return new EntrySet(startingPoint);
	}

	/**
	 * Create a simple Entry which parent is null.
	 */
	private Entry<V> createRoot()
	{
		return new Entry<V>('r', null);
	}

	@Override
	public void clear()
	{
		root.clear();
		size = 0;
		modCount++;
	}

	@Override
	public Set<Map.Entry<String, V>> entrySet()
	{
		return new EntrySet(root);
	}

	private final class EntrySet extends AbstractSet<Map.Entry<String, V>>
	{
		private final Entry<V> startingPoint;

		private EntrySet(Entry<V> startingPoint)
		{
			this.startingPoint = startingPoint;
		}

		@Override
		public Iterator<Map.Entry<String, V>> iterator()
		{
			return new EntryIterator(startingPoint);
		}

		@Override
		public int size()
		{
			return startingPoint.size();
		}

		@Override
		public void clear()
		{
			modCount++;
			startingPoint.clear();
		}
	}

	private final class EntryIterator implements Iterator<Map.Entry<String, V>>
	{
		private int expectedModCount = modCount;
		private Entry<V> next;
		private Entry<V> lastReturned = null;

		private EntryIterator(Entry<V> startingPoint)
		{
			next = startingPoint;
		}

		@Override
		public boolean hasNext()
		{
			if(next == null)
				return false;
			// Don't recalculate next if someone calls hasNext twice without calling next
			if(next == lastReturned || lastReturned == null)
			{
				if(lastReturned == null)
				{
					next = next.successor(lastReturned, "", 0, true);
				}
				else
				{
					CharSequence lastKey = lastReturned.getKey();
					int lastDepth = lastKey.length();
					next = next.successor(lastReturned, lastKey, lastDepth, true);
				}
			}
			return next != null;
		}

		@Override
		public Map.Entry<String, V> next()
		{
			verifyUnmodified();
			if(!hasNext())
				throw new NoSuchElementException();
			lastReturned = next;
			return next;
		}

		@Override
		public void remove()
		{
			verifyUnmodified();
			if(lastReturned == null)
				throw new IllegalStateException("You probably forgot to call next before calling remove");
			if(removeEntry(lastReturned) == null)
				throw new IllegalStateException("You probably forgot to call next before calling remove");
			expectedModCount++;
		}

		private void verifyUnmodified()
		{
			if(expectedModCount != modCount)
				throw new ConcurrentModificationException("Trie modified during iteration");
		}
	}
}
