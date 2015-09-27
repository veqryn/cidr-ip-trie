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
   * Data set of verified values:
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

  protected static final <T extends Serializable> byte[] pickle(final T obj) throws IOException {
    try (final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        final ObjectOutputStream oos = new ObjectOutputStream(baos);) {
      oos.writeObject(obj);
      return baos.toByteArray();
    }
  }

  protected static final <T extends Serializable> T unpickle(final byte[] b, final Class<T> cl)
      throws IOException, ClassNotFoundException {
    try (final ByteArrayInputStream bais = new ByteArrayInputStream(b);
        final ObjectInputStream ois = new ObjectInputStream(bais);) {
      final Object o = ois.readObject();
      return cl.cast(o);
    }
  }

}
