/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.github.veqryn.collect;

import java.io.Serializable;
import java.util.BitSet;

/**
 * KeyCodec interface, for encoding, decoding, and analyzing keys in a Trie.
 *
 * @author Mark Christopher Duncan
 */
public interface KeyCodec<K> extends Serializable {

  /**
   * @param key
   * @return number of elements in this key object
   */
  public int length(K key);

  /**
   * @param key
   * @param index (0 based)
   * @return true if the element at this index should be in the left node,
   *         false if the element at this index should be in the right node
   */
  public boolean isLeft(K key, int index);

  /**
   * @param bits BitSet where the bit at the ({@code numElements} - 1)
   *        location represents the first element of this key,
   *        and each bit after that is another element in this key
   * @param numElements the number of elements in this key
   * @return A new key equal to the one originally put in
   */
  public K recreateKey(BitSet bits, int numElements);

}
