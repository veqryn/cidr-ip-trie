/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.github.veqryn.collect;

import static com.github.veqryn.net.TestUtil.cidrsInOrder;
import static org.junit.Assert.assertEquals;

import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;

import org.junit.Test;

import com.github.veqryn.net.Cidr4;
import com.github.veqryn.net.TestUtil;

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
  public void testNullAllowed() {
    final Cidr4Trie<String> trie = new Cidr4Trie<>();
    assertEquals(null, trie.get(null));
    assertEquals(false, trie.containsKey(null));
    assertEquals(false, trie.containsValue(null));
    assertEquals(null, trie.remove(null));
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

    for (final Entry<Cidr4, String> entry : trie.entrySet()) {
      System.out.println(entry);
    }

    int i = 0;
    for (final Entry<Cidr4, String> entry : trie.entrySet()) {
      assertEquals(cidrsInOrder[i++], entry.getKey().getCidrSignature());
    }

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
    // assertEquals("depth 3 s: 0.0.0.0/3", trie.get(s3));
    assertEquals("depth 1 t: 128.0.0.0/1", trie.get(t1));
    // assertEquals("depth 3 t: 224.0.0.0/3", trie.get(t3));

    // assertEquals("[depth 1 s: 0.0.0.0/1]", trie.suffixValues(s1)+ "");
    // assertEquals("[depth 1 s: 0.0.0.0/1, depth 3 s: 0.0.0.0/3]",
    // trie.suffixValues(s3)+ "");
    // assertEquals("[depth 1 t: 128.0.0.0/1]", trie.suffixValues(t1)+ "");
    // assertEquals("[depth 1 t: 128.0.0.0/1, depth 3 t: 224.0.0.0/3]",
    // trie.suffixValues(t3)+ "");

    assertEquals("null=null", trie.root + "");


    assertEquals("0.0.0.0/1", trie.root.left.getKey() + "");

    // assertEquals("0.0.0.0/2", trie.root.left.left.getKey() + "");

    assertEquals("0.0.0.0/3", trie.root.left.left.left.getKey() + "");

    assertEquals("128.0.0.0/1", trie.root.right.getKey() + "");

    // assertEquals("192.0.0.0/2", trie.root.right.right.getKey() + "");

    assertEquals("224.0.0.0/3", trie.root.right.right.right.getKey() + "");


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


    assertEquals("0.0.0.0/1", trie.root.left.getKey() + "");

    // assertEquals("0.0.0.0/2", trie.root.left.left.getKey() + "");

    assertEquals("0.0.0.0/3", trie.root.left.left.left.getKey() + "");

    assertEquals("128.0.0.0/1", trie.root.right.getKey() + "");

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
