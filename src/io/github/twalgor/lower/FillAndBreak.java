package io.github.twalgor.lower;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import io.github.twalgor.log.Log;
import io.github.twalgor.common.Edge;
import io.github.twalgor.common.Graph;
import io.github.twalgor.common.LocalGraph;
import io.github.twalgor.common.TreeDecomposition;
import io.github.twalgor.common.XBitSet;
import io.github.twalgor.common.Minor;
import io.github.twalgor.decomposer.SemiPID;
import io.github.twalgor.decomposer.SemiPIDFull;
import io.github.twalgor.main.Shared;
import io.github.twalgor.acsd.ACSDecomposition;

public class FillAndBreak {
//  static final boolean VERBOSE = true;
  static final boolean VERBOSE = false;
//  static final boolean TRACE = true;
  static final boolean TRACE = false;
//  static final boolean VERIFY = true;

  static final int BASE_SIZE = 70;
//  static final int NMAX_FOR_DP = 60;
//  static final int MAX_UNCONT = 30;
  public static final int UNC_CHUNK = 5;
  public static final int N_TRY = 100;
  static final int VERSION = 1;

  Graph g;
  public int lb;
  public Minor obs;

  Shared shared;
  
  TreeDecomposition td;

  Random random;

  long t0;
  

  static Log log;

  public FillAndBreak(Graph g, Shared shared) {
    this.g = g;
    this.shared = shared;
    random = new Random(1);
  }

  TreeDecomposition decomopse() {
    t0 = System.currentTimeMillis();

    BalancedContractionLB bclb = new BalancedContractionLB(g, BASE_SIZE);
    lb = bclb.lowerbound();
    Minor minor = new Minor(g);
    for (int e : bclb.contractions) {
      minor = minor.contract(minor.map[e / g.n], minor.map[e % g.n]);
    }

    if (VERBOSE) {
      log.log("minor of " + minor.m + " vertices " + ", width = " + lb + ", " + (System.currentTimeMillis() - t0)
          + " millisecs");
      log.log(minor.toString());
    }

    obs = deriveObstruction(minor, new HashSet<Edge>());
    assert SemiPID.decompose(obs.getGraph()).width == lb;

    if (VERBOSE) {
      log.log("obstruction: " + obs + ", " + (System.currentTimeMillis() - t0) + " millisecs");
    }

    while (true) {
      Minor newObs = improve();
      if (newObs == null) {
        return td;
      }
      lb = SemiPID.decompose(newObs.getGraph()).width;
      if (VERBOSE) {
        log.log("new lb: " + lb + ", " + (System.currentTimeMillis() - t0) + " millisecs");
        log.log("obstruction: " + newObs);
        log.close();
      }
      obs = newObs;
    }
  }
  
  int lowerBound(int upperBound) {
    t0 = System.currentTimeMillis();

    BalancedContractionLB bclb = new BalancedContractionLB(g, BASE_SIZE);
    lb = bclb.lowerbound();
    Minor minor = new Minor(g);
    for (int e : bclb.contractions) {
      minor = minor.contract(minor.map[e / g.n], minor.map[e % g.n]);
    }

    if (VERBOSE) {
      log.log("minor of " + minor.m + " vertices " + ", width = " + lb + ", " + (System.currentTimeMillis() - t0)
          + " millisecs");
      log.log(minor.toString());
    }

    obs = deriveObstruction(minor, new HashSet<Edge>());
    assert SemiPID.decompose(obs.getGraph()).width == lb;

    if (VERBOSE) {
      log.log("obstruction: " + obs + ", " + (System.currentTimeMillis() - t0) + " millisecs");
    }

    while (lb < upperBound) {
      Minor newObs = improve();
      if (newObs == null) {
        return lb;
      }
      lb = SemiPID.decompose(newObs.getGraph()).width;
      if (VERBOSE) {
        log.log("new lb: " + lb + ", " + (System.currentTimeMillis() - t0) + " millisecs");
        log.log("obstruction: " + newObs);
        log.close();
      }
      obs = newObs;
    }
    return lb;
  }
  
  public int initialLowerBound() {
    ContractionLB clb = new ContractionLB(g);
    lb = clb.lowerbound();

    Minor minor = clb.boundingMinor;

    if (VERBOSE) {
      log.log("minor of " + minor.m + " vertices " + ", width = " + lb + ", " + (System.currentTimeMillis() - t0)
          + " millisecs");
      log.log(minor.toString());
    }

    obs = deriveObstruction(minor, new HashSet<Edge>());
    return lb;

  }
  
  public int improvedLowerBound() {
    Minor newObs = improve();
    if (newObs != null) {
      lb = SemiPID.decompose(newObs.getGraph()).width;
      obs = newObs;
      return lb;
    } else
      return -1;
  }

  Minor improve() {
    Graph h = obs.getGraph();
    if (h.isClique(h.all)) {
      Edge[] contracteds = obs.contractionEdges();
      obs = new Minor(g);
      for (int i = 0; i < contracteds.length - 1; i++) {
        obs = obs.contract(obs.map[contracteds[i].u], obs.map[contracteds[i].v]);
      }
    }
    return lift(obs, new HashSet<>());
  }
  
  Minor lift(Minor minor, Set<Edge> filled) {
    if (lb == shared.getUB()) {
      return null;
    }
    if (TRACE) {
      System.out.println(spaces(filled.size()) + "lifting minor of " + minor.m + " vertices");
    }
    Graph h = getGraph(minor, filled);
    assert !h.isClique(h.all);
    int v1 = -1;
    int minDeg = 0;
    for (int v = 0; v < h.n; v++) {
      if (v1 == -1 || h.neighborSet[v].cardinality() < minDeg) {
        v1 = v;
        minDeg = h.neighborSet[v].cardinality();
      }
    }
    XBitSet nnb = h.all.subtract(h.neighborSet[v1]);
    nnb.clear(v1);
    assert !nnb.isEmpty();
    int v2 = -1;
    minDeg = 0;
    for (int v = nnb.nextSetBit(0); v >= 0; v = nnb.nextSetBit(v + 1)) {
      if (v2 == -1 || h.neighborSet[v].cardinality() < minDeg) {
        v2 = v;
        minDeg = h.neighborSet[v].cardinality();
      }
    }

    int u1 = minor.invMap[v1];
    int u2 = minor.invMap[v2];
    
    Set<Edge> filled1 = new HashSet<>(filled);
    
    filled1.add(new Edge(u1, u2, g.n));

    if (isFeasible(getGraph(minor, filled1), lb)) {
      minor = lift(minor, filled1);
      if (minor == null) {
        return null;
      }
    }
    assert !isFeasible(getGraph(minor, filled1), lb);

    FillBreaker fb = new FillBreaker(minor, u1, u2, filled);
    return fb.breakFill();
  }
 
  Graph getGraph(Minor minor, Set<Edge> filled) {
    Graph h = minor.getGraph().copy();
    for (Edge e: filled) {
      assert minor.map[e.u] != minor.map[e.v];
      h.addEdge(minor.map[e.u], minor.map[e.v]);
    }
    return h;
  }
  
  String spaces(int n) {
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < n; i++) {
      sb.append(" ");
    }
    return sb.toString();
  }

  boolean isFeasible(Graph h, int k) {
    if (h.n <= k + 1) {
      return true;
    }
    SemiPID spid = new SemiPID(h, k, false);
    boolean isFeasible = spid.isFeasible();
    if (TRACE) {
      SemiPIDFull spidfull = new SemiPIDFull(h, k);
      spidfull.computeSafeSeps();
      assert isFeasible == !spidfull.safeSeps.isEmpty();
    }
    return spid.isFeasible();
  }
  
  Minor deriveObstruction(Minor minor, Set<Edge> filled) {
    Graph h = getGraph(minor, filled);
    int k = SemiPID.decompose(h).width;
  
    Set<Edge> uncontractables = new HashSet<>();
    Minor mm = minor;
    
    while (true) {
      h = getGraph(mm, filled);
      assert !isFeasible(h, k - 1);

      if (h.n == k + 1) {
        assert h.isClique(h.all);
        return mm;
      }

      ArrayList<Edge> edges = new ArrayList<>();
      for (int v = 0; v < h.n; v++) {
        XBitSet nb = h.neighborSet[v];
        for (int w = nb.nextSetBit(v + 1); w >= 0; w = nb.nextSetBit(w + 1)) {
          if (inFillEdge(v, w, mm, filled)) {
            continue;
          }
          Edge e = originalEdge(v, w, mm);
          if (!uncontractables.contains(e)) {
            edges.add(e);
          }
        }
      }

      if (VERBOSE) {
        log.log(edges.size() + " candidate edges for contraction, n = " + h.n);
      }

      Minor mm1 = null;
      for (Edge e : edges) {
        mm1 = mm.contract(mm.map[e.u], mm.map[e.v]);
        if (!isFeasible(getGraph(mm1, filled), k - 1)) {
          break;
        }
        else {
          uncontractables.add(e);
          mm1 = null;
        }
      }
      if (mm1 != null) {
        mm = mm1;
      }
      else {
        return mm;
      }
    }
  }
  
  Edge originalEdge(int v, int w, Minor mm) {
    XBitSet c1 = mm.components[v];
    XBitSet c2 = mm.components[w];
    for (int x = c1.nextSetBit(0); x >= 0; x = c1.nextSetBit(x + 1)) {
      for (int y = c2.nextSetBit(0); y >= 0; y = c2.nextSetBit(y + 1)) {
        if (g.areAdjacent(x, y)) {
          return new Edge(x, y, g.n);
        }
      }
    }
    assert false;
    return null;
  }

  boolean inFillEdge(int v, int w, Minor minor, Set<Edge> filled) {
    for (Edge e: filled) {
      int x = minor.map[e.u];
      int y = minor.map[e.v];
      if (x == v && y == w || x == w && y == v) {
        return true;
      }
    }
    return false;
  }

  class FillBreaker {
    Minor minor;
    Graph g;
    Graph h;
    int s;
    int t;
    Set<Edge> filled;
    Edge[] contracted;
    XBitSet fullCont;

    
    FillBreaker(Minor minor, int s, int t, Set<Edge> filled) {
      this.minor = minor;
      this.s = s;
      this.t = t;
      this.filled = filled;
      g = minor.g;
      h = getGraph(minor, filled);
      
      h.addEdge(minor.map[s], minor.map[t]);
      assert !isFeasible(h, lb);
      h.removeEdge(minor.map[s], minor.map[t]);
    }

    Minor breakFill() {
      if (lb == shared.getUB()) {
        return null;
      }
      if (TRACE) {
        System.out.println(indent() + "breaking fill: " + g.numberOfEdges() + ", " + h.n + 
            ", s = " + s + ", t = " + t);
      }
      contracted = minor.contractionEdges();
      fullCont = XBitSet.all(contracted.length);
  
      XBitSet uncont = new XBitSet();
      
      while (feasible(uncont)) {
        if (lb == shared.getUB()) {
          return null;
        }
        if (TRACE) {
          System.out.println(indent() + "uncont: " + uncont);
        }
        XBitSet best = null;
        int nstbBest = 0;
//        int nSSBest = 0;
        XBitSet rem = fullCont.subtract(uncont);
        for (int i = 0; i < N_TRY; i++) {
          int sampleSize = Math.min(UNC_CHUNK, rem.cardinality());
          XBitSet addition = randomSubset(rem, sampleSize);
          XBitSet uncont1 = uncont.unionWith(addition);
          if (!feasible(uncont1)) {
            best = uncont1;
            break;
          }
        int nstb1 = nSepsToBreak(uncont1);
//          int nSS1 = computeNSS(uncont1);
          if (best == null || nstb1 < nstbBest) {
//          if (best == null || nSS1 < nSSBest) {
            best = uncont1;
            nstbBest = nstb1;
//            nSSBest = nSS1;
            if (TRACE) {
              System.out.println(indent() + i + ": better uncont " + nstbBest + ", " + best);
//              System.out.println(indent() + i + ": better uncont " + nSSBest + ", " + best);
            }
          }
        }
        uncont = best;
      }
      Minor minor = contract(g, fullCont.subtract(uncont));
      return deriveObstruction(minor, filled);
    }
    
    String indent() {
      return spaces(filled.size());
    }

    int computeNSS(XBitSet uncont) {
      Minor minor = contract(g, fullCont.subtract(uncont));
      Graph h = getGraph(minor, filled);
      SemiPIDFull spidfull = new SemiPIDFull(h, lb);
      spidfull.computeSafeSeps();
      return spidfull.safeSeps.size();
    }

    int nSepsToBreak(XBitSet uncont) {
      Minor minor = contract(g, fullCont.subtract(uncont));
      Graph h = getGraph(minor, filled);
      SemiPIDFull spidfull = new SemiPIDFull(h, lb);
      spidfull.computeSafeSeps();
      
      int count = 0;
      for(XBitSet sep: spidfull.safeSeps) {
//        if (sep.cardinality() == lb) {
          if (crosses(minor.map[s], minor.map[t], sep, h)) {
            count++;
          }
//        }
      }
      return count;
    }
    
    boolean crosses(int u, int v, XBitSet sep, Graph h) {
      if (sep.get(u) || sep.get(v)) {
        return false;
      }
      ArrayList<XBitSet> components = h.separatedComponents(sep);
      for (XBitSet compo: components) {
        if (compo.get(u) && compo.get(v)) {
          return false;
        }
      }
      return true;
    }
    
    boolean feasible(XBitSet uncont) {
      Minor minor = contract(g, fullCont.subtract(uncont));
      return isFeasible(getGraph(minor, filled), lb);
    }
    
    Minor contract(Graph g, XBitSet conts) {
      Minor minor = new Minor(g);
      for (int i = conts.nextSetBit(0); i >= 0; i = conts.nextSetBit(i + 1)) {
        minor = minor.contract(minor.map[contracted[i].u], minor.map[contracted[i].v]);
      }
      return minor;
    }
    

    XBitSet randomSubset(XBitSet set, int k) {
      XBitSet result = new XBitSet();
      int n = set.cardinality();
      int j = k;
      for (int i = set.nextSetBit(0); i >= 0; i = set.nextSetBit(i+1)) {
        if (random.nextInt(n) < j) {
          result.set(i);
          j--;
        }
        n--;
      }
      return result; 
    }
  }

  
  XBitSet randomSubset(XBitSet set, int k) {
    XBitSet result = new XBitSet();
    int n = set.cardinality();
    int j = k;
    for (int i = set.nextSetBit(0); i >= 0; i = set.nextSetBit(i+1)) {
      if (random.nextInt(n) < j) {
        result.set(i);
        j--;
      }
      n--;
    }
    return result; 
  }
}
