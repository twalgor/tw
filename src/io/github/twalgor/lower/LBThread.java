package io.github.twalgor.lower;

import io.github.twalgor.common.Minor;
import io.github.twalgor.main.ResultFile;
import io.github.twalgor.main.Shared;

public class LBThread extends Thread {
  Minor baseMinor;
  Shared shared;
  ResultFile rf;
  
  public LBThread(Minor baseMinor, Shared shared, ResultFile rf) {
    this.baseMinor = baseMinor;
    this.shared = shared;
    this.rf = rf;
  }
  
  @Override
  public void run() {
    
    FillAndBreak fb = new FillAndBreak(baseMinor.getGraph(), shared);
    fb.initialLowerBound();
    shared.setLB(fb.lb);
    Minor cert = fb.obs.composeWith(baseMinor);
    long t = System.currentTimeMillis();
    rf.addLine("certificate width " + shared.getLB() + " n " + 
        cert.m + " time " + (t - shared.getT0()));
    for (int i = 0; i < cert.m; i++) {
      rf.addLine(i + " "+ cert.components[i]);
    }
    rf.close();
    while (shared.getLB() < shared.getUB()) {
      t = System.currentTimeMillis();
      System.out.println(fb.lb + ", " + shared.getLB() + ":" + shared.getUB() + 
          ", " + (t - shared.getT0()) + " millilsecs");
      fb.improvedLowerBound();
      if (fb.lb > shared.getLB()) {
        t = System.currentTimeMillis();
        System.out.println("lowerbound improved: " + fb.lb + ", " +  
            (t - shared.getT0()) + " millilsecs");
        shared.setLB(fb.lb);
        cert = fb.obs.composeWith(baseMinor);
        t = System.currentTimeMillis();
        rf.addLine("certificate width " + shared.getLB() + " n " + 
            cert.m + " time " + (t - shared.getT0()));
        for (int i = 0; i < cert.m; i++) {
          rf.addLine(i + " "+ cert.components[i]);
        }
        rf.close();
      }
    }
  }

}
