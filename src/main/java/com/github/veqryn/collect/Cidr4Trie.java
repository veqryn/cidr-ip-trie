/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.github.veqryn.collect;

import java.io.Serializable;
import java.util.BitSet;
import java.util.Comparator;
import java.util.Map;

import com.github.veqryn.net.Cidr4;

/**
 * CidrTrie
 *
 * @author Mark Christopher Duncan
 *
 * @param <V>
 */
public final class Cidr4Trie<V> extends AbstractNavigableBinaryTrie<Cidr4, V> {

  private static final long serialVersionUID = -8113898642923790939L;



  public Cidr4Trie() {
    super(new Cidr4Codec(), false, true);
  }

  public Cidr4Trie(final boolean cacheKeys, final boolean writeKeys) {
    super(new Cidr4Codec(), cacheKeys, writeKeys);
  }

  public Cidr4Trie(final Map<Cidr4, V> otherMap) {
    super(new Cidr4Codec(), otherMap, false, true);
  }

  public Cidr4Trie(final Map<Cidr4, V> otherMap, final boolean cacheKeys, final boolean writeKeys) {
    super(new Cidr4Codec(), otherMap, cacheKeys, writeKeys);
  }

  public Cidr4Trie(final Cidr4Trie<V> otherTrie) {
    super(otherTrie);
  }



  /**
   * Cidr4Codec
   */
  public static final class Cidr4Codec implements KeyCodec<Cidr4>, Serializable {

    private static final long serialVersionUID = 5349501966718289752L;

    @Override
    public final int length(final Cidr4 cidr) {
      return cidr.getMaskBits();
    }

    @Override
    public final boolean isLeft(final Cidr4 cidr, final int index) {
      return (cidr.getLowBinaryInteger(true) & (1 << (31 - index))) == 0;
    }

    @Override
    public final Cidr4 recreateKey(final BitSet bits, final int numElements) {

      if (bits.length() == 0) {
        return new Cidr4(0, numElements);
      }

      int binary = (int) bits.toLongArray()[0];

      final int move = 32 - numElements;
      if (move > 0) {
        binary = binary << move;
      }

      return new Cidr4(binary, numElements);
    }

    @Override
    public final Comparator<Cidr4> comparator() {
      return null;
    }

  }

}
