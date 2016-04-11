/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.github.veqryn.collect;

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

//  @Test
//  public void testIndividualUnicodeCharacters() {
//
//    final PatriciaTrie<String> trie = new PatriciaTrie<>();
//    for (final String unicodeCharacter : new TestingUtil.UnicodeGenerator()) {
//      assertEquals(0, trie.size());
//      trie.put(unicodeCharacter, unicodeCharacter);
//      assertEquals(1, trie.size());
//      final Iterator<Entry<String, String>> iter = trie.entrySet().iterator();
//      final Entry<String, String> entry = iter.next();
//      assertEquals(unicodeCharacter, entry.getValue());
//      assertEquals(unicodeCharacter, entry.getKey());
//      assertEquals(entry.getValue(), entry.getKey());
//      iter.remove();
//    }
//  }

//  @Test
//  public void testMultipleUnicodeCharacters() {
//
//    final PatriciaTrie<String> trie = new PatriciaTrie<>();
//    for (final String unicodeCharacter1 : new TestingUtil.UnicodeGenerator(0, 0x10FFFF, 512)) {
//      for (final String unicodeCharacter2 : new TestingUtil.UnicodeGenerator(0, 0x10FFFF, 512)) {
//        assertEquals(0, trie.size());
//        final String unicodeCharacters = unicodeCharacter1 + unicodeCharacter2;
//        trie.put(unicodeCharacters, unicodeCharacters);
//        assertEquals(unicodeCharacters, trie.pollFirstEntry().getKey());
//      }
//    }
//  }

}
