/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.github.veqryn.collect;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.SortedSet;
import java.util.TreeSet;

import org.junit.Test;

import com.github.veqryn.util.TestingUtil;

/**
 * Tests for the PatriciaTrie class
 *
 * @author Mark Christopher Duncan
 */
public class TestPatriciaTrie {

  final String[] testWords = new String[] {
      "and",
      "ant",
      "antacid",
      "ante",
      "antecede",
      "anteceded",
      "antecededs",
      "antecededsic",
      "antecedent",
      "antewest",
      "awe"};

  @Test
  public void testWords() {

    final Trie<String, String> trie1 = new PatriciaTrie<String>();
    final Trie<String, String> trie2 = new PatriciaTrie<String>().prefixedByMap("a", true);

    final Map<String, String> wordMap = new HashMap<>();

    for (final String word : testWords) {
      trie1.put(word, word);
      trie2.put(word, word);
      wordMap.put(word, word);
    }

    final Trie<String, String> trie3 = new PatriciaTrie<String>(trie1);
    final Trie<String, String> trie4 = new PatriciaTrie<String>(trie2);
    final Trie<String, String> trie5 = new PatriciaTrie<String>(wordMap);

    tryThisTrie(trie1);
    tryThisTrie(trie2);
    tryThisTrie(trie3);
    tryThisTrie(trie4);
    tryThisTrie(trie5);

    assertEquals("antecededs", wordMap.remove("antecededs"));

    assertEquals(wordMap, trie1);
    assertEquals(trie1, trie2);
    assertEquals(trie2, trie3);
    assertEquals(trie3, trie4);
    assertEquals(trie4, trie5);
  }

  public void tryThisTrie(final Trie<String, String> trie) {

    assertEquals(testWords.length, trie.size());

    assertEquals("ant", trie.shortestPrefixOfValue("antecede", true));

    assertEquals("ant", trie.shortestPrefixOfValue("antecede", false));

    assertEquals("antecede", trie.longestPrefixOfValue("antecede", true));

    assertEquals("ante", trie.longestPrefixOfValue("antecede", false));

    assertArrayEquals(new Object[] {"ant", "ante", "antecede"},
        trie.prefixOfValues("antecede", true).toArray());

    assertArrayEquals(new Object[] {"ant", "ante"},
        trie.prefixOfValues("antecede", false).toArray());

    assertArrayEquals(new Object[] {"ant", "ante", "antecede"},
        trie.prefixOfMap("antecede", true).keySet().toArray());

    assertArrayEquals(new Object[] {"ant", "ante"},
        trie.prefixOfMap("antecede", false).keySet().toArray());

    assertArrayEquals(
        new Object[] {"antecede", "anteceded", "antecededs", "antecededsic", "antecedent"},
        trie.prefixedByValues("antecede", true).toArray());

    assertArrayEquals(new Object[] {"anteceded", "antecededs", "antecededsic", "antecedent"},
        trie.prefixedByValues("antecede", false).toArray());

    assertArrayEquals(
        new Object[] {"antecede", "anteceded", "antecededs", "antecededsic", "antecedent"},
        trie.prefixedByMap("antecede", true).keySet().toArray());

    assertArrayEquals(new Object[] {"anteceded", "antecededs", "antecededsic", "antecedent"},
        trie.prefixedByMap("antecede", false).keySet().toArray());

    // Try removing from a View:
    final Collection<String> prefixedByValues = trie.prefixedByValues("antecede", false);

    assertArrayEquals(new Object[] {"anteceded", "antecededs", "antecededsic", "antecedent"},
        prefixedByValues.toArray());

    assertTrue(prefixedByValues.remove("antecededs"));
    assertFalse(prefixedByValues.remove("antecedents"));

    assertArrayEquals(new Object[] {"anteceded", "antecededsic", "antecedent"},
        prefixedByValues.toArray());
  }

  @Test
  public void testIndividualUnicodeCharacters() {

    final PatriciaTrie<String> trie = new PatriciaTrie<>();
    for (final String unicodeCharacter : new TestingUtil.UnicodeGenerator()) {
      assertEquals(0, trie.size());
      trie.put(unicodeCharacter, unicodeCharacter);
      assertEquals(1, trie.size());
      final Iterator<Entry<String, String>> iter = trie.entrySet().iterator();
      final Entry<String, String> entry = iter.next();
      assertEquals(unicodeCharacter, entry.getValue());
      assertEquals(unicodeCharacter, entry.getKey());
      assertEquals(entry.getValue(), entry.getKey());
      iter.remove();
    }
  }

  @Test
  public void testMultipleUnicodeCharacters() {

    final PatriciaTrie<String> trie = new PatriciaTrie<>();
    for (final String unicodeCharacter1 : new TestingUtil.UnicodeGenerator(0, 0x10FFFF, 512)) {
      for (final String unicodeCharacter2 : new TestingUtil.UnicodeGenerator(0, 0x10FFFF, 512)) {
        assertEquals(0, trie.size());
        final String unicodeCharacters = unicodeCharacter1 + unicodeCharacter2;
        trie.put(unicodeCharacters, unicodeCharacters);
        final String firstKey = trie.keySet().iterator().next();
        assertEquals(firstKey, trie.remove(firstKey));
        assertEquals(unicodeCharacters, firstKey);
      }
    }
  }

  @Test
  public void testComparator() {

    final Comparator<? super String> comparator = new PatriciaTrie<>().getCodec().comparator();
    assertNotNull(comparator);

    final SortedSet<String> expected = new TreeSet<>();
    final SortedSet<String> actual = new TreeSet<>(comparator);

    for (final String word : testWords) {
      expected.add(word);
      actual.add(word);
    }

    assertEquals(expected.size(), actual.size());

    final Iterator<String> expIter = expected.iterator();
    final Iterator<String> actIter = actual.iterator();
    while (expIter.hasNext()) {
      assertEquals(expIter.next(), actIter.next());
    }
  }
}


