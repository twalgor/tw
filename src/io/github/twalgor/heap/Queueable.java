package io.github.twalgor.heap;

public interface Queueable extends Comparable {
  public void setHeapIndex(int i);
  public int getHeapIndex();
}
