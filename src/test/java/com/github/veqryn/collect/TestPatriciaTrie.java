/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.github.veqryn.collect;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NavigableMap;
import java.util.NoSuchElementException;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.commons.collections4.BulkTest;
import org.apache.commons.collections4.map.AbstractMapTest;
import org.apache.commons.collections4.map.AbstractSortedMapTest;
import org.junit.Assert;

import junit.framework.Test;


/**
 * Tests for the PatriciaTrie class.
 * Runs some 500+ tests from the Apache Commons Collections (4) project,
 * specifically tests SortedMap's and Map's and their various views.
 * Tests with String data.
 *
 * @author Mark Christopher Duncan
 */
public class TestPatriciaTrie extends AbstractSortedMapTest<String, String> {

  // Test string order when using comparator:
  // hello, tmp, blah, bar, baz, foo, nonnullkey, all, again,
  // you, see, key, key2, gee, golly, gosh, goodbye, we'll


  // Set up our Test:

  public TestPatriciaTrie(final String testName) {
    super(testName);
  }

  public static Test suite() {
    return BulkTest.makeSuite(TestPatriciaTrie.class);
  }

  @Override
  public NavigableMap<String, String> makeObject() {
    return new PatriciaTrie<String>();
  }

  @Override
  public SortedMap<String, String> makeConfirmedMap() {
    // TODO: apparently the only thing apache does not test
    // for is the order keys and values are returned in
    return new TreeMap<String, String>(new PatriciaTrie<String>().comparator());
  }

  @Override
  public boolean isAllowNullKey() {
    return false;
  }

  @Override
  public boolean isAllowNullValue() {
    return false;
  }

  @Override
  public boolean isSetValueSupported() {
    return false;
  }



  // Configure our sub-map views:

  @Override
  public BulkTest bulkTestHeadMap() {
    return new TestTrieHeadMap(this);
  }

  protected class TestTrieHeadMap extends TestHeadMap<String, String> {
    public TestTrieHeadMap(final AbstractMapTest<String, String> main) {
      super(main);
    }

    @Override
    public boolean isSetValueSupported() {
      return false;
    }
  }



  @Override
  public BulkTest bulkTestTailMap() {
    return new TestTrieTailMap(this);
  }

  protected class TestTrieTailMap extends TestTailMap<String, String> {
    public TestTrieTailMap(final AbstractMapTest<String, String> main) {
      super(main);
    }

    @Override
    public boolean isSetValueSupported() {
      return false;
    }
  }



  @Override
  public BulkTest bulkTestSubMap() {
    return new TestTrieSubMap(this);
  }

  protected class TestTrieSubMap extends TestSubMap<String, String> {
    public TestTrieSubMap(final AbstractMapTest<String, String> main) {
      super(main);
    }

    @Override
    public boolean isSetValueSupported() {
      return false;
    }
  }



  @Override
  public BulkTest bulkTestMapEntrySet() {
    return new TestMapEntrySet();
  }

  protected class TestTrieEntrySet extends TestMapEntrySet {
    public boolean isSetValueSupported() {
      return false;
    }
  }



  // TODO: Test Descending Map view
  // TODO: This should work, but I keep getting AbstractMethodError on makeObject
  // public BulkTest bulkTestDescendingMap() {
  // return new TestTrieDescendingMap<V>(this);
  // }
  //
  // public static class TestTrieDescendingMap<V> extends TestViewMap<String, V> {
  //
  // public TestTrieDescendingMap(final AbstractMapTest<String, V> main) {
  // super("PatriciaTrie.TrieDescendingMap", main);
  // final Map<String, V> sm = makeFullMap();
  // for (final Entry<String, V> entry : sm.entrySet()) {
  // this.subSortedKeys.add(entry.getKey());
  // this.subSortedValues.add(entry.getValue());
  // }
  // this.subSortedNewValues.addAll(Arrays.asList(main.getNewSampleValues()));
  // Collections.reverse(this.subSortedNewValues);
  // }
  //
  // @Override
  // public boolean isSetValueSupported() {
  // return false;
  // }
  //
  // @Override
  // public SortedMap<String, V> makeObject() {
  // return ((NavigableMap<String, V>) main.makeObject()).descendingMap();
  // }
  //
  // @Override
  // public SortedMap<String, V> makeFullMap() {
  // return ((NavigableMap<String, V>) main.makeFullMap()).descendingMap();
  // }
  //
  // @Override
  // public String getCompatibilityVersion() {
  // return main.getCompatibilityVersion() + ".TrieDescendingMapView";
  // }
  // }

  // -----------------------------------------------------------------------


  public static <K, V> K getFirstKey(final Map<K, V> map) {
    final Iterator<Entry<K, V>> iter = map.entrySet().iterator();
    if (iter.hasNext()) {
      return iter.next().getKey();
    }
    throw new NoSuchElementException("no first key");
  }

  public static <K, V> K getLastKey(final Map<K, V> map) {
    K key = null;
    for (final Entry<K, V> entry : map.entrySet()) {
      key = entry.getKey();
    }
    if (key != null) {
      return key;
    }
    throw new NoSuchElementException("no last key");
  }

  public static String toBinary(final byte[] bytes) {
    final StringBuilder sb = new StringBuilder(bytes.length * Byte.SIZE);
    for (int i = 0; i < Byte.SIZE * bytes.length; i++) {
      sb.append((bytes[i / Byte.SIZE] << i % Byte.SIZE & 0x80) == 0 ? '0' : '1');
    }
    return sb.toString();
  }

  public static void main(final String[] args) {

    final String[] keys = new String[] {
        "a", "b", "c", "d", "e", "f", "g", "h", "i", "j", "k", "l", "m",
        "n", "o", "p", "q", "r", "s", "t", "u", "v", "w", "x", "y", "z"
    };

    final PatriciaTrie<String> trie = new PatriciaTrie<String>();

    for (final String key : keys) {
      System.out.println(key);
      System.out.println(Arrays.toString(key.getBytes(StandardCharsets.UTF_16BE)));
      System.out.println(toBinary(key.getBytes(StandardCharsets.UTF_16BE)));
      trie.put(key, toBinary(key.getBytes(StandardCharsets.UTF_16BE)));
      System.out.println(AbstractBinaryTrie.resolveKey(trie.getNode(key), trie));
      System.out.println(toBinary(AbstractBinaryTrie.resolveKey(trie.getNode(key), trie)
          .getBytes(StandardCharsets.UTF_16BE)));
      System.out.println();
    }
    System.out.println();
    System.out.println(trie);
    System.out.println();

  }

  public void testPrefixedByMap() {
    final PatriciaTrie<String> trie = new PatriciaTrie<String>();

    final String[] keys = new String[] {
        "Albert", "Xavier", "XyZ", "Anna", "Alien", "Alberto",
        "Alberts", "Allie", "Alliese", "Alabama", "Banane",
        "Blabla", "Amber", "Ammun", "Akka", "Akko", "Albertoo",
        "Amma"
    };

    for (final String key : keys) {
      trie.put(key, key);
    }

    Map<String, String> map;
    Iterator<String> iterator;
    Iterator<Map.Entry<String, String>> entryIterator;
    Map.Entry<String, String> entry;

    map = trie.prefixedByMap("Al", true);
    Assert.assertEquals(8, map.size());
    Assert.assertEquals("Alabama", getFirstKey(map));
    Assert.assertEquals("Alliese", getLastKey(map));
    Assert.assertEquals("Albertoo", map.get("Albertoo"));
    Assert.assertNotNull(trie.get("Xavier"));
    Assert.assertNull(map.get("Xavier"));
    Assert.assertNull(trie.get("Alice"));
    Assert.assertNull(map.get("Alice"));
    iterator = map.values().iterator();
    Assert.assertEquals("Alabama", iterator.next());
    Assert.assertEquals("Albert", iterator.next());
    Assert.assertEquals("Alberto", iterator.next());
    Assert.assertEquals("Albertoo", iterator.next());
    Assert.assertEquals("Alberts", iterator.next());
    Assert.assertEquals("Alien", iterator.next());
    Assert.assertEquals("Allie", iterator.next());
    Assert.assertEquals("Alliese", iterator.next());
    Assert.assertFalse(iterator.hasNext());

    map = trie.prefixedByMap("Albert", true);
    iterator = map.keySet().iterator();
    Assert.assertEquals("Albert", iterator.next());
    Assert.assertEquals("Alberto", iterator.next());
    Assert.assertEquals("Albertoo", iterator.next());
    Assert.assertEquals("Alberts", iterator.next());
    Assert.assertFalse(iterator.hasNext());
    Assert.assertEquals(4, map.size());
    Assert.assertEquals("Albert", getFirstKey(map));
    Assert.assertEquals("Alberts", getLastKey(map));
    Assert.assertNull(trie.get("Albertz"));
    map.put("Albertz", "Albertz");
    Assert.assertEquals("Albertz", trie.get("Albertz"));
    Assert.assertEquals(5, map.size());
    Assert.assertEquals("Albertz", getLastKey(map));
    iterator = map.keySet().iterator();
    Assert.assertEquals("Albert", iterator.next());
    Assert.assertEquals("Alberto", iterator.next());
    Assert.assertEquals("Albertoo", iterator.next());
    Assert.assertEquals("Alberts", iterator.next());
    Assert.assertEquals("Albertz", iterator.next());
    Assert.assertFalse(iterator.hasNext());
    Assert.assertEquals("Albertz", map.remove("Albertz"));

    map = trie.prefixedByMap("Alberto", true);
    Assert.assertEquals(2, map.size());
    Assert.assertEquals("Alberto", getFirstKey(map));
    Assert.assertEquals("Albertoo", getLastKey(map));
    entryIterator = map.entrySet().iterator();
    entry = entryIterator.next();
    Assert.assertEquals("Alberto", entry.getKey());
    Assert.assertEquals("Alberto", entry.getValue());
    entry = entryIterator.next();
    Assert.assertEquals("Albertoo", entry.getKey());
    Assert.assertEquals("Albertoo", entry.getValue());
    Assert.assertFalse(entryIterator.hasNext());
    trie.put("Albertoad", "Albertoad");
    Assert.assertEquals(3, map.size());
    Assert.assertEquals("Alberto", getFirstKey(map));
    Assert.assertEquals("Albertoo", getLastKey(map));
    entryIterator = map.entrySet().iterator();
    entry = entryIterator.next();
    Assert.assertEquals("Alberto", entry.getKey());
    Assert.assertEquals("Alberto", entry.getValue());
    entry = entryIterator.next();
    Assert.assertEquals("Albertoad", entry.getKey());
    Assert.assertEquals("Albertoad", entry.getValue());
    entry = entryIterator.next();
    Assert.assertEquals("Albertoo", entry.getKey());
    Assert.assertEquals("Albertoo", entry.getValue());
    Assert.assertFalse(entryIterator.hasNext());
    Assert.assertEquals("Albertoo", trie.remove("Albertoo"));
    Assert.assertEquals("Alberto", getFirstKey(map));
    Assert.assertEquals("Albertoad", getLastKey(map));
    Assert.assertEquals(2, map.size());
    entryIterator = map.entrySet().iterator();
    entry = entryIterator.next();
    Assert.assertEquals("Alberto", entry.getKey());
    Assert.assertEquals("Alberto", entry.getValue());
    entry = entryIterator.next();
    Assert.assertEquals("Albertoad", entry.getKey());
    Assert.assertEquals("Albertoad", entry.getValue());
    Assert.assertFalse(entryIterator.hasNext());
    Assert.assertEquals("Albertoad", trie.remove("Albertoad"));
    trie.put("Albertoo", "Albertoo");

    map = trie.prefixedByMap("X", true);
    Assert.assertEquals(2, map.size());
    Assert.assertFalse(map.containsKey("Albert"));
    Assert.assertTrue(map.containsKey("Xavier"));
    Assert.assertFalse(map.containsKey("Xalan"));
    iterator = map.values().iterator();
    Assert.assertEquals("Xavier", iterator.next());
    Assert.assertEquals("XyZ", iterator.next());
    Assert.assertFalse(iterator.hasNext());

    map = trie.prefixedByMap("An", true);
    Assert.assertEquals(1, map.size());
    Assert.assertEquals("Anna", getFirstKey(map));
    Assert.assertEquals("Anna", getLastKey(map));
    iterator = map.keySet().iterator();
    Assert.assertEquals("Anna", iterator.next());
    Assert.assertFalse(iterator.hasNext());

    map = trie.prefixedByMap("Ban", true);
    Assert.assertEquals(1, map.size());
    Assert.assertEquals("Banane", getFirstKey(map));
    Assert.assertEquals("Banane", getLastKey(map));
    iterator = map.keySet().iterator();
    Assert.assertEquals("Banane", iterator.next());
    Assert.assertFalse(iterator.hasNext());

    map = trie.prefixedByMap("Am", true);
    Assert.assertFalse(map.isEmpty());
    Assert.assertEquals(3, map.size());
    Assert.assertEquals("Amber", trie.remove("Amber"));
    iterator = map.keySet().iterator();
    Assert.assertEquals("Amma", iterator.next());
    Assert.assertEquals("Ammun", iterator.next());
    Assert.assertFalse(iterator.hasNext());
    iterator = map.keySet().iterator();
    map.put("Amber", "Amber");
    Assert.assertEquals(3, map.size());
    try {
      iterator.next();
      Assert.fail("CME expected");
    } catch (final ConcurrentModificationException expected) {
    }
    Assert.assertEquals("Amber", getFirstKey(map));
    Assert.assertEquals("Ammun", getLastKey(map));

    map = trie.prefixedByMap("Ak\0", true);
    Assert.assertTrue(map.isEmpty());

    map = trie.prefixedByMap("Ak", true);
    Assert.assertEquals(2, map.size());
    Assert.assertEquals("Akka", getFirstKey(map));
    Assert.assertEquals("Akko", getLastKey(map));
    map.put("Ak", "Ak");
    Assert.assertEquals("Ak", getFirstKey(map));
    Assert.assertEquals("Akko", getLastKey(map));
    Assert.assertEquals(3, map.size());
    trie.put("Al", "Al");
    Assert.assertEquals(3, map.size());
    Assert.assertEquals("Ak", map.remove("Ak"));
    Assert.assertEquals("Akka", getFirstKey(map));
    Assert.assertEquals("Akko", getLastKey(map));
    Assert.assertEquals(2, map.size());
    iterator = map.keySet().iterator();
    Assert.assertEquals("Akka", iterator.next());
    Assert.assertEquals("Akko", iterator.next());
    Assert.assertFalse(iterator.hasNext());
    Assert.assertEquals("Al", trie.remove("Al"));

    map = trie.prefixedByMap("Akka", true);
    Assert.assertEquals(1, map.size());
    Assert.assertEquals("Akka", getFirstKey(map));
    Assert.assertEquals("Akka", getLastKey(map));
    iterator = map.keySet().iterator();
    Assert.assertEquals("Akka", iterator.next());
    Assert.assertFalse(iterator.hasNext());

    map = trie.prefixedByMap("Ab", true);
    Assert.assertTrue(map.isEmpty());
    Assert.assertEquals(0, map.size());
    try {
      final Object o = getFirstKey(map);
      Assert.fail("got a first key: " + o);
    } catch (final NoSuchElementException nsee) {
    }
    try {
      final Object o = getLastKey(map);
      Assert.fail("got a last key: " + o);
    } catch (final NoSuchElementException nsee) {
    }
    iterator = map.values().iterator();
    Assert.assertFalse(iterator.hasNext());

    map = trie.prefixedByMap("Albertooo", true);
    Assert.assertTrue(map.isEmpty());
    Assert.assertEquals(0, map.size());
    try {
      final Object o = getFirstKey(map);
      Assert.fail("got a first key: " + o);
    } catch (final NoSuchElementException nsee) {
    }
    try {
      final Object o = getLastKey(map);
      Assert.fail("got a last key: " + o);
    } catch (final NoSuchElementException nsee) {
    }
    iterator = map.values().iterator();
    Assert.assertFalse(iterator.hasNext());

    try {
      map = trie.prefixedByMap("", true);
      Assert.fail("zero length argument should not be allowed");
    } catch (final IllegalArgumentException e) {
    }
    // map = trie.prefixedByMap("", true);
    // Assert.assertSame(trie, map); // stricter than necessary, but a good check

    map = trie.prefixedByMap("\0", true);
    Assert.assertTrue(map.isEmpty());
    Assert.assertEquals(0, map.size());
    try {
      final Object o = getFirstKey(map);
      Assert.fail("got a first key: " + o);
    } catch (final NoSuchElementException nsee) {
    }
    try {
      final Object o = getLastKey(map);
      Assert.fail("got a last key: " + o);
    } catch (final NoSuchElementException nsee) {
    }
    iterator = map.values().iterator();
    Assert.assertFalse(iterator.hasNext());
  }

  public void testPrefixedByMapRemoval() {
    final PatriciaTrie<String> trie = new PatriciaTrie<String>();

    final String[] keys = new String[] {
        "Albert", "Xavier", "XyZ", "Anna", "Alien", "Alberto",
        "Alberts", "Allie", "Alliese", "Alabama", "Banane",
        "Blabla", "Amber", "Ammun", "Akka", "Akko", "Albertoo",
        "Amma"
    };

    for (final String key : keys) {
      trie.put(key, key);
    }

    Map<String, String> map = trie.prefixedByMap("Al", true);
    Assert.assertEquals(8, map.size());
    Iterator<String> iter = map.keySet().iterator();
    Assert.assertEquals("Alabama", iter.next());
    Assert.assertEquals("Albert", iter.next());
    Assert.assertEquals("Alberto", iter.next());
    Assert.assertEquals("Albertoo", iter.next());
    Assert.assertEquals("Alberts", iter.next());
    Assert.assertEquals("Alien", iter.next());
    iter.remove();
    Assert.assertEquals(7, map.size());
    Assert.assertEquals("Allie", iter.next());
    Assert.assertEquals("Alliese", iter.next());
    Assert.assertFalse(iter.hasNext());

    map = trie.prefixedByMap("Ak", true);
    Assert.assertEquals(2, map.size());
    iter = map.keySet().iterator();
    Assert.assertEquals("Akka", iter.next());
    iter.remove();
    Assert.assertEquals(1, map.size());
    Assert.assertEquals("Akko", iter.next());
    if (iter.hasNext()) {
      Assert.fail("shouldn't have next (but was: " + iter.next() + ")");
    }
    Assert.assertFalse(iter.hasNext());
  }



  // -----------------------------------------------------------------------

  @Override
  public String getCompatibilityVersion() {
    return "1";
  }


  // Use this to write out new .obj files for compatibility tracking
  // public static void main(final String[] args) throws IOException {
  // final TestPatriciaTrie<String> test = new TestPatriciaTrie<>("");
  // {
  // final Map<String, String> map = test.makeObject();
  // if (!(map instanceof Serializable)) {
  // return;
  // }
  // final String path = test.getCanonicalEmptyCollectionName(map);
  // Files.createDirectories(Paths.get(path).getParent());
  // test.writeExternalFormToDisk((Serializable) map, path);
  // }
  //
  // {
  // final Map<String, String> map = test.makeFullMap();
  // if (!(map instanceof Serializable)) {
  // return;
  // }
  // final String path = test.getCanonicalFullCollectionName(map);
  // Files.createDirectories(Paths.get(path).getParent());
  // test.writeExternalFormToDisk((Serializable) map, path);
  // }
  // }

}
