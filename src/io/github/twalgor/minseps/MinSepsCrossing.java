
package io.github.twalgor.minseps;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import io.github.twalgor.common.Graph;
import io.github.twalgor.common.XBitSet;
import io.github.twalgor.log.Log;

public class MinSepsCrossing {
//  private static boolean VERBOSE = true;
  private static boolean VERBOSE = false;
//  
//  private static boolean DEBUG = true;
  private static boolean DEBUG = false;

  private static int xmasRatio = 10;
  private static File dataFile;
  private static Log log;
  Graph g;
  int width;

  public HashSet<XBitSet> minimalSeparators;
  int maxNSeps;
  int a, b;
  XBitSet aExcluded;
  XBitSet bExcluded;
  XBitSet toCross;
  
  public MinSepsCrossing(Graph g, int width) {
    this.g = g;
    this.width = width;
  }
  
  public void setMaxNSeps(int m) {
    maxNSeps = m;
  }
  public void generateCrossing(XBitSet toCross) {
    if (VERBOSE) {
      System.out.println("generateCrossing: " + toCross);
    }
    this.toCross = toCross;
    minimalSeparators = new HashSet<>();

    Integer[] bCandidate = new Integer[toCross.cardinality()];
    {
      int i = 0;
      for (int v = toCross.nextSetBit(0); v >= 0; v = toCross.nextSetBit(v + 1)) {
        bCandidate[i++] = v;
      }
    }
    
    Arrays.sort(bCandidate, (u, v) -> 
     - g.neighborSet[u].intersectWith(toCross).cardinality()  
     + g.neighborSet[v].intersectWith(toCross).cardinality());
    
    bExcluded = new XBitSet(g.n);
    
    for (int i = 0; i < bCandidate.length; i++) {
      if (maxNSeps != 0 && minimalSeparators.size() >= maxNSeps) {
        return;
      }
      b = bCandidate[i];
      if (VERBOSE) {
        System.out.println("b = " + b + ", " + 
            minimalSeparators.size() + " minimal separators so");
      }

      aExcluded = new XBitSet(g.n);
      aExcluded.set(b);
      aExcluded.or(g.neighborSet[b]);

      Integer[] aCandidate = new Integer[toCross.subtract(aExcluded).cardinality()];
      {
        int j = 0;
        for (int v = toCross.nextSetBit(0); v >= 0; v = toCross.nextSetBit(v + 1)) {
          if (!aExcluded.get(v)) {
            aCandidate[j++] = v;
          }
        }
      }

      Arrays.sort(aCandidate, (u, v) -> 
      - g.neighborSet[u].intersectWith(aExcluded).cardinality()  
      + g.neighborSet[v].intersectWith(aExcluded).cardinality());

      for (int j = 0; j < aCandidate.length; j++) {
        if (maxNSeps != 0 && minimalSeparators.size() >= maxNSeps) {
          return;
        }

        a = aCandidate[j];

        XBitSet aFixed = new XBitSet(new int[]{a});
        
        XBitSet sFixed = g.neighborSet(aFixed).intersectWith(aExcluded);
//        we should be careful in utilizing the vertices fixed in S on b side
//        sFixed.or(g.neighborSet[b].intersectWith(bExcluded));

        branch(aFixed, sFixed, "");
        aExcluded.set(a);
      }
      bExcluded.set(b);
    }
  }
  
  private void branch(XBitSet aFixed, XBitSet sFixed, String indent) {
    if (maxNSeps != 0 && minimalSeparators.size() >= maxNSeps) {
      return;
    }
    if (DEBUG) {
      System.out.println(indent + "branch:" + aFixed + ", " + sFixed);
    }

    XBitSet aCompoNeighbors = g.neighborSet(aFixed);
    
    if (aCompoNeighbors.intersects(aExcluded)) {
      sFixed = sFixed.unionWith(aCompoNeighbors.intersectWith(aExcluded));
    }
    XBitSet toDecide = aCompoNeighbors.subtract(sFixed);

    if (DEBUG) {
      System.out.println(indent + "branching");
      System.out.println(indent + " aFixed " + aFixed);
      System.out.println(indent + " sFixed " + sFixed);
      System.out.println(indent + " toDecide: " + toDecide);
    }
    
    if (sFixed.cardinality() > width) {
      if (DEBUG) {
        System.out.println(indent + "returning null: sFixed > " + width);
      }
      return;
    }
    
    
    XBitSet external = g.all.subtract(aFixed);
    external.andNot(aCompoNeighbors);
    XBitSet internal = toDecide.subtract(g.neighborSet(external));
    
    if (!internal.isEmpty()) {
      aFixed = aFixed.unionWith(internal);
      aCompoNeighbors.andNot(internal);
      toDecide.andNot(internal);
    }
    
    assert aCompoNeighbors.equals(g.neighborSet(aFixed));
    assert aCompoNeighbors.equals(sFixed.unionWith(toDecide));
    
    ArrayList<XBitSet> components = 
        g.separatedComponents(aFixed.unionWith(aCompoNeighbors));
    
    if (components.size() >= 2) {
      XBitSet bCompo = null;
      for (XBitSet compo: components) {
        if (compo.get(b)) {
          bCompo = compo;
        }
      }
      if (bCompo == null) {
        System.out.println("b = " + b + ", aFixed = " + aFixed + ", sFixed = " + sFixed + ", toDecide = " + toDecide);
      }
      assert bCompo!= null;
      XBitSet bCompoNeighbors = g.neighborSet(bCompo);
      if (!sFixed.isSubset(bCompoNeighbors)) {
        if (DEBUG) {
          System.out.println(indent + "bCompo not adjacent with some vertex in sFixed");
        }
        return;
      }
      ArrayList<XBitSet> compos = g.separatedComponents(bCompoNeighbors);
      for (XBitSet compo: compos) {
        if (compo.get(a)) {
          aFixed = compo;
        }
      }

      aCompoNeighbors = g.neighborSet(aFixed);
      toDecide = aCompoNeighbors.subtract(sFixed);
      external = g.all.subtract(aFixed);
      external.andNot(aCompoNeighbors);
      internal = toDecide.subtract(g.neighborSet(external));
        
      if (!internal.isEmpty()) {
        aFixed = aFixed.unionWith(internal);
        toDecide.andNot(internal);
      }
    }
    
    int nd = toDecide.cardinality();
    int nc = aFixed.cardinality();
    int ns = sFixed.cardinality();

    if (ns > width) {
      if (DEBUG) {
        System.out.println(indent + "ns exceeds width");
      }
      return;
    }
    
    if (ns == width) {
      if (isMinimalSeparator(sFixed)) {
        if (DEBUG) {
          System.out.println(indent + "minimal Separator found: " + sFixed);
        }
        minimalSeparators.add((XBitSet) sFixed.clone());
      }
      return;
    }
    
    if (nd == 0) {
      if (DEBUG) {
        System.out.println(indent + "no neihbor vertex to decide");
      }
//      assert isMinimalSeparator(sFixed);
      if (isMinimalSeparator(sFixed)) {
        minimalSeparators.add((XBitSet) sFixed.clone());
      }
      return;
    }

    if ((nd > (width - ns) && 
        (nc + (nd - (width - ns)) * xmasRatio) * 2 + width > g.n)) {
      // xmas check with vertex disjoint paths
      XBitSet rest = g.all.subtract(aFixed);
      rest.clear(b);
      rest.andNot(sFixed);
      rest.andNot(toDecide);
      XBitSet reached = (XBitSet) toDecide.clone();
      XBitSet frontier = (XBitSet) reached.clone();

      int depth = 1;
      assert width > ns;
      while (true) {
        XBitSet newFrontier = new XBitSet(g.n);
        for (int v = frontier.nextSetBit(0); v >= 0; v = frontier.nextSetBit(v + 1)) {
          XBitSet nb = g.neighborSet[v].intersectWith(rest);
          if (!nb.isEmpty()) {
            int w = nb.nextSetBit(0);
            newFrontier.set(w);
            rest.clear(w);
          }
        }
        if (newFrontier.cardinality() < width - ns) {
          break;
        }
        reached.or(newFrontier);
        frontier = newFrontier;
        depth++;
      }
      int addition = reached.cardinality() - depth * (width - ns);
      if ((nc + addition) * 2 + width > g.n) {
        if (DEBUG) {
          System.out.println(indent + "returning null because of the xmass check by vertex disjoint paths " + 
              ns + ", nd = " + nd + ", nc = " + nc + ", width = " + width + ", depth = " + 
              depth + ", reached = " + reached.cardinality());
        }
        return;
      }
    }
    // The size of the final a-component should not exceed the size of another component
    // so, 2 * a  + s <= g.n where a and s are the final separator size and the a-compo size
    // Suppose t of nd are in the final separator, then a >= nc + nd - t and s >= ns + t
    // so, 2 * (nc + nd) + ns - t <= g.n.
    // If ns + nd <= width then t <= nd and otherwise t <= width - ns
    // We have 2 * nc + ns + nd <= g.n in the former case and
    // 2 * (nc + ns + nd) - width <= g.n in the latter case
    if (ns + nd <= width && 2 * nc + ns + nd > g.n
        ||
        ns + nd > width && 2 * (nc + ns + nd) - width > g.n
        ) {
      if (DEBUG) {
        System.out.println(indent + "returning null because of the cardinality constraint: ns = " + 
            ns + ", nd = " + nd + ", nc = " + nc + ", width = " + width);
      }
      return;
    }

    XBitSet bProximity = new XBitSet(g.n);
    XBitSet prev = null;
    bProximity.set(b);
    while (!bProximity.intersects(toDecide)) {
      prev = bProximity;
      bProximity = g.closedNeighborSet(bProximity);
      bProximity.andNot(sFixed);
    }
    XBitSet candidates = bProximity.intersectWith(toDecide);
    int v = 0;
    int max = 0;
    for (int u = candidates.nextSetBit(0); u >= 0; u = candidates.nextSetBit(u + 1)) {
      if (g.neighborSet[u].intersectWith(prev).cardinality() > max) {
        v = u;
        max = g.neighborSet[u].intersectWith(prev).cardinality();
      }
    }

    if (DEBUG) {
      System.out.println(indent + " trying v = " + v + " to decide");
    }

    if (!aExcluded.get(v)) {
      aFixed.set(v);
      branch(aFixed, sFixed, indent + " ");
      aFixed.clear(v);
    }

    sFixed.set(v);
    branch(aFixed, sFixed, indent + " ");
    sFixed.clear(v);
  }

  boolean isMinimalSeparator(XBitSet separator) {
    ArrayList<XBitSet> components = g.separatedComponents(separator);
    int count = 0;
    for (XBitSet compo: components) {
      if (isFullComponent(compo, separator)) {
        count++;
      }
    }
    return count >= 2;
  }

  boolean isFullComponent(XBitSet component, XBitSet sep) {
    for (int v = sep.nextSetBit(0); v >= 0; 
        v = sep.nextSetBit(v + 1)) {
      if (!component.intersects(g.neighborSet[v])) {
        return false;
      }
    }
    return true;
  }

  void printMS() {
    for (XBitSet sep: minimalSeparators) {
      System.out.println(sep);
    }
  }
}
