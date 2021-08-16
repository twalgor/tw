package io.github.twalgor.common;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import io.github.twalgor.minseps.MinSepsGenerator;

public class Chordal {
//  static final boolean VERIFY = true;
  static final boolean VERIFY = false;
  Graph g;
  public int[] ord; 
  public Set<XBitSet> minSeps;
  public Set<XBitSet> maxCliques;
  
  
  public Chordal(Graph g) {
    this.g = g;
    assert g.isChordal();
  }
  
  public Set<XBitSet> maximalCliques() {
    order();
    return maxCliques;
  }

  public Set<XBitSet> minimalSeparators() {
    order();
    if (VERIFY) {
      MinSepsGenerator msg = new MinSepsGenerator(g, g.n - 1);
      msg.generate();
      assert minSeps.size() == msg.minSeps.size(): minSeps.size() + " : " + msg.minSeps.size();
    }
    return minSeps;
  }

  public void order() {
    ord = new int[g.n];
    XBitSet[] set = new XBitSet[g.n + 1];
    XBitSet[] nb = new XBitSet[g.n];
    XBitSet[] nbOrd = new XBitSet[g.n];
    set[0] = (XBitSet) g.all.clone();
    for (int i = 1; i <= g.n; i++) {
      set[i] = new XBitSet(g.n);
    }
    for (int v = 0; v < g.n; v++) {
      nb[v] = new XBitSet(g.n);
    }
    
    maxCliques = new HashSet<>();
    minSeps = new HashSet<>();
    int j = 0;
    XBitSet remaining = (XBitSet) g.all.clone();
    for (int i = g.n - 1; i>= 0; i--) {
      int v = set[j].nextSetBit(0);
      ord[i] = v;
      nbOrd[i] = nb[v];
//      System.out.println("i = " + i + ", v = " + v + ", nbOrd[i] set to " + nb[v]);
//      System.out.println("  ... " + nb[v].convert(ord));
      if (i < g.n - 1 && nbOrd[i + 1].cardinality() >= nbOrd[i].cardinality()) {
        maxCliques.add(nbOrd[i + 1].addBit(i + 1).convert(ord));
        minSeps.add(nbOrd[i].convert(ord));
      }
      
      remaining.clear(v);
      set[nb[v].cardinality()].clear(v);
      XBitSet nbv = g.neighborSet[v].intersectWith(remaining);
      for (int w = nbv.nextSetBit(0); w >= 0; w = nbv.nextSetBit(w + 1)) {
        set[nb[w].cardinality()].clear(w);
        nb[w].set(i);
        set[nb[w].cardinality()].set(w);
      }
      j++;
      while (j >= 0 && set[j].isEmpty()) {
        j--;
      }
    }
    maxCliques.add(nbOrd[0].addBit(0).convert(ord));
  }
  
  public Set<Bag> cliqueTree() {
    int[] ord = new int[g.n];
    XBitSet[] set = new XBitSet[g.n + 1];
    XBitSet[] nb = new XBitSet[g.n];
    XBitSet[] nbOrd = new XBitSet[g.n];
    set[0] = (XBitSet) g.all.clone();
    for (int i = 1; i <= g.n; i++) {
      set[i] = new XBitSet(g.n);
    }
    for (int v = 0; v < g.n; v++) {
      nb[v] = new XBitSet(g.n);
    }
    
    Bag[] bagAt = new Bag[g.n];
    XBitSet bagGenerators = new XBitSet(g.n);
    int j = 0;
    XBitSet remaining = (XBitSet) g.all.clone();
    int i1 = -1;
    for (int i = g.n - 1; i>= 0; i--) {
      int v = set[j].nextSetBit(0);
      ord[i] = v;
      nbOrd[i] = nb[v];
//      System.out.println("i = " + i + ", v = " + v + ", nbOrd[i] set to " + nb[v]);
//      System.out.println("  ... " + nb[v].convert(ord));
      if (i < g.n - 1 && nbOrd[i + 1].cardinality() >= nbOrd[i].cardinality()) {
        bagGenerators.set(i + 1);
        bagAt[i + 1] = new Bag(nbOrd[i + 1].addBit(i + 1));
        if (i1 >= 0) {
          for (int h = bagGenerators.nextSetBit(i + 2); h >= 0; 
              h = bagGenerators.nextSetBit(h + 1)) {
            //        System.out.println("h = " + h + ", nbOrd[i + 1] = " + nbOrd[i + 1] + 
            //            ", bagGenerators = " + bagGenerators);
            if (nbOrd[i1].isSubset(nbOrd[h].addBit(h))){
//              System.out.println("put edge between " + (i + 1) + " and " + h);
              bagAt[i + 1].addNeighbor(bagAt[h]);
              bagAt[h].addNeighbor(bagAt[i + 1]);
              break;
            }
          }
        }
        i1 = i;
      }
      
      remaining.clear(v);
      set[nb[v].cardinality()].clear(v);
      XBitSet nbv = g.neighborSet[v].intersectWith(remaining);
      for (int w = nbv.nextSetBit(0); w >= 0; w = nbv.nextSetBit(w + 1)) {
        set[nb[w].cardinality()].clear(w);
        nb[w].set(i);
        set[nb[w].cardinality()].set(w);
      }
      j++;
      while (j >= 0 && set[j].isEmpty()) {
        j--;
      }
    }
    bagGenerators.set(0);
    assert nbOrd[0] != null;
    bagAt[0] = new Bag(nbOrd[0].addBit(0));
    if (i1 >= 0) {
      for (int h = bagGenerators.nextSetBit(1); h >= 0; 
          h = bagGenerators.nextSetBit(h + 1)) {
        if (nbOrd[i1].isSubset(nbOrd[h].addBit(h))){
          bagAt[0].addNeighbor(bagAt[h]);
          bagAt[h].addNeighbor(bagAt[0]);
          break;
        }
      }
    }
    
    verifyOrder(ord);

    Set<Bag> bags = new HashSet<>();
    for (int i = bagGenerators.nextSetBit(0); i >= 0;
        i = bagGenerators.nextSetBit(i + 1)) {
      bagAt[i].vertices = bagAt[i].vertices.convert(ord);
      bags.add(bagAt[i]);
//      System.out.println(i + ":" + bagAt[i]);
    }
    
    Bag.validateTD(bags, g);
    return bags;
  }
  
  
  void verifyOrder(int[] ord) {
    XBitSet remaining = (XBitSet) g.all.clone();
    for (int i = 0; i < g.n; i++) {
      assert g.isClique(g.neighborSet[ord[i]].intersectWith(remaining));
      remaining.clear(ord[i]);
    }
  }

  public static int twOfChordal(Graph g) {
    assert g.isChordal();
    Chordal chordal = new Chordal(g);
    return maxCardinality(chordal.maximalCliques()) - 1;
  }
  
  static int maxCardinality(Set<XBitSet> vss) {
    int max = 0;
    for (XBitSet vs : vss) {
      if (vs.cardinality() > max) {
        max = vs.cardinality();
      }
    }
    return max;
  }

  
  public static TreeDecomposition chordalToTD(Graph g) {
    Chordal chordal = new Chordal(g);
    Set<Bag> ct = chordal.cliqueTree();
    return chordal.toTD(ct);
  }
  
  TreeDecomposition toTD(Set<Bag> bags) {
    TreeDecomposition td = new TreeDecomposition(0, 0, g);
    for (Bag bag: bags) {
      recurseTD(bag, null, td);
      return td;  
    }
    assert false;
    return null;
  }
  
  int recurseTD(Bag bag, Bag parent, TreeDecomposition td) {
    int r = td.addBag(bag.vertices.toArray());
    if (bag.vertices.cardinality() > td.width + 1) {
      td.width = bag.vertices.cardinality() - 1;
    }
    for (Bag bag1: bag.neighborBags) {
      if (bag1 == parent) {
        continue;
      }
      int b = recurseTD(bag1, bag, td);
      td.addEdge(r,  b);
    }
    return r;
  }
  
  public boolean isMinimalTriangulationOf(Graph h) {
    order();
    for (XBitSet sep: minSeps) {
      if (h.fullComponents(sep).size() < 2) {
        return false;
      }
    }
    return true;
  }

}
