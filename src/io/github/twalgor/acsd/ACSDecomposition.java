package io.github.twalgor.acsd;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import io.github.twalgor.common.Chordal;
import io.github.twalgor.common.Graph;
import io.github.twalgor.common.TreeDecomposition;
import io.github.twalgor.common.XBitSet;
import io.github.twalgor.decomposer.SemiPID;
import io.github.twalgor.greedy.MCS_M;
import io.github.twalgor.greedy.MMAF;
import io.github.twalgor.greedy.MMD;
import io.github.twalgor.lower.ContractionLB;

public class ACSDecomposition {
  static final boolean TRACE = false;

  Graph g;
  MTAlg mtAlg;
  public Graph h;
  public Set<XBitSet> acAtoms;
  Set<XBitSet> filledSeps;
  int width;
  
  public enum MTAlg {mcs, mmd, mmaf}

  public ACSDecomposition(Graph g, MTAlg mtAlg) {
    this.g = g;
    this.mtAlg = mtAlg;
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

  Set<XBitSet> candidateSeps() {
    Graph t = g.copy();
    switch (mtAlg) {
    case mcs: 
      MCS_M mcs = new MCS_M(t);
      mcs.triangulate();
      break;
    case mmaf: 
      MMAF mmaf = new MMAF(t);
      mmaf.triangulate();
      break;
    case mmd:
      MMD mmd = new MMD(t);
      mmd.triangulate();
      break;

    }
    Chordal chordal = new Chordal(t);
    return chordal.minimalSeparators();
  }
  
  public void decomposeByACS() {
    Set<XBitSet> remainingSeps = candidateSeps();
    h = g.copy();
    
    filledSeps = new HashSet<>();
    
    boolean going = true;
    while (going) {
      going = false;
      Set<XBitSet> rem = new HashSet<>();
      for (XBitSet sep: remainingSeps) {
        if (almostClique(sep, h)) {
          h.fill(sep);
          filledSeps.add(sep);
          going = true;
        }
        else {
          rem.add(sep);
        }
      }
      remainingSeps = rem;
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
  
  void exportEdges(Graph f, int[] inv, Graph h) {
    for (int v = 0; v < f.n; v++) {
      XBitSet nb = f.neighborSet[v].convert(inv);
      h.neighborSet[inv[v]].or(nb);
    }
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
  
  String summary() {
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
