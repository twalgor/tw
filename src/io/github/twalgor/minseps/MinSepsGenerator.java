package io.github.twalgor.minseps;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;

import io.github.twalgor.common.Graph;
import io.github.twalgor.common.XBitSet;

public class MinSepsGenerator {
//  static final boolean TRACE = true;
  static boolean TRACE = false;
  Graph g;
  int k;
  XBitSet aExcluded;
  public Set<XBitSet> minSeps;

  int aTarget;
  XBitSet aCompoTarget;
  XBitSet bCompoTarget;
  
  
    
  public MinSepsGenerator(Graph g, int k) {
    this.g = g;
    this.k = k;
    if (TRACE) {
      System.out.println("MInSepsGenerator n = " + g.n + ", k = " + k);
    }
  }
  
  public void generate() {
    minSeps = new HashSet<>();
    Integer[] vertices = new Integer[g.n];
    for (int i = 0; i < g.n; i++) {
      vertices[i] = i;
    }

    Arrays.sort(vertices, new NeighborSizeComparator());

    aExcluded = new XBitSet(g.n);
    for (int a : vertices) {
      XBitSet aSide = new XBitSet(new int[] {a});
      XBitSet bSide = g.all.subtract(g.neighborSet[a]);
      bSide.clear(a);
      XBitSet sFixed = g.neighborSet[a].intersectWith(aExcluded);
      if (sFixed.cardinality() > k) {
        continue;
      }
      generateFrom(a, aSide, bSide, g.neighborSet[a],  
          sFixed, aExcluded, "");
      aExcluded.set(a);
    }

  }

  void generateFrom(int a, XBitSet aSide, XBitSet rest, XBitSet separator, XBitSet sFixed,
      XBitSet aExcluded, String indent) {
    if (TRACE) {
      System.out.println(indent + "generateFrom a = " + a + 
          ", aSide = " + aSide);
      System.out.println(indent+ " rest = " + rest + ", aExcluded" + aExcluded); ;
      System.out.println(indent+ " separator = " + separator);
      System.out.println(indent + " sFixed  = " + sFixed);

      System.out.println(indent + minSeps.size() + " minSeps so far");
    }
    if (TRACE) {
      System.out.println(indent + "generateFrom a = " + a + 
          ", aSide = " + aSide + ", rest = " + rest + ", separator = " 
          + separator + ", aExcluded = " + aExcluded);
      System.out.println(indent + "k = " + k + ", " +  minSeps.size() + " minSeps so far");
    }

    assert g.neighborSet(aSide).equals(separator);
    ArrayList<XBitSet> fulls = new ArrayList<>();
    ArrayList<XBitSet> nonFulls = new ArrayList<>();
    g.listComponents(rest, separator, fulls, nonFulls);
    
    for (XBitSet full: fulls) {
      if (TRACE) {
        System.out.println(indent + "full" + full);
      }
      branch(a, aSide, full, separator, sFixed, aExcluded, indent);
    }
    
    for (XBitSet bCompo: nonFulls) {
      XBitSet sep = g.neighborSet(bCompo);
      if (!sFixed.isSubset(sep)) {
        continue;
      }
      XBitSet rest1 = g.all.subtract(bCompo);
      rest1.andNot(sep);
      ArrayList<XBitSet> compos = g.componentsOf(rest1);
      for (XBitSet c: compos) {
        if (c.get(a)) {
          if (!c.intersects(aExcluded)) {
            branch(a, c, bCompo, sep, sFixed, aExcluded, indent);
          }
          break;
        }
      }
    }
  }
  
  void branch(int a, XBitSet aSide, XBitSet bSide, XBitSet separator, 
      XBitSet sFixed, XBitSet aExcluded,       
      String indent) {
    if (TRACE) {
      System.out.println(indent + "branch for a = " + a + 
          ", aSide = " + aSide);
      System.out.println(indent + "bSide = " + bSide);
      System.out.println(indent + "separator = " + separator);
      System.out.println(indent + "sFixed = " + sFixed);

      System.out.println(indent + minSeps.size() + " minSeps so far");
    }
//    if (minSeps.size() > maxOnGenerated) {
//      return;
//    }

    int nA = aSide.cardinality();
    int nS = separator.cardinality();
    if (nS <= k && nA > (g.n - nS) / 2
      || nS > k && nA + (nS - k) > (g.n - k) / 2) {
      return;
    }

    assert sFixed.isSubset(separator);
    assert sFixed.cardinality() <= k;
    assert g.neighborSet(aSide).equals(separator);
    assert g.neighborSet(bSide).equals(separator);
    if (separator.cardinality() <= k) {
      if (TRACE) {
        System.out.println(indent + "minSep added: " + separator);
      }
      minSeps.add(separator);
//      if (minSeps.size() > maxOnGenerated) {
//        return;
//      }
    }
    if (TRACE) {
      System.out.println(indent + "sFixed " + sFixed);
    }

    if (sFixed.cardinality() == k) {
      return;
    }
    
    XBitSet toDecide = separator.subtract(sFixed);
    assert !toDecide.intersects(aExcluded);
    
    if (TRACE) {
      System.out.println(indent + "toDecide " + toDecide);
    }

    if (toDecide.isEmpty()) {
      return;
    }
    
    
//    if (sterile(aSide, bSide, separator, sFixed)) {
//      return;    
//    }
    
    int v = largestNeighborhoodVertex(toDecide, bSide);
    if (TRACE) {
      System.out.println(indent + "branching on " + v);
    }
    XBitSet rest = bSide.subtract(g.neighborSet[v]);
    XBitSet nb = g.neighborSet[v].subtract(separator);
    nb.andNot(aSide);
    XBitSet separator1 = separator.removeBit(v).unionWith(nb);
    XBitSet sFixed1 = sFixed.unionWith(nb.intersectWith(aExcluded));
    if (TRACE) {
      System.out.println(indent + "sFixed1 = " + sFixed1);
    }
    if (sFixed1.cardinality() <= k) {
      generateFrom(a, aSide.addBit(v), rest, 
        separator1, sFixed1, aExcluded, indent + " ");
    }
    if (sFixed.cardinality() < k) {
      branch(a, aSide, bSide, separator, sFixed.addBit(v), aExcluded, indent + " ");
    }
  }
  
  int largestNeighborhoodVertex(XBitSet toDecide, XBitSet bSide) {
    int vLargest = toDecide.nextSetBit(0);
    assert vLargest >= 0;
    int sLargest = g.neighborSet[vLargest].intersectWith(bSide).cardinality();
    for (int v = toDecide.nextSetBit(vLargest); v >= 0; v = toDecide.nextSetBit(v + 1)) {
      int sN = g.neighborSet[v].intersectWith(bSide).cardinality(); 
      if (sN > sLargest) {
        vLargest = v;
        sLargest = sN;
      }
    }
    return vLargest;
  }

  boolean sterile(XBitSet aSide, XBitSet bSide, XBitSet separator, XBitSet sFixed) {
    int nA = aSide.cardinality();
    int nB = bSide.cardinality();
    int nS = separator.cardinality();
    int nF = sFixed.cardinality();
    int nR = nS - nF;
    assert nR > 0;

    XBitSet toDecide = separator.subtract(sFixed);
        
    if (nS > k) {
      if (nA + (nS - k) > (g.n - k) / 2) {
        return true;
      }
      // nA + nS + want - k > (g.n - k) / invAlpha
      int want = (g.n - k) / 2 - nA - nS + k + 1;
      
      if (want * (nS - nF) > nB * (nS - k)) {
        return false;
      }

//      if (true) {
//        return false;
//      }
      // sterility check with vertex disjoint paths(with hanging trees)
      XBitSet rest = (XBitSet) bSide.clone();
      XBitSet[] treeNeighbors = new XBitSet[nR];

      {
        int i = 0;
        for (int v = toDecide.nextSetBit(0);
            v >= 0; v = toDecide.nextSetBit(v + 1)) {
          treeNeighbors[i++] =
              g.neighborSet[v].intersectWith(rest);
        }
      }
      int taken = 0;
      int nSurviving = nR;
      int depth = 0;
      assert k > nF;
      while (true) {
        for (int i = 0; i < nSurviving; i++) {
          treeNeighbors[i].and(rest);
        }
        Arrays.sort(treeNeighbors, 0, nSurviving, 
            XBitSet.cardinalityComparator);
        if (taken - depth * (k - nF) +  
            treeNeighbors[nSurviving - (k - nF) - 1].cardinality()
            >= want) { 
          return true;
        }
        int j = 0;
        for (int i = 0; i < nSurviving; i++) {
          treeNeighbors[i].and(rest);
          if (!treeNeighbors[i].isEmpty()) {
            int w = treeNeighbors[i].nextSetBit(0);
            treeNeighbors[i].clear(w);
            taken++;
            treeNeighbors[i].or(g.neighborSet[w].intersectWith(rest));
            rest.clear(w);
            treeNeighbors[j++] = treeNeighbors[i];
          }
        }
        nSurviving = j;
        depth++;

        if (nSurviving < (k - nF)) {
          break;
        }
        if (taken - (k - nF) * depth >= want) {
          return true;
        }
        if (nSurviving == k - nF) {
          break;
        }
      }
    }
    return false;
  }

  class NeighborSizeComparator implements Comparator<Integer> {
    @Override
    public int compare(Integer v, Integer w) {
      int c = g.neighborSet[w].cardinality() - 
          g.neighborSet[v].cardinality();
      if (c != 0) return c;
      return v - w;
    }
  }
}

