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
public class AbstractBinaryTrie<K, V> implements NavigableMap<K, V>, Serializable {
  // maybe implement guava Multimap or SortedSetMultimap
  // maybe implement apache commons collection interfaces?
  // TODO: implement externalizable, writeObject, readObject

  private static final long serialVersionUID = 4494549156276631388L;

  protected final KeyCodec<K> codec;

  protected final Node root = new Node(null);

  protected transient volatile long size = 0;
  protected transient volatile boolean dirty = false;
  protected transient volatile int modCount = 0;

  protected transient volatile TrieEntrySet entrySet = null;
  protected transient volatile NavigableSet<K> keySet = null;
  protected transient volatile Collection<V> values = null;
  protected transient volatile NavigableMap<K, V> descendingMap = null;
  protected transient volatile Comparator<K> comparator = null;


  public AbstractBinaryTrie(final KeyCodec<K> keyCodec) {
    this.codec = keyCodec;
  }



  protected final class Node implements Map.Entry<K, V>, Serializable {

    private static final long serialVersionUID = -534919147906916778L;

    protected transient volatile K key = null;
    protected V value = null;
    protected Node left = null;
    protected Node right = null;
    protected final Node parent;

    protected Node(final Node parent) {
      this.parent = parent;
    }

    protected Node getOrCreateEmpty(final boolean leftNode) {
      if (leftNode) {
        if (left == null) {
          left = new Node(this);
        }
        return left;
      } else {
        if (right == null) {
          right = new Node(this);
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
     * @return the key
     */
    @Override
    public final K getKey() {
      if (this.parent == null) {
        return null; // We are the root node
      }
      if (key != null) {
        return key;
      }

      final BitSet bits = new BitSet();
      int levelsDeep = 0;
      Node previousParent = this;
      Node currentParent = this.parent;
      while (currentParent != null) {
        if (currentParent.right == previousParent) {
          bits.set(levelsDeep);
        }
        previousParent = currentParent;
        currentParent = currentParent.parent;
        levelsDeep++;
      }

      key = codec.recreateKey(bits, levelsDeep);

      if (key == null) {
        throw new IllegalStateException("Unable to create non-null key with key-codec");
      }
      if (AbstractBinaryTrie.this.getNode(key, true) != this) {
        throw new IllegalStateException("Created key not equal to our original key");
      }
      return key;
    }

    /**
     * @return the value associated with the key
     */
    @Override
    public final V getValue() {
      return value;
    }

    /**
     * Replaces the value currently associated with the key with the given value.
     *
     * @return the value associated with the key before this method was called
     */
    @Override
    public final V setValue(final V value) {
      final V oldValue = this.value;
      this.value = value;
      return oldValue;
    }

    @Override
    public final int hashCode() {
      final K key = getKey();
      final int keyHash = (key == null ? 0 : key.hashCode());
      final int valueHash = (value == null ? 0 : value.hashCode());
      return keyHash ^ valueHash;
    }

    @Override
    public final boolean equals(final Object o) {
      if (!(o instanceof Map.Entry)) {
        return false;
      }
      final Map.Entry<?, ?> e = (Map.Entry<?, ?>) o;
      return eq(getKey(), e.getKey()) && eq(value, e.getValue());
    }

    @Override
    public final String toString() {
      return getKey() + "=" + value;
    }

  }



  /**
   * Test two values for equality. Differs from o1.equals(o2) only in
   * that it copes with {@code null} o1 properly.
   */
  protected static final boolean eq(final Object o1, final Object o2) {
    return (o1 == null ? o2 == null : o1.equals(o2));
  }

  /**
   * Return SimpleImmutableEntry for entry, or null if null
   */
  protected Map.Entry<K, V> exportEntry(final Map.Entry<K, V> entry) {
    // TODO: create an immutable node
    return (entry == null || entry == root) ? null : new AbstractMap.SimpleImmutableEntry<>(entry);
  }

  /**
   * Returns the key corresponding to the specified Entry.
   *
   * @throws NoSuchElementException if the Entry is null
   */
  protected K keyOrNoSuchElementException(final Entry<K, ?> entry) {
    if (entry == null || entry == root) {
      throw new NoSuchElementException();
    }
    return entry.getKey();
  }

  /**
   * Returns the key corresponding to the specified Entry,
   * or null if it does not exist
   */
  protected K keyOrNull(final Entry<K, ?> entry) {
    if (entry == null || entry == root) {
      return null;
    }
    return entry.getKey();
  }

  protected void clearTransientMemory() {
    entrySet = null;
    keySet = null;
    values = null;
    descendingMap = null;
    comparator = null;
    // clear keys from Nodes
    Node subTree = root;
    while ((subTree = successor(subTree)) != null) {
      subTree.key = null;
    }
  }



  @Override
  public void clear() {
    this.root.value = null;
    this.root.left = null;
    this.root.right = null;
    this.size = 0L;
    ++this.modCount;
    this.dirty = false;
  }


  @Override
  public boolean isEmpty() {
    return root.isEmpty();
  }


  @Override
  public int size() {
    return size(false);
  }

  protected int size(final boolean countEmptyNodes) {
    // TODO: maybe switch to keeping size up to date all the time
    if (this.dirty) {
      long total = 0L;
      Node subTree = root;
      while ((subTree = successor(subTree, countEmptyNodes)) != null) {
        ++total;
      }
      this.size = total;
      this.dirty = false;
    }
    return size > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) size;
  }



  @Override
  public Comparator<? super K> comparator() {
    if (comparator == null) {
      comparator = new Comparator<K>() {
        @Override
        public int compare(final K o1, final K o2) {
          if (o1 == null || o2 == null) {
            throw new IllegalArgumentException("Null keys not allowed");
          }
          if (o1 == o2 || o1.equals(o2)) {
            return 0;
          }
          final int l1 = codec.length(o1);
          final int l2 = codec.length(o2);
          final int min = Math.min(l1, l2);
          boolean left1;
          boolean left2;
          for (int i = 0; i < min; ++i) {
            left1 = codec.isLeft(o1, i);
            left2 = codec.isLeft(o2, i);
            if (left1 && !left2) {
              return -1;
            }
            if (!left1 && left2) {
              return 1;
            }
          }
          return l1 - l2;
        }
      };
    }
    return comparator;
  }



  @Override
  public V put(final K key, final V value) {
    if (key == null) {
      throw new IllegalArgumentException(getClass().getName()
          + " does not accept null keys: " + key);
    }
    if (value == null) {
      throw new IllegalArgumentException(getClass().getName()
          + " does not accept null values: " + value);
    }

    final int stopDepth = codec.length(key);

    if (stopDepth <= 0) {
      throw new IllegalArgumentException(getClass().getName()
          + " does not accept keys of length <= 0: " + key);
    }

    Node subNode = root;
    int i = 0;
    while (true) {
      subNode = subNode.getOrCreateEmpty(codec.isLeft(key, i++));
      if (i == stopDepth) {
        this.dirty = true;
        ++this.modCount;
        return subNode.setValue(value);
      }
    }
  }

  @Override
  public void putAll(final Map<? extends K, ? extends V> map) {
    for (final Map.Entry<? extends K, ? extends V> entry : map.entrySet()) {
      put(entry.getKey(), entry.getValue());
    }
  }



  @Override
  public V remove(final Object key) {
    @SuppressWarnings("unchecked")
    final Node p = getNode((K) key);
    if (p == null) {
      return null;
    }

    final V oldValue = p.value;
    deleteNode(p, false);
    return oldValue;
  }

  protected void deleteNode(final Node node, final boolean deleteChildren) {
    if (node == null) {
      return;
    }

    node.value = null;

    if (deleteChildren) {
      node.left = null;
      node.right = null;
    }

    Node previousParent = node;
    Node currentParent = node.parent;
    while (previousParent.isEmpty() && currentParent != null) {
      if (currentParent.left == previousParent) {
        currentParent.left = null;
      } else {
        currentParent.right = null;
      }
      previousParent = currentParent;
      currentParent = currentParent.parent;
    }
    ++modCount;
    dirty = true;
  }



  @SuppressWarnings("unchecked")
  @Override
  public boolean containsKey(final Object key) {
    return getNode((K) key) != null;
  }

  @SuppressWarnings("unchecked")
  @Override
  public V get(final Object key) {
    final Node node = getNode((K) key);
    return node == null ? null : node.value;
  }

  protected Node getNode(final K key) {
    return getNode(key, false);
  }

  protected Node getNode(final K key, final boolean canBeEmpty) {

    if (key == null) {
      return null;
    }

    final int stopDepth = codec.length(key);

    // Look up a single record
    Node subNode = root;
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

  protected Node ceilingOrHigherNode(final K key, final boolean higher) {

    if (key == null) {
      return null;
    }

    final int stopDepth = codec.length(key);

    // Look up a single record
    boolean left;
    Node predecessor;
    Node subNode = root;
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
    return exportEntry(ceilingOrHigherNode(key, false));
  }

  @Override
  public K ceilingKey(final K key) {
    if (key == null) {
      throw new NullPointerException(getClass().getName()
          + " does not accept null keys: " + key);
    }
    return keyOrNull(ceilingOrHigherNode(key, false));
  }

  @Override
  public Map.Entry<K, V> higherEntry(final K key) {
    if (key == null) {
      throw new NullPointerException(getClass().getName()
          + " does not accept null keys: " + key);
    }
    return exportEntry(ceilingOrHigherNode(key, true));
  }

  @Override
  public K higherKey(final K key) {
    if (key == null) {
      throw new NullPointerException(getClass().getName()
          + " does not accept null keys: " + key);
    }
    return keyOrNull(ceilingOrHigherNode(key, true));
  }

  protected Node floorOrLowerNode(final K key, final boolean lower) {

    if (key == null) {
      return null;
    }

    final int stopDepth = codec.length(key);

    // Look up a single record
    boolean left;
    Node predecessor;
    Node subNode = root;
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
    return exportEntry(floorOrLowerNode(key, false));
  }

  @Override
  public K floorKey(final K key) {
    if (key == null) {
      throw new NullPointerException(getClass().getName()
          + " does not accept null keys: " + key);
    }
    return keyOrNull(floorOrLowerNode(key, false));
  }

  @Override
  public Map.Entry<K, V> lowerEntry(final K key) {
    if (key == null) {
      throw new NullPointerException(getClass().getName()
          + " does not accept null keys: " + key);
    }
    return exportEntry(floorOrLowerNode(key, true));
  }

  @Override
  public K lowerKey(final K key) {
    if (key == null) {
      throw new NullPointerException(getClass().getName()
          + " does not accept null keys: " + key);
    }
    return keyOrNull(floorOrLowerNode(key, true));
  }


  protected List<V> getAll(final K key) {
    final List<Node> nodes = getAll(key, true);
    final List<V> values = new ArrayList<>(nodes.size());
    for (final Node node : nodes) {
      values.add(node.value);
    }
    return values;
  }

  protected List<Node> getAll(final K key, final boolean dummy) {

    final List<Node> nodes = new ArrayList<>();

    if (key == null) {
      return nodes;
    }
    // TODO: this assumes our key goes to a leaf...

    // Do not branch, because we are looking up a single record,
    // not examining every branch that could contain it, or every value contained in a branch
    Node subNode = root;
    int i = 0;
    while (true) {
      if (codec.isLeft(key, i++)) {
        subNode = subNode.left;
      } else {
        subNode = subNode.right;
      }

      if (subNode == null) {
        return nodes;
      }
      if (subNode.value != null) {
        nodes.add(subNode);
      }
    }
  }


  protected Node firstNode() {
    return successor(root);
  }

  @Override
  public Map.Entry<K, V> firstEntry() {
    return exportEntry(firstNode());
  }

  @Override
  public K firstKey() {
    return keyOrNoSuchElementException(successor(root));
  }

  @Override
  public Map.Entry<K, V> pollFirstEntry() {
    final Node polled = firstNode();
    final Map.Entry<K, V> result = exportEntry(polled);
    if (polled != null) {
      deleteNode(polled, false);
    }
    return result;
  }


  /**
   * Returns the successor of the specified Node Entry, or null if no such.
   */
  protected Node successor(final Node node) {
    return successor(node, false);
  }

  protected Node successor(Node node, final boolean canBeEmpty) {

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

      Node previousParent = node;
      Node currentParent = node.parent;
      while (currentParent != null) {

        if (currentParent.left == previousParent && currentParent.right != null) {

          if (currentParent.right.value == null && !canBeEmpty) {
            node = currentParent.right;
            continue outer;
          }
          return currentParent.right;
        }
        previousParent = currentParent;
        currentParent = currentParent.parent;
      }
      return null;

    }
    return null;
  }



  protected Node lastNode() {
    // Rely on the fact that leaf nodes can not be empty
    Node parent = root;
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
    return exportEntry(lastNode());
  }

  @Override
  public K lastKey() {
    return keyOrNoSuchElementException(lastNode());
  }

  @Override
  public Map.Entry<K, V> pollLastEntry() {
    final Node polled = lastNode();
    final Map.Entry<K, V> result = exportEntry(polled);
    if (polled != null && polled != root) {
      deleteNode(polled, false);
    }
    return result;
  }


  /**
   * Returns the predecessor of the specified Node Entry, or null if no such.
   */
  protected Node predecessor(final Node node) {
    return predecessor(node, false);
  }

  protected Node predecessor(Node node, final boolean canBeEmpty) {

    while (node != null && node.parent != null) {

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
      Node child = node.parent.left;
      while (child.right != null || child.left != null) {
        if (child.right != null) {
          child = child.right;
        } else {
          child = child.left;
        }
      }

      if (child.value == null) {
        throw new IllegalStateException("Should not have a leaf node with no value");
      }
      return child;

    }
    return null;
  }



  @Override
  public boolean containsValue(final Object value) {
    if (value == null) {
      return false;
    }
    for (Node e = firstNode(); e != null; e = successor(e)) {
      if (eq(value, e.value)) {
        return true;
      }
    }
    return false;
  }


  @Override
  public Set<Map.Entry<K, V>> entrySet() {
    final TrieEntrySet es = entrySet;
    return (es != null) ? es : (entrySet = new TrieEntrySet());
  }


  @Override
  public NavigableSet<K> keySet() {
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

    private final NavigableMap<E, ? extends Object> m;

    TrieKeySet(final NavigableMap<E, ? extends Object> map) {
      m = map;
    }

    @Override
    public Iterator<E> iterator() {
      if (m instanceof AbstractBinaryTrie) {
        return ((AbstractBinaryTrie<E, ? extends Object>) m).keyIterator();
      } else {
        return (((AbstractBinaryTrie.NavigableTrieSubMap<E, ? extends Object>) m).keyIterator());
      }
    }

    @Override
    public Iterator<E> descendingIterator() {
      if (m instanceof AbstractBinaryTrie) {
        return ((AbstractBinaryTrie<E, ? extends Object>) m).descendingKeyIterator();
      } else {
        return (((AbstractBinaryTrie.NavigableTrieSubMap<E, ? extends Object>) m)
            .descendingKeyIterator());
      }
    }

    @Override
    public int size() {
      return m.size();
    }

    @Override
    public boolean isEmpty() {
      return m.isEmpty();
    }

    @Override
    public boolean contains(final Object o) {
      return m.containsKey(o);
    }

    @Override
    public void clear() {
      m.clear();
    }

    @Override
    public E lower(final E e) {
      return m.lowerKey(e);
    }

    @Override
    public E floor(final E e) {
      return m.floorKey(e);
    }

    @Override
    public E ceiling(final E e) {
      return m.ceilingKey(e);
    }

    @Override
    public E higher(final E e) {
      return m.higherKey(e);
    }

    @Override
    public E first() {
      return m.firstKey();
    }

    @Override
    public E last() {
      return m.lastKey();
    }

    @Override
    public Comparator<? super E> comparator() {
      return m.comparator();
    }

    @Override
    public E pollFirst() {
      final Map.Entry<E, ? extends Object> e = m.pollFirstEntry();
      return (e == null) ? null : e.getKey();
    }

    @Override
    public E pollLast() {
      final Map.Entry<E, ? extends Object> e = m.pollLastEntry();
      return (e == null) ? null : e.getKey();
    }

    @Override
    public boolean remove(final Object o) {
      final int oldSize = size();
      m.remove(o);
      return size() != oldSize;
    }

    @Override
    public NavigableSet<E> subSet(final E fromElement, final boolean fromInclusive,
        final E toElement, final boolean toInclusive) {
      return new TrieKeySet<>(m.subMap(fromElement, fromInclusive,
          toElement, toInclusive));
    }

    @Override
    public NavigableSet<E> headSet(final E toElement, final boolean inclusive) {
      return new TrieKeySet<>(m.headMap(toElement, inclusive));
    }

    @Override
    public NavigableSet<E> tailSet(final E fromElement, final boolean inclusive) {
      return new TrieKeySet<>(m.tailMap(fromElement, inclusive));
    }

    @Override
    public SortedSet<E> subSet(final E fromElement, final E toElement) {
      return subSet(fromElement, true, toElement, false);
    }

    @Override
    public SortedSet<E> headSet(final E toElement) {
      return headSet(toElement, false);
    }

    @Override
    public SortedSet<E> tailSet(final E fromElement) {
      return tailSet(fromElement, true);
    }

    @Override
    public NavigableSet<E> descendingSet() {
      return new TrieKeySet<E>(m.descendingMap());
    }

  }



  @Override
  public Collection<V> values() {
    // TODO: optimize this for our trie, then delete (currently copied from AbstractMap)
    if (values == null) {
      values = new AbstractCollection<V>() {
        @Override
        public final Iterator<V> iterator() {
          return new Iterator<V>() {
            private final Iterator<Entry<K, V>> i = entrySet().iterator();

            @Override
            public final boolean hasNext() {
              return i.hasNext();
            }

            @Override
            public final V next() {
              return i.next().getValue();
            }

            @Override
            public final void remove() {
              i.remove();
            }
          };
        }

        @Override
        public final int size() {
          return AbstractBinaryTrie.this.size();
        }

        @Override
        public final boolean isEmpty() {
          return AbstractBinaryTrie.this.isEmpty();
        }

        @Override
        public final void clear() {
          AbstractBinaryTrie.this.clear();
        }

        @Override
        public final boolean contains(final Object v) {
          return AbstractBinaryTrie.this.containsValue(v);
        }
      };
    }
    return values;
  }



  @Override
  public int hashCode() {
    // TODO: optimize this for our trie, then delete (currently copied from AbstractMap)
    int h = 0;
    final Iterator<Entry<K, V>> i = entrySet().iterator();
    while (i.hasNext()) {
      h += i.next().hashCode();
    }
    return h;
  }

  @Override
  public boolean equals(final Object o) {
    // TODO: optimize this for our trie, then delete (currently copied from AbstractMap)
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



  @Override
  public String toString() {
    // TODO: optimize this for our trie, then delete (currently copied from AbstractMap)
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



  protected final class TrieEntrySet extends AbstractSet<Map.Entry<K, V>> {

    @Override
    public Iterator<Map.Entry<K, V>> iterator() {
      return new EntryIterator();
    }

    @Override
    public final boolean contains(final Object o) {
      if (!(o instanceof Map.Entry)) {
        return false;
      }
      @SuppressWarnings("unchecked")
      final Map.Entry<K, V> entry = (Map.Entry<K, V>) o;
      final V value = entry.getValue();
      final Entry<K, V> p = getNode(entry.getKey());
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
      final Node p = getNode(entry.getKey());
      if (p != null && eq(p.getValue(), value)) {
        deleteNode(p, false);
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



  protected final class EntryIterator extends PrivateEntryIterator<Map.Entry<K, V>> {

    protected EntryIterator() {
      super(firstNode());
    }

    @Override
    public final Map.Entry<K, V> next() {
      return nextEntry();
    }
  }

  protected final class ValueIterator extends PrivateEntryIterator<V> {

    protected ValueIterator() {
      super(firstNode());
    }

    @Override
    public V next() {
      return nextEntry().getValue();
    }
  }

  protected Iterator<K> keyIterator() {
    return new KeyIterator();
  }

  protected final class KeyIterator extends PrivateEntryIterator<K> {

    protected KeyIterator() {
      super(firstNode());
    }

    @Override
    public K next() {
      return nextEntry().getKey();
    }
  }

  protected Iterator<K> descendingKeyIterator() {
    return new DescendingKeyIterator();
  }

  protected final class DescendingKeyIterator extends PrivateEntryIterator<K> {

    protected DescendingKeyIterator() {
      super(lastNode());
    }

    @Override
    public K next() {
      return prevEntry().getKey();
    }
  }


  protected abstract class PrivateEntryIterator<T> implements Iterator<T> {
    protected Node next;
    protected Node lastReturned;
    protected int expectedModCount;

    protected PrivateEntryIterator(final Node first) {
      expectedModCount = modCount;
      lastReturned = null;
      next = first;
    }

    @Override
    public final boolean hasNext() {
      return next != null;
    }

    protected final Entry<K, V> nextEntry() {
      final Node e = next;
      if (e == null) {
        throw new NoSuchElementException();
      }
      if (modCount != expectedModCount) {
        throw new ConcurrentModificationException();
      }
      next = successor(e);
      lastReturned = e;
      return e;
    }

    protected final Entry<K, V> prevEntry() {
      final Node e = next;
      if (e == null) {
        throw new NoSuchElementException();
      }
      if (modCount != expectedModCount) {
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
      if (modCount != expectedModCount) {
        throw new ConcurrentModificationException();
      }
      deleteNode(lastReturned, false);
      expectedModCount = modCount;
      lastReturned = null;
    }

  }



  protected abstract static class NavigableTrieSubMap<K, V> extends AbstractMap<K, V>
      implements NavigableMap<K, V>, Serializable {

    /** Returns ascending iterator from the perspective of this submap */
    abstract Iterator<K> keyIterator();

    /** Returns descending iterator from the perspective of this submap */
    abstract Iterator<K> descendingKeyIterator();
  }



  @Override
  public NavigableMap<K, V> descendingMap() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public NavigableSet<K> descendingKeySet() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public NavigableMap<K, V> subMap(final K fromKey, final boolean fromInclusive, final K toKey,
      final boolean toInclusive) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public NavigableMap<K, V> subMap(final K fromKey, final K toKey) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public NavigableMap<K, V> headMap(final K toKey, final boolean inclusive) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public NavigableMap<K, V> headMap(final K toKey) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public NavigableMap<K, V> tailMap(final K fromKey, final boolean inclusive) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public NavigableMap<K, V> tailMap(final K fromKey) {
    // TODO Auto-generated method stub
    return null;
  }

}
