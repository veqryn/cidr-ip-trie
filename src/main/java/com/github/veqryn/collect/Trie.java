/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.github.veqryn.collect;

import java.util.Collection;
import java.util.Map;

public interface Trie<K, V> {

  // Methods specific only to Trie:

  Collection<V> valuesPrefixOf(K key, boolean keyInclusive);


  Collection<V> valuesPrefixedBy(K key, boolean keyInclusive);


  Collection<V> valuesPrefixOfOrBy(K key);


  V valueShortestPrefixOf(K key, boolean keyInclusive);


  V valueShortestPrefixedBy(K key, boolean keyInclusive);


  V valueLongestPrefixOf(K key, boolean keyInclusive);


  V valueLongestPrefixedBy(K key, boolean keyInclusive);



  // Methods from java.util.Map:

  /**
   * Removes all of the mappings from this map.
   * The map will be empty after this call returns.
   */
  void clear();


  /**
   * Returns <tt>true</tt> if this map contains no key-value mappings.
   *
   * @return <tt>true</tt> if this map contains no key-value mappings
   */
  boolean isEmpty();


  /**
   * Returns the number of key-value mappings in this map. If the
   * map contains more than <tt>Integer.MAX_VALUE</tt> elements, returns
   * <tt>Integer.MAX_VALUE</tt>.
   *
   * @return the number of key-value mappings in this map
   */
  int size();


  /**
   * Associates the specified value with the specified key in this map.
   * If the map previously contained a mapping for
   * the key, the old value is replaced by the specified value. (A map
   * <tt>m</tt> is said to contain a mapping for a key <tt>k</tt> if and only
   * if {@link #containsKey(Object) m.containsKey(k)} would return
   * <tt>true</tt>.)
   *
   * @param key key with which the specified value is to be associated
   * @param value value to be associated with the specified key
   * @return the previous value associated with <tt>key</tt>, or
   *         <tt>null</tt> if there was no mapping for <tt>key</tt>.
   * @throws ClassCastException if the class of the specified key or value
   *         prevents it from being stored in this map
   * @throws NullPointerException if the specified key or value is null
   * @throws IllegalArgumentException if the length of the key, as
   *         found by the {@link KeyCodec}, is less than or equal to zero
   */
  V put(K key, V value);


  /**
   * Copies all of the mappings from the specified map to this map.
   * The effect of this call is equivalent to that
   * of calling {@link #put(Object,Object) put(k, v)} on this map once
   * for each mapping from key <tt>k</tt> to value <tt>v</tt> in the
   * specified map. The behavior of this operation is undefined if the
   * specified map is modified while the operation is in progress.
   *
   * @param m mappings to be stored in this map
   * @throws ClassCastException if the class of a key or value in the
   *         specified map prevents it from being stored in this map
   * @throws NullPointerException if the specified map is null, or the
   *         specified map contains null keys or values
   * @throws IllegalArgumentException if the length of any key, as
   *         found by the {@link KeyCodec}, is less than or equal to zero
   */
  void putAll(Map<? extends K, ? extends V> m);


  /**
   * Removes the mapping for a key from this map if it is present.
   * More formally, if this map contains a mapping
   * from key <tt>k</tt> to value <tt>v</tt> such that
   * <code>(key==null ?  k==null : key.equals(k))</code>, that mapping
   * is removed. (The map can contain at most one such mapping.)
   *
   * <p>
   * Returns the value to which this map previously associated the key,
   * or <tt>null</tt> if the map contained no mapping for the key.
   *
   * <p>
   * The map will not contain a mapping for the specified key once the
   * call returns.
   *
   * @param key key whose mapping is to be removed from the map
   * @return the previous value associated with <tt>key</tt>, or
   *         <tt>null</tt> if there was no mapping for <tt>key</tt>.
   * @throws ClassCastException if the key is of an inappropriate type for this map
   * @throws NullPointerException if the specified key is null
   */
  V remove(Object key);


  /**
   * Returns <tt>true</tt> if this map contains a mapping for the specified
   * key. More formally, returns <tt>true</tt> if and only if
   * this map contains a mapping for a key <tt>k</tt> such that
   * <tt>(key==null ? k==null : key.equals(k))</tt>. (There can be
   * at most one such mapping.)
   *
   * @param key key whose presence in this map is to be tested
   * @return <tt>true</tt> if this map contains a mapping for the specified key
   * @throws ClassCastException if the key is of an inappropriate type for this map
   * @throws NullPointerException if the specified key is null
   */
  boolean containsKey(Object key);


  /**
   * Returns the value to which the specified key is mapped,
   * or {@code null} if this map contains no mapping for the key.
   *
   * <p>
   * More formally, if this map contains a mapping from a key
   * {@code k} to a value {@code v} such that {@code (key==null ? k==null :
   * key.equals(k))}, then this method returns {@code v}; otherwise
   * it returns {@code null}. (There can be at most one such mapping.)
   *
   * @param key the key whose associated value is to be returned
   * @return the value to which the specified key is mapped, or
   *         {@code null} if this map contains no mapping for the key
   * @throws ClassCastException if the key is of an inappropriate type for this map
   * @throws NullPointerException if the specified key is null
   */
  V get(Object key);


  // Skipping some Map interface methods because they are not relevant to the limited
  // context where a Trie will be used (ie: seeing what values are assigned to keys
  // that are prefixes, suffixes, or at a specified key), and because they are not
  // optimized for use with a Trie (use an ordered map if performance is not a concern).
  // Skipping over: Map.containsValue, .values, .keySet, .entrySet, .equals, .hashCode



}
