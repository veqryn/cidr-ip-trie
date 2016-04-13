/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.github.veqryn.collect;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Map.Entry;

import com.google.common.collect.testing.MapTestSuiteBuilder;
import com.google.common.collect.testing.TestStringMapGenerator;
import com.google.common.collect.testing.features.CollectionFeature;
import com.google.common.collect.testing.features.CollectionSize;
import com.google.common.collect.testing.features.MapFeature;

import junit.framework.Test;
import junit.framework.TestSuite;


/**
 * Tests for the PatriciaTrie class.
 * Runs some 1355 tests from the Google Guava (19) project,
 * specifically tests Map's and their various views.
 * Tests with String data.
 *
 * @author Chris Duncan
 */
public class TestPatriciaTrieWithGuava {


  public static Test suite() {
    return new TestPatriciaTrieWithGuava().allTests();
  }

  public Test allTests() {
    final TestSuite suite = new TestSuite("com.github.veqryn.collect.PatriciaTrie");
    suite.addTest(testsForPatriciaTrie());
    return suite;
  }

  protected Collection<Method> suppressForPatriciaTrie() {
    return Collections.emptySet();
  }

  public Test testsForPatriciaTrie() {
    return MapTestSuiteBuilder
        .using(new TestStringMapGenerator() {
          @Override
          protected Map<String, String> create(
              final Entry<String, String>[] entries) {
            return populate(new PatriciaTrie<String>(), entries);
          }
        })
        .named("PatriciaTrie")
        .withFeatures(
            MapFeature.GENERAL_PURPOSE,
            MapFeature.ALLOWS_NULL_ENTRY_QUERIES,
            MapFeature.FAILS_FAST_ON_CONCURRENT_MODIFICATION,
            CollectionFeature.SUPPORTS_ITERATOR_REMOVE,
            // Assumes Insertion Order if you don't implement SortedMap
            // CollectionFeature.KNOWN_ORDER,
            CollectionFeature.SERIALIZABLE,
            CollectionSize.ANY)
        .suppressing(suppressForPatriciaTrie())
        .createTestSuite();
  }

  private static <T, M extends Map<T, String>> M populate(
      final M map, final Entry<T, String>[] entries) {

    for (final Entry<T, String> entry : entries) {
      map.put(entry.getKey(), entry.getValue());
    }
    return map;
  }

}
