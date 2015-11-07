/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.github.veqryn.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Iterator;

/**
 * Utility methods for tests.
 *
 * @author Mark Christopher Duncan
 */
public class TestingUtil {

  private TestingUtil() {}


  /**
   * Turn any serializable object into a byte array
   *
   * @param obj
   * @return byte[]
   * @throws IOException
   */
  public static final <T extends Serializable> byte[] pickle(final T obj) throws IOException {
    try (final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        final ObjectOutputStream oos = new ObjectOutputStream(baos);) {
      oos.writeObject(obj);
      return baos.toByteArray();
    }
  }

  /**
   * Turn a byte array into a serializable object
   *
   * @param bytes
   * @param clazz
   * @return T
   * @throws IOException
   * @throws ClassNotFoundException
   */
  public static final <T extends Serializable> T unpickle(final byte[] bytes, final Class<T> clazz)
      throws IOException, ClassNotFoundException {
    try (final ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
        final ObjectInputStream ois = new ObjectInputStream(bais);) {
      final Object o = ois.readObject();
      return clazz.cast(o);
    }
  }


  /**
   * Turn a byte array into 1's and 0's (in a String)
   *
   * @param bytes byte[] array
   * @return binary as a String
   */
  public static String toBinary(final byte[] bytes) {
    final StringBuilder sb = new StringBuilder(bytes.length * Byte.SIZE);
    for (int i = 0; i < Byte.SIZE * bytes.length; i++) {
      sb.append((bytes[i / Byte.SIZE] << i % Byte.SIZE & 0x80) == 0 ? '0' : '1');
    }
    return sb.toString();
  }


  /**
   * Generates all valid unicode code points,
   * that can be encoded then decoded, as Strings,
   * in the order defined by unicode standard.
   */
  public static class UnicodeGenerator implements Iterator<String>, Iterable<String> {

    private int codePoint;
    private final int max;
    private final int increment;

    /** Generate all unicode code points that are decode-able */
    public UnicodeGenerator() {
      codePoint = 0;
      max = 0x10FFFF;
      increment = 1;
    }

    /**
     * Generate all unicode code points from the start (inclusive)
     * to the end (exclusive), that are decode-able.
     *
     * @param start starting code point (inclusive)
     * @param end ending code point (exclusive)
     * @param increment amount to increment the code point by
     */
    public UnicodeGenerator(final int start, final int end, final int increment) {
      codePoint = start;
      max = end + 1;
      this.increment = increment;
    }

    @Override
    public boolean hasNext() {
      return codePoint <= max;
    }

    @Override
    public String next() {

      if (0xD800 <= codePoint && codePoint < 0xE000) {
        codePoint = 0xE000; // surrogates
      }
      if (codePoint == 0xFFFE) {
        codePoint = 0xFFFF; // non-character
      }

      final String unicode = new String(Character.toChars(codePoint));

      // for testing purposes, we want to hit both sides of the char boundaries
      if (codePoint < increment) {
        ++codePoint;
      } else if (increment > 1 && codePoint + increment == 65536) {
        codePoint = 65535;
      } else if (codePoint == 65535) {
        codePoint = 65536;
      } else if (increment > 1 && codePoint + increment == 1114112) {
        codePoint = 1114111;
      } else {
        codePoint += increment;
      }

      return unicode;
    }

    @Override
    public void remove() {
      throw new UnsupportedOperationException("Can not remove from a generator");
    }

    @Override
    public Iterator<String> iterator() {
      return this;
    }
  }

}
