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

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedMap;

import com.google.caliper.Param;
import com.google.caliper.Runner;
import com.google.caliper.SimpleBenchmark;
import com.google.common.collect.Maps;

public class CharacterTrieBenchmark extends SimpleBenchmark
{
	@Param({"1000", "10000", "100000"}) private int nrOfElements;

	@Param CollectionType type;

	private Map<String, String> elements;

	String first;
	String last;
	String middle;

	private static final String NON_EXISTING = "asdaqwopiruwepHJGjhagds";

	@Override
	protected void setUp() throws Exception
	{
		HashMap<String, String> elementMap = Maps.newHashMapWithExpectedSize(nrOfElements);

		for(int i = 1; i <= nrOfElements; i++)
		{
			String s = "" + i;
			elementMap.put(s, s);
		}

		elements = type.createMap(elementMap);
		first = "1";
		last = "" + nrOfElements;
		middle = "" + (nrOfElements / 2);
	}

	public int timeIteration(int reps)
	{
		int dummy = 0;
		for(int i = 0; i < reps; i++)
		{
			for(Entry<String, String> entry : elements.entrySet())
			{
				if(entry != null)
				{
					dummy++;
				}
			}
		}
		return dummy;
	}

	public int timeContains(int reps)
	{
		int dummy = 0;
		for(int i = 0; i < reps; i++)
		{
			dummy += elements.containsKey(first) ? 1 : 0;
			dummy += elements.containsKey(middle) ? 1 : 0;
			dummy += elements.containsKey(last) ? 1 : 0;
			dummy += elements.containsKey(NON_EXISTING) ? 1 : 0;
		}
		return dummy;
	}

	public int timePrefixSearching(int reps)
	{
		int dummy = 0;
		for(int i = 0; i < reps; i++)
		{
			for(Entry<String, String> entry : type.entriesWithPrefix(elements, "100"))
			{
				if(entry != null)
				{
					dummy++;
				}
			}
		}
		return dummy;
	}

	private enum CollectionType
	{
		MAP
		{

	@Override
			<T> Map<String, T> createMap(Map<String, T> elements)
			{
				return Maps.newTreeMap();
			}

	@Override
			<T> Set<Entry<String, T>> entriesWithPrefix(Map<String, T> elements, String prefix)
			{
				return ((SortedMap<String, T>) elements).tailMap(prefix).entrySet();
			}

	},

	CHARACTER_TRIE{

	@Override
			<T> Map<String, T> createMap(Map<String, T> elements)
			{
				return CharacterTrie.newTrie(elements);
			}

	@Override
			<T> Set<Entry<String, T>> entriesWithPrefix(Map<String, T> elements, String prefix)
			{
				return ((CharacterTrie<T>) elements).getEntriesWithPrefix(prefix);
			}

	};

	abstract <T> Map<String, T> createMap(Map<String, T> elements);

	abstract <T> Set<Entry<String, T>> entriesWithPrefix(Map<String, T> elements, String prefix);

	}

	public static void main(String[] args) throws Exception
	{
		// CharacterTrieBenchmark run = new CharacterTrieBenchmark();
		// run.type = CollectionType.CHARACTER_TRIE;
		// run.nrOfElements = 100000;
		// run.setUp();
		// run.timePrefixSearching(1000);

		Runner.main(CharacterTrieBenchmark.class, args);
	}
}
