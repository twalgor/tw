package io.github.twalgor.upper;

import java.util.Random;

import io.github.twalgor.common.Graph;
import io.github.twalgor.common.LocalGraph;
import io.github.twalgor.common.XBitSet;
import io.github.twalgor.greedy.MMAF;
import io.github.twalgor.main.Shared;

public class LocalUBNew {
  Graph g;
  XBitSet vs;
  LocalGraph lg;
  int ub;
  public int getUB() {
    return ub;
  }
  
  Shared shared;
  Graph triangulated;
  Random random;
  HBTMerge hbtMerge;
  
  public LocalUBNew(Graph g, XBitSet vs, Shared shared) {
    this.g = g;
    this.vs = vs;
    this.shared = shared;
    lg = new LocalGraph(g, vs);
    ub = lg.h.n - 1;
    random = new Random(1);
  }
  
  public Graph getGraph() {
    return lg.h;
  }
  
  public void initialUB() {
    Graph h = lg.h.copy();
    MMAF mmaf = new MMAF(h);
    mmaf.triangulate();
    ub = mmaf.width;
    triangulated = h;
  }

  public void improveUB() {
    if (hbtMerge == null) {
      hbtMerge = new HBTMerge(lg.h, 0, random);
      hbtMerge.initialize();
    }
    while (hbtMerge.width >= ub && ub > shared.getLB()) {
      hbtMerge.improve();
    }
    if (hbtMerge.width < ub) {
      ub = hbtMerge.width;
      triangulated = hbtMerge.triangulated;
    }
  }
  
  public void fillTriangulation(Graph tr) {
    assert tr.n == g.n;
    for (int v = 0; v < triangulated.n; v++) {
      int v1 = lg.inv[v];
      XBitSet nb = triangulated.neighborSet[v];
      for (int w = nb.nextSetBit(v + 1); w >= 0; w = nb.nextSetBit(w + 1)) {
        int w1 = lg.inv[w];
        tr.addEdge(v1, w1);
      }
    }
  }
}
