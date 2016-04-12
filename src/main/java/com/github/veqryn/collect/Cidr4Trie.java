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
 * Implementation of a CIDR Trie, which can be used for routing IP's from
 * a routing/forwarding table, and other forms of prefix matching.
 *
 * <p>
 * This {@link Cidr4} Trie is a {@link Trie} specifically for storing IPv4
 * ranges. CIDR stands for 'Classless Inter-Domain Routing', a method for
 * allocating Internet Protocol address ranges. For more information, see:
 * <a href="https://en.wikipedia.org/wiki/Classless_Inter-Domain_Routing">
 * wikipedia entry on CIDR's</a>.
 *
 * <p>
 * 'Longest Prefix Matching', where an IP, in /32 CIDR format
 * (192.168.20.19/32), needs to be looked up to see which of the matching
 * routes is the most specific (the longest). For example, if the trie
 * contained keys for 192.168.20.16/28 and 192.168.0.0/16, both are prefixes
 * that contain our IP, but the most specific one is 192.168.20.16/28 because
 * it is the longest. This can be accomplished with
 * {@link Trie#valueLongestPrefixOf}, while a list of all prefixes can be
 * found with {@link Trie#prefixOfValues}. For more information, see:
 * <a href="http://en.wikipedia.org/wiki/Longest_prefix_match">wikipedia entry
 * on maximum prefix length match</a>.
 *
 * <p>
 * Additionally, {@link Trie#prefixedByValues} can be used to find all of the
 * CIDR's & IP that are prefixed by a given CIDR. For example, if given
 * 192.168.0.0/16 then 192.168.20.16/28 and 192.168.20.19/32 could be returned.
 *
 * <p>
 * This Trie implementation extends {@link AbstractBinaryTrie},
 * an uncompressed binary bitwise implementation of a Trie for use with short
 * binary data such as IP addresses and CIDR ranges, and therefore is
 * purpose-built for extremely fast lookups, offering O(A(K)) lookup time
 * for get and prefix matching methods, while using up similar memory as
 * a TreeMap when fully loaded.
 * It also implements the {@link Trie} and {@link java.util.Map} interfaces.
 *
 * <p>
 * This implementation returns values in the order of their CIDR keys
 * (an example order would be: 6.6.0.0/16, 6.6.0.0/24, 6.6.0.0/32,
 * 6.6.0.1/32, 6.6.0.4/30, 6.7.0.0/16, 6.7.0.0/32)
 *
 * @author Mark Christopher Duncan
 *
 * @param <V> Value
 */
public final class Cidr4Trie<V> extends AbstractBinaryTrie<Cidr4, V> {

  private static final long serialVersionUID = -8113898642923790939L;



  /**
   * Create an empty {@link Cidr4Trie}.
   */
  public Cidr4Trie() {
    super(new Cidr4Codec());
  }

  /**
   * Create a {@link Cidr4Trie}.
   * The trie will be filled with the CIDRs and values in the provided map.
   *
   * @param otherMap Map of CIDRs and values, which will be {@link #putAll}
   *        into the newly created trie
   */
  public Cidr4Trie(final Map<Cidr4, V> otherMap) {
    super(new Cidr4Codec(), otherMap);
  }

  /**
   * Copy constructor, creates a shallow copy of this
   * {@link Cidr4Trie} instance.
   * (The keys and values themselves are not copied.)
   *
   * @param otherTrie Cidr4Trie
   */
  public Cidr4Trie(final Cidr4Trie<V> otherTrie) {
    super(otherTrie);
  }



  /**
   * Implementation of {@link KeyCodec} for use with Cidr IPv4 ranges.
   * Specifically for use with {@link AbstractBinaryTrie}, because each bit,
   * starting from the left, determines which node it is (left or right), and
   * the number of leading bits (bits in our netmask) is the length.
   */
  public static final class Cidr4Codec implements KeyCodec<Cidr4>, Serializable {

    private static final long serialVersionUID = 5349501966718289752L;

    @Override
    public final int length(final Cidr4 cidr) {
      // Leading number of most significant bits is our length
      return cidr.getMaskBits();
    }

    @Override
    public final boolean isLeft(final Cidr4 cidr, final int index) {
      // Index of a cidr/ip is left-based bit
      return (cidr.getLowBinaryInteger(true) & (1 << (31 - index))) == 0;
    }

    @Override
    public final Cidr4 recreateKey(final BitSet bits, final int numElements) {

      if (bits.length() == 0) {
        return new Cidr4(0, numElements);
      }

      // Maximum of 32 bits in a Cidr4 (IPv4 based)
      int binary = (int) bits.toLongArray()[0];

      // Shift our bits over if we are not 32 bits long
      final int move = 32 - numElements;
      if (move > 0) {
        binary = binary << move;
      }

      return new Cidr4(binary, numElements);
    }

    @Override
    public final Comparator<Cidr4> comparator() {
      // Cidr4 is naturally comparable consistent with KeyCodec
      return null;
    }

  }

}
