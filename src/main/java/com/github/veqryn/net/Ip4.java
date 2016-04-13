/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.github.veqryn.net;

import static com.github.veqryn.net.Cidrs.getLowestContainingCidrForRange;
import static com.github.veqryn.net.Ips.format;
import static com.github.veqryn.net.Ips.toArray;
import static com.github.veqryn.net.Ips.toInteger;

import java.io.Serializable;
import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * Light weight immutable IPv4 type,
 * which implements hashCode, equals, Comparable, and Serializable.
 * Some methods and method signatures influenced by org.apache.commons.net.util.SubnetUtils
 *
 * <pre>
 * // Example usage:
 * // Various ways to construct:
 * Ip4 myIP1 = new Ip4(192, 168, 1, 104);
 * Ip4 myIP2 = new Ip4("192.168.1.103");
 * Ip4 myIP3 = new Ip4(-1062731415); // Java doesn't have unsigned integer types
 * Ip4 myIP4 = new Ip4(myIP1);
 *
 * System.out.println(myIP1.equals(myIP2)); // false
 *
 * // [192.168.1.103, 192.168.1.104, 192.168.1.105]
 * System.out.println(new TreeSet&lt;Ip4&gt;(Arrays.asList(myIP1, myIP2, myIP3, myIP4)));
 *
 * System.out.println(myIP1.getAddress()); // "192.168.1.104"
 *
 * System.out.println(myIP1.getBinaryInteger()); // -1062731416
 *
 * Cidr4 orFewerMaskBits = myIP1.getLowestContainingCidr(28); // 192.168.1.96/28
 *
 * Cidr4 slash32Cidr = myIP1.getCidr(); // 192.168.1.104/32
 *
 * InetAddress inetAddress = myIP1.getInetAddress();
 * </pre>
 *
 * @author Chris Duncan
 */
public final class Ip4 implements Comparable<Ip4>, Serializable {

  private static final long serialVersionUID = 1929530070657767617L;

  private final int address;

  /**
   * Constructor that takes a IPv4 string, e.g. "192.168.0.1"
   *
   * @param ipAddress A IPv4 string, e.g. "192.168.0.1"
   * @throws IllegalArgumentException if the parameter is invalid,
   *         i.e. does not match n.n.n.n where n=1-3 decimal digits
   */
  public Ip4(final String ipAddress) {
    address = toInteger(ipAddress, false);
  }

  /**
   * Constructor that duplicates another Ip
   *
   * @param ip Ip
   */
  public Ip4(final Ip4 ip) {
    address = ip.address;
  }

  /**
   * Constructor that takes individual unsigned int octets
   *
   * @param octet1 e.g. 192
   * @param octet2 e.g. 168
   * @param octet3 e.g. 0
   * @param octet4 e.g. 1
   */
  public Ip4(final int octet1, final int octet2,
      final int octet3, final int octet4) {
    address = toInteger(new int[] {octet1, octet2, octet3, octet4}, false);
  }

  /**
   * Constructor that takes a binary integer value
   *
   * @param ipIntegerValue IPv4 integer in binary format,
   *        where Integer.MIN_VALUE = 128.0.0.0
   *        and 0 = 0.0.0.0
   *        and Integer.MAX_VALUE = 127.255.255.255
   *        and -1 = 255.255.255.255
   */
  public Ip4(final int ipIntegerValue) {
    this(ipIntegerValue, true);
  }

  /**
   * Constructor that takes a packed integer value
   *
   * @param ipIntegerValue IPv4 integer
   * @param binary false if using a sortable packed integer,
   *        where Integer.MIN_VALUE = 0.0.0.0
   *        and 0 = 128.0.0.0
   *        and Integer.MAX_VALUE = 255.255.255.255<br>
   *        true if using a binary integer,
   *        where Integer.MIN_VALUE = 128.0.0.0
   *        and 0 = 0.0.0.0
   *        and Integer.MAX_VALUE = 127.255.255.255
   *        and -1 = 255.255.255.255
   */
  protected Ip4(final int ipIntegerValue, final boolean binary) {
    address = binary ? ipIntegerValue ^ Integer.MIN_VALUE : ipIntegerValue;
  }



  /**
   * @return a packed sortable integer equal to the address,
   *         where Integer.MIN_VALUE = 0.0.0.0
   *         and 0 = 128.0.0.0
   *         and Integer.MAX_VALUE = 255.255.255.255
   */
  protected final int getSortableInteger() {
    return address;
  }

  /**
   * @return a binary integer equal to the address,
   *         where Integer.MIN_VALUE = 128.0.0.0
   *         and 0 = 0.0.0.0
   *         and Integer.MAX_VALUE = 127.255.255.255
   *         and -1 = 255.255.255.255
   */
  public final int getBinaryInteger() {
    return address ^ Integer.MIN_VALUE;
  }

  /**
   * @return the address in dotted format, may be "0.0.0.0" if there is no valid address
   */
  public final String getAddress() {
    return format(toArray(address, false));
  }

  /**
   * @return the address as a Cidr with /32 range
   */
  public final Cidr4 getCidr() {
    return new Cidr4(this);
  }

  /**
   * @param maskBits the maximum number of bits in the netmask (e.g. 32 - 1)
   * @return A new Cidr representing the lowest cidr that has
   *         no more than this many maskBits and contains our cidr range
   *         e.g. 192.168.10.10/31 -&gt; mask 24 -&gt; 192.168.0.0/24
   */
  public final Cidr4 getLowestContainingCidr(final int maskBits) {
    return getLowestContainingCidrForRange(this.address, this.address, maskBits, false);
  }

  /**
   * This method uses InetAddress.getByAddress, and so does not block.
   *
   * @return java.net.InetAddress representing this IPv4 address
   * @throws UnknownHostException if host not found
   */
  public final InetAddress getInetAddress() throws UnknownHostException {
    final int[] ints = toArray(address, false);
    return InetAddress.getByAddress(new byte[] {
        (byte) ints[0],
        (byte) ints[1],
        (byte) ints[2],
        (byte) ints[3]});
  }

  @Override
  public final int hashCode() {
    return address;
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
    if (address != ((Ip4) obj).address) {
      return false;
    }
    return true;
  }

  @Override
  public final int compareTo(final Ip4 other) {
    return Integer.compare(this.address, other.address);
  }

  @Override
  public final String toString() {
    return getAddress();
  }

}
