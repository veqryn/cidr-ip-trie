/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.github.veqryn.net;

import static com.github.veqryn.net.Ips.IP_ADDRESS;
import static com.github.veqryn.net.Ips.toInteger;

import java.util.regex.Pattern;

/**
 * Utility class containing methods for operating on Cidrs
 *
 * @author Chris Duncan
 */
final class Cidrs {

  protected static final String SLASH_FORMAT = IP_ADDRESS + "/(\\d{1,3})";
  protected static final Pattern cidrPattern = Pattern.compile(SLASH_FORMAT);
  protected static final int NBITS = 32;

  private Cidrs() {}

  /**
   * @param low Low value of CIDR range
   * @param high High value of CIDR range
   * @param maskBits the maximum number of bits in the netmask (e.g. 32 - 1)
   * @param binary false if using a sortable packed integer,
   *        where Integer.MIN_VALUE = 0.0.0.0
   *        and 0 = 128.0.0.0
   *        and Integer.MAX_VALUE = 255.255.255.255</br>
   *        true if using a binary integer,
   *        where Integer.MIN_VALUE = 128.0.0.0
   *        and 0 = 0.0.0.0
   *        and Integer.MAX_VALUE = 127.255.255.255
   *        and -1 = 255.255.255.255
   * @return A new Cidr representing the lowest cidr that has
   *         no more than this many maskBits and contains our cidr range, e.g.:</br>
   *         192.168.10.10/31 => mask 24 => 192.168.10.0/24</br>
   *         192.168.10.10/31 => mask 24 => 192.168.0.0/16</br>
   *         192.168.10.10/31 => mask 24 => 192.0.0.0/8</br>
   *         192.168.10.10/31 => mask 32 => 192.168.10.10/31
   */
  protected static final Cidr4 getLowestContainingCidrForRange(final int low, final int high,
      final int maskBits, final boolean binary) {
    final int netmask = getNetMask(maskBits);
    final int network = binary ? low : low ^ Integer.MIN_VALUE;
    final int newLow = getLowestBinaryWithNetmask(network, netmask) ^ Integer.MIN_VALUE;
    final int newHigh = Math.max(binary ? high ^ Integer.MIN_VALUE : high,
        getHighestBinaryWithNetmask(network, netmask) ^ Integer.MIN_VALUE);
    return new Cidr4(newLow, newHigh, false);
  }

  /**
   * @param low IPv4 address in binary form as an integer</br>
   *        255.255.255.255 => 11111111.11111111.11111111.11111111 binary</br>
   *        0.0.0.0 => 00000000.00000000.00000000.00000000 binary
   * @param high IPv4 address in binary form as an integer</br>
   *        255.255.255.255 => 11111111.11111111.11111111.11111111 binary</br>
   *        0.0.0.0 => 00000000.00000000.00000000.00000000 binary
   * @return binary integer netmask
   */
  protected static final int getDifferenceNetmask(final int low, final int high) {
    int diff = low ^ high;
    diff |= diff >> 1;
    diff |= diff >> 2;
    diff |= diff >> 4;
    diff |= diff >> 8;
    diff |= diff >> 16;
    return ~diff;
  }

  /**
   * @param binaryIntegerAddress IPv4 address in binary form as an integer</br>
   *        255.255.255.255 => 11111111111111111111111111111111 binary</br>
   *        0.0.0.0 => 00000000000000000000000000000000 binary
   * @param netmask binary netmask
   * @return the binary integer after the netmask is applied
   */
  protected static final int getLowestBinaryWithNetmask(final int binaryIntegerAddress,
      final int netmask) {
    return binaryIntegerAddress & netmask;
  }

  /**
   * @param binaryIntegerAddress IPv4 address in binary form as an integer</br>
   *        255.255.255.255 => 11111111.11111111.11111111.11111111 binary</br>
   *        0.0.0.0 => 00000000.00000000.00000000.00000000 binary
   * @param netmask binary netmask
   * @return the largest binary integer allowed by this netmask
   */
  protected static final int getHighestBinaryWithNetmask(final int binaryIntegerAddress,
      final int netmask) {
    return binaryIntegerAddress | ~(netmask);
  }

  /**
   * @param cidrRangePart e.g. 32, 24, 16, etc
   * @return a binary netmask for IPv4 from the number of bits specification /x
   */
  protected static final int getNetMask(final int cidrRangePart) {
    int netmask = 0;
    for (int j = 0; j < cidrRangePart; ++j) {
      netmask |= (1 << 31 - j);
    }
    return netmask;
  }

  /**
   * Get the number of the mask bits /n
   *
   * @param netmask binary netmask
   * @return Cidr range in number of mask bits (1-32)
   */
  protected static final int getMaskBitCount(final int netmask) {
    return 32 - Integer.bitCount(~netmask);
  }

  /**
   * Convert two dotted decimal addresses to a single xxx.xxx.xxx.xxx/yy format
   * by counting the 1-bit population in the mask address.
   * (It may be better to count NBITS-#trailing zeroes for this case)
   *
   * @param address IPv4 address in string dotted decimal format
   * @param mask IPv4 net mask in dotted decimal format
   * @return CIDR string (e.g. "192.168.0.1/32")
   */
  protected static final String toCidrNotation(final String address, final String mask) {
    return address + "/" + Integer.bitCount(toInteger(mask, true));
  }

}
