package io.github.twalgor.common;

import java.util.Arrays;

public class Subgraph {
  Graph g;
  XBitSet vertices;
  public int[] conv;
  public int[] inv;
  public Graph h;
  
  public Subgraph(Graph g, XBitSet vertices) {
    this.g = g;
    this.vertices = vertices;
    
    conv = new int[g.n];
    inv = new int[vertices.cardinality()];
    Arrays.fill(conv, -1);
    int w = 0;
    for (int v = vertices.nextSetBit(0); v >= 0; v = vertices.nextSetBit(v + 1)) {
      inv[w] = v;
      conv[v] = w++;
    }

    h = new Graph(w);
    h.inheritEdges(g, conv, inv);
  }

}
