/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.github.veqryn.collect;

import java.util.Collection;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Set;

public interface NavigableTrie<K, V> extends Trie<K, V>, NavigableMap<K, V> {



  /**
   * Returns <tt>true</tt> if this map maps one or more keys to the
   * specified value. More formally, returns <tt>true</tt> if and only if
   * this map contains at least one mapping to a value <tt>v</tt> such that
   * <tt>(value==null ? v==null : value.equals(v))</tt>. This operation
   * will probably require time linear in the map size for most
   * implementations of the <tt>Map</tt> interface.
   *
   * @param value value whose presence in this map is to be tested
   * @return <tt>true</tt> if this map maps one or more keys to the specified value
   * @throws ClassCastException if the value is of an inappropriate type for this map
   * @throws NullPointerException if the specified value is null
   */
  @Override
  boolean containsValue(Object value);


  /**
   * Returns a {@link Collection} view of the values contained in this map.
   * The collection is backed by the map, so changes to the map are
   * reflected in the collection, and vice-versa. If the map is
   * modified while an iteration over the collection is in progress
   * (except through the iterator's own <tt>remove</tt> operation),
   * the results of the iteration are undefined. The collection
   * supports element removal, which removes the corresponding
   * mapping from the map, via the <tt>Iterator.remove</tt>,
   * <tt>Collection.remove</tt>, <tt>removeAll</tt>,
   * <tt>retainAll</tt> and <tt>clear</tt> operations. It does not
   * support the <tt>add</tt> or <tt>addAll</tt> operations.
   *
   * @return a collection view of the values contained in this map
   */
  @Override
  Collection<V> values();


  /**
   * Returns a {@link Set} view of the keys contained in this map.
   * The set is backed by the map, so changes to the map are
   * reflected in the set, and vice-versa. If the map is modified
   * while an iteration over the set is in progress (except through
   * the iterator's own <tt>remove</tt> operation), the results of
   * the iteration are undefined. The set supports element removal,
   * which removes the corresponding mapping from the map, via the
   * <tt>Iterator.remove</tt>, <tt>Set.remove</tt>,
   * <tt>removeAll</tt>, <tt>retainAll</tt>, and <tt>clear</tt>
   * operations. It does not support the <tt>add</tt> or <tt>addAll</tt>
   * operations.
   *
   * <p>
   * The keys contained within this Set may not have the same
   * references as the original keys put in this map (in other words,
   * the keys in this set may fail an identity comparison (==) test
   * against the keys originally placed in the map).
   * However, they keys in this set will always be equal to the
   * original keys put in this map, assuming the {@link KeyCodec}
   * being used recreates they appropriately.
   *
   * @return a set view of the keys contained in this map
   */
  @Override
  Set<K> keySet();


  /**
   * Returns a {@link Set} view of the mappings contained in this map.
   * The set is backed by the map, so changes to the map are
   * reflected in the set, and vice-versa. If the map is modified
   * while an iteration over the set is in progress (except through
   * the iterator's own <tt>remove</tt> operation, or through the
   * <tt>setValue</tt> operation on a map entry returned by the
   * iterator) the results of the iteration are undefined. The set
   * supports element removal, which removes the corresponding
   * mapping from the map, via the <tt>Iterator.remove</tt>,
   * <tt>Set.remove</tt>, <tt>removeAll</tt>, <tt>retainAll</tt> and
   * <tt>clear</tt> operations. It does not support the
   * <tt>add</tt> or <tt>addAll</tt> operations.
   *
   * <p>
   * The keys contained within this Set's Entries may not have the same
   * references as the original keys put in this map (in other words,
   * the keys in the entries may fail an identity comparison (==) test
   * against the keys originally placed in the map).
   * However, they keys in the entries will always be equal to the
   * original keys put in this map, assuming the {@link KeyCodec}
   * being used recreates they appropriately.
   *
   * @return a set view of the mappings contained in this map
   */
  @Override
  Set<Map.Entry<K, V>> entrySet();


  /**
   * Compares the specified object with this map for equality. Returns
   * <tt>true</tt> if the given object is also a map and the two maps
   * represent the same mappings. More formally, two maps <tt>m1</tt> and
   * <tt>m2</tt> represent the same mappings if
   * <tt>m1.entrySet().equals(m2.entrySet())</tt>. This ensures that the
   * <tt>equals</tt> method works properly across different implementations
   * of the <tt>Map</tt> interface.
   *
   * @param o object to be compared for equality with this map
   * @return <tt>true</tt> if the specified object is equal to this map
   */
  @Override
  boolean equals(Object o);


  /**
   * Returns the hash code value for this map. The hash code of a map is
   * defined to be the sum of the hash codes of each entry in the map's
   * <tt>entrySet()</tt> view. This ensures that <tt>m1.equals(m2)</tt>
   * implies that <tt>m1.hashCode()==m2.hashCode()</tt> for any two maps
   * <tt>m1</tt> and <tt>m2</tt>, as required by the general contract of
   * {@link Object#hashCode}.
   *
   * @return the hash code value for this map
   * @see Map.Entry#hashCode()
   * @see Object#equals(Object)
   * @see #equals(Object)
   */
  @Override
  int hashCode();



  @Override
  NavigableTrie<K, V> descendingMap();

  @Override
  NavigableTrie<K, V> subMap(K fromKey, K toKey);

  @Override
  NavigableTrie<K, V> subMap(K fromKey, boolean fromInclusive, K toKey, boolean toInclusive);

  @Override
  NavigableTrie<K, V> headMap(K toKey);

  @Override
  NavigableTrie<K, V> headMap(K toKey, boolean inclusive);

  @Override
  NavigableTrie<K, V> tailMap(K fromKey);

  @Override
  NavigableTrie<K, V> tailMap(K fromKey, boolean inclusive);



}
