package io.github.twalgor.common;

import java.util.Arrays;

public class VDP {
//  private static boolean VERBOSE = true;
private static boolean VERBOSE = false;
//  private static boolean DEBUG = true;
private static boolean DEBUG = false;
  private Graph g;
  Vertex[] vertices;
  Edge[] edgeFromSource;
  Vertex source;
  int nPath;
  
  public VDP(Graph g) {
    this.g = g;
    vertices = new Vertex[g.n];

    for (int v = 0; v < g.n; v++) {
      vertices[v] = new Vertex(v);
      vertices[v].inEdges = new Edge[g.neighborSet[v].cardinality()];
      vertices[v].outEdges = new Edge[g.neighborSet[v].cardinality()];
    }
    for (int v = 0; v < g.n; v++) {
      Vertex s = vertices[v];
      for (int u = g.neighborSet[v].nextSetBit(0); u >= 0; 
          u = g.neighborSet[v].nextSetBit(u + 1)) {
        if (v < u) {
          Vertex t = vertices[u];
          Edge e = new Edge(s, t);
          Edge f = new Edge(t, s);
          e.reverse = f;
          f.reverse = e;
          s.outEdges[s.outDegree++] = e; 
          t.inEdges[t.inDegree++] = e;
          s.inEdges[s.inDegree++] = f; 
          t.outEdges[t.outDegree++] = f;
        }
      }
    }
    boolean flag = false;
    for (int v = 0; v < g.n; v++) {
      Vertex s = vertices[v];
      if (s.inEdges.length != s.inDegree) {
        System.err.println(v + " in:" + s.inDegree + ", " + 
            s.inEdges.length);
        flag = true;
      }
      if (s.outEdges.length != s.outDegree) {
        System.err.println(v + " out:" + s.outDegree + ", " + 
            s.outEdges.length);
        flag = true;
      }
    }
    if (flag) g.writeTo(System.err);
  }

  public void setUp(XBitSet target, XBitSet available) {
    for (int i = 0; i < g.n; i++) {
      Vertex v = vertices[i];
      v.present = available.get(i);
      v.inTarget = target.get(i);
    }
    clearPaths();
  }
  
  public void setSource(XBitSet sourceNeighbors) {
    int w = sourceNeighbors.cardinality();
    source = new Vertex(g.n);
    source.outDegree = w;
    source.outEdges = new Edge[w];
    
    w = 0;
    for (int i = sourceNeighbors.nextSetBit(0); i >= 0; 
        i = sourceNeighbors.nextSetBit(i + 1)) {
      Vertex v = vertices[i];
      Edge e =  new Edge(source, v);
      e.present = true;
      source.outEdges[w++] = e; 
    }
  }
  
  public void addSourceNeighbors(XBitSet sourceNeighbors) {
    int w = sourceNeighbors.cardinality();
    int j = source.outDegree;
    source.outDegree += w;
    source.outEdges = Arrays.copyOf(source.outEdges, source.outDegree);
    
    for (int i = sourceNeighbors.nextSetBit(0); i >= 0; 
        i = sourceNeighbors.nextSetBit(i + 1)) {
      Vertex v = vertices[i];
      Edge e =  new Edge(source, v);
      e.present = true;
      source.outEdges[j++] = e; 
    }
  }
  
  public void setCapacity(XBitSet vs, int capacity) {
    for (int i = vs.nextSetBit(0); i >= 0; i = vs.nextSetBit(i + 1)) {
      Vertex v = vertices[i];
      v.capacity = capacity;
    }
  }
  
  public int minCutSize() {
    int k = 0;
    while (true) {
      Edge[] ap = findAugmentingPath();
      if (ap == null) {
        return k;
      }
      else {
        if (VERBOSE) {
          System.out.println("augmenting path found");
          for (Edge e: ap) {
            System.out.print(e + " ");
          }
          System.out.println();
        }
        augment(ap);
        k++;
      }
    }
  }
  
  public int maxUsed() {
    int max = 0;
    for (Vertex v: vertices) {
      if (v.used > max) {
        max = v.used;
      }
    }
    return max;
  }
  
  public boolean minCutSizeAtMost(int w) {
    int k = 0;
    while (w <= k) {
      Edge[] ap = findAugmentingPath();
      if (ap == null) {
        return true;
      }
      else {
        if (VERBOSE) {
          System.out.println("augmenting path found");
          for (Edge e: ap) {
            System.out.print(e + " ");
          }
          System.out.println();
        }
        augment(ap);
        w++;
      }
    }
    return false;
  }
  
  public XBitSet minCut() {
    while (true) {
      Edge[] ap = findAugmentingPath();
      if (ap != null) {
        if (VERBOSE) {
          System.out.println("augmenting path found");
          for (Edge e: ap) {
            System.out.print(e + " ");
          }
          System.out.println();
        }
        augment(ap);
      }
      else {
        XBitSet cut = new XBitSet(g.n);
        getCut(cut);
        return cut;
      }
    }
  }
  
  private void getCut(XBitSet cut) {
    for (Vertex v: vertices) {
      if (v.present && v.inMark && !v.outMark) {
        cut.set(v.id);
      }
    }
  }
  
  public int connectivity() {
    int c = 0;
    while (true) {
      Edge[] ap = findAugmentingPath();
      if (ap != null) {
        if (VERBOSE) {
          System.out.println("augmenting path found");
          for (Edge e: ap) {
            System.out.print(e + " ");
          }
          System.out.println();
        }
        augment(ap);
        c++;
      }
      else {
        return c;
      }
    }
  }
  
  private Edge[] findAugmentingPath() {
    clearMarks();
    Edge[] path = new Edge[g.numberOfEdges() * 2];
    for (Edge e: source.outEdges) {
      path[0] = e;
      Edge[] ap = findAugmentingPath(true, path, 1);
      if (ap != null) {
        return ap;
      }
    }
    return null;
  }
  
  private Edge[] findAugmentingPath(boolean forward, Edge[] path, int np) {
//    if (VERBOSE) {
    if (DEBUG) {
      System.out.print(forward);
      for (int i = 0; i < np; i++) {
        Edge e = path[i];
        System.out.print(" " + e);
      }
      System.out.println(path[np-1].t);
    }
    Vertex v = path[np - 1].t;
    if (forward && v.inTarget) {
//      Edge[] result = simpleAugmentingPath(path, np);
      Edge[] result = Arrays.copyOf(path, np);
      return result;
    }
    if (!v.present) {
      return null;
    }
    
    if (forward && v.inMark ||
        !forward && v.outMark) {
      return null;
    }
    
    if (forward) {
      v.inMark = true;
      if (v.used < v.capacity) {
        v.outMark = true;
      }
    }
    else {
      v.outMark = true;
      if (v.used > 0) {
        v.inMark = true;
      }
    }
    
    if (v.outMark) {
      for (Edge out: v.outEdges) {
        path[np] = out;
        Edge[] result = findAugmentingPath(true, path, np + 1);
        if (result != null) {
          return result;
        }
      }
    }
    if (v.inMark) {
      for (Edge in: v.outEdges) {
        if (in.reverse.used > 0) {
          path[np] = in;
          Edge[] result = findAugmentingPath(false, path, np + 1);
          if (result != null) {
            return result;
          }
        }
      }
    }

    return null;
  }
  
  private Edge[] simpleAugmentingPath(Edge[] path, int np) {
    int m = 0;
    int i = 0;
    Vertex v = path[0].s;
    while (i < np) {
      if (v == path[i].s) { 
        i = np - 1;
        while (path[i].s != v) {
          i--;
        }
      }
      path[m] = path[i];
      if (path[m].s == v) {
        v = path[m].t;
      }
      else {
        v = path[m].s;
      }
      m++;
      i++;
    }
    return Arrays.copyOf(path, m);
  }
  
  private void augment(Edge[] ap) {
    for (Edge edge: ap) {
      if (edge.reverse == null || edge.reverse.used == 0) {
        // reverse edge
        edge.used++;
        if (!edge.t.inTarget) {
          edge.t.used++;
        }
      }
      else {
        edge.s.used--;
        edge.reverse.used--;
      }
    }
    nPath++;
  }

  private void clearMarks() {
    for (Vertex v: vertices) {
      v.inMark = false;
      v.outMark = false;
    }
  }
  
  private void clearPaths() {
    nPath = 0;
    for (Vertex v: vertices) {
      v.used = 0;
      for (Edge e: v.outEdges) {
        e.used = 0;
      }
    }
  }
  
  private class Vertex {
    int id;
    boolean present;
    boolean inTarget;
    int inDegree;
    int outDegree;
    Edge[] inEdges;
    Edge[] outEdges;
    
    int capacity;
    int used;
    
    boolean inMark, outMark;
    
    private Vertex(int id) {
      this.id = id;
    }
    
    public String toString() {
      return id + "(" + used + "/" + capacity + "," + inMark + "," + outMark + ")";
    }
  }
  
  private class Edge {
    Vertex s, t;
    Edge reverse;
    boolean present;
    int used;
    
    private Edge(Vertex s, Vertex t) {
      this.s = s;
      this.t = t;
    }
    
    public String toString() {
      return "(" + s.id + "," + t.id + ")";
    }
  }
  
}
