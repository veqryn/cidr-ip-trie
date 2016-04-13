/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.github.veqryn.collect;

import java.util.Collections;
import java.util.Comparator;
import java.util.Map;
import java.util.NavigableMap;
import java.util.NavigableSet;
import java.util.NoSuchElementException;
import java.util.SortedMap;

/**
 * Defines the interface for a Navigable Trie, which is the combination of
 * a {@link NavigableMap} and a {@link Trie} (also called a radix tree or
 * prefix tree), an ordered tree data structure. For more information, see the
 * <a href="http://en.wikipedia.org/wiki/Trie">wikipedia entry on tries</a>.
 *
 * <p>
 * Tries have an advantage over regular Tree's in that the time to lookup
 * any given key's node is constant, based on the length of that key.
 * Looking up a given key also finds all nodes that are prefixes of that key
 * at the same time, and all nodes that are prefixed by that key are children
 * of the found node. This makes a trie ideal for prefix searching.
 *
 * <p>
 * Trie traversal, insert, delete, predecessor, successor, get, prefix of,
 * prefixed by operations are performed in at worst O(K) time, where K
 * is the number of elements in the largest item in the trie. In practice,
 * operations actually take O(A(K)) time, where A(K) is the average number
 * of elements in all items in the trie.
 *
 * <p>
 * A Trie requires very few comparisons to keys while doing any operation.
 * While performing a lookup, each comparison (at most K of them, described
 * above) will perform a single element comparison against only the given key,
 * instead of comparing the entire key to multiple other keys.
 *
 * <p>
 * The Trie will return values in an order determined by a combination of
 * the length and elements in a key, specific to the implementation and
 * purpose of the concrete Trie being used.
 * However it is guaranteed that <code>Prefix-Of</code> methods will return
 * values in the order of their key's length, from smallest to largest.
 *
 * <p>
 * Trie extends {@link Map}, but unlike a normal Map or Tree, Tries
 * do not need to store the Key associated with any node/value,
 * because the key is defined by its position within the trie.
 * All subsequent keys underneath that key's node are descendants
 * of that key and are prefixed by it.
 * For this reason, implementations of Trie may or may not hold onto
 * the key instances that are put into it. If a trie implementation
 * or configuration does not hold onto the key, any method which returns
 * or directly uses a key (example: {@link #keySet}, {@link #entrySet},
 * {@link #equals}, {@link #hashCode}, most methods specific to NavigableMap
 * and {@link SortedMap}) could potentially take longer than a regular
 * Map/Tree would to complete, because each key must be recreated
 * before being used.
 *
 * <p>
 * For these reasons, it is recommended that the concrete implementation of
 * NavigableTrie be configured to keep or cache the key instances, if methods
 * that return, compare, or hash the keys, will be used. Otherwise memory
 * usage can be reduced by not keeping keys instances around.
 *
 *
 * @author Chris Duncan
 *
 * @param <K> Key
 * @param <V> Value
 */
public interface NavigableTrie<K, V> extends Trie<K, V>, NavigableMap<K, V> {

  // Methods specific only to NavigableTrie:

  /**
   * Returns a {@link NavigableTrie} Sub-Map view of this map whose keys are
   * prefixes of the given key (or are mapped directly to the search key (if
   * {@code keyInclusive} is true). The returned trie map is backed by this
   * trie map, so changes in the returned map are reflected in this map, and
   * vice-versa. The returned map supports all operations that this map supports.
   *
   * <p>
   * In a {@link Trie} with fixed size keys, this map will be empty (or have at
   * most one entry if {@code keyInclusive} is true).
   *
   * <p>
   * For example, if the Trie contains 'and', 'ant', 'antacid', 'ante',
   * 'antecede', 'anteceded', 'antecedent', 'antelope', 'ape'; then
   * a lookup of 'antecede' with <code>keyInclusive true</code> would
   * return the keys 'ant', 'ante', and 'antecede', in that order.
   *
   * <p>
   * The returned map will throw an {@code IllegalArgumentException}
   * on an attempt to insert a key outside its range.
   *
   * @param key the key used in the search for prefixes
   * @param keyInclusive true if the returned view should include the
   *        key associated with the search key, if it exists
   * @return a view of the portion of this map whose keys are prefixes of, mapped
   *         to, or prefixed by the given key, depending on the options selected
   * @throws ClassCastException if the key is of an inappropriate type for this map
   * @throws NullPointerException if the specified key is null
   * @throws IllegalArgumentException if this map itself has a
   *         restricted range, and {@code fromKey} lies outside the
   *         bounds of the range; or if the length of fromKey
   *         is less than or equal to zero
   */
  @Override
  NavigableTrie<K, V> prefixOfMap(K key, boolean keyInclusive);


  /**
   * Returns a {@link NavigableTrie} Sub-Map view of this map whose keys are
   * prefixed by the given key (or are mapped directly to the search key (if
   * {@code keyInclusive} is true). The returned trie map is backed by this
   * trie map, so changes in the returned map are reflected in this map, and
   * vice-versa. The returned map supports all operations that this map supports.
   *
   * <p>
   * In a {@link Trie} with fixed size keys, this map will be empty (or have at
   * most one entry if {@code keyInclusive} is true).
   *
   * <p>
   * For example, if the Trie contains 'and', 'ant', 'antacid', 'ante',
   * 'antecede', 'anteceded', 'antecedent', 'antelope', 'ape'; then
   * a lookup of 'antecede' with <code>keyInclusive true</code> would
   * return the keys 'antecede', 'anteceded', and 'antecedent',
   * with the search key first, and the remaining order determined
   * by the implementation being used.
   *
   * <p>
   * The returned map will throw an {@code IllegalArgumentException}
   * on an attempt to insert a key outside its range.
   *
   * @param key the key used in the search for prefixes
   * @param keyInclusive true if the returned view should include the
   *        key associated with the search key, if it exists
   * @return a view of the portion of this map whose keys are prefixes of, mapped
   *         to, or prefixed by the given key, depending on the options selected
   * @throws ClassCastException if the key is of an inappropriate type for this map
   * @throws NullPointerException if the specified key is null
   * @throws IllegalArgumentException if this map itself has a
   *         restricted range, and {@code fromKey} lies outside the
   *         bounds of the range; or if the length of fromKey
   *         is less than or equal to zero
   */
  @Override
  NavigableTrie<K, V> prefixedByMap(K key, boolean keyInclusive);



  // Methods inherited from SortedMap or NavigableMap:


  /**
   * Returns the comparator used to order the keys in this map, or
   * {@code null} if this map uses the {@linkplain Comparable
   * natural ordering} of its keys.
   *
   * @return the comparator used to order the keys in this map,
   *         or {@code null} if this map uses the natural ordering
   *         of its keys
   */
  @Override
  Comparator<? super K> comparator();



  /**
   * Returns a key-value mapping associated with the greatest key
   * strictly less than the given key, or {@code null} if there is
   * no such key.
   *
   * @param key the key
   * @return an entry with the greatest key less than {@code key},
   *         or {@code null} if there is no such key
   * @throws ClassCastException if the specified key cannot be compared
   *         with the keys currently in the map
   * @throws NullPointerException if the specified key is null
   */
  @Override
  Map.Entry<K, V> lowerEntry(K key);


  /**
   * Returns the greatest key strictly less than the given key, or
   * {@code null} if there is no such key.
   *
   * @param key the key
   * @return the greatest key less than {@code key},
   *         or {@code null} if there is no such key
   * @throws ClassCastException if the specified key cannot be compared
   *         with the keys currently in the map
   * @throws NullPointerException if the specified key is null
   */
  @Override
  K lowerKey(K key);


  /**
   * Returns a key-value mapping associated with the greatest key
   * less than or equal to the given key, or {@code null} if there
   * is no such key.
   *
   * @param key the key
   * @return an entry with the greatest key less than or equal to
   *         {@code key}, or {@code null} if there is no such key
   * @throws ClassCastException if the specified key cannot be compared
   *         with the keys currently in the map
   * @throws NullPointerException if the specified key is null
   */
  @Override
  Map.Entry<K, V> floorEntry(K key);


  /**
   * Returns the greatest key less than or equal to the given key,
   * or {@code null} if there is no such key.
   *
   * @param key the key
   * @return the greatest key less than or equal to {@code key},
   *         or {@code null} if there is no such key
   * @throws ClassCastException if the specified key cannot be compared
   *         with the keys currently in the map
   * @throws NullPointerException if the specified key is null
   */
  @Override
  K floorKey(K key);



  /**
   * Returns a key-value mapping associated with the least key
   * greater than or equal to the given key, or {@code null} if
   * there is no such key.
   *
   * @param key the key
   * @return an entry with the least key greater than or equal to
   *         {@code key}, or {@code null} if there is no such key
   * @throws ClassCastException if the specified key cannot be compared
   *         with the keys currently in the map
   * @throws NullPointerException if the specified key is null
   */
  @Override
  Map.Entry<K, V> ceilingEntry(K key);


  /**
   * Returns the least key greater than or equal to the given key,
   * or {@code null} if there is no such key.
   *
   * @param key the key
   * @return the least key greater than or equal to {@code key},
   *         or {@code null} if there is no such key
   * @throws ClassCastException if the specified key cannot be compared
   *         with the keys currently in the map
   * @throws NullPointerException if the specified key is null
   */
  @Override
  K ceilingKey(K key);


  /**
   * Returns a key-value mapping associated with the least key
   * strictly greater than the given key, or {@code null} if there
   * is no such key.
   *
   * @param key the key
   * @return an entry with the least key greater than {@code key},
   *         or {@code null} if there is no such key
   * @throws ClassCastException if the specified key cannot be compared
   *         with the keys currently in the map
   * @throws NullPointerException if the specified key is null
   */
  @Override
  Map.Entry<K, V> higherEntry(K key);


  /**
   * Returns the least key strictly greater than the given key, or
   * {@code null} if there is no such key.
   *
   * @param key the key
   * @return the least key greater than {@code key},
   *         or {@code null} if there is no such key
   * @throws ClassCastException if the specified key cannot be compared
   *         with the keys currently in the map
   * @throws NullPointerException if the specified key is null
   */
  @Override
  K higherKey(K key);



  /**
   * Returns a key-value mapping associated with the least
   * key in this map, or {@code null} if the map is empty.
   *
   * @return an entry with the least key,
   *         or {@code null} if this map is empty
   */
  @Override
  Map.Entry<K, V> firstEntry();


  /**
   * Returns the first (lowest) key currently in this map.
   *
   * @return the first (lowest) key currently in this map
   * @throws NoSuchElementException if this map is empty
   */
  @Override
  K firstKey();


  /**
   * Removes and returns a key-value mapping associated with
   * the least key in this map, or {@code null} if the map is empty.
   *
   * @return the removed first entry of this map,
   *         or {@code null} if this map is empty
   */
  @Override
  Map.Entry<K, V> pollFirstEntry();



  /**
   * Returns a key-value mapping associated with the greatest
   * key in this map, or {@code null} if the map is empty.
   *
   * @return an entry with the greatest key,
   *         or {@code null} if this map is empty
   */
  @Override
  Map.Entry<K, V> lastEntry();


  /**
   * Returns the last (highest) key currently in this map.
   *
   * @return the last (highest) key currently in this map
   * @throws NoSuchElementException if this map is empty
   */
  @Override
  K lastKey();


  /**
   * Removes and returns a key-value mapping associated with
   * the greatest key in this map, or {@code null} if the map is empty.
   *
   * @return the removed last entry of this map,
   *         or {@code null} if this map is empty
   */
  @Override
  Map.Entry<K, V> pollLastEntry();



  /**
   * Returns a {@link NavigableSet} view of the keys contained in this map.
   * The set's iterator returns the keys in ascending order.
   * The set is backed by the map, so changes to the map are reflected in
   * the set, and vice-versa. If the map is modified while an iteration
   * over the set is in progress (except through the iterator's own {@code
   * remove} operation), the results of the iteration are undefined. The
   * set supports element removal, which removes the corresponding mapping
   * from the map, via the {@code Iterator.remove}, {@code Set.remove},
   * {@code removeAll}, {@code retainAll}, and {@code clear} operations.
   * It does not support the {@code add} or {@code addAll} operations.
   *
   * @return a navigable set view of the keys in this map
   */
  @Override
  NavigableSet<K> navigableKeySet();


  /**
   * Returns a reverse order {@link NavigableSet} view of the keys contained in this map.
   * The set's iterator returns the keys in descending order.
   * The set is backed by the map, so changes to the map are reflected in
   * the set, and vice-versa. If the map is modified while an iteration
   * over the set is in progress (except through the iterator's own {@code
   * remove} operation), the results of the iteration are undefined. The
   * set supports element removal, which removes the corresponding mapping
   * from the map, via the {@code Iterator.remove}, {@code Set.remove},
   * {@code removeAll}, {@code retainAll}, and {@code clear} operations.
   * It does not support the {@code add} or {@code addAll} operations.
   *
   * @return a reverse order navigable set view of the keys in this map
   */
  @Override
  NavigableSet<K> descendingKeySet();



  /**
   * Returns a reverse order view of the mappings contained in this map.
   * The descending map is backed by this map, so changes to the map are
   * reflected in the descending map, and vice-versa. If either map is
   * modified while an iteration over a collection view of either map
   * is in progress (except through the iterator's own {@code remove}
   * operation), the results of the iteration are undefined.
   *
   * <p>
   * The returned map has an ordering equivalent to
   * <tt>{@link Collections#reverseOrder(Comparator) Collections.reverseOrder}(comparator())</tt>.
   * The expression {@code m.descendingMap().descendingMap()} returns a
   * view of {@code m} essentially equivalent to {@code m}.
   *
   * @return a reverse order view of this map
   */
  @Override
  NavigableTrie<K, V> descendingMap();



  /**
   * Returns a view of the portion of this map whose keys range from
   * {@code fromKey}, inclusive, to {@code toKey}, exclusive. (If
   * {@code fromKey} and {@code toKey} are equal, the returned map
   * is empty.) The returned map is backed by this map, so changes
   * in the returned map are reflected in this map, and vice-versa.
   * The returned map supports all operations that this map supports.
   *
   * <p>
   * The returned map will throw an {@code IllegalArgumentException}
   * on an attempt to insert a key outside its range.
   *
   * <p>
   * Equivalent to {@code subMap(fromKey, true, toKey, false)}.
   *
   * @param fromKey low endpoint (inclusive) of the keys in the returned map
   * @param toKey high endpoint (exclusive) of the keys in the returned map
   * @return a view of the portion of this map whose keys range from
   *         {@code fromKey}, inclusive, to {@code toKey}, exclusive
   * @throws ClassCastException if {@code fromKey} and {@code toKey}
   *         cannot be compared to one another using this map's comparator
   *         (or, if the map has no comparator, using natural ordering).
   *         Implementations may, but are not required to, throw this
   *         exception if {@code fromKey} or {@code toKey}
   *         cannot be compared to keys currently in the map.
   * @throws NullPointerException if {@code fromKey} or {@code toKey} is null
   * @throws IllegalArgumentException if {@code fromKey} is greater than
   *         {@code toKey}; or if this map itself has a restricted
   *         range, and {@code fromKey} or {@code toKey} lies
   *         outside the bounds of the range; or if the length of either key
   *         is less than or equal to zero
   */
  @Override
  NavigableTrie<K, V> subMap(K fromKey, K toKey);


  /**
   * Returns a view of the portion of this map whose keys range from
   * {@code fromKey} to {@code toKey}. If {@code fromKey} and
   * {@code toKey} are equal, the returned map is empty unless
   * {@code fromInclusive} and {@code toInclusive} are both true. The
   * returned map is backed by this map, so changes in the returned map are
   * reflected in this map, and vice-versa. The returned map supports all
   * operations that this map supports.
   *
   * <p>
   * The returned map will throw an {@code IllegalArgumentException}
   * on an attempt to insert a key outside of its range, or to construct a
   * submap either of whose endpoints lie outside its range.
   *
   * @param fromKey low endpoint of the keys in the returned map
   * @param fromInclusive {@code true} if the low endpoint
   *        is to be included in the returned view
   * @param toKey high endpoint of the keys in the returned map
   * @param toInclusive {@code true} if the high endpoint
   *        is to be included in the returned view
   * @return a view of the portion of this map whose keys range from
   *         {@code fromKey} to {@code toKey}
   * @throws ClassCastException if {@code fromKey} and {@code toKey}
   *         cannot be compared to one another using this map's comparator
   *         (or, if the map has no comparator, using natural ordering).
   *         Implementations may, but are not required to, throw this
   *         exception if {@code fromKey} or {@code toKey}
   *         cannot be compared to keys currently in the map.
   * @throws NullPointerException if {@code fromKey} or {@code toKey} is null
   * @throws IllegalArgumentException if {@code fromKey} is greater than
   *         {@code toKey}; or if this map itself has a restricted
   *         range, and {@code fromKey} or {@code toKey} lies
   *         outside the bounds of the range; or if the length of either key
   *         is less than or equal to zero
   */
  @Override
  NavigableTrie<K, V> subMap(K fromKey, boolean fromInclusive, K toKey, boolean toInclusive);


  /**
   * Returns a view of the portion of this map whose keys are
   * strictly less than {@code toKey}. The returned map is backed
   * by this map, so changes in the returned map are reflected in
   * this map, and vice-versa. The returned map supports all
   * operations that this map supports.
   *
   * <p>
   * The returned map will throw an {@code IllegalArgumentException}
   * on an attempt to insert a key outside its range.
   *
   * <p>
   * Equivalent to {@code headMap(toKey, false)}.
   *
   * @param toKey high endpoint (exclusive) of the keys in the returned map
   * @return a view of the portion of this map whose keys are strictly
   *         less than {@code toKey}
   * @throws ClassCastException if {@code toKey} is not compatible
   *         with this map's comparator (or, if the map has no comparator,
   *         if {@code toKey} does not implement {@link Comparable}).
   *         Implementations may, but are not required to, throw this
   *         exception if {@code toKey} cannot be compared to keys
   *         currently in the map.
   * @throws NullPointerException if {@code toKey} is null
   * @throws IllegalArgumentException if this map itself has a
   *         restricted range, and {@code toKey} lies outside the
   *         bounds of the range; or if this map itself has a restricted
   *         range, and {@code fromKey} or {@code toKey} lies
   *         outside the bounds of the range; or if the length of toKey
   *         is less than or equal to zero
   */
  @Override
  NavigableTrie<K, V> headMap(K toKey);


  /**
   * Returns a view of the portion of this map whose keys are less than (or
   * equal to, if {@code inclusive} is true) {@code toKey}. The returned
   * map is backed by this map, so changes in the returned map are reflected
   * in this map, and vice-versa. The returned map supports all
   * operations that this map supports.
   *
   * <p>
   * The returned map will throw an {@code IllegalArgumentException}
   * on an attempt to insert a key outside its range.
   *
   * @param toKey high endpoint of the keys in the returned map
   * @param inclusive {@code true} if the high endpoint
   *        is to be included in the returned view
   * @return a view of the portion of this map whose keys are less than
   *         (or equal to, if {@code inclusive} is true) {@code toKey}
   * @throws ClassCastException if {@code toKey} is not compatible
   *         with this map's comparator (or, if the map has no comparator,
   *         if {@code toKey} does not implement {@link Comparable}).
   *         Implementations may, but are not required to, throw this
   *         exception if {@code toKey} cannot be compared to keys
   *         currently in the map.
   * @throws NullPointerException if {@code toKey} is null
   * @throws IllegalArgumentException if this map itself has a
   *         restricted range, and {@code toKey} lies outside the
   *         bounds of the range; or if the length of toKey
   *         is less than or equal to zero
   */
  @Override
  NavigableTrie<K, V> headMap(K toKey, boolean inclusive);


  /**
   * Returns a view of the portion of this map whose keys are
   * greater than or equal to {@code fromKey}. The returned map is
   * backed by this map, so changes in the returned map are
   * reflected in this map, and vice-versa. The returned map
   * supports all operations that this map supports.
   *
   * <p>
   * The returned map will throw an {@code IllegalArgumentException}
   * on an attempt to insert a key outside its range.
   *
   * <p>
   * Equivalent to {@code tailMap(fromKey, true)}.
   *
   * @param fromKey low endpoint (inclusive) of the keys in the returned map
   * @return a view of the portion of this map whose keys are greater
   *         than or equal to {@code fromKey}
   * @throws ClassCastException if {@code fromKey} is not compatible
   *         with this map's comparator (or, if the map has no comparator,
   *         if {@code fromKey} does not implement {@link Comparable}).
   *         Implementations may, but are not required to, throw this
   *         exception if {@code fromKey} cannot be compared to keys
   *         currently in the map.
   * @throws NullPointerException if {@code fromKey} is null
   * @throws IllegalArgumentException if this map itself has a
   *         restricted range, and {@code fromKey} lies outside the
   *         bounds of the range; or if the length of fromKey
   *         is less than or equal to zero
   */
  @Override
  NavigableTrie<K, V> tailMap(K fromKey);


  /**
   * Returns a view of the portion of this map whose keys are greater than (or
   * equal to, if {@code inclusive} is true) {@code fromKey}. The returned
   * map is backed by this map, so changes in the returned map are reflected
   * in this map, and vice-versa. The returned map supports all
   * operations that this map supports.
   *
   * <p>
   * The returned map will throw an {@code IllegalArgumentException}
   * on an attempt to insert a key outside its range.
   *
   * @param fromKey low endpoint of the keys in the returned map
   * @param inclusive {@code true} if the low endpoint
   *        is to be included in the returned view
   * @return a view of the portion of this map whose keys are greater than
   *         (or equal to, if {@code inclusive} is true) {@code fromKey}
   * @throws ClassCastException if {@code fromKey} is not compatible
   *         with this map's comparator (or, if the map has no comparator,
   *         if {@code fromKey} does not implement {@link Comparable}).
   *         Implementations may, but are not required to, throw this
   *         exception if {@code fromKey} cannot be compared to keys
   *         currently in the map.
   * @throws NullPointerException if {@code fromKey} is null
   * @throws IllegalArgumentException if this map itself has a
   *         restricted range, and {@code fromKey} lies outside the
   *         bounds of the range; or if the length of fromKey
   *         is less than or equal to zero
   */
  @Override
  NavigableTrie<K, V> tailMap(K fromKey, boolean inclusive);


}
