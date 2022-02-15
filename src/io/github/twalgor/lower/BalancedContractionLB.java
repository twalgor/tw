package io.github.twalgor.lower;

import java.util.ArrayList;

import io.github.twalgor.heap.Heap;
import io.github.twalgor.heap.Queueable;
import io.github.twalgor.common.Graph;
import io.github.twalgor.common.Subgraph;
import io.github.twalgor.common.TreeDecomposition;
import io.github.twalgor.common.XBitSet;
import io.github.twalgor.decomposer.SemiPID;

public class BalancedContractionLB {
//  static final boolean TRACE = true;
  static final boolean TRACE = false;
  Graph g;
  int baseSize;
  Vertex[] vertex;
  Graph h;
  XBitSet vs;
  Heap heap;
  ArrayList<Integer> contractions;
  XBitSet affected;
  

  public BalancedContractionLB(Graph g, int baseSize) {
    this.g = g;
    this.baseSize = baseSize;
    assert g.n >= baseSize;
  }

  public int lowerbound() {
    h = g.copy();
    vs = (XBitSet) g.all.clone();
    contractions = new ArrayList<>();
    
    vertex = new Vertex[g.n];
    heap = new Heap(g.n);
    for (int v = 0; v < g.n; v++) {
      vertex[v] = new Vertex(v);
    }
    
    for (int v = 0; v < g.n; v++) {
      vertex[v].evaluate();
      heap.add(vertex[v]);
    }
    
    while (!heap.isEmpty() && vs.cardinality() > baseSize) {
//      System.out.println("heap size = " + heap.size() + ", vs = " + vs);
      if (h.isClique(vs)) {
        return vs.cardinality() - 1;
      }
      Vertex vert = (Vertex) heap.removeMin(); 
      assert vs.get(vert.v);
      assert vs.get(vert.t);
      assert vertex[vert.t].hx >= 0: vert.t + "<-" + vert.v; 

      affected = h.neighborSet[vert.v].intersectWith(vs);
      affected.or(h.neighborSet[vert.t].intersectWith(vs));
      contract(vert.v, vert.t);
      assert !vs.get(vert.v);
      affected.clear(vert.v);
      affected.set(vert.t);
      
      for (int v = affected.nextSetBit(0); v >= 0; v = affected.nextSetBit(v + 1)) {
        heap.remove(vertex[v]);
      }
      for (int v = affected.nextSetBit(0); v >= 0; v = affected.nextSetBit(v + 1)) {
        vertex[v].evaluate();
        assert vs.get(vertex[v].t);
      }
      for (int v = affected.nextSetBit(0); v >= 0; v = affected.nextSetBit(v + 1)) {
        heap.add(vertex[v]);
      }
      for (int v = vs.nextSetBit(0); v >= 0; v = vs.nextSetBit(v + 1)) {
        assert vertex[v].t != vert.v: vert.v + ", " + v + " " + affected.get(v) + " " + 
            h.neighborSet[v];
        assert vertex[v].hx >= 0;
        assert vs.get(vertex[v].t); 
        assert vertex[vertex[v].t].hx >= 0;
      }
    }
    Subgraph sub = new Subgraph(h, vs);
    if (TRACE) {
      System.out.println("solving exactly, n = " + sub.h.n);
    }
    for (int k = sub.h.minDegree(); k < g.n; k++) {
      SemiPID spid = new SemiPID(sub.h, k, false);
      TreeDecomposition td = spid.decompose();
      if (td != null) {
        return td.width;
      }
    }
    assert false;
    return 0;
  }
  
  void contract(int v, int t) {
    vs.clear(v);
    if (TRACE) {
      System.out.println("contracting " + v + " into " + t + ", remaining = " + vs);
    }
    contractions.add(originalEdge(v, t));
    XBitSet nb = h.neighborSet[v].intersectWith(vs).removeBit(t);
    h.neighborSet[t].or(nb);
    
    for (int w = nb.nextSetBit(0); w >= 0; w = nb.nextSetBit(w + 1)) {
      assert affected.get(w);
      assert vertex[w].hx >= 0;
      if (!h.neighborSet[w].get(t)) {
        h.neighborSet[w].set(t);
        XBitSet common = h.neighborSet[w].intersectWith(h.neighborSet[t]).intersectWith(vs);
        for (int z = common.nextSetBit(0); z >= 0; z = common.nextSetBit(z + 1)) {
          affected.set(z);
          assert vertex[z].hx >= 0;
        }
      }
    }
    vertex[t].component.or(vertex[v].component);
  }

  int originalEdge(int v, int t) {
    XBitSet c1 = vertex[v].component;
    XBitSet c2 = vertex[t].component;
    for (int u = c1.nextSetBit(0); u >= 0; u = c1.nextSetBit(u + 1)) {
      for (int w = c2.nextSetBit(0); w >= 0; w = c2.nextSetBit(w + 1)) {
        if (g.neighborSet[u].get(w)) {
          if (u < w) {
            return u * g.n + w;
          }
          else {
            return w * g.n + u;
          }
        }
      }
    }
    assert false;
    return -1;
  }

  int countMissings(XBitSet s) {
    int count = 0;
    for (int v = s.nextSetBit(0); v >= 0; v = s.nextSetBit(v + 1)) {
      count += s.subtract(h.neighborSet[v]).cardinality() - 1;
    }
    return count / 2;
  }

  int nearness(XBitSet s) {
    for (int nearness = 0; nearness <= 3; nearness++) {
      if (isNearClique(s, nearness)) {
        return nearness;
      }
    }
    return 4;
  }

  boolean isNearClique(XBitSet s, int nearness) {
    if (nearness <= s.cardinality() - 1) {
      return true;
    }
    if (h.isClique(s)) {
      return true;
    }
    for (int v = s.nextSetBit(0); v >= 0; v = s.nextSetBit(v + 1)) {
      if (isNearClique(s.removeBit(v), nearness - 1)) {
        return true;
      }
    }
    return false;
  }
  
  class Vertex implements Queueable{
    int v;
    XBitSet component;
    int t;
    int largerCompoSize;
    int nearness;
    int missings;
    
    int hx;
    
    Vertex (int v) {
      this.v = v;
      component = new XBitSet(new int[] {v});
    }

    void evaluate() {
      XBitSet nb = h.neighborSet[v].intersectWith(vs);
      t = -1;
      for (int z = nb.nextSetBit(0); z >= 0; z = nb.nextSetBit(z + 1)) {
        XBitSet s = nb.removeBit(z);
        int lc = Math.max(component.cardinality(), vertex[z].component.cardinality());
        int nn = nearness(s);
        int ms = countMissings(s);
        if (t == -1 ||
            lc < largerCompoSize ||
            lc == largerCompoSize &&
              (nn < nearness ||
                  nn == nearness && ms < missings)) {
          t = z;
          largerCompoSize = lc;
          nearness = nn;
          missings = ms;
        }
      }
    }
    
    @Override
    public int compareTo(Object x) {
      Vertex vert = (Vertex) x;
      if (largerCompoSize != vert.largerCompoSize) {
        return largerCompoSize - vert.largerCompoSize;
      }
      if (nearness != vert.nearness) { 
        return nearness  - vert.nearness;
      }
      if (missings != vert.missings) {
        return missings - vert.missings;
      }
      return v - vert.v;
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
  
  private static void test(String path, String name, int width) throws Exception{
    Graph g = Graph.readGraph("instance/" + path, name);
//    Graph g = Graph.readGraph("instance/" + path, name);

    System.out.println("Graph " + name + " read, n = " + 
        g.n + ", m = " + g.numberOfEdges() + ", width = " + width);
    // for (int v = 0; v < g.n; v++) {
    // System.out.println(v + ": " + g.degree[v] + ", " + g.neighborSet[v]);
    // }
    BalancedContractionLB clb = new BalancedContractionLB(g, 70);
    int lb = clb.lowerbound();
    System.out.println("contraction lower bound: " + lb);
    for (int e: clb.contractions) {
      System.out.println("(" + e / g.n + ", " + e % g.n + ")");
    }
  }

  public static void main(String args[]) throws Exception {
//    test("pkt", "pkt_20_4_50_002", 4);
//    test("pkt", "pkt_20_4_60_001", 4);
//    test("pkt", "pkt_20_4_80_001", 4);
//    test("pkt", "pkt_30_4_40_004", 4);
//    test("pkt", "pkt_30_4_40_013", 4);
//    test("pkt", "pkt_30_4_50_001", 4);
//    test("pkt", "pkt_30_4_80_001", 4);
//    test("supercubic", "sc_15_023_1", 3);
//    test("supercubic", "sc_15_030_1", 5);
//    test("supercubic", "sc_20_030_1", 5);
//    test("supercubic", "sc_20_060_1", 8);
//    test("supercubic", "sc_40_060_1", 7);
//    test("supercubic", "sc_40_080_1", 9);
//    test("random", "gnm_070_210_1", 22);
//          test("random", "gnm_070_280_1");

//      test("random", "gnm_080_240_1");
    //  test("random", "gnm_080_320_1");
//      test("random", "gnm_090_270_1");
//      test("random", "gnm_090_360_1");
    //  test("random", "gnm_090_450_1");
//    test("pace17exact", "ex001", 10);
//    test("pace17exact", "ex002", 49);
//    test("pace17exact", "ex003", 44);
//    test("pace17exact", "ex004", 486);
//    test("pace17exact", "ex006", 7);
//  test("pace17exact", "ex007", 12);
//    test("pace17exact", "ex010", 9);
//    test("pace17exact", "ex014", 18);
//    test("pace17exact", "ex015", 15);
//    test("pace17exact", "ex019", 11);
//    test("pace17exact", "ex036", 119);
//    test("pace17exact", "ex038", 26);
//    test("pace17exact", "ex041", 9);
//    test("pace17exact", "ex048", 15);
//    test("pace17exact", "ex049", 13);
//    test("pace17exact", "ex050", 28);
//    test("pace17exact", "ex052", 9);
//    test("pace17exact", "ex053", 9);
//    test("pace17exact", "ex057", 117);
//    test("pace17exact", "ex059", 10);
//    test("pace17exact", "ex061", 22);
//    test("pace17exact", "ex063", 44);
//    test("pace17exact", "ex064", 7);
//    test("pace17exact", "ex065", 25);
//    test("pace17exact", "ex066", 15);
//    test("pace17exact", "ex075", 8);
//    test("pace17exact", "ex081", 6);
//    test("pace17exact", "ex091", 9);
//    test("pace17exact", "ex095", 11);
//    test("pace17exact", "ex096", 9);
//    test("pace17exact", "ex100", 12);
//    test("pace17exact", "ex107", 12);
//    test("pace17exact", "ex121", 34);
//    test("pace17exact", "ex162", 9);
    test("Promedas/", "Promedas_44_11", 18);
  }
}
