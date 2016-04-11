/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.github.veqryn.collect;

import static com.github.veqryn.net.TestUtil.cidrsInOrder;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;
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
 * @author Mark Christopher Duncan
 */
public class TestCidr4Trie {

  static {
    boolean assertsEnabled = false;
    assert (assertsEnabled = true); // Intentional side effect!!!
    if (!assertsEnabled) {
      throw new RuntimeException("Asserts must be enabled (use '-ea')!!!");
    }
  }

  // TODO: all of these tests will be refactored and simplified,
  // after the AbstractBinaryTree class has itself been refactored.
  // This is just enough to get me through the refactoring phase.


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
  public void testWhatever() throws ClassNotFoundException, IOException {

    final Cidr4Trie<String> trie = new Cidr4Trie<>(false);

    for (final Object[] cidr : TestUtil.cidrs) {
      // avoid duplicates, so remove 0.0.0.0/0 and 0.0.0.0/1 and 128.0.0.0/1
      if (cidr[9].equals("0.0.0.0/0")
          || cidr[9].equals("0.0.0.0/1")
          || cidr[9].equals("128.0.0.0/1")) {
        continue;
      }
      trie.put(new Cidr4((String) cidr[9]), (String) cidr[9]);
    }
    System.out.println(trie.size());

    final Set<Entry<Cidr4, String>> entries = trie.entrySet();
    System.out.println(entries.size());
    System.out.println();
    final Collection<String> values = trie.values();
    System.out.println(trie.longestPrefixOfValue(new Cidr4("128.0.0.0/10"), true));
    System.out.println();
    for (final String value : values) {
      System.out.println(value);
    }
    System.out.println();
    for (final String value : trie.prefixOfValues(new Cidr4("128.0.0.4/32"), true)) {
      System.out.println(value);
    }
    System.out.println();
    for (final String value : trie.prefixedByValues(new Cidr4("128.0.0.0/24"), true)) {
      System.out.println(value);
    }
    System.out.println();

    final NavigableTrie<Cidr4, String> reversed = trie.descendingMap();
    System.out.println(reversed.size());
    final Set<Entry<Cidr4, String>> reversedEntries = reversed.entrySet();
    System.out.println(reversedEntries.size());
    System.out.println();
    final Collection<String> reversedValues = reversed.values();
    System.out.println(reversed.longestPrefixOfValue(new Cidr4("128.0.0.0/10"), true));
    System.out.println();
    for (final String value : reversedValues) {
      System.out.println(value);
    }
    System.out.println();
    for (final Entry<Cidr4, String> value : reversed.entrySet()) {
      System.out.println(value);
    }
  }


  @Test
  public void testMapInterface() {

    final Cidr4 s1 = new Cidr4(0, 1); // zeroes
    final Cidr4 s3 = new Cidr4(0, 3); // zeroes

    final Cidr4 t1 = new Cidr4(-1, 1); // ones
    final Cidr4 t3 = new Cidr4(-1, 3); // ones

    final Cidr4Trie<String> trie = new Cidr4Trie<>();

    trie.put(s1, "depth 1 s: " + s1);
    trie.put(s3, "depth 3 s: " + s3);

    trie.put(t1, "depth 1 t: " + t1);
    trie.put(t3, "depth 3 t: " + t3);

    assertEquals(4, trie.size());

    assertEquals("{0.0.0.0/1=depth 1 s: 0.0.0.0/1, 0.0.0.0/3=depth 3 s: 0.0.0.0/3, "
        + "128.0.0.0/1=depth 1 t: 128.0.0.0/1, 224.0.0.0/3=depth 3 t: 224.0.0.0/3}",
        trie + "");

    assertEquals(
        "[depth 1 s: 0.0.0.0/1, depth 3 s: 0.0.0.0/3, depth 1 t: 128.0.0.0/1, depth 3 t: 224.0.0.0/3]",
        trie.values() + "");

    assertEquals("[0.0.0.0/1, 0.0.0.0/3, 128.0.0.0/1, 224.0.0.0/3]", trie.keySet() + "");
    assertEquals("[0.0.0.0/1=depth 1 s: 0.0.0.0/1, 0.0.0.0/3=depth 3 s: 0.0.0.0/3, "
        + "128.0.0.0/1=depth 1 t: 128.0.0.0/1, 224.0.0.0/3=depth 3 t: 224.0.0.0/3]",
        trie.entrySet() + "");

    assertEquals("depth 1 s: 0.0.0.0/1", trie.get(s1));
    assertEquals("depth 3 s: 0.0.0.0/3", trie.get(s3));
    assertEquals("depth 1 t: 128.0.0.0/1", trie.get(t1));
    assertEquals("depth 3 t: 224.0.0.0/3", trie.get(t3));

    // assertEquals("[depth 1 s: 0.0.0.0/1]", trie.getAll(s1) + "");
    assertEquals("[depth 1 s: 0.0.0.0/1, depth 3 s: 0.0.0.0/3]",
        trie.prefixOfValues(s3, true) + "");
    // assertEquals("[depth 1 t: 128.0.0.0/1]", trie.getAll(t1) + "");
    assertEquals("[depth 1 t: 128.0.0.0/1, depth 3 t: 224.0.0.0/3]",
        trie.prefixOfValues(t3, true) + "");

    assertEquals("null=null", trie.root + "");


    assertEquals("0.0.0.0/1", AbstractBinaryTrie.resolveKey(trie.root.left, trie) + "");

    // assertEquals("0.0.0.0/2", Cidr4Trie.resolveKey(trie.root.left.left, trie) + "");

    assertEquals("0.0.0.0/3", AbstractBinaryTrie.resolveKey(trie.root.left.left.left, trie) + "");

    assertEquals("128.0.0.0/1", AbstractBinaryTrie.resolveKey(trie.root.right, trie) + "");

    // assertEquals("192.0.0.0/2", Cidr4Trie.resolveKey(trie.root.right.right, trie) + "");

    assertEquals("224.0.0.0/3",
        AbstractBinaryTrie.resolveKey(trie.root.right.right.right, trie) + "");


    assertEquals("depth 1 s: 0.0.0.0/1", trie.root.left.value + "");

    assertEquals(null, trie.root.left.left.value);

    assertEquals("depth 3 s: 0.0.0.0/3", trie.root.left.left.left.value + "");

    assertEquals("depth 1 t: 128.0.0.0/1", trie.root.right.value + "");

    assertEquals(null, trie.root.right.right.value);

    assertEquals("depth 3 t: 224.0.0.0/3", trie.root.right.right.right.value + "");


    assertEquals("depth 3 t: 224.0.0.0/3", trie.remove(t3));

    assertEquals(3, trie.size());

    assertEquals("{0.0.0.0/1=depth 1 s: 0.0.0.0/1, 0.0.0.0/3=depth 3 s: 0.0.0.0/3, "
        + "128.0.0.0/1=depth 1 t: 128.0.0.0/1}", trie + "");

    assertEquals("[depth 1 s: 0.0.0.0/1, depth 3 s: 0.0.0.0/3, depth 1 t: 128.0.0.0/1]",
        trie.values() + "");

    assertEquals("[0.0.0.0/1, 0.0.0.0/3, 128.0.0.0/1]", trie.keySet() + "");

    assertEquals(
        "[0.0.0.0/1=depth 1 s: 0.0.0.0/1, 0.0.0.0/3=depth 3 s: 0.0.0.0/3, 128.0.0.0/1=depth 1 t: 128.0.0.0/1]",
        trie.entrySet() + "");


    assertEquals("null=null", trie.root + "");


    assertEquals("0.0.0.0/1", AbstractBinaryTrie.resolveKey(trie.root.left, trie) + "");

    // assertEquals("0.0.0.0/2", Cidr4Trie.resolveKey(trie.root.left.left, trie) + "");

    assertEquals("0.0.0.0/3", AbstractBinaryTrie.resolveKey(trie.root.left.left.left, trie) + "");

    assertEquals("128.0.0.0/1", AbstractBinaryTrie.resolveKey(trie.root.right, trie) + "");

    assertEquals(null, trie.root.right.right);


    assertEquals("depth 1 s: 0.0.0.0/1", trie.root.left.value + "");

    assertEquals(null, trie.root.left.left.value);

    assertEquals("depth 3 s: 0.0.0.0/3", trie.root.left.left.left.value + "");

    assertEquals("depth 1 t: 128.0.0.0/1", trie.root.right.value + "");

    assertEquals(null, trie.root.right.right);


    trie.clear();


    assertEquals(0, trie.size());

    assertEquals("{}", trie + "");

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
