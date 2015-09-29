/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.github.veqryn.collect;

import java.io.Serializable;
import java.util.AbstractCollection;
import java.util.AbstractSet;
import java.util.Collection;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

/**
 * AbstractBinaryTrie class
 *
 * @author Mark Christopher Duncan
 *
 * @param <K> Key
 * @param <V> Value
 */
public class AbstractBinaryTrie<K, V> implements Map<K, V>, Serializable {
  // TODO: NavigableMap
  // TODO: maybe implement gauva Multimap or SortedSetMultimap

  private static final long serialVersionUID = 4494549156276631388L;

  protected final KeyCodec<K> codec;

  protected final Node<K, V> root = new Node<>(null);

  protected transient volatile long size = 0;
  protected transient volatile boolean dirty = false;
  protected transient volatile int modCount = 0;

  protected transient volatile TrieEntrySet entrySet = null;
  protected transient volatile Set<K> keySet = null;
  protected transient volatile Collection<V> values = null;

  public AbstractBinaryTrie(final KeyCodec<K> keyCodec) {
    this.codec = keyCodec;
  }


  protected static final class Node<K, V> implements Map.Entry<K, V>, Serializable {

    private static final long serialVersionUID = -534919147906916778L;

    protected K key;
    protected V value = null;
    protected Node<K, V> left = null;
    protected Node<K, V> right = null;
    protected final Node<K, V> parent;

    protected Node(final Node<K, V> parent) {
      this.parent = parent;
    }

    protected Node<K, V> getOrCreateEmpty(final boolean leftNode) {
      if (leftNode) {
        if (left == null) {
          left = new Node<>(this);
        }
        return left;
      } else {
        if (right == null) {
          right = new Node<>(this);
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
      return eq(key, e.getKey()) && eq(value, e.getValue());
    }

    @Override
    public final String toString() {
      return key + "=" + value;
    }

  }


  /**
   * Test two values for equality. Differs from o1.equals(o2) only in
   * that it copes with {@code null} o1 properly.
   */
  protected static final boolean eq(final Object o1, final Object o2) {
    return (o1 == null ? o2 == null : o1.equals(o2));
  }


  @Override
  public void clear() {
    this.root.value = null;
    this.root.left = null;
    this.root.right = null;
    this.size = 0L;
    this.modCount = 0;
    this.dirty = false;
  }

  @Override
  public boolean isEmpty() {
    return root.isEmpty();
  }

  @Override
  public int size() {
    if (this.dirty) {
      long total = 0L;
      Node<K, V> subTree = root;
      while ((subTree = successor(subTree, false)) != null) {
        ++total;
      }
      this.size = total;
      this.dirty = false;
    }
    return size > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) size;
  }

  @SuppressWarnings("unchecked")
  @Override
  public boolean containsKey(final Object key) {
    return getNode((K) key) != null;
  }

  @SuppressWarnings("unchecked")
  @Override
  public V get(final Object key) {
    final Node<K, V> node = getNode((K) key);
    return node == null ? null : node.value;
  }

  protected Node<K, V> getNode(final K key) {
    return getNode(key, 0, Integer.MAX_VALUE);
  }

  protected Node<K, V> getNode(final K key, final int startDepth, final int stopDepth) {

    if (startDepth < 0
        || stopDepth < 1
        || startDepth > stopDepth) {
      throw new IllegalArgumentException("startDepth (" + startDepth + ") must be >= zero, "
          + "and <= to stopDepth (" + stopDepth + "), which must be >= 1");
    }

    if (key == null) {
      return null;
    }

    if (codec.length(key) < startDepth) {
      return null;
    }

    // Do not branch, even if startDepth > 0, because we are looking up a single record,
    // not examining every branch that could contain it, or every value contained in a branch
    Node<K, V> subNode = root;
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
      if (subNode.value != null
          && key.equals(subNode.key)
          && (i >= startDepth && i <= stopDepth)) {
        return subNode;
      }
      if (i >= stopDepth) {
        return null;
      }
    }
  }

  @Override
  public V put(final K key, final V value) {
    if (key == null) {
      throw new IllegalArgumentException(getClass().getName() + " does not accept null keys");
    }
    if (value == null) {
      throw new IllegalArgumentException(getClass().getName() + " does not accept null values");
    }

    final int stopDepth = codec.length(key);
    Node<K, V> subNode = root;
    int i = 0;
    while (true) {
      subNode = subNode.getOrCreateEmpty(codec.isLeft(key, i++));
      if (i == stopDepth) {
        this.dirty = true;
        ++this.modCount;
        // TODO: is this necessary? the only purpose would be so someone could do == on the key
        // I do not particularly even want to keep the key around,
        // since it can be calculated by our position in the tree...
        // Only keeping around to make the Map interface easier to deal with...
        subNode.key = key;
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
    final Node<K, V> p = getNode((K) key);
    if (p == null) {
      return null;
    }

    final V oldValue = p.value;
    deleteNode(p, false);
    return oldValue;
  }

  protected void deleteNode(final Node<K, V> node, final boolean deleteChildren) {
    if (node == null) {
      return;
    }

    node.value = null;

    if (deleteChildren) {
      node.left = null;
      node.right = null;
    }

    Node<K, V> previousParent = node;
    Node<K, V> currentParent = node.parent;
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

  @Override
  public boolean containsValue(final Object value) {
    if (value == null) {
      return false;
    }
    for (Node<K, V> e = root; e != null; e = successor(e, false)) {
      if (eq(value, e.value)) {
        return true;
      }
    }
    return false;
  }

  protected Node<K, V> getLastNode() {
    Node<K, V> parent = root;
    while (parent.right != null || parent.left != null) {
      if (parent.right != null) {
        parent = parent.right;
      } else {
        parent = parent.left;
      }
    }
    return parent;
  }

  /**
   * Returns the successor of the specified Node Entry, or null if no such.
   */
  protected static <K, V> Node<K, V> successor(final Node<K, V> node,
      final boolean canBeEmpty) {
    // TODO: convert to loop instead of recursive

    if (node == null) {
      return null;
    }

    if (node.left != null) {
      if (node.left.value == null && !canBeEmpty) {
        return successor(node.left, canBeEmpty);
      }
      return node.left;
    }

    if (node.right != null) {
      if (node.right.value == null && !canBeEmpty) {
        return successor(node.right, canBeEmpty);
      }
      return node.right;
    }

    Node<K, V> previousParent = node;
    Node<K, V> currentParent = node.parent;
    while (currentParent != null) {

      if (currentParent.left == previousParent && currentParent.right != null) {

        if (currentParent.right.value == null && !canBeEmpty) {
          return successor(currentParent.right, canBeEmpty);
        }
        return currentParent.right;
      }
      previousParent = currentParent;
      currentParent = currentParent.parent;
    }
    return null;
  }

  /**
   * Returns the predecessor of the specified Node Entry, or null if no such.
   */
  protected static <K, V> Node<K, V> predecessor(final Node<K, V> node,
      final boolean canBeEmpty) {
    // TODO: convert to loop instead of recursive

    if (node == null || node.parent == null) {
      return null;
    }

    // we are on the left, or we have no left sibling, so go up
    if (node == node.parent.left
        || node.parent.left == null) {

      if (node.parent.value == null && !canBeEmpty) {
        return predecessor(node.parent, canBeEmpty);
      }
      return node.parent;
    }

    // we are on the right and have a left sibling
    // so explore the left sibling, all the way down to the right-most child
    Node<K, V> child = node.parent.left;
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

  @Override
  public Set<Map.Entry<K, V>> entrySet() {
    final TrieEntrySet es = entrySet;
    return (es != null) ? es : (entrySet = new TrieEntrySet());
  }

  @Override
  public Set<K> keySet() {
    // TODO: optimize this for our Trie, then delete (currently copied from AbstractMap)
    if (keySet == null) {
      keySet = new AbstractSet<K>() {
        @Override
        public final Iterator<K> iterator() {
          return new Iterator<K>() {
            private final Iterator<Entry<K, V>> i = entrySet().iterator();

            @Override
            public final boolean hasNext() {
              return i.hasNext();
            }

            @Override
            public final K next() {
              return i.next().getKey();
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
        public final boolean contains(final Object k) {
          return AbstractBinaryTrie.this.containsKey(k);
        }
      };
    }
    return keySet;
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
      final Node<K, V> p = getNode(entry.getKey());
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
      super(successor(root, false));
    }

    @Override
    public final Map.Entry<K, V> next() {
      return nextEntry();
    }
  }



  protected abstract class PrivateEntryIterator<T> implements Iterator<T> {
    protected Node<K, V> next;
    protected Node<K, V> lastReturned;
    protected int expectedModCount;

    protected PrivateEntryIterator(final Node<K, V> first) {
      expectedModCount = modCount;
      lastReturned = null;
      next = first;
    }

    @Override
    public final boolean hasNext() {
      return next != null;
    }

    protected final Entry<K, V> nextEntry() {
      final Node<K, V> e = next;
      if (e == null) {
        throw new NoSuchElementException();
      }
      if (modCount != expectedModCount) {
        throw new ConcurrentModificationException();
      }
      next = successor(e, false);
      lastReturned = e;
      return e;
    }

    protected final Entry<K, V> prevEntry() {
      final Node<K, V> e = next;
      if (e == null) {
        throw new NoSuchElementException();
      }
      if (modCount != expectedModCount) {
        throw new ConcurrentModificationException();
      }
      next = predecessor(e, false);
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

}
