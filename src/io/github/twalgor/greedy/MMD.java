package io.github.twalgor.greedy;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import io.github.twalgor.common.Chordal;
import io.github.twalgor.common.Graph;
import io.github.twalgor.common.TreeDecomposition;
import io.github.twalgor.common.XBitSet;
import io.github.twalgor.heap.Heap;
import io.github.twalgor.heap.Queueable;

public class MMD {
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

  public MMD(Graph g) {
    this.g = g;
  }

  public void triangulate() {
    if (TRACE) {
      System.out.println("g.n = " + g.n);
    }
    removed = new XBitSet(g.n);
    width = 0;
    while (removed.cardinality() < g.n) {
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

      XBitSet affected = null;
      affected = new XBitSet(g.n);
      for (int v = nb[vMin.id].nextSetBit(0); v >= 0;
          v = nb[vMin.id].nextSetBit(v + 1)) {
        for (int w = nb[vMin.id].nextSetBit(v + 1); w >= 0;
            w = nb[vMin.id].nextSetBit(w + 1)) {
          if (!nb[v].get(w)) {
            affected.or(nb[v].unionWith(nb[w]));
          }
        }
      }
      affected.clear(vMin.id);


      fill(vMin.id);

      assert nb[vMin.id].isSubset(remaining);
      for (int v = nb[vMin.id].nextSetBit(0); v >= 0;
          v = nb[vMin.id].nextSetBit(v + 1)) {
        assert vertex[v].hx >= 0;
        assert heap.h[vertex[v].hx] == vertex[v];
        heap.remove(vertex[v]);
        vertex[v].evaluate();
        heap.add(vertex[v]);
      }

      for (int v = affected.nextSetBit(0); v >= 0;
          v = affected.nextSetBit(v + 1)) {
        heap.remove(vertex[v]);
        vertex[v].evaluate();
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
    int hx;

    Vertex(int id, int degree) {
      super();
      this.id = id;
      this.degree = degree;
    }

    Vertex(int id) {
      this(id, nb[id].cardinality());
    }

    void evaluate() {
      degree = nb[id].cardinality();
    }
    
    @Override
    public int compareTo(Object x) {
      Vertex v = (Vertex) x;
      if (degree == v.degree) {
        return id - v.id;
      }
      return degree - v.degree;
    }

    @Override
    public boolean equals(Object o) {
      return compareTo((Vertex) o) == 0;
    }

    @Override
    public String toString() {
      return id + ":" + degree;
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
}
