package io.github.twalgor.decomposer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import io.github.twalgor.common.Graph;
import io.github.twalgor.common.TreeDecomposition;
import io.github.twalgor.common.XBitSet;
import io.github.twalgor.minseps.MinSepsGenerator;
import io.github.twalgor.sieve.SubblockSieve;

public class SemiPIDFull {
  static final int LINE_LENGTH =50;
//  static final boolean VALIDATE = true;
  static final boolean VALIDATE = false;
//  static final boolean CONSTRUCT_TD = true;
//static final boolean CONSTRUCT_TD = false;
  static final boolean VERBOSE = true;
//  static final boolean VERBOSE = false;
//  static final boolean TRACE = true;
  static boolean TRACE = false;
//  static final boolean TRACE_ROOT = true;
  static final boolean TRACE_ROOT = false;

  static final boolean CONSTRUCT_TD = false;
  
  Graph g;
  String graphName;
  int k;
  public Set<XBitSet> minSeps;
  boolean pmcOnly;
  Map<XBitSet, XBitSet> feasiblesMap;
  ArrayList<XBitSet> minSeparateds;
  
  SubblockSieve[] sieve;
  public Set<XBitSet> safeSeps;

  public SemiPIDFull(Graph g, int k) {
    this.g = g;
    this.k = k;
    MinSepsGenerator msg = new MinSepsGenerator(g, k);
    msg.generate();
    minSeps = msg.minSeps;
  }
  
  public void computeSafeSeps() {
    safeSeps = new HashSet<>();
    if (k >= g.n - 1) {
      return;
    }
    
    dp();

    for (XBitSet sep: minSeps) {
      if (isAllFeasible(sep)) {
        safeSeps.add(sep);
      }
    }
    return;
  }

  public void dp() {
    minSeparateds = new ArrayList<>();
    
    for (XBitSet sep: minSeps) {
      ArrayList<XBitSet> fulls = g.fullComponents(sep);
      for (XBitSet full: fulls) {
        minSeparateds.add(full);
      }
    }

    minSeparateds.sort(XBitSet.cardinalityComparator);
    
    feasiblesMap = new HashMap<>();

    sieve = new SubblockSieve[g.n];
    for (int v = 0; v < g.n; v++) {
      sieve[v] = new SubblockSieve(g, k + 1);
    }

    for (XBitSet component: minSeparateds) {
      XBitSet sep = g.neighborSet(component);
      XBitSet cap = findCap(component, sep, null);
      if (cap != null) {
        if (TRACE) {
          System.out.println(indent(component) + 
                "block: " + component);
        }
        feasiblesMap.put(component, cap);
        sieve[component.nextSetBit(0)].add(component, sep);
      }
    }
  }
  
  boolean isAllFeasible(XBitSet cap) {
    ArrayList<XBitSet> components = g.componentsOf(g.all.subtract(cap));
    for (XBitSet compo: components) {
      if (feasiblesMap.get(compo) == null) {
        return false;
      }
    }
    return true;
  }

  XBitSet findCap(XBitSet component, XBitSet sep, XBitSet knownCap) {
    if (TRACE || knownCap != null && sep.isSubset(knownCap)) {
      System.out.println(indent(component) + "findCap " + component + ", " + sep);
      if (knownCap != null) {
        System.out.println(indent(component) + "knownCap is " + knownCap);
      }
    }
    if (component.cardinality() + sep.cardinality() <= k + 1 &&
        g.isClique(component)) {
      return component.unionWith(sep);
    }
    int v0 = component.nextSetBit(0);

    ArrayList<XBitSet> candidates = sieve[v0].get(component, sep);
    for (XBitSet cand: candidates) {
      XBitSet candSep = g.neighborSet(cand);
      if (TRACE || knownCap != null && sep.isSubset(knownCap)) {
        System.out.println(indent(component) + "cand = " + cand + ", candSep = " + candSep);
      }
      if (sep.isSubset(candSep)) {
        if (otherFullsAllFeasible(candSep, cand, component)) {
          return candSep;  
        }
        else {
          continue;
        }
      }
      XBitSet union = sep.unionWith(candSep);
      assert !union.equals(sep);
      assert union.cardinality() <= k + 1;
      XBitSet cap = tryUnion(component.subtract(cand).subtract(union), union, knownCap);
      if (TRACE || knownCap != null && sep.isSubset(knownCap)) {
        System.out.println(indent(component) + "cap = " + cap);
      }
      if (cap != null) {
        return cap;
      }
    }
    if (TRACE || knownCap != null && sep.isSubset(knownCap)) {
      System.out.println(indent(component) + "try adding " + v0 + " to the bag");
    }
    return tryUnion(component.removeBit(v0), sep.addBit(v0), knownCap);
  }

  boolean  otherFullsAllFeasible(XBitSet candSep, XBitSet cand, XBitSet component) {
    ArrayList<XBitSet> fulls = new ArrayList<>();
    ArrayList<XBitSet> nonFulls = new ArrayList<>();
    g.listComponents(component.subtract(cand).subtract(candSep), candSep, fulls, nonFulls);
    for (XBitSet full: fulls) {
      if (feasiblesMap.get(full) == null) {
        return false;
      }
    }
    return true;
  }

  XBitSet tryUnion(XBitSet scope, XBitSet union, XBitSet knownCap) {
    if (TRACE || knownCap != null && union.isSubset(knownCap)) {
      System.out.println(indent(scope) + "tryUnion0 " + 
          scope + ", " + union);
    }
    ArrayList<XBitSet> fulls = new ArrayList<>();
    ArrayList<XBitSet> nonFulls = new ArrayList<>();
    g.listComponents(scope, union, fulls, nonFulls);
    for (XBitSet compo: nonFulls) {
      if (feasiblesMap.get(compo) == null) {
        if (TRACE || knownCap != null && union.isSubset(knownCap)) {
          System.out.println(indent(scope) + "infeasible, returninig null, compo = " + compo);
        }
        return null;
      }
    }
    if (fulls.isEmpty()) {
      if (TRACE) {
        System.out.println(indent(scope) + "no fulls, returninig " + 
            union);
      }
      if (!pmcOnly || g.isCliquish(union)) {
        return union;
      }
      else {
        return null;
      }
    }
    if (fulls.size() >= 2) {
      for (XBitSet full: fulls) {
        if (feasiblesMap.get(full) == null) {
          if (TRACE) {
            System.out.println(indent(scope) + 
                "infeasible full in at leaste two fulls, returninig null");
          }
          return null;
        }
      }
      if (TRACE) {
        System.out.println(indent(scope) + 
            "at least two fulls, all feasible, returninig " + union);
      }
      return union;
    }
    if (union.cardinality() == k + 1) {
      if (TRACE) {
        System.out.println(indent(scope) + 
            "no room for extending, returninig null");
      }
      return null;
    }
    assert fulls.size() == 1;
    XBitSet full = fulls.get(0);
    return findCap(full, union, knownCap);
  }

  int fillTD(XBitSet bag, XBitSet component, TreeDecomposition td) {
    if (CONSTRUCT_TD) {
      System.out.println("bag = " + bag);
      System.out.println(" component = " + component);
    }
    int r = td.addBag(bag.toArray());
    if (bag.cardinality() > td.width + 1) {
      td.width = bag.cardinality() - 1;
    }
    ArrayList<XBitSet> components = g.componentsOf(component.subtract(bag));
    for (XBitSet compo: components) {
      XBitSet cap = feasiblesMap.get(compo);
      assert cap != null:"compo = " + compo + 
          "\nsep = " + g.neighborSet(compo) + 
          "\nbag = " + bag;
      int b = fillTD(cap, compo, td);
      td.addEdge(r, b);
    }
    return r;
  }
  
  String indent(XBitSet compo) {
    return spaces((g.n - compo.cardinality()) * LINE_LENGTH / g.n);
  }
  
  static String spaces(int n) {
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < n; i++) {
      sb.append(" ");
    }
    return sb.toString();
  }
}

