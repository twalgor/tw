package io.github.twalgor.greedy;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import io.github.twalgor.acsd.ACSDecomposition;
import io.github.twalgor.acsd.ACSDecomposition.MTAlg;
import io.github.twalgor.common.Graph;
import io.github.twalgor.common.LocalGraph;
import io.github.twalgor.common.XBitSet;
import io.github.twalgor.heap.Heap;
import io.github.twalgor.heap.Queueable;
import io.github.twalgor.log.Log;

public class MMAF {
//  static final boolean TRACE = true;
  static final boolean TRACE = false;
  Graph g;
  XBitSet[] nb;

  Heap heap;

  int[] ord;
  int[] parent;
  int targetWidth;
  public int width;
  XBitSet removed;
  
  boolean debug;
  
  public int getWidth() {
    return width;
  }

  public MMAF(Graph g) {
    this.g = g;
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
      debug = false;
      XBitSet toRemove = new XBitSet(g.n);
      for (int v = removed.nextClearBit(0); v < g.n; v = removed.nextClearBit(v + 1)) {
        if (debug) {
          System.out.println("isLBSimplicial " + v + " :" + isLBSimplicial(v, g, removed) +
              ", removed = " + removed);
        }
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
  
  boolean isChordal(Graph h, XBitSet removed) {
    for (int v = 0; v < h.n; v++) {
      if (!isLBSimplicial(v, h, removed)) {
        return false;
      }
    }
    return true;
  }

  void order() {
    int n = g.n - removed.cardinality();
    XBitSet remaining = g.all.subtract(removed);
    heap = new Heap(n);

    nb = new XBitSet[g.n];
    Vertex[] vertex = new Vertex[g.n];
    for (int v = remaining.nextSetBit(0); v >= 0; v = remaining.nextSetBit(v + 1)) {
      nb[v] = g.neighborSet[v].intersectWith(remaining);
    }
    
    for (int v = remaining.nextSetBit(0); v >= 0; v = remaining.nextSetBit(v + 1)) {
      vertex[v] = new Vertex(v);
      vertex[v].evaluate();
      heap.add(vertex[v]);
    }

    Set<Integer> safeVertices = new HashSet<>();

    ord = new int[n];
    parent = new int[g.n];
    for (int v = remaining.nextSetBit(0); v >= 0; v = remaining.nextSetBit(v + 1)) {
      parent[v] = v;
    }

//    debug = true;
    while (!remaining.isEmpty()) {
      if (TRACE) {
        System.out.println(" remaining = " + remaining.cardinality());
      }

      if (debug) {
        System.out.println(remaining.cardinality() + " remaining out of " + g.n);
      }
      for (int vSafe: safeVertices) {
        if (debug) {
          System.out.println("vSafe = " + vSafe);
        }
        if (nb[vSafe].cardinality() > width) {
          width = nb[vSafe].cardinality();
        }
        heap.remove(vertex[vSafe]);
        for (int v = nb[vSafe].nextSetBit(0); v >= 0;
            v = nb[vSafe].nextSetBit(v + 1)) {
          nb[v].clear(vSafe);
        }
        for (int v = nb[vSafe].nextSetBit(0); v >= 0;
              v = nb[vSafe].nextSetBit(v + 1)) {
            heap.remove(vertex[v]);
            vertex[v].evaluate();
            heap.add(vertex[v]);
        }
        ord[n - remaining.cardinality()] = vSafe;
        remaining.clear(vSafe);
      }
      safeVertices.clear();

      if (remaining.isEmpty()) {
        break;
      }

      Vertex vMin = (Vertex) heap.removeMin();

      if (debug) {
        System.out.println("vMin = " + vMin + 
            ", nb = " + nb[vMin.id]);
      }
      
      if (nb[vMin.id].cardinality() > width) {
        //        System.out.println("vMin = " + vMin.id + ", cardinality = " + 
        //            + nb[vMin.id].cardinality());
        width = nb[vMin.id].cardinality();
      }

      ord[n - remaining.cardinality()] = vMin.id;

      remaining.clear(vMin.id);

      ArrayList<XBitSet> components = 
          g.separatedComponents(nb[vMin.id].unionWith(g.all.subtract(remaining)));
      for (XBitSet compo: components) {
        if (debug) {
          System.out.println("compo: " + compo);
        }
      }

      for (XBitSet compo: components) {
        if (debug) {
          System.out.println("filling " + 
              g.neighborSet(compo).intersectWith(remaining) + " of " + compo + " for " + vMin.id);
        }
        g.fill(g.neighborSet(compo).intersectWith(remaining));
      }

      for (int v = nb[vMin.id].nextSetBit(0); v >= 0; v = nb[vMin.id].nextSetBit(v + 1)) {
        nb[v].clear(vMin.id);
      }
      
      XBitSet affected = (XBitSet) nb[vMin.id].clone();

      for (int v = nb[vMin.id].nextSetBit(0); v >= 0; v = nb[vMin.id].nextSetBit(v + 1)) {
        XBitSet missing = nb[vMin.id].subtract(nb[v]);
        missing.clear(v);
        for (int w = missing.nextSetBit(v + 1); w >= 0; w = missing.nextSetBit(w + 1)) {
          nb[v].set(w);
          nb[w].set(v);
          affected.or(nb[v].intersectWith(nb[w]));
        }
      }

      assert nb[vMin.id].isSubset(remaining);

      for (int v = affected.nextSetBit(0); v >= 0;
          v = affected.nextSetBit(v + 1)) {
        heap.remove(vertex[v]);
      }
      for (int v = affected.nextSetBit(0); v >= 0;
          v = affected.nextSetBit(v + 1)) {
        vertex[v].evaluate();
      }
      for (int v = affected.nextSetBit(0); v >= 0;
          v = affected.nextSetBit(v + 1)) {
        heap.add(vertex[v]);
      }
      
      for (int v = nb[vMin.id].nextSetBit(0); v >= 0;
          v = nb[vMin.id].nextSetBit(v + 1)) {
        if (nb[v].isSubset(nb[vMin.id])) {
          safeVertices.add(v);
          parent[v] = vMin.id;
        }
      }
    }
  }

  boolean isTriviallySafe(XBitSet separator) {
    int s = separator.cardinality();
    if (s <= 2) {
      return true;
    }
    if (s == 3 && g.isIndependent(separator)) {
      return true;
      
    }
    return false;
  }

  void fill(int w) {
    for (int v = nb[w].nextSetBit(0); v >= 0; v = nb[w].nextSetBit(v + 1)) {
      nb[v].or(nb[w]);
      nb[v].clear(w);
      nb[v].clear(v);
    }
  }

  class Vertex implements Queueable {
    int id;
    int degree;
    int nFill;
    int hx;

    Vertex(int id) {
      super();
      this.id = id;
    }

    void evaluate() {
      degree = nb[id].cardinality();
      nFill = 0;
      for (int v = nb[id].nextSetBit(0); v >= 0; v = nb[id].nextSetBit(v + 1)) {
        nFill += (nb[id].subtract(nb[v]).cardinality() - 1);
      }
      nFill /= 2;
    }

    @Override
    public int compareTo(Object x) {
      Vertex v = (Vertex) x;
      if (degree == 0 && v.degree != 0) {
        return -1;
      }
      if (v.degree == 0 && degree != 0) {
        return 1;
      }
      if (nFill * v.degree == v.nFill * degree) {
        return id - v.id;
      }
      return nFill * v.degree - v.nFill * degree;
    }

    @Override
    public boolean equals(Object o) {
      return compareTo((Vertex) o) == 0;
    }

    @Override
    public String toString() {
      return id + ":" + degree + "," + nFill;
    }

    @Override
    public void setHeapIndex(int i) {
      hx = i;
    }

    @Override
    public int getHeapIndex() {
      return hx;
    }
  }
  
  private static void test(String path, String name) {
    Log log = new Log("MMAF", name);

    Graph g = Graph.readGraph(path, name);
    // Graph g = Graph.readGraph("instance/" + path, name);

    log.log("Graph " + name + " read, n = " + g.n + ", m = " + g.numberOfEdges());

    ACSDecomposition acsd = new ACSDecomposition(g, MTAlg.mmaf);
    acsd.decomposeByACS();
    XBitSet largestAtom = null;
    for (XBitSet atom : acsd.acAtoms) {
      if (largestAtom == null || atom.cardinality() > largestAtom.cardinality()) {
        largestAtom = atom;
      }
    }

    log.log("Largest atom: " + largestAtom.cardinality());

    LocalGraph local = new LocalGraph(g, largestAtom);

    long t0 = System.currentTimeMillis();
    MMAF mmaf = new MMAF(local.h);
    mmaf.triangulate();
    long t = System.currentTimeMillis();
    log.log("width " + mmaf.width + ", "  + (t - t0) + " milliseces");
  }
  
  public static void main(String[] args) {
//  Graph g = Graph.readGraph(System.in);
  test("..\\instance\\PACE2017bonus_gr", "\\Promedus_12_14");
}

}
