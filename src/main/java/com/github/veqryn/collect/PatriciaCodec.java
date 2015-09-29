/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.github.veqryn.collect;

import java.io.Serializable;
import java.nio.charset.Charset;
import java.util.BitSet;

/**
 * PatriciaCodec, for use with the AbstractBinaryTrie.
 * (Practical Algorithm To Retrieve Information Coded In Alphanumeric)
 *
 * @author Mark Christopher Duncan
 */
public final class PatriciaCodec implements KeyCodec<String>, Serializable {

  private static final long serialVersionUID = -3361216681617901600L;

  protected static final int BITS_IN_BYTE = 8;
  protected static final Charset CHARSET = Charset.forName("UTF-16BE");
  protected static final int BIT_LENGTH = 16;

  @Override
  public final int length(final String key) {
    return key.length() * BIT_LENGTH;
  }

  @Override
  public final boolean isLeft(final String key, final int index) {
    final byte[] bytes = key.getBytes(CHARSET);
    // Get the index of the array for the byte with this bit
    final int bitIndex = index / BITS_IN_BYTE;
    // Position of this bit in a byte
    final int bitPosition = index % BITS_IN_BYTE;
    if (bitIndex >= bytes.length) {
      return true;
    }
    return (bytes[bitIndex] >> bitPosition & 1) == 0;
  }

  @Override
  public final String recreateKey(final BitSet bits, final int numElements) {

    if (bits.length() == 0) {
      return "";
    }
    // Bits come in the wrong order for what we are doing
    final BitSet reversed = new BitSet();
    for (int i = 0, j = numElements - 1; i < numElements; ++i, --j) {
      reversed.set(j, bits.get(i));
    }

    final byte[] bytes = reversed.toByteArray();
    return new String(bytes, CHARSET);
  }

}
