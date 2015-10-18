/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.github.veqryn.collect;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

/**
 * Defines the interface for a trie (also called a radix tree or prefix
 * tree), an ordered tree data structure. For more information, see the
 * <a href="http://en.wikipedia.org/wiki/Trie">wikipedia entry on tries</a>.
 *
 * <p>
 * Tries have an advantage over regular Tree's in that the time to lookup
 * any given key's node is constant, based on the length of that key.
 * Looking up a given key also gives all nodes that are prefixes of that
 * key for free, and all nodes that are prefixed by that key are children
 * of the found node. This makes a trie ideal for prefix searching.
 *
 * <p>
 * Trie extends {@link Map}, but unlike a normal Map or Tree, Tries
 * do not need to store the Key associated with any node/value,
 * because the is defined by its position within the trie.
 * All subsequent keys underneath that key's node are descendants
 * of that key and are prefixed by it.
 * For this reason, implementations of Trie may or may not hold onto
 * the key instances that are put into it. If a trie implementation
 * or configuration does not hold onto the key, any method which
 * returns or directly uses a key ({@link #keySet}, {@link #entrySet},
 * {@link #equals}, {@link #hashCode}) could potentially take longer
 * than a regular Map/Tree would to complete, because each key must
 * be recreated before being used.
 *
 *
 * @author Mark Christopher Duncan
 *
 * @param <K> Key
 * @param <V> Value
 */
public interface Trie<K, V> extends Map<K, V> {

  // Methods specific only to Trie:

  /**
   * Returns a {@link Collection} <b>view</b> of this {@link Trie}
   * of the values whose mapped keys are prefixes of the given key.
   *
   * <p>
   * In a Trie with fixed size keys, this is essentially a {@link #get}
   * operation (but returning nothing if keyInclusive is false).
   *
   * <p>
   * For example, if the Trie contains 'and', 'ant', 'antacid', 'ante',
   * 'antecede', 'anteceded', 'antecedent', 'antelope', 'ape'; then
   * a lookup of 'antecede' with <code>keyInclusive true</code> would
   * return the values of 'ant', 'ante', and 'antecede', in that order.
   *
   * <p>
   * The collection is backed by the trie, so changes to the trie are
   * reflected in the collection, and vice-versa. If the trie is
   * modified while an iteration over the collection is in progress
   * (except through the iterator's own <tt>remove</tt> operation),
   * the results of the iteration are undefined. The collection
   * supports element removal, which removes the corresponding
   * mapping from the trie, via the <tt>Iterator.remove</tt>,
   * <tt>Collection.remove</tt>, <tt>removeAll</tt>,
   * <tt>retainAll</tt> and <tt>clear</tt> operations. It does not
   * support the <tt>add</tt> or <tt>addAll</tt> operations.
   *
   * @param key the key used in the search for prefixes
   * @param keyInclusive true if the returned view should include the
   *        value associated with the search key, if it exists
   * @return a collection view of the values contained in this trie,
   *         whose keys are prefixes of the given key
   * @throws ClassCastException if the key is of an inappropriate type for this map
   * @throws NullPointerException if the specified key is null
   * @throws IllegalArgumentException if the length of the key is
   *         less than or equal to zero
   */
  Collection<V> valuesPrefixOf(K key, boolean keyInclusive);


  /**
   * Returns a {@link Collection} <b>view</b> of this {@link Trie}
   * of the values whose mapped keys are prefixed by the given key.
   *
   * <p>
   * In a Trie with fixed size keys, this is essentially a {@link #get}
   * operation (but returning nothing if keyInclusive is false).
   *
   * <p>
   * For example, if the Trie contains 'and', 'ant', 'antacid', 'ante',
   * 'antecede', 'anteceded', 'antecedent', 'antelope', 'ape'; then
   * a lookup of 'antecede' with <code>keyInclusive true</code> would
   * return the values of 'antecede', 'anteceded', and 'antecedent',
   * in that order.
   *
   * <p>
   * The collection is backed by the trie, so changes to the trie are
   * reflected in the collection, and vice-versa. If the trie is
   * modified while an iteration over the collection is in progress
   * (except through the iterator's own <tt>remove</tt> operation),
   * the results of the iteration are undefined. The collection
   * supports element removal, which removes the corresponding
   * mapping from the trie, via the <tt>Iterator.remove</tt>,
   * <tt>Collection.remove</tt>, <tt>removeAll</tt>,
   * <tt>retainAll</tt> and <tt>clear</tt> operations. It does not
   * support the <tt>add</tt> or <tt>addAll</tt> operations.
   *
   * @param key the key used in the search for prefixes
   * @param keyInclusive true if the returned view should include the
   *        value associated with the search key, if it exists
   * @return a collection view of the values contained in this trie,
   *         whose keys are prefixed by the given key
   * @throws ClassCastException if the key is of an inappropriate type for this map
   * @throws NullPointerException if the specified key is null
   * @throws IllegalArgumentException if the length of the key is
   *         less than or equal to zero
   */
  Collection<V> valuesPrefixedBy(K key, boolean keyInclusive);


  /**
   * Returns a {@link Collection} <b>view</b> of this {@link Trie}
   * of the values whose mapped keys are prefixes of the given key,
   * are mapped directly to the key, or are prefixed by the key.
   *
   * <p>
   * In a Trie with fixed size keys, this is essentially a {@link #get}
   * operation.
   *
   * <p>
   * For example, if the Trie contains 'and', 'ant', 'antacid', 'ante',
   * 'antecede', 'anteceded', 'antecedent', 'antelope', 'ape'; then
   * a lookup of 'antecede' would return the values of 'ant', 'ante',
   * 'antecede', 'anteceded', and 'antecedent', in that order.
   *
   * <p>
   * The collection is backed by the trie, so changes to the trie are
   * reflected in the collection, and vice-versa. If the trie is
   * modified while an iteration over the collection is in progress
   * (except through the iterator's own <tt>remove</tt> operation),
   * the results of the iteration are undefined. The collection
   * supports element removal, which removes the corresponding
   * mapping from the trie, via the <tt>Iterator.remove</tt>,
   * <tt>Collection.remove</tt>, <tt>removeAll</tt>,
   * <tt>retainAll</tt> and <tt>clear</tt> operations. It does not
   * support the <tt>add</tt> or <tt>addAll</tt> operations.
   *
   * @param key the key used in the search for prefixes
   * @return a collection view of the values contained in this trie, whose
   *         keys are prefixes of, mapped to, or prefixed by the given key
   * @throws ClassCastException if the key is of an inappropriate type for this map
   * @throws NullPointerException if the specified key is null
   * @throws IllegalArgumentException if the length of the key is
   *         less than or equal to zero
   */
  Collection<V> valuesPrefixesOfOrBy(K key);


  /**
   * Returns the value mapped to the shortest key that is a prefix
   * of the given key.
   *
   * <p>
   * In a Trie with fixed size keys, this is essentially a {@link #get}
   * operation (but returning nothing if keyInclusive is false).
   *
   * <p>
   * For example, if the Trie contains 'and', 'ant', 'antacid', 'ante',
   * 'antecede', 'anteceded', 'antecedent', 'antelope', 'ape'; then
   * a lookup of 'antecede' with <code>keyInclusive true</code> would
   * return the value of 'ant'.
   *
   * @param key the key used in the search for prefixes
   * @param keyInclusive true if the search may include the key
   * @return the value mapped to the shortest key that is a prefix of
   *         the given key, or {@code null} if there are no prefixes
   * @throws ClassCastException if the key is of an inappropriate type for this map
   * @throws NullPointerException if the specified key is null
   * @throws IllegalArgumentException if the length of the key is
   *         less than or equal to zero
   */
  V valueShortestPrefixOf(K key, boolean keyInclusive);


  /**
   * Returns the value mapped to the shortest key that is prefixed
   * by the given key.
   *
   * <p>
   * In a Trie with fixed size keys, this is essentially a {@link #get}
   * operation (but returning nothing if keyInclusive is false).
   *
   * <p>
   * For example, if the Trie contains 'and', 'ant', 'antacid', 'ante',
   * 'antecede', 'anteceded', 'antecedent', 'antelope', 'ape'; then
   * a lookup of 'antecede' with <code>keyInclusive true</code> would
   * return the value of 'antecede'.
   *
   * @param key the key used in the search for prefixes
   * @param keyInclusive true if the search may include the key
   * @return the value mapped to the shortest key that is prefixed by
   *         the given key, or {@code null} if there are no prefixes
   * @throws ClassCastException if the key is of an inappropriate type for this map
   * @throws NullPointerException if the specified key is null
   * @throws IllegalArgumentException if the length of the key is
   *         less than or equal to zero
   */
  V valueShortestPrefixedBy(K key, boolean keyInclusive);


  /**
   * Returns the value mapped to the longest key that is a prefix
   * of the given key.
   *
   * <p>
   * For example, this method can be used for Longest Prefix Matching
   * of IP addresses against CIDR ranges.
   *
   * <p>
   * In a Trie with fixed size keys, this is essentially a {@link #get}
   * operation (but returning nothing if keyInclusive is false).
   *
   * <p>
   * For example, if the Trie contains 'and', 'ant', 'antacid', 'ante',
   * 'antecede', 'anteceded', 'antecedent', 'antelope', 'ape'; then
   * a lookup of 'antecede' with <code>keyInclusive true</code> would
   * return the value of 'antecede'.
   *
   * @param key the key used in the search for prefixes
   * @param keyInclusive true if the search may include the key
   * @return the value mapped to the longest key that is a prefix of
   *         the given key, or {@code null} if there are no prefixes
   * @throws ClassCastException if the key is of an inappropriate type for this map
   * @throws NullPointerException if the specified key is null
   * @throws IllegalArgumentException if the length of the key is
   *         less than or equal to zero
   */
  V valueLongestPrefixOf(K key, boolean keyInclusive);


  /**
   * Returns the value mapped to the longest key that is prefixed
   * by the given key.
   *
   * <p>
   * In a Trie with fixed size keys, this is essentially a {@link #get}
   * operation (but returning nothing if keyInclusive is false).
   *
   * <p>
   * For example, if the Trie contains 'and', 'ant', 'antacid', 'ante',
   * 'antecede', 'anteceded', 'antecedent', 'antelope', 'ape'; then
   * a lookup of 'antecede' with <code>keyInclusive true</code> would
   * return the value of 'antecedent'.
   *
   * @param key the key used in the search for prefixes
   * @param keyInclusive true if the search may include the key
   * @return the value mapped to the longest key that is prefixed by
   *         the given key, or {@code null} if there are no prefixes
   * @throws ClassCastException if the key is of an inappropriate type for this map
   * @throws NullPointerException if the specified key is null
   * @throws IllegalArgumentException if the length of the key is
   *         less than or equal to zero
   */
  V valueLongestPrefixedBy(K key, boolean keyInclusive);



  // Methods from java.util.Map:

  /**
   * Removes all of the mappings from this map.
   * The map will be empty after this call returns.
   */
  @Override
  void clear();


  /**
   * Returns <tt>true</tt> if this map contains no key-value mappings.
   *
   * @return <tt>true</tt> if this map contains no key-value mappings
   */
  @Override
  boolean isEmpty();


  /**
   * Returns the number of key-value mappings in this map. If the
   * map contains more than <tt>Integer.MAX_VALUE</tt> elements, returns
   * <tt>Integer.MAX_VALUE</tt>.
   *
   * @return the number of key-value mappings in this map
   */
  @Override
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
   * @throws IllegalArgumentException if the length of the key is less
   *         than or equal to zero
   */
  @Override
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
   * @throws IllegalArgumentException if the length of any key is less
   *         than or equal to zero
   */
  @Override
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
  @Override
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
  @Override
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
  @Override
  V get(Object key);



  // The following methods are not very useful for the context where a Trie will be
  // useful (ie: seeing what values are assigned to keys that are prefixes of
  // or prefixed by a specified key), and may not be optimized for use with a trie,
  // (for example with implementations that are configured to discard keys)
  // but are still necessary due to extending the Map interface.


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
   * Trie implementations that are configured to discard keys after
   * insertion (a <code>put</code>) will take longer to iterate the
   * first time through this Set, as the keys will need to be recreated
   * for each value/node in the Trie. Subsequence iterations would be
   * faster if the keys are cached, but at a cost of increased memory
   * consumption. Therefore if this method will be used, it is encouraged
   * to configure the concrete Trie implementation to not discard keys.
   *
   * <p>
   * Depending on the Trie implementation and configuration/settings,
   * (Trie implementations may discard or keep keys after insertion)
   * the keys contained within this Set may not have the same
   * references as the original keys put in this map (in other words,
   * the keys in this set may fail an identity comparison (==) test
   * against the keys originally placed in the map).
   * However, they keys in this set will always be equal to the
   * original keys put in this map, assuming the implementation
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
   * Trie implementations that are configured to discard keys after
   * insertion (a <code>put</code>) will take longer to iterate the
   * first time through this Set, as the keys will need to be recreated
   * for each value/node in the Trie. Subsequence iterations would be
   * faster if the keys are cached, but at a cost of increased memory
   * consumption. Therefore if this method will be used, it is encouraged
   * to configure the concrete Trie implementation to not discard keys.
   *
   * <p>
   * Depending on the Trie implementation and configuration/settings,
   * (Trie implementations may discard or keep keys after insertion)
   * the keys contained within this Set's Entries may not have the same
   * references as the original keys put in this map (in other words,
   * the keys in this set may fail an identity comparison (==) test
   * against the keys originally placed in the map).
   * However, they keys in this set will always be equal to the
   * original keys put in this map, assuming the implementation
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
   * <p>
   * Trie implementations that are configured to discard keys after
   * insertion (a <code>put</code>) will take longer to be compared
   * against another Map implementation, as all keys must be compared
   * for equality, and the keys will need to be recreated
   * for each value/node in the Trie. Subsequence comparisons would be
   * faster if the keys are cached, but at a cost of increased memory
   * consumption. Therefore if this method will be used, it is encouraged
   * to configure the concrete Trie implementation to not discard keys.
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
   * <p>
   * Trie implementations that are configured to discard keys after
   * insertion (a <code>put</code>) will take longer to generate a hashCode,
   * as all keys must be hashed and the keys will need to be recreated
   * for each value/node in the Trie. Subsequence comparisons would be
   * faster if the keys are cached, but at a cost of increased memory
   * consumption. Therefore if this method will be used, it is encouraged
   * to configure the concrete Trie implementation to not discard keys.
   *
   * @return the hash code value for this map
   * @see Map.Entry#hashCode()
   * @see Object#equals(Object)
   * @see #equals(Object)
   */
  @Override
  int hashCode();


}
