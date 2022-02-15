package io.github.twalgor.common;

public class Edge implements Comparable<Edge> {
  public int u;
  public int v;
  public int n;
  
  public Edge(int u, int v, int n) {
    if (u < v) {
      this.u = u;
      this.v = v;
    }
    else {
      this.u = v;
      this.v = u;
    }
    this.n = n;
  }
  
  @Override
  public int hashCode() {
    return u * n + v;
  }
  
  @Override
  public String toString() {
    return "(" + u + ", " + v + ")";
  }

  @Override
  public boolean equals(Object x) {
    Edge e = (Edge) x;
    return u == e.u && v == e.v;
  }
  
  @Override
  public int compareTo(Edge e) {
    if (u != e.u) {
      return u - e.u;
    }
    return v - e.v;
  }
}