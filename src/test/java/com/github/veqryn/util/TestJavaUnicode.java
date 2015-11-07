/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.github.veqryn.util;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;

import org.junit.Test;


/**
 * Tests our assumptions about how Java handles Unicode.
 *
 * @author Mark Christopher Duncan
 */
public class TestJavaUnicode {

  protected static final Charset CHARSET = StandardCharsets.UTF_16BE;

  /** [start,end) ranges of character points that Java can encode and decode */
  public static final int[][] singleCharCodePointRanges = new int[][] {
      {0x0000, 0xD800},
      {0xE000, 0xFFFE},
      // 0xFFFE is defined as a non-character, never to appear
      {0xFFFF, 0x10000},
  };

  /** [start, end) high surrogate range */
  public static final int[] highSurrogatesRange = new int[] {0xD800, 0xDC00};

  /** [start, end) low surrogate range */
  public static final int[] lowSurrogatesRange = new int[] {0xDC00, 0xE000};

  /** 2 dimensional index of char pairs, representing the supplemental unicode code points */
  public static final int[][][] doubleCharCodePointRanges =
      cartesianProductOfRange(highSurrogatesRange, lowSurrogatesRange);



  @Test
  public void testJavaUnicodeBehavior() {
    // 0 is starting code point, 0xFFFF (65535) is last code point that can be 1 character encoded
    for (int i = 0x0000; i <= 0xFFFF; ++i) {
      final char[] chars = Character.toChars(i);
      assertEquals(1, chars.length);
      assertEquals(chars[0], (char) i);
    }

    // Test our generator
    final Iterator<String> iter = new TestingUtil.UnicodeGenerator();

    // Ensure Java can encode and decode in valid non-surrogate ranges
    for (final int[] range : singleCharCodePointRanges) {
      for (int i = range[0]; i < range[1]; ++i) {
        final char[] chars = Character.toChars(i);
        assertEquals(1, chars.length);
        assertEquals(new String(chars), "" + chars[0]);
        assertEquals(chars[0], (char) i);
        assertEquals(("" + chars[0]), new String(("" + chars[0]).getBytes(CHARSET), CHARSET));
        assertEquals(("" + (char) i), new String(("" + (char) i).getBytes(CHARSET), CHARSET));
        assertEquals("" + chars[0], iter.next());
      }
    }

    // For supplemental character, ensure we can encode and decode as 2 "character" long strings
    int i = 0x10000;
    for (final int[][] row : doubleCharCodePointRanges) {
      for (final int[] values : row) {
        final char[] chars = Character.toChars(i++);
        assertEquals(2, chars.length);
        final char[] assumedChars = new char[] {(char) values[0], (char) values[1]};
        assertArrayEquals(chars, assumedChars);
        assertEquals(new String(chars), "" + chars[0] + chars[1]);
        assertEquals(new String(chars), new String(assumedChars));
        assertEquals(new String(chars), new String(new String(chars).getBytes(CHARSET), CHARSET));
        assertEquals(new String(assumedChars),
            new String(new String(assumedChars).getBytes(CHARSET), CHARSET));
        assertEquals(new String(chars), iter.next());
      }
    }
    assertEquals("Must test characters up to 0x10FFFF", 0x10FFFF, i - 1); // 1114111
    assertEquals("Must test characters up to 0x10FFFF", false, iter.hasNext());
  }


  /**
   * Create the cartesian product of two integer ranges
   *
   * @param range1 [start,end) int range
   * @param range2 [start,end) int range
   * @return 2 dimensional index of int pairs (pairs are in an array of length 2)
   */
  protected static int[][][] cartesianProductOfRange(final int[] range1, final int[] range2) {
    final int range1Length = range1[1] - range1[0];
    final int range2Length = range2[1] - range2[0];
    final int[][][] rval = new int[range1Length][range2Length][2];
    for (int index1 = 0, i = range1[0]; i < range1[1]; ++index1, ++i) {
      for (int index2 = 0, j = range2[0]; j < range2[1]; ++index2, ++j) {
        rval[index1][index2][0] = i;
        rval[index1][index2][1] = j;
      }
    }
    return rval;
  }

}
