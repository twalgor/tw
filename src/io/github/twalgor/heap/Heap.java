package io.github.twalgor.heap;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class Heap {
  public Queueable[] h;
  int n;
  
  public Heap(int s) {
    h = new Queueable[s];
    n = 0;
  }
  
  public int size() {
    return n;
  }
  
  public boolean isEmpty() {
    return n == 0;
  }
  
  public void add(Queueable x) {
    if (n == h.length) {
      h = Arrays.copyOf(h, n * 2);
    }
    h[n] = x;
    x.setHeapIndex(n);
    n++;
    promote(n - 1);
    assert hasIntegrity();
  }

  public Queueable removeMin() {
    assert n >= 1;
    Queueable removed = h[0];
    n--;
    h[0] = h[n];
    h[0].setHeapIndex(0);
    demote(0);
    removed.setHeapIndex(-1);
    assert hasIntegrity();
    return removed;
  }
  
  public void remove(Queueable x) {
    assert n > 0;
    int i = x.getHeapIndex();
    assert h[i].equals(x): h[i] + " : " + x;
    assert i < n;
    h[i] = h[n];
    h[i].setHeapIndex(i);
    if (h[i].compareTo(x) < 0) {
      promote(i);
    }
    else {
      demote(i);
    }
    x.setHeapIndex(-1);
    assert hasIntegrity();
  }
  
  void promote(int i) {
//    System.out.println("promoting " + i + ": " + h[i]);
    if (i == 0) {
      return;
    }
    int p = (i + 1) / 2 - 1;
    if (h[i].compareTo(h[p]) < 0) {
      Queueable x = h[i];
      h[i] = h[p];
      h[p] = x;
      h[i].setHeapIndex(i);
      h[p].setHeapIndex(p);
      promote(p);
    }
  }

  void demote(int i) {
    if ((i + 1) * 2 > n) {
      return;
    }
    int c = (i + 1) * 2 - 1;
    if (c + 1 < n && h[c + 1].compareTo(h[c]) < 0) {
      c++;
    }
    
    if (h[c].compareTo(h[i]) < 0) {
      Queueable x = h[i];
      h[i] = h[c];
      h[c] = x;
      h[i].setHeapIndex(i);
      h[c].setHeapIndex(c);
      demote(c);
    }
  }
  
  public boolean contains (Queueable x) {
    for (int i = 0; i < n; i++) {
      if (h[i].equals(x)) {
        return true;
      }
    }
    return false;
  }
  
  boolean hasIntegrity() {
    Set<Queueable> set = new HashSet<>();
    for (int i = 0; i < n; i++) {
      assert !set.contains(h[i]): "duplicate " + h[i];
      set.add(h[i]);
    }
    for (int i = 0; i < n; i++) {
      assert h[i] != null: i;
      assert h[i].getHeapIndex() == i: i + ":" + h[i].getHeapIndex();
      int j = (i + 1) * 2 - 1; 
      if (j < n) {
        assert h[j] != null: j;
        assert h[i].compareTo(h[j]) < 0: i +":" + h[i] + ">=" + j + ":" + h[j] + 
        ", " + h[i].compareTo(h[j]); 
      }
      if (j + 1 < n) {
        assert h[j + 1] != null: (j + 1);
        assert h[i].compareTo(h[j + 1]) < 0:
          i +":" + h[i] + ">=" + (j - 1) + ":" + h[j - 1]; ; 
      }
    }
    return true;
  }

  public void validate() {
    for (int i = 1; i < n; i++) {
      assert h[i].getHeapIndex() == i;
      int p = (i + 1) / 2 - 1;
      assert h[p].compareTo(h[i]) < 0;
    }
  }

  public Queueable elementAt(int i) {
    return h[i];
  }

}
