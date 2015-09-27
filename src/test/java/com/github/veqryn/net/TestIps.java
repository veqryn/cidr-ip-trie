/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.github.veqryn.net;

import static com.github.veqryn.net.TestUtil.ips;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import org.junit.Test;

/**
 * Tests for the Ips utility class
 */
public class TestIps {

  @Test
  public void testRangeCheck() {
    // (begin,end]
    assertEquals(1, Ips.rangeCheck(1, 0, 10));
    assertEquals(5, Ips.rangeCheck(5, 0, 10));
    assertEquals(10, Ips.rangeCheck(10, 0, 10));
    try {
      Ips.rangeCheck(0, 0, 10);
      fail("Expected an IllegalArgumentException");
    } catch (final IllegalArgumentException e) {
    }
    try {
      Ips.rangeCheck(11, 0, 10);
      fail("Expected an IllegalArgumentException");
    } catch (final IllegalArgumentException e) {
    }
  }

  @Test
  public void testToIntegerWithIntArray() {
    for (final Object[] ip : ips) {
      assertEquals("To binary int failed for " + ip[0],
          ip[2], Ips.toInteger((int[]) ip[1], true));
      assertEquals("To sortable int failed for " + ip[0],
          ip[3], Ips.toInteger((int[]) ip[1], false));
    }
  }

  @Test
  public void testToIntegerWithString() {
    for (final Object[] ip : ips) {
      assertEquals("To binary int failed for " + ip[0],
          ip[2], Ips.toInteger((String) ip[0], true));
      assertEquals("To sortable int failed for " + ip[0],
          ip[3], Ips.toInteger((String) ip[0], false));
    }
  }

  @Test(expected = IllegalArgumentException.class)
  public void testToIntegerWithBadString() {
    Ips.toInteger("192.168.1.0/24", true);
  }

  @Test
  public void testGetBinaryOctets() {
    for (final Object[] ip : ips) {
      assertEquals("To octet byte 1 failed for " + ip[0],
          (byte) (int) ip[5], Ips.getBinaryOctet1((int) ip[2], true));
      assertEquals("To octet byte 1 failed for sortable " + ip[0],
          (byte) (int) ip[5], Ips.getBinaryOctet1((int) ip[3], false));

      assertEquals("To octet byte 2 failed for " + ip[0],
          (byte) (int) ip[6], Ips.getBinaryOctet2((int) ip[2]));

      assertEquals("To octet byte 3 failed for " + ip[0],
          (byte) (int) ip[7], Ips.getBinaryOctet3((int) ip[2]));

      assertEquals("To octet byte 4 failed for " + ip[0],
          (byte) (int) ip[8], Ips.getBinaryOctet4((int) ip[2]));
    }
  }

  @Test
  public void testToArray() {
    for (final Object[] ip : ips) {
      assertArrayEquals("To array failed for " + ip[0],
          (int[]) ip[1], Ips.toArray((int) ip[2], true));
      assertArrayEquals("To array failed for " + ip[0],
          (int[]) ip[1], Ips.toArray((int) ip[3], false));
    }
  }

  @Test
  public void testFormat() {
    for (final Object[] ip : ips) {
      assertEquals("Format failed for " + ip[0],
          ip[0], Ips.format((int[]) ip[1]));
    }
  }

  @Test
  public void testGetBitsInt() {
    for (final Object[] ip : ips) {
      assertArrayEquals("To bits failed for int " + ip[0],
          (byte[]) ip[9], Ips.getBits((int) ip[2], 0, 32));
    }
  }

  @Test
  public void testGetBitsLong() {
    for (final Object[] ip : ips) {
      assertArrayEquals("To bits failed for long " + ip[0],
          (byte[]) ip[9], Ips.getBits((long) ip[4], 32, 64));
    }
    assertArrayEquals(new byte[] {
        1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1,
        1, 0, 1, 0, 0, 1, 1, 0, 1, 1, 1, 0, 0, 0, 1, 1,
        1, 1, 1, 1, 0, 1, 1, 0, 1, 0, 1, 1, 0, 0, 1, 0},
        Ips.getBits(Long.MIN_VALUE + (3547462489L << 1), 0, 64));
  }

  @Test
  public void testGetBitsShort() {
    assertArrayEquals(new byte[] {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
        Ips.getBits((short) 0, 0, 16));
    assertArrayEquals(new byte[] {0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,},
        Ips.getBits(Short.MAX_VALUE, 0, 16));
    assertArrayEquals(new byte[] {1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
        Ips.getBits(Short.MIN_VALUE, 0, 16));
    assertArrayEquals(new byte[] {1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,},
        Ips.getBits((short) -1, 0, 16));
  }

  @Test
  public void testGetBitsByte() {
    assertArrayEquals(new byte[] {0, 0, 0, 0, 0, 0, 0, 0},
        Ips.getBits((byte) 0, 0, 8));
    assertArrayEquals(new byte[] {0, 1, 1, 1, 1, 1, 1, 1},
        Ips.getBits(Byte.MAX_VALUE, 0, 8));
    assertArrayEquals(new byte[] {1, 0, 0, 0, 0, 0, 0, 0},
        Ips.getBits(Byte.MIN_VALUE, 0, 8));
    assertArrayEquals(new byte[] {1, 1, 1, 1, 1, 1, 1, 1},
        Ips.getBits((byte) -1, 0, 8));
  }

  @Test
  public void testIntegerToUnsignedLong() {
    for (final Object[] ip : ips) {
      assertEquals("To unsigned long failed for " + ip[0],
          ip[4], Ips.integerToUnsignedLong((int) ip[2], true));
      assertEquals("To unsigned long failed for sortable " + ip[0],
          ip[4], Ips.integerToUnsignedLong((int) ip[3], false));
    }
  }

  @Test
  public void testUnsignedLongToInteger() {
    for (final Object[] ip : ips) {
      assertEquals("To int from long failed for " + ip[0],
          ip[2], Ips.unsignedLongToInteger((long) ip[4], true));
      assertEquals("To int sortable from long failed for " + ip[0],
          ip[3], Ips.unsignedLongToInteger((long) ip[4], false));
    }
  }

}
