package io.github.twalgor.common;

import java.util.Arrays;
import java.util.Collection;
import java.util.Set;

public class Bag {
  public XBitSet vertices;
  public Bag[] neighborBags;
  
  public Bag(XBitSet vertices) {
    this.vertices = vertices;
    neighborBags = new Bag[0];
  }
  
  
  public void addNeighbor(Bag bag) {
    neighborBags = Arrays.copyOf(neighborBags, neighborBags.length + 1);
    neighborBags[neighborBags.length - 1] = bag;
  }

  public void removeNeighbor(Bag bag) {
    int i = indexOf(bag, neighborBags);
    assert i >= 0;
    Bag[] tmp = neighborBags;
    neighborBags = new Bag[neighborBags.length - 1];
    for (int j = 0; j < i; j++) {
      neighborBags[j] = tmp[j];
    }
    for (int j = i; j < neighborBags.length; j++) {
      neighborBags[j] = tmp[j + 1];
    }
  }
  
  int indexOf(Bag bag, Bag[] ba) {
    for (int i = 0; i < ba.length; i++) {
      if (bag == ba[i]) {
        return i;
      }
    }
    return -1;
  }

  public void collectVertices(XBitSet vs, Bag parent) {
    vs.or(vertices);
    for (Bag bag: neighborBags) {
      if (bag == parent) {
        continue;
      }
      bag.collectVertices(vs, this);
    }
  }
  
  public static void validateTD(Set<Bag> tdBags, Graph g) {
    assert union(tdBags).equals(g.all): union(tdBags);
    Graph toCover = g.copy();
    for (Bag bag: tdBags) {
      for (int v = bag.vertices.nextSetBit(0); v >= 0;
          v = bag.vertices.nextSetBit(v + 1)) {
        toCover.neighborSet[v].andNot(bag.vertices);
      }
    }
    for (int v = 0; v < g.n; v++) {
      assert toCover.neighborSet[v].isEmpty(): v + ":" + toCover.neighborSet[v];
    }
    for (Bag aBag: tdBags) {
      aBag.assertConnectivity(null, new XBitSet(g.n));
      break;
    }

  }
  
  void assertConnectivity(Bag parent, XBitSet visited) {
    if (parent != null) {
      assert !vertices.intersects(visited.subtract(parent.vertices)):"parent:" + parent.vertices + 
      ", this:" + vertices + ", visited: " + visited;
    }
    visited.or(vertices);
    for (Bag bag1: neighborBags) {
      if (bag1 == parent) {
        continue;
      }
      bag1.assertConnectivity(this, visited);
    }
  }

  public static XBitSet union(Collection<Bag> bags) {
    XBitSet result = new XBitSet();
    for (Bag bag: bags) {
      result.or(bag.vertices);
    }
    return result;
  }
  
  public static Bag findBagContaining(XBitSet vs, Set<Bag> bags) {
    for (Bag bag: bags) {
      if (vs.isSubset(bag.vertices)) {
        return bag;
      }
    }
    return null;
  }
  
  @Override
  public String toString() {
    return vertices.toString();
  }



  
}
