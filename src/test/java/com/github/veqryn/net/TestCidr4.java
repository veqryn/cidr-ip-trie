/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.github.veqryn.net;

import static com.github.veqryn.net.Ips.format;
import static com.github.veqryn.net.Ips.toArray;
import static com.github.veqryn.net.TestUtil.cidrs;
import static com.github.veqryn.net.TestUtil.pickle;
import static com.github.veqryn.net.TestUtil.unpickle;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Tests for the Ips utility class
 */
public class TestCidr4 {

  private static List<Cidr4> fromString;
  private static List<Cidr4> fromLowStringWithNetmask;
  private static List<Cidr4> fromHighStringWithNetmask;
  private static List<Cidr4> fromLowBinaryWithNetmask;
  private static List<Cidr4> fromHighBinaryWithNetmask;
  private static List<Cidr4> fromLowSortableWithNetmask;
  private static List<Cidr4> fromHighSortableWithNetmask;
  private static List<Cidr4> fromBinaryIntToInt;
  private static List<Cidr4> fromSortableIntToInt;
  private static List<Cidr4> cidrsFromOctetsAndMask;
  private static List<Cidr4> fromOtherCidrs;

  @BeforeClass
  public static void setup() {
    fromString = getCidrsFromString();
    fromLowStringWithNetmask = getCidrsFromLowStringWithNetmask();
    fromHighStringWithNetmask = getCidrsFromHighStringWithNetmask();
    fromLowBinaryWithNetmask = getCidrsFromLowBinaryWithNetmask();
    fromHighBinaryWithNetmask = getCidrsFromHighBinaryWithNetmask();
    fromLowSortableWithNetmask = getCidrsFromLowSortableWithNetmask();
    fromHighSortableWithNetmask = getCidrsFromHighSortableWithNetmask();
    fromBinaryIntToInt = getCidrsFromBinaryIntToInt();
    fromSortableIntToInt = getCidrsFromSortableIntToInt();
    cidrsFromOctetsAndMask = getCidrsFromOctetsAndMask();
    fromOtherCidrs = getCidrsFromOtherCidrs(fromString);
  }

  @Test
  public void testGetCidrSignature() {
    for (int i = 0; i < cidrs.length; ++i) {
      assertEquals(cidrs[i][9], fromString.get(i).getCidrSignature());
    }
  }

  @Test
  public void testGetLowAddress() {
    for (int i = 0; i < cidrs.length; ++i) {
      assertEquals(cidrs[i][10], fromString.get(i).getLowAddress(true));
    }
    assertEquals("0.0.0.0", new Cidr4("128.0.0.4/32").getLowAddress(false));
    assertEquals("0.0.0.0", new Cidr4("128.0.0.4/31").getLowAddress(false));
    assertEquals("128.0.0.5", new Cidr4("128.0.0.4/30").getLowAddress(false));
    assertEquals("128.0.0.1", new Cidr4("128.0.0.4/29").getLowAddress(false));
  }

  @Test
  public void testGetLowSortableInteger() {
    for (int i = 0; i < cidrs.length; ++i) {
      assertEquals(cidrs[i][15], fromString.get(i).getLowSortableInteger(true));
    }
    assertEquals(Integer.MIN_VALUE, new Cidr4("128.0.0.4/32").getLowSortableInteger(false));
    assertEquals(Integer.MIN_VALUE, new Cidr4("128.0.0.4/31").getLowSortableInteger(false));
    assertEquals(5, new Cidr4("128.0.0.4/30").getLowSortableInteger(false));
    assertEquals(1, new Cidr4("128.0.0.4/29").getLowSortableInteger(false));
  }

  @Test
  public void testGetLowBinaryInteger() {
    for (int i = 0; i < cidrs.length; ++i) {
      assertEquals(cidrs[i][13], fromString.get(i).getLowBinaryInteger(true));
    }
    assertEquals(0, new Cidr4("128.0.0.4/32").getLowBinaryInteger(false));
    assertEquals(0, new Cidr4("128.0.0.4/31").getLowBinaryInteger(false));
    assertEquals(Integer.MIN_VALUE + 5, new Cidr4("128.0.0.4/30").getLowBinaryInteger(false));
    assertEquals(Integer.MIN_VALUE + 1, new Cidr4("128.0.0.4/29").getLowBinaryInteger(false));
  }

  @Test
  public void testGetLowIp() {
    for (int i = 0; i < cidrs.length; ++i) {
      assertEquals(new Ip4((String) cidrs[i][10]), fromString.get(i).getLowIp(true));
    }
    assertEquals(new Ip4(0, true), new Cidr4("128.0.0.4/32").getLowIp(false));
    assertEquals(new Ip4(0, true), new Cidr4("128.0.0.4/31").getLowIp(false));
    assertEquals(new Ip4(Integer.MIN_VALUE + 5, true), new Cidr4("128.0.0.4/30").getLowIp(false));
    assertEquals(new Ip4(Integer.MIN_VALUE + 1, true), new Cidr4("128.0.0.4/29").getLowIp(false));
  }

  @Test
  public void testGetLowCidr() {
    for (int i = 0; i < cidrs.length; ++i) {
      assertEquals(new Cidr4((String) cidrs[i][10], true), fromString.get(i).getLowCidr(true));
    }
    assertEquals(new Cidr4(0, 32), new Cidr4("128.0.0.4/32").getLowCidr(false));
    assertEquals(new Cidr4(0, 32), new Cidr4("128.0.0.4/31").getLowCidr(false));
    assertEquals(new Cidr4(Integer.MIN_VALUE + 5, 32), new Cidr4("128.0.0.4/30").getLowCidr(false));
    assertEquals(new Cidr4(Integer.MIN_VALUE + 1, 32), new Cidr4("128.0.0.4/29").getLowCidr(false));
  }

  @Test
  public void testGetHighAddress() {
    for (int i = 0; i < cidrs.length; ++i) {
      assertEquals(cidrs[i][11], fromString.get(i).getHighAddress(true));
    }
    assertEquals("0.0.0.0", new Cidr4("128.0.0.4/32").getHighAddress(false));
    assertEquals("0.0.0.0", new Cidr4("128.0.0.4/31").getHighAddress(false));
    assertEquals("128.0.0.6", new Cidr4("128.0.0.4/30").getHighAddress(false));
    assertEquals("128.0.0.6", new Cidr4("128.0.0.4/29").getHighAddress(false));
    assertEquals("128.0.0.14", new Cidr4("128.0.0.4/28").getHighAddress(false));
  }

  @Test
  public void testGetHighSortableInteger() {
    for (int i = 0; i < cidrs.length; ++i) {
      assertEquals(cidrs[i][16], fromString.get(i).getHighSortableInteger(true));
    }
    assertEquals(Integer.MIN_VALUE, new Cidr4("128.0.0.4/32").getHighSortableInteger(false));
    assertEquals(Integer.MIN_VALUE, new Cidr4("128.0.0.4/31").getHighSortableInteger(false));
    assertEquals(6, new Cidr4("128.0.0.4/30").getHighSortableInteger(false));
    assertEquals(6, new Cidr4("128.0.0.4/29").getHighSortableInteger(false));
    assertEquals(14, new Cidr4("128.0.0.4/28").getHighSortableInteger(false));
  }

  @Test
  public void testGetHighBinaryInteger() {
    for (int i = 0; i < cidrs.length; ++i) {
      assertEquals(cidrs[i][14], fromString.get(i).getHighBinaryInteger(true));
    }
    assertEquals(0, new Cidr4("128.0.0.4/32").getHighBinaryInteger(false));
    assertEquals(0, new Cidr4("128.0.0.4/31").getHighBinaryInteger(false));
    assertEquals(Integer.MIN_VALUE + 6, new Cidr4("128.0.0.4/30").getHighBinaryInteger(false));
    assertEquals(Integer.MIN_VALUE + 6, new Cidr4("128.0.0.4/29").getHighBinaryInteger(false));
    assertEquals(Integer.MIN_VALUE + 14, new Cidr4("128.0.0.4/28").getHighBinaryInteger(false));
  }

  @Test
  public void testGetHighIp() {
    for (int i = 0; i < cidrs.length; ++i) {
      assertEquals(new Ip4((String) cidrs[i][11]), fromString.get(i).getHighIp(true));
    }
    assertEquals(new Ip4(0, true), new Cidr4("128.0.0.4/32").getHighIp(false));
    assertEquals(new Ip4(0, true), new Cidr4("128.0.0.4/31").getHighIp(false));
    assertEquals(new Ip4(Integer.MIN_VALUE + 6, true), new Cidr4("128.0.0.4/30").getHighIp(false));
    assertEquals(new Ip4(Integer.MIN_VALUE + 6, true), new Cidr4("128.0.0.4/29").getHighIp(false));
    assertEquals(new Ip4(Integer.MIN_VALUE + 14, true), new Cidr4("128.0.0.4/28").getHighIp(false));
  }

  @Test
  public void testGetHighCidr() {
    for (int i = 0; i < cidrs.length; ++i) {
      assertEquals(new Cidr4((String) cidrs[i][11], true), fromString.get(i).getHighCidr(true));
    }
    assertEquals(new Cidr4(0, 32), new Cidr4("128.0.0.4/32").getHighCidr(false));
    assertEquals(new Cidr4(0, 32), new Cidr4("128.0.0.4/31").getHighCidr(false));
    assertEquals(new Cidr4(Integer.MIN_VALUE + 6, 32),
        new Cidr4("128.0.0.4/30").getHighCidr(false));
    assertEquals(new Cidr4(Integer.MIN_VALUE + 6, 32),
        new Cidr4("128.0.0.4/29").getHighCidr(false));
    assertEquals(new Cidr4(Integer.MIN_VALUE + 14, 32),
        new Cidr4("128.0.0.4/28").getHighCidr(false));
  }

  @Test
  public void testGetLowestContainingCidr() {
    // TODO: do this
  }

  @Test
  public void testGetBinaryNetmask() {
    for (int i = 0; i < cidrs.length; ++i) {
      assertEquals(Cidrs.getNetMask((int) cidrs[i][12]), fromString.get(i).getBinaryNetmask());
    }
  }

  @Test
  public void testGetNetmask() {
    for (int i = 0; i < cidrs.length; ++i) {
      assertEquals(format(toArray(Cidrs.getNetMask((int) cidrs[i][12]), true)),
          fromString.get(i).getNetmask());
    }
  }

  @Test
  public void testGetMaskBits() {
    for (int i = 0; i < cidrs.length; ++i) {
      assertEquals(cidrs[i][12], fromString.get(i).getMaskBits());
    }
  }

  @Test
  public void testGetAddressCount() {
    for (int i = 0; i < cidrs.length; ++i) {
      assertEquals(cidrs[i][17], fromString.get(i).getAddressCount(true));
    }
  }

  @Test
  public void testGetAllAddresses() {
    // TODO
  }

  @Test
  public void testGetAllIps() {
    // TODO
  }

  @Test
  public void testIsInRangeString() {
    // TODO
  }

  @Test
  public void testIsInRangeIp() {
    // TODO
  }

  @Test
  public void testIsInRangeInt() {
    // TODO
  }

  @Test
  public void testIsInRangeCidr() {
    // TODO
  }

  @Test
  public void testSerializability() throws ClassNotFoundException, IOException {
    for (final Cidr4 cidr : fromString) {
      final byte[] bytes = pickle(cidr);
      final Cidr4 other = unpickle(bytes, Cidr4.class);
      assertEquals(cidr, other);
      assertEquals(cidr.hashCode(), other.hashCode());
    }
  }

  @Test
  public void testComparability() {
    // TODO
  }

  @Test
  public void testNotEqual() {
    for (int i = 0; i < cidrs.length; ++i) {
      for (int j = 0; j < cidrs.length; ++j) {
        if (i == j
            || cidrs[i][9].equals("0.0.0.0/0")
            || cidrs[i][9].equals("0.0.0.0/1")) {
          continue;
        }
        // equals
        assertNotEquals(fromString.get(i), fromString.get(j));
        // compareTo
        assertNotEquals(0, fromString.get(i).compareTo(fromString.get(j)));
      }
    }
  }

  @Test
  public void testEquality() {
    for (int i = 0; i < cidrs.length; ++i) {
      assertEquals(fromString.get(i), fromLowStringWithNetmask.get(i));
      assertEquals(fromString.get(i), fromHighStringWithNetmask.get(i));
      assertEquals(fromString.get(i), fromLowBinaryWithNetmask.get(i));
      assertEquals(fromString.get(i), fromHighBinaryWithNetmask.get(i));
      assertEquals(fromString.get(i), fromLowSortableWithNetmask.get(i));
      assertEquals(fromString.get(i), fromHighSortableWithNetmask.get(i));
      assertEquals(fromString.get(i), fromBinaryIntToInt.get(i));
      assertEquals(fromString.get(i), fromSortableIntToInt.get(i));
      assertEquals(fromString.get(i), cidrsFromOctetsAndMask.get(i));
      assertEquals(fromString.get(i), fromOtherCidrs.get(i));

      assertEquals(0, fromString.get(i).compareTo(fromLowStringWithNetmask.get(i)));
      assertEquals(0, fromString.get(i).compareTo(fromHighStringWithNetmask.get(i)));
      assertEquals(0, fromString.get(i).compareTo(fromLowBinaryWithNetmask.get(i)));
      assertEquals(0, fromString.get(i).compareTo(fromHighBinaryWithNetmask.get(i)));
      assertEquals(0, fromString.get(i).compareTo(fromLowSortableWithNetmask.get(i)));
      assertEquals(0, fromString.get(i).compareTo(fromHighSortableWithNetmask.get(i)));
      assertEquals(0, fromString.get(i).compareTo(fromBinaryIntToInt.get(i)));
      assertEquals(0, fromString.get(i).compareTo(fromSortableIntToInt.get(i)));
      assertEquals(0, fromString.get(i).compareTo(cidrsFromOctetsAndMask.get(i)));
      assertEquals(0, fromString.get(i).compareTo(fromOtherCidrs.get(i)));
    }
  }


  /**
   * Implicitly tests the constructor with parameters:
   * String
   *
   * @return List<Cidr4>
   */
  protected static List<Cidr4> getCidrsFromString() {
    final List<Cidr4> rval = new ArrayList<>();
    for (final Object[] cidr : cidrs) {
      rval.add(new Cidr4((String) cidr[0]));
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
  protected static List<Cidr4> getCidrsFromLowStringWithNetmask() {
    final List<Cidr4> rval = new ArrayList<>();
    for (final Object[] cidr : cidrs) {
      rval.add(new Cidr4((String) cidr[1],
          Ips.format(Ips.toArray(Cidrs.getNetMask((int) cidr[3]), true))));
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
  protected static List<Cidr4> getCidrsFromHighStringWithNetmask() {
    final List<Cidr4> rval = new ArrayList<>();
    for (final Object[] cidr : cidrs) {
      rval.add(new Cidr4((String) cidr[2],
          Ips.format(Ips.toArray(Cidrs.getNetMask((int) cidr[3]), true))));
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
  protected static List<Cidr4> getCidrsFromLowBinaryWithNetmask() {
    final List<Cidr4> rval = new ArrayList<>();
    for (final Object[] cidr : cidrs) {
      rval.add(new Cidr4((int) cidr[5], (int) cidr[3]));
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
  protected static List<Cidr4> getCidrsFromHighBinaryWithNetmask() {
    final List<Cidr4> rval = new ArrayList<>();
    for (final Object[] cidr : cidrs) {
      rval.add(new Cidr4((int) cidr[6], (int) cidr[3]));
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
  protected static List<Cidr4> getCidrsFromLowSortableWithNetmask() {
    final List<Cidr4> rval = new ArrayList<>();
    for (final Object[] cidr : cidrs) {
      rval.add(new Cidr4((int) cidr[7], false, (int) cidr[3]));
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
  protected static List<Cidr4> getCidrsFromHighSortableWithNetmask() {
    final List<Cidr4> rval = new ArrayList<>();
    for (final Object[] cidr : cidrs) {
      rval.add(new Cidr4((int) cidr[8], false, (int) cidr[3]));
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
   * int
   * int
   * bolean
   *
   * @return List<Cidr4>
   */
  protected static List<Cidr4> getCidrsFromBinaryIntToInt() {
    final List<Cidr4> rval = new ArrayList<>();
    for (final Object[] cidr : cidrs) {
      rval.add(new Cidr4((int) cidr[5], (int) cidr[6], true));
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
  protected static List<Cidr4> getCidrsFromSortableIntToInt() {
    final List<Cidr4> rval = new ArrayList<>();
    for (final Object[] cidr : cidrs) {
      rval.add(new Cidr4((int) cidr[7], (int) cidr[8], false));
    }
    return rval;
  }

  /**
   * Implicitly tests the constructor with parameters:
   * int
   * int
   * int
   * int
   * int
   *
   * @return List<Cidr4>
   */
  protected static List<Cidr4> getCidrsFromOctetsAndMask() {
    final List<Cidr4> rval = new ArrayList<>();
    for (final Object[] cidr : cidrs) {
      rval.add(new Cidr4(
          ((int[]) cidr[4])[0],
          ((int[]) cidr[4])[1],
          ((int[]) cidr[4])[2],
          ((int[]) cidr[4])[3],
          ((int[]) cidr[4])[4]));
    }
    return rval;
  }

}
