/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.github.veqryn.collect;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import java.util.Iterator;
import java.util.Map.Entry;

import org.junit.Test;

import com.github.veqryn.util.TestingUtil;

/**
 * Tests for the PatriciaTrie class
 *
 * @author Mark Christopher Duncan
 */
public class TestPatriciaTrie {

  @Test
  public void testWords() {

    final Trie<String, String> trie = new PatriciaTrie<String>();

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

    for (final String word : testWords) {
      trie.put(word, word);
    }

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
}
