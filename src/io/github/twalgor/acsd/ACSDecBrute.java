package io.github.twalgor.acsd;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import io.github.twalgor.common.Chordal;
import io.github.twalgor.common.Graph;
import io.github.twalgor.common.Subgraph;
import io.github.twalgor.common.TreeDecomposition;
import io.github.twalgor.common.XBitSet;
import io.github.twalgor.decomposer.SemiPID;
import io.github.twalgor.greedy.MMD;
import io.github.twalgor.lower2.ContractionLB;

public class ACSDecBrute {
//  static final boolean TRACE = true;
  static final boolean TRACE = false;

//  static final boolean DEBUG = true;
  static final boolean DEBUG = false;
  Graph g;
  Graph h;
  public Set<XBitSet> acAtoms;
  Set<XBitSet> filledSeps;
  int width;

  public ACSDecBrute(Graph g) {
    this.g = g;
  }
  
  TreeDecomposition decomposeByDP(Graph f) {
    for (int k = lowerbound(f); k < f.n; k++) {
      SemiPID spid = new SemiPID(f, k, false);
      TreeDecomposition td = spid.decompose();
      if (td != null) {
        return td;
      }
    }
    return null;
  }
  
  int lowerbound(Graph f) {
    ContractionLB clb = new ContractionLB(f);
    return clb.lowerbound();
  }
  
  public void decomposeByACS() {
    h = g.copy();
    
    filledSeps = new HashSet<>();
    boolean going = true;
    while (going) {
      going = false;
      for (int v = 0; v < h.n; v++) {
        Subgraph sub = new Subgraph(h, h.all.removeBit(v));
        Set<XBitSet> cliqueSeps = cliqueSeps(sub.h);
        if (TRACE) {
          System.out.println(cliqueSeps.size() + " clique seps for v = " + v);
        }
        
        for (XBitSet sep: cliqueSeps) {
          XBitSet sep1 = sep.convert(sub.inv).addBit(v);
          if (h.fullComponents(sep1).size() < 2) {
            continue;
          }
          if (!h.isClique(sep1)) {
            going = true;
          }
          h.fill(sep1);
          filledSeps.add(sep1);
        }
      }
      if (TRACE) {
        System.out.println(filledSeps.size() + " filledSeps ");
      }
    }
    
    acAtoms = new HashSet<>();
    Set<XBitSet> cliqueSeparateds = new HashSet<>();
    for (XBitSet sep: filledSeps) {
      assert h.isClique(sep);
      ArrayList<XBitSet> components = h.separatedComponents(sep);
      for (XBitSet compo: components) {
        cliqueSeparateds.add(compo);
      }
    }
    cliqueSeparateds.add(h.all);
    
    XBitSet[] csa = new XBitSet[cliqueSeparateds.size()];
    cliqueSeparateds.toArray(csa);
    Arrays.sort(csa, XBitSet.cardinalityComparator);
    
    XBitSet remaining = (XBitSet) h.all.clone();
    for (XBitSet compo: csa) {
      XBitSet rCompo = compo.intersectWith(remaining);
      if (!rCompo.isEmpty()) {
        XBitSet sep = h.neighborSet(compo);
        assert h.isClique(sep): compo + ":" + sep;
        if (sep.isSubset(remaining)) {
          acAtoms.add(rCompo.unionWith(sep));
        }
      }
      remaining.andNot(rCompo);
    }
  }
  
  private Set<XBitSet> cliqueSeps(Graph h) {
    Graph t = h.copy();
    MMD mdd = new MMD(t);
    mdd.triangulate();
    Chordal chordal = new Chordal(t);
    Set<XBitSet> minSeps = chordal.minimalSeparators();
    Set<XBitSet> result = new HashSet<>();
    for (XBitSet sep: minSeps) {
      if (h.isClique(sep)) {
        result.add(sep);
      }
    }
    return result;
  }

  boolean almostClique(XBitSet sep, Graph h) {
    for (int v = sep.nextSetBit(0); v >= 0; v = sep.nextSetBit(v + 1)) {
      if (h.isClique(sep.removeBit(v))) {
        return true;
      }
    }
    return false;
  }

  void verify() {
    for (XBitSet atom: acAtoms) {
      ArrayList<XBitSet> components = h.separatedComponents(atom);
      for (XBitSet compo: components) {
        assert h.isClique(h.neighborSet(compo)): atom + ", " + compo + "\n" + h.neighborSet(compo)
        + filledSeps.contains(h.neighborSet(compo));
      }
    }
  }
  
  public String summary() {
    StringBuilder sb = new StringBuilder();
    sb.append("atoms " + acAtoms.size());
    sb.append(" largest " + largestAtomSize());
    return sb.toString();
  }

  int largestAtomSize() {
    int largest = 0;
    for (XBitSet atom: acAtoms) {
      if (atom.cardinality() > largest) {
        largest = atom.cardinality();
      }
    }
    return largest;
  }
}
