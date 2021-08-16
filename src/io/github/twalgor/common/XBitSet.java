/*
 * Copyright (c) 2016, 2019 Hisao Tamaki
 */
package io.github.twalgor.common;

import java.util.Arrays;
import java.util.BitSet;
import java.util.Comparator;

/**
 * This class extends {@code java.util.BitSet} which implements 
 * a variable length bit vector. 
 * The main purpose is to provide methods that create
 * a new vector as a result of a set operation such as 
 * union and intersection, rather than modifying the 
 * existing one.  See API documentation for {@code java.util.BitSet}.
 *
 * November 2019: some methods are moved from Util class
 * 
 * @author  Hisao Tamaki
 */

public class XBitSet extends BitSet 
  implements Comparable<XBitSet>{
  
  /**
   * Creates an empty {@code XBitSet}.
   */
  public XBitSet() {
    super();
  }

  /**
   * Creates an empty {@code XBitSet} whose initial size is large enough to explicitly
   * contain members smaller than {@code n}.
   *
   * @param  n the initial size of the {@code XBitSet} 
   * @throws NegativeArraySizeException if the specified initial size
   *         is negative
   */
  public XBitSet(int n) {
    super(n);
  }
  
  /**
   * Creates an {@code XBitSet} with members provided by an array
   *
   * @param  a an array of members to be in the {@code XBitSet}
   */
  public XBitSet(int a[]) {
    super();
    for (int i = 0; i < a.length; i++) {
      set(a[i]);
    }
  }  
  
  /**
   * Creates an {@code XBitSet} with members provided by an array.
   * The initial size is large enough to explicitly
   * contain members smaller than {@code n}.  
   *
   * @param  n the initial size of the {@code XBitSet}
   * @param  a an array of indices where the bits should be set
   * @throws NegativeArraySizeException if the specified initial size
   *         is negative
   */
  public XBitSet(int n, int a[]) {
    super(n);
    for (int i = 0; i < a.length; i++) {
      set(a[i]);
    }
  }  
  
  /**
   * Returns {@code true} if this target {@code XBitSet} is a subset
   * of the argument {@code XBitSet}
   *
   * @param  set an {@code XBitSet}
   * @return boolean indicating whether this {@code XBitSet} is a subset
   *         of the argument {@code XBitSet}
  */
  public boolean isSubset(XBitSet set) {
    BitSet tmp = (BitSet) this.clone();
    tmp.andNot(set);
    return tmp.isEmpty();
  }

  /**
   * Returns {@code true} if this target {@code XBitSet} is disjoint
   * from the argument {@code XBitSet}
   *
   * @param  set an {@code XBitSet}
   * @return boolean indicating whether this {@code XBitSet} is 
   *  disjoint from the argument {@code XBitSet}
  */
  public boolean isDisjoint(XBitSet set) {
    BitSet tmp = (BitSet) this.clone();
    tmp.and(set);
    return tmp.isEmpty();
  }

  /**
   * Returns {@code true} if this target {@code XBitSet} has a 
   * non-empty intersection with the argument {@code XBitSet}
   *
   * @param  set an {@code XBitSet}
   * @return boolean indicating whether this {@code XBitSet} 
   *  intersects with the argument {@code XBitSet}
  */

  public boolean intersects(XBitSet set) {
    return super.intersects(set);
  }

  /**
   * Returns {@code true} if this target {@code XBitSet} is a superset
   * of the argument bit set
   *
   * @param  set an {@code XBitSet}
   * @return boolean indicating whether this {@code XBitSet} is a superset
   *  of the argument {@code XBitSet}
  */
  public boolean isSuperset(XBitSet set) {
    BitSet tmp = (BitSet) set.clone();
    tmp.andNot(this);
    return tmp.isEmpty();
  }
  
  /**
   * Returns a {@code XBitSet} that is the union of this
   * target {@code XBitSet} and the argument {@code XBitSet}
   *
   * @param  set an {@code XBitSet}
   * @return the union {@code XBitSet}
  */
  public XBitSet unionWith(XBitSet set) {
    XBitSet result = (XBitSet) this.clone();
    result.or(set);
    return result;
  }

  /**
   * Returns a {@code XBitSet} that is this
   * target {@code XBitSet} with specified bit set
   * @param  i the bit to be set
   * @return the resulting {@code XBitSet}
  */
  public XBitSet addBit(int i) {
    XBitSet result = (XBitSet) this.clone();
    result.set(i);
    return result;
  }

  /**
   * Returns a {@code XBitSet} that is this
   * target {@code XBitSet} with specified bit cleared
   * @param  i the bit to be cleared
   * @return the resulting {@code XBitSet}
  */
  public XBitSet removeBit(int i) {
    XBitSet result = (XBitSet) this.clone();
    result.clear(i);
    return result;
  }

  /**
   * Returns an {@code XBitSet} that is the intersection of this
   * target {@code XBitSet} and the argument {@code XBitSet}
   *
   * @param  set an {@code XBitSet}
   * @return the intersection {@code XBitSet}
  */
  public XBitSet intersectWith(XBitSet set) {
    XBitSet result = (XBitSet) this.clone();
    result.and(set);
    return result;
  }

  /**
   * Returns an {@code XBitSet} that is the result of
   * of removing the members of the argument {@code XBitSet}
   * from the target {@code XBitSet}.
   * @param  set an {@code XBitSet}
   * @return the difference {@code XBitSet}
  */
  public XBitSet subtract(XBitSet set) {
    XBitSet result = (XBitSet) this.clone();
    result.andNot(set);
    return result;
  }
  
  /**
   * Returns {@code true} if the target {@code XBitSet} has a member 
   * that is smaller than the smallest member of the argument {@code XBitSet}.
   * Both the target and the argument {@code XBitSet} must be non-empty
   * to ensure a meaningful result.
   * @param  set an {@code XBitSet}
   * @return {@code true} if the target {@code XBitSet} has a member
   * smaller than the smallest member of the argument {@code XBitSet};
   * {@code false} otherwise 
  */
  public boolean hasSmaller(XBitSet set) {
    assert !isEmpty() && !set.isEmpty();
    return this.nextSetBit(0) < set.nextSetBit(0);
  }

  @Override
  /**
   * Compare the target {@code XBitSet} with the argument
   * {@code XBitSet}, where the bit vectors are viewed as
   * binary representation of an integer, the bit {@code i} 
   * set meaning that the number contains {@code 2^i}.
   * @return negative value if the target is smaller, positive if it is
   * larger, and zero if it equals the argument  
   */
  public int compareTo(XBitSet set) {
    int l1 = this.length();
    int l2 = set.length();
    if (l1 != l2) {
      return l1 - l2;
    }
    for (int i = l1 - 1; i >= 0; i--) {
      if (this.get(i) && !set.get(i)) return 1;
      else if (!this.get(i) && set.get(i)) return -1;
    }
    return 0;
  }

  /**
   * Converts the target {@code XBitSet} into an {@code XBitSet} 
   * according to the conversion map: a bit {@code b} is in the result
   * if and only if {@code b} = {@code conv[a]} for some bit {@code a}
   * in the target. Those {@code a} such that {@code conv[a] < 0} are ignored.
   * @param conv the conversion map of type {@code int[]}  
   * @return the converted {@code XBitSet}
   * @throws IndexOutOfBoundsException when {@code conv[a]} is negative for some
   * bit {@code a} set in the target.
   * Nov 18, 2019 added by Hisao Tamaki.
   */
  public XBitSet convert(int[] conv) {
    XBitSet result = new XBitSet();
    for (int v = nextSetBit(0); v >= 0; v = nextSetBit(v + 1)) {
      if (conv[v] >= 0) {
        result.set(conv[v]);
      }
    }
    return result;
  }

  /**
   * Converts the target {@code XBitSet} into an array
   * that contains all the members in the set
   * @return the array representation of the set
   */
  public int[] toArray() {
    int[] result = new int[cardinality()];
    int k = 0;
    for (int i = nextSetBit(0); i >=0; i= nextSetBit(i + 1)) {
      result[k++] = i;
    }
    return result;
  }

  /**
   * Creates an index map for the target {@code XBitSet}, 
   * that is an {@code int} array {@code a} of specified size {@code n} 
   * such that {@code a[i]} is the index of {@ code i} in {@code this.toArray()}
   *  if {@ code i} is a bit set in the target {@code XBitSet} and -1 otherwise.
   * @ n the size of the index map
   * @return the array representation of the set
   */
  public int[] indexMap(int n) {
    int[] result = new int[n];
    Arrays.fill(result, -1);
    int k = 0;
    for (int i = nextSetBit(0); i >=0; i= nextSetBit(i + 1)) {
      result[i] = k++;
    }
    return result;
  }

  /**
   * Short string representation of the target {@code XBitSet}.
   * @return the short string representation of the target
   */
  public String toShortString() {
    String s = cardinality() + ":" + toString();
    if (s.length() > 100) {
      s = s.substring(0, 100) + "...";
    }
    return s;
  }
  
  /**
   * String representation of the target {@code XBitSet}.
   * @return the string representation of the target
   */
  @Override
  public String toString() {
    return cardinality() + super.toString();
  }
  
  /**
   * Checks if this target bit set has an element
   * that is smaller than every element in 
   * the argument bit set
   * @param vs bit set 
   * @return {@code true} if this bit set has an element
   * smaller than every element in {@code vs}
   */
  public boolean hasSmallerVertexThan(XBitSet vs) {
    if (this.isEmpty()) return false;
    else if (vs.isEmpty()) return true;
    else return nextSetBit(0) < vs.nextSetBit(0);
  }

  /**
   * Creates a singleton XBitSet with a specified member
   * @param v the specified member
   * @return the singleton XBitSet constructed
   */
  public static XBitSet singleton(int v) {
    XBitSet result = new XBitSet();
    result.set(v);
    return result;
  }
  
  /**
   * holds the reference to an instance of the {@code DescendingComparator}
   * for {@code BitSet}
   */
  public static final Comparator<BitSet> descendingComparator =
      new DescendingComparator();

  /**
   * holds the reference to an instance of the {@code AscendingComparator}
   * for {@code BitSet}
   */
  
  public static final Comparator<BitSet> ascendingComparator =
      new AscendingComparator();
  
  /**
   * holds the reference to an instance of the {@code LexicographicComparator}
   * for {@code BitSet}
   */
  
  public static final Comparator<BitSet> lexicographicComparator =
      new LexicographicComparator();
  
  /**
   * holds the reference to an instance of the {@code CardinalityComparator}
   * for {@code BitSet}
   */
  public static final Comparator<BitSet> cardinalityComparator =
      new CardinalityComparator();
  
  /**
   * A comparator for {@code BitSet}. The {@code compare}
   * method compares the two vectors the binary integer values 
   * where the highest bit is the most significant. 
   */
  public static class DescendingComparator implements Comparator<BitSet> {
    @Override
    public int compare(BitSet s1, BitSet s2) {
      int l1 = s1.length();
      int l2 = s2.length();
      if (l1 != l2) {
        return l1 - l2;
      }
      for (int i = l1 - 1; i >= 0; i--) {
        if (s1.get(i) && !s2.get(i)) return 1;
        else if (!s1.get(i) && s2.get(i)) return -1;
      }
      return 0;
    }
  }

  /**
   * A comparator for {@code BitSet}. The {@code compare} method compares
   * the two vectors in the binary integer values where the
   * lowest bit is the most significant. 
   */
  public static class AscendingComparator implements Comparator<BitSet> {
    @Override
    public int compare(BitSet s1, BitSet s2) {
      int l1 = s1.length();
      int l2 = s2.length();

      for (int i = 0; i < Math.min(l1,  l2); i++) {
        if (s1.get(i) && !s2.get(i)) return 1;
        else if (!s1.get(i) && s2.get(i)) return -1;
      }
      return l1 - l2;
    }
  }
  
  /**
   * A comparator for {@code BitSet}. The {@code compare} method compares
   * the two vectors in the lexicographic order 
   */
  public static class LexicographicComparator implements Comparator<BitSet> {
    @Override
    public int compare(BitSet s1, BitSet s2) {
      int l1 = s1.length();
      int l2 = s2.length();

      for (int i = 0; i < Math.min(l1,  l2); i++) {
        if (s1.get(i) && !s2.get(i)) {
          return -1;
        }
        else if (!s1.get(i) && s2.get(i)) {
          return 1;
        }
      }
      return l1 - l2;
    }
  }
  
  /**
   * A comparator for {@code BitSet}. The {@code compare} method compares
   * the two sets in terms of the cardinality. In case of
   * a tie, the two sets are compared by the {@code DescendingComparator} 
   */
  public static class CardinalityComparator implements Comparator<BitSet> {
    @Override
    public int compare(BitSet s1, BitSet s2) {
      int c1 = s1.cardinality();
      int c2 = s2.cardinality();
      if (c1 != c2) {
        return c1 - c2;
      }
      else 
        return descendingComparator.compare(s1, s2);
    }
  }
}
