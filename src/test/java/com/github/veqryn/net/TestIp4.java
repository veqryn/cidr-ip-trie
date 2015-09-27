/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.github.veqryn.net;

import static com.github.veqryn.net.TestUtil.ips;
import static com.github.veqryn.net.TestUtil.ipsInOrder;
import static com.github.veqryn.net.TestUtil.pickle;
import static com.github.veqryn.net.TestUtil.unpickle;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Tests for the Ip4 class
 */
public class TestIp4 {
  private static List<Ip4> fromString;
  private static List<Ip4> fromArray;
  private static List<Ip4> fromInt;
  private static List<Ip4> fromSortable;
  private static List<Ip4> fromCopy;

  @BeforeClass
  public static void setup() {
    fromString = getIpsFromString();
    fromArray = getIpsFromIntArray();
    fromInt = getIpsFromBinaryInt();
    fromSortable = getIpsFromSortableInt();
    fromCopy = copyIps(fromString);
  }

  @Test
  public void testGetSortableInteger() {
    for (int i = 0; i < ips.length; ++i) {
      assertEquals(ips[i][3], fromString.get(i).getSortableInteger());
    }
  }

  @Test
  public void testGetBinaryInteger() {
    for (int i = 0; i < ips.length; ++i) {
      assertEquals(ips[i][2], fromString.get(i).getBinaryInteger());
    }
  }

  @Test
  public void testGetAddress() {
    for (int i = 0; i < ips.length; ++i) {
      assertEquals(ips[i][0], fromString.get(i).getAddress());
    }
  }

  @Test
  public void testGetCidr() {
    for (int i = 0; i < ips.length; ++i) {
      assertEquals(ips[i][0] + "/32", fromString.get(i).getCidr().getCidrSignature());
    }
  }

  @Test
  public void testGetLowestContainingCidr() {
    for (int i = 0; i < ips.length; ++i) {
      assertEquals(ips[i][0] + "/32",
          fromString.get(i).getLowestContainingCidr(32).getCidrSignature());

      assertEquals(
          ((String) ips[i][0]).substring(0, ((String) ips[i][0]).lastIndexOf('.')) + ".0/24",
          fromString.get(i).getLowestContainingCidr(24).getCidrSignature());

      assertEquals(((String) ips[i][0]).substring(0,
          ((String) ips[i][0]).indexOf('.', 1 + ((String) ips[i][0]).indexOf('.')))
          + ".0.0/16",
          fromString.get(i).getLowestContainingCidr(16).getCidrSignature());

      assertEquals(
          ((String) ips[i][0]).substring(0, ((String) ips[i][0]).indexOf('.')) + ".0.0.0/8",
          fromString.get(i).getLowestContainingCidr(8).getCidrSignature());
    }

    assertEquals("192.168.211.250/31",
        new Ip4("192.168.211.251").getLowestContainingCidr(31).getCidrSignature());

    assertEquals("192.168.211.248/30",
        new Ip4("192.168.211.251").getLowestContainingCidr(30).getCidrSignature());

    assertEquals("192.168.211.248/29",
        new Ip4("192.168.211.251").getLowestContainingCidr(29).getCidrSignature());

    assertEquals("192.168.211.240/28",
        new Ip4("192.168.211.251").getLowestContainingCidr(28).getCidrSignature());

    assertEquals("192.168.211.128/25",
        new Ip4("192.168.211.251").getLowestContainingCidr(25).getCidrSignature());

    assertEquals("192.168.128.0/17",
        new Ip4("192.168.211.251").getLowestContainingCidr(17).getCidrSignature());

    assertEquals("192.128.0.0/9",
        new Ip4("192.168.211.251").getLowestContainingCidr(9).getCidrSignature());

    assertEquals("128.0.0.0/1",
        new Ip4("192.168.211.251").getLowestContainingCidr(1).getCidrSignature());

    assertEquals("0.0.0.0/1",
        new Ip4("127.255.255.255").getLowestContainingCidr(1).getCidrSignature());
  }

  @Test
  public void testGetInetAddress() throws UnknownHostException {
    for (int i = 0; i < ips.length; ++i) {
      assertEquals(ips[i][0], fromString.get(i).getInetAddress().getHostAddress());
    }
  }

  @Test
  public void testSerializability() throws ClassNotFoundException, IOException {
    for (final Ip4 ip : fromString) {
      final byte[] bytes = pickle(ip);
      final Ip4 other = unpickle(bytes, Ip4.class);
      assertEquals(ip, other);
      assertEquals(ip.hashCode(), other.hashCode());
    }
  }

  @Test
  public void testComparibility() {
    final List<Ip4> unordered = new ArrayList<Ip4>(fromString);

    final List<Ip4> ordered1 = new ArrayList<>(unordered);
    Collections.sort(ordered1);
    for (int i = 0; i < ips.length; ++i) {
      assertEquals(ipsInOrder[i], ordered1.get(i).toString());
    }

    Collections.shuffle(unordered);
    final Set<Ip4> ordered2 = new TreeSet<>(unordered);
    int i = 0;
    for (final Ip4 ip : ordered2) {
      assertEquals(ipsInOrder[i++], ip.toString());
    }
  }

  @Test
  public void testEquality() {
    for (int i = 0; i < ips.length; ++i) {
      assertEquals(fromString.get(i), fromArray.get(i));
      assertEquals(fromString.get(i), fromInt.get(i));
      assertEquals(fromString.get(i), fromSortable.get(i));
      assertEquals(fromString.get(i), fromCopy.get(i));
      assertEquals(fromArray.get(i), fromInt.get(i));
      assertEquals(fromArray.get(i), fromSortable.get(i));
      assertEquals(fromArray.get(i), fromCopy.get(i));
      assertEquals(fromInt.get(i), fromSortable.get(i));
      assertEquals(fromInt.get(i), fromCopy.get(i));
      assertEquals(fromSortable.get(i), fromCopy.get(i));
    }
  }

  @Test
  public void testNotEqual() {
    for (int i = 0; i < ips.length; ++i) {
      for (int j = 0; j < ips.length; ++j) {
        if (i == j) {
          continue;
        }
        // equals
        assertNotEquals(fromString.get(i), fromString.get(j));
        // compareTo
        assertNotEquals(0, fromString.get(i).compareTo(fromString.get(j)));
      }
    }
  }

  /**
   * Implicitly tests the constructor with parameters:
   * String
   *
   * @return List<Ip4>
   */
  protected static List<Ip4> getIpsFromString() {
    final List<Ip4> rval = new ArrayList<>();
    for (final Object[] ip : ips) {
      rval.add(new Ip4((String) ip[0]));
    }
    return rval;
  }

  /**
   * Implicitly tests the constructor with parameters:
   * int
   * int
   * int
   * int
   *
   * @return List<Ip4>
   */
  protected static List<Ip4> getIpsFromIntArray() {
    final List<Ip4> rval = new ArrayList<>();
    for (final Object[] ip : ips) {
      rval.add(new Ip4(
          ((int[]) ip[1])[0],
          ((int[]) ip[1])[1],
          ((int[]) ip[1])[2],
          ((int[]) ip[1])[3]));
    }
    return rval;
  }

  /**
   * Implicitly tests the constructor with parameters:
   * int
   *
   * @return List<Ip4>
   */
  protected static List<Ip4> getIpsFromBinaryInt() {
    final List<Ip4> rval = new ArrayList<>();
    for (final Object[] ip : ips) {
      rval.add(new Ip4((int) ip[2]));
    }
    return rval;
  }

  /**
   * Implicitly tests the constructor with parameters:
   * int
   * boolean
   *
   * @return List<Ip4>
   */
  protected static List<Ip4> getIpsFromSortableInt() {
    final List<Ip4> rval = new ArrayList<>();
    for (final Object[] ip : ips) {
      rval.add(new Ip4((int) ip[3], false));
    }
    return rval;
  }

  /**
   * Implicitly tests the constructor with parameters:
   * Ip4
   *
   * @return List<Ip4>
   */
  protected static List<Ip4> copyIps(final List<Ip4> otherIps) {
    final List<Ip4> rval = new ArrayList<>();
    for (final Ip4 ip : otherIps) {
      rval.add(new Ip4(ip));
    }
    return rval;
  }

}
