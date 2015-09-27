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
      assertEquals(fromString.get(i).getSortableInteger(), ips[i][3]);
    }
  }

  @Test
  public void testGetBinaryInteger() {
    for (int i = 0; i < ips.length; ++i) {
      assertEquals(fromString.get(i).getBinaryInteger(), ips[i][2]);
    }
  }

  @Test
  public void testGetAddress() {
    for (int i = 0; i < ips.length; ++i) {
      assertEquals(fromString.get(i).getAddress(), ips[i][0]);
    }
  }

  @Test
  public void testGetCidr() {
    for (int i = 0; i < ips.length; ++i) {
      assertEquals(fromString.get(i).getCidr().getCidrSignature(), ips[i][0] + "/32");
    }
  }

  @Test
  public void testGetLowestContainingCidr() {
    for (int i = 0; i < ips.length; ++i) {
      assertEquals(fromString.get(i).getLowestContainingCidr(32).getCidrSignature(),
          ips[i][0] + "/32");

      assertEquals(fromString.get(i).getLowestContainingCidr(24).getCidrSignature(),
          ((String) ips[i][0]).substring(0, ((String) ips[i][0]).lastIndexOf('.')) + ".0/24");

      assertEquals(fromString.get(i).getLowestContainingCidr(16).getCidrSignature(),
          ((String) ips[i][0]).substring(0,
              ((String) ips[i][0]).indexOf('.', 1 + ((String) ips[i][0]).indexOf('.')))
              + ".0.0/16");

      assertEquals(fromString.get(i).getLowestContainingCidr(8).getCidrSignature(),
          ((String) ips[i][0]).substring(0, ((String) ips[i][0]).indexOf('.')) + ".0.0.0/8");
    }

    assertEquals(new Ip4("192.168.211.251").getLowestContainingCidr(31).getCidrSignature(),
        "192.168.211.250/31");

    assertEquals(new Ip4("192.168.211.251").getLowestContainingCidr(30).getCidrSignature(),
        "192.168.211.248/30");

    assertEquals(new Ip4("192.168.211.251").getLowestContainingCidr(29).getCidrSignature(),
        "192.168.211.248/29");

    assertEquals(new Ip4("192.168.211.251").getLowestContainingCidr(28).getCidrSignature(),
        "192.168.211.240/28");

    assertEquals(new Ip4("192.168.211.251").getLowestContainingCidr(25).getCidrSignature(),
        "192.168.211.128/25");

    assertEquals(new Ip4("192.168.211.251").getLowestContainingCidr(17).getCidrSignature(),
        "192.168.128.0/17");

    assertEquals(new Ip4("192.168.211.251").getLowestContainingCidr(9).getCidrSignature(),
        "192.128.0.0/9");

    assertEquals(new Ip4("192.168.211.251").getLowestContainingCidr(1).getCidrSignature(),
        "128.0.0.0/1");

    assertEquals(new Ip4("127.255.255.255").getLowestContainingCidr(1).getCidrSignature(),
        "0.0.0.0/1");
  }

  @Test
  public void testGetInetAddress() throws UnknownHostException {
    for (int i = 0; i < ips.length; ++i) {
      assertEquals(fromString.get(i).getInetAddress().getHostAddress(), ips[i][0]);
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
      assertEquals(ordered1.get(i).toString(), ipsInOrder[i]);
    }

    Collections.shuffle(unordered);
    final Set<Ip4> ordered2 = new TreeSet<>(unordered);
    int i = 0;
    for (final Ip4 ip : ordered2) {
      assertEquals(ip.toString(), ipsInOrder[i++]);
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
