/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.github.veqryn.collect;

import java.util.BitSet;

import com.github.veqryn.net.Cidr4;

/**
 * Cidr4Codec
 *
 * @author Mark Christopher Duncan
 */
public class Cidr4Codec implements KeyCodec<Cidr4> {

  private static final long serialVersionUID = 5349501966718289752L;

  @Override
  public int length(final Cidr4 cidr) {
    return cidr.getMaskBits();
  }

  @Override
  public boolean isLeft(final Cidr4 cidr, final int index) {
    return (cidr.getLowBinaryInteger(true) & (1 << (31 - index))) == 0;
  }

  @Override
  public Cidr4 recreateKey(final BitSet bits, final int numElements) {

    if (bits.length() == 0) {
      return new Cidr4(0, numElements);
    }

    int binary = (int) bits.toLongArray()[0];

    final int move = 32 - numElements;
    if (move > 0) {
      binary = binary << move;
    }

    return new Cidr4(binary, numElements);
  }

}
