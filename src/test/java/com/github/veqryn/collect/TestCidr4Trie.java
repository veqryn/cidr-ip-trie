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
import java.util.Set;
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

  // TODO: all of these tests will be refactored and simplified,
  // after the AbstractBinaryTree class has itself been refactored.
  // This is just enough to get me through the refactoring phase.


  @Test
  public void testClone() {

    final Cidr4 s1 = new Cidr4(0, 1); // zeroes
    final Cidr4 s3 = new Cidr4(0, 3); // zeroes

    final Cidr4 t1 = new Cidr4(-1, 1); // ones
    final Cidr4 t3 = new Cidr4(-1, 3); // ones

    final Cidr4Trie<String> trie = new Cidr4Trie<>();
    final KeyCodec<Cidr4> codec = trie.codec;

    trie.put(s1, "depth 1 s: " + s1);
    trie.put(s3, "depth 3 s: " + s3);

    trie.put(t1, "depth 1 t: " + t1);
    trie.put(t3, "depth 3 t: " + t3);

    final AbstractBinaryTrie<Cidr4, String> trie2 = trie.clone();
    final KeyCodec<Cidr4> codec2 = trie2.codec;
    {
      assertEquals(codec, codec2);
      assertEquals(trie.size(), trie2.size());

      final Iterator<Entry<Cidr4, String>> iter = trie.entrySet().iterator();
      final Iterator<Entry<Cidr4, String>> iter2 = trie.entrySet().iterator();
      for (int i = 0; i < trie.size(); ++i) {
        assertEquals(iter.next(), iter2.next());
      }
      assertEquals(trie, trie2);
    }
    trie.put(t3, "not equal");
    {
      assertEquals(trie.size(), trie2.size());
      assertNotEquals(trie, trie2);
    }
    trie.put(t3, "depth 3 t: " + t3);
    {
      assertEquals(trie.size(), trie2.size());
      assertEquals(trie, trie2);
    }
    trie2.remove(t3);
    {
      assertEquals(trie.size(), trie2.size() + 1);
      assertNotEquals(trie, trie2);
    }
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

  @SuppressWarnings("unchecked")
  @Test
  public void testWhatever() throws ClassNotFoundException, IOException {

    Cidr4Trie<String> trie = new Cidr4Trie<>(false, false);

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
    System.out.println(trie.valueLongestPrefixOf(new Cidr4("128.0.0.0/10"), true));
    System.out.println();
    for (final String value : values) {
      System.out.println(value);
    }
    System.out.println();
    for (final String value : trie.valuesPrefixOf(new Cidr4("128.0.0.4/32"), true)) {
      System.out.println(value);
    }
    System.out.println();
    for (final String value : trie.valuesPrefixedBy(new Cidr4("128.0.0.0/24"), true)) {
      System.out.println(value);
    }
    System.out.println();

    final NavigableTrie<Cidr4, String> reversed = trie.descendingMap();
    System.out.println(reversed.size());
    final Set<Entry<Cidr4, String>> reversedEntries = reversed.entrySet();
    System.out.println(reversedEntries.size());
    System.out.println();
    final Collection<String> reversedValues = reversed.values();
    System.out.println(reversed.valueLongestPrefixOf(new Cidr4("128.0.0.0/10"), true));
    System.out.println();
    for (final String value : reversedValues) {
      System.out.println(value);
    }
    System.out.println();
    for (final Entry<Cidr4, String> value : reversed.entrySet()) {
      System.out.println(value);
    }
    System.out.println();
    byte[] bytes = TestingUtil.pickle(trie);
    // Files.write(Paths.get("key.txt"), bytes);
    final Cidr4Trie<String> other = TestingUtil.unpickle(bytes, Cidr4Trie.class);
    assertEquals(trie, other);
    assertEquals(trie.hashCode(), other.hashCode());

    other.setCacheKeys(true);
    other.setWriteKeys(true);
    bytes = TestingUtil.pickle(other);
    trie = TestingUtil.unpickle(bytes, Cidr4Trie.class);
    assertEquals(trie, other);
    assertEquals(trie.hashCode(), other.hashCode());
  }

  @Test
  public void testEquality() {

    final Cidr4 s1 = new Cidr4(0, 1); // zeroes
    final Cidr4 s3 = new Cidr4(0, 3); // zeroes

    final Cidr4 t1 = new Cidr4(-1, 1); // ones
    final Cidr4 t3 = new Cidr4(-1, 3); // ones

    final Set<Cidr4> set = new TreeSet<>();
    final Cidr4Trie<String> trie = new Cidr4Trie<>();

    for (int i = 0; i < 1; ++i) {

      set.add(s1);
      set.add(s3);
      set.add(t1);
      set.add(t3);

      trie.put(s1, "depth 1 s: " + s1);
      trie.put(s3, "depth 3 s: " + s3);

      trie.put(t1, "depth 1 t: " + t1);
      trie.put(t3, "depth 3 t: " + t3);
    }

    assertEquals(4, set.size());
    assertEquals(4, trie.size());

    final Iterator<Cidr4> iter = trie.keySet().iterator();
    for (final Cidr4 cidr : set) {
      assertEquals(cidr, iter.next());
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
        trie.valuesPrefixOf(s3, true) + "");
    // assertEquals("[depth 1 t: 128.0.0.0/1]", trie.getAll(t1) + "");
    assertEquals("[depth 1 t: 128.0.0.0/1, depth 3 t: 224.0.0.0/3]",
        trie.valuesPrefixOf(t3, true) + "");

    assertEquals("null=null", trie.root + "");


    assertEquals("0.0.0.0/1", AbstractBinaryTrie.resolveKey(trie.root.left, trie) + "");

    // assertEquals("0.0.0.0/2", Cidr4Trie.resolveKey(trie.root.left.left, trie) + "");

    assertEquals("0.0.0.0/3", AbstractBinaryTrie.resolveKey(trie.root.left.left.left, trie) + "");

    assertEquals("128.0.0.0/1", AbstractBinaryTrie.resolveKey(trie.root.right, trie) + "");

    // assertEquals("192.0.0.0/2", Cidr4Trie.resolveKey(trie.root.right.right, trie) + "");

    assertEquals("224.0.0.0/3",
        AbstractBinaryTrie.resolveKey(trie.root.right.right.right, trie) + "");


    assertEquals("depth 1 s: 0.0.0.0/1", trie.root.left.getValue() + "");

    assertEquals(null, trie.root.left.left.getValue());

    assertEquals("depth 3 s: 0.0.0.0/3", trie.root.left.left.left.getValue() + "");

    assertEquals("depth 1 t: 128.0.0.0/1", trie.root.right.getValue() + "");

    assertEquals(null, trie.root.right.right.getValue());

    assertEquals("depth 3 t: 224.0.0.0/3", trie.root.right.right.right.getValue() + "");


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


    assertEquals("depth 1 s: 0.0.0.0/1", trie.root.left.getValue() + "");

    assertEquals(null, trie.root.left.left.getValue());

    assertEquals("depth 3 s: 0.0.0.0/3", trie.root.left.left.left.getValue() + "");

    assertEquals("depth 1 t: 128.0.0.0/1", trie.root.right.getValue() + "");

    assertEquals(null, trie.root.right.right);


    trie.clear();


    assertEquals(0, trie.size());

    assertEquals("{}", trie + "");

  }

}
