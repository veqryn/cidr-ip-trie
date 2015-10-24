/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.github.veqryn.collect;

import java.io.Serializable;
import java.util.AbstractCollection;
import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.Map;
import java.util.NavigableMap;
import java.util.NavigableSet;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;


/**
 * Implementation of the {@link NavigableTrie} interface ({@link Trie} +
 * {@link NavigableMap} interfaces), as an
 * uncompressed binary bitwise trie. For more information, see:
 * <a href="http://en.wikipedia.org/wiki/Trie">wikipedia entry on tries</a>.
 *
 * <p>
 * Works best with short binary data, such as IP addresses or CIDR ranges.
 *
 * Keys will be analyzed as they come in by a {@link KeyCodec}, for the
 * length of their elements, and for the element at any index belonging in
 * either the left or right nodes in a tree. A single empty root node is our
 * starting point, with nodes containing references to their parent, their
 * left and right children if any, and their value if any. Leaf nodes must
 * always have a value, but intermediate nodes may or may not have values.
 *
 * <p>
 * Keys and Values may never be {@code null}, and therefore if any node
 * has a value, it implicitly has a key.
 *
 * <p>
 * Keys and Values are returned in an order according to the order of the
 * elements in the key, and the number of elements in the key.
 *
 * <p>
 * It is recommended that {@code cacheKeys} be set to true, if NavigableMap
 * and {@link SortedMap} methods that return, compare, or hash the keys,
 * will be used. Otherwise memory usage can be reduced by not keeping keys
 * instances around.
 *
 * @author Mark Christopher Duncan
 *
 * @param <K> Key
 * @param <V> Value
 */
public class AbstractNavigableBinaryTrie<K, V> extends AbstractBinaryTrie<K, V>
    implements NavigableTrie<K, V> {
  // maybe implement guava Multimap or SortedSetMultimap
  // maybe implement apache commons collection interfaces?

  private static final long serialVersionUID = 8021211307125173880L;

  protected transient NavigableSet<K> navigableKeySet = null;
  protected transient NavigableTrie<K, V> descendingMap = null;



  // Constructors:

  /**
   * Create an empty {@link AbstractNavigableBinaryTrie}, implementing
   * {@link NavigableTrie}, using the given {@link KeyCodec}, and settings for
   * keeping/caching or not of keys after insertion and writing out or not of
   * keys during serialization.
   *
   * <p>
   * {@link Trie}s do not necessarily need to store the full instances of each
   * key, because a key can be determined and recreated by its position within
   * the trie's structure. Therefore this implementation provides options
   * on what to do with the key instances after their node has been created.
   *
   * <p>
   * If {@code cacheKeys} is set to true, keys will be permanently kept after
   * being inserted with the {@link #put} operation, and will maintain their
   * == identity with the original keys. If set to false, keys will be
   * discarded, allowing for a much smaller memory footprint, at the cost
   * of increased cpu time should the keys need to be recreated (which would
   * only occur if the methods {@link #keySet}, {@link #entrySet},
   * {@link #equals}, and {@link #hashCode} were called, because they either
   * give up the full key for outside use, or hash or compare full keys).
   * After being recreated, the keys will be cached so that subsequent lookups
   * do not need to recreate the key. The recreated keys will be equal
   * to the original key, but will not be the same reference pointer
   * (unless the KeyCodec being used is doing some magic).
   *
   * <p>
   * If {@code writeKeys} is set to true, keys will be written out during
   * serialization (alternating with their respective values, just like a
   * regular map). If set to false, keys will not be written out, and
   * instead the node structure (and each node's value) will be written out.
   * Because each Node has 3 reference pointers to up to 3 other Nodes,
   * (pointers are usually 32 bits) writing out the nodes like this ends up
   * saving space when the size of keys > size of 3 pointers, but costs extra
   * space if size of key < 3 pointers. Even if the size of keys is smaller,
   * writing the root nodes would be faster than recreating keys if the keys
   * are not being kept/cached.
   *
   * <p>
   * To summarize, there are really 3 options:
   *
   * <p>
   * 1. cacheKeys = false & writeKeys = false: Less memory used at runtime,
   * but slower to use keySet, entrySet, equals and hashCode. Fast speed of
   * serialization, but size of serialized trie will most likely be larger,
   * but this all depends on the key class.
   *
   * <p>
   * 2. cacheKeys = false & writeKeys = true: Less memory used at runtime,
   * but slower to use keySet, entrySet, equals and hashCode. Slightly slower
   * speed of serialization, but size of serialized trie will most likely be
   * smaller, but this all depends on the key class.
   *
   * <p>
   * 3. cacheKeys = true & writeKeys = true: More memory used at runtime,
   * but faster to use keySet, entrySet, equals and hashCode. Fast speed of
   * serialization, and size of serialized trie will most likely be smaller,
   * but this all depends on the key class.
   *
   *
   * @param keyCodec KeyCodec for analyzing of keys
   * @param cacheKeys true if the Trie should store keys after insertion,
   *        false if the Trie should discard keys after the insertion of their
   *        value
   * @param writeKeys true if on serialization of the trie, the keys should
   *        be written out to the stream, and false if the keys should not be
   *        (the trie will write out the structure of its nodes instead)
   */
  public AbstractNavigableBinaryTrie(final KeyCodec<K> keyCodec, final boolean cacheKeys,
      final boolean writeKeys) {
    super(keyCodec, cacheKeys, writeKeys);
  }


  /**
   * Create an empty {@link AbstractNavigableBinaryTrie}, implementing
   * {@link NavigableTrie}, using the given {@link KeyCodec}, and settings for
   * keeping/caching or not of keys after insertion and writing out or not of
   * keys during serialization. The trie will be filled with the keys and
   * values in the provided map.
   *
   * <p>
   * {@link Trie}s do not necessarily need to store the full instances of each
   * key, because a key can be determined and recreated by its position within
   * the trie's structure. Therefore this implementation provides options
   * on what to do with the key instances after their node has been created.
   *
   * <p>
   * If {@code cacheKeys} is set to true, keys will be permanently kept after
   * being inserted with the {@link #put} operation, and will maintain their
   * == identity with the original keys. If set to false, keys will be
   * discarded, allowing for a much smaller memory footprint, at the cost
   * of increased cpu time should the keys need to be recreated (which would
   * only occur if the methods {@link #keySet}, {@link #entrySet},
   * {@link #equals}, and {@link #hashCode} were called, because they either
   * give up the full key for outside use, or hash or compare full keys).
   * After being recreated, the keys will be cached so that subsequent lookups
   * do not need to recreate the key. The recreated keys will be equal
   * to the original key, but will not be the same reference pointer
   * (unless the KeyCodec being used is doing some magic).
   *
   * <p>
   * If {@code writeKeys} is set to true, keys will be written out during
   * serialization (alternating with their respective values, just like a
   * regular map). If set to false, keys will not be written out, and
   * instead the node structure (and each node's value) will be written out.
   * Because each Node has 3 reference pointers to up to 3 other Nodes,
   * (pointers are usually 32 bits) writing out the nodes like this ends up
   * saving space when the size of keys > size of 3 pointers, but costs extra
   * space if size of key < 3 pointers. Even if the size of keys is smaller,
   * writing the root nodes would be faster than recreating keys if the keys
   * are not being kept/cached.
   *
   * <p>
   * To summarize, there are really 3 options:
   *
   * <p>
   * 1. cacheKeys = false & writeKeys = false: Less memory used at runtime,
   * but slower to use keySet, entrySet, equals and hashCode. Fast speed of
   * serialization, but size of serialized trie will most likely be larger,
   * but this all depends on the key class.
   *
   * <p>
   * 2. cacheKeys = false & writeKeys = true: Less memory used at runtime,
   * but slower to use keySet, entrySet, equals and hashCode. Slightly slower
   * speed of serialization, but size of serialized trie will most likely be
   * smaller, but this all depends on the key class.
   *
   * <p>
   * 3. cacheKeys = true & writeKeys = true: More memory used at runtime,
   * but faster to use keySet, entrySet, equals and hashCode. Fast speed of
   * serialization, and size of serialized trie will most likely be smaller,
   * but this all depends on the key class.
   *
   * @param keyCodec KeyCodec for analyzing of keys
   * @param otherMap Map of keys and values, which will be {@link #putAll}
   *        into the newly created trie
   * @param cacheKeys true if the Trie should store keys after insertion,
   *        false if the Trie should discard keys after the insertion of their
   *        value
   * @param writeKeys true if on serialization of the trie, the keys should
   *        be written out to the stream, and false if the keys should not be
   *        (the trie will write out the structure of its nodes instead)
   */
  public AbstractNavigableBinaryTrie(final KeyCodec<K> keyCodec, final Map<K, V> otherMap,
      final boolean cacheKeys, final boolean writeKeys) {
    super(keyCodec, otherMap, cacheKeys, writeKeys);
  }

  /**
   * Copy constructor, creates a shallow copy of this
   * {@link AbstractNavigableBinaryTrie} instance, implementing
   * {@link NavigableTrie}.
   *
   * @param otherTrie AbstractBinaryTrie
   */
  public AbstractNavigableBinaryTrie(final AbstractBinaryTrie<K, V> otherTrie) {
    super(otherTrie);
  }



  // Utility methods:

  /**
   * @param node the Node to get the key from, or null
   * @param trie the Trie this Node belongs to
   * @return the key corresponding to the specified Node
   * @throws NoSuchElementException if the Entry is null
   */
  protected static final <K, V> K keyOrNoSuchElementException(final Node<K, V> node,
      final AbstractBinaryTrie<K, V> trie) {
    if (node == null || node.value == null) {
      throw new NoSuchElementException();
    }
    // Resolve the Key if missing
    return resolveKey(node, trie);
  }

  /**
   * @param node the Node to get the key from, or null
   * @param trie the Trie this Node belongs to
   * @return the key corresponding to the specified Node,
   *         or null if it does not exist
   */
  protected static final <K, V> K keyOrNull(final Node<K, V> node,
      final AbstractBinaryTrie<K, V> trie) {
    if (node == null || node.value == null) {
      return null;
    }
    // Resolve the Key if missing
    return resolveKey(node, trie);
  }



  @Override
  public final Comparator<? super K> comparator() {
    return codec.comparator();
  }

  /**
   * Compares two keys using the correct comparison method for this class
   *
   * @param k1 first Key
   * @param k2 second Key
   * @return a negative integer, zero, or a positive integer as the first
   *         argument is less than, equal to, or greater than the second.
   */
  @SuppressWarnings("unchecked")
  protected final int compare(final K k1, final K k2) {
    return codec.comparator() == null ? ((Comparable<? super K>) k1).compareTo(k2)
        : codec.comparator().compare(k1, k2);
  }



  // NavigableMap methods:

  /**
   * @param key search key
   * @return the ceiling Node (the Node greater than or equal to the given key,
   *         or null)
   */
  protected Node<K, V> ceilingNode(final K key) {
    return ceilingOrHigherNode(key, false);
  }

  /**
   * @param key search key
   * @return the higher Node (the Node greater than the given key, or null)
   */
  protected Node<K, V> higherNode(final K key) {
    return ceilingOrHigherNode(key, true);
  }

  /**
   * @param key search key
   * @param higher true if the Node should be higher than the key
   * @return the ceiling Node (the Node greater than or equal to the given key,
   *         or null), or the higher Node (the Node greater than the given key,
   *         or null) if {@code higher} is {@code true}
   */
  protected Node<K, V> ceilingOrHigherNode(final K key, final boolean higher) {

    if (key == null || this.isEmpty()) {
      return null;
    }

    final int stopDepth = codec.length(key);

    // Look up a single record
    boolean left;
    Node<K, V> predecessor;
    Node<K, V> subNode = root;
    int i = 0;
    while (true) {
      predecessor = subNode;
      if (left = codec.isLeft(key, i++)) {
        subNode = subNode.left;
      } else {
        subNode = subNode.right;
      }

      if (subNode == null) {
        // If the element is left, but we have no left,
        // then the successor of our parent must be greater than us
        if (left) {
          return successor(predecessor);
        }
        // If the element is right, but we have no right,
        // then the successor of our parent might be less than us,
        // so we have to find the successor pretending that we do exist
        subNode = predecessor;
        while (subNode.parent != null) {

          if (subNode == subNode.parent.left && subNode.parent.right != null) {

            if (subNode.parent.right.value == null) {
              return successor(subNode.parent.right);
            }
            return subNode.parent.right;
          }
          subNode = subNode.parent;
        }
        return null;
      }
      if (subNode.value != null && i == stopDepth) {
        return higher ? successor(subNode) : subNode;
      }
      if (i >= stopDepth) {
        return successor(subNode);
      }
    }
  }

  @Override
  public Map.Entry<K, V> ceilingEntry(final K key) {
    if (key == null) {
      throw new NullPointerException(getClass().getName()
          + " does not accept null keys: " + key);
    }
    return exportEntry(ceilingNode(key), this);
  }

  @Override
  public K ceilingKey(final K key) {
    if (key == null) {
      throw new NullPointerException(getClass().getName()
          + " does not accept null keys: " + key);
    }
    return keyOrNull(ceilingNode(key), this);
  }

  @Override
  public Map.Entry<K, V> higherEntry(final K key) {
    if (key == null) {
      throw new NullPointerException(getClass().getName()
          + " does not accept null keys: " + key);
    }
    return exportEntry(higherNode(key), this);
  }

  @Override
  public K higherKey(final K key) {
    if (key == null) {
      throw new NullPointerException(getClass().getName()
          + " does not accept null keys: " + key);
    }
    return keyOrNull(higherNode(key), this);
  }



  /**
   * @param key search key
   * @return the floor Node (the Node lower than or equal to the given key,
   *         or null)
   */
  protected Node<K, V> floorNode(final K key) {
    return floorOrLowerNode(key, false);
  }

  /**
   * @param key search key
   * @return the lower Node (the Node lower than the given key, or null)
   */
  protected Node<K, V> lowerNode(final K key) {
    return floorOrLowerNode(key, true);
  }

  /**
   * @param key search key
   * @param lower true if the Node should be lower than the key
   * @return the floor Node (the Node lower than or equal to the given key,
   *         or null), or the lower Node (the Node lower than the given key,
   *         or null) if {@code lower} is {@code true}
   */
  protected Node<K, V> floorOrLowerNode(final K key, final boolean lower) {

    if (key == null) {
      return null;
    }

    final int stopDepth = codec.length(key);

    // Look up a single record
    boolean left;
    Node<K, V> predecessor;
    Node<K, V> subNode = root;
    int i = 0;
    while (true) {
      predecessor = subNode;
      if (left = codec.isLeft(key, i++)) {
        subNode = subNode.left;
      } else {
        subNode = subNode.right;
      }

      if (subNode == null) {
        // If element is left, or we have no left sibling,
        // then our predecessor (parent) must be lower than us
        if (left || predecessor.left == null) {
          return predecessor.value != null ? predecessor : predecessor(predecessor);
        }
        // If element is right and have a left sibling,
        // then our predecessor must the right-most leaf of our left sibling
        subNode = predecessor.left;
        while (subNode.right != null || subNode.left != null) {
          if (subNode.right != null) {
            subNode = subNode.right;
          } else {
            subNode = subNode.left;
          }
        }

        if (subNode.value == null) {
          throw new IllegalStateException("Should not have a leaf node with no value");
        }
        return subNode;

      }
      if (subNode.value != null && i == stopDepth) {
        return lower ? predecessor(subNode) : subNode;
      }
      if (i >= stopDepth) {
        return predecessor(subNode);
      }
    }
  }

  @Override
  public Map.Entry<K, V> floorEntry(final K key) {
    if (key == null) {
      throw new NullPointerException(getClass().getName()
          + " does not accept null keys: " + key);
    }
    return exportEntry(floorNode(key), this);
  }

  @Override
  public K floorKey(final K key) {
    if (key == null) {
      throw new NullPointerException(getClass().getName()
          + " does not accept null keys: " + key);
    }
    return keyOrNull(floorNode(key), this);
  }

  @Override
  public Map.Entry<K, V> lowerEntry(final K key) {
    if (key == null) {
      throw new NullPointerException(getClass().getName()
          + " does not accept null keys: " + key);
    }
    return exportEntry(lowerNode(key), this);
  }

  @Override
  public K lowerKey(final K key) {
    if (key == null) {
      throw new NullPointerException(getClass().getName()
          + " does not accept null keys: " + key);
    }
    return keyOrNull(lowerNode(key), this);
  }



  @Override
  public Map.Entry<K, V> firstEntry() {
    return exportEntry(firstNode(), this);
  }

  @Override
  public K firstKey() {
    return keyOrNoSuchElementException(successor(root), this);
  }

  @Override
  public Map.Entry<K, V> pollFirstEntry() {
    final Node<K, V> polled = firstNode();
    final Map.Entry<K, V> result = exportEntry(polled, this);
    if (polled != null) {
      deleteNode(polled);
    }
    return result;
  }



  @Override
  public Map.Entry<K, V> lastEntry() {
    return exportEntry(lastNode(), this);
  }

  @Override
  public K lastKey() {
    return keyOrNoSuchElementException(lastNode(), this);
  }

  @Override
  public Map.Entry<K, V> pollLastEntry() {
    final Node<K, V> polled = lastNode();
    final Map.Entry<K, V> result = exportEntry(polled, this);
    if (polled != null && polled != root) {
      deleteNode(polled);
    }
    return result;
  }



  // Views:

  @Override
  public NavigableSet<K> keySet() {
    return navigableKeySet();
  }

  @Override
  public NavigableSet<K> navigableKeySet() {
    if (navigableKeySet == null) {
      navigableKeySet = new NavigableTrieKeySet<K>(this);
    }
    return navigableKeySet;
  }

  /** NavigableTrieKeySet key set view implementing NavigableSet */
  protected static final class NavigableTrieKeySet<E> extends AbstractSet<E>
      implements NavigableSet<E> {

    protected final NavigableTrie<E, ? extends Object> m;

    /**
     * Creates a new NavigableTrieKeySet view
     *
     * @param map the backing trie map
     */
    protected NavigableTrieKeySet(final NavigableTrie<E, ? extends Object> map) {
      m = map;
    }

    @Override
    public final Iterator<E> iterator() {
      if (m instanceof AbstractBinaryTrie) {
        return ((AbstractNavigableBinaryTrie<E, ? extends Object>) m).keyIterator();
      } else {
        return (((AbstractNavigableBinaryTrie.NavigableTrieSubMap<E, ? extends Object>) m)
            .keyIterator());
      }
    }

    @Override
    public final Iterator<E> descendingIterator() {
      if (m instanceof AbstractBinaryTrie) {
        return ((AbstractNavigableBinaryTrie<E, ? extends Object>) m).descendingKeyIterator();
      } else {
        return (((AbstractNavigableBinaryTrie.NavigableTrieSubMap<E, ? extends Object>) m)
            .descendingKeyIterator());
      }
    }

    @Override
    public final int size() {
      return m.size();
    }

    @Override
    public final boolean isEmpty() {
      return m.isEmpty();
    }

    @Override
    public final boolean contains(final Object o) {
      return m.containsKey(o);
    }

    @Override
    public final void clear() {
      m.clear();
    }

    @Override
    public final E lower(final E e) {
      return m.lowerKey(e);
    }

    @Override
    public final E floor(final E e) {
      return m.floorKey(e);
    }

    @Override
    public final E ceiling(final E e) {
      return m.ceilingKey(e);
    }

    @Override
    public final E higher(final E e) {
      return m.higherKey(e);
    }

    @Override
    public final E first() {
      return m.firstKey();
    }

    @Override
    public final E last() {
      return m.lastKey();
    }

    @Override
    public final Comparator<? super E> comparator() {
      return m.comparator();
    }

    @Override
    public final E pollFirst() {
      final Map.Entry<E, ? extends Object> e = m.pollFirstEntry();
      return (e == null) ? null : e.getKey();
    }

    @Override
    public final E pollLast() {
      final Map.Entry<E, ? extends Object> e = m.pollLastEntry();
      return (e == null) ? null : e.getKey();
    }

    @Override
    public final boolean remove(final Object o) {
      return m.remove(o) != null;
    }

    @Override
    public final NavigableSet<E> subSet(final E fromElement, final boolean fromInclusive,
        final E toElement, final boolean toInclusive) {
      return new NavigableTrieKeySet<>(m.subMap(fromElement, fromInclusive,
          toElement, toInclusive));
    }

    @Override
    public final NavigableSet<E> headSet(final E toElement, final boolean inclusive) {
      return new NavigableTrieKeySet<>(m.headMap(toElement, inclusive));
    }

    @Override
    public final NavigableSet<E> tailSet(final E fromElement, final boolean inclusive) {
      return new NavigableTrieKeySet<>(m.tailMap(fromElement, inclusive));
    }

    @Override
    public final SortedSet<E> subSet(final E fromElement, final E toElement) {
      return subSet(fromElement, true, toElement, false);
    }

    @Override
    public final SortedSet<E> headSet(final E toElement) {
      return headSet(toElement, false);
    }

    @Override
    public final SortedSet<E> tailSet(final E fromElement) {
      return tailSet(fromElement, true);
    }

    @Override
    public final NavigableSet<E> descendingSet() {
      return new NavigableTrieKeySet<E>(m.descendingMap());
    }

  }



  /**
   * @return Iterator returning resolved keys in descending order
   */
  protected final Iterator<K> descendingKeyIterator() {
    return new DescendingKeyIterator<K, V>(this);
  }

  /** Iterator for returning only keys in descending order */
  protected static final class DescendingKeyIterator<K, V> extends AbstractEntryIterator<K, V, K> {

    protected DescendingKeyIterator(final AbstractBinaryTrie<K, V> map) {
      super(map, map.lastNode());
    }

    @Override
    public final K next() {
      return exportEntry(prevNode(), m).getKey();
    }
  }



  @Override
  public final NavigableTrie<K, V> descendingMap() {
    final NavigableTrie<K, V> km = descendingMap;
    return (km != null) ? km : (descendingMap = new DescendingSubMap<K, V>(this,
        true, null, true,
        true, null, true));
  }



  @Override
  public final NavigableSet<K> descendingKeySet() {
    return descendingMap().navigableKeySet();
  }



  @Override
  public final NavigableTrie<K, V> subMap(final K fromKey, final K toKey) {
    return subMap(fromKey, true, toKey, false);
  }

  @Override
  public final NavigableTrie<K, V> subMap(final K fromKey, final boolean fromInclusive,
      final K toKey, final boolean toInclusive) {
    return new AscendingSubMap<K, V>(this,
        false, fromKey, fromInclusive,
        false, toKey, toInclusive);
  }



  @Override
  public final NavigableTrie<K, V> headMap(final K toKey) {
    return headMap(toKey, false);
  }

  @Override
  public final NavigableTrie<K, V> headMap(final K toKey, final boolean inclusive) {
    return new AscendingSubMap<K, V>(this,
        true, null, true,
        false, toKey, inclusive);
  }



  @Override
  public final NavigableTrie<K, V> tailMap(final K fromKey) {
    return tailMap(fromKey, true);
  }

  @Override
  public final NavigableTrie<K, V> tailMap(final K fromKey, final boolean inclusive) {
    return new AscendingSubMap<K, V>(this,
        false, fromKey, inclusive,
        true, null, true);
  }



  // Sub-Map View Implementations:

  /** AscendingSubMap sub map view with ascending order */
  protected static final class AscendingSubMap<K, V> extends NavigableTrieSubMap<K, V> {

    private static final long serialVersionUID = 912986545866124060L;

    /**
     * Create a new AscendingSubMap sub map view.
     *
     * <p>
     * Endpoints are represented as triples
     * (fromStart, lo, loInclusive) and (toEnd, hi, hiInclusive).
     *
     * If fromStart is true, then the low (absolute) bound is the
     * start of the backing map, and the other values are ignored.
     *
     * Otherwise, if loInclusive is true, lo is the inclusive bound,
     * else lo is the exclusive bound. Similarly for the upper bound.
     *
     * @param map the backing map
     * @param fromStart
     * @param lo
     * @param loInclusive
     * @param toEnd
     * @param hi
     * @param hiInclusive
     */
    protected AscendingSubMap(final AbstractNavigableBinaryTrie<K, V> map,
        final boolean fromStart, final K lo, final boolean loInclusive,
        final boolean toEnd, final K hi, final boolean hiInclusive) {
      super(map, fromStart, lo, loInclusive, toEnd, hi, hiInclusive);
    }

    @Override
    public final Comparator<? super K> comparator() {
      return m.comparator();
    }

    @Override
    public final NavigableTrie<K, V> subMap(final K fromKey, final boolean fromInclusive,
        final K toKey, final boolean toInclusive) {

      if (!inRange(fromKey, fromInclusive)) {
        throw new IllegalArgumentException("fromKey out of range: " + fromKey);
      }
      if (!inRange(toKey, toInclusive)) {
        throw new IllegalArgumentException("toKey out of range: " + toKey);
      }
      return new AscendingSubMap<K, V>(m,
          false, fromKey, fromInclusive,
          false, toKey, toInclusive);
    }

    @Override
    public final NavigableTrie<K, V> headMap(final K toKey, final boolean inclusive) {

      if (!inRange(toKey, inclusive)) {
        throw new IllegalArgumentException("toKey out of range: " + toKey);
      }
      return new AscendingSubMap<K, V>(m,
          fromStart, lo, loInclusive,
          false, toKey, inclusive);
    }

    @Override
    public final NavigableTrie<K, V> tailMap(final K fromKey, final boolean inclusive) {

      if (!inRange(fromKey, inclusive)) {
        throw new IllegalArgumentException("fromKey out of range: " + fromKey);
      }
      return new AscendingSubMap<K, V>(m,
          false, fromKey, inclusive,
          toEnd, hi, hiInclusive);
    }

    @Override
    public final NavigableTrie<K, V> descendingMap() {

      final NavigableTrie<K, V> mv = descendingSubMapView;
      return (mv != null) ? mv : (descendingSubMapView =
          new DescendingSubMap<K, V>(m,
              fromStart, lo, loInclusive,
              toEnd, hi, hiInclusive));
    }

    @Override
    public Collection<V> values() {
      if (valuesSubMapView == null) {
        valuesSubMapView = new AscendingTrieSubMapValues();
      }
      return valuesSubMapView;
    }

    protected final class AscendingTrieSubMapValues extends TrieSubMapValues {
      @Override
      public final Iterator<V> iterator() {
        return new SubMapValueIterator(absLowest(), absHighFence());
      }

      @Override
      protected final Iterator<Node<K, V>> getSubMapNodeIterator() {
        return new SubMapNodeIterator(absLowest(), absHighFence());
      }
    }

    @Override
    protected final Iterator<K> keyIterator() {
      return new SubMapKeyIterator(absLowest(), absHighFence());
    }

    @Override
    protected final Iterator<K> descendingKeyIterator() {
      return new DescendingSubMapKeyIterator(absHighest(), absLowFence());
    }

    protected final class AscendingEntrySetView extends TrieEntrySetSubMapView {
      @Override
      public Iterator<Map.Entry<K, V>> iterator() {
        return new SubMapEntryIterator(absLowest(), absHighFence());
      }
    }

    @Override
    public final Set<Map.Entry<K, V>> entrySet() {
      final TrieEntrySetSubMapView es = entrySetSubMapView;
      return (es != null) ? es : new AscendingEntrySetView();
    }

    @Override
    protected final Node<K, V> subLowest() {
      return absLowest();
    }

    @Override
    protected final Node<K, V> subHighest() {
      return absHighest();
    }

    @Override
    protected final Node<K, V> subCeiling(final K key) {
      return absCeiling(key);
    }

    @Override
    protected final Node<K, V> subHigher(final K key) {
      return absHigher(key);
    }

    @Override
    protected final Node<K, V> subFloor(final K key) {
      return absFloor(key);
    }

    @Override
    protected final Node<K, V> subLower(final K key) {
      return absLower(key);
    }

  }



  /** DescendingSubMap sub map view with descending order */
  protected static final class DescendingSubMap<K, V> extends NavigableTrieSubMap<K, V> {

    private static final long serialVersionUID = 912986545866120460L;

    /**
     * Create a new DescendingSubMap sub map view.
     *
     * <p>
     * Endpoints are represented as triples
     * (fromStart, lo, loInclusive) and (toEnd, hi, hiInclusive).
     *
     * If fromStart is true, then the low (absolute) bound is the
     * start of the backing map, and the other values are ignored.
     *
     * Otherwise, if loInclusive is true, lo is the inclusive bound,
     * else lo is the exclusive bound. Similarly for the upper bound.
     *
     * @param map the backing map
     * @param fromStart
     * @param lo
     * @param loInclusive
     * @param toEnd
     * @param hi
     * @param hiInclusive
     */
    protected DescendingSubMap(final AbstractNavigableBinaryTrie<K, V> m,
        final boolean fromStart, final K lo, final boolean loInclusive,
        final boolean toEnd, final K hi, final boolean hiInclusive) {
      super(m, fromStart, lo, loInclusive, toEnd, hi, hiInclusive);
    }

    protected final Comparator<? super K> reverseComparator =
        Collections.reverseOrder(m.comparator());

    @Override
    public final Comparator<? super K> comparator() {
      return reverseComparator;
    }

    @Override
    public final NavigableTrie<K, V> subMap(final K fromKey, final boolean fromInclusive,
        final K toKey, final boolean toInclusive) {

      if (!inRange(fromKey, fromInclusive)) {
        throw new IllegalArgumentException("fromKey out of range: " + fromKey);
      }
      if (!inRange(toKey, toInclusive)) {
        throw new IllegalArgumentException("toKey out of range: " + toKey);
      }
      return new DescendingSubMap<K, V>(m,
          false, toKey, toInclusive,
          false, fromKey, fromInclusive);
    }

    @Override
    public final NavigableTrie<K, V> headMap(final K toKey, final boolean inclusive) {

      if (!inRange(toKey, inclusive)) {
        throw new IllegalArgumentException("toKey out of range: " + toKey);
      }
      return new DescendingSubMap<K, V>(m,
          false, toKey, inclusive,
          toEnd, hi, hiInclusive);
    }

    @Override
    public final NavigableTrie<K, V> tailMap(final K fromKey, final boolean inclusive) {

      if (!inRange(fromKey, inclusive)) {
        throw new IllegalArgumentException("fromKey out of range: " + fromKey);
      }
      return new DescendingSubMap<K, V>(m,
          fromStart, lo, loInclusive,
          false, fromKey, inclusive);
    }

    @Override
    public final NavigableTrie<K, V> descendingMap() {

      final NavigableTrie<K, V> mv = descendingSubMapView;
      return (mv != null) ? mv : (descendingSubMapView =
          new AscendingSubMap<K, V>(m,
              fromStart, lo, loInclusive,
              toEnd, hi, hiInclusive));
    }

    @Override
    public Collection<V> values() {
      if (valuesSubMapView == null) {
        valuesSubMapView = new DescendingTrieSubMapValues();
      }
      return valuesSubMapView;
    }

    /** Iterator for returning only values in descending order */
    protected final class DescendingTrieSubMapValues extends TrieSubMapValues {

      @Override
      public final Iterator<V> iterator() {
        return new DescendingSubMapValueIterator(absHighest(), absLowFence());
      }

      @Override
      protected final Iterator<Node<K, V>> getSubMapNodeIterator() {
        return new DescendingSubMapNodeIterator(absHighest(), absLowFence());
      }
    }

    @Override
    protected final Iterator<K> keyIterator() {
      return new DescendingSubMapKeyIterator(absHighest(), absLowFence());
    }

    @Override
    protected final Iterator<K> descendingKeyIterator() {
      return new SubMapKeyIterator(absLowest(), absHighFence());
    }

    /** Iterator for returning entries (key-value pairs) in descending order */
    protected final class DescendingEntrySetView extends TrieEntrySetSubMapView {
      @Override
      public Iterator<Map.Entry<K, V>> iterator() {
        return new DescendingSubMapEntryIterator(absHighest(), absLowFence());
      }
    }

    @Override
    public final Set<Map.Entry<K, V>> entrySet() {
      final TrieEntrySetSubMapView es = entrySetSubMapView;
      return (es != null) ? es : new DescendingEntrySetView();
    }

    @Override
    protected final Node<K, V> subLowest() {
      return absHighest();
    }

    @Override
    protected final Node<K, V> subHighest() {
      return absLowest();
    }

    @Override
    protected final Node<K, V> subCeiling(final K key) {
      return absFloor(key);
    }

    @Override
    protected final Node<K, V> subHigher(final K key) {
      return absLower(key);
    }

    @Override
    protected final Node<K, V> subFloor(final K key) {
      return absCeiling(key);
    }

    @Override
    protected final Node<K, V> subLower(final K key) {
      return absHigher(key);
    }

  }



  /** Base NavigableTrieSubMap sub-map view class for extending */
  protected abstract static class NavigableTrieSubMap<K, V> extends AbstractMap<K, V>
      implements NavigableTrie<K, V>, NavigableMap<K, V>, Serializable {

    private static final long serialVersionUID = 4159238497306996386L;

    /** The backing map. */
    protected final AbstractNavigableBinaryTrie<K, V> m;

    /**
     * Endpoints are represented as triples
     * (fromStart, lo, loInclusive) and (toEnd, hi, hiInclusive).
     *
     * If fromStart is true, then the low (absolute) bound is the
     * start of the backing map, and the other values are ignored.
     *
     * Otherwise, if loInclusive is true, lo is the inclusive bound,
     * else lo is the exclusive bound. Similarly for the upper bound.
     */
    protected final K lo, hi;
    protected final boolean fromStart, toEnd;
    protected final boolean loInclusive, hiInclusive;

    // Views
    protected transient NavigableTrie<K, V> descendingSubMapView = null;
    protected transient TrieEntrySetSubMapView entrySetSubMapView = null;
    protected transient NavigableTrieKeySet<K> navigableKeySetSubMapView = null;
    protected transient Collection<V> valuesSubMapView = null;


    protected NavigableTrieSubMap(final AbstractNavigableBinaryTrie<K, V> m,
        final boolean fromStart, final K lo, final boolean loInclusive,
        final boolean toEnd, final K hi, final boolean hiInclusive) {

      if (!fromStart && !toEnd) {
        if (m.compare(lo, hi) > 0) {
          throw new IllegalArgumentException("fromKey > toKey: fromKey: "
              + lo + ", toKey: " + hi);
        }

      } else {

        // Type check
        if (!fromStart) {
          m.compare(lo, lo);
        }

        if (!toEnd) {
          m.compare(hi, hi);
        }
      }

      this.m = m;
      this.fromStart = fromStart;
      this.lo = lo;
      this.loInclusive = loInclusive;
      this.toEnd = toEnd;
      this.hi = hi;
      this.hiInclusive = hiInclusive;
    }

    // internal utilities

    protected final boolean tooLow(final K key) {
      if (!fromStart) {
        final int c = m.compare(key, lo);
        if (c < 0 || (c == 0 && !loInclusive)) {
          return true;
        }
      }
      return false;
    }

    protected final boolean tooHigh(final K key) {
      if (!toEnd) {
        final int c = m.compare(key, hi);
        if (c > 0 || (c == 0 && !hiInclusive)) {
          return true;
        }
      }
      return false;
    }

    protected final boolean inRange(final K key) {
      return !tooLow(key) && !tooHigh(key);
    }

    protected final boolean inClosedRange(final K key) {
      return (fromStart || m.compare(key, lo) >= 0)
          && (toEnd || m.compare(hi, key) >= 0);
    }

    protected final boolean inRange(final K key, final boolean inclusive) {
      return inclusive ? inRange(key) : inClosedRange(key);
    }

    /*
     * Absolute versions of relation operations.
     * Subclasses map to these using like-named "sub"
     * versions that invert senses for descending maps
     */

    protected final Node<K, V> absLowest() {
      final Node<K, V> e = (fromStart ? m.firstNode()
          : (loInclusive ? m.ceilingNode(lo) : m.higherNode(lo)));
      return (e == null || tooHigh(resolveKey(e, m))) ? null : e;
    }

    protected final Node<K, V> absHighest() {
      final Node<K, V> e =
          (toEnd ? m.lastNode() : (hiInclusive ? m.floorNode(hi) : m.lowerNode(hi)));
      return (e == null || tooLow(resolveKey(e, m))) ? null : e;
    }

    protected final Node<K, V> absCeiling(final K key) {
      if (tooLow(key)) {
        return absLowest();
      }
      final Node<K, V> e = m.ceilingNode(key);
      return (e == null || tooHigh(resolveKey(e, m))) ? null : e;
    }

    protected final Node<K, V> absHigher(final K key) {
      if (tooLow(key)) {
        return absLowest();
      }
      final Node<K, V> e = m.higherNode(key);
      return (e == null || tooHigh(resolveKey(e, m))) ? null : e;
    }

    protected final Node<K, V> absFloor(final K key) {
      if (tooHigh(key)) {
        return absHighest();
      }
      final Node<K, V> e = m.floorNode(key);
      return (e == null || tooLow(resolveKey(e, m))) ? null : e;
    }

    protected final Node<K, V> absLower(final K key) {
      if (tooHigh(key)) {
        return absHighest();
      }
      final Node<K, V> e = m.lowerNode(key);
      return (e == null || tooLow(resolveKey(e, m))) ? null : e;
    }

    /** Returns the absolute high fence for ascending traversal */
    protected final Node<K, V> absHighFence() {
      return (toEnd ? null : (hiInclusive ? m.higherNode(hi) : m.ceilingNode(hi)));
    }

    /** Return the absolute low fence for descending traversal */
    protected final Node<K, V> absLowFence() {
      return (fromStart ? null : (loInclusive ? m.lowerNode(lo) : m.floorNode(lo)));
    }

    // Abstract methods defined in ascending vs descending classes
    // These relay to the appropriate absolute versions

    protected abstract Node<K, V> subLowest();

    protected abstract Node<K, V> subHighest();

    protected abstract Node<K, V> subCeiling(K key);

    protected abstract Node<K, V> subHigher(K key);

    protected abstract Node<K, V> subFloor(K key);

    protected abstract Node<K, V> subLower(K key);

    /** Returns ascending iterator from the perspective of this submap */
    protected abstract Iterator<K> keyIterator();

    /** Returns descending iterator from the perspective of this submap */
    protected abstract Iterator<K> descendingKeyIterator();

    @Override
    public abstract Collection<V> values();

    // public methods

    @Override
    public final boolean isEmpty() {
      return (fromStart && toEnd) ? m.isEmpty() : entrySet().isEmpty();
    }

    @Override
    public final int size() {
      return (fromStart && toEnd) ? m.size() : entrySet().size();
    }

    @SuppressWarnings("unchecked")
    @Override
    public final boolean containsKey(final Object key) {
      return inRange((K) key) && m.containsKey(key);
    }

    @Override
    public final V put(final K key, final V value) {
      if (!inRange(key)) {
        throw new IllegalArgumentException("key out of range: " + key);
      }
      return m.put(key, value);
    }

    @SuppressWarnings("unchecked")
    @Override
    public final V get(final Object key) {
      return !inRange((K) key) ? null : m.get(key);
    }

    @SuppressWarnings("unchecked")
    @Override
    public final V remove(final Object key) {
      return !inRange((K) key) ? null : m.remove(key);
    }

    @Override
    public final Map.Entry<K, V> ceilingEntry(final K key) {
      return exportEntry(subCeiling(key), m);
    }

    @Override
    public final K ceilingKey(final K key) {
      return keyOrNull(subCeiling(key), m);
    }

    @Override
    public final Map.Entry<K, V> higherEntry(final K key) {
      return exportEntry(subHigher(key), m);
    }

    @Override
    public final K higherKey(final K key) {
      return keyOrNull(subHigher(key), m);
    }

    @Override
    public final Map.Entry<K, V> floorEntry(final K key) {
      return exportEntry(subFloor(key), m);
    }

    @Override
    public final K floorKey(final K key) {
      return keyOrNull(subFloor(key), m);
    }

    @Override
    public final Map.Entry<K, V> lowerEntry(final K key) {
      return exportEntry(subLower(key), m);
    }

    @Override
    public final K lowerKey(final K key) {
      return keyOrNull(subLower(key), m);
    }

    @Override
    public final K firstKey() {
      return keyOrNoSuchElementException(subLowest(), m);
    }

    @Override
    public final K lastKey() {
      return keyOrNoSuchElementException(subHighest(), m);
    }

    @Override
    public final Map.Entry<K, V> firstEntry() {
      return exportEntry(subLowest(), m);
    }

    @Override
    public final Map.Entry<K, V> lastEntry() {
      return exportEntry(subHighest(), m);
    }

    @Override
    public final Map.Entry<K, V> pollFirstEntry() {
      final Node<K, V> e = subLowest();
      final Map.Entry<K, V> result = exportEntry(e, m);
      if (e != null) {
        m.deleteNode(e);
      }
      return result;
    }

    @Override
    public final Map.Entry<K, V> pollLastEntry() {
      final Node<K, V> e = subHighest();
      final Map.Entry<K, V> result = exportEntry(e, m);
      if (e != null) {
        m.deleteNode(e);
      }
      return result;
    }

    @Override
    public final NavigableSet<K> navigableKeySet() {
      final NavigableTrieKeySet<K> nksv = navigableKeySetSubMapView;
      return (nksv != null) ? nksv : (navigableKeySetSubMapView = new NavigableTrieKeySet<K>(this));
    }

    @Override
    public final Set<K> keySet() {
      return navigableKeySet();
    }

    @Override
    public final NavigableSet<K> descendingKeySet() {
      return descendingMap().navigableKeySet();
    }

    @Override
    public final NavigableTrie<K, V> subMap(final K fromKey, final K toKey) {
      return subMap(fromKey, true, toKey, false);
    }

    @Override
    public final NavigableTrie<K, V> headMap(final K toKey) {
      return headMap(toKey, false);
    }

    @Override
    public final NavigableTrie<K, V> tailMap(final K fromKey) {
      return tailMap(fromKey, true);
    }



    // Trie methods

    @Override
    public V shortestPrefixOfValue(final K key, final boolean keyInclusive) {
      final Iterator<V> iter = prefixOfValues(key, keyInclusive).iterator();
      return iter.hasNext() ? iter.next() : null;
    }

    @Override
    public V longestPrefixOfValue(final K key, final boolean keyInclusive) {
      final Iterator<V> iter = prefixOfValues(key, keyInclusive).iterator();
      V value = null;
      while (iter.hasNext()) {
        value = iter.next();
      }
      return value;
    }

    @Override
    public Collection<V> prefixOfValues(final K key, final boolean keyInclusive) {
      return prefixValues(key, true, keyInclusive, false);
    }

    @Override
    public Collection<V> prefixedByValues(final K key, final boolean keyInclusive) {
      return prefixValues(key, false, keyInclusive, true);
    }

    protected Collection<V> prefixValues(final K key, final boolean includePrefixOf,
        final boolean keyInclusive, final boolean includePrefixedBy) {
      if (key == null) {
        throw new NullPointerException(getClass().getName() + " does not accept null keys: " + key);
      }
      if (m.codec.length(key) <= 0) {
        throw new IllegalArgumentException(getClass().getName()
            + " does not accept keys of length <= 0: " + key);
      }
      if (includePrefixOf) {
        return new TriePrefixSubMapValues(null, false, key, keyInclusive);
      }
      return new TriePrefixSubMapValues(key, keyInclusive, null, false);
    }

    @Override
    public Trie<K, V> prefixOfMap(final K key, final boolean keyInclusive) {
      return prefixMap(key, true, keyInclusive, false);
    }

    @Override
    public Trie<K, V> prefixedByMap(final K key, final boolean keyInclusive) {
      return prefixMap(key, false, keyInclusive, true);
    }

    protected Trie<K, V> prefixMap(final K key, final boolean includePrefixOf,
        final boolean keyInclusive, final boolean includePrefixedBy) {
      if (key == null) {
        throw new NullPointerException(getClass().getName() + " does not accept null keys: " + key);
      }
      if (m.codec.length(key) <= 0) {
        throw new IllegalArgumentException(getClass().getName()
            + " does not accept keys of length <= 0: " + key);
      }

      if (includePrefixOf) {
        return new TriePrefixSubMap(null, false, key, keyInclusive);
      }
      return new TriePrefixSubMap(key, keyInclusive, null, false);
    }



    /** TriePrefixSubMapEntrySet prefix entry set sub map view */
    protected class TriePrefixSubMapEntrySet extends TriePrefixEntrySet<K, V> {

      /**
       * Create a new TriePrefixSubMapEntrySet View
       *
       * @param mustBePrefixedBy null or the key that all must be prefixed by
       * @param mustBePrefixedByInclusive true if the mustBePrefixedBy is inclusive
       * @param mustBePrefixOf null or the key that all must be prefixes of
       * @param mustBePrefixOfInclusive true if the mustBePrefixOf is inclusive
       */
      protected TriePrefixSubMapEntrySet(final K mustBePrefixedBy,
          final boolean mustBePrefixedByInclusive, final K mustBePrefixOf,
          final boolean mustBePrefixOfInclusive) {
        super(m, mustBePrefixedBy, mustBePrefixedByInclusive, mustBePrefixOf,
            mustBePrefixOfInclusive);
      }

      @Override
      public final Iterator<Map.Entry<K, V>> iterator() {
        return new EntryPrefixSubMapIterator(mustBePrefixedBy, mustBePrefixedByInclusive,
            mustBePrefixOf, mustBePrefixOfInclusive);
      }

      @Override
      protected boolean inRange(final K key) {
        return super.inRange(key) && NavigableTrieSubMap.this.inRange(key, true);
      }
    }


    /** Sub Map View class for a Set of Keys that are prefixes of a Key. */
    protected class TriePrefixSubMapKeySet extends TriePrefixKeySet<K, V> {

      /**
       * Create a new TriePrefixSubMapKeySet View
       *
       * @param mustBePrefixedBy null or the key that all must be prefixed by
       * @param mustBePrefixedByInclusive true if the mustBePrefixedBy is inclusive
       * @param mustBePrefixOf null or the key that all must be prefixes of
       * @param mustBePrefixOfInclusive true if the mustBePrefixOf is inclusive
       */
      protected TriePrefixSubMapKeySet(final K mustBePrefixedBy,
          final boolean mustBePrefixedByInclusive, final K mustBePrefixOf,
          final boolean mustBePrefixOfInclusive) {
        super(m, mustBePrefixedBy, mustBePrefixedByInclusive, mustBePrefixOf,
            mustBePrefixOfInclusive);
      }

      @Override
      public Iterator<K> iterator() {
        return new KeyPrefixSubMapIterator(mustBePrefixedBy, mustBePrefixedByInclusive,
            mustBePrefixOf, mustBePrefixOfInclusive);
      }

      @Override
      protected boolean inRange(final K key) {
        return super.inRange(key) && NavigableTrieSubMap.this.inRange(key, true);
      }
    }


    /** TriePrefixSubMapValues values collection view */
    protected class TriePrefixSubMapValues extends TriePrefixValues<K, V> {

      /**
       * Create a new TriePrefixSubMapValues View
       *
       * @param mustBePrefixedBy null or the key that all must be prefixed by
       * @param mustBePrefixedByInclusive true if the mustBePrefixedBy is inclusive
       * @param mustBePrefixOf null or the key that all must be prefixes of
       * @param mustBePrefixOfInclusive true if the mustBePrefixOf is inclusive
       */
      protected TriePrefixSubMapValues(final K mustBePrefixedBy,
          final boolean mustBePrefixedByInclusive, final K mustBePrefixOf,
          final boolean mustBePrefixOfInclusive) {
        super(m, mustBePrefixedBy, mustBePrefixedByInclusive, mustBePrefixOf,
            mustBePrefixOfInclusive);
      }

      @Override
      public Iterator<V> iterator() {
        return new ValuePrefixSubMapIterator(mustBePrefixedBy, mustBePrefixedByInclusive,
            mustBePrefixOf, mustBePrefixOfInclusive);
      }

      @Override
      public boolean remove(final Object o) {
        Node<K, V> node = null;
        // only remove values that occur in this sub-trie
        final Iterator<Node<K, V>> iter =
            new NodePrefixSubMapIterator(mustBePrefixedBy, mustBePrefixedByInclusive,
                mustBePrefixOf, mustBePrefixOfInclusive);
        while (iter.hasNext()) {
          node = iter.next();
          if (eq(node.value, o)) {
            iter.remove();
            return true;
          }
        }
        return false;
      }
    }



    /** Iterator for returning prefix entries in ascending order from a sub map */
    protected final class EntryPrefixSubMapIterator
        extends AbstractPrefixIterator<K, V, Map.Entry<K, V>> {

      protected EntryPrefixSubMapIterator(final K mustBePrefixedBy,
          final boolean mustBePrefixedByInclusive, final K mustBePrefixOf,
          final boolean mustBePrefixOfInclusive) {
        super(m, mustBePrefixedBy, mustBePrefixedByInclusive, mustBePrefixOf,
            mustBePrefixOfInclusive);
      }

      @Override
      public final Map.Entry<K, V> next() {
        return exportEntry(nextNode(), trie);
      }

      @Override
      protected boolean subInRange(final Node<K, V> node) {
        return super.subInRange(node)
            && NavigableTrieSubMap.this.inRange(resolveKey(node, m), true);
      }
    }

    /** Iterator for returning prefix keys in ascending order from a sub map */
    protected final class KeyPrefixSubMapIterator
        extends AbstractPrefixIterator<K, V, K> {

      protected KeyPrefixSubMapIterator(final K mustBePrefixedBy,
          final boolean mustBePrefixedByInclusive, final K mustBePrefixOf,
          final boolean mustBePrefixOfInclusive) {
        super(m, mustBePrefixedBy, mustBePrefixedByInclusive, mustBePrefixOf,
            mustBePrefixOfInclusive);
      }

      @Override
      public final K next() {
        return exportEntry(nextNode(), trie).getKey();
      }

      @Override
      protected boolean subInRange(final Node<K, V> node) {
        return super.subInRange(node)
            && NavigableTrieSubMap.this.inRange(resolveKey(node, m), true);
      }
    }

    /** Iterator for returning only prefix values in ascending order from a sub map */
    protected final class ValuePrefixSubMapIterator extends AbstractPrefixIterator<K, V, V> {

      protected ValuePrefixSubMapIterator(final K mustBePrefixedBy,
          final boolean mustBePrefixedByInclusive, final K mustBePrefixOf,
          final boolean mustBePrefixOfInclusive) {
        super(m, mustBePrefixedBy, mustBePrefixedByInclusive, mustBePrefixOf,
            mustBePrefixOfInclusive);
      }

      @Override
      public final V next() {
        return nextNode().value;
      }

      @Override
      protected boolean subInRange(final Node<K, V> node) {
        return super.subInRange(node)
            && NavigableTrieSubMap.this.inRange(resolveKey(node, m), true);
      }
    }

    /** Iterator for returning only prefix nodes in ascending order from a sub map */
    protected final class NodePrefixSubMapIterator
        extends AbstractPrefixIterator<K, V, Node<K, V>> {

      protected NodePrefixSubMapIterator(final K mustBePrefixedBy,
          final boolean mustBePrefixedByInclusive, final K mustBePrefixOf,
          final boolean mustBePrefixOfInclusive) {
        super(m, mustBePrefixedBy, mustBePrefixedByInclusive, mustBePrefixOf,
            mustBePrefixOfInclusive);
      }

      @Override
      public final Node<K, V> next() {
        return nextNode();
      }

      @Override
      protected boolean subInRange(final Node<K, V> node) {
        return super.subInRange(node)
            && NavigableTrieSubMap.this.inRange(resolveKey(node, m), true);
      }
    }



    /** TriePrefixSubMap prefix sub map view */
    protected class TriePrefixSubMap extends TriePrefixMap<K, V> {

      private static final long serialVersionUID = -3408129333725624796L;

      /**
       * Create a new TriePrefixSubMap prefix sub map view
       *
       * @param mustBePrefixedBy null or the key that all must be prefixed by
       * @param mustBePrefixedByInclusive true if the mustBePrefixedBy is inclusive
       * @param mustBePrefixOf null or the key that all must be prefixes of
       * @param mustBePrefixOfInclusive true if the mustBePrefixOf is inclusive
       */
      protected TriePrefixSubMap(final K mustBePrefixedBy, final boolean mustBePrefixedByInclusive,
          final K mustBePrefixOf, final boolean mustBePrefixOfInclusive) {
        super(m, mustBePrefixedBy, mustBePrefixedByInclusive, mustBePrefixOf,
            mustBePrefixOfInclusive);
      }


      @Override
      protected Collection<V> prefixValues(final K key, final boolean includePrefixOf,
          final boolean keyInclusive, final boolean includePrefixedBy) {
        if (key == null) {
          throw new NullPointerException(
              getClass().getName() + " does not accept null keys: " + key);
        }
        if (trie.codec.length(key) <= 0) {
          throw new IllegalArgumentException(getClass().getName()
              + " does not accept keys of length <= 0: " + key);
        }
        // !keyInclusive because if we want to make a non-inclusive map,
        // the range check should allow the key to match a non-inclusive prefix
        if (!inRange(key, !keyInclusive)) {
          throw new IllegalArgumentException("key out of range: " + key);
        }

        if (includePrefixOf) {
          // Wants prefix of, create with new prefix of key, pass along current mustBePrefixedBy
          return new TriePrefixSubMapValues(mustBePrefixedBy, mustBePrefixedByInclusive, key,
              keyInclusive);

        } else {
          // Wants prefixed by, create with new prefixed by key, pass current mustBePrefixOf
          return new TriePrefixSubMapValues(key, keyInclusive, mustBePrefixOf,
              mustBePrefixOfInclusive);
        }
      }

      @Override
      protected Trie<K, V> prefixMap(final K key, final boolean includePrefixOf,
          final boolean keyInclusive, final boolean includePrefixedBy) {
        if (key == null) {
          throw new NullPointerException(
              getClass().getName() + " does not accept null keys: " + key);
        }
        if (trie.codec.length(key) <= 0) {
          throw new IllegalArgumentException(getClass().getName()
              + " does not accept keys of length <= 0: " + key);
        }
        // !keyInclusive because if we want to make a non-inclusive map,
        // the range check should allow the key to match a non-inclusive prefix
        if (!inRange(key, !keyInclusive)) {
          throw new IllegalArgumentException("key out of range: " + key);
        }

        if (includePrefixOf) {
          // Wants prefix of, create with new prefix of key, pass along current mustBePrefixedBy
          return new TriePrefixSubMap(mustBePrefixedBy, mustBePrefixedByInclusive, key,
              keyInclusive);

        } else {
          // Wants prefixed by, create with new prefixed by key, pass current mustBePrefixOf
          return new TriePrefixSubMap(key, keyInclusive, mustBePrefixOf, mustBePrefixOfInclusive);
        }
      }


      @Override
      protected boolean inRange(final K key, final boolean forceInclusive) {
        return super.inRange(key, forceInclusive)
            && NavigableTrieSubMap.this.inRange(key, !forceInclusive);
      }


      @Override
      public Set<Map.Entry<K, V>> entrySet() {
        final Set<Map.Entry<K, V>> es = entrySet;
        return (es != null) ? es : (entrySet =
            new TriePrefixSubMapEntrySet(mustBePrefixedBy, mustBePrefixedByInclusive,
                mustBePrefixOf, mustBePrefixOfInclusive));
      }

      @Override
      public Set<K> keySet() {
        final Set<K> ks = keySet;
        return (ks != null) ? ks : (keySet =
            new TriePrefixSubMapKeySet(mustBePrefixedBy, mustBePrefixedByInclusive, mustBePrefixOf,
                mustBePrefixOfInclusive));
      }

      @Override
      public Collection<V> values() {
        final Collection<V> vs = values;
        return (vs != null) ? vs : (values =
            new TriePrefixSubMapValues(mustBePrefixedBy, mustBePrefixedByInclusive, mustBePrefixOf,
                mustBePrefixOfInclusive));
      }
    }



    // View classes

    /** TrieEntrySetSubMapView entry set sub map view */
    protected abstract class TrieEntrySetSubMapView extends AbstractSet<Map.Entry<K, V>> {

      private transient long size = -1L;
      private transient int sizeModCount = -1;

      @Override
      public final int size() {
        if (fromStart && toEnd) {
          return m.size();
        }
        if (size == -1L || sizeModCount != m.modCount) {
          sizeModCount = m.modCount;
          size = 0L;
          final Iterator<Map.Entry<K, V>> i = iterator();
          while (i.hasNext()) {
            ++size;
            i.next();
          }
        }
        return size > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) size;
      }

      @Override
      public final boolean isEmpty() {
        final Node<K, V> n = absLowest();
        return n == null || tooHigh(resolveKey(n, NavigableTrieSubMap.this.m));
      }

      @SuppressWarnings("unchecked")
      @Override
      public final boolean contains(final Object o) {
        if (!(o instanceof Map.Entry)) {
          return false;
        }
        final Map.Entry<K, V> entry = (Map.Entry<K, V>) o;
        final K key = entry.getKey();
        if (!inRange(key)) {
          return false;
        }
        final Node<K, V> node = m.getNode(key);
        return node != null && eq(node.value, entry.getValue());
      }

      @SuppressWarnings("unchecked")
      @Override
      public final boolean remove(final Object o) {
        if (!(o instanceof Map.Entry)) {
          return false;
        }
        final Map.Entry<K, V> entry = (Map.Entry<K, V>) o;
        final K key = entry.getKey();
        if (!inRange(key)) {
          return false;
        }
        final Node<K, V> node = m.getNode(key);
        if (node != null && eq(node.value, entry.getValue())) {
          m.deleteNode(node);
          return true;
        }
        return false;
      }
    }


    /** TrieSubMapValues value collection sub map view */
    protected abstract class TrieSubMapValues extends AbstractCollection<V> {

      private transient long size = -1L;
      private transient int sizeModCount = -1;

      @Override
      public final int size() {
        if (fromStart && toEnd) {
          return m.size();
        }
        if (size == -1L || sizeModCount != m.modCount) {
          sizeModCount = m.modCount;
          size = 0L;
          final Iterator<V> i = iterator();
          while (i.hasNext()) {
            ++size;
            i.next();
          }
        }
        return size > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) size;
      }

      @Override
      public final boolean isEmpty() {
        return !iterator().hasNext();
      }

      protected abstract Iterator<Node<K, V>> getSubMapNodeIterator();

      @Override
      public final boolean remove(final Object o) {
        Node<K, V> node = null;
        final Iterator<Node<K, V>> iter = getSubMapNodeIterator();
        while (iter.hasNext()) {
          node = iter.next();
          if (eq(node.value, o)) {
            iter.remove();
            return true;
          }
        }
        return false;
      }
    }



    /** Iterator for returning entries (key-value pairs) in ascending order from a sub map */
    protected final class SubMapEntryIterator extends SubMapIterator<Map.Entry<K, V>> {

      protected SubMapEntryIterator(final Node<K, V> first, final Node<K, V> fence) {
        super(first, fence);
      }

      @Override
      public final Map.Entry<K, V> next() {
        return exportEntry(nextNode(), NavigableTrieSubMap.this.m);
      }
    }

    /** Iterator for returning only keys in ascending order from a sub map */
    protected final class SubMapKeyIterator extends SubMapIterator<K> {

      protected SubMapKeyIterator(final Node<K, V> first, final Node<K, V> fence) {
        super(first, fence);
      }

      @Override
      public final K next() {
        return exportEntry(nextNode(), NavigableTrieSubMap.this.m).getKey();
      }
    }

    /** Iterator for returning only values in ascending order from a sub map */
    protected final class SubMapValueIterator extends SubMapIterator<V> {

      protected SubMapValueIterator(final Node<K, V> first, final Node<K, V> fence) {
        super(first, fence);
      }

      @Override
      public final V next() {
        return nextNode().value;
      }
    }

    /** Iterator for returning nodes in ascending order from a sub map */
    protected final class SubMapNodeIterator extends SubMapIterator<Node<K, V>> {

      protected SubMapNodeIterator(final Node<K, V> first, final Node<K, V> fence) {
        super(first, fence);
      }

      @Override
      public final Node<K, V> next() {
        return nextNode();
      }
    }

    /** Iterator for returning entries (key-value pairs) in descending order from a sub map */
    protected final class DescendingSubMapEntryIterator extends SubMapIterator<Map.Entry<K, V>> {

      protected DescendingSubMapEntryIterator(final Node<K, V> last, final Node<K, V> fence) {
        super(last, fence);
      }

      @Override
      public final Map.Entry<K, V> next() {
        return exportEntry(prevNode(), NavigableTrieSubMap.this.m);
      }
    }

    /** Iterator for returning only keys in descending order from a sub map */
    protected final class DescendingSubMapKeyIterator extends SubMapIterator<K> {

      protected DescendingSubMapKeyIterator(final Node<K, V> last, final Node<K, V> fence) {
        super(last, fence);
      }

      @Override
      public final K next() {
        return exportEntry(prevNode(), NavigableTrieSubMap.this.m).getKey();
      }
    }

    /** Iterator for returning only values in descending order from a sub map */
    protected final class DescendingSubMapValueIterator extends SubMapIterator<V> {

      protected DescendingSubMapValueIterator(final Node<K, V> last, final Node<K, V> fence) {
        super(last, fence);
      }

      @Override
      public final V next() {
        return prevNode().value;
      }
    }

    /** Iterator for returning nodes in descending order from a sub map */
    protected final class DescendingSubMapNodeIterator extends SubMapIterator<Node<K, V>> {

      protected DescendingSubMapNodeIterator(final Node<K, V> last, final Node<K, V> fence) {
        super(last, fence);
      }

      @Override
      public final Node<K, V> next() {
        return prevNode();
      }
    }


    /** SubMapIterator for extending by Iterators for SubMaps */
    protected abstract class SubMapIterator<T> implements Iterator<T> {

      protected Node<K, V> lastReturned;
      protected Node<K, V> next;
      protected final Object fenceKey;
      protected int expectedModCount;

      /**
       * Create a new SubMapIterator sub map iterator
       *
       * @param first the first Node to start with
       * @param fence the Node we must stop before
       */
      protected SubMapIterator(final Node<K, V> first, final Node<K, V> fence) {
        expectedModCount = m.modCount;
        lastReturned = null;
        next = first;
        fenceKey = resolveKey(fence, NavigableTrieSubMap.this.m);
      }

      @Override
      public final boolean hasNext() {
        return next != null && (fenceKey == null || !fenceKey.equals(resolveKey(next, m)));
      }

      /**
       * @return the successor Node (ascending order) or null
       */
      protected final Node<K, V> nextNode() {
        final Node<K, V> e = next;
        if (e == null || (fenceKey != null && fenceKey.equals(resolveKey(e, m)))) {
          throw new NoSuchElementException();
        }
        if (m.modCount != expectedModCount) {
          throw new ConcurrentModificationException();
        }
        next = successor(e);
        lastReturned = e;
        return e;
      }

      /**
       * @return the predecessor Node (descending order) or null
       */
      protected final Node<K, V> prevNode() {
        final Node<K, V> e = next;
        if (e == null || (fenceKey != null && fenceKey.equals(resolveKey(e, m)))) {
          throw new NoSuchElementException();
        }
        if (m.modCount != expectedModCount) {
          throw new ConcurrentModificationException();
        }
        next = predecessor(e);
        lastReturned = e;
        return e;
      }

      @Override
      public final void remove() {
        if (lastReturned == null) {
          throw new IllegalStateException();
        }
        if (m.modCount != expectedModCount) {
          throw new ConcurrentModificationException();
        }
        m.deleteNode(lastReturned);
        lastReturned = null;
        expectedModCount = m.modCount;
      }
    }
  }


}
