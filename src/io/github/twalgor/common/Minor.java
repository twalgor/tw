package io.github.twalgor.common;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import io.github.twalgor.common.Graph;
import io.github.twalgor.common.XBitSet;

public class Minor {
  public Graph g;
  public int m;
  public XBitSet[] components;
  public int[] map;
  public int[] invMap;
  public Graph h;
  
  public Minor(Graph g) {
    this.g = g;
    m = g.n;
    components = new XBitSet[g.n];
    map = new int[g.n];
    invMap = new int[g.n];
    for (int i = 0; i < g.n; i++) {
      components[i] = new XBitSet(new int[] {i});
      map[i] = i;
      invMap[i] = i;
    }
    h = toGraph();
  }
  
  
  public Minor(Graph g, XBitSet[] components) {
    this.g = g;
    this.components = components;
    this.m = components.length;
    invMap = new int[m];
    map = new int[g.n];
    Arrays.fill(map, -1); // some vertices of g may not belong to any component
    for (int i = 0; i < m; i++) {
      invMap[i] = components[i].nextSetBit(0);
      for (int v = components[i].nextSetBit(0); v >= 0; 
          v = components[i].nextSetBit(v + 1)) {
        map[v] = i;  
      }
    }
    h = toGraph();
  }
  
  public Minor(Graph g, int m, int[] map) {
    this.g = g;
    this.map = map;
    this.m = m;
    components = new XBitSet[m];
    for (int i = 0; i < m; i++) {
      components[i] = new XBitSet(g.n);
    }
    for (int v = 0; v < g.n; v++) {
      if (map[v] != -1) {
        components[map[v]].set(v);
      }
    }
    invMap = new int[m];
    for (int i = 0; i < m; i++) {
      invMap[i] = components[i].nextSetBit(0);
    }
    h = toGraph();
  }
  
  public Minor contract(int i, int j) {
    if (i > j) {
      return contract(j, i);
    }
    XBitSet union = components[i].unionWith(components[j]);
    XBitSet[] compos = new XBitSet[m - 1];
    for (int h = 0; h < m - 1; h++) {
      if (h == i) {
        compos[h] = union;
      }
      else if (h < j) {
        compos[h] = components[h];
      }
      else {
        compos[h] = components[h + 1];
      }
    }
    return new Minor(g, compos);
  }
  
  public Minor remove(int i) {
    XBitSet[] compos = new XBitSet[m - 1];
    for (int h = 0; h < m - 1; h++) {
      if (h < i) {
        compos[h] = components[h];
      }
      else {
        compos[h] = components[h + 1];
      }
    }
    return new Minor(g, compos);
  }


  public int inverseInto(int v, XBitSet vs) {
    assert v < h.n; 
    XBitSet is = components[v].intersectWith(vs);
    assert is.cardinality() <= 1;
    if (is.isEmpty()) {
      return -1;
    }
    else {
      return is.nextSetBit(0);
    }
  }
  
  public Minor invert(int[] inv, Graph f) {
    XBitSet[] components1 = new XBitSet[m];
    for (int i = 0; i < m; i++) {
      components1[i] = components[i].convert(inv);
    }
    return new Minor(f, components1);
  }
  
  public Graph getGraph() {
    if (h == null) {
      h = toGraph();
    }
    return h;
  }
  
  public Minor composeWith(Minor mm) {
    XBitSet[] components = new XBitSet[m];
    for (int i = 0; i < m; i++) {
      components[i] = new XBitSet(mm.g.n);
      XBitSet c = this.components[i];
      for (int v = c.nextSetBit(0); v >= 0; v = c.nextSetBit(v + 1)) {
        components[i].or(mm.components[v]);
      }
    }
    return new Minor(mm.g, components);
  }
  
  public Minor composeWith(XBitSet body, XBitSet[] contractions) {
    assert pairwiseDisjoint(contractions);
    XBitSet singletons = (XBitSet) body.clone();
    for (XBitSet cont: contractions) {
      XBitSet intersect = cont.intersectWith(body); 
      assert intersect.cardinality() == 1;
      singletons.clear(intersect.nextSetBit(0));
    }
    XBitSet[] newCompos = new XBitSet[body.cardinality()];
    int i = 0;
    for (int v = singletons.nextSetBit(0); v >= 0; v = singletons.nextSetBit(v + 1)) {
      newCompos[i++] = components[v];
    }

    for (int h = 0; h < contractions.length; h++) {
      newCompos[i + h] = new XBitSet(g.n);
      for (int j = contractions[h].nextSetBit(0); j >= 0;
          j = contractions[h].nextSetBit(j + 1)) {
        assert j < m;
        newCompos[i + h].or(components[j]);
      }
    }
    assert pairwiseDisjoint(newCompos);
    return new Minor(g, newCompos);
  }
  
  
  boolean pairwiseDisjoint(XBitSet[] sets) {
    for (int i = 0; i < sets.length; i++) {
      for (int j = i + 1; j < sets.length; j++) {
        if (sets[i].intersects(sets[j])) {
          return false;
        }
      }
    }
    return true;
  }
  
  Graph toGraph() {
    Graph h = new Graph(m);
    for (int i = 0; i < m; i++) {
      XBitSet nb = g.neighborSet(components[i]).convert(map);
      assert nb.length() <= m;
      nb.clear(i);
      h.setNeighbors(i,nb); 
    }
    for (int v = 0; v < h.n; v++) {
      XBitSet nb = h.neighborSet[v];
      for (int w = nb.nextSetBit(0); w >= 0; w = nb.nextSetBit(w + 1)) {
        assert h.neighborSet[w].get(v): v + ":" + w;
      }
    }
    return h;
  }
  
  public int edgeID(int u, int v) {
    int u0 = components[u].nextSetBit(u);
    int v0 = components[v].nextSetBit(u);
    if (u0 < v0) {
      return u0 * g.n + v0;
    }
    else {
      return v0 * g.n + u0;
    }
  }
  
  public Edge[] contractionEdges() {
    Edge[] result = new Edge[g.n - m];
    int ne = 0;
    for (XBitSet compo: components) {
      if (compo.cardinality() >= 2) {
        Edge[] edges = spanningEdges(compo);
        for (int i = 0; i < edges.length; i++) {
          assert edges[i] != null;
          result[ne++] = edges[i];
        }
      }
    }
    assert ne == result.length;
    Arrays.sort(result);
    return result;
  }

  Edge[] spanningEdges(XBitSet compo) {
    int nv = compo.cardinality();
    Edge[] result = new Edge[nv - 1];
    int ne = 0;
    Graph f = new Graph(g.n);
    for (int u = compo.nextSetBit(0); u >= 0; u = compo.nextSetBit(u + 1)) {
      XBitSet nb = g.neighborSet[u].intersectWith(compo);
      for (int v = nb.nextSetBit(u + 1); v >= 0; v = nb.nextSetBit(v + 1)) {
        if (!connected(u, v, f)) {
          f.addEdge(u, v);
          result[ne++] = new Edge(u, v, g.n);
          if (ne == nv - 1) {
            break;
          }
        }
      }
      if (ne == nv - 1) {
        break;
      }
    }
    return result;
  }
  
  boolean connected(int u, int v, Graph f) {
    ArrayList<XBitSet> compoList = f.componentsOf(f.all);
    for (XBitSet compo: compoList) {
      if (compo.get(u) && compo.get(v)) {
        return true;
      }
    }
    return false;
  }
  
  public String toString() {
    String s = "m = " + m + ", e = " + toGraph().numberOfEdges();
    return s + "\n" + count();
  }
  
  public String count() {
    StringBuffer sb = new StringBuffer();
    int[] count = new int[g.n];
    for (int i = 0; i < m; i++) {
      count[components[i].cardinality()]++;
    }
    for (int c = 0; c < g.n; c++) {
      if (count[c] > 0) {
        sb.append("(" + c + ")" + count[c] + " ");
      }
    }
    return sb.toString();
  }

  public void dump() {
    System.out.println("minor with " + m + " vertices" + ", n = " + g.n);
    System.out.println("components");
    for (XBitSet compo: components) {
      System.out.println(" " + compo.cardinality() + ":" + compo);
    }
    System.out.println("adjacencies");
    Graph h = toGraph();
    for (int v = 0; v < m; v++) {
      XBitSet nb = h.neighborSet[v];
      System.out.println(" " + v + ":" + nb);
    }
  }
  
  public int edgeSignature(int u, int v) {
    assert u < m && v < m;
    int u1 = components[u].nextSetBit(0);
    int v1 = components[v].nextSetBit(0);
    if (u1 < v1) {
      return u1 * g.n + v1;
    }
    else {
      return v1 * g.n + u1;
    }
  }
}
