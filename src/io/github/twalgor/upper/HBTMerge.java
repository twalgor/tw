package io.github.twalgor.upper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.TimeoutException;

import io.github.twalgor.common.Chordal;
import io.github.twalgor.log.Log;
import io.github.twalgor.common.Graph;
import io.github.twalgor.common.LocalGraph;
import io.github.twalgor.common.Subgraph;
import io.github.twalgor.common.TreeDecomposition;
import io.github.twalgor.common.XBitSet;
import io.github.twalgor.decomposer.SemiPID;
import io.github.twalgor.greedy.MMAF;
import io.github.twalgor.sieve.SubblockSieve;

public class HBTMerge {
//  static final boolean TRACE = true;
    static final boolean TRACE = false;
  //  static final boolean TRACE_FILTER = true;
  static final boolean TRACE_FILTER = false;
//  static final boolean TRACE_EVALUATE = true;
  static final boolean TRACE_EVALUATE = false;
//  static final boolean TRACE_MERGE = true;
  static final boolean TRACE_MERGE = false;
//    static final boolean TRACE_DP = true;
  static final boolean TRACE_DP = false;
  static final boolean VERIFY = false;

  public static final int N_INITIAL_GREEDY = 10;
  public static final int BASE_SIZE = 60;
  public static final int N_TRY = 50;
  static final int MAX_INDENT = 40;

  public static Log log;

  Graph g;
  int depth;
  int width;
  Graph triangulated;
  
  HBTMerge side;

  Set<XBitSet> pmcs;

  Map<XBitSet, Block> blockMap;
  Map<XBitSet, PMC> pmcMap;
  Block[] ba;

  SubblockSieve sieve;

  int baseSize;

  Random random;

  long t0;

  public HBTMerge(Graph g, int depth, Random random) {
    this.g = g;
    this.depth = depth;
    this.random = random;
    baseSize = BASE_SIZE;
    if (TRACE) {
      System.out.println(indent() + "Mergeable of depth " + depth);
    }
  }

  void initialize() {
    int minWidth = 0;
    int[] bestInv = null;
    Graph bestH = null;
    for (int i = 0; i < N_INITIAL_GREEDY; i++) {
      int[] conv = randomPermutation(g.n, random);
      int[] inv = Graph.inversion(g.n, conv, g.n);
      Graph h = new Graph(g.n);
      h.inheritEdges(g, conv, inv);
      MMAF mmaf = new MMAF(h);
      mmaf.triangulate();
      if (bestInv == null || mmaf.width < minWidth) {
        minWidth = mmaf.width;
        if (TRACE) {
          System.out.println(indent() + "better mmaf width " + minWidth);
        }
        bestInv = inv;
        bestH = h;
      }
    }
    Chordal chordal = new Chordal(bestH);
    Set<XBitSet> cliques = chordal.maximalCliques();
    pmcs = new HashSet<>();
    for (XBitSet clique : cliques) {
      pmcs.add(clique.convert(bestInv));
    }
    width = minWidth;

    dp();
    optimalTriangulation();
  }

  int[] randomPermutation(int n, Random random) {
    ArrayList<Integer> perm = new ArrayList<>();
    for (int i = 0; i < n; i++) {
      perm.add(i); 
    }
    Collections.shuffle(perm, random);
    int[] result = new int[n];
    for (int i = 0; i < n; i++) {
      result[i] = perm.get(i);
    }
    return result;
  }

  void improve() {
    if (side == null) {
      side = new HBTMerge(g, depth + 1, random);
      side.initialize();
    }
    if (side.width > width) {
      side.improve();
    }
    else {
      mergeWith(side);
    }
  }

  void mergeWith(HBTMerge side) {
    XBitSet pmc = randomPMC();
    XBitSet component = largestComponent(pmc);
    XBitSet scope = pmc.unionWith(component);
    if (TRACE) {
      System.out.println(indent() + "scope of cardinality " + scope.cardinality());
    }
    Set<XBitSet> focuses = new HashSet<>();
    for (XBitSet pmc1: side.pmcs) {
      if (pmc1.cardinality() > width || !pmc1.isSubset(scope)) {
        continue;
      }
      XBitSet focus = getFocus(scope, pmc1);
      if (focus != null) {
        focuses.add(focus);
      }
    }
    XBitSet[] fa = new XBitSet[focuses.size()];
    focuses.toArray(fa);
    Arrays.sort(fa, XBitSet.cardinalityComparator);
    for (int i = 0; i < Math.min(fa.length, N_TRY); i++) {
      XBitSet focus = fa[i];
      LocalGraph local = new LocalGraph(g, focus);
      Graph h = local.h;

      TreeDecomposition td = null;
      if (h.n <= baseSize) {
//        td = SemiPID.decompose(h, false);
        td = SemiPID.decompose(h);
        if (TRACE_MERGE) {
          System.out.println(indent() + "exact td of width " + td.width + ", baseSize = " + baseSize);
        }
        if (td.width <= width) {
          Graph f = h.copy();
          for (int b = 1; b <= td.nb; b++) {
            XBitSet bag = new XBitSet(td.bags[b]);
            for (int j = 0; j < td.neighbor[b].length; j++) {
              int b1 = td.neighbor[b][j];
              if (b1 > b) {
                XBitSet bag1 = new XBitSet(td.bags[b1]);
                XBitSet sep = bag.intersectWith(bag1);
                f.fill(sep);
              }
            }
          }
          MMAF mmaf1 = new MMAF(f);
          mmaf1.triangulate();
          //          assert mmaf1.width <= td.width;
          if (TRACE) {
            System.out.println(indent() + "triangulated with width " + mmaf1.width);
          }
          Chordal chordal = new Chordal(f);
          Set<XBitSet> cliques = chordal.maximalCliques();
          for (XBitSet clique: cliques) {
            assert h.isPMC(clique);
            XBitSet pmc1 = clique.convert(local.inv);
            assert g.isCliquish(pmc1);
            if (g.fullComponents(pmc1).isEmpty()) {
              pmcs.add(pmc1);
            }
          }
        }
      }
      else {
        MMAF mmaf = new MMAF(h);
        mmaf.triangulate();
        if (TRACE_MERGE) {
          System.out.println(indent() + "mmaf td of width " + mmaf.width);
        }

        if (mmaf.width <= width) {
          Chordal chordal = new Chordal(h);
          Set<XBitSet> pmcs1 = chordal.maximalCliques();
          for (XBitSet pmc1: pmcs1) {
            assert h.isPMC(pmc1);
            XBitSet pmc2 = pmc1.convert(local.inv);
            assert g.isCliquish(pmc2);
            if (g.fullComponents(pmc2).isEmpty()) {
              pmcs.add(pmc2);
            }
          }
        }
      }
    }
    pmcs.addAll(side.pmcs);
    int w = dp();
    assert w <= width;
    assert w <= side.width;
    if (w < width) {
      width = w;
      if (TRACE) {
        System.out.println(indent() + "improved upperbound " + width);
      }
      optimalTriangulation();
      filter();
    }
  }

  XBitSet largestComponent(XBitSet pmc) {
    ArrayList<XBitSet> components = g.separatedComponents(pmc);
    XBitSet largest = null;
    for (XBitSet compo: components) {
      if (largest == null || compo.cardinality() > largest.cardinality() ) {
        largest = compo;
      }
    }
    return largest;
  }

  XBitSet getFocus(XBitSet scope, XBitSet pmc) {
    ArrayList<XBitSet> components = g.separatedComponents(pmc);
    for (XBitSet compo: components) {
      if (!compo.isSubset(scope)) {
        return compo.unionWith(g.neighborSet(compo)).intersectWith(scope);
      }
    }
    return null;
  }

  XBitSet randomPMC() {
    XBitSet[] pa = new XBitSet[pmcs.size()];
    pmcs.toArray(pa);
    int r = random.nextInt(pa.length);
    return pa[r];
  }
  
  void filter() {
    if (TRACE_FILTER) {
      System.out.println("filtering " + blockMap.size() + " blocks and " + pmcMap.size() + " pmcs");
    }
    Set<XBitSet> pmcsToKeep = new HashSet<>();

    for (PMC pmc : pmcMap.values()) {
      if (pmc.width <= width ) {
        pmcsToKeep.add(pmc.vertices);
      }
    }
    pmcMap.clear();
    blockMap.clear();
    
    for (XBitSet pmc: pmcsToKeep) {
      makePMC(pmc);
    }
  }

  void filterOld() {
    if (TRACE_FILTER) {
      System.out.println("filtering " + blockMap.size() + " blocks and " + pmcMap.size() + " pmcs");
    }
    Set<PMC> pmcsToKeep = new HashSet<>();
    Set<Block> blocksToKeep = new HashSet<>();

    for (PMC pmc : pmcMap.values()) {
      if (pmc.width <= width ) {
        pmcsToKeep.add(pmc);
        for (Block block : pmc.subblock) {
          blocksToKeep.add(block);
        }
      }
    }

    ba = new Block[blockMap.size()];
    blockMap.values().toArray(ba);

    for (Block block : ba) {
      if (!blocksToKeep.contains(block)) {
        blockMap.remove(block.component);
      } else {
        PMC[] ca = new PMC[block.caps.size()];
        block.caps.toArray(ca);
        for (PMC p : ca) {
          if (!pmcsToKeep.contains(p)) {
            block.caps.remove(p);
          }
        }
      }
    }

    PMC[] pa = new PMC[pmcMap.size()];
    pmcMap.values().toArray(pa);

    for (PMC pmc : pa) {
      if (!pmcsToKeep.contains(pmc)) {
        pmcMap.remove(pmc.vertices);
      }
    }

    if (TRACE_FILTER) {
      System.out.println(" .. " + blockMap.size() + " blocks and " + pmcMap.size() + " pmcs after filtering");
    }
  }

  int maxSizeOf(Set<PMC> pmcs) {
    int max = 0;
    for (PMC pmc : pmcs) {
      if (pmc.vertices.cardinality() > max) {
        max = pmc.vertices.cardinality();
      }
    }
    return max;
  }

  int dp() {
    if (TRACE_DP) {
      System.out.println(indent() + "dp ...");
    }
    blockMap = new HashMap<>();
    pmcMap = new HashMap<>();

    for (XBitSet pmc: pmcs) {
      makePMC(pmc);
    }
    if (TRACE_DP) {
      System.out.println(indent() + blockMap.size() + " blocks, " + 
          pmcMap.size() + " pmcs");
    }

    Block[] ba = new Block[blockMap.size()];
    blockMap.values().toArray(ba);

    Arrays.sort(ba);

    for (Block block : ba) {
      block.evaluate();
    }

    for (PMC pmc : pmcMap.values()) {
      pmc.computeWidth();
    }

    int value = g.n - 1;
    for (PMC pmc : pmcMap.values()) {
      if (pmc.width < value) {
        value = pmc.width;
      }
    }

    return value;
  }

  void optimalTriangulation() {
    triangulated = g.copy();
    for (PMC pmc : pmcMap.values()) {
      if (pmc.width == width && pmc.allFeasible()) {
        pmc.triangulate(g.all);
        return;
      }
    }
  }

  Block makeBlock(XBitSet component) {
    // ensures that a block for a component is unique
    // thus, the equality for blocks is the identity
    Block block = blockMap.get(component);
    if (block == null) {
      block = new Block(component);
      assert g.fullComponents(block.separator).size() >= 2;
      blockMap.put(component, block);
    }
    return block;
  }

  PMC makePMC(XBitSet separator) {
    // ensures that a bag for a vertex set is unique
    // thus, the equality for bags is the identity
    PMC pmc = pmcMap.get(separator);
    if (pmc == null) {
      pmc = new PMC(separator);
      pmcMap.put(separator, pmc);
    }
    return pmc;
  }

  boolean allFeasible(XBitSet separator) {
    ArrayList<XBitSet> components = g.separatedComponents(separator);
    for (XBitSet compo : components) {
      Block b = blockMap.get(compo);
      assert b != null;
      if (!b.isFeasible()) {
        return false;
      }
    }
    return true;
  }

  void checkTimeout() throws TimeoutException {
    //    boolean interrupted = Thread.interrupted();
    //    System.out.println("interruppted = " + interrupted);
    if (Thread.interrupted()) {
      System.out.println("interruppted");
      throw new TimeoutException();
    }
  }

  double balanceOf(XBitSet sep, Graph h) {
    int s = sep.cardinality();
    double balance = 0.0;
    ArrayList<XBitSet> components = h.separatedComponents(sep);
    for (XBitSet compo : components) {
      double r = (double) compo.cardinality() / (double) (h.n - s);
      if (r > balance) {
        balance = r;
      }
    }
    return balance;
  }

  String indent() {
    return indent(depth);
  }

  String indent(int d) {
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < d; i++) {
      sb.append(" ");
    }
    return sb.toString();
  }

  static int indexOf(Object x, Object[] a) {
    for (int i = 0; i < a.length; i++) {
      if (x == a[i]) {
        return i;
      }
    }
    return 0;
  }

  int maxCard(Set<XBitSet> sets) {
    int max = 0;
    for (XBitSet set : sets) {
      if (set.cardinality() > max) {
        max = set.cardinality();
      }
    }
    return max;
  }

  int indexOfBagContaining(XBitSet separator, TreeDecomposition td) {
    for (int b = 1; b <= td.nb; b++) {
      if (separator.isSubset(new XBitSet(td.bags[b]))) {
        return b;
      }
    }
    return 0;
  }

  boolean confined(XBitSet es, XBitSet vs, int n) {
    for (int e = es.nextSetBit(0); e >= 0; e = es.nextSetBit(e + 1)) {
      if (!vs.get(e / n) || !vs.get(e % n))
        return false;
    }
    return true;
  }

  String spaces(int n) {
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < n; i++) {
      sb.append(" ");
    }
    return sb.toString();
  }

  class Block implements Comparable<Block> {
    XBitSet component;
    XBitSet separator;
    Set<PMC> caps;
    int width;
    int nearness;

    Block(XBitSet component) {
      this.component = component;
      separator = g.neighborSet(component);
      caps = new HashSet<PMC>();
    }

    void triangulate() {
      PMC bestCap = bestCap();

      if (bestCap != null) {
        bestCap.triangulate(component);
      } 
      else {
        XBitSet cn = g.closedNeighborSet(component);
        assert cn.cardinality() <= HBTMerge.this.width + 1: cn;
        triangulated.fill(cn);
      }
    }

    int fillDecomposition(TreeDecomposition td) {
      PMC bestCap = bestCap();

      if (bestCap != null) {
        return bestCap.fillDecomposition(component, td);
      } else {
        XBitSet cn = g.closedNeighborSet(component);
        assert cn.cardinality() <= HBTMerge.this.width + 1;
        return td.addBag(cn.toArray());
      }
    }

    PMC bestCap() {
      int best = 
          component.cardinality() + separator.cardinality() - 1;
      PMC bestCap = null;
      for (PMC cap : caps) {
        if (cap.width < best) {
          bestCap = cap;
          best = cap.widthFor(component);
        }
      }
      return bestCap;
    }

    void evaluate() {
      if (TRACE_EVALUATE) {
        System.out.println(indent() + "evaluating " + this);
      }
      width = component.cardinality() + separator.cardinality() - 1;
      for (PMC pmc : caps) {
        int w = pmc.widthFor(component);
        if (w < width) {
          width = w;
        }
      }
      if (TRACE_EVALUATE) {
        System.out.println(indent() + "width = " + width);
      }
    }

    boolean isFeasible() {
      return width <= HBTMerge.this.width;
    }

    boolean addCap(PMC bag) {
      if (caps.contains(bag)) {
        return false;
      }
      caps.add(bag);
      return true;
    }

    Block theOtherBlock() {
      ArrayList<XBitSet> fulls = g.fullComponents(separator);
      assert fulls.size() >= 2;
      for (XBitSet full : fulls) {
        if (!full.equals(component)) {
          return makeBlock(full);
        }
      }
      assert false;
      return null;
    }

    void greedilyDecompose() {
      XBitSet vs = component.unionWith(separator);
      Subgraph sub = new Subgraph(g, vs);
      sub.h.fill(separator.convert(sub.conv));
      MMAF mmaf = new MMAF(sub.h);
      mmaf.triangulate();
      Set<XBitSet> cliques = new Chordal(sub.h).maximalCliques();
      for (XBitSet clique : cliques) {
        makePMC(clique.convert(sub.inv));
      }
    }

    boolean hasSmaller(ArrayList<XBitSet> components, XBitSet vs) {
      for (XBitSet compo : components) {
        if (XBitSet.cardinalityComparator.compare(compo, vs) < 0) {
          return true;
        }
      }
      return false;
    }

    boolean isFullComponent(XBitSet component, XBitSet sep) {
      for (int v = sep.nextSetBit(0); v >= 0; v = sep.nextSetBit(v + 1)) {
        if (component.isDisjoint(g.neighborSet[v])) {
          return false;
        }
      }
      return true;
    }

    String indent() {
      double logN = Math.log(g.n);
      double logS = Math.log(component.cardinality());
      return HBTMerge.this.indent((int) ((double) MAX_INDENT * (logN - logS) / logN));
    }

    @Override
    public int compareTo(Block s) {
      return XBitSet.cardinalityComparator.compare(component, s.component);
    }

    @Override
    public String toString() {
      StringBuilder sb = new StringBuilder();
      sb.append("component = " + component);
      sb.append(", separator = " + separator);
      sb.append(", " + caps.size() + " caps");
      sb.append(", width = " + width + " isFeasible = " + isFeasible());
      return sb.toString();
    }

    void dump() {
      System.out.println(indent() + this);
      for (PMC cap : caps) {
        cap.dump(this);
      }
    }

    ArrayList<Block> otherBlocks() {
      ArrayList<XBitSet> components = g.separatedComponents(component.unionWith(separator));
      XBitSet full = null;
      for (XBitSet compo : components) {
        if (g.neighborSet(compo).equals(separator)) {
          full = compo;
          break;
        }
      }
      assert full != null;

      ArrayList<Block> result = new ArrayList<>();
      result.add(makeBlock(full));

      for (XBitSet compo : components) {
        if (compo != full) {
          result.add(makeBlock(compo));
        }
      }
      return result;
    }

    int fillTD(TreeDecomposition td, int[] conv) {
      PMC cap = bestCap();
      if (cap == null) {
        return -1;
      }
      int r = td.addBag(cap.vertices.convert(conv).toArray());
      for (Block block : cap.subblock) {
        if (block.component.isSubset(component)) {
          int b = block.fillTD(td, conv);
          if (b == -1) {
            return -1;
          }
          td.addEdge(r, b);
        }
      }
      return r;
    }
  }

  class PMC {
    XBitSet vertices;
    Block[] subblock;
    int width;
    boolean mark;

    PMC(XBitSet vertices) {
      super();
      //      assertPMC(separator);

      this.vertices = vertices;
      assert g != null;
      ArrayList<XBitSet> components = g.separatedComponents(vertices);
      subblock = new Block[components.size()];
      for (int i = 0; i < subblock.length; i++) {
        XBitSet compo = components.get(i);
        subblock[i] = makeBlock(compo);
      }

      for (Block block : subblock) {
        XBitSet sep = block.separator;
        XBitSet component = vertices.subtract(sep);
        XBitSet nb = g.neighborSet(component);
        for (Block b : subblock) {
          if (b.component.intersects(nb)) {
            component.or(b.component);
          }
        }
        assert g.neighborSet(component).equals(sep);
        Block superblock = makeBlock(component);
        superblock.addCap(this);
      }
    }

    boolean allFeasible() {
      for (Block block: subblock) {
        if (block.width > width) {
          return false;
        }
      }
      return true;
    }

    void computeWidth() {
      width = vertices.cardinality() - 1;
      for (Block block : subblock) {
        if (block.width > width) {
          width = block.width;
        }
      }
    }

    int widthFor(XBitSet component) {
      width = vertices.cardinality() - 1;
      for (Block block : subblock) {
        if (block.component.isSubset(component) && block.width > width) {
          width = block.width;
        }
      }
      return width;
    }

    boolean isFeasible() {
      return width < HBTMerge.this.width;
    }

    void triangulate(XBitSet component) {
      assert widthFor(component) <= HBTMerge.this.width;
      triangulated.fill(vertices);
      for (Block block : subblock) {
        if (block.component.isSubset(component)) {
          assert block.width <= HBTMerge.this.width;
          block.triangulate();
        }
      }
    }

    int fillDecomposition(XBitSet component, TreeDecomposition td) {
      int r = td.addBag(vertices.toArray());
      for (Block block : subblock) {
        if (block.component.isSubset(component)) {
          int b = block.fillDecomposition(td);
          td.addEdge(r, b);
        }
      }
      return r;
    }

    @Override
    public String toString() {
      StringBuilder sb = new StringBuilder();
      sb.append("separator = " + vertices.toString().substring(0, 100));
      sb.append("\n");
      sb.append("value = " + width + ", " + subblock.length + " subblocks:");
      for (Block block : subblock) {
        sb.append(" " + block.component.cardinality());
      }
      return sb.toString();
    }

    void dump(Block parent) {
      System.out.println(parent.indent() + "p:" + this);
      for (Block block : subblock) {
        if (block.component.isSubset(parent.component)) {
          block.dump();
        }
      }
    }
  }

  class Focus {
    XBitSet vertices;
    Set<XBitSet> separators;

    Focus(XBitSet vertices, Set<XBitSet> separators) {
      this.vertices = vertices;
      this.separators = separators;
    }

    String indent() {
      double logN = Math.log(g.n);
      double logS = Math.log(vertices.cardinality());
      return HBTMerge.this.indent((int) ((double) MAX_INDENT * (logN - logS) / logN));
    }

    @Override
    public String toString() {
      StringBuilder sb = new StringBuilder();
      sb.append(indent() + "vertices = " + vertices.toString().substring(100));
      sb.append("\n" + indent() + separators.size() + " separators: ");
      //      for (XBitSet separator: separators) {
      //        System.out.println(indent() + "\n " + Util.vertexSetToShortString(separator));
      //      }
      return sb.toString();
    }
  }

  void printState(String prefix) {
    System.out.println(prefix + width + ", " + blockMap.size() + ", " + pmcMap.size());
  }

  static void log(String message) {
    if (log != null) {
      log.log(message, true);
    } else {
      System.out.println(message);
    }
  }
}

