package com.github.veqryn.collect;

import java.util.Comparator;

public abstract class AbstractKeyCodec<K> implements KeyCodec<K> {

  private static final long serialVersionUID = -3361215683005193832L;

  @Override
  public Comparator<? super K> comparator() {
    return comparator;
  }

  Comparator<? super K> comparator =
      new Comparator<K>() {
        @Override
        public int compare(final K o1, final K o2) {
          if (o1 == null || o2 == null) {
            throw new IllegalArgumentException("Null keys not allowed");
          }
          if (o1 == o2 || o1.equals(o2)) {
            return 0;
          }
          final int l1 = AbstractKeyCodec.this.length(o1);
          final int l2 = AbstractKeyCodec.this.length(o2);
          final int min = Math.min(l1, l2);
          boolean left1;
          boolean left2;
          for (int i = 0; i < min; ++i) {
            left1 = AbstractKeyCodec.this.isLeft(o1, i);
            left2 = AbstractKeyCodec.this.isLeft(o2, i);
            if (left1 && !left2) {
              return -1;
            }
            if (!left1 && left2) {
              return 1;
            }
          }
          return l1 - l2;
        }
      };

}
