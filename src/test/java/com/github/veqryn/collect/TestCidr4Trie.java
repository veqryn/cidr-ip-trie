/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.github.veqryn.collect;

import static com.github.veqryn.net.TestUtil.cidrsInOrder;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.NavigableMap;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.junit.Test;

import com.github.veqryn.collect.AbstractBinaryTrie.Node;
import com.github.veqryn.net.Cidr4;
import com.github.veqryn.net.TestUtil;
import com.github.veqryn.util.TestingUtil;


/**
 * Tests for the CidrTrie class
 *
 * @author Chris Duncan
 */
public class TestCidr4Trie {

  static {
    boolean assertsEnabled = false;
    assert (assertsEnabled = true); // Intentional side effect!!!
    if (!assertsEnabled) {
      throw new RuntimeException("Asserts must be enabled (use '-ea')!!!");
    }
  }

  // TODO: tests on Navigable Interface


  @Test
  public void testSimple() {

    final List<Cidr4> testList = new ArrayList<>();
    final Cidr4Trie<Cidr4> trie = new Cidr4Trie<>();

    final Cidr4 cidr80_28 = new Cidr4("192.168.1.80/28");
    final Cidr4 cidr96_28 = new Cidr4("192.168.1.96/28");
    final Cidr4 cidr98_31 = new Cidr4("192.168.1.98/31");
    final Cidr4 cidr100_30 = new Cidr4("192.168.1.100/30");
    final Cidr4 cidr101_32 = new Cidr4("192.168.1.101/32");
    final Cidr4 cidr102_32 = new Cidr4("192.168.1.102/32");

    // expected order
    testList.add(cidr80_28);
    testList.add(cidr96_28);
    testList.add(cidr98_31);
    testList.add(cidr100_30);
    testList.add(cidr101_32);
    testList.add(cidr102_32);

    // Put out of order
    trie.put(cidr96_28, cidr96_28);
    trie.put(cidr101_32, cidr101_32);
    trie.put(cidr98_31, cidr98_31);
    trie.put(cidr102_32, cidr102_32);
    trie.put(cidr101_32, cidr101_32); // Duplicate on purpose
    trie.put(cidr80_28, cidr80_28);
    trie.put(cidr100_30, cidr100_30);

    // Basic stuff
    assertFalse(trie.isEmpty());
    assertEquals(6, trie.size());

    assertEquals(cidr98_31, trie.get(cidr98_31));

    assertTrue(trie.containsKey(cidr98_31));
    assertFalse(trie.containsKey(new Cidr4("192.168.1.100/31")));

    assertTrue(trie.containsValue(cidr98_31));
    assertFalse(trie.containsValue(new Cidr4("192.168.1.100/31")));

    // Views
    final Iterator<Entry<Cidr4, Cidr4>> entryIter = trie.entrySet().iterator();
    for (final Cidr4 cidr : testList) {
      final Entry<Cidr4, Cidr4> nextEntry = entryIter.next();
      assertEquals(cidr, nextEntry.getKey());
      assertEquals(cidr, nextEntry.getValue());
    }
    assertFalse(entryIter.hasNext());

    final Iterator<Cidr4> keyIter = trie.keySet().iterator();
    for (final Cidr4 cidr : testList) {
      final Cidr4 nextKey = keyIter.next();
      assertEquals(cidr, nextKey);
    }
    assertFalse(keyIter.hasNext());

    final Iterator<Cidr4> valIter = trie.values().iterator();
    for (final Cidr4 cidr : testList) {
      final Cidr4 nextVal = valIter.next();
      assertEquals(cidr, nextVal);
    }
    assertFalse(valIter.hasNext());

    // Prefix Of
    assertEquals(cidr102_32, trie.longestPrefixOfValue(cidr102_32, true));
    assertEquals(cidr100_30, trie.longestPrefixOfValue(cidr100_30, true));
    assertEquals(cidr100_30, trie.longestPrefixOfValue(cidr102_32, false));
    assertEquals(cidr100_30, trie.longestPrefixOfValue(new Cidr4("192.168.1.103/32"), true));

    assertEquals(cidr96_28, trie.shortestPrefixOfValue(cidr102_32, true));

    assertArrayEquals(new Cidr4[] {cidr96_28, cidr100_30, cidr102_32},
        trie.prefixOfValues(cidr102_32, true).toArray());

    // Prefix By
    assertArrayEquals(new Cidr4[] {cidr100_30, cidr101_32, cidr102_32},
        trie.prefixedByValues(cidr100_30, true).toArray());

    // View edit
    final Trie<Cidr4, Cidr4> prefixView = trie.prefixOfMap(cidr102_32, true);
    // can not be removed, because it is not inside the view (even though it is in the map)
    assertEquals(null, prefixView.remove(cidr80_28));
    assertEquals(6, trie.size());
    assertEquals(cidr80_28, trie.get(cidr80_28));
    // can be removed
    assertEquals(cidr96_28, prefixView.remove(cidr96_28));
    assertEquals(5, trie.size());

    // A view of a view is ok
    assertEquals(trie.prefixOfMap(cidr100_30, true), prefixView.prefixOfMap(cidr100_30, true));

    // Bounded on both sides
    assertArrayEquals(new Cidr4[] {cidr100_30, cidr102_32},
        prefixView.prefixedByValues(cidr100_30, true).toArray());

    // Clear
    trie.clear();
    assertTrue(trie.isEmpty());
    assertEquals(0, trie.size());
  }


  @Test
  public void testPrefixOf() {

    final Cidr4Trie<String> trie = new Cidr4Trie<>(getTestCidrs());

    // Longest Prefix Of (inclusive)
    assertEquals("128.0.0.0/32", trie.longestPrefixOfValue(new Cidr4("128.0.0.0/32"), true));
    assertEquals("128.0.0.0/29", trie.longestPrefixOfValue(new Cidr4("128.0.0.2/32"), true));
    assertEquals("128.0.0.4/32", trie.longestPrefixOfValue(new Cidr4("128.0.0.4/32"), true));
    assertEquals("128.0.0.4/30", trie.longestPrefixOfValue(new Cidr4("128.0.0.6/32"), true));
    assertEquals("0.0.0.0/31", trie.longestPrefixOfValue(new Cidr4("0.0.0.1/32"), true));
    assertEquals("128.0.0.4/31", trie.longestPrefixOfValue(new Cidr4("128.0.0.4/31"), true));
    assertEquals("128.0.0.0/28", trie.longestPrefixOfValue(new Cidr4("128.0.0.8/30"), true));

    // Longest Prefix Of (exclusive)
    assertEquals("128.0.0.0/29", trie.longestPrefixOfValue(new Cidr4("128.0.0.0/32"), false));
    assertEquals("128.0.0.4/31", trie.longestPrefixOfValue(new Cidr4("128.0.0.4/32"), false));
    assertEquals("128.0.0.4/31", trie.longestPrefixOfValue(new Cidr4("128.0.0.5/32"), false));
    assertEquals("128.0.0.4/30", trie.longestPrefixOfValue(new Cidr4("128.0.0.6/32"), false));

    // Shortest Prefix Of (inclusive)
    assertEquals("128.0.0.0/1", trie.shortestPrefixOfValue(new Cidr4("128.0.0.0/1"), true));
    assertEquals("128.0.0.0/1", trie.shortestPrefixOfValue(new Cidr4("128.0.0.0/32"), true));
    assertEquals("128.0.0.0/1", trie.shortestPrefixOfValue(new Cidr4("255.255.255.255/32"), true));
    assertEquals("0.0.0.0/1", trie.shortestPrefixOfValue(new Cidr4("0.0.0.0/32"), true));
    assertEquals("0.0.0.0/1", trie.shortestPrefixOfValue(new Cidr4("127.255.255.255/32"), true));

    // Shortest Prefix Of (exclusive)
    assertEquals(null, trie.shortestPrefixOfValue(new Cidr4("128.0.0.0/1"), false));
    assertEquals("128.0.0.0/1", trie.shortestPrefixOfValue(new Cidr4("128.0.0.0/32"), false));


    // Prefix Of Values (inclusive)
    assertArrayEquals(new String[] {"128.0.0.0/1"},
        trie.prefixOfValues(new Cidr4("128.0.0.0/1"), true).toArray(new String[] {}));

    String[] prefixOfArray1 = new String[] {
        "128.0.0.0/1",
        "128.0.0.0/8",
        "128.0.0.0/16",
        "128.0.0.0/24",
        "128.0.0.0/28",
        "128.0.0.0/29",
        "128.0.0.4/30",
        "128.0.0.4/31",
        "128.0.0.4/32"};
    assertArrayEquals(prefixOfArray1,
        trie.prefixOfValues(new Cidr4("128.0.0.4/32"), true).toArray(new String[] {}));


    String[] prefixOfArray2 = new String[] {
        "128.0.0.0/1",
        "128.0.0.0/8",
        "128.0.0.0/16",
        "128.0.0.0/24",
        "128.0.0.0/28"};
    assertArrayEquals(prefixOfArray2,
        trie.prefixOfValues(new Cidr4("128.0.0.8/30"), true).toArray(new String[] {}));

    String[] prefixOfArray3 = new String[] {
        "0.0.0.0/1",
        "0.0.0.0/8",
        "0.0.0.0/16",
        "0.0.0.0/24",
        "0.0.0.0/30",
        "0.0.0.2/32"};
    assertArrayEquals(prefixOfArray3,
        trie.prefixOfValues(new Cidr4("0.0.0.2/32"), true).toArray(new String[] {}));

    // Prefix Of Map (inclusive)
    NavigableMap<Cidr4, String> prefixOfMap1 = new TreeMap<>();
    for (final String cidr : prefixOfArray1) {
      prefixOfMap1.put(new Cidr4(cidr), cidr);
    }
    assertEquals(prefixOfMap1, trie.prefixOfMap(new Cidr4("128.0.0.4/32"), true));

    NavigableMap<Cidr4, String> prefixOfMap2 = new TreeMap<>();
    for (final String cidr : prefixOfArray2) {
      prefixOfMap2.put(new Cidr4(cidr), cidr);
    }
    assertEquals(prefixOfMap2, trie.prefixOfMap(new Cidr4("128.0.0.8/30"), true));

    NavigableMap<Cidr4, String> prefixOfMap3 = new TreeMap<>();
    for (final String cidr : prefixOfArray3) {
      prefixOfMap3.put(new Cidr4(cidr), cidr);
    }
    assertEquals(prefixOfMap3, trie.prefixOfMap(new Cidr4("0.0.0.2/32"), true));

    // Prefix Of Values (exclusive)
    assertArrayEquals(new String[] {},
        trie.prefixOfValues(new Cidr4("128.0.0.0/1"), false).toArray(new String[] {}));

    prefixOfArray1 = new String[] {
        "128.0.0.0/1",
        "128.0.0.0/8",
        "128.0.0.0/16",
        "128.0.0.0/24",
        "128.0.0.0/28",
        "128.0.0.0/29",
        "128.0.0.4/30",
        "128.0.0.4/31"};
    assertArrayEquals(prefixOfArray1,
        trie.prefixOfValues(new Cidr4("128.0.0.4/32"), false).toArray(new String[] {}));


    prefixOfArray2 = new String[] {
        "128.0.0.0/1",
        "128.0.0.0/8",
        "128.0.0.0/16",
        "128.0.0.0/24",
        "128.0.0.0/28"};
    assertArrayEquals(prefixOfArray2,
        trie.prefixOfValues(new Cidr4("128.0.0.8/30"), false).toArray(new String[] {}));

    prefixOfArray3 = new String[] {
        "0.0.0.0/1",
        "0.0.0.0/8",
        "0.0.0.0/16",
        "0.0.0.0/24",
        "0.0.0.0/30"};
    assertArrayEquals(prefixOfArray3,
        trie.prefixOfValues(new Cidr4("0.0.0.2/32"), false).toArray(new String[] {}));

    // Prefix Of Map (exclusive)
    prefixOfMap1 = new TreeMap<>();
    for (final String cidr : prefixOfArray1) {
      prefixOfMap1.put(new Cidr4(cidr), cidr);
    }
    assertEquals(prefixOfMap1, trie.prefixOfMap(new Cidr4("128.0.0.4/32"), false));

    prefixOfMap2 = new TreeMap<>();
    for (final String cidr : prefixOfArray2) {
      prefixOfMap2.put(new Cidr4(cidr), cidr);
    }
    assertEquals(prefixOfMap2, trie.prefixOfMap(new Cidr4("128.0.0.8/30"), false));

    prefixOfMap3 = new TreeMap<>();
    for (final String cidr : prefixOfArray3) {
      prefixOfMap3.put(new Cidr4(cidr), cidr);
    }
    assertEquals(prefixOfMap3, trie.prefixOfMap(new Cidr4("0.0.0.2/32"), false));
  }


  @Test
  public void testPrefixedBy() {

    final Cidr4Trie<String> trie = new Cidr4Trie<>(getTestCidrs());

    // Prefix Of Values (inclusive)
    assertArrayEquals(new String[] {"128.0.0.4/32"},
        trie.prefixedByValues(new Cidr4("128.0.0.4/32"), true).toArray(new String[] {}));

    String[] prefixedByArray1 = new String[] {
        "128.0.0.0/24",
        "128.0.0.0/28",
        "128.0.0.0/29",
        "128.0.0.0/32",
        "128.0.0.3/32",
        "128.0.0.4/30",
        "128.0.0.4/31",
        "128.0.0.4/32",
        "128.0.0.5/32"};
    assertArrayEquals(prefixedByArray1,
        trie.prefixedByValues(new Cidr4("128.0.0.0/24"), true).toArray(new String[] {}));

    String[] prefixedByArray2 = new String[] {
        "128.0.0.4/30",
        "128.0.0.4/31",
        "128.0.0.4/32",
        "128.0.0.5/32"};
    assertArrayEquals(prefixedByArray2,
        trie.prefixedByValues(new Cidr4("128.0.0.4/30"), true).toArray(new String[] {}));


    // Prefix Of Map (inclusive)
    NavigableMap<Cidr4, String> prefixedByMap1 = new TreeMap<>();
    for (final String cidr : prefixedByArray1) {
      prefixedByMap1.put(new Cidr4(cidr), cidr);
    }
    assertEquals(prefixedByMap1, trie.prefixedByMap(new Cidr4("128.0.0.0/24"), true));

    NavigableMap<Cidr4, String> prefixedByMap2 = new TreeMap<>();
    for (final String cidr : prefixedByArray2) {
      prefixedByMap2.put(new Cidr4(cidr), cidr);
    }
    assertEquals(prefixedByMap2, trie.prefixedByMap(new Cidr4("128.0.0.4/30"), true));

    // Prefix Of Values (exclusive)
    assertArrayEquals(new String[] {},
        trie.prefixedByValues(new Cidr4("128.0.0.4/32"), false).toArray(new String[] {}));

    prefixedByArray1 = new String[] {
        "128.0.0.0/28",
        "128.0.0.0/29",
        "128.0.0.0/32",
        "128.0.0.3/32",
        "128.0.0.4/30",
        "128.0.0.4/31",
        "128.0.0.4/32",
        "128.0.0.5/32"};
    assertArrayEquals(prefixedByArray1,
        trie.prefixedByValues(new Cidr4("128.0.0.0/24"), false).toArray(new String[] {}));

    prefixedByArray2 = new String[] {
        "128.0.0.4/31",
        "128.0.0.4/32",
        "128.0.0.5/32"};
    assertArrayEquals(prefixedByArray2,
        trie.prefixedByValues(new Cidr4("128.0.0.4/30"), false).toArray(new String[] {}));


    // Prefix Of Map (inclusive)
    prefixedByMap1 = new TreeMap<>();
    for (final String cidr : prefixedByArray1) {
      prefixedByMap1.put(new Cidr4(cidr), cidr);
    }
    assertEquals(prefixedByMap1, trie.prefixedByMap(new Cidr4("128.0.0.0/24"), false));

    prefixedByMap2 = new TreeMap<>();
    for (final String cidr : prefixedByArray2) {
      prefixedByMap2.put(new Cidr4(cidr), cidr);
    }
    assertEquals(prefixedByMap2, trie.prefixedByMap(new Cidr4("128.0.0.4/30"), false));
  }


  @Test
  public void testEquality() {

    final NavigableMap<Cidr4, String> testMap = getTestCidrs();
    final Set<Cidr4> set = new TreeSet<>();
    final Cidr4Trie<String> trie1 = new Cidr4Trie<>();
    final Cidr4Trie<String> trie2 = new Cidr4Trie<>();

    assertEquals(trie1, trie2);

    // Insert multiple times
    for (int i = 0; i < 10; ++i) {

      trie1.putAll(testMap);

      for (final Entry<Cidr4, String> entry : testMap.entrySet()) {
        set.add(entry.getKey());
        trie2.put(entry.getKey(), entry.getValue());
      }
    }

    assertEquals(testMap.size(), set.size());
    assertEquals(testMap.size(), trie1.size());
    assertEquals(testMap.size(), trie2.size());
    assertEquals(testMap.keySet(), set);
    assertEquals(testMap, trie1);
    assertEquals(testMap, trie2);
    assertEquals(trie1, trie2);
    assertEquals(testMap.hashCode(), trie1.hashCode());
    assertEquals(testMap.hashCode(), trie2.hashCode());
  }


  @Test
  public void testOrder() {

    final Cidr4Trie<String> trie = new Cidr4Trie<>();

    for (final Object[] cidr : TestUtil.cidrs) {
      // avoid duplicates, so remove 0.0.0.0/0 and 0.0.0.0/1 and 128.0.0.0/1
      if (cidr[9].equals("0.0.0.0/0")
          || cidr[9].equals("0.0.0.0/1")
          || cidr[9].equals("128.0.0.0/1")) {
        continue;
      }
      trie.put(new Cidr4((String) cidr[9]), (String) cidr[9]);
    }

    assertEquals(TestUtil.cidrsInOrder.length, trie.size());

    int i = 0;
    for (final Entry<Cidr4, String> entry : trie.entrySet()) {
      assertEquals(cidrsInOrder[i++], entry.getKey().getCidrSignature());
    }

    Node<Cidr4, String> node = trie.lastNode();
    for (i = cidrsInOrder.length - 1; i >= 0; --i) {
      assertEquals(cidrsInOrder[i], AbstractBinaryTrie.resolveKey(node, trie).getCidrSignature());
      node = AbstractBinaryTrie.predecessor(node);
    }
  }


  @Test
  @SuppressWarnings("unchecked")
  public void testSerialization() throws ClassNotFoundException, IOException {

    final Cidr4Trie<String> trie1 = new Cidr4Trie<>(getTestCidrs());
    final Cidr4Trie<String> trie2 = new Cidr4Trie<>(trie1);

    assertEquals(trie1, trie2);
    assertEquals(trie1.hashCode(), trie2.hashCode());
    assertSame(trie1.getCodec(), trie2.getCodec());

    final byte[] bytes1 = TestingUtil.pickle(trie1);
    final byte[] bytes2 = TestingUtil.pickle(trie2);

    final Cidr4Trie<String> other1 = TestingUtil.unpickle(bytes1, Cidr4Trie.class);
    final Cidr4Trie<String> other2 = TestingUtil.unpickle(bytes2, Cidr4Trie.class);
    assertEquals(trie1, other1);
    assertEquals(trie2, other2);
    assertEquals(other1, other2);
    assertEquals(trie1.hashCode(), other1.hashCode());
    assertEquals(trie2.hashCode(), other2.hashCode());

    other1.cacheKeys = true;
    other1.writeKeys = true;
    other2.cacheKeys = true;
    other2.writeKeys = true;
    final byte[] bytesWriteKeys1 = TestingUtil.pickle(other1);
    final byte[] bytesWriteKeys2 = TestingUtil.pickle(other2);

    final Cidr4Trie<String> otherWriteKeys1 =
        TestingUtil.unpickle(bytesWriteKeys1, Cidr4Trie.class);
    final Cidr4Trie<String> otherWriteKeys2 =
        TestingUtil.unpickle(bytesWriteKeys2, Cidr4Trie.class);
    assertEquals(trie1, otherWriteKeys1);
    assertEquals(trie2, otherWriteKeys2);
    assertEquals(otherWriteKeys1, otherWriteKeys2);
    assertEquals(trie1.hashCode(), otherWriteKeys1.hashCode());
    assertEquals(trie2.hashCode(), otherWriteKeys2.hashCode());
  }


  @Test
  public void testClone() {

    final NavigableMap<Cidr4, String> testMap = getTestCidrs();

    final Cidr4Trie<String> trie1 = new Cidr4Trie<>(testMap);

    final AbstractBinaryTrie<Cidr4, String> trie2 = trie1.clone();

    assertEquals(trie1.codec, trie2.codec);
    assertEquals(trie1.size(), trie2.size());

    final Iterator<Entry<Cidr4, String>> iter1 = trie1.entrySet().iterator();
    final Iterator<Entry<Cidr4, String>> iter2 = trie2.entrySet().iterator();
    while (iter1.hasNext()) {
      assertEquals(iter1.next(), iter2.next());
    }
    assertEquals(trie1, trie2);
    assertEquals(trie1.hashCode(), trie2.hashCode());

    final String cidrString = (String) TestUtil.cidrs[25][9];
    final Cidr4 cidr = new Cidr4(cidrString);

    trie1.put(cidr, "not equal");
    assertEquals(trie1.size(), trie2.size());
    assertNotEquals(trie1, trie2);

    trie1.put(cidr, cidrString);
    assertEquals(trie1.size(), trie2.size());
    assertEquals(trie1, trie2);

    trie2.remove(cidr);
    assertEquals(trie1.size(), trie2.size() + 1);
    assertNotEquals(trie1, trie2);
  }


  /**
   * @return a NavigableMap containing our testing CIDR's
   */
  private NavigableMap<Cidr4, String> getTestCidrs() {
    final NavigableMap<Cidr4, String> map = new TreeMap<>();
    for (final Object[] cidr : TestUtil.cidrs) {
      map.put(new Cidr4((String) cidr[9]), (String) cidr[9]);
    }
    return map;
  }
}
