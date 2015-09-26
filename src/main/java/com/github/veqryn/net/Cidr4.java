/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.github.veqryn.net;

import static com.github.veqryn.net.Cidrs.NBITS;
import static com.github.veqryn.net.Cidrs.cidrPattern;
import static com.github.veqryn.net.Cidrs.getHighestBinaryWithNetmask;
import static com.github.veqryn.net.Cidrs.getLowestBinaryWithNetmask;
import static com.github.veqryn.net.Cidrs.getLowestContainingCidrForRange;
import static com.github.veqryn.net.Cidrs.getMaskBitCount;
import static com.github.veqryn.net.Cidrs.getNetMask;
import static com.github.veqryn.net.Cidrs.toCidrNotation;
import static com.github.veqryn.net.Ips.format;
import static com.github.veqryn.net.Ips.matchAddress;
import static com.github.veqryn.net.Ips.rangeCheck;
import static com.github.veqryn.net.Ips.toArray;
import static com.github.veqryn.net.Ips.toInteger;

import java.io.Serializable;
import java.util.regex.Matcher;

/**
 * Light weight immutable CIDR IPv4 type, which implements hashCode, equals, and Comparable.
 * Some methods and method signatures influenced by org.apache.commons.net.util.SubnetUtils
 */
public final class Cidr4 implements Comparable<Cidr4>, Serializable {

  private static final long serialVersionUID = -5580964083176413038L;

  private final int low;
  private final int high;

  /**
   * Constructor that takes a CIDR-notation string, e.g. "192.168.0.1/16"
   *
   * @param cidrNotation A CIDR-notation string, e.g. "192.168.0.1/16"
   * @throws IllegalArgumentException if the parameter is invalid,
   *         i.e. does not match n.n.n.n/m where n=1-3 decimal digits,
   *         m = 1-3 decimal digits in range 1-32
   */
  public Cidr4(final String cidrNotation) {
    final Matcher matcher = cidrPattern.matcher(cidrNotation);

    if (matcher.matches()) {
      final int address = toInteger(matchAddress(matcher), true);
      final int netmask = getNetMask(rangeCheck(Integer.parseInt(matcher.group(5)), 0, NBITS));

      final int network = getLowestBinaryWithNetmask(address, netmask);
      this.low = network ^ Integer.MIN_VALUE;
      this.high = getHighestBinaryWithNetmask(network, netmask) ^ Integer.MIN_VALUE;

    } else {
      throw new IllegalArgumentException("Could not parse [" + cidrNotation + "]");
    }
  }

  /**
   * Constructor that takes a CIDR-notation string, e.g. "192.168.0.1/16"
   * Or an IP dotted decimal format address, e.g. "192.168.0.1"
   *
   * @param cidrNotation A CIDR-notation string, e.g. "192.168.0.1/16"
   *        ("192.168.0.1" accepted as if it was /32, if acceptAddressWithoutRange true)
   * @param acceptAddressWithoutRange true if n.n.n.n should be accepted
   * @throws IllegalArgumentException if the parameter is invalid,
   *         i.e. does not match n.n.n.n/m where n=1-3 decimal digits,
   *         m = 1-3 decimal digits in range 1-32
   *         (if acceptAddressWithoutRange then n.n.n.n is also accepted)
   */
  public Cidr4(final String cidrNotation, final boolean acceptAddressWithoutRange) {
    this((acceptAddressWithoutRange && !cidrNotation.contains("/"))
        ? cidrNotation + "/32" : cidrNotation);
  }

  /**
   * Constructor that takes a dotted decimal address and a dotted decimal mask.
   *
   * @param address An IP address, e.g. "192.168.0.1"
   * @param mask A dotted decimal netmask e.g. "255.255.0.0"
   * @throws IllegalArgumentException if the address or mask is invalid,
   *         i.e. does not match n.n.n.n where n=1-3 decimal digits and the mask is not all zeros
   */
  public Cidr4(final String address, final String mask) {
    this(toCidrNotation(address, mask));
  }

  /**
   * Constructor that takes a integer value for address and the
   * number of mask bits for the cidr range
   *
   * @param address Low value of CIDR range,
   *        where Integer.MIN_VALUE = 128.0.0.0
   *        and 0 = 0.0.0.0
   *        and Integer.MAX_VALUE = 127.255.255.255
   *        and -1 = 255.255.255.255
   * @param maskBits e.g. 32
   */
  public Cidr4(final int address, final int maskBits) {
    this(address, true, maskBits);
  }

  /**
   * Constructor that takes a integer value for address and the
   * number of mask bits for the cidr range
   *
   * @param address Low value of CIDR range
   * @param binary false if using a sortable packed integer,
   *        where Integer.MIN_VALUE = 0.0.0.0
   *        and 0 = 128.0.0.0
   *        and Integer.MAX_VALUE = 255.255.255.255</br>
   *        true if using a binary integer,
   *        where Integer.MIN_VALUE = 128.0.0.0
   *        and 0 = 0.0.0.0
   *        and Integer.MAX_VALUE = 127.255.255.255
   *        and -1 = 255.255.255.255
   * @param maskBits e.g. 32
   */
  protected Cidr4(int address, final boolean binary, final int maskBits) {
    address = binary ? address : address ^ Integer.MIN_VALUE;
    final int netmask = getNetMask(rangeCheck(maskBits, 0, NBITS));
    final int network = getLowestBinaryWithNetmask(address, netmask);
    this.low = network ^ Integer.MIN_VALUE;
    this.high = getHighestBinaryWithNetmask(network, netmask) ^ Integer.MIN_VALUE;
  }

  /**
   * Constructor that makes a copy of the provided Cidr
   *
   * @param cidr Cidr
   */
  public Cidr4(final Cidr4 cidr) {
    this.low = cidr.low;
    this.high = cidr.high;
  }

  /**
   * Constructor that takes a single Ip value for a CIDR with range /32
   *
   * @param address Ip of the single address this CIDR represents
   */
  public Cidr4(final Ip4 address) {
    this.low = address.getSortableInteger();
    this.high = address.getSortableInteger();
  }

  /**
   * Constructor that takes the low and high Ip values of the CIDR range.
   *
   * @param low Low Ip of CIDR range
   * @param high High Ip of CIDR range
   */
  public Cidr4(final Ip4 low, final Ip4 high) {
    this(low.getSortableInteger(), high.getSortableInteger(), false);
  }

  /**
   * Constructor that takes the low and high integer values of the CIDR range.
   * Where Integer.MIN_VALUE = 0.0.0.0
   * and 0 = 128.0.0.0
   * and Integer.MAX_VALUE = 255.255.255.255
   *
   * @param low Low value of CIDR range
   * @param high High value of CIDR range
   * @param binary false if using a sortable packed integer,
   *        where Integer.MIN_VALUE = 0.0.0.0
   *        and 0 = 128.0.0.0
   *        and Integer.MAX_VALUE = 255.255.255.255</br>
   *        true if using a binary integer,
   *        where Integer.MIN_VALUE = 128.0.0.0
   *        and 0 = 0.0.0.0
   *        and Integer.MAX_VALUE = 127.255.255.255
   *        and -1 = 255.255.255.255
   */
  protected Cidr4(final int low, final int high, final boolean binary) {
    final int newlow = binary ? low ^ Integer.MIN_VALUE : low;
    final int newHigh = binary ? high ^ Integer.MIN_VALUE : high;
    if (newlow > newHigh) {
      throw new IllegalArgumentException("Low IP integer value must be <= High value");
    }
    this.low = newlow;
    this.high = newHigh;
  }

  /**
   * Constructor that takes individual unsigned int octets, and the cidr range / mask bit count
   *
   * @param octet1 e.g. 192
   * @param octet2 e.g. 168
   * @param octet3 e.g. 0
   * @param octet4 e.g. 1
   * @param maskBits e.g. 32
   */
  public Cidr4(final int octet1, final int octet2,
      final int octet3, final int octet4, final int maskBits) {

    final int address = toInteger(new int[] {octet1, octet2, octet3, octet4}, true);
    final int netmask = getNetMask(rangeCheck(maskBits, 0, NBITS));

    final int network = getLowestBinaryWithNetmask(address, netmask);
    this.low = network ^ Integer.MIN_VALUE;
    this.high = getHighestBinaryWithNetmask(network, netmask) ^ Integer.MIN_VALUE;
  }



  /**
   * @param hostCountInclusive whether to include the network and broadcast addresses
   * @return the Lowest IP address in dotted format, may be "0.0.0.0" if there is no valid address
   */
  public final String getLowAddress(final boolean hostCountInclusive) {
    return format(toArray(getLowSortableInteger(hostCountInclusive), false));
  }

  /**
   * @param hostCountInclusive whether to include the network and broadcast addresses
   * @return a packed integer equal to the lowest ip in the CIDR range,
   *         where Integer.MIN_VALUE = 0.0.0.0 (returned if there is no valid address)
   *         and 0 = 128.0.0.0
   *         and Integer.MAX_VALUE = 255.255.255.255
   */
  protected final int getLowSortableInteger(final boolean hostCountInclusive) {
    if (hostCountInclusive) {
      return low;
    }
    if (this.high > this.low + 1) {
      return low + 1;
    }
    return Integer.MIN_VALUE;
  }

  /**
   * @param hostCountInclusive whether to include the network and broadcast addresses
   * @return a binary integer equal to the lowest ip in the CIDR range,
   *         where Integer.MIN_VALUE = 128.0.0.0
   *         and 0 = 0.0.0.0 (returned if there is no valid address)
   *         and Integer.MAX_VALUE = 127.255.255.255
   *         and -1 = 255.255.255.255
   */
  public final int getLowBinaryInteger(final boolean hostCountInclusive) {
    if (hostCountInclusive) {
      return low ^ Integer.MIN_VALUE;
    }
    if (this.high > this.low + 1) {
      return (low + 1) ^ Integer.MIN_VALUE;
    }
    return 0;
  }

  /**
   * @param hostCountInclusive whether to include the network and broadcast addresses
   * @return a Ip equal to the lowest IP value in the CIDR range,
   *         may be "0.0.0.0" if there is no valid address
   */
  public final Ip4 getLowIp(final boolean hostCountInclusive) {
    return new Ip4(getLowSortableInteger(hostCountInclusive), false);
  }

  /**
   * @param hostCountInclusive whether to include the network and broadcast addresses
   * @return a Cidr equal to the lowest IP value in the CIDR range,
   *         may be "0.0.0.0" if there is no valid address
   */
  public final Cidr4 getLowCidr(final boolean hostCountInclusive) {
    final int lowest = getLowSortableInteger(hostCountInclusive);
    return new Cidr4(lowest, lowest, false);
  }

  /**
   * @param maskBits the maximum number of bits in the netmask (e.g. 32 - 1)
   * @return A new Cidr representing the lowest cidr that has
   *         no more than this many maskBits and contains our cidr range
   *         e.g. 192.168.10.10/31 => mask 24 => 192.168.0.0/24
   */
  public final Cidr4 getLowestContainingCidr(final int maskBits) {
    return getLowestContainingCidrForRange(this.low, this.high, maskBits, false);
  }

  /**
   * @param hostCountInclusive whether to include the network and broadcast addresses
   * @return the Highest IP address in dotted format, may be "0.0.0.0" if there is no valid address
   */
  public final String getHighAddress(final boolean hostCountInclusive) {
    return format(toArray(getHighSortableInteger(hostCountInclusive), false));
  }

  /**
   * @param hostCountInclusive whether to include the network and broadcast addresses
   * @return a packed integer equal to the highest ip in the CIDR range,
   *         where Integer.MIN_VALUE = 0.0.0.0 (returned if there is no valid address)
   *         and 0 = 128.0.0.0
   *         and Integer.MAX_VALUE = 255.255.255.255
   */
  protected final int getHighSortableInteger(final boolean hostCountInclusive) {
    if (hostCountInclusive) {
      return high;
    }
    if (this.high > this.low + 1) {
      return high - 1;
    }
    return Integer.MIN_VALUE;
  }

  /**
   * @param hostCountInclusive whether to include the network and broadcast addresses
   * @return a binary integer equal to the highest ip in the CIDR range,
   *         where Integer.MIN_VALUE = 128.0.0.0
   *         and 0 = 0.0.0.0 (returned if there is no valid address)
   *         and Integer.MAX_VALUE = 127.255.255.255
   *         and -1 = 255.255.255.255
   */
  public final int getHighBinaryInteger(final boolean hostCountInclusive) {
    if (hostCountInclusive) {
      return high ^ Integer.MIN_VALUE;
    }
    if (this.high > this.low + 1) {
      return (high - 1) ^ Integer.MIN_VALUE;
    }
    return 0;
  }

  /**
   * @param hostCountInclusive whether to include the network and broadcast addresses
   * @return a Ip equal to the highest IP value in the CIDR range,
   *         may be "0.0.0.0" if there is no valid address
   */
  public final Ip4 getHighIp(final boolean hostCountInclusive) {
    return new Ip4(getHighSortableInteger(hostCountInclusive), false);
  }

  /**
   * @param hostCountInclusive whether to include the network and broadcast addresses
   * @return a Cidr equal to the highest IP value in the CIDR range,
   *         may be "0.0.0.0" if there is no valid address
   */
  public final Cidr4 getHighCidr(final boolean hostCountInclusive) {
    final int highest = getHighSortableInteger(hostCountInclusive);
    return new Cidr4(highest, highest, false);
  }

  /**
   * Get the netmask used for this Cidr in binary integer format
   *
   * @return binary integer netmask
   */
  public final int getBinaryNetmask() {
    return ~(low ^ high);
  }

  /**
   * Get the netmask used for this Cidr in dotted decimal format
   *
   * @return netmask in dotted decimal format (e.g. "255.255.255.0")
   */
  public final String getNetmask() {
    return format(toArray(getBinaryNetmask(), true));
  }

  /**
   * Get the number of mask bits for the Cidr range
   *
   * @return mask bit count (e.g. "192.168.0.0/16" would return 16)
   */
  public final int getMaskBits() {
    return getMaskBitCount(getBinaryNetmask());
  }

  /**
   * @return CIDR string (e.g. "192.168.0.1/32")
   */
  public final String getCidrSignature() {
    return getLowAddress(true) + '/' + getMaskBits();
  }

  /**
   * Get the number of addresses included in this Cidr range
   *
   * @param hostCountInclusive whether to include the network and broadcast addresses
   * @return the number of addresses in this range. could be zero if hostCountInclusive is false
   */
  public final int getAddressCount(final boolean hostCountInclusive) {
    return Math.max(0, high - low + (hostCountInclusive ? 1 : -1));
  }

  /**
   * Get all IP addresses in this range
   *
   * @param hostCountInclusive whether to include the network and broadcast addresses
   * @return an array of all IP addresses in this Cidr range in dot-delimited IPv4 format,
   *         could be empty if hostCountInclusive is false
   */
  public final String[] getAllAddresses(final boolean hostCountInclusive) {
    final int count = getAddressCount(hostCountInclusive);
    final String[] addresses = new String[count];
    if (count == 0) {
      return addresses;
    }
    final int max = getHighSortableInteger(hostCountInclusive);
    for (int j = 0, add = getLowSortableInteger(hostCountInclusive); add <= max; ++j, ++add) {
      addresses[j] = format(toArray(add, false));
    }
    return addresses;
  }

  /**
   * Get all IP addresses in this range
   *
   * @param hostCountInclusive whether to include the network and broadcast addresses
   * @return a Ip array of all IP addresses in this Cidr range,
   *         could be empty if hostCountInclusive is false
   */
  public final Ip4[] getAllIps(final boolean hostCountInclusive) {
    final int count = getAddressCount(hostCountInclusive);
    final Ip4[] addresses = new Ip4[count];
    if (count == 0) {
      return addresses;
    }
    final int max = getHighSortableInteger(hostCountInclusive);
    for (int j = 0, add = getLowSortableInteger(hostCountInclusive); add <= max; ++j, ++add) {
      addresses[j] = new Ip4(add, false);
    }
    return addresses;
  }

  /**
   * Check if the parameter <code>address</code> is within
   * the range of our CIDR, inclusive
   *
   * @param address A dot-delimited IPv4 address, e.g. "192.168.0.1"
   * @param hostCountInclusive whether to include the network and broadcast addresses
   * @return true if the passed address is contained within our range
   */
  public final boolean isInRange(final String address, final boolean hostCountInclusive) {
    return isInRange(toInteger(address, false), hostCountInclusive);
  }

  /**
   * Check if the parameter <code>address</code> is within
   * the range of our CIDR, inclusive
   *
   * @param address Ip
   * @param hostCountInclusive whether to include the network and broadcast addresses
   * @return true if the passed address is contained within of our range
   */
  public final boolean isInRange(final Ip4 address, final boolean hostCountInclusive) {
    return isInRange(address.getSortableInteger(), hostCountInclusive);
  }

  /**
   * Check if the parameter <code>address</code> is within
   * the range of our CIDR, inclusive
   *
   * @param address integer IPv4 value,
   *        where Integer.MIN_VALUE = 0.0.0.0
   *        and 0 = 128.0.0.0
   *        and Integer.MAX_VALUE = 255.255.255.255
   * @param hostCountInclusive whether to include the network and broadcast addresses
   * @return true if the passed address is contained within of our range
   */
  protected final boolean isInRange(final int address, final boolean hostCountInclusive) {
    if (hostCountInclusive) {
      return address >= low && address <= high;
    }
    return address >= low + 1 && address <= high - 1;
  }

  /**
   * Check if the parameter <code>address</code> is within
   * the range of our CIDR, inclusive
   *
   * @param cidr Cidr
   * @param hostCountInclusive whether to include the network and broadcast addresses
   * @return true if the passed cidr is equal to or contained within of our range
   */
  public final boolean isInRange(final Cidr4 cidr, final boolean hostCountInclusive) {
    if (hostCountInclusive) {
      return cidr.low >= this.low && cidr.high <= this.high;
    }
    return cidr.low >= this.low + 1 && cidr.high <= this.high - 1;
  }

  @Override
  public final int hashCode() {
    return (31 + high) * 31 + low;
  }

  @Override
  public final boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    final Cidr4 other = (Cidr4) obj;
    if (high != other.high) {
      return false;
    }
    if (low != other.low) {
      return false;
    }
    return true;
  }

  /**
   * Given a cidr range, sort from lowest to highest starting IP,
   * then from widest (highest) to narrowest (lowest) ending IP.
   * This format is important to be able to generate subsets and submaps
   * from a NavigableSet or NavigableMap.
   */
  @Override
  public final int compareTo(final Cidr4 other) {
    final int lowDiff = Integer.compare(this.low, other.low);
    if (lowDiff != 0) {
      return lowDiff;
    }
    final int highDiff = Integer.compare(this.high, other.high);
    if (highDiff != 0) {
      return -highDiff; // negative = widest first
    }
    return 0;
  }

  @Override
  public final String toString() {
    final StringBuilder buf = new StringBuilder();
    buf.append('[').append(getLowAddress(true)).append("--")
        .append(getHighAddress(true)).append(']');
    return buf.toString();
  }

}
