/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.github.veqryn.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

/**
 * Utility methods for tests.
 *
 * @author Mark Christopher Duncan
 */
public class TestingUtil {


  /**
   * Turn any serializable object into a byte array
   *
   * @param obj
   * @return byte[]
   * @throws IOException
   */
  public static final <T extends Serializable> byte[] pickle(final T obj) throws IOException {
    try (final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        final ObjectOutputStream oos = new ObjectOutputStream(baos);) {
      oos.writeObject(obj);
      return baos.toByteArray();
    }
  }

  /**
   * Turn a byte array into a serializable object
   *
   * @param bytes
   * @param clazz
   * @return T
   * @throws IOException
   * @throws ClassNotFoundException
   */
  public static final <T extends Serializable> T unpickle(final byte[] bytes, final Class<T> clazz)
      throws IOException, ClassNotFoundException {
    try (final ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
        final ObjectInputStream ois = new ObjectInputStream(bais);) {
      final Object o = ois.readObject();
      return clazz.cast(o);
    }
  }

}
