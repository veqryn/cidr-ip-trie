package com.github.veqryn.collect;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NavigableMap;

import com.google.common.collect.testing.MapTestSuiteBuilder;
import com.google.common.collect.testing.TestStringSortedMapGenerator;
import com.google.common.collect.testing.features.CollectionFeature;
import com.google.common.collect.testing.features.CollectionSize;
import com.google.common.collect.testing.features.MapFeature;

import junit.framework.Test;
import junit.framework.TestSuite;


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
    // return NavigableMapTestSuiteBuilder
    return MapTestSuiteBuilder
        .using(new TestStringSortedMapGenerator() {
          // .using(new TestStringMapGenerator() {
          @Override
          protected NavigableMap<String, String> create(
              final Entry<String, String>[] entries) {
            return populate(new PatriciaTrie<String>(), entries);
          }
        })
        .named("HashMap")
        .withFeatures(
            MapFeature.GENERAL_PURPOSE,
            MapFeature.ALLOWS_NULL_ENTRY_QUERIES,
            MapFeature.FAILS_FAST_ON_CONCURRENT_MODIFICATION,
            CollectionFeature.SUPPORTS_ITERATOR_REMOVE,
            CollectionFeature.KNOWN_ORDER,
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
