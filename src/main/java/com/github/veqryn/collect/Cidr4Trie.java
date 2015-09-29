/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.github.veqryn.collect;

import com.github.veqryn.net.Cidr4;

/**
 * CidrTrie
 *
 * @author Mark Christopher Duncan
 */
public final class Cidr4Trie<V> extends AbstractBinaryTrie<Cidr4, V> {

  private static final long serialVersionUID = -8113898642923790939L;

  public Cidr4Trie() {
    super(new Cidr4Codec());
  }

}
