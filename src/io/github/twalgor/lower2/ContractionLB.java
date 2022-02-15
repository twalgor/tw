package io.github.twalgor.lower2;

import java.util.ArrayList;

import io.github.twalgor.common.Graph;
import io.github.twalgor.common.XBitSet;
import io.github.twalgor.heap.Heap;
import io.github.twalgor.heap.Queueable;

public class ContractionLB {
//  static final boolean TRACE = true;
  static final boolean TRACE = false;
  Graph g;
  Vertex[] vertex;
  XBitSet vertices;

  public ContractionLB(Graph g) {
    this.g = g;
    vertex = new Vertex[g.n];
    for (int v = 0; v < g.n; v++) {
      vertex[v] = new Vertex(v, (XBitSet) g.neighborSet[v].clone());
    }
  }
  
  public int lowerbound() {
    int lowerbound = 0;

    vertices = (XBitSet) g.all.clone();

    Heap heap = new Heap(vertices.cardinality());
    for (int i = vertices.nextSetBit(0); i >= 0; i = vertices.nextSetBit(i + 1)) {
      heap.add(vertex[i]);
    }
    
    while (!heap.isEmpty() && vertices.cardinality() > lowerbound + 1) {
      Vertex v0 = (Vertex) heap.removeMin();
      int deg = v0.nb.cardinality();
      if (deg > lowerbound) {
        lowerbound = deg;
      }
      ArrayList<Vertex> minDegVertices = new ArrayList<>();
      minDegVertices.add(v0);
      while (!heap.isEmpty()) {
        Vertex v = (Vertex) heap.removeMin();
        if (v.nb.cardinality() > deg) {
          heap.add(v);
          if (minDegVertices.size() == 1) {
            lowerbound = v.nb.cardinality();
          }
          break;
        }
        else {
          minDegVertices.add(v);
        }
      }
      
      if (deg == 0) {
        if (TRACE) {
          System.out.println("deg = " + deg + ", lowerbound = " + lowerbound);
        }
        for (Vertex v: minDegVertices) {
          vertices.clear(v.id);
        }
        continue;
      }
      
      Vertex best = v0;
      for (Vertex v: minDegVertices) {
        if (v.minCommon() < best.minCommon()) {
          best = v;
        }
      }
      for (Vertex v: minDegVertices) {
        if (v != best) {
          heap.add(v);
        }
      }
      if (TRACE) {
        System.out.println("vertices: " + vertices);
        System.out.println("heap size = " + heap.size());
      }
      int p = best.bestPartner();
      if (TRACE) {
        System.out.println("best = " + best.id + ", deg = " + deg + ", lowerbound = " + lowerbound + 
            ", best partner = " + p);
      }
      heap.remove(vertex[p]);

      XBitSet affected = best.nb.unionWith(vertex[p].nb);
      affected.clear(best.id);
      affected.clear(p);
      
      for (int w = affected.nextSetBit(0); w >= 0;
          w = affected.nextSetBit(w + 1)) {
        heap.remove(vertex[w]);
      }

      best.contractWith(p);
      
      for (int w = affected.nextSetBit(0); w >= 0;
          w = affected.nextSetBit(w + 1)) {
        heap.add(vertex[w]);
      }
      
      heap.add(best);
      
      for (int v = vertices.nextSetBit(0); v >= 0; v = vertices.nextSetBit(v + 1)) {
        assert vertex[v].nb.isSubset(vertices): v + ":" + vertex[v].nb + "\n" +
            vertex[v].nb.subtract(vertices);
      }
    }
    return lowerbound;
  }

  class Vertex implements Queueable{
    int id;
    XBitSet nb;
    int hx;
    
    Vertex (int id, XBitSet nb) {
      this.id = id;
      this.nb = nb;
    }

    int minCommon() {
      int min = nb.cardinality();
      for (int w = nb.nextSetBit(0); w >= 0; w = nb.nextSetBit(w + 1)) {
        if (nb.intersectWith(vertex[w].nb).cardinality() <
            min) {
          min = nb.intersectWith(vertex[w].nb).cardinality();
        }
      }
      return min;
    }
    
    int bestPartner() {
      int best = -1;
      for (int w = nb.nextSetBit(0); w >= 0; w = nb.nextSetBit(w + 1)) {
        if (best == -1 ||
            nb.intersectWith(vertex[w].nb).cardinality() <
            nb.intersectWith(vertex[best].nb).cardinality()) {
          best = w;
        }
      }
      return best;
    }
    
    void contractWith(int partner) {
      vertices.clear(partner);
      nb.or(vertex[partner].nb);
      for (int i = vertex[partner].nb.nextSetBit(0); 
          i >= 0; i = vertex[partner].nb.nextSetBit(i + 1)) {
        vertex[i].nb.clear(partner);
        vertex[i].nb.set(id);
      }
      nb.clear(id);
      assert !nb.get(partner);
    }
    
    @Override
    public int compareTo(Object x) {
      Vertex v = (Vertex) x;      if (nb.cardinality() != v.nb.cardinality()) { 
        return nb.cardinality() - v.nb.cardinality();
      }
      return id - v.id;  
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
