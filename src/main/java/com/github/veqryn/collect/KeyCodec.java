/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.github.veqryn.collect;

import java.io.Serializable;

/**
 * KeyCodec interface, for encoding, decoding, and analyzing keys in a Trie.
 *
 * @author Mark Christopher Duncan
 */
public interface KeyCodec<K> extends Serializable {

  /**
   * @return number of bits in an element
   *         (Must be >= 1)
   */
  public int bitsPerElement();

  /**
   * @return maximum number of elements
   *         (maxLength() * bitsPerElement() must be <= Integer.MAX_VALUE)
   */
  public int maxLength();

  /**
   * @param key
   * @return number of elements in this key object
   */
  public int length(K key);

  /**
   * @param key
   * @return number of bits in this key object
   */
  public int bitLength(K key);

  /**
   * @param key
   * @param index (0 based)
   * @return true if the element at this index should be in the left node,
   *         false if the element at this index should be in the right node
   */
  public boolean isLeft(K key, int index);

}
