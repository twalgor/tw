package io.github.twalgor.greedy;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import io.github.twalgor.common.Chordal;
import io.github.twalgor.common.Graph;
import io.github.twalgor.common.TreeDecomposition;
import io.github.twalgor.common.XBitSet;
import io.github.twalgor.minseps.MinSepsGenerator;

public class MCS_M {
//  static final boolean VERIFY = true;
  static final boolean VERIFY = false;
  static final boolean TRACE = false;
  Graph g;
  Graph h;
  Set<XBitSet> minSeps;
  Set<XBitSet> maxCliques;
  XBitSet removed;
  public int width;
  
  
  public MCS_M(Graph g) {
    this.g = g;
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
    for (XBitSet sep: maxCliques) {
      if (sep.cardinality() > width + 1) {
        width = sep.cardinality() - 1;
      }
    }
    return minSeps;
  }

  public void triangulate() {
    if (TRACE) {
      System.out.println("g.n = " + g.n);
    }
    removed = new XBitSet(g.n);
    width = 0;
    while (removed.cardinality() < g.n) {
//      System.out.println("removed = " + Util.vertexSetToShortString(removed));
      order();
      XBitSet toRemove = new XBitSet(g.n);
      for (int v = removed.nextClearBit(0); v < g.n; v = removed.nextClearBit(v + 1)) {
        if (isLBSimplicial(v, g, removed)) {
          toRemove.set(v);
        }
      }
      removed.or(toRemove);
      if (TRACE) {
        System.out.println("removed = " + removed.cardinality());
      }
    }
  }
  void order() {
    h = g.copy();
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
      int weight0 = nb[v].cardinality();
      XBitSet passable = new XBitSet(g.n);
      for (int h = 0; h < weight0; h++) {
        passable.or(set[h]);
        XBitSet reachable = nbv.intersectWith(passable);
        XBitSet toScan = reachable;
        while (!toScan.isEmpty()) {
          XBitSet save = (XBitSet) reachable.clone();
          for (int w = toScan.nextSetBit(0); w > 0; w = toScan.nextSetBit(w + 1)) {
            reachable.or(g.neighborSet[w].intersectWith(passable));
            nbv.or(g.neighborSet[w].intersectWith(remaining).subtract(passable));
          }
          toScan = reachable.subtract(save);
        }
      }
      
      for (int w = nbv.nextSetBit(0); w >= 0; w = nbv.nextSetBit(w + 1)) {
        h.addEdge(w, v);
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
  
  boolean isLBSimplicial(int v, Graph h, XBitSet removed) {
    XBitSet sep = h.neighborSet[v].unionWith(removed);
    sep.set(v);
    ArrayList<XBitSet> components = h.separatedComponents(sep);
    for (XBitSet compo: components) {
      if (!h.isClique(h.neighborSet(compo).subtract(removed))) {
        return false;
      }
    }
    return true;
  }
  
  void verifyOrder(int[] ord) {
    XBitSet remaining = (XBitSet) h.all.clone();
    for (int i = 0; i < h.n; i++) {
      assert h.isClique(h.neighborSet[ord[i]].intersectWith(remaining));
      remaining.clear(ord[i]);
    }
  }

}
