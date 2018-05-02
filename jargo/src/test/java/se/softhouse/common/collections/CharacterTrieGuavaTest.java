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

import java.util.Map;
import java.util.Map.Entry;

import junit.framework.Test;
import junit.framework.TestCase;

import com.google.common.collect.testing.MapTestSuiteBuilder;
import com.google.common.collect.testing.TestStringMapGenerator;
import com.google.common.collect.testing.features.CollectionFeature;
import com.google.common.collect.testing.features.CollectionSize;
import com.google.common.collect.testing.features.MapFeature;

public class CharacterTrieGuavaTest extends TestCase
{
	public static Test suite()
	{
		return MapTestSuiteBuilder.using(new TestStringMapGenerator(){
			@Override
			protected Map<String, String> create(Entry<String, String>[] entries)
			{
				CharacterTrie<String> trie = CharacterTrie.newTrie();
				for(Entry<String, String> entry : entries)
				{
					trie.put(entry.getKey(), entry.getValue());
				}
				return trie;
			}
		}).withFeatures(CollectionFeature.FAILS_FAST_ON_CONCURRENT_MODIFICATION, MapFeature.GENERAL_PURPOSE,
						CollectionFeature.SUPPORTS_ITERATOR_REMOVE, CollectionSize.ANY)
				.named("CharacterTrie guava-test").createTestSuite();
	}
}
