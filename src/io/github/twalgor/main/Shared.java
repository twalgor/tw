package io.github.twalgor.main;

public class Shared {
  int ub;
  int lb;
  boolean stop;
  long t0;
  
  public synchronized int getUB() {
    return ub;
  }

  public synchronized void setUB(int ub) {
    this.ub = ub;
  }

  public synchronized int getLB() {
    return lb;
  }

  public synchronized void setLB(int lb) {
    this.lb = lb;
  }

  public synchronized boolean getStop() {
    return stop;
  }

  public synchronized void setStop(boolean stop) {
    this.stop = stop;
  }

  
  
  public long getT0() {
    return t0;
  }
  
  public Shared(int ub, int lb, long t0) {
    this.ub = ub;
    this.lb = lb;
    this.t0 = t0;
  }
  
  
}
