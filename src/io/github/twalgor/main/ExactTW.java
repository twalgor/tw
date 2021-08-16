package io.github.twalgor.main;

import java.io.File;

import io.github.twalgor.acsd.ACSDecomposition;
import io.github.twalgor.acsd.ACSDecomposition.MTAlg;
import io.github.twalgor.common.Chordal;
import io.github.twalgor.common.Graph;
import io.github.twalgor.common.LocalGraph;
import io.github.twalgor.common.TreeDecomposition;
import io.github.twalgor.common.XBitSet;
import io.github.twalgor.decomposer.SemiPID;

public class ExactTW {
  public static void main(String[] args) {
    assert args.length >= 2;
    String graphPath = args[0];
    String tdPath = args[1];
    boolean viaACSD = false;
    if (args.length == 3 &&
      args[2].equals("-acsd")) {
      viaACSD = true;
    }
    
    Graph g = Graph.readGraph(new File(graphPath));
    
    if (viaACSD) {
      ACSDecomposition acsd = new ACSDecomposition(g, MTAlg.mmaf);
      acsd.decomposeByACS();
      Graph t = g.copy();
      for (XBitSet atom: acsd.acAtoms) {
        LocalGraph local = new LocalGraph(g, atom);
        TreeDecomposition tdLocal = SemiPID.decompose(local.h);
        for (int b = 1; b <= tdLocal.nb; b++) {
          t.fill(new XBitSet(tdLocal.bags[b]).convert(local.inv));
        }
      }
      TreeDecomposition td = Chordal.chordalToTD(t);
      td.g = g;
      td.save(tdPath);
    }
    else {
      TreeDecomposition td = SemiPID.decompose(g);
      td.save(tdPath);
    }
  }
}
