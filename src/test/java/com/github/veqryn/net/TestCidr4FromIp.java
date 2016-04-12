/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.github.veqryn.net;

import static com.github.veqryn.net.TestUtil.ips;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import java.util.ArrayList;
import java.util.List;

import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Tests for the Cidr4 class
 *
 * @author Chris Duncan
 */
public class TestCidr4FromIp {

  private static List<Ip4> ip4;

  private static List<Cidr4> cidrsFromIpStringWith32;
  private static List<Cidr4> cidrsFromIpStringWithout32;
  private static List<Cidr4> cidrsFromIpStringNetmask;
  private static List<Cidr4> cidrsFromIpBinaryIntNetmask;
  private static List<Cidr4> cidrsFromIpSortableIntNetmask;
  private static List<Cidr4> cidrsFromOtherCidrs;
  private static List<Cidr4> cidrsFromIp4;
  private static List<Cidr4> cidrsFromIp4ToIp4;
  private static List<Cidr4> cidrsFromIpIntToInt;

  @BeforeClass
  public static void setup() {
    ip4 = getIpsFromString();
    cidrsFromIpStringWith32 = getCidrsFromIpStringWith32();
    cidrsFromIpStringWithout32 = getCidrsFromIpStringWithout32();
    cidrsFromIpStringNetmask = getCidrsFromIpStringNetmask();
    cidrsFromIpBinaryIntNetmask = getCidrsFromIpBinaryIntNetmask();
    cidrsFromIpSortableIntNetmask = getCidrsFromIpSortableIntNetmask();
    cidrsFromOtherCidrs = getCidrsFromOtherCidrs(cidrsFromIpStringWith32);
    cidrsFromIp4 = getCidrsFromIp4();
    cidrsFromIp4ToIp4 = getCidrsFromIp4ToIp4();
    cidrsFromIpIntToInt = getCidrsFromIpIntToInt();
  }

  @Test
  public void testNotEqual() {
    for (int i = 0; i < ips.length; ++i) {
      for (int j = 0; j < ips.length; ++j) {
        if (i == j) {
          continue;
        }
        // equals
        assertNotEquals(cidrsFromIpStringWith32.get(i), cidrsFromIpStringWith32.get(j));
        // compareTo
        assertNotEquals(0,
            cidrsFromIpStringWith32.get(i).compareTo(cidrsFromIpStringWith32.get(j)));
      }
    }
  }

  @Test
  public void testCidrsFromIps() {
    for (int i = 0; i < ips.length; ++i) {
      assertEquals(cidrsFromIpStringWith32.get(i), cidrsFromIpStringWithout32.get(i));
      assertEquals(cidrsFromIpStringWith32.get(i), cidrsFromIpStringNetmask.get(i));
      assertEquals(cidrsFromIpStringWith32.get(i), cidrsFromIpBinaryIntNetmask.get(i));
      assertEquals(cidrsFromIpStringWith32.get(i), cidrsFromIpSortableIntNetmask.get(i));
      assertEquals(cidrsFromIpStringWith32.get(i), cidrsFromOtherCidrs.get(i));
      assertEquals(cidrsFromIpStringWith32.get(i), cidrsFromIp4.get(i));
      assertEquals(cidrsFromIpStringWith32.get(i), cidrsFromIp4ToIp4.get(i));
      assertEquals(cidrsFromIpStringWith32.get(i), cidrsFromIpIntToInt.get(i));

      assertEquals(0, cidrsFromIpStringWith32.get(i).compareTo(cidrsFromIpStringWithout32.get(i)));

      assertEquals(ip4.get(i).getCidr(), cidrsFromIpStringWith32.get(i));

      assertEquals(ip4.get(i).getAddress(), cidrsFromIpStringWith32.get(i).getLowAddress(true));
      assertEquals(ip4.get(i).getAddress(), cidrsFromIpStringWith32.get(i).getHighAddress(true));

      assertEquals(ip4.get(i).getBinaryInteger(),
          cidrsFromIpStringWith32.get(i).getLowBinaryInteger(true));
      assertEquals(ip4.get(i).getBinaryInteger(),
          cidrsFromIpStringWith32.get(i).getHighBinaryInteger(true));

      assertEquals(ip4.get(i).getSortableInteger(),
          cidrsFromIpStringWith32.get(i).getLowSortableInteger(true));
      assertEquals(ip4.get(i).getSortableInteger(),
          cidrsFromIpStringWith32.get(i).getHighSortableInteger(true));

      assertEquals(ip4.get(i), cidrsFromIpStringWith32.get(i).getLowIp(true));
      assertEquals(ip4.get(i), cidrsFromIpStringWith32.get(i).getHighIp(true));

      assertEquals(ip4.get(i).getAddress(),
          cidrsFromIpStringWith32.get(i).getLowCidr(true).getLowAddress(true));
      assertEquals(ip4.get(i).getAddress(),
          cidrsFromIpStringWith32.get(i).getHighCidr(true).getLowAddress(true));

      assertEquals(ip4.get(i).getAddress(),
          cidrsFromIpStringWith32.get(i).getLowCidr(true).getHighAddress(true));
      assertEquals(ip4.get(i).getAddress(),
          cidrsFromIpStringWith32.get(i).getHighCidr(true).getHighAddress(true));
    }
  }


  /**
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
   * String
   *
   * @return List<Cidr4>
   */
  protected static List<Cidr4> getCidrsFromIpStringWith32() {
    final List<Cidr4> rval = new ArrayList<>();
    for (final Object[] ip : ips) {
      rval.add(new Cidr4((String) ip[0] + "/32"));
    }
    return rval;
  }

  /**
   * Implicitly tests the constructor with parameters:
   * String
   * boolean
   *
   * @return List<Cidr4>
   */
  protected static List<Cidr4> getCidrsFromIpStringWithout32() {
    final List<Cidr4> rval = new ArrayList<>();
    for (final Object[] ip : ips) {
      rval.add(new Cidr4((String) ip[0], true));
    }
    return rval;
  }

  /**
   * Implicitly tests the constructor with parameters:
   * String
   * String
   *
   * @return List<Cidr4>
   */
  protected static List<Cidr4> getCidrsFromIpStringNetmask() {
    final List<Cidr4> rval = new ArrayList<>();
    for (final Object[] ip : ips) {
      rval.add(new Cidr4((String) ip[0], "255.255.255.255"));
    }
    return rval;
  }

  /**
   * Implicitly tests the constructor with parameters:
   * int
   * int
   *
   * @return List<Cidr4>
   */
  protected static List<Cidr4> getCidrsFromIpBinaryIntNetmask() {
    final List<Cidr4> rval = new ArrayList<>();
    for (final Object[] ip : ips) {
      rval.add(new Cidr4((int) ip[2], 32));
    }
    return rval;
  }

  /**
   * Implicitly tests the constructor with parameters:
   * int
   * boolean
   * int
   *
   * @return List<Cidr4>
   */
  protected static List<Cidr4> getCidrsFromIpSortableIntNetmask() {
    final List<Cidr4> rval = new ArrayList<>();
    for (final Object[] ip : ips) {
      rval.add(new Cidr4((int) ip[3], false, 32));
    }
    return rval;
  }

  /**
   * Implicitly tests the constructor with parameters:
   * Cidr4
   *
   * @return List<Cidr4>
   */
  protected static List<Cidr4> getCidrsFromOtherCidrs(final List<Cidr4> others) {
    final List<Cidr4> rval = new ArrayList<>();
    for (final Cidr4 cidr : others) {
      rval.add(new Cidr4(cidr));
    }
    return rval;
  }

  /**
   * Implicitly tests the constructor with parameters:
   * Ip4
   *
   * @return List<Cidr4>
   */
  protected static List<Cidr4> getCidrsFromIp4() {
    final List<Cidr4> rval = new ArrayList<>();
    for (final Ip4 ip : getIpsFromString()) {
      rval.add(new Cidr4(ip));
    }
    return rval;
  }

  /**
   * Implicitly tests the constructor with parameters:
   * Ip4
   *
   * @return List<Cidr4>
   */
  protected static List<Cidr4> getCidrsFromIp4ToIp4() {
    final List<Cidr4> rval = new ArrayList<>();
    for (final Ip4 ip : getIpsFromString()) {
      rval.add(new Cidr4(ip, ip));
    }
    return rval;
  }

  /**
   * Implicitly tests the constructor with parameters:
   * int
   * int
   * bolean
   *
   * @return List<Cidr4>
   */
  protected static List<Cidr4> getCidrsFromIpIntToInt() {
    final List<Cidr4> rval = new ArrayList<>();
    for (final Object[] ip : ips) {
      rval.add(new Cidr4((int) ip[3], (int) ip[3], false));
    }
    return rval;
  }

}
