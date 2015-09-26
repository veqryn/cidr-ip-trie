/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.github.veqryn.collect;

import java.io.Serializable;
import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

import com.github.veqryn.net.Cidr4;
import com.github.veqryn.net.Ips;


public class AbstractBinaryTrie<V> extends AbstractMap<Cidr4, V>
    implements Map<Cidr4, V>, Serializable {
  // TODO: maybe implement NavigableMap
  // TODO: maybe implement gauva Multimap or SortedSetMultimap

  private static final long serialVersionUID = 4494549156276631388L;

  private final int depth = 32;
  private final Node<V> root = new Node<>(null);

  protected volatile transient long size = 0;
  protected volatile transient boolean dirty = false;
  protected volatile transient int modCount = 0;

  protected transient EntrySet entrySet = null;

  public AbstractBinaryTrie() {}

  protected final Node<V> getRoot() {
    return this.root;
  }

  public final int getDepth() {
    return this.depth;
  }

  protected static final class Node<V> implements Map.Entry<Cidr4, V>, Serializable {

    private static final long serialVersionUID = 5552867613461961370L;

    protected V value = null;
    protected Node<V> left = null;
    protected Node<V> right = null;
    protected final Node<V> parent;

    protected Node(final Node<V> parent) {
      this.parent = parent;
    }

    protected final Node<V> getChild(final byte b) {
      return b == 0 ? left : right;
    }

    protected final Node<V> getOrCreateChild(final byte b) {
      if (b == 0) {
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
     * @return true if this Entry Node has no value and no child nodes
     */
    protected final boolean isEmpty() {
      return left == null && right == null && value == null;
    }

    /**
     * @return the Cidr key for this node and value
     */
    @Override
    public final Cidr4 getKey() {
      if (this.parent == null) {
        return null;
      }
      int binary = 0;
      int levelsDeep = 0;
      Node<V> previousParent = this;
      Node<V> currentParent = this.parent;
      while (currentParent != null) {
        if (currentParent.left == previousParent) {
          binary = binary >>> 1;
        } else {
          binary = (binary >>> 1) | Integer.MIN_VALUE;
        }
        previousParent = currentParent;
        currentParent = currentParent.parent;
        ++levelsDeep;
      }
      return new Cidr4(binary, levelsDeep);
    }

    /**
     * @return the value associated with the key
     */
    @Override
    public final V getValue() {
      return value;
    }

    /**
     * Replaces the value currently associated with the key with the given
     * value.
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
      final Cidr4 key = getKey();
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

  @Override
  public final void clear() {
    this.root.value = null;
    this.root.left = null;
    this.root.right = null;
    this.size = 0L;
    this.modCount = 0;
    this.dirty = false;
  }

  @Override
  public final boolean isEmpty() {
    return root.isEmpty();
  }

  @Override
  public final int size() {
    if (this.dirty) {
      long total = 0L;
      Node<V> subTree = root;
      while ((subTree = successor(subTree, true)) != null) {
        ++total;
      }
      this.size = total;
      this.dirty = false;
    }
    return size > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) size;
  }

  /**
   * Test two values for equality. Differs from o1.equals(o2) only in
   * that it copes with {@code null} o1 properly.
   */
  protected static final boolean eq(final Object o1, final Object o2) {
    return (o1 == null ? o2 == null : o1.equals(o2));
  }

  @Override
  public final boolean containsKey(final Object key) {
    return get((Cidr4) key, 0, depth) != null;
  }

  protected final boolean containsKey(final Object key, final int startDepth, final int stopDepth) {
    return get((Cidr4) key, startDepth, stopDepth) != null;
  }

  @Override
  public final V get(final Object key) {
    return get((Cidr4) key, 0, depth);
  }

  protected final V get(final Cidr4 key, final int startDepth, final int stopDepth) {
    if (key == null) {
      return null;
    }
    return get(key.getLowBinaryInteger(true), startDepth, stopDepth);
  }

  protected final V get(final int key, final int startDepth, final int stopDepth) {
    final Node<V> node = getNode(key, startDepth, stopDepth);
    return node == null ? null : node.value;
  }

  protected final Node<V> getExactNode(final Cidr4 key) {
    final int maskBits = key.getMaskBits();
    return getNode(key.getHighBinaryInteger(true), maskBits, maskBits);
  }

  protected final Node<V> getNode(final int key, final int startDepth, final int stopDepth) {
    // Do not branch, even if startDepth < stopDepth, because we are looking up a single IP
    // and getting every CIDR range that contains that IP
    // We are not looking up every value contained by a CIDR (for that iterate through a subMap)
    final int min = Math.min(1, startDepth);
    final int max = Math.min(depth, stopDepth);
    final byte[] bits = Ips.getBits(key, 0, max);
    Node<V> subTree = root;
    int i = 0;
    while (true) {
      final byte bit = bits[i];
      final Node<V> current = subTree.getChild(bit);
      if (current == null) {
        return null;
      }
      ++i;
      if (current.value != null && (i >= min && i <= max)) {
        return current;
      }
      if (i >= max) {
        return null;
      }
      subTree = current;
    }
  }

  public final Set<V> suffixValues(final Cidr4 key) {
    return suffixValues(key, 0, depth);
  }

  protected final Set<V> suffixValues(final Cidr4 key, final int startDepth, final int stopDepth) {
    if (key == null) {
      return new LinkedHashSet<>();
    }
    return suffixValues(key.getLowBinaryInteger(true), startDepth, stopDepth);
  }

  protected final Set<V> suffixValues(final int key, final int startDepth, final int stopDepth) {
    // Do not branch, even if startDepth < stopDepth, because we are looking up a single IP
    // and getting every CIDR range that contains that IP
    // We are not looking up every value contained by a CIDR (for that iterate through a subMap)
    final Set<V> values = new LinkedHashSet<>();
    final int min = Math.min(1, startDepth);
    final int max = Math.min(depth, stopDepth);
    final byte[] bits = Ips.getBits(key, 0, max);
    Node<V> subTree = root;
    int i = 0;
    while (true) {
      final byte bit = bits[i];
      final Node<V> current = subTree.getChild(bit);
      if (current == null) {
        return values;
      }
      ++i;
      if (current.value != null && (i >= min && i <= max)) {
        values.add(current.value);
      }
      if (i == max) {
        return values;
      }
      subTree = current;
    }
  }

  @Override
  public final V put(final Cidr4 key, final V value) {
    if (key == null) {
      throw new IllegalArgumentException(this.getClass().getName() + " does not accept null keys");
    }
    return put(key.getHighBinaryInteger(true), value, key.getMaskBits());
  }

  protected final V put(final int key, final V value, final int stopDepth) {
    if (value == null) {
      throw new IllegalArgumentException(this.getClass().getName() + " does not accept null values");
    }
    final int max = Math.min(depth, stopDepth);
    final byte[] bits = Ips.getBits(key, 0, max);
    Node<V> subTree = root;
    int i = 0;
    while (true) {
      final byte bit = bits[i];
      final Node<V> current = subTree.getOrCreateChild(bit);
      if (++i == max) {
        this.dirty = true;
        ++this.modCount;
        return current.setValue(value);
      }
      subTree = current;
    }
  }

  @Override
  public final void putAll(final Map<? extends Cidr4, ? extends V> map) {
    for (final Map.Entry<? extends Cidr4, ? extends V> entry : map.entrySet()) {
      put(entry.getKey(), entry.getValue());
    }
  }

  protected final void deleteNode(final Node<V> node) {
    if (node == null) {
      return;
    }
    node.value = null;

    Node<V> previousParent = node;
    Node<V> currentParent = node.parent;
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
  public final Set<Map.Entry<Cidr4, V>> entrySet() {
    final EntrySet es = entrySet;
    return (es != null) ? es : (entrySet = new EntrySet());
  }

  /**
   * Returns the successor of the specified Entry, or null if no such.
   */
  protected static final <V> Node<V> successor(final Node<V> node, final boolean mustHaveValue) {
    if (node == null) {
      return null;
    } else if (node.left != null) {
      if (mustHaveValue && node.left.value == null) {
        return successor(node.left, mustHaveValue);
      }
      return node.left;
    } else if (node.right != null) {
      if (mustHaveValue && node.right.value == null) {
        return successor(node.right, mustHaveValue);
      }
      return node.right;
    } else {
      Node<V> previousParent = node;
      Node<V> currentParent = node.parent;
      while (currentParent != null) {
        if (currentParent.left == previousParent && currentParent.right != null) {
          if (mustHaveValue && currentParent.right.value == null) {
            return successor(currentParent.right, mustHaveValue);
          }
          return currentParent.right;
        }
        previousParent = currentParent;
        currentParent = currentParent.parent;
      }
      return null;
    }
  }

  /**
   * Returns the predecessor of the specified Entry, or null if no such.
   */
  protected static final <V> Node<V> predecessor(final Node<V> node, final boolean mustHaveValue) {
    if (node == null) {
      return null;
    }
    Node<V> previousParent = node;
    Node<V> currentParent = node.parent;
    while (currentParent != null) {
      if (currentParent.right == previousParent && currentParent.left != null) {
        if (mustHaveValue && currentParent.left.value == null) {
          return successor(currentParent.left, mustHaveValue);
        }
        return currentParent.left;
      }
      previousParent = currentParent;
      currentParent = currentParent.parent;
    }
    return null;
  }

  protected final class EntrySet extends AbstractSet<Map.Entry<Cidr4, V>> {
    @Override
    public Iterator<Map.Entry<Cidr4, V>> iterator() {
      return new EntryIterator();
    }

    @Override
    public final boolean contains(final Object o) {
      if (!(o instanceof Map.Entry)) {
        return false;
      }
      final Map.Entry<?, ?> entry = (Map.Entry<?, ?>) o;
      final Object value = entry.getValue();
      final Object key = entry.getKey();
      if (!(key instanceof Cidr4)) {
        return false;
      }
      final Node<V> p = getExactNode((Cidr4) key);
      return p != null && eq(p.getValue(), value);
    }

    @Override
    public final boolean remove(final Object o) {
      if (!(o instanceof Map.Entry)) {
        return false;
      }
      final Map.Entry<?, ?> entry = (Map.Entry<?, ?>) o;
      final Object value = entry.getValue();
      final Object key = entry.getKey();
      if (!(key instanceof Cidr4)) {
        return false;
      }
      final Node<V> p = getExactNode((Cidr4) key);
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



  protected final class EntryIterator extends PrivateEntryIterator<Map.Entry<Cidr4, V>> {

    protected EntryIterator() {
      super(successor(root, true));
    }

    @Override
    public final Map.Entry<Cidr4, V> next() {
      return nextEntry();
    }
  }



  protected abstract class PrivateEntryIterator<T> implements Iterator<T> {
    protected Node<V> next;
    protected Node<V> lastReturned;
    protected int expectedModCount;

    protected PrivateEntryIterator(final Node<V> first) {
      expectedModCount = modCount;
      lastReturned = null;
      next = first;
    }

    @Override
    public final boolean hasNext() {
      return next != null;
    }

    protected final Entry<Cidr4, V> nextEntry() {
      final Node<V> e = next;
      if (e == null) {
        throw new NoSuchElementException();
      }
      if (modCount != expectedModCount) {
        throw new ConcurrentModificationException();
      }
      next = successor(e, true);
      lastReturned = e;
      return e;
    }

    protected final Entry<Cidr4, V> prevEntry() {
      final Node<V> e = next;
      if (e == null) {
        throw new NoSuchElementException();
      }
      if (modCount != expectedModCount) {
        throw new ConcurrentModificationException();
      }
      next = predecessor(e, true);
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
      deleteNode(lastReturned);
      expectedModCount = modCount;
      lastReturned = null;
    }
  }



  @Override
  public final AbstractBinaryTrie<V> clone() throws CloneNotSupportedException {
    throw new CloneNotSupportedException("Clone is not supported for: " + this.getClass().getName());
  }

}
