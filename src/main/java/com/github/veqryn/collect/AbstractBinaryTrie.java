/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.github.veqryn.collect;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collection;
import java.util.List;
import java.util.Map;


/**
 * AbstractBinaryTrie class
 *
 * @author Mark Christopher Duncan
 *
 * @param <K> Key
 * @param <V> Value
 */
public class AbstractBinaryTrie<K, V> implements Trie<K, V>, Serializable, Cloneable {
  // TODO: in new interface, implement more 'value' based methods

  private static final long serialVersionUID = 4494549156276631388L;


  protected final KeyCodec<K> codec;

  protected final Node<K, V> root = new Node<K, V>(null);

  protected long size = 0;

  protected transient volatile int modCount = 0;



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



  public KeyCodec<K> getCodec() {
    return codec;
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

    return node;
  }



  /**
   * Test two values for equality. Differs from o1.equals(o2) only in
   * that it copes with {@code null} o1 properly.
   */
  protected static final boolean eq(final Object o1, final Object o2) {
    return (o1 == null ? o2 == null : (o1 == o2 || o1.equals(o2)));
  }



  protected void clearTransientMemory() {
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

  protected int size(final Node<K, V> parentFence, final boolean countEmptyNodes) {
    long total = 0L;
    Node<K, V> subTree = root;
    while ((subTree = successor(subTree, parentFence, countEmptyNodes)) != null) {
      ++total;
    }
    return total > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) total;
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



  protected Node<K, V> firstNode() {
    return successor(root);
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
  public int hashCode() {
    // To stay compatible with Map interface, we are equal to any map with the same mappings
    int h = 0;
    for (Node<K, V> node = this.firstNode(); node != null; node = successor(node)) {
      final V value = node.getValue();
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

    return false;
  }

  protected static final <K, V> boolean compareNodeAndExistenceOfChildren(
      final AbstractBinaryTrie.Node<K, V> myNode,
      final AbstractBinaryTrie.Node<K, V> otherNode) {

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

    return true;
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
      final V value = node.getValue();
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


}
