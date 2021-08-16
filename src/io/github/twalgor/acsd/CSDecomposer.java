package io.github.twalgor.acsd;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import io.github.twalgor.common.Chordal;
import io.github.twalgor.common.Graph;
import io.github.twalgor.common.Subgraph;
import io.github.twalgor.common.XBitSet;
import io.github.twalgor.greedy.MMD;

public class CSDecomposer {
//  static final boolean TRACE = true;
  static final boolean TRACE = false;
  static final boolean VERIFY = false;
  
  Graph g;
  public Set<XBitSet> atoms;
  
  public CSDecomposer(Graph g) {
    this.g = g;
  }
  
  public void decompose() {
    Graph t = g.copy();
    MMD mmd = new MMD(t);
    mmd.triangulate();
    Chordal chordal = new Chordal(t);
    chordal.order();
    
    atoms = new HashSet<>();
    
    XBitSet remaining = (XBitSet) g.all.clone();
    XBitSet scanned =new XBitSet(g.n);
    Graph h = g.copy();
    for (int i = 0; i < g.n; i++) {
      int v = chordal.ord[i];
      scanned.set(v);
      XBitSet nb = h.neighborSet[v].subtract(scanned);
      h.fill(nb);
      if (g.isClique(nb)) {
        ArrayList<XBitSet> fulls = g.fullComponents(nb);

        if (fulls.size() >= 2) {
          for (XBitSet full: fulls) {
            if (full.get(v)) {
              assert full.isSubset(scanned);
              atoms.add(full.intersectWith(remaining).unionWith(nb));
              remaining.andNot(full);
            }
          }
        }
      }
    }
    if (!remaining.isEmpty()) {
      atoms.add(remaining);
    }
    if (VERIFY) {
      verify();
    }
  }
  
  void verify() {
    for (XBitSet atom: atoms) {
      verifyAtom(atom);
    }
  }
  
  void verifyAtom(XBitSet atom) {
    ArrayList<XBitSet> components = g.separatedComponents(atom);
    for (XBitSet compo: components) {
      XBitSet sep = g.neighborSet(compo);
      System.out.println("sep = " + sep + " compo = " + compo);
      ArrayList<XBitSet> fulls = g.fullComponents(sep);
      System.out.println(" " + fulls.size() + " full components");
      for (XBitSet full: fulls) {
        System.out.println(" " + full);
      }
      assert g.isClique(sep);
      assert g.fullComponents(sep).size() >= 2;
    }
    Subgraph sub = new Subgraph(g, atom);
    MMD mmd = new MMD(sub.h);
    mmd.triangulate();
    Set<XBitSet> seps = new Chordal(sub.h).minimalSeparators();
    for (XBitSet sep: seps) {
      assert !g.isClique(sep.convert(sub.inv));
    }
  }
}
