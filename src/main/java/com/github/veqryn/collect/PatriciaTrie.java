/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.github.veqryn.collect;

import java.io.Serializable;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.BitSet;
import java.util.Map;

/**
 * Implementation of a PATRICIA Trie (Practical Algorithm To Retrieve
 * Information Coded In Alphanumeric).
 * Can handle international/unicode/utf characters.
 *
 * <p>
 * A PATRICIA Trie is a {@link Trie} specifically for storing String data.
 * This allows for quickly finding the values of Strings that are prefixes of
 * or prefixed by any other given String.
 *
 * <p>
 * This Trie implementation extends {@link AbstractBinaryTrie},
 * an uncompressed binary bitwise implementation of a Trie for use with short
 * binary data such as IP addresses and CIDR ranges, and therefore this
 * implementation will take up more memory space than a trie implementation
 * devoted to use only with String data, such as a compressed Ternary Trie.
 * This PATRICIA Trie implementation mostly exists for use in case a more
 * specific implementation can not be found, and as a proof of concept that
 * the {@link AbstractBinaryTrie} this is based on is abstract and extensible
 * enough for use with any form of data. Also exists so that the Trie classes
 * can be tested using Apache Commons Collections 4 test suites and the Google
 * Guava test suites for String Maps
 * (we can not use this test on the Cidr Trie).
 *
 * @author Mark Christopher Duncan
 *
 * @param <V> Value
 */
public final class PatriciaTrie<V> extends AbstractBinaryTrie<String, V> {

  private static final long serialVersionUID = -6067883352977753038L;


  /**
   * Create an empty {@link PatriciaTrie}.
   */
  public PatriciaTrie() {
    super(new PatriciaCodec());
  }

  /**
   * Create a {@link PatriciaTrie}.
   * The trie will be filled with the keys and values in the provided map.
   *
   * @param otherMap Map of strings and values, which will be {@link #putAll}
   *        into the newly created trie
   */
  public PatriciaTrie(final Map<String, V> otherMap) {
    super(new PatriciaCodec(), otherMap);
  }

  /**
   * Copy constructor, creates a shallow copy of this
   * {@link PatriciaTrie} instance.
   * (The keys and values themselves are not copied.)
   *
   * @param otherTrie PatriciaTrie
   */
  public PatriciaTrie(final PatriciaTrie<V> otherTrie) {
    super(otherTrie);
  }



  /**
   * Implementation of {@link KeyCodec} for use with String data.
   * Specifically for use with {@link AbstractBinaryTrie},
   * because it decodes strings by each bit, instead of each
   * character, as the backing trie is binary in nature.
   * Can handle international characters.
   */
  public static final class PatriciaCodec extends AbstractKeyCodec<String>
      implements KeyCodec<String>, Serializable {

    private static final long serialVersionUID = -3361216681617901600L;

    protected static final Charset CHARSET = StandardCharsets.UTF_16BE;
    protected static final int CHARSET_BIT_LENGTH = 16;

    @Override
    public final int length(final String key) {
      return key.length() * CHARSET_BIT_LENGTH;
    }

    @Override
    public final boolean isLeft(final String key, final int index) {
      final byte[] bytes = key.getBytes(CHARSET);
      // Get the index of the array for the byte with this bit
      final int bitIndex = index / 8;
      // Position of this bit in a byte
      final int bitPosition = index % 8 + 1;
      if (bitIndex >= bytes.length) {
        return true;
      }
      return (bytes[bitIndex] >> (8 - bitPosition) & 1) == 0;
    }

    @Override
    public final String recreateKey(final BitSet bits, final int numElements) {
      return new String(toByteArray(bits, numElements), CHARSET);
    }

  }

}
