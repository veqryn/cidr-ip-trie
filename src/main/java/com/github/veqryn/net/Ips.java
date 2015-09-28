/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.github.veqryn.net;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility class containing methods for operating on IPs
 *
 * @author Mark Christopher Duncan
 */
public final class Ips {

  protected static final String IP_ADDRESS = "(\\d{1,3})\\.(\\d{1,3})\\.(\\d{1,3})\\.(\\d{1,3})";
  protected static final Pattern addressPattern = Pattern.compile(IP_ADDRESS);

  private Ips() {}

  /**
   * Convert a dotted decimal format address to a packed integer format
   *
   * @param address A dot-delimited IPv4 address, e.g. "192.168.0.1"
   * @param binary true if you want an integer that can be used as a netmask</br>
   *        255.255.255.255 => 11111111.11111111.11111111.11111111 binary</br>
   *        0.0.0.0 => 00000000.00000000.00000000.00000000 binary</br>
   *        false if you want a sortable packed integer representing an ip</br>
   *        255.255.255.255 => 10000000.00000000.00000000.00000000 binary, 2147483647 int</br>
   *        0.0.0.0 => 01111111.11111111.11111111.11111111 binary, -2147483648 int
   * @return 32-bit integer value of this address
   */
  protected static final int toInteger(final String address, final boolean binary) {
    final Matcher matcher = addressPattern.matcher(address);
    if (matcher.matches()) {
      return toInteger(matchAddress(matcher), binary);
    } else {
      throw new IllegalArgumentException("Could not parse [" + address + "]");
    }
  }

  /**
   * Convert IP address in an array to a packed integer format
   *
   * @param ipValues Ip parts in a 4-element array, e.g. [192,168,0,1]
   * @param binary true if you want an integer that can be used as a netmask</br>
   *        255.255.255.255 => 11111111.11111111.11111111.11111111 binary</br>
   *        0.0.0.0 => 00000000.00000000.00000000.00000000 binary</br>
   *        false if you want a sortable packed integer representing an ip</br>
   *        255.255.255.255 => 10000000.00000000.00000000.00000000 binary, 2147483647 int</br>
   *        0.0.0.0 => 01111111.11111111.11111111.11111111 binary, -2147483648 int
   * @return 32-bit integer value of this address
   */
  protected static final int toInteger(final int[] ipValues, final boolean binary) {
    int address = 0;
    for (int i = 0; i <= 3; ++i) {
      final int n = rangeCheck(ipValues[i], -1, 255);
      address |= ((n & 0xff) << 8 * (3 - i)); // Also works: address += n << (24 - (8 * i));
    }
    if (!binary) {
      address ^= Integer.MIN_VALUE;
    }
    return address;
  }

  /**
   * @param matcher Matcher with IP values in groups 1-4
   * @return 4-element array representing this address
   */
  protected static final int[] matchAddress(final Matcher matcher) {
    return new int[] {
        Integer.parseInt(matcher.group(1)),
        Integer.parseInt(matcher.group(2)),
        Integer.parseInt(matcher.group(3)),
        Integer.parseInt(matcher.group(4))};
  }


  /**
   * @return value if it is in range (begin,end], throws an exception otherwise.
   */
  protected static final int rangeCheck(final int value, final int begin, final int end) {
    if (value > begin && value <= end) {
      // (begin,end]
      return value;
    }
    throw new IllegalArgumentException(
        "Value [" + value + "] not in range (" + begin + "," + end + "]");
  }

  /**
   * @param address packed integer representing an IPv4 address
   * @param binary true if the address is in binary form
   * @return first octet as an unsigned binary byte (e.g. "192.168.4.1" would return -64)
   */
  protected final static byte getBinaryOctet1(final int address, final boolean binary) {
    return (byte) (((binary ? address : address ^ Integer.MIN_VALUE) >>> 24) & (0xff));
  }

  /**
   * @param address packed integer representing an IPv4 address
   * @param binary true if the address is in binary form
   * @return second octet as an unsigned binary byte (e.g. "192.168.4.1" would return -88)
   */
  protected final static byte getBinaryOctet2(final int address) {
    return (byte) ((address >>> 16) & (0xff));
  }

  /**
   * @param address packed integer representing an IPv4 address
   * @param binary true if the address is in binary form
   * @return third octet as an unsigned binary byte (e.g. "192.168.4.1" would return 4)
   */
  protected final static byte getBinaryOctet3(final int address) {
    return (byte) ((address >>> 8) & (0xff));
  }

  /**
   * @param address packed integer representing an IPv4 address
   * @param binary true if the address is in binary form
   * @return forth octet as an unsigned binary byte (e.g. "192.168.4.1" would return 1)
   */
  protected final static byte getBinaryOctet4(final int address) {
    return (byte) (address & (0xff));
  }

  /**
   * Convert a integer address into a 4-element array
   *
   * @param address integer representing an IPv4 address
   * @param binary true if the address is in binary form,
   *        false if a sortable packed integer
   * @return 4-element array representing this address
   */
  protected static final int[] toArray(final int address, final boolean binary) {
    final int ipValues[] = new int[4];
    for (int j = 3; j >= 0; --j) {
      ipValues[j] |=
          (((j != 0 || binary ? address : address ^ Integer.MIN_VALUE) >>> 8 * (3 - j)) & (0xff));
    }
    return ipValues;
  }

  /**
   * Get an array of the bits in a long integer
   *
   * @param binary long integer
   * @param start the starting bit position inclusive (0 based)
   * @param end the ending bit position exclusive (0 based)
   * @return 64 or less length byte array containing 0s and 1s
   */
  public static final byte[] getBits(final long binary, final int start, final int end) {
    if (start < 0 ||
        start > 64 ||
        end < 0 ||
        end > 64 ||
        start > end) {
      throw new IllegalArgumentException(
          "Illegal position arguments: start: " + start + ", end: " + end);
    }
    final int min = 64 - end;
    final byte[] bits = new byte[end - start];
    for (int j = 0, i = 63 - start; i >= min; ++j, --i) {
      bits[j] = (byte) ((binary >> i) & 1);
    }
    return bits;
  }

  /**
   * Get an array of the bits in an integer
   *
   * @param binary integer
   * @param start the starting bit position inclusive (0 based)
   * @param end the ending bit position exclusive (0 based)
   * @return 32 or less length byte array containing 0s and 1s
   */
  public static final byte[] getBits(final int binary, final int start, final int end) {
    if (start < 0 ||
        start > 32 ||
        end < 0 ||
        end > 32 ||
        start > end) {
      throw new IllegalArgumentException(
          "Illegal position arguments: start: " + start + ", end: " + end);
    }
    final int min = 32 - end;
    final byte[] bits = new byte[end - start];
    for (int j = 0, i = 31 - start; i >= min; ++j, --i) {
      bits[j] = (byte) ((binary >> i) & 1);
    }
    return bits;
  }

  /**
   * Get an array of the bits in an short
   *
   * @param binary short
   * @param start the starting bit position inclusive (0 based)
   * @param end the ending bit position exclusive (0 based)
   * @return 16 or less length byte array containing 0s and 1s
   */
  public static final byte[] getBits(final short binary, final int start, final int end) {
    if (start < 0 ||
        start > 16 ||
        end < 0 ||
        end > 16 ||
        start > end) {
      throw new IllegalArgumentException(
          "Illegal position arguments: start: " + start + ", end: " + end);
    }
    final int min = 16 - end;
    final byte[] bits = new byte[end - start];
    for (int j = 0, i = 15 - start; i >= min; ++j, --i) {
      bits[j] = (byte) ((binary >> i) & 1);
    }
    return bits;
  }

  /**
   * Get an array of the bits in an byte
   *
   * @param binary byte
   * @param start the starting bit position inclusive (0 based)
   * @param end the ending bit position exclusive (0 based)
   * @return 8 or less length byte array containing 0s and 1s
   */
  public static final byte[] getBits(final byte binary, final int start, final int end) {
    if (start < 0 ||
        start > 8 ||
        end < 0 ||
        end > 8 ||
        start > end) {
      throw new IllegalArgumentException(
          "Illegal position arguments: start: " + start + ", end: " + end);
    }
    final int min = 8 - end;
    final byte[] bits = new byte[end - start];
    for (int j = 0, i = 7 - start; i >= min; ++j, --i) {
      bits[j] = (byte) ((binary >> i) & 1);
    }
    return bits;
  }

  /**
   * Convert a 4-element array into dotted decimal format
   *
   * @param octets 4-element array representing an IPv4 address
   * @return String representation of IPv4 address
   */
  protected static final String format(final int[] octets) {
    final StringBuilder str = new StringBuilder();
    for (int i = 0; i < octets.length; ++i) {
      str.append(octets[i]);
      if (i != octets.length - 1) {
        str.append(".");
      }
    }
    return str.toString();
  }

  /**
   * @param address integer equal to the address
   * @param binary false if using a sortable packed integer,
   *        where Integer.MIN_VALUE = 0.0.0.0
   *        and 0 = 128.0.0.0
   *        and Integer.MAX_VALUE = 255.255.255.255</br>
   *        true if using a binary integer,
   *        where Integer.MIN_VALUE = 128.0.0.0
   *        and 0 = 0.0.0.0
   *        and Integer.MAX_VALUE = 127.255.255.255
   *        and -1 = 255.255.255.255
   * @return unsigned long integer value,
   *         where 0L = 0.0.0.0
   *         and 2147483648L = 128.0.0.0
   *         and 4294967296L = 255.255.255.255
   */
  protected static final long integerToUnsignedLong(final int address, final boolean binary) {
    if (binary) {
      return address < 0 ? 0xffffffff00000000L ^ address : address;
    }
    return address < 0 ? 0xffffffff80000000L ^ address : 2147483648L ^ address;
  }

  /**
   *
   * @param longAddress unsigned long integer value,
   *        where 0L = 0.0.0.0
   *        and 2147483648L = 128.0.0.0
   *        and 4294967296L = 255.255.255.255
   * @param binary true if you want an integer that can be used as a netmask</br>
   *        255.255.255.255 => 11111111.11111111.11111111.11111111 binary</br>
   *        0.0.0.0 => 00000000.00000000.00000000.00000000 binary</br>
   *        false if you want a sortable packed integer representing an ip</br>
   *        255.255.255.255 => 10000000.00000000.00000000.00000000 binary, 2147483647 int</br>
   *        0.0.0.0 => 01111111.11111111.11111111.11111111 binary, -2147483648 int
   * @return integer equal to the address, with value depending on binary parameter
   */
  protected static final int unsignedLongToInteger(final long longAddress, final boolean binary) {
    return binary ? ((int) longAddress) : (Integer.MIN_VALUE ^ (int) longAddress);
  }

}
