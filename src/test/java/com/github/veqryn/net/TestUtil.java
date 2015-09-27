/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.github.veqryn.net;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

/**
 * Data and utility methods for tests.
 */
public class TestUtil {

  /**
   * The ips held in the 'ips' object array, but in order
   */
  protected static final String[] ipsInOrder = new String[] {
      "0.0.0.0",
      "0.0.0.1",
      "127.255.255.255",
      "128.0.0.0",
      "192.168.0.1",
      "211.113.251.89",
      "255.255.255.255",
  };

  /**
   * Data set of verified ip values:
   *
   * <pre>
   * index -- value
   * 0 -- String IPv4 address
   * 1 -- octets int array
   * 2 -- binary int
   * 3 -- sortable int
   * 4 -- unsigned long
   * 5 -- octet 1 int
   * 6 -- octet 2 int
   * 7 -- octet 3 int
   * 8 -- octet 4 int
   * 9 -- binary bit byte array
   * </pre>
   */
  protected static final Object[][] ips = new Object[][] {

      {"0.0.0.0", new int[] {0, 0, 0, 0}, 0, Integer.MIN_VALUE, 0L,
          0, 0, 0, 0,
          new byte[] {
              0, 0, 0, 0, 0, 0, 0, 0,
              0, 0, 0, 0, 0, 0, 0, 0,
              0, 0, 0, 0, 0, 0, 0, 0,
              0, 0, 0, 0, 0, 0, 0, 0}},

      {"0.0.0.1", new int[] {0, 0, 0, 1}, 1, Integer.MIN_VALUE + 1, 1L,
          0, 0, 0, 1,
          new byte[] {
              0, 0, 0, 0, 0, 0, 0, 0,
              0, 0, 0, 0, 0, 0, 0, 0,
              0, 0, 0, 0, 0, 0, 0, 0,
              0, 0, 0, 0, 0, 0, 0, 1}},

      {"127.255.255.255", new int[] {127, 255, 255, 255}, Integer.MAX_VALUE, -1, 2147483647L,
          127, -1, -1, -1,
          new byte[] {
              0, 1, 1, 1, 1, 1, 1, 1,
              1, 1, 1, 1, 1, 1, 1, 1,
              1, 1, 1, 1, 1, 1, 1, 1,
              1, 1, 1, 1, 1, 1, 1, 1}},

      {"128.0.0.0", new int[] {128, 0, 0, 0}, Integer.MIN_VALUE, 0, 2147483648L,
          -128, 0, 0, 0,
          new byte[] {
              1, 0, 0, 0, 0, 0, 0, 0,
              0, 0, 0, 0, 0, 0, 0, 0,
              0, 0, 0, 0, 0, 0, 0, 0,
              0, 0, 0, 0, 0, 0, 0, 0}},

      {"255.255.255.255", new int[] {255, 255, 255, 255}, -1, Integer.MAX_VALUE, 4294967295L,
          -1, -1, -1, -1,
          new byte[] {
              1, 1, 1, 1, 1, 1, 1, 1,
              1, 1, 1, 1, 1, 1, 1, 1,
              1, 1, 1, 1, 1, 1, 1, 1,
              1, 1, 1, 1, 1, 1, 1, 1}},

      {"192.168.0.1", new int[] {192, 168, 0, 1}, -1062731775, 1084751873, 3232235521L,
          -64, -88, 0, 1,
          new byte[] {
              1, 1, 0, 0, 0, 0, 0, 0,
              1, 0, 1, 0, 1, 0, 0, 0,
              0, 0, 0, 0, 0, 0, 0, 0,
              0, 0, 0, 0, 0, 0, 0, 1}},

      {"211.113.251.89", new int[] {211, 113, 251, 89}, -747504807, 1399978841, 3547462489L,
          -45, 113, -5, 89,
          new byte[] {
              1, 1, 0, 1, 0, 0, 1, 1,
              0, 1, 1, 1, 0, 0, 0, 1,
              1, 1, 1, 1, 1, 0, 1, 1,
              0, 1, 0, 1, 1, 0, 0, 1}},

  };

  /**
   * Data set of verified netmask values:
   *
   * <pre>
   * index -- value
   * 0 -- Cidr range /x int
   * 1 -- int value of netmask
   * 2 -- dotted decimal String
   * </pre>
   */
  protected static final Object[][] netmasks = new Object[][] {
      {1, -2147483648, "128.0.0.0"},
      {2, -1073741824, "192.0.0.0"},
      {3, -536870912, "224.0.0.0"},
      {4, -268435456, "240.0.0.0"},
      {5, -134217728, "248.0.0.0"},
      {6, -67108864, "252.0.0.0"},
      {7, -33554432, "254.0.0.0"},
      {8, -16777216, "255.0.0.0"},
      {9, -8388608, "255.128.0.0"},
      {10, -4194304, "255.192.0.0"},
      {11, -2097152, "255.224.0.0"},
      {12, -1048576, "255.240.0.0"},
      {13, -524288, "255.248.0.0"},
      {14, -262144, "255.252.0.0"},
      {15, -131072, "255.254.0.0"},
      {16, -65536, "255.255.0.0"},
      {17, -32768, "255.255.128.0"},
      {18, -16384, "255.255.192.0"},
      {19, -8192, "255.255.224.0"},
      {20, -4096, "255.255.240.0"},
      {21, -2048, "255.255.248.0"},
      {22, -1024, "255.255.252.0"},
      {23, -512, "255.255.254.0"},
      {24, -256, "255.255.255.0"},
      {25, -128, "255.255.255.128"},
      {26, -64, "255.255.255.192"},
      {27, -32, "255.255.255.224"},
      {28, -16, "255.255.255.240"},
      {29, -8, "255.255.255.248"},
      {30, -4, "255.255.255.252"},
      {31, -2, "255.255.255.254"},
      {32, -1, "255.255.255.255"},
  };

  /**
   * Turn any serializable object into a byte array
   *
   * @param obj
   * @return byte[]
   * @throws IOException
   */
  protected static final <T extends Serializable> byte[] pickle(final T obj) throws IOException {
    try (final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        final ObjectOutputStream oos = new ObjectOutputStream(baos);) {
      oos.writeObject(obj);
      return baos.toByteArray();
    }
  }

  /**
   * Turn a byte array into a serializable object
   *
   * @param b
   * @param cl
   * @return T
   * @throws IOException
   * @throws ClassNotFoundException
   */
  protected static final <T extends Serializable> T unpickle(final byte[] b, final Class<T> cl)
      throws IOException, ClassNotFoundException {
    try (final ByteArrayInputStream bais = new ByteArrayInputStream(b);
        final ObjectInputStream ois = new ObjectInputStream(bais);) {
      final Object o = ois.readObject();
      return cl.cast(o);
    }
  }

}
