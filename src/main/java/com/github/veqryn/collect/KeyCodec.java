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
 * KeyCodec interface, for encoding, decoding, and analyzing keys in a Trie.
 *
 * @author Mark Christopher Duncan
 *
 * @param <K> Key
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

  /**
   * Returns a comparator consistent with the <code>isLeft</code> and <code>length</code> methods,
   * or <code>null</code> if the key (<code>K</code>) is naturally consistent.</br>
   *
   * To be consistent, the comparator must follow these rules:</br>
   *
   * 1. Two keys are considered equal if <code>length</code> returns the same value for both,
   * and for each element index less than the length, <code>isLeft</code> returns the same value for
   * both.</br>
   *
   * 2. Key1 is considered less than Key2 if Key1 has a smaller <code>length</code> and for each
   * element index less than its length, <code>isLeft</code> returns the same value for both.</br>
   *
   * 3. Key1 is considered less than Key2 if for any element index within both key's range,
   * <code>isLeft</code> is true for Key1 but false for Key2
   *
   * @return Comparator consistent with the KeyCodec interface methods
   */
  public Comparator<? super K> comparator();

}
