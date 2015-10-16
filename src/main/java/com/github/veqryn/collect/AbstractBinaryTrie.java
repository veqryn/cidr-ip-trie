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
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.NavigableSet;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.SortedSet;

/**
 * AbstractBinaryTrie class
 *
 * @author Mark Christopher Duncan
 *
 * @param <K> Key
 * @param <V> Value
 */
public class AbstractBinaryTrie<K, V> implements Trie<K, V>, NavigableTrie<K, V>,
    NavigableMap<K, V>, Map<K, V>, Serializable, Cloneable {
  // maybe implement guava Multimap or SortedSetMultimap
  // maybe implement apache commons collection interfaces?
  // TODO: in new interface, implement more 'value' based methods

  private static final long serialVersionUID = 4494549156276631388L;

  protected final KeyCodec<K> codec;

  protected final Node<K, V> root = new Node<K, V>(null);

  protected long size = 0;

  protected transient volatile int modCount = 0;

  protected transient volatile TrieEntrySet entrySet = null;
  protected transient volatile NavigableSet<K> keySet = null;
  protected transient volatile Collection<V> values = null;
  protected transient volatile NavigableTrie<K, V> descendingMap = null;


  public AbstractBinaryTrie(final KeyCodec<K> keyCodec) {
    if (keyCodec == null) {
      throw new NullPointerException("KeyCodec may not be null");
    }
    this.codec = keyCodec;
  }

  public AbstractBinaryTrie(final KeyCodec<K> keyCodec, final Map<K, V> otherMap) {
    if (keyCodec == null) {
      throw new NullPointerException("KeyCodec may not be null");
    }
    this.codec = keyCodec;
    this.putAll(otherMap);
  }

  public AbstractBinaryTrie(final AbstractBinaryTrie<K, V> otherTrie) {
    this.codec = otherTrie.codec;
    this.buildFromExisting(otherTrie);
  }



  protected static final class Node<K, V> implements Serializable {
    // Does not implement Map.Entry so that we do not accidentally
    // return a Node instance from a public method

    private static final long serialVersionUID = -5827641765558398662L;

    /**
     * Do not directly reference <code>privateKey</code>.
     * Instead use <code>resolveKey(trie, node)</code>
     */
    protected transient volatile K privateKey = null;
    protected V value = null;
    protected Node<K, V> left = null;
    protected Node<K, V> right = null;
    protected final Node<K, V> parent;

    protected Node(final Node<K, V> parent) {
      this.parent = parent;
    }

    protected final Node<K, V> getOrCreateEmpty(final boolean leftNode) {
      if (leftNode) {
        if (left == null) {
          left = new Node<K, V>(this);
        }
        return left;
      } else {
        if (right == null) {
          right = new Node<K, V>(this);
        }
        return right;
      }
    }

    /**
     * @return true if this Entry node has no value and no child nodes
     */
    protected final boolean isEmpty() {
      return value == null && left == null && right == null;
    }

    /**
     * @return the value associated with the key
     */
    protected final V getValue() {
      return value;
    }

    /**
     * Replaces the value currently associated with the key with the given value.
     *
     * @return the value associated with the key before this method was called
     */
    protected final V setValue(final V value) {
      final V oldValue = this.value;
      this.value = value;
      return oldValue;
    }

    /**
     * Do not directly reference <code>getPrivateKeyOrNull</code>.
     * Instead use <code>resolveKey(trie, node)</code>
     *
     * @return the key (K) if it has been resolved, or null otherwise
     */
    protected final K getPrivateKeyOrNull() {
      return privateKey;
    }

    protected final CodecElements getCodecElements() {

      if (this.parent == null) {
        return null; // We are the root node
      }

      final BitSet bits = new BitSet();
      int levelsDeep = 0;
      Node<K, V> previousParent = this;
      Node<K, V> currentParent = this.parent;
      while (currentParent != null) {
        if (currentParent.right == previousParent) {
          bits.set(levelsDeep);
        }
        previousParent = currentParent;
        currentParent = currentParent.parent;
        levelsDeep++;
      }

      return new CodecElements(bits, levelsDeep);
    }

    @Override
    public final int hashCode() {
      throw new IllegalStateException("Nodes should not be hashed or compared for equality");
    }

    @Override
    public final boolean equals(final Object obj) {
      throw new IllegalStateException("Nodes should not be hashed or compared for equality");
    }

    @Override
    public final String toString() {
      return (getPrivateKeyOrNull() != null ? getPrivateKeyOrNull() : getCodecElements())
          + "=" + value;
    }

  }

  protected static final class CodecElements implements Serializable {

    private static final long serialVersionUID = -3206679175141036878L;

    protected final BitSet bits;
    protected final int levelsDeep;

    protected CodecElements(final BitSet bits, final int levelsDeep) {
      this.bits = bits;
      this.levelsDeep = levelsDeep;
    }

    public final BitSet getBits() {
      return bits;
    }

    public final int getLevelsDeep() {
      return levelsDeep;
    }

    @Override
    public final int hashCode() {
      throw new IllegalStateException(
          "CodecElements should not be hashed or compared for equality");
    }

    @Override
    public final boolean equals(final Object obj) {
      throw new IllegalStateException(
          "CodecElements should not be hashed or compared for equality");
    }

    @Override
    public final String toString() {
      return levelsDeep + "/" + bits;
    }

  }

  protected static final <K, V> K resolveKey(final Node<K, V> node,
      final AbstractBinaryTrie<K, V> trie) {
    final Node<K, V> resolved = resolveNode(node, trie);
    return resolved == null ? null : resolved.getPrivateKeyOrNull();
  }

  protected static final <K, V> Node<K, V> resolveNode(final Node<K, V> node,
      final AbstractBinaryTrie<K, V> trie) {

    System.out.println(); // TODO: remove this when done testing

    if (node == null || node.parent == null) {
      return null; // If no parents, then it is the root node
    }

    // key has already been resolved, or we shouldn't have a key because we don't have a value
    if (node.privateKey != null || node.value == null) {
      return node;
    }

    final CodecElements elements = node.getCodecElements();

    final K key = trie.codec.recreateKey(elements.bits, elements.levelsDeep);

    if (key == null) {
      throw new IllegalStateException("Unable to create non-null key with key-codec");
    }
    if (getNode(key, trie.root, trie.codec, true) != node) {
      throw new IllegalStateException("Created key not equal to our original key");
    }

    node.privateKey = key;

    System.out.println("Resolving Node's Key: " + key); // TODO: remove this when done testing

    return node;

  }



  /**
   * Test two values for equality. Differs from o1.equals(o2) only in
   * that it copes with {@code null} o1 properly.
   */
  protected static final boolean eq(final Object o1, final Object o2) {
    return (o1 == null ? o2 == null : (o1 == o2 || o1.equals(o2)));
  }

  /**
   * Return SimpleImmutableEntry for entry, or null if null
   */
  protected static final <K, V> Map.Entry<K, V> exportEntry(final Node<K, V> entry,
      final AbstractBinaryTrie<K, V> trie) {
    // Resolve the Key if missing
    if (entry == null || entry.getValue() == null) {
      return null;
    }
    final Node<K, V> resolved = resolveNode(entry, trie);
    return new AbstractMap.SimpleImmutableEntry<>(resolved.getPrivateKeyOrNull(),
        resolved.getValue());
  }

  /**
   * Returns the key corresponding to the specified Entry.
   *
   * @throws NoSuchElementException if the Entry is null
   */
  protected static final <K, V> K keyOrNoSuchElementException(final Node<K, V> entry,
      final AbstractBinaryTrie<K, V> trie) {
    if (entry == null || entry.getValue() == null) {
      throw new NoSuchElementException();
    }
    // Resolve the Key if missing
    return resolveKey(entry, trie);
  }

  /**
   * Returns the key corresponding to the specified Entry,
   * or null if it does not exist
   */
  protected static final <K, V> K keyOrNull(final Node<K, V> entry,
      final AbstractBinaryTrie<K, V> trie) {
    if (entry == null || entry.getValue() == null) {
      return null;
    }
    // Resolve the Key if missing
    return resolveKey(entry, trie);
  }



  protected void clearTransientMemory() {
    entrySet = null;
    keySet = null;
    values = null;
    descendingMap = null;
    // clear keys from Nodes
    Node<K, V> subTree = root;
    while ((subTree = successor(subTree)) != null) {
      subTree.privateKey = null;
    }
  }


  @Override
  public void clear() {
    this.root.value = null;
    this.root.left = null;
    this.root.right = null;
    this.size = 0L;
    ++this.modCount;
  }


  @Override
  public boolean isEmpty() {
    return root.isEmpty();
  }


  @Override
  public int size() {
    return size > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) size;
  }

  protected int size(final Node<K, V> parentFence, final boolean countEmptyNodes) {
    long total = 0L;
    Node<K, V> subTree = root;
    while ((subTree = successor(subTree, parentFence, countEmptyNodes)) != null) {
      ++total;
    }
    return total > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) total;
  }



  public KeyCodec<K> getCodec() {
    return codec;
  }


  @Override
  public final Comparator<? super K> comparator() {
    return codec.comparator();
  }

  /**
   * Compares two keys using the correct comparison method for this class.
   */
  @SuppressWarnings("unchecked")
  protected final int compare(final K k1, final K k2) {
    return codec.comparator() == null ? ((Comparable<? super K>) k1).compareTo(k2)
        : codec.comparator().compare(k1, k2);
  }



  /**
   * Returns a shallow copy of this {@code AbstractBinaryTrie} instance.
   * (The keys and values themselves are not cloned.)
   *
   * @return a shallow copy of this trie/map
   */
  @Override
  public AbstractBinaryTrie<K, V> clone() {
    return new AbstractBinaryTrie<K, V>(this);
  }

  protected void buildFromExisting(final AbstractBinaryTrie<K, V> otherTrie) {

    Node<K, V> myNode = this.root;
    Node<K, V> otherNode = otherTrie.root;

    // Pre-Order tree traversal
    outer: while (otherNode != null) {

      if (otherNode.left != null) {
        otherNode = otherNode.left;
        myNode = myNode.getOrCreateEmpty(true);
        myNode.value = otherNode.value;
        // myNode.key = otherNode.key;
        continue;
      }

      if (otherNode.right != null) {
        otherNode = otherNode.right;
        myNode = myNode.getOrCreateEmpty(false);
        myNode.value = otherNode.value;
        // myNode.key = otherNode.key;
        continue;
      }

      // We are a leaf node
      while (otherNode.parent != null) {

        if (otherNode == otherNode.parent.left && otherNode.parent.right != null) {
          otherNode = otherNode.parent.right;
          myNode = myNode.parent.getOrCreateEmpty(false);
          myNode.value = otherNode.value;
          // myNode.key = otherNode.key;
          continue outer;
        }
        otherNode = otherNode.parent;
        myNode = myNode.parent;
      }
      break;

    }

    this.size = otherTrie.size;
    ++this.modCount;

  }



  @Override
  public V put(final K key, final V value)
      throws ClassCastException, NullPointerException, IllegalArgumentException {

    if (key == null) {
      throw new NullPointerException(getClass().getName()
          + " does not accept null keys: " + key);
    }
    if (value == null) {
      throw new NullPointerException(getClass().getName()
          + " does not accept null values: " + value);
    }

    final int stopDepth = codec.length(key);

    if (stopDepth <= 0) {
      throw new IllegalArgumentException(getClass().getName()
          + " does not accept keys of length <= 0: " + key);
    }

    Node<K, V> subNode = root;
    int i = 0;
    while (true) {
      subNode = subNode.getOrCreateEmpty(codec.isLeft(key, i++));
      if (i == stopDepth) {
        if (subNode.value == null) {
          ++this.size;
        }
        ++this.modCount;
        // subNode.privateKey = key;
        return subNode.value = value;
      }
    }
  }

  @Override
  public void putAll(final Map<? extends K, ? extends V> map)
      throws ClassCastException, NullPointerException, IllegalArgumentException {
    for (final Map.Entry<? extends K, ? extends V> entry : map.entrySet()) {
      put(entry.getKey(), entry.getValue());
    }
  }



  @Override
  public V remove(final Object key) throws ClassCastException, NullPointerException {
    if (key == null) {
      throw new NullPointerException(getClass().getName()
          + " does not accept null keys: " + key);
    }
    @SuppressWarnings("unchecked")
    final Node<K, V> p = getNode((K) key);
    if (p == null) {
      return null;
    }

    final V oldValue = p.value;
    deleteNode(p);
    return oldValue;
  }

  protected void deleteNode(Node<K, V> node) {
    if (node == null || node.value == null) {
      return;
    }

    --this.size;
    ++modCount;
    node.value = null;
    node.privateKey = null;

    while (node.isEmpty() && node.parent != null) {
      if (node.parent.left == node) {
        node.parent.left = null;
      } else {
        node.parent.right = null;
      }
      node = node.parent;
    }
  }



  @SuppressWarnings("unchecked")
  @Override
  public boolean containsKey(final Object key) throws ClassCastException, NullPointerException {
    if (key == null) {
      throw new NullPointerException(getClass().getName()
          + " does not allow null keys: " + key);
    }
    return getNode((K) key) != null;
  }

  @SuppressWarnings("unchecked")
  @Override
  public V get(final Object key) throws ClassCastException, NullPointerException {
    if (key == null) {
      throw new NullPointerException(getClass().getName()
          + " does not accept null keys: " + key);
    }
    final Node<K, V> node = getNode((K) key);
    return node == null ? null : node.value;
  }

  protected Node<K, V> getNode(final K key) {
    return getNode(key, root, codec, false);
  }

  protected static <K, V> Node<K, V> getNode(final K key, final Node<K, V> startingNode,
      final KeyCodec<K> codec, final boolean canBeEmpty) {
    // While we could technically combine getNode and getNodes, the speed and
    // simplicity of getNode outweighs forcing exact match get's to use getNodes

    if (key == null) {
      return null;
    }

    final int stopDepth = codec.length(key);

    if (stopDepth <= 0) {
      throw new IllegalArgumentException(AbstractBinaryTrie.class.getClass().getName()
          + " does not accept keys of length <= 0: " + key);
    }

    // Look up a single record
    Node<K, V> subNode = startingNode;
    int i = 0;
    while (true) {
      if (codec.isLeft(key, i++)) {
        subNode = subNode.left;
      } else {
        subNode = subNode.right;
      }

      if (subNode == null) {
        return null;
      }
      if (i == stopDepth && (subNode.value != null || canBeEmpty)) {
        return subNode;
      }
      if (i >= stopDepth) {
        return null;
      }
    }
  }



  protected static <K, V> List<Node<K, V>> getNodes(final K key, Node<K, V> node,
      final KeyCodec<K> codec, final boolean includePrefixOfKey, final boolean keyInclusive,
      final boolean includePrefixedByKey, final boolean canBeEmpty) {

    final List<Node<K, V>> nodes = new ArrayList<>();

    if (!includePrefixOfKey && !keyInclusive && !includePrefixedByKey) {
      throw new IllegalArgumentException("Not including any of suffixes, the key, or prefixes");
    }

    if (key == null) {
      return nodes;
    }

    final int stopDepth = codec.length(key);

    if (stopDepth <= 0) {
      throw new IllegalArgumentException(AbstractBinaryTrie.class.getClass().getName()
          + " does not accept keys of length <= 0: " + key);
    }

    Node<K, V> limit = null;
    int i = 0;
    while (node != null) {

      if (i > stopDepth) {
        // Traverse all nodes under the Key (limit)
        node = successor(node, limit, canBeEmpty);

      } else {
        // Traverse only the path that matches our Key
        if (codec.isLeft(key, i++)) {
          node = node.left;
        } else {
          node = node.right;
        }
        // Force any subsequent tree traversal to be under this node (the Key)
        if (i == stopDepth) {
          limit = node;
        }
      }

      if (node != null) {
        // If conditions match, add the nodes
        if (node.value != null || canBeEmpty) {

          if (i < stopDepth && includePrefixOfKey) {
            nodes.add(node);
          }

          if (i == stopDepth && keyInclusive) {
            nodes.add(node);
          }

          if (i > stopDepth && includePrefixedByKey) {
            nodes.add(node);
          }

        }

        // Exit early if all further conditions are false
        if ((i + 1 == stopDepth && (!keyInclusive && !includePrefixedByKey))
            || (i >= stopDepth && !includePrefixedByKey)) {
          return nodes;
        }
      }
    }

    return nodes;

  }


  // TODO: turn these into View's backed by the trie map
  @Override
  public Collection<V> valuesPrefixOf(final K key, final boolean keyInclusive) {
    final Collection<V> values = new ArrayList<>();
    for (final Node<K, V> node : getNodes(key, root, codec, true, keyInclusive, false, false)) {
      values.add(node.value);
    }
    return values;
  }

  @Override
  public Collection<V> valuesPrefixedBy(final K key, final boolean keyInclusive) {
    final Collection<V> values = new ArrayList<>();
    for (final Node<K, V> node : getNodes(key, root, codec, false, keyInclusive, true, false)) {
      values.add(node.value);
    }
    return values;
  }

  @Override
  public Collection<V> valuesPrefixOfOrBy(final K key) {
    final Collection<V> values = new ArrayList<>();
    for (final Node<K, V> node : getNodes(key, root, codec, true, true, true, false)) {
      values.add(node.value);
    }
    return values;
  }


  // TODO: Can be made more efficient, no need to get all nodes, just the first/last
  @Override
  public V valueShortestPrefixOf(final K key, final boolean keyInclusive) {
    final List<Node<K, V>> nodes = getNodes(key, root, codec, true, keyInclusive, false, false);
    return nodes.isEmpty() ? null : nodes.get(0).value;
  }

  @Override
  public V valueShortestPrefixedBy(final K key, final boolean keyInclusive) {
    final List<Node<K, V>> nodes = getNodes(key, root, codec, false, keyInclusive, true, false);
    return nodes.isEmpty() ? null : nodes.get(0).value;
  }

  @Override
  public V valueLongestPrefixOf(final K key, final boolean keyInclusive) {
    final List<Node<K, V>> nodes = getNodes(key, root, codec, true, keyInclusive, false, false);
    return nodes.isEmpty() ? null : nodes.get(nodes.size() - 1).value;
  }

  @Override
  public V valueLongestPrefixedBy(final K key, final boolean keyInclusive) {
    final List<Node<K, V>> nodes = getNodes(key, root, codec, false, keyInclusive, true, false);
    return nodes.isEmpty() ? null : nodes.get(nodes.size() - 1).value;
  }



  protected Node<K, V> ceilingNode(final K key) {
    return ceilingOrHigherNode(key, false);
  }

  protected Node<K, V> higherNode(final K key) {
    return ceilingOrHigherNode(key, true);
  }

  protected Node<K, V> ceilingOrHigherNode(final K key, final boolean higher) {

    if (key == null) {
      return null;
    }

    final int stopDepth = codec.length(key);

    // Look up a single record
    boolean left;
    Node<K, V> predecessor;
    Node<K, V> subNode = root;
    int i = 1;
    while (true) {
      predecessor = subNode;
      if (left = codec.isLeft(key, i++)) {
        subNode = subNode.left;
      } else {
        subNode = subNode.right;
      }

      if (subNode == null) {
        if (i == 0 && !left) {
          // Special case: the first element is right, and we have nothing to the right of root
          // therefore, there is nothing equal to or larger than this key, so return null
          return null;
        }
        return successor(predecessor);
      }
      if (subNode.value != null && i == stopDepth) {
        return higher ? successor(subNode) : subNode;
      }
      if (i >= stopDepth) {
        return successor(predecessor);
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

  protected Node<K, V> floorNode(final K key) {
    return floorOrLowerNode(key, false);
  }

  protected Node<K, V> lowerNode(final K key) {
    return floorOrLowerNode(key, true);
  }

  protected Node<K, V> floorOrLowerNode(final K key, final boolean lower) {

    if (key == null) {
      return null;
    }

    final int stopDepth = codec.length(key);

    // Look up a single record
    boolean left;
    Node<K, V> predecessor;
    Node<K, V> subNode = root;
    int i = 1;
    while (true) {
      predecessor = subNode;
      if (left = codec.isLeft(key, i++)) {
        subNode = subNode.left;
      } else {
        subNode = subNode.right;
      }

      if (subNode == null) {
        if (i == 0) {
          if (left) {
            // Special case: the first element is left, and we have nothing to the left of root
            // therefore, there is nothing equal to or less than this key, so return null
            return null;
          } else {
            // First element is right, and we have nothing to the right of root
            // therefore, return the very last node
            return lastNode();
          }
        }
        return predecessor.value != null ? predecessor : predecessor(predecessor);
      }
      if (subNode.value != null && i == stopDepth) {
        return lower ? predecessor(subNode) : subNode;
      }
      if (i >= stopDepth) {
        return predecessor.value != null ? predecessor : predecessor(predecessor);
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



  protected Node<K, V> firstNode() {
    return successor(root);
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


  /**
   * Returns the successor of the specified Node Entry, or null if no such.
   */
  protected static <K, V> Node<K, V> successor(final Node<K, V> node) {
    return successor(node, null, false);
  }

  protected static <K, V> Node<K, V> successor(Node<K, V> node, final Node<K, V> parentFence,
      final boolean canBeEmpty) {

    // The fact that nodes do not always have values complicates an otherwise simple
    // Pre-Order parent linkage (stackless, flag-less) tree traversal

    // We can include the parentFence, but we can not go above it
    final Node<K, V> limit = parentFence == null ? null : parentFence.parent;

    outer: while (node != null) {

      if (node.left != null) {
        if (node.left.value == null && !canBeEmpty) {
          node = node.left;
          continue;
        }
        return node.left;
      }

      if (node.right != null) {
        if (node.right.value == null && !canBeEmpty) {
          node = node.right;
          continue;
        }
        return node.right;
      }

      // We are a leaf node
      while (node.parent != null && node.parent != limit) {

        if (node == node.parent.left && node.parent.right != null) {

          if (node.parent.right.value == null && !canBeEmpty) {
            node = node.parent.right;
            continue outer;
          }
          return node.parent.right;
        }
        node = node.parent;
      }
      return null;

    }
    return null;
  }



  protected Node<K, V> lastNode() {
    // Rely on the fact that leaf nodes can not be empty
    Node<K, V> parent = root;
    while (parent.right != null || parent.left != null) {
      if (parent.right != null) {
        parent = parent.right;
      } else {
        parent = parent.left;
      }
    }
    return parent == root ? null : parent;
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


  /**
   * Returns the predecessor of the specified Node Entry, or null if no such.
   */
  protected static <K, V> Node<K, V> predecessor(final Node<K, V> node) {
    return predecessor(node, null, false);
  }

  protected static <K, V> Node<K, V> predecessor(Node<K, V> node, final Node<K, V> parentFence,
      final boolean canBeEmpty) {

    // The fact that nodes do not always have values complicates an otherwise simple
    // Reverse Post-Order parent linkage (stackless, flag-less) tree traversal

    final Node<K, V> limit = parentFence == null ? null : parentFence.parent;

    while (node != null && node.parent != null && node.parent != limit) {

      // we are on the left, or we have no left sibling, so go up
      if (node == node.parent.left || node.parent.left == null) {

        if (node.parent.value == null && !canBeEmpty) {
          node = node.parent;
          continue;
        }
        return node.parent;
      }

      // we are on the right and have a left sibling
      // so explore the left sibling, all the way down to the right-most child
      node = node.parent.left;
      while (node.right != null || node.left != null) {
        if (node.right != null) {
          node = node.right;
        } else {
          node = node.left;
        }
      }

      if (node.value == null) {
        throw new IllegalStateException("Should not have a leaf node with no value");
      }
      return node;

    }
    return null;
  }



  @Override
  public boolean containsValue(final Object value) throws ClassCastException, NullPointerException {
    if (value == null) {
      throw new NullPointerException(getClass().getName()
          + " does not allow null values: " + value);
    }
    for (Node<K, V> e = firstNode(); e != null; e = successor(e)) {
      if (eq(value, e.value)) {
        return true;
      }
    }
    return false;
  }



  @Override
  public int hashCode() {
    // To stay compatible with Map interface, we are equal to any map with the same mappings
    int h = 0;
    final Iterator<Entry<K, V>> i = entrySet().iterator();
    while (i.hasNext()) {
      h += i.next().hashCode();
    }
    return h;
  }

  @Override
  public boolean equals(final Object o) {

    if (o == this) {
      return true;
    }

    if (!(o instanceof Map)) {
      return false;
    }
    final Map<?, ?> m = (Map<?, ?>) o;
    if (m.size() != size()) {
      return false;
    }

    if (m instanceof AbstractBinaryTrie) {
      // We are comparing against another AbstractBinaryTrie, so we can take shortcuts
      final AbstractBinaryTrie<?, ?> t = (AbstractBinaryTrie<?, ?>) m;
      return compareAllNodes(this.root, t.root);

    } else {
      // To stay compatible with Map interface, we are equal to any map with the same mappings
      try {
        final Iterator<Entry<K, V>> i = entrySet().iterator();
        while (i.hasNext()) {
          final Entry<K, V> e = i.next();
          final K key = e.getKey();
          final V value = e.getValue();
          if (value == null) {
            if (!(m.get(key) == null && m.containsKey(key))) {
              return false;
            }
          } else {
            if (!value.equals(m.get(key))) {
              return false;
            }
          }
        }
      } catch (final ClassCastException unused) {
        return false;
      } catch (final NullPointerException unused) {
        return false;
      }

      return true;
    }

  }

  protected static final boolean compareNodeAndExistenceOfChildren(
      final AbstractBinaryTrie.Node<?, ?> myNode,
      final AbstractBinaryTrie.Node<?, ?> otherNode) {

    if ((myNode.left == null && otherNode.left != null)
        || (myNode.left != null && otherNode.left == null)) {
      return false;
    }

    if ((myNode.right == null && otherNode.right != null)
        || (myNode.right != null && otherNode.right == null)) {
      return false;
    }

    if (!eq(myNode.value, otherNode.value)) {
      return false;
    }

    return true;

  }

  protected static final boolean compareAllNodes(Node<?, ?> myNode, Node<?, ?> otherNode) {

    // Pre-Order tree traversal
    outer: while (otherNode != null) {

      if (otherNode.left != null) {
        otherNode = otherNode.left;
        myNode = myNode.left;
        if (!compareNodeAndExistenceOfChildren(myNode, otherNode)) {
          return false;
        }
        continue;
      }

      if (otherNode.right != null) {
        otherNode = otherNode.right;
        myNode = myNode.right;
        if (!compareNodeAndExistenceOfChildren(myNode, otherNode)) {
          return false;
        }
        continue;
      }

      // We are a leaf node
      while (otherNode.parent != null) {

        if (otherNode == otherNode.parent.left && otherNode.parent.right != null) {
          otherNode = otherNode.parent.right;
          myNode = myNode.parent.right;
          if (!compareNodeAndExistenceOfChildren(myNode, otherNode)) {
            return false;
          }
          continue outer;
        }
        otherNode = otherNode.parent;
        myNode = myNode.parent;
      }
      break;

    }

    return true;

  }



  @Override
  public String toString() {
    // maybe create a ascii diagram for the tree structure?
    final Iterator<Entry<K, V>> i = entrySet().iterator();
    if (!i.hasNext()) {
      return "{}";
    }

    final StringBuilder sb = new StringBuilder();
    sb.append('{');
    for (;;) {
      final Entry<K, V> e = i.next();
      final K key = e.getKey();
      final V value = e.getValue();
      sb.append(key == this ? "(this Map)" : key);
      sb.append('=');
      sb.append(value == this ? "(this Map)" : value);
      if (!i.hasNext()) {
        return sb.append('}').toString();
      }
      sb.append(',').append(' ');
    }
  }



  @Override
  public final NavigableSet<K> keySet() {
    return navigableKeySet();
  }

  @Override
  public NavigableSet<K> navigableKeySet() {
    if (keySet == null) {
      keySet = new TrieKeySet<K>(this);
    }
    return keySet;
  }

  protected static final class TrieKeySet<E> extends AbstractSet<E>
      implements NavigableSet<E> {

    protected final NavigableTrie<E, ? extends Object> m;

    protected TrieKeySet(final NavigableTrie<E, ? extends Object> map) {
      m = map;
    }

    @Override
    public final Iterator<E> iterator() {
      if (m instanceof AbstractBinaryTrie) {
        return ((AbstractBinaryTrie<E, ? extends Object>) m).keyIterator();
      } else {
        return (((AbstractBinaryTrie.NavigableTrieSubMap<E, ? extends Object>) m).keyIterator());
      }
    }

    @Override
    public final Iterator<E> descendingIterator() {
      if (m instanceof AbstractBinaryTrie) {
        return ((AbstractBinaryTrie<E, ? extends Object>) m).descendingKeyIterator();
      } else {
        return (((AbstractBinaryTrie.NavigableTrieSubMap<E, ? extends Object>) m)
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
      final int oldSize = size();
      m.remove(o);
      return size() != oldSize;
    }

    @Override
    public final NavigableSet<E> subSet(final E fromElement, final boolean fromInclusive,
        final E toElement, final boolean toInclusive) {
      return new TrieKeySet<>(m.subMap(fromElement, fromInclusive,
          toElement, toInclusive));
    }

    @Override
    public final NavigableSet<E> headSet(final E toElement, final boolean inclusive) {
      return new TrieKeySet<>(m.headMap(toElement, inclusive));
    }

    @Override
    public final NavigableSet<E> tailSet(final E fromElement, final boolean inclusive) {
      return new TrieKeySet<>(m.tailMap(fromElement, inclusive));
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
      return new TrieKeySet<E>(m.descendingMap());
    }

  }



  @Override
  public Collection<V> values() {
    if (values == null) {
      values = new TrieValues<K, V>(this);
    }
    return values;
  }

  protected static final class TrieValues<K, V> extends AbstractCollection<V> {

    protected final AbstractBinaryTrie<K, V> m; // the backing map

    protected TrieValues(final AbstractBinaryTrie<K, V> map) {
      this.m = map;
    }

    @Override
    public final Iterator<V> iterator() {
      return new ValueIterator<K, V>(m);
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
      return m.containsValue(o);
    }

    @Override
    public final boolean remove(final Object o) {
      for (Node<K, V> e = m.firstNode(); e != null; e = successor(e)) {
        if (eq(e.getValue(), o)) {
          m.deleteNode(e);
          return true;
        }
      }
      return false;
    }

    @Override
    public final void clear() {
      m.clear();
    }
  }



  @Override
  public Set<Map.Entry<K, V>> entrySet() {
    final TrieEntrySet es = entrySet;
    return (es != null) ? es : (entrySet = new TrieEntrySet());
  }

  protected final class TrieEntrySet extends AbstractSet<Map.Entry<K, V>> {

    @Override
    public final Iterator<Map.Entry<K, V>> iterator() {
      return new EntryIterator<K, V>(AbstractBinaryTrie.this);
    }

    @Override
    public final boolean contains(final Object o) {
      if (!(o instanceof Map.Entry)) {
        return false;
      }
      @SuppressWarnings("unchecked")
      final Map.Entry<K, V> entry = (Map.Entry<K, V>) o;
      final V value = entry.getValue();
      final Node<K, V> p = getNode(entry.getKey());
      return p != null && eq(p.getValue(), value);
    }

    @Override
    public final boolean remove(final Object o) {
      if (!(o instanceof Map.Entry)) {
        return false;
      }
      @SuppressWarnings("unchecked")
      final Map.Entry<K, V> entry = (Map.Entry<K, V>) o;
      final V value = entry.getValue();
      final Node<K, V> p = getNode(entry.getKey());
      if (p != null && eq(p.getValue(), value)) {
        deleteNode(p);
        return true;
      }
      return false;
    }

    @Override
    public final int size() {
      return AbstractBinaryTrie.this.size();
    }

    @Override
    public final void clear() {
      AbstractBinaryTrie.this.clear();
    }
  }



  protected static final class EntryIterator<K, V>
      extends PrivateEntryIterator<K, V, Map.Entry<K, V>> {

    protected EntryIterator(final AbstractBinaryTrie<K, V> map) {
      super(map, map.firstNode());
    }

    @Override
    public final Map.Entry<K, V> next() {
      return exportEntry(nextNode(), m);
    }
  }

  protected static final class ValueIterator<K, V> extends PrivateEntryIterator<K, V, V> {

    protected ValueIterator(final AbstractBinaryTrie<K, V> map) {
      super(map, map.firstNode());
    }

    @Override
    public final V next() {
      return nextNode().getValue();
    }
  }

  protected final Iterator<K> keyIterator() {
    return new KeyIterator<K, V>(this);
  }

  protected static final class KeyIterator<K, V> extends PrivateEntryIterator<K, V, K> {

    protected KeyIterator(final AbstractBinaryTrie<K, V> map) {
      super(map, map.firstNode());
    }

    @Override
    public final K next() {
      return exportEntry(nextNode(), m).getKey();
    }
  }

  protected final Iterator<K> descendingKeyIterator() {
    return new DescendingKeyIterator<K, V>(this);
  }

  protected static final class DescendingKeyIterator<K, V> extends PrivateEntryIterator<K, V, K> {

    protected DescendingKeyIterator(final AbstractBinaryTrie<K, V> map) {
      super(map, map.lastNode());
    }

    @Override
    public final K next() {
      return exportEntry(prevNode(), m).getKey();
    }
  }


  protected abstract static class PrivateEntryIterator<K, V, T> implements Iterator<T> {

    protected final AbstractBinaryTrie<K, V> m; // the backing map

    protected Node<K, V> next;
    protected Node<K, V> lastReturned;
    protected int expectedModCount;

    protected PrivateEntryIterator(final AbstractBinaryTrie<K, V> map, final Node<K, V> first) {
      this.m = map;
      expectedModCount = m.modCount;
      lastReturned = null;
      next = first;
    }

    @Override
    public final boolean hasNext() {
      return next != null;
    }

    protected final Node<K, V> nextNode() {
      final Node<K, V> e = next;
      if (e == null) {
        throw new NoSuchElementException();
      }
      if (m.modCount != expectedModCount) {
        throw new ConcurrentModificationException();
      }
      next = successor(e);
      lastReturned = e;
      return e;
    }

    protected final Node<K, V> prevNode() {
      final Node<K, V> e = next;
      if (e == null) {
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
      // TODO: Do I need this (from TreeMap)? Very confused by this...
      // deleted entries are replaced by their successors
      // if (lastReturned.left != null && lastReturned.right != null) {
      // next = lastReturned;}
      m.deleteNode(lastReturned);
      expectedModCount = m.modCount;
      lastReturned = null;
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



  protected static final class AscendingSubMap<K, V> extends NavigableTrieSubMap<K, V> {

    private static final long serialVersionUID = 912986545866124060L;

    protected AscendingSubMap(final AbstractBinaryTrie<K, V> m,
        final boolean fromStart, final K lo, final boolean loInclusive,
        final boolean toEnd, final K hi, final boolean hiInclusive) {
      super(m, fromStart, lo, loInclusive, toEnd, hi, hiInclusive);
    }

    @Override
    public final Comparator<? super K> comparator() {
      return m.comparator();
    }

    @Override
    public final NavigableTrie<K, V> subMap(final K fromKey, final boolean fromInclusive,
        final K toKey, final boolean toInclusive) {

      if (!inRange(fromKey, fromInclusive)) {
        throw new IllegalArgumentException("fromKey out of range");
      }
      if (!inRange(toKey, toInclusive)) {
        throw new IllegalArgumentException("toKey out of range");
      }
      return new AscendingSubMap<K, V>(m,
          false, fromKey, fromInclusive,
          false, toKey, toInclusive);
    }

    @Override
    public final NavigableTrie<K, V> headMap(final K toKey, final boolean inclusive) {

      if (!inRange(toKey, inclusive)) {
        throw new IllegalArgumentException("toKey out of range");
      }
      return new AscendingSubMap<K, V>(m,
          fromStart, lo, loInclusive,
          false, toKey, inclusive);
    }

    @Override
    public final NavigableTrie<K, V> tailMap(final K fromKey, final boolean inclusive) {

      if (!inRange(fromKey, inclusive)) {
        throw new IllegalArgumentException("fromKey out of range");
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
        valuesSubMapView = new TrieSubMapValues(absLowest(), absHighFence());
      }
      return valuesSubMapView;
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



  protected static final class DescendingSubMap<K, V> extends NavigableTrieSubMap<K, V> {

    private static final long serialVersionUID = 912986545866120460L;

    protected DescendingSubMap(final AbstractBinaryTrie<K, V> m,
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
        throw new IllegalArgumentException("fromKey out of range");
      }
      if (!inRange(toKey, toInclusive)) {
        throw new IllegalArgumentException("toKey out of range");
      }
      return new DescendingSubMap<K, V>(m,
          false, toKey, toInclusive,
          false, fromKey, fromInclusive);
    }

    @Override
    public final NavigableTrie<K, V> headMap(final K toKey, final boolean inclusive) {

      if (!inRange(toKey, inclusive)) {
        throw new IllegalArgumentException("toKey out of range");
      }
      return new DescendingSubMap<K, V>(m,
          false, toKey, inclusive,
          toEnd, hi, hiInclusive);
    }

    @Override
    public final NavigableTrie<K, V> tailMap(final K fromKey, final boolean inclusive) {

      if (!inRange(fromKey, inclusive)) {
        throw new IllegalArgumentException("fromKey out of range");
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
        valuesSubMapView = new DescendingTrieSubMapValues(absHighest(), absLowFence());
      }
      return valuesSubMapView;
    }

    protected final class DescendingTrieSubMapValues extends TrieSubMapValues {
      protected DescendingTrieSubMapValues(final Node<K, V> last, final Node<K, V> fence) {
        super(last, fence);
      }

      @Override
      public final Iterator<V> iterator() {
        return new DescendingSubMapValueIterator(first, fence);
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



  protected abstract static class NavigableTrieSubMap<K, V> extends AbstractMap<K, V>
      implements NavigableTrie<K, V>, NavigableMap<K, V>, Serializable {

    private static final long serialVersionUID = 4159238497306996386L;

    /** The backing map. */
    protected final AbstractBinaryTrie<K, V> m;

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
    protected transient volatile NavigableTrie<K, V> descendingSubMapView = null;
    protected transient volatile TrieEntrySetSubMapView entrySetSubMapView = null;
    protected transient volatile TrieKeySet<K> navigableKeySetSubMapView = null;
    protected transient volatile Collection<V> valuesSubMapView = null;


    protected NavigableTrieSubMap(final AbstractBinaryTrie<K, V> m,
        final boolean fromStart, final K lo, final boolean loInclusive,
        final boolean toEnd, final K hi, final boolean hiInclusive) {

      if (!fromStart && !toEnd) {
        if (m.compare(lo, hi) > 0) {
          throw new IllegalArgumentException("fromKey > toKey");
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
        throw new IllegalArgumentException("key out of range");
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
      final TrieKeySet<K> nksv = navigableKeySetSubMapView;
      return (nksv != null) ? nksv : (navigableKeySetSubMapView = new TrieKeySet<K>(this));
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

    // TODO: turn into views
    @Override
    public Collection<V> valuesPrefixOf(final K key, final boolean keyInclusive) {
      if (!inRange(key, keyInclusive)) {
        throw new IllegalArgumentException("key out of range");
      }
      final Collection<V> values = new ArrayList<>();
      for (final Node<K, V> node : getNodes(key, m.root, m.codec, true, keyInclusive, false,
          false)) {
        if (inRange(resolveKey(node, m), keyInclusive)) {
          values.add(node.value);
        }
      }
      return values;
    }

    @Override
    public Collection<V> valuesPrefixedBy(final K key, final boolean keyInclusive) {
      if (!inRange(key, keyInclusive)) {
        throw new IllegalArgumentException("key out of range");
      }
      final Collection<V> values = new ArrayList<>();
      for (final Node<K, V> node : getNodes(key, m.root, m.codec, false, keyInclusive, true,
          false)) {
        if (inRange(resolveKey(node, m), keyInclusive)) {
          values.add(node.value);
        }
      }
      return values;
    }

    @Override
    public Collection<V> valuesPrefixOfOrBy(final K key) {
      if (!inRange(key, true)) {
        throw new IllegalArgumentException("key out of range");
      }
      final Collection<V> values = new ArrayList<>();
      for (final Node<K, V> node : getNodes(key, m.root, m.codec, true, true, true, false)) {
        if (inRange(resolveKey(node, m), true)) {
          values.add(node.value);
        }
      }
      return values;
    }


    // TODO: Can be made more efficient, no need to get all nodes, just the first/last
    @Override
    public V valueShortestPrefixOf(final K key, final boolean keyInclusive) {
      if (!inRange(key, keyInclusive)) {
        throw new IllegalArgumentException("key out of range");
      }
      final List<Node<K, V>> nodes =
          getNodes(key, m.root, m.codec, true, keyInclusive, false, false);
      if (nodes.isEmpty() || !inRange(resolveKey(nodes.get(0), m), keyInclusive)) {
        return null;
      }
      return nodes.get(0).value;
    }

    @Override
    public V valueShortestPrefixedBy(final K key, final boolean keyInclusive) {
      if (!inRange(key, keyInclusive)) {
        throw new IllegalArgumentException("key out of range");
      }
      final List<Node<K, V>> nodes =
          getNodes(key, m.root, m.codec, false, keyInclusive, true, false);
      if (nodes.isEmpty() || !inRange(resolveKey(nodes.get(0), m), keyInclusive)) {
        return null;
      }
      return nodes.get(0).value;
    }

    @Override
    public V valueLongestPrefixOf(final K key, final boolean keyInclusive) {
      if (!inRange(key, keyInclusive)) {
        throw new IllegalArgumentException("key out of range");
      }
      final List<Node<K, V>> nodes =
          getNodes(key, m.root, m.codec, true, keyInclusive, false, false);
      if (nodes.isEmpty() || !inRange(resolveKey(nodes.get(nodes.size() - 1), m), keyInclusive)) {
        return null;
      }
      return nodes.get(nodes.size() - 1).value;
    }

    @Override
    public V valueLongestPrefixedBy(final K key, final boolean keyInclusive) {
      if (!inRange(key, keyInclusive)) {
        throw new IllegalArgumentException("key out of range");
      }
      final List<Node<K, V>> nodes =
          getNodes(key, m.root, m.codec, false, keyInclusive, true, false);
      if (nodes.isEmpty() || !inRange(resolveKey(nodes.get(nodes.size() - 1), m), keyInclusive)) {
        return null;
      }
      return nodes.get(nodes.size() - 1).value;
    }



    // View classes

    protected abstract class TrieEntrySetSubMapView extends AbstractSet<Map.Entry<K, V>> {

      private transient int size = -1, sizeModCount = -1;

      @Override
      public final int size() {
        if (fromStart && toEnd) {
          return m.size();
        }
        if (size == -1 || sizeModCount != m.modCount) {
          sizeModCount = m.modCount;
          size = 0;
          final Iterator<Map.Entry<K, V>> i = iterator();
          while (i.hasNext()) {
            ++size;
            i.next();
          }
        }
        return size;
      }

      @Override
      public final boolean isEmpty() {
        final Node<K, V> n = absLowest();
        return n == null || tooHigh(resolveKey(n, NavigableTrieSubMap.this.m));
      }

      @Override
      public final boolean contains(final Object o) {
        if (!(o instanceof Map.Entry)) {
          return false;
        }
        @SuppressWarnings("unchecked")
        final Map.Entry<K, V> entry = (Map.Entry<K, V>) o;
        final K key = entry.getKey();
        if (!inRange(key)) {
          return false;
        }
        final Node<K, V> node = m.getNode(key);
        return node != null && eq(node.getValue(), entry.getValue());
      }

      @Override
      public final boolean remove(final Object o) {
        if (!(o instanceof Map.Entry)) {
          return false;
        }
        @SuppressWarnings("unchecked")
        final Map.Entry<K, V> entry = (Map.Entry<K, V>) o;
        final K key = entry.getKey();
        if (!inRange(key)) {
          return false;
        }
        final Node<K, V> node = m.getNode(key);
        if (node != null && eq(node.getValue(), entry.getValue())) {
          m.deleteNode(node);
          return true;
        }
        return false;
      }
    }

    protected class TrieSubMapValues extends AbstractCollection<V> {

      protected final Node<K, V> first;
      protected final Node<K, V> fence;

      protected TrieSubMapValues(final Node<K, V> first, final Node<K, V> fence) {
        this.first = first;
        this.fence = fence;
      }

      @Override
      public Iterator<V> iterator() {
        return new SubMapValueIterator(first, fence);
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
        return m.containsValue(o);
      }

      @Override
      public final boolean remove(final Object o) {
        for (Node<K, V> e = m.firstNode(); e != null; e = successor(e)) {
          if (eq(e.getValue(), o)) {
            m.deleteNode(e);
            return true;
          }
        }
        return false;
      }

      @Override
      public final void clear() {
        m.clear();
      }
    }



    protected final class SubMapEntryIterator extends SubMapIterator<Map.Entry<K, V>> {

      protected SubMapEntryIterator(final Node<K, V> first, final Node<K, V> fence) {
        super(first, fence);
      }

      @Override
      public final Map.Entry<K, V> next() {
        return exportEntry(nextNode(), NavigableTrieSubMap.this.m);
      }

      @Override
      public final void remove() {
        removeAscending();
      }
    }

    protected final class SubMapKeyIterator extends SubMapIterator<K> {

      protected SubMapKeyIterator(final Node<K, V> first, final Node<K, V> fence) {
        super(first, fence);
      }

      @Override
      public final K next() {
        return exportEntry(nextNode(), NavigableTrieSubMap.this.m).getKey();
      }

      @Override
      public final void remove() {
        removeAscending();
      }
    }

    protected final class SubMapValueIterator extends SubMapIterator<V> {

      protected SubMapValueIterator(final Node<K, V> first, final Node<K, V> fence) {
        super(first, fence);
      }

      @Override
      public final V next() {
        return nextNode().getValue();
      }

      @Override
      public void remove() {
        removeAscending();
      }
    }

    protected final class DescendingSubMapEntryIterator extends SubMapIterator<Map.Entry<K, V>> {

      protected DescendingSubMapEntryIterator(final Node<K, V> last, final Node<K, V> fence) {
        super(last, fence);
      }

      @Override
      public final Map.Entry<K, V> next() {
        return exportEntry(prevNode(), NavigableTrieSubMap.this.m);
      }

      @Override
      public final void remove() {
        removeDescending();
      }
    }

    protected final class DescendingSubMapKeyIterator extends SubMapIterator<K> {

      protected DescendingSubMapKeyIterator(final Node<K, V> last, final Node<K, V> fence) {
        super(last, fence);
      }

      @Override
      public final K next() {
        return exportEntry(prevNode(), NavigableTrieSubMap.this.m).getKey();
      }

      @Override
      public final void remove() {
        removeDescending();
      }
    }

    protected final class DescendingSubMapValueIterator extends SubMapIterator<V> {

      protected DescendingSubMapValueIterator(final Node<K, V> last, final Node<K, V> fence) {
        super(last, fence);
      }

      @Override
      public final V next() {
        return prevNode().getValue();
      }

      @Override
      public void remove() {
        removeDescending();
      }
    }


    /**
     * Iterators for SubMaps
     */
    protected abstract class SubMapIterator<T> implements Iterator<T> {

      protected Node<K, V> lastReturned;
      protected Node<K, V> next;
      protected final Object fenceKey;
      protected int expectedModCount;

      protected SubMapIterator(final Node<K, V> first, final Node<K, V> fence) {
        expectedModCount = m.modCount;
        lastReturned = null;
        next = first;
        fenceKey = resolveKey(fence, NavigableTrieSubMap.this.m);
      }

      @Override
      public final boolean hasNext() {
        return next != null && (fenceKey == null || resolveKey(next, m) != fenceKey);
      }

      protected final Node<K, V> nextNode() {
        final Node<K, V> e = next;
        if (e == null || (fenceKey != null && resolveKey(e, m) == fenceKey)) {
          throw new NoSuchElementException();
        }
        if (m.modCount != expectedModCount) {
          throw new ConcurrentModificationException();
        }
        next = successor(e);
        lastReturned = e;
        return e;
      }

      protected final Node<K, V> prevNode() {
        final Node<K, V> e = next;
        if (e == null || (fenceKey != null && resolveKey(e, m) == fenceKey)) {
          throw new NoSuchElementException();
        }
        if (m.modCount != expectedModCount) {
          throw new ConcurrentModificationException();
        }
        next = predecessor(e);
        lastReturned = e;
        return e;
      }

      protected final void removeAscending() {
        if (lastReturned == null) {
          throw new IllegalStateException();
        }
        if (m.modCount != expectedModCount) {
          throw new ConcurrentModificationException();
        }
        // TODO: Do I need this (from TreeMap)? Very confused by this...
        // Why would the next ever become the last?
        // deleted entries are replaced by their successors
        // if (lastReturned.left != null && lastReturned.right != null) {
        // next = lastReturned; }
        m.deleteNode(lastReturned);
        lastReturned = null;
        expectedModCount = m.modCount;
      }

      protected final void removeDescending() {
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
