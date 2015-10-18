/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.github.veqryn.collect;

import java.util.NavigableMap;

/**
 *
 *
 * @author Mark Christopher Duncan
 *
 * @param <K> Key
 * @param <V> Value
 */
public interface NavigableTrie<K, V> extends Trie<K, V>, NavigableMap<K, V> {

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
