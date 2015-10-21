/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.github.veqryn.collect;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.AbstractCollection;
import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.BitSet;
import java.util.Collection;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;


/**
 * Implementation of the {@link Trie} interface, as an
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
 * It is recommended that {@code cacheKeys} be set to true, if methods that
 * return, compare, or hash the keys, will be used. Otherwise memory usage
 * can be reduced by not keeping keys instances around.
 *
 * @author Mark Christopher Duncan
 *
 * @param <K> Key
 * @param <V> Value
 */
public class AbstractBinaryTrie<K, V> implements Trie<K, V>, Serializable, Cloneable {

  private static final long serialVersionUID = 4494549156276631388L;


  /** The {@link KeyCodec} being used to analyze keys */
  protected final KeyCodec<K> codec;

  /** If true, keys will be kept around. If false, keys will be recreated if needed */
  protected transient boolean cacheKeys; // final
  /** If true, keys will be written out on serialization, if false the nodes will be */
  protected transient boolean writeKeys; // final

  /** The entry point for the start of any lookup. Root can not hold a value. */
  protected transient Node<K, V> root = new Node<K, V>(null); // final

  protected transient long size = 0;

  protected transient int modCount = 0;

  protected transient Set<Map.Entry<K, V>> entrySet = null;
  protected transient Set<K> keySet = null;
  protected transient Collection<V> values = null;



  /**
   * Create an empty {@link AbstractBinaryTrie} using the given
   * {@link KeyCodec}, and settings for keeping/caching or not of keys after
   * insertion and writing out or not of keys during serialization.
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
  public AbstractBinaryTrie(final KeyCodec<K> keyCodec, final boolean cacheKeys,
      final boolean writeKeys) {
    if (keyCodec == null) {
      throw new NullPointerException("KeyCodec may not be null");
    }
    this.cacheKeys = cacheKeys;
    this.writeKeys = writeKeys;
    this.codec = keyCodec;
  }

  /**
   * Create a {@link AbstractBinaryTrie} using the given {@link KeyCodec},
   * and settings for keeping/caching or not of keys after insertion and
   * writing out or not of keys during serialization. The trie will be filled
   * with the keys and values in the provided map.
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
  public AbstractBinaryTrie(final KeyCodec<K> keyCodec, final Map<K, V> otherMap,
      final boolean cacheKeys, final boolean writeKeys) {
    this(keyCodec, cacheKeys, writeKeys);
    this.putAll(otherMap);
  }

  /**
   * Copy constructor, creates a shallow copy of this
   * {@link AbstractBinaryTrie} instance.
   *
   * @param otherTrie AbstractBinaryTrie
   */
  public AbstractBinaryTrie(final AbstractBinaryTrie<K, V> otherTrie) {
    this(otherTrie.codec, otherTrie.cacheKeys, otherTrie.writeKeys);
    this.buildFromExisting(otherTrie);
  }



  /**
   * @return {@link KeyCodec} used by this {@link Trie}
   */
  public KeyCodec<K> getCodec() {
    return codec;
  }



  /**
   * Internal representation of a Node Entry
   */
  protected static final class Node<K, V> implements Serializable {
    // Does not implement java.util.Map.Entry so that we do not accidentally
    // return a Node instance from a public method

    private static final long serialVersionUID = -5827641765558398662L;

    /**
     * Do not directly reference <code>privateKey</code> expecting a non-null key.
     * Instead use {@link AbstractBinaryTrie#resolveKey(Node, AbstractBinaryTrie)}
     * or {@link AbstractBinaryTrie#resolveNode(Node, AbstractBinaryTrie)} to
     * first create the key if it does not exist, and return the cached key.
     *
     * @return the key (K) if it has been resolved, or null otherwise.
     */
    private transient K privateKey = null;

    /**
     * @return the value (V) or null if this node does not have a value
     */
    protected V value = null;

    protected Node<K, V> left = null;
    protected Node<K, V> right = null;
    protected final Node<K, V> parent; // only root has null parent

    /**
     * Create a new empty Node, with the given parent
     *
     * @param parent Node
     */
    protected Node(final Node<K, V> parent) {
      this.parent = parent;
    }

    /**
     * Return the left or right child Node under this Node,
     * or create and return an empty child if it does not already exist
     *
     * @param leftNode true if this should return the left child,
     *        false if this should return the right child
     * @return left or right child node
     */
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
     * @return CodecElements instance consisting of {@code levelsDeep} int
     *         representing how far from the root this Node was found, and
     *         {@code bits} BitSet representing the elements
     */
    protected final CodecElements getCodecElements() {
      // This will ONLY ever be called if cacheKeys or writeKeys is false

      if (this.parent == null) {
        return null; // We are the root node
      }

      final BitSet bits = new BitSet();
      int levelsDeep = 0;
      Node<K, V> node = this;
      while (node.parent != null) {
        if (node.parent.right == node) {
          bits.set(levelsDeep);
        }
        node = node.parent;
        levelsDeep++;
      }

      return new CodecElements(bits, levelsDeep);
    }

    /**
     * hashCode should never be called on a Node.
     * Instead, export the node into a resolved Entry,
     * or just use the value and resolved key separately.
     */
    @Override
    public final int hashCode() {
      throw new IllegalStateException("Nodes should not be hashed or compared");
    }

    /**
     * equals should never be called on a Node.
     * Instead, export the node into a resolved Entry,
     * or just use the value and resolved key separately.
     */
    @Override
    public final boolean equals(final Object obj) {
      throw new IllegalStateException("Nodes should not be hashed or compared");
    }

    @Override
    public final String toString() {
      return (privateKey != null ? privateKey : getCodecElements()) + "=" + value;
    }
  }


  /**
   * First class object to hold the information needed by the {@link KeyCodec}
   * to recreate a key. Created because Java does not have tuples, and
   * Node must be able to return the two pieces of information we need to
   * know in order to recreate the key: {@code levelsDeep} int representing
   * how far from the root this Node was found, and {@code bits} BitSet
   * representing the elements.
   */
  protected static final class CodecElements implements Serializable {

    private static final long serialVersionUID = -3206679175141036878L;

    /** int representing how far from the root this Node was found */
    protected final BitSet bits;

    /** BitSet representing the elements */
    protected final int levelsDeep;

    /**
     *
     * @param bits
     * @param levelsDeep
     */
    protected CodecElements(final BitSet bits, final int levelsDeep) {
      this.bits = bits;
      this.levelsDeep = levelsDeep;
    }

    @Override
    public final int hashCode() {
      throw new IllegalStateException("CodecElements should not be hashed or compared");
    }

    @Override
    public final boolean equals(final Object obj) {
      throw new IllegalStateException("CodecElements should not be hashed or compared");
    }

    @Override
    public final String toString() {
      return levelsDeep + "/" + bits;
    }
  }


  /**
   * Ensure a Node's key has been resolved (it is non-null), otherwise recreate
   * it and cache it, before returning the Key.
   *
   * @param node The Node to be resolved
   * @param trie The Trie this node belongs to
   * @return non-null Key for a Node
   */
  protected static final <K, V> K resolveKey(final Node<K, V> node,
      final AbstractBinaryTrie<K, V> trie) {
    final Node<K, V> resolved = resolveNode(node, trie);
    return resolved == null ? null : resolved.privateKey;
  }

  /**
   * Ensure a Node's key has been resolved (it is non-null), otherwise recreate
   * it and cache it, before returning the Node.
   *
   * @param node The Node to be resolved
   * @param trie The Trie this node belongs to
   * @return Node with a non-null Key
   */
  protected static final <K, V> Node<K, V> resolveNode(final Node<K, V> node,
      final AbstractBinaryTrie<K, V> trie) {

    if (node == null || node.parent == null) {
      return null; // If no parents, then it is the root node
    }

    // key has already been resolved, or we shouldn't have a key because we don't have a value
    if (node.privateKey != null || node.value == null) {
      return node;
    }

    final CodecElements elements = node.getCodecElements();

    final KeyCodec<K> codec = trie.codec;
    final K key = codec.recreateKey(elements.bits, elements.levelsDeep);

    if (key == null) {
      throw new IllegalStateException("Unable to create non-null key with key-codec: " + codec);
    }
    assert getNode(key, trie.root, 0, codec, true) == node : "Created key must equal original key";

    node.privateKey = key;

    return node;
  }



  /**
   * Test two values for equality. Differs from o1.equals(o2) only in
   * that it copes with {@code null} o1 properly.
   *
   * @param o1 Object or null
   * @param o2 Object or null
   * @return true o1 equals o2
   */
  protected static final boolean eq(final Object o1, final Object o2) {
    return (o1 == null ? o2 == null : (o1 == o2 || o1.equals(o2)));
  }



  /**
   * Resolve the Node's key, then return the node as an immutable Map.Entry
   * Returns null if the node is null or the node's value is null (meaning it
   * is an empty intermediate node).
   *
   * @param node the Node to export
   * @param trie the Trie this Node is in
   * @return AbstractMap.SimpleImmutableEntry Map.Entry
   */
  protected static final <K, V> Map.Entry<K, V> exportEntry(final Node<K, V> node,
      final AbstractBinaryTrie<K, V> trie) {
    if (node == null || node.value == null) {
      return null;
    }
    // Resolve the Key if missing
    return new AbstractMap.SimpleImmutableEntry<>(resolveKey(node, trie), node.value);
  }



  /**
   * Clear out transient fields, including the keys of all nodes
   */
  protected void clearTransientMemory() {
    entrySet = null;
    keySet = null;
    values = null;
    // clear keys from Nodes
    for (Node<K, V> node = this.firstNode(); node != null; node = successor(node)) {
      node.privateKey = null;
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

  /**
   * Counts the size of the Trie under a given node
   *
   * @param parentFence starting Node, not included in the count
   * @param countEmptyNodes true if empty intermediate nodes should be counted
   * @return the number of nodes that are descendants of the parentFence node
   */
  protected int size(final Node<K, V> parentFence, final boolean countEmptyNodes) {
    long total = 0L;
    Node<K, V> subTree = parentFence;
    while ((subTree = successor(subTree, parentFence, countEmptyNodes)) != null) {
      ++total;
    }
    return total > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) total;
  }



  /**
   * Returns a shallow copy of this {@link AbstractBinaryTrie} instance.
   * (The keys and values themselves are not cloned.)
   *
   * @return a shallow copy of this trie/map
   */
  @Override
  public AbstractBinaryTrie<K, V> clone() {
    return new AbstractBinaryTrie<K, V>(this);
  }

  /**
   * Copies the node structure and node values from {@code otherTrie} onto
   * this Trie. This creates a shallow copy.
   *
   * @param otherTrie AbstractBinaryTrie
   */
  protected void buildFromExisting(final AbstractBinaryTrie<K, V> otherTrie) {

    Node<K, V> myNode = this.root;
    Node<K, V> otherNode = otherTrie.root;

    // Pre-Order tree traversal
    outer: while (otherNode != null) {

      if (otherNode.left != null) {
        otherNode = otherNode.left;
        myNode = myNode.getOrCreateEmpty(true);
        myNode.value = otherNode.value;
        if (cacheKeys && myNode.value != null) {
          myNode.privateKey = otherNode.privateKey;
        }
        continue;
      }

      if (otherNode.right != null) {
        otherNode = otherNode.right;
        myNode = myNode.getOrCreateEmpty(false);
        myNode.value = otherNode.value;
        if (cacheKeys && myNode.value != null) {
          myNode.privateKey = otherNode.privateKey;
        }
        continue;
      }

      // We are a leaf node
      while (otherNode.parent != null) {

        if (otherNode == otherNode.parent.left && otherNode.parent.right != null) {
          otherNode = otherNode.parent.right;
          myNode = myNode.parent.getOrCreateEmpty(false);
          myNode.value = otherNode.value;
          if (cacheKeys && myNode.value != null) {
            myNode.privateKey = otherNode.privateKey;
          }
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
        if (cacheKeys || subNode.privateKey != null) {
          subNode.privateKey = key;
        }
        ++this.modCount;
        final V oldValue = subNode.value;
        subNode.value = value;
        return oldValue;
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

  /**
   * Delete a Node. If a leaf node, this will also delete any empty
   * intermediate parents of the Node, to maintain the contract that all
   * leaf nodes must have a value.
   *
   * @param node Node to delete
   */
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

  /**
   * Return the Node for a given key, or null if not found or the key is null
   *
   * @param key the Key searched for
   * @return Node if found, or null
   */
  protected Node<K, V> getNode(final K key) {
    return getNode(key, root, 0, codec, false);
  }

  /**
   * Return the Node for a given key, or null if not found or the key is null
   *
   * @param key the Key searched for
   * @param startingNode the node to begin our search underneath (usually root)
   * @param startingIndex the key element index corresponding to the depth of
   *        the startingNode (usually zero)
   * @param codec KeyCodec
   * @param canBeEmpty true if empty intermediate nodes can be returned, false
   *        if only nodes with values may be returned
   * @return Node if found, or null
   */
  protected static <K, V> Node<K, V> getNode(final K key, final Node<K, V> startingNode,
      int startingIndex, final KeyCodec<K> codec, final boolean canBeEmpty) {
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
    while (true) {
      if (codec.isLeft(key, startingIndex++)) {
        subNode = subNode.left;
      } else {
        subNode = subNode.right;
      }

      if (subNode == null) {
        return null;
      }
      if (startingIndex == stopDepth && (subNode.value != null || canBeEmpty)) {
        return subNode;
      }
      if (startingIndex >= stopDepth) {
        return null;
      }
    }
  }



  /**
   * @return the first Node in the Trie, or null
   */
  protected Node<K, V> firstNode() {
    return successor(root);
  }



  /**
   * @param node Node to find the successor of (the next node)
   * @return the successor of the specified Node, or null if no such.
   */
  protected static <K, V> Node<K, V> successor(final Node<K, V> node) {
    return successor(node, null, false);
  }

  /**
   * @param node Node to find the successor of (the next node)
   * @param parentFence Node to force the search for successors to be under
   *        (descendants of), or null if no limit
   * @param canBeEmpty true if empty intermediate nodes can be returned, false
   *        if only nodes with values may be returned
   * @return the successor of the specified Node, or null if no such.
   */
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



  /**
   * @return the last Node in the Trie, or null if none
   */
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



  /**
   * @param node the Node to find the predecessor of (previous)
   * @return the predecessor of the specified Node Entry, or null if no such.
   */
  protected static <K, V> Node<K, V> predecessor(final Node<K, V> node) {
    return predecessor(node, null, false);
  }

  /**
   * @param node the Node to find the predecessor of (previous)
   * @param parentFence Node to force the search for predecessor to be under
   *        (descendants of), or null if no limit
   * @param canBeEmpty true if empty intermediate nodes can be returned, false
   *        if only nodes with values may be returned
   * @return the predecessor of the specified Node Entry, or null if no such.
   */
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
  public Collection<V> valuesPrefixOf(final K key, final boolean keyInclusive) {
    return new TriePrefixValues<K, V>(this, key, true, keyInclusive, false, false);
  }

  @Override
  public Collection<V> valuesPrefixedBy(final K key, final boolean keyInclusive) {
    return new TriePrefixValues<K, V>(this, key, false, keyInclusive, true, false);
  }

  @Override
  public Collection<V> valuesPrefixesOfOrBy(final K key) {
    return new TriePrefixValues<K, V>(this, key, true, true, true, false);
  }


  @Override
  public V valueShortestPrefixOf(final K key, final boolean keyInclusive) {
    final Iterator<V> iter =
        new TriePrefixValues<K, V>(this, key, true, keyInclusive, false, false).iterator();
    return iter.hasNext() ? iter.next() : null;
  }

  @Override
  public V valueLongestPrefixOf(final K key, final boolean keyInclusive) {
    final Iterator<V> iter =
        new TriePrefixValues<K, V>(this, key, true, keyInclusive, false, false).iterator();
    V value = null;
    while (iter.hasNext()) {
      value = iter.next();
    }
    return value;
  }



  /** View class for a Collection of Values that are prefixes of a Key. */
  protected static class TriePrefixValues<K, V> extends AbstractCollection<V> {

    protected final AbstractBinaryTrie<K, V> trie; // the backing trie

    protected final K key;
    protected final boolean includePrefixOfKey;
    protected final boolean keyInclusive;
    protected final boolean includePrefixedByKey;
    protected final boolean canBeEmpty;


    /**
     * Create a new TriePrefixValues View
     *
     * @param trie the backing trie
     * @param key the search key
     * @param includePrefixOfKey true if the values' keys may include prefixes of the search key
     * @param keyInclusive true if the values' keys may include the search key
     * @param includePrefixedByKey true if the values' keys may include keys
     *        prefixed by the search key
     * @param canBeEmpty true if empty intermediate nodes can be returned, false
     *        if only nodes with values may be returned
     */
    protected TriePrefixValues(final AbstractBinaryTrie<K, V> trie, final K key,
        final boolean includePrefixOfKey, final boolean keyInclusive,
        final boolean includePrefixedByKey, final boolean canBeEmpty) {

      if (key == null) {
        throw new NullPointerException(getClass().getName()
            + " does not allow null keys: " + key);
      }
      this.trie = trie;
      this.key = key;
      this.includePrefixOfKey = includePrefixOfKey;
      this.keyInclusive = keyInclusive;
      this.includePrefixedByKey = includePrefixedByKey;
      this.canBeEmpty = canBeEmpty;
    }

    @Override
    public final Iterator<V> iterator() {
      return new ValuePrefixIterator<K, V>(trie, key, includePrefixOfKey, keyInclusive,
          includePrefixedByKey, canBeEmpty);
    }

    @SuppressWarnings("unused")
    @Override
    public final int size() {
      long total = 0L;
      for (final V value : this) {
        ++total;
      }
      return total > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) total;
    }

    @Override
    public final boolean isEmpty() {
      return !iterator().hasNext();
    }

    @Override
    public final boolean remove(final Object o) {
      Node<K, V> node = null;
      // only remove values that occur in this sub-trie
      final Iterator<Node<K, V>> iter =
          new NodePrefixIterator<K, V>(trie, key, includePrefixOfKey, keyInclusive,
              includePrefixedByKey, canBeEmpty);
      while (iter.hasNext()) {
        node = iter.next();
        if (eq(node.value, o)) {
          iter.remove();
          return true;
        }
      }
      return false;
    }

    @Override
    public final void clear() {
      // only remove values that occur in this sub-trie
      final Iterator<Node<K, V>> iter =
          new NodePrefixIterator<K, V>(trie, key, includePrefixOfKey, keyInclusive,
              includePrefixedByKey, canBeEmpty);
      while (iter.hasNext()) {
        iter.next();
        iter.remove();
      }
    }
  }



  /** Iterator for returning only prefix values in ascending order */
  protected static final class ValuePrefixIterator<K, V> extends AbstractPrefixIterator<K, V, V> {

    protected ValuePrefixIterator(final AbstractBinaryTrie<K, V> trie, final K key,
        final boolean includePrefixOfKey, final boolean keyInclusive,
        final boolean includePrefixedByKey, final boolean canBeEmpty) {
      super(trie, key, includePrefixOfKey, keyInclusive, includePrefixedByKey, canBeEmpty);
    }

    @Override
    public final V next() {
      return nextNode().value;
    }
  }

  /** Iterator for returning prefix Nodes in ascending order (must export before returning them) */
  protected static final class NodePrefixIterator<K, V>
      extends AbstractPrefixIterator<K, V, Node<K, V>> {

    protected NodePrefixIterator(final AbstractBinaryTrie<K, V> trie, final K key,
        final boolean includePrefixOfKey, final boolean keyInclusive,
        final boolean includePrefixedByKey, final boolean canBeEmpty) {
      super(trie, key, includePrefixOfKey, keyInclusive, includePrefixedByKey, canBeEmpty);
    }

    @Override
    public final Node<K, V> next() {
      return nextNode();
    }
  }


  /**
   * Base Prefix Iterator class for extending
   *
   * @param <K> Key
   * @param <V> Value
   * @param <T> Iterator object type
   */
  protected abstract static class AbstractPrefixIterator<K, V, T> implements Iterator<T> {

    protected final AbstractBinaryTrie<K, V> trie; // the backing trie

    protected Node<K, V> next;
    protected Node<K, V> lastReturned;
    protected int expectedModCount;
    protected final K key;
    protected final int stopDepth;
    protected int index = 0;
    protected Node<K, V> limit = null;
    protected final boolean includePrefixOfKey;
    protected final boolean keyInclusive;
    protected final boolean includePrefixedByKey;
    protected final boolean canBeEmpty;

    /**
     * Create a new prefix iterator
     *
     * @param trie the backing trie
     * @param key the search key (may not be null)
     * @param includePrefixOfKey true if the values' keys may include prefixes of the search key
     * @param keyInclusive true if the values' keys may include the search key
     * @param includePrefixedByKey true if the values' keys may include keys
     *        prefixed by the search key
     * @param canBeEmpty true if empty intermediate nodes can be returned, false
     *        if only nodes with values may be returned
     */
    protected AbstractPrefixIterator(final AbstractBinaryTrie<K, V> trie, final K key,
        final boolean includePrefixOfKey, final boolean keyInclusive,
        final boolean includePrefixedByKey, final boolean canBeEmpty) {

      if (key == null) {
        throw new NullPointerException(getClass().getName()
            + " does not allow null keys: " + key);
      }

      this.trie = trie;
      this.expectedModCount = trie.modCount;

      this.key = key;
      this.includePrefixOfKey = includePrefixOfKey;
      this.keyInclusive = keyInclusive;
      this.includePrefixedByKey = includePrefixedByKey;
      this.canBeEmpty = canBeEmpty;

      this.stopDepth = trie.codec.length(key);

      if (this.stopDepth <= 0) {
        throw new IllegalArgumentException(AbstractBinaryTrie.class.getClass().getName()
            + " does not accept keys of length <= 0: " + key);
      }

      this.lastReturned = null;
      this.next = getNextPrefixNode(trie.root); // must always start at root
    }


    // TODO: previous/descending could be done by first getting the last node via nextPrefix,
    // then working your way back with opposite logic to nextPrefix
    /**
     * @param node Node to find the next successor node of
     * @return the successor prefix node, or null if none
     */
    protected Node<K, V> getNextPrefixNode(Node<K, V> node) {
      // Prefix-Of = all nodes that are direct parents of the Key's node
      // Prefix-By = all children nodes of the Key's node

      while (node != null) {

        if (index > stopDepth) {
          // Traverse all nodes under the Key (limit)
          node = successor(node, limit, canBeEmpty);

        } else {
          // Traverse only the path that matches our Key
          if (trie.codec.isLeft(key, index++)) {
            node = node.left;
          } else {
            node = node.right;
          }
          if (index == stopDepth) {
            // Force any subsequent tree traversal to be under this node (the Key)
            limit = node;
          }
        }

        if (node != null) {
          // If conditions match, return the node
          if ((node.value != null || canBeEmpty) && inRange(node)) {

            if (index < stopDepth) {
              if (includePrefixOfKey) {
                return node;
              }

            } else if (index == stopDepth) {
              if (keyInclusive) {
                return node;
              }

            } else { // index > stopDepth
              if (includePrefixedByKey) {
                return node;
              }
            }

          }

          // Exit early if all further conditions are false
          if ((index + 1 == stopDepth && (!keyInclusive && !includePrefixedByKey))
              || (index >= stopDepth && !includePrefixedByKey)) {
            return null;
          }
        }
      }

      return null;
    }


    /**
     * Overrided for use with sub-maps
     *
     * @param node Node to query if in range
     * @return true if the Node is in range for this trie or submap
     */
    protected boolean inRange(final Node<K, V> node) {
      return true;
    }


    @Override
    public final boolean hasNext() {
      return next != null;
    }

    /**
     * @return the next Node in ascending order
     */
    protected final Node<K, V> nextNode() {
      final Node<K, V> e = next;
      if (e == null) {
        throw new NoSuchElementException();
      }
      if (trie.modCount != expectedModCount) {
        throw new ConcurrentModificationException();
      }
      next = getNextPrefixNode(e);
      lastReturned = e;
      return e;
    }

    @Override
    public final void remove() {
      if (lastReturned == null) {
        throw new IllegalStateException();
      }
      if (trie.modCount != expectedModCount) {
        throw new ConcurrentModificationException();
      }
      trie.deleteNode(lastReturned);
      expectedModCount = trie.modCount;
      lastReturned = null;
    }

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
  public Set<K> keySet() {
    if (keySet == null) {
      keySet = new TrieKeySet<K>(this);
    }
    return keySet;
  }


  /** KeySet View Set of Keys */
  protected static final class TrieKeySet<K> extends AbstractSet<K>
      implements Set<K> {

    protected final AbstractBinaryTrie<K, ? extends Object> m;

    /**
     * Create a new TrieKeySet view
     *
     * @param map the backing AbstractBinaryTrie
     */
    protected TrieKeySet(final AbstractBinaryTrie<K, ? extends Object> map) {
      m = map;
    }

    @Override
    public final Iterator<K> iterator() {
      return m.keyIterator();
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
    public final boolean remove(final Object o) {
      return m.remove(o) != null;
    }

  }



  @Override
  public Collection<V> values() {
    if (values == null) {
      values = new TrieValues<K, V>(this);
    }
    return values;
  }

  /** TrieValues View Collection of Values */
  protected static final class TrieValues<K, V> extends AbstractCollection<V> {

    protected final AbstractBinaryTrie<K, V> m; // the backing map

    /**
     * Create a new TrieValues view
     *
     * @param map the backing AbstractBinaryTrie
     */
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
        if (eq(e.value, o)) {
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
    final Set<Map.Entry<K, V>> es = entrySet;
    return (es != null) ? es : (entrySet = new TrieEntrySet());
  }

  /** TrieEntrySet View Set of Map.Entry key-value pairs */
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
      return p != null && eq(p.value, value);
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
      if (p != null && eq(p.value, value)) {
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



  /** Iterator for returning exported Map.Entry views of Nodes in ascending order */
  protected static final class EntryIterator<K, V>
      extends AbstractEntryIterator<K, V, Map.Entry<K, V>> {

    protected EntryIterator(final AbstractBinaryTrie<K, V> map) {
      super(map, map.firstNode());
    }

    @Override
    public final Map.Entry<K, V> next() {
      return exportEntry(nextNode(), m);
    }
  }

  /** Iterator for returning only values in ascending order */
  protected static final class ValueIterator<K, V> extends AbstractEntryIterator<K, V, V> {

    protected ValueIterator(final AbstractBinaryTrie<K, V> map) {
      super(map, map.firstNode());
    }

    @Override
    public final V next() {
      return nextNode().value;
    }
  }

  /**
   * @return Iterator returning resolved keys in ascending order
   */
  protected final Iterator<K> keyIterator() {
    return new KeyIterator<K, V>(this);
  }

  /** Iterator for returning only resolved keys in ascending order */
  protected static final class KeyIterator<K, V> extends AbstractEntryIterator<K, V, K> {

    protected KeyIterator(final AbstractBinaryTrie<K, V> map) {
      super(map, map.firstNode());
    }

    @Override
    public final K next() {
      return exportEntry(nextNode(), m).getKey();
    }
  }



  /**
   * Base Entry Iterator for extending
   *
   * @param <K> Key
   * @param <V> Value
   * @param <T> Iterator object type
   */
  protected abstract static class AbstractEntryIterator<K, V, T> implements Iterator<T> {

    protected final AbstractBinaryTrie<K, V> m; // the backing map

    protected Node<K, V> next;
    protected Node<K, V> lastReturned;
    protected int expectedModCount;

    /**
     * Create a new AbstractEntryIterator
     *
     * @param map the backing trie
     * @param first the first Node returned by nextNode or prevNode
     */
    protected AbstractEntryIterator(final AbstractBinaryTrie<K, V> map, final Node<K, V> first) {
      this.m = map;
      expectedModCount = m.modCount;
      lastReturned = null;
      next = first;
    }

    @Override
    public final boolean hasNext() {
      return next != null;
    }

    /**
     * @return the successor Node (ascending order) or null
     */
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

    /**
     * @return the predecessor Node (descending order) or null
     */
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
  public int hashCode() {
    // To stay compatible with Map interface, we are equal to any map with the same mappings
    int h = 0;
    for (Node<K, V> node = this.firstNode(); node != null; node = successor(node)) {
      final V value = node.value;
      final K key = resolveKey(node, this);
      // Map.hashCode compatibility
      h += (key == null ? 0 : key.hashCode()) ^
          (value == null ? 0 : value.hashCode());
    }
    return h;
  }



  @SuppressWarnings("unchecked")
  @Override
  public boolean equals(final Object o) {

    if (o == this) {
      return true;
    }

    if (o instanceof AbstractBinaryTrie) {
      // We are comparing against another AbstractBinaryTrie, so we can take shortcuts
      final AbstractBinaryTrie<K, V> t = (AbstractBinaryTrie<K, V>) o;
      if (t.size() != size()) {
        return false;
      }
      return compareAllNodes(this.root, t.root);
    }

    if (o instanceof Map) {
      final Map<K, V> m = (Map<K, V>) o;
      if (m.size() != size()) {
        return false;
      }
      // To stay compatible with Map interface, we are equal to any map with the same mappings
      try {
        for (Node<K, V> node = this.firstNode(); node != null; node = successor(node)) {
          final V value = node.value;
          final K key = resolveKey(node, this);
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

    return false;
  }


  /**
   * Compares one Node to another Node. Does not recursively compare children,
   * only tests the existence of children. Assumes both nodes are in the same
   * position in the structure (and therefore have equal keys).
   *
   * @param myNode Node (or null)
   * @param otherNode Node (or null)
   * @return false if either node has children the other lacks, and false
   *         if the values are not equal
   */
  protected static final <K, V> boolean compareNodeAndExistenceOfChildren(
      final AbstractBinaryTrie.Node<K, V> myNode,
      final AbstractBinaryTrie.Node<K, V> otherNode) {

    if (myNode == null && otherNode == null) {
      return true;
    }

    if ((myNode == null && otherNode != null)
        || (myNode != null && otherNode == null)) {
      return false;
    }

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

  /**
   * Compare the node's values and node structure of two tries, starting at
   * two Node at the same place in their respective structures, and then
   * walking both trie's.
   *
   * @param myNode Node from one trie (usually root)
   * @param otherNode Node from the other trie (usually root)
   * @return true if the trie's are equal because the node structures are equal
   *         and the nodes at the same spot in their respective structures have
   *         equal values
   */
  protected static final <K, V> boolean compareAllNodes(Node<K, V> myNode, Node<K, V> otherNode) {

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

    return compareNodeAndExistenceOfChildren(myNode, otherNode);
  }



  @Override
  public String toString() {
    // maybe create a ascii diagram for the tree structure?
    if (this.isEmpty()) {
      return "{}";
    }

    final StringBuilder sb = new StringBuilder();
    sb.append('{');

    for (Node<K, V> node = this.firstNode(); node != null;) {
      final V value = node.value;
      final K key = resolveKey(node, this);
      sb.append(key == this ? "(this Map)" : key);
      sb.append('=');
      sb.append(value == this ? "(this Map)" : value);
      if ((node = successor(node)) == null) {
        break;
      }
      sb.append(',').append(' ');
    }
    return sb.append('}').toString();
  }



  /**
   * Write out this trie to the output stream.
   * First write the default object
   * Second write out cacheKeys, writeKeys, then size
   * Third, if writeKeys is true then write out alternating key-value pairs,
   * and if false then write out the node structure (with values but without
   * keys)
   *
   * @param s ObjectOutputStream
   * @throws IOException
   */
  private final void writeObject(final ObjectOutputStream s) throws IOException {
    // Write out the codec and any hidden stuff
    s.defaultWriteObject();

    // Write out cacheKeys (whether we are keeping keys around or not)
    s.writeBoolean(cacheKeys);

    // Write out writeKeys (whether we are writing out keys or not)
    s.writeBoolean(writeKeys);

    // Write out size (number of Mappings)
    s.writeLong(size);

    if (writeKeys) {
      // If writeKeys, Write out keys and values (alternating)
      for (Node<K, V> node = this.firstNode(); node != null; node = successor(node)) {
        s.writeObject(resolveKey(node, this));
        s.writeObject(node.value);
        if (!cacheKeys) {
          node.privateKey = null; // Clear the key
        }
      }
    } else {
      // If not writing keys, Just write out the root node
      s.writeObject(root);
      // Because each Node has 3 reference pointers to up to 3 other Nodes,
      // (pointers are 32 or 64 bits) writing out the nodes like this ends up saving space when the
      // size of keys > size of 3 pointers, but costs extra space if size of key < 3 pointers
      // Even if the size of keys is smaller, writing the root nodes would be faster than
      // resolving and writing un-cached non-resolved keys
    }
  }

  /**
   * Read in this trie from the input stream.
   * First read the default object
   * Second read in cacheKeys, writeKeys, then size
   * Third, if writeKeys is true then read in and put alternating key-value
   * pairs, and if false then read in the node structure (with values but without
   * keys)
   *
   * @param s ObjectInputStream
   * @throws IOException
   * @throws ClassNotFoundException
   */
  @SuppressWarnings("unchecked")
  private final void readObject(final ObjectInputStream s)
      throws IOException, ClassNotFoundException {
    // Read in the codec and any hidden stuff
    s.defaultReadObject();

    // Read in cacheKeys (whether we are keeping keys around or not)
    this.cacheKeys = s.readBoolean();

    // Read in writeKeys (whether we are writing out keys or not)
    this.writeKeys = s.readBoolean();

    // Read in size (number of Mappings)
    final long originalSize = s.readLong();

    if (writeKeys) {
      // If writeKeys, read in keys and values (alternating)
      this.root = new Node<K, V>(null);
      for (int i = 0; i < originalSize; ++i) {
        final K key = (K) s.readObject();
        final V value = (V) s.readObject();
        this.put(key, value);
      }
      assert(this.size == originalSize);
    } else {
      // If not writing keys, Just read in the root node
      this.root = (Node<K, V>) s.readObject();
      assert(this.root.value == null);
      assert(this.root.parent == null);
      this.size = originalSize;
    }

  }


}
