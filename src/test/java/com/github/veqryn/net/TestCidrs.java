/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.github.veqryn.net;

import static com.github.veqryn.net.TestUtil.ips;
import static org.junit.Assert.assertEquals;

import org.junit.Test;

/**
 * Tests for the Cidrs utility class
 */
public class TestCidrs {

  @Test
  public void testGetLowestContainingCidrForRangeIp() {

    for (final Object[] ip : ips) {

      assertEquals(ip[0] + "/32",
          Cidrs.getLowestContainingCidrForRange(
              (int) ip[2], (int) ip[2], 32, true).getCidrSignature());

      assertEquals(ip[0] + "/32",
          Cidrs.getLowestContainingCidrForRange(
              (int) ip[3], (int) ip[3], 32, false).getCidrSignature());

      assertEquals(((String) ip[0]).substring(0, ((String) ip[0]).lastIndexOf('.')) + ".0/24",
          Cidrs.getLowestContainingCidrForRange(
              (int) ip[2], (int) ip[2], 24, true).getCidrSignature());

      assertEquals(((String) ip[0]).substring(0, ((String) ip[0]).lastIndexOf('.')) + ".0/24",
          Cidrs.getLowestContainingCidrForRange(
              (int) ip[3], (int) ip[3], 24, false).getCidrSignature());

      assertEquals(((String) ip[0]).substring(0,
          ((String) ip[0]).indexOf('.', 1 + ((String) ip[0]).indexOf('.')))
          + ".0.0/16",
          Cidrs.getLowestContainingCidrForRange(
              (int) ip[2], (int) ip[2], 16, true).getCidrSignature());

      assertEquals(((String) ip[0]).substring(0,
          ((String) ip[0]).indexOf('.', 1 + ((String) ip[0]).indexOf('.')))
          + ".0.0/16",
          Cidrs.getLowestContainingCidrForRange(
              (int) ip[3], (int) ip[3], 16, false).getCidrSignature());

      assertEquals(((String) ip[0]).substring(0, ((String) ip[0]).indexOf('.')) + ".0.0.0/8",
          Cidrs.getLowestContainingCidrForRange(
              (int) ip[2], (int) ip[2], 8, true).getCidrSignature());

      assertEquals(((String) ip[0]).substring(0, ((String) ip[0]).indexOf('.')) + ".0.0.0/8",
          Cidrs.getLowestContainingCidrForRange(
              (int) ip[3], (int) ip[3], 8, false).getCidrSignature());
    }

    assertEquals("192.168.211.251/32",
        Cidrs.getLowestContainingCidrForRange(
            (int) 3232289787L, (int) 3232289787L, 32, true).getCidrSignature());

    assertEquals("192.168.211.250/31",
        Cidrs.getLowestContainingCidrForRange(
            (int) 3232289787L, (int) 3232289787L, 31, true).getCidrSignature());

    assertEquals("192.168.211.248/30",
        Cidrs.getLowestContainingCidrForRange(
            (int) 3232289787L, (int) 3232289787L, 30, true).getCidrSignature());

    assertEquals("192.168.211.248/29",
        Cidrs.getLowestContainingCidrForRange(
            (int) 3232289787L, (int) 3232289787L, 29, true).getCidrSignature());

    assertEquals("192.168.211.240/28",
        Cidrs.getLowestContainingCidrForRange(
            (int) 3232289787L, (int) 3232289787L, 28, true).getCidrSignature());

    assertEquals("192.168.211.128/25",
        Cidrs.getLowestContainingCidrForRange(
            (int) 3232289787L, (int) 3232289787L, 25, true).getCidrSignature());

    assertEquals("192.168.128.0/17",
        Cidrs.getLowestContainingCidrForRange(
            (int) 3232289787L, (int) 3232289787L, 17, true).getCidrSignature());

    assertEquals("192.128.0.0/9",
        Cidrs.getLowestContainingCidrForRange(
            (int) 3232289787L, (int) 3232289787L, 9, true).getCidrSignature());

    assertEquals("128.0.0.0/1",
        Cidrs.getLowestContainingCidrForRange(
            (int) 3232289787L, (int) 3232289787L, 1, true).getCidrSignature());

    assertEquals("0.0.0.0/1",
        Cidrs.getLowestContainingCidrForRange(
            2147483647, 2147483647, 1, true).getCidrSignature());
  }

  @Test
  public void testGetLowestContainingCidrForRangeCidr() {

    // This one actually caught a bug, because 3232289781L to 3232289783L
    // is 192.168.211.245--192.168.211.247, and the widest possible netmask
    // which contains both is 30, which if re-applied to the IP range
    // makes it become 192.168.211.244--192.168.211.247

    assertEquals("192.168.211.244/30",
        Cidrs.getLowestContainingCidrForRange(
            (int) 3232289781L, (int) 3232289783L, 32, true).getCidrSignature());

    assertEquals("192.168.211.244/30",
        Cidrs.getLowestContainingCidrForRange(
            (int) 3232289781L, (int) 3232289783L, 31, true).getCidrSignature());

    assertEquals("192.168.211.244/30",
        Cidrs.getLowestContainingCidrForRange(
            (int) 3232289781L, (int) 3232289783L, 30, true).getCidrSignature());

    assertEquals("192.168.211.240/29",
        Cidrs.getLowestContainingCidrForRange(
            (int) 3232289781L, (int) 3232289783L, 29, true).getCidrSignature());

    assertEquals("192.168.211.240/28",
        Cidrs.getLowestContainingCidrForRange(
            (int) 3232289781L, (int) 3232289783L, 28, true).getCidrSignature());

    assertEquals("192.168.211.128/25",
        Cidrs.getLowestContainingCidrForRange(
            (int) 3232289781L, (int) 3232289783L, 25, true).getCidrSignature());

    assertEquals("192.168.128.0/17",
        Cidrs.getLowestContainingCidrForRange(
            (int) 3232289781L, (int) 3232289783L, 17, true).getCidrSignature());

    assertEquals("192.128.0.0/9",
        Cidrs.getLowestContainingCidrForRange(
            (int) 3232289781L, (int) 3232289783L, 9, true).getCidrSignature());

    assertEquals("128.0.0.0/1",
        Cidrs.getLowestContainingCidrForRange(
            (int) 3232289781L, (int) 3232289783L, 1, true).getCidrSignature());

    assertEquals("0.0.0.0/0",
        Cidrs.getLowestContainingCidrForRange(
            1, Integer.MIN_VALUE, 32, true).getCidrSignature());

    assertEquals("1.49.32.0/19",
        Cidrs.getLowestContainingCidrForRange(
            19999900, 20001000, 32, true).getCidrSignature());

    assertEquals("1.49.0.0/18",
        Cidrs.getLowestContainingCidrForRange(
            19999900, 20001000, 18, true).getCidrSignature());
  }

  @Test
  public void testGetDifferenceNetmask() {
    assertEquals(Integer.MIN_VALUE, Cidrs.getDifferenceNetmask(0x80000000, 0xffffffff));
    assertEquals(-1, Cidrs.getDifferenceNetmask(0, 0));
    assertEquals(-1, Cidrs.getDifferenceNetmask(147483648, 147483648));
    assertEquals(Integer.MIN_VALUE, Cidrs.getDifferenceNetmask(1000, Integer.MAX_VALUE));
    assertEquals(-8192, Cidrs.getDifferenceNetmask(19999900, 20001000));
    assertEquals(-4, Cidrs.getDifferenceNetmask((int) 3232289781L, (int) 3232289783L));
  }

}
