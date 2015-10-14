/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.github.veqryn.collect;

import java.io.Serializable;
import java.util.Comparator;

/**
 * AbstractKeyCodec implements KeyCodec interface,
 * for encoding, decoding, and analyzing keys in a Trie.
 * Includes a predefined comparator that is based solely on the
 * abstract <code>length</code> and <code>isLeft</code> methods.
 *
 * @author Mark Christopher Duncan
 *
 * @param <K> Key
 */
public abstract class AbstractKeyCodec<K> implements KeyCodec<K>, Serializable {

  private static final long serialVersionUID = -3361215683005193832L;

  @Override
  public Comparator<? super K> comparator() {
    return comparator;
  }

  protected final Comparator<? super K> comparator =
      new Comparator<K>() {
        @Override
        public int compare(final K o1, final K o2) {
          if (o1 == null || o2 == null) {
            throw new IllegalArgumentException("Null keys not allowed");
          }
          if (o1 == o2 || o1.equals(o2)) {
            return 0;
          }
          final int l1 = AbstractKeyCodec.this.length(o1);
          final int l2 = AbstractKeyCodec.this.length(o2);
          final int min = Math.min(l1, l2);
          boolean left1;
          boolean left2;
          for (int i = 0; i < min; ++i) {
            left1 = AbstractKeyCodec.this.isLeft(o1, i);
            left2 = AbstractKeyCodec.this.isLeft(o2, i);
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
