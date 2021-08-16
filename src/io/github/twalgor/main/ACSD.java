package io.github.twalgor.main;

import java.io.File;
import java.util.Set;

import io.github.twalgor.acsd.ACSDecBrute;
import io.github.twalgor.acsd.ACSDecomposition;
import io.github.twalgor.acsd.ACSDecomposition.MTAlg;
import io.github.twalgor.common.Chordal;
import io.github.twalgor.common.Graph;
import io.github.twalgor.common.XBitSet;
import io.github.twalgor.common.TreeDecomposition;

public class ACSD {
  public static void main(String[] args) {
    assert args.length == 3;
    String algType = args[0];
    String graphPath = args[1];
    String acsdPath = args[2];
    Graph g = Graph.readGraph(new File(graphPath));

    Set<XBitSet> acAtoms = null;
    if (algType.equals("-STD")) {
      ACSDecBrute acsdb = new ACSDecBrute(g);
      acsdb.decomposeByACS();
      acAtoms = acsdb.acAtoms;
    }
    else {
      ACSDecomposition acsd = null;
      switch (algType) {
      case "-MCS": acsd = new ACSDecomposition(g, MTAlg.mcs);
      break;
      case "-MMD": acsd = new ACSDecomposition(g, MTAlg.mmd);
      break;
      case "-MMAF": acsd = new ACSDecomposition(g, MTAlg.mmaf);
      }
      acsd.decomposeByACS();
      acAtoms = acsd.acAtoms;
    }
    
    Graph t = g.copy();
    for (XBitSet atom: acAtoms) {
      t.fill(atom);
    }
    TreeDecomposition td = Chordal.chordalToTD(t);
    td.save(acsdPath);
  }
}
