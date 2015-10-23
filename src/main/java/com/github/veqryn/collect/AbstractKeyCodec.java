/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.github.veqryn.collect;

import java.io.Serializable;
import java.util.BitSet;
import java.util.Comparator;

/**
 * AbstractKeyCodec implements {@link KeyCodec} interface,
 * for encoding, decoding, and analyzing keys in a {@link Trie}
 * (specifically for use with {@link AbstractBinaryTrie} and
 * {@link AbstractNavigableBinaryTrie}).
 *
 * <p>
 * Includes a predefined comparator that is based solely on the
 * abstract <code>length</code> and <code>isLeft</code> methods.
 *
 * @author Mark Christopher Duncan
 *
 * @param <K> Key
 */
public abstract class AbstractKeyCodec<K> implements KeyCodec<K>, Serializable {

  private static final long serialVersionUID = -3361215683005193832L;

  protected final Comparator<? super K> comparator = new KeyComparator();


  @Override
  public Comparator<? super K> comparator() {
    return comparator;
  }


  /**
   * A comparator that uses only the <code>length</code> and
   * <code>isLeft</code> methods.
   */
  public class KeyComparator implements Comparator<K>, Serializable {

    private static final long serialVersionUID = 695712059554821573L;

    @Override
    public int compare(final K o1, final K o2) {
      if (o1 == null || o2 == null) {
        throw new IllegalArgumentException("Null keys not allowed");
      }
      if (o1 == o2 || o1.equals(o2)) {
        return 0;
      }
      final int l1 = AbstractKeyCodec.this.length(o1);
      final int l2 = AbstractKeyCodec.this.length(o2);
      final int min = Math.min(l1, l2);
      boolean left1;
      boolean left2;
      for (int i = 0; i < min; ++i) {
        left1 = AbstractKeyCodec.this.isLeft(o1, i);
        left2 = AbstractKeyCodec.this.isLeft(o2, i);
        if (left1 && !left2) {
          return -1;
        }
        if (!left1 && left2) {
          return 1;
        }
      }
      return l1 - l2;
    }

  }

  /**
   * {@link BitSet#toByteArray()} will generate bytes, but probably not in
   * the order you expect because it is using a different encoding scheme.
   *
   * <p>
   * This utility method, {@link toByteArray} will generate them in the order
   * you expect, where the bit at bits.get(0) becomes the first bit in the
   * first byte (index 0), and bits.get(31) becomes the last bit in the forth
   * byte (index 3).
   *
   * @param bits BitSet
   * @param numBits number of bits total (in order to pad the bits set length)
   * @return byte[] with proper bit order
   */
  public static final byte[] toByteArray(final BitSet bits, final int numBits) {
    final int bitsLength = numBits; // bits.length();
    final int maxByteIndex = ((bitsLength + 7) / 8) - 1;
    final byte[] bytes = new byte[maxByteIndex + 1];
    for (int i = 0; i < bitsLength; ++i) {
      if (bits.get(i)) {
        bytes[maxByteIndex - (i / 8)] |= 1 << (i % 8);
      }
    }
    return bytes;
  }


}
