/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.github.veqryn.collect;

import java.util.NavigableMap;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.commons.collections4.BulkTest;
import org.apache.commons.collections4.map.AbstractMapTest;
import org.apache.commons.collections4.map.AbstractSortedMapTest;

import junit.framework.Test;


/**
 * Tests for the PatriciaTrie class.
 * Runs some 500+ tests from the Apache Commons Collections (4) project,
 * specifically tests SortedMap's and Map's and their various views.
 * Tests with String data.
 *
 * @author Mark Christopher Duncan
 */
public class TestPatriciaTrie<V> extends AbstractSortedMapTest<String, V> {

  // Test string order when using comparator:
  // hello, tmp, blah, bar, baz, foo, nonnullkey, all, again,
  // you, see, key, key2, gee, golly, gosh, goodbye, we'll


  // Set up our Test:

  public TestPatriciaTrie(final String testName) {
    super(testName);
  }

  public static Test suite() {
    return BulkTest.makeSuite(TestPatriciaTrie.class);
  }

  @Override
  public NavigableMap<String, V> makeObject() {
    return new PatriciaTrie<V>();
  }

  @Override
  public SortedMap<String, V> makeConfirmedMap() {
    // TODO: apparently the only thing apache does not test
    // for is the order keys and values are returned in
    return new TreeMap<String, V>(new PatriciaTrie<String>().comparator());
  }

  @Override
  public boolean isAllowNullKey() {
    return false;
  }

  @Override
  public boolean isAllowNullValue() {
    return false;
  }

  @Override
  public boolean isSetValueSupported() {
    return false;
  }



  // Configure our sub-map views:

  @Override
  public BulkTest bulkTestHeadMap() {
    return new TestTrieHeadMap(this);
  }

  protected class TestTrieHeadMap extends TestHeadMap<String, V> {
    public TestTrieHeadMap(final AbstractMapTest<String, V> main) {
      super(main);
    }

    @Override
    public boolean isSetValueSupported() {
      return false;
    }
  }



  @Override
  public BulkTest bulkTestTailMap() {
    return new TestTrieTailMap(this);
  }

  protected class TestTrieTailMap extends TestTailMap<String, V> {
    public TestTrieTailMap(final AbstractMapTest<String, V> main) {
      super(main);
    }

    @Override
    public boolean isSetValueSupported() {
      return false;
    }
  }



  @Override
  public BulkTest bulkTestSubMap() {
    return new TestTrieSubMap(this);
  }

  protected class TestTrieSubMap extends TestSubMap<String, V> {
    public TestTrieSubMap(final AbstractMapTest<String, V> main) {
      super(main);
    }

    @Override
    public boolean isSetValueSupported() {
      return false;
    }
  }



  @Override
  public BulkTest bulkTestMapEntrySet() {
    return new TestMapEntrySet();
  }

  protected class TestTrieEntrySet extends TestMapEntrySet {
    public boolean isSetValueSupported() {
      return false;
    }
  }



  // TODO: Test Descending Map view
  // TODO: This should work, but I keep getting AbstractMethodError on makeObject
  // public BulkTest bulkTestDescendingMap() {
  // return new TestTrieDescendingMap<V>(this);
  // }
  //
  // public static class TestTrieDescendingMap<V> extends TestViewMap<String, V> {
  //
  // public TestTrieDescendingMap(final AbstractMapTest<String, V> main) {
  // super("PatriciaTrie.TrieDescendingMap", main);
  // final Map<String, V> sm = makeFullMap();
  // for (final Entry<String, V> entry : sm.entrySet()) {
  // this.subSortedKeys.add(entry.getKey());
  // this.subSortedValues.add(entry.getValue());
  // }
  // this.subSortedNewValues.addAll(Arrays.asList(main.getNewSampleValues()));
  // Collections.reverse(this.subSortedNewValues);
  // }
  //
  // @Override
  // public boolean isSetValueSupported() {
  // return false;
  // }
  //
  // @Override
  // public SortedMap<String, V> makeObject() {
  // return ((NavigableMap<String, V>) main.makeObject()).descendingMap();
  // }
  //
  // @Override
  // public SortedMap<String, V> makeFullMap() {
  // return ((NavigableMap<String, V>) main.makeFullMap()).descendingMap();
  // }
  //
  // @Override
  // public String getCompatibilityVersion() {
  // return main.getCompatibilityVersion() + ".TrieDescendingMapView";
  // }
  // }



  // -----------------------------------------------------------------------

  @Override
  public String getCompatibilityVersion() {
    return "1";
  }


  // Use this to write out new .obj files for compatibility tracking
  // public static void main(final String[] args) throws IOException {
  // final TestPatriciaTrie<String> test = new TestPatriciaTrie<>("");
  // {
  // final Map<String, String> map = test.makeObject();
  // if (!(map instanceof Serializable)) {
  // return;
  // }
  // final String path = test.getCanonicalEmptyCollectionName(map);
  // Files.createDirectories(Paths.get(path).getParent());
  // test.writeExternalFormToDisk((Serializable) map, path);
  // }
  //
  // {
  // final Map<String, String> map = test.makeFullMap();
  // if (!(map instanceof Serializable)) {
  // return;
  // }
  // final String path = test.getCanonicalFullCollectionName(map);
  // Files.createDirectories(Paths.get(path).getParent());
  // test.writeExternalFormToDisk((Serializable) map, path);
  // }
  // }

}
