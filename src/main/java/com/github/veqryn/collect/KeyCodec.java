/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.github.veqryn.collect;

import java.io.Serializable;
import java.util.BitSet;
import java.util.Comparator;
import java.util.NavigableMap;
import java.util.SortedMap;

/**
 * KeyCodec interface, for encoding, decoding, and analyzing keys in a
 * {@link Trie}.
 * Specifically for use with {@link AbstractBinaryTrie}.
 *
 * <p>
 * Includes methods to define a key by determining the key's length,
 * and whether the element at a given index in the key should go to
 * the left or right node in the Trie.
 *
 * <p>
 * Also includes methods to recreate a key based on positional information,
 * and to return a comparator for use with {@link NavigableMap} methods
 * (not yet implemented).
 *
 * @author Mark Christopher Duncan
 *
 * @param <K> Key
 */
public interface KeyCodec<K> extends Serializable {

  /**
   * Returns the number of elements in the key.
   *
   * @param key
   * @return number of elements in this key object
   */
  int length(K key);

  /**
   * Returns true if the element at the given index should be in left node,
   * and false if the element should be in the right node.
   *
   * @param key
   * @param index (0 based)
   * @return true if the element at this index should be in the left node,
   *         false if the element at this index should be in the right node
   */
  boolean isLeft(K key, int index);

  /**
   * Recreates a key based on the position of the node in the Trie.
   * The position is given as the length of the key (int), and each element
   * is in the key is described as being either left (0) or right (1) at
   * it's inversed index position (in a BitSet).
   *
   * <p>
   * If the Trie being used is configured to always cache/keep the keys
   * after insertion, and to write out the keys during serialization,
   * then this method would never get called, and therefore if the Trie
   * is configured to always, without exception, keep the keys and write
   * out the keys during serialization, then the implementation of this
   * method is optional.
   *
   * @param bits BitSet where the bit at the <code>(numElements - 1)</code>
   *        index in the BitSet represents the first element of this key
   *        (index 0), and each index after that is the next element in this
   *        key. (Depending on the key class being decoded, it may be necessary
   *        to reverse the BitSet before use.)
   * @param numElements the number of elements in this key
   * @return A new key equal to the one originally put in
   */
  K recreateKey(BitSet bits, int numElements);

  /**
   * Returns a comparator consistent with the <code>isLeft</code> and
   * <code>length</code> methods, or <code>null</code> if the key
   * (<code>K</code>) is naturally consistent.
   *
   * <p>
   * To be consistent, the comparator must follow these rules:
   *
   * <p>
   * 1. Two keys are considered equal if <code>length</code> returns the same
   * value for both, and for each element index less than the length,
   * <code>isLeft</code> returns the same value for both.
   *
   * <p>
   * 2. Key1 is considered less than Key2 if Key1 has a smaller
   * <code>length</code> and for each element index less than its length,
   * <code>isLeft</code> returns the same value for both.
   *
   * <p>
   * 3. Key1 is considered less than Key2 if for any element index within both
   * key's range, <code>isLeft</code> is true for Key1 but false for Key2
   *
   * <p>
   * This method is not necessary for {@link Trie} methods, and is only called
   * by methods that are part of the (future, not yet implemented)
   * {@link NavigableTrie} interface
   * (including any {@link SortedMap} and {@link NavigableMap} methods).
   * Therefore a Trie that only implements the Trie interface (by extending
   * only {@link AbstractBinaryTrie}) would not need to implement this method,
   * while a Trie that implements the NavigableTrie interface by extending
   * (future, not yet added) {@link AbstractNavigableBinaryTrie}) would need to
   * implement this method.
   *
   * @return Comparator consistent with the KeyCodec interface methods
   */
  Comparator<? super K> comparator();

}
