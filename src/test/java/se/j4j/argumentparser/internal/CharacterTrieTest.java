package se.j4j.argumentparser.internal;

import static java.util.Arrays.asList;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertTrue;
import static org.fest.assertions.Assertions.assertThat;

import java.util.HashSet;

import org.junit.Test;

import se.j4j.argumentparser.internal.CharacterTrie;

public class CharacterTrieTest
{
	@Test
	public void testTrieTree()
	{

		Object hello = new Object();
		Object him = new Object();
		Object himmi = new Object();
		Object world = new Object();
		CharacterTrie<Object> tree = CharacterTrie.newTrie();

		assertEquals("{}", tree.toString());
		assertEquals(new HashSet<Object>(), tree.keys());
		assertFalse(tree.delete("nonexisting key"));

		assertNull("Failed to insert hello", tree.set("hello", hello));
		assertNull("Failed to insert himmi", tree.set("himmi", himmi));
		assertNull("Failed to insert him", tree.set("him", him));
		assertNull("Failed to insert world", tree.set("world", world));

		assertNotNull("world should already exist in the tree", tree.set("world", world));

		assertEquals("Wrong tree size, insertion in tree must have failed", 4, tree.size());

		// Removing a node which have children
		assertTrue("Failed to delete \"him\" from " + tree, tree.delete("him"));
		assertFalse("Deleted \"him\" from: " + tree + ", even though it's part of \"himmi\" ", tree.delete("him"));
		// Make sure the children was left intact
		assertTrue("\"himmi\" did not exist in tree" + tree, tree.contains("himmi"));

		// Clean up parents because they have no children
		assertTrue("Failed to delete \"himmi\" from " + tree, tree.delete("himmi"));

		assertFalse("Deleted non-existing object \"Bye\" from " + tree, tree.delete("Bye"));

		assertTrue("hello didn't exist in the tree", tree.contains("hello"));
		assertFalse("Bye did exist in the tree", tree.contains("Bye"));
		assertEquals("Wrong tree size, deletion from tree must have failed", 2, tree.size());

		assertEquals(hello, tree.get("hello"));
		assertEquals(world, tree.get("world"));
		assertNull(tree.get("Bye"));
		assertNull(tree.get("hell"));
		assertEquals(new HashSet<String>(asList("hello", "world")), tree.keys());
		assertThat(tree.values()).isEqualTo(asList(world, hello));

		// TODO: what happens if an item is removed during a tree.keys()
		// operation?
	}

	@Test
	public void testStartsWith()
	{
		CharacterTrie<Object> tree = CharacterTrie.newTrie();
		Object value = new Object();

		tree.set("name=", value);

		assertEquals(value, tree.getLastMatch("name=value"));
		assertThat(tree.getLastMatch("")).isNull();
	}

	@Test(expected = IllegalArgumentException.class)
	public void testInvalidKeys()
	{
		assertFalse(CharacterTrie.validKey(""));
		assertFalse(CharacterTrie.validKey(null));
		CharacterTrie<Object> tree = CharacterTrie.newTrie();
		tree.set("", null);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testNullKey()
	{
		CharacterTrie<Object> tree = CharacterTrie.newTrie();
		tree.set(null, null);
	}

	@Test
	public void testValidKeys()
	{
		assertTrue(CharacterTrie.validKey("a"));
	}
}