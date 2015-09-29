/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.github.veqryn.collect;

import java.io.Serializable;
import java.util.Map;

/**
 * PatriciaTrie
 * (Practical Algorithm To Retrieve Information Coded In Alphanumeric)
 *
 * @author Mark Christopher Duncan
 */
public final class PatriciaTrie<V> extends AbstractBinaryTrie<String, V>
    implements Map<String, V>, Serializable {

  private static final long serialVersionUID = -6067883352977753038L;

  public PatriciaTrie() {
    super(new PatriciaCodec());
  }

}
