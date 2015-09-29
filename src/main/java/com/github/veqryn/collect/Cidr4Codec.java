/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.github.veqryn.collect;

import com.github.veqryn.net.Cidr4;

/**
 * Cidr4Codec
 *
 * @author Mark Christopher Duncan
 */
public class Cidr4Codec implements KeyCodec<Cidr4> {

  private static final long serialVersionUID = 5349501966718289752L;

  @Override
  public int bitsPerElement() {
    return 1;
  }

  @Override
  public int maxLength() {
    return 32;
  }

  @Override
  public int length(final Cidr4 cidr) {
    return cidr.getMaskBits();
  }

  @Override
  public int bitLength(final Cidr4 cidr) {
    return cidr.getMaskBits();
  }

  @Override
  public boolean isLeft(final Cidr4 cidr, final int index) {
    return (cidr.getLowBinaryInteger(true) & (1 << (31 - index))) == 0;
  }

}
