/*
 * Copyright (c) 2016, 2019 Hisao Tamaki
 */
package io.github.twalgor.common;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Random;

/**
 * This class provides a representation of undirected simple graphs.
 * The vertices are identified by non-negative integers
 * smaller than {@code n} where {@code n} is the number
 * of vertices of the graph.
 * The degree (the number of adjacent vertices) of each vertex
 * is stored in an array {@code degree} indexed by the vertex number
 * and the adjacency lists of each vertex
 * is also referenced from an array {@code neighbor} indexed by
 * the vertex number. These arrays as well as the int variable {@code n}
 * are public to allow easy access to the graph content.
 * Reading from and writing to files as well as some basic
 * graph algorithms, such as decomposition into connected components,
 * are provided.
 * 
 * {@code isInbound} method added 12 March, 2019
 * {@code getComponent} method added 22 October, 2019
 * major refactoring November 2019:
 *   some methods are renamed to be more consistent with common terminology
 *   several methods are moved from Util class
 * 
 * adjacency array neighbor removed: adjacency is represented only by neighborSet
 *  02 Jan 2020
 * @author  Hisao Tamaki
 */
public class Graph {
  /**
   * number of vertices
   */
  public int n;

  /**
   * set representation of the adjacencies.
   * {@code neighborSet[v]} is the set of vertices
   * adjacent to vertex {@code v}
   */
  public XBitSet[] neighborSet;

  /**
   * the set of all vertices, represented as an all-one
   * bit vector
   */
  public XBitSet all;

  /*
   * variables used in the DFS aglgorithms for
   * connected componetns and
   * biconnected components.
   */
  private int nc;
  private int mark[];
  private int dfn[];
  private int low[];
  private int dfCount;
  private XBitSet articulationSet;

  /**
   * Construct a graph with the specified number of
   * vertices and no edges.  Edges will be added by
   * the {@code addEdge} method
   * @param n the number of vertices
   */
  public Graph(int n) {
    this.n = n;
    this.neighborSet = new XBitSet[n];
    for (int i = 0; i < n; i++) {
      neighborSet[i] = new XBitSet(n);
    }
    this.all = new XBitSet(n);
    for (int i = 0; i < n; i++) {
      all.set(i);
    }
  }

  /**
   * Set the neighbor set of vertex {@code v}   
   * @param v the vertex for which the neighbor set is to be set
   * @param nb the neighbor set
   */
  
  public void setNeighbors(int v, XBitSet nb) {
    neighborSet[v] = nb;
  }

  /**
   * Add an edge between two specified vertices.
   * This is done by adding each vertex to the neighborSet of the other
   * No effect if the specified edge is already present.
   * @param u vertex (one end of the edge)
   * @param v vertex (the other end of the edge)
   */
  public void addEdge(int u, int v) {
    neighborSet[u].set(v);
    neighborSet[v].set(u);
  }
  
  /**
   * Remove an edge between two specified vertices.
   * No effect if the specified edge is already missing.
   * @param u vertex (one end of the edge)
   * @param v vertex (the other end of the edge)
   */
  public void removeEdge(int u, int v) {
    neighborSet[u].clear(v);
    neighborSet[v].clear(u);
  }

  /**
   * Add vertex {@code v} to the adjacency set of {@code u}
   * @param u vertex number
   * @param v vertex number
   */
  private void addToNeighbors(int u, int v) {
    neighborSet[u].set(v);
  }

  /**
   * Returns the number of edges of this graph
   * @return the number of edges
   */
  public int numberOfEdges() {
    int count = 0;
    for (int i = 0; i < n; i++) {
      count += neighborSet[i].cardinality();
    }
    return count / 2;
  }

  /**
   * Create a conversion table for the given vertex set that maps
   * the vertex of a graph with specified number of vertices 
   * into the local index of the vertex  in the given set of vertices.
   * @param n the number of vertices of the graph
   * @param vertices a vertex set of the target graph
   * @return an {@code int} array {@code conv} such that if {@code v} is in 
   * {@code vertices} and has {@code k} smaller vertices 
   * in {@code vertices} then {@code conv[v]} = {@code k}; 
   * if {@code v} is not in given vertex set, {@code conv[v]} = -1;  
   */
  public static int[] conversion(int n, XBitSet vertices) {
    int[] conv = new int[n];
    int k = 0;
    for (int v = 0; v < n; v++) {
      if (vertices.get(v)) {
        conv[v] = k++;
      }
      else {
        conv[v] = -1;
      }
    }
    return conv;
  }
  
  /**
   * Create the inversion table for the given conversion table
   * @param n the number of vertices of the graph
   * @param conv an {@code int} array of length {@code n} 
   * @param k a positive integer less than or equal to {@code n}: 
   * assume that {@code -1 <= conv[i] < k} holds for each {@code i} 
   * with {@code 0 <= i < n} and, for each {@code j} with {@code 0 <= j < k}, 
   * the index {@code i} such that 
   * {@code conv[i] = j} is unique.
   * @return an {@code int} array {@code inv} of length {@code k} 
   * such that {@code conv[inv[j]] = j} for each {@code j} with {@code 0 <= j < k}. 
   */
  public static int[] inversion(int n, int[] conv, int k) {
    assert conv.length == n;
    int[] inv = new int[k];
    for (int v = 0; v < n; v++) {
      if (conv[v] >= 0) {
        inv[conv[v]] = v;
      }
    }
    return inv;
  }
  /**
   * Inherit edges of the given graph into this graph,
   * according to the conversion tables for vertex numbers.
   * @param g graph
   * @param conv vertex conversion table from the given graph to
   * this graph: if {@code v} is a vertex of graph {@code g}, then
   * {@code conv[v]} is the corresponding vertex in this graph;
   * {@code conv[v] = -1} if {@code v} does not have a corresponding vertex
   * in this graph
   * @param inv vertex conversion table from this graph to
   * the argument graph: if {@code v} is a vertex of this graph,
   * then {@code inv[v]} is the corresponding vertex in graph {@code g};
   * it is assumed that {@code v} always have a corresponding vertex in
   * graph g.
   *
   */
  public void inheritEdges(Graph g, int conv[], int inv[]) {
    for (int v = 0; v < n; v++) {
      int x = inv[v];
      for (int y = g.neighborSet[x].nextSetBit(0); y >= 0;
          y = g.neighborSet[x].nextSetBit(y + 1)) {
        int u = conv[y];
        if (u >= 0) {
            addEdge(u,  v);
        }
      }
    }
  }

  /**
   * Decides if this graph is an edge supergraph of given graph
   * @param g Given graph
   * @return {@code true} if this graph is an edge supergraph of g 
   */
  public boolean isEdgeSupergraph(Graph g) {
    for (int v = 0; v < g.n; v++) {
      if (!g.neighborSet[v].isSubset(neighborSet[v])) {
        return false;
      }
    }
    return true;
  }
  
  /**
   * Read a graph from the specified file in {@code dgf} format and
   * return the resulting {@code Graph} object.
   * @param path the path of the directory containing the file
   * @param name the file name without the extension ".dgf"
   * @return the resulting {@code Graph} object; null if the reading fails
   */
  public static Graph readGraphDgf(String path, String name) {
    File file = new File(path + File.separator + name + ".dgf");
    return readGraphDgf(file);
  }

  /**
   * Read a graph from the specified file in {@code dgf} format and
   * return the resulting {@code Graph} object.
   * @param file file from which to read
   * @return the resulting {@code Graph} object; null if the reading fails
   */
  public static Graph readGraphDgf(File file) {
    try {
      BufferedReader br = new BufferedReader(new FileReader(file));
      String line = br.readLine();
      while (line.startsWith("c")) {
        line = br.readLine();
      }
      if (line.startsWith("p")) {
        String s[] = line.split(" ");
        int n = Integer.parseInt(s[2]);
        // m is twice the number of edges explicitly listed
        int m = Integer.parseInt(s[3]);
        Graph g = new Graph(n);

        for (int i = 0; i < m; i++) {
          line = br.readLine();
          while (!line.startsWith("e")) {
            line = br.readLine();
          }
          s = line.split(" ");
          int u = Integer.parseInt(s[1]) - 1;
          int v = Integer.parseInt(s[2]) - 1;
          g.addEdge(u, v);
        }
        return g;
      }
      else {
        throw new RuntimeException("!!No problem descrioption");
      }

    } catch (FileNotFoundException e) {
      e.printStackTrace();
    } catch (IOException e) {
      e.printStackTrace();
    }
    return null;
  }

  /**
   * Read a graph from the specified file in {@code col} format and
   * return the resulting {@code Graph} object.
   * @param path the path of the directory containing the file
   * @param name the file name without the extension ".col"
   * @return the resulting {@code Graph} object; null if the reading fails
   */
  public static Graph readGraphCol(String path, String name) {
    File file = new File(path + File.separator + name + ".col");
    try {
      BufferedReader br = new BufferedReader(new FileReader(file));
      String line = br.readLine();
      while (line.startsWith("c")) {
        line = br.readLine();
      }
      if (line.startsWith("p")) {
        String s[] = line.split(" ");
        int n = Integer.parseInt(s[2]);
        // m is twice the number of edges in this format
        int m = Integer.parseInt(s[3]);
        Graph g = new Graph(n);

        for (int i = 0; i < m; i++) {
          line = br.readLine();
          while (line != null && !line.startsWith("e")) {
            line = br.readLine();
          }
          if (line == null) {
            break;
          }
          s = line.split(" ");
          int u = Integer.parseInt(s[1]);
          int v = Integer.parseInt(s[2]);
          g.addEdge(u - 1, v - 1);
        }
        return g;
      }
      else {
        throw new RuntimeException("!!No problem descrioption");
      }

    } catch (FileNotFoundException e) {
      e.printStackTrace();
    } catch (IOException e) {
      e.printStackTrace();
    }
    return null;
  }

  /**
   * Read a graph from the specified file in {@code gr} format and
   * return the resulting {@code Graph} object.
   * The vertex numbers 1~n in the gr file format are
   * converted to 0~n-1 in the internal representation.
   * @param file graph file in {@code gr} format
   * @return the resulting {@code Graph} object; null if the reading fails
   */
  public static Graph readGraph(String path, String name) {
    File file = new File(path + File.separator + name + ".gr");
    return readGraph(file);
  }

  /**
   * Read a graph from the specified file in {@code gr} format and
   * return the resulting {@code Graph} object.
   * The vertex numbers 1~n in the gr file format are
   * converted to 0~n-1 in the internal representation.
   * @param path the path of the directory containing the file
   * @param name the file name without the extension ".gr"
   * @return the resulting {@code Graph} object; null if the reading fails
   */
  public static Graph readGraph(File file) {
    try {
      BufferedReader br = new BufferedReader(new FileReader(file));
      String line = br.readLine();
      while (line.startsWith("c")) {
        line = br.readLine();
      }
      if (line.startsWith("p")) {
        String s[] = line.split(" ");
        if (!s[1].equals("tw")) {
          throw new RuntimeException("!!Not treewidth instance");
        }
        int n = Integer.parseInt(s[2]);
        int m = Integer.parseInt(s[3]);
        Graph g = new Graph(n);

        for (int i = 0; i < m; i++) {
          line = br.readLine();
          while (line.startsWith("c")) {
            line = br.readLine();
          }
          s = line.split(" ");
          int u = Integer.parseInt(s[0]);
          int v = Integer.parseInt(s[1]);
          g.addEdge(u - 1, v - 1);
        }
        return g;
      }
      else {
        throw new RuntimeException("!!No problem descrioption");
      }

    } catch (FileNotFoundException e) {
      e.printStackTrace();
    } catch (IOException e) {
      e.printStackTrace();
    }
    return null;
  }

  /**
   * finds the first occurence of the
   * given integer in the given int array
   * @param x value to be searched
   * @param a array
   * @return the smallest {@code i} such that
   * {@code a[i]} = {@code x};
   * -1 if no such {@code i} exists
   */
  private static int indexOf(int x, int a[]) {
    if (a == null) {
      return -1;
    }
    for (int i = 0; i < a.length; i++) {
      if (x == a[i]) {
        return i;
      }
    }
    return -1;
  }

  /**
   * returns true if two vetices are adjacent to each other
   * in this targat graph
   * @param u a vertex
   * @param v another vertex
   * @return {@code true} if {@code u} is adjcent to {@code v};
   * {@code false} otherwise
   */
  public boolean areAdjacent(int u, int v) {
    return neighborSet[u].get(v);
  }

  /**
   * returns the minimum degree, the smallest d such that
   * there is some vertex {@code v} with {@code degree[v]} = d,
   * of this target graph
   * @return the minimum degree
   */
  public int minDegree() {
    if (n == 0) {
      return 0;
    }
    int min = neighborSet[0].cardinality();
    for (int v = 0; v < n; v++) {
      int d = neighborSet[v].cardinality();
      if (d < min) min = d;
    }
    return min;
  }

  /**
   * Computes the neighbor set for a given set of vertices
   * @param set set of vertices
   * @return an {@code XBitSet} representing the neighbor set of
   * the given vertex set
   */
  public XBitSet neighborSet(XBitSet set) {
    XBitSet result = new XBitSet(n);
    for (int v = set.nextSetBit(0); v >= 0;
        v = set.nextSetBit(v + 1)) {
      result.or(neighborSet[v]);
    }
    result.andNot(set);
    return result;
  }

  /**
   * Computes the closed neighbor set for a given set of vertices
   * @param set set of vertices
   * @return an {@code XBitSet} representing the closed neighbor set of
   * the given vertex set
   */
  public XBitSet closedNeighborSet(XBitSet set) {
    XBitSet result = (XBitSet) set.clone();
    for (int v = set.nextSetBit(0); v >= 0;
        v = set.nextSetBit(v + 1)) {
      result.or(neighborSet[v]);
    }
    return result;
  }

  /**
   * Compute connected components of this target graph after
   * the removal of the vertices in the given separator,
   * using Depth-First Search
   * @param separator set of vertices to be removed
   * @return the arrayList of connected components,
   * the vertex set of each component represented by a {@code XBitSet}
   */
  public ArrayList<XBitSet> separatedComponentsDFS(XBitSet separator) {
    ArrayList<XBitSet> result = new ArrayList<XBitSet>();
    mark = new int[n];
    for (int v = 0; v < n; v++) {
      if (separator.get(v)) {
        mark[v] = -1;
      }
    }

    nc = 0;

    for (int v = 0; v < n; v++) {
      if (mark[v] == 0) {
        nc++;
        markFrom(v);
      }
    }

    for (int c = 1; c <= nc; c++) {
      result.add(new XBitSet(n));
    }

    for (int v = 0; v < n; v++) {
      int c = mark[v];
      if (c >= 1) {
        result.get(c - 1).set(v);
      }
    }
    return result;
  }

  /**
   * Recursive method for depth-first search
   * vertices reachable from the given vertex,
   * passing through only unmarked vertices (vertices
   * with the mark[] value being 0 or -1),
   * are marked by the value of {@code nc} which
   * is a positive integer
   * @param v vertex to be visited
   */
  private void markFrom(int v) {
    if (mark[v] != 0) return;
    mark[v] = nc;
    for (int w = neighborSet[v].nextSetBit(0); w >= 0;
        w = neighborSet[v].nextSetBit(w + 1)) {
      markFrom(w);
    }
  }

  /**
   * list connected components in a given vertex set,
   * in two list: one for the full components of the given separator
   * and another for others
   * @param vertices set of vertices 
   * @param separator separator
   * @param fulls ArrayList in which to save full components
   * @param nonFulls ArrayList in which to save non-full compnents
   */
  public void listComponents(XBitSet vertices, XBitSet separator, 
      ArrayList<XBitSet> fulls, ArrayList<XBitSet> nonFulls) {
    XBitSet rest = (XBitSet) vertices.clone();
    for (int v = rest.nextSetBit(0); v >= 0;
        v = rest.nextSetBit(v + 1)) {
      XBitSet c = (XBitSet) neighborSet[v].clone();
      XBitSet toBeScanned = c.subtract(separator);
      c.set(v);
      while (!toBeScanned.isEmpty()) {
        XBitSet save = (XBitSet) c.clone();
        for (int w = toBeScanned.nextSetBit(0); w >= 0;
          w = toBeScanned.nextSetBit(w + 1)) {
          c.or(neighborSet[w]);
        }
        toBeScanned = c.subtract(save);
        toBeScanned.andNot(separator);
      }
      if (separator.isSubset(c)) {
        fulls.add(c.subtract(separator));
      }
      else {
        nonFulls.add(c.subtract(separator));
      }
      rest.andNot(c);
    }    
  }
  
  /**
   * Compute the connected component of this target graph after
   * the removal of the vertices in the given separator,
   * to which the given vertex belongs
   * Renamed from {@code getComponent}: Nov 18, 2019 by Hisao Tamaki
   * @param separator set of vertices to be removed
   * @v vertex for which the component belongs to
   * @return the connected component represented by a {@code XBitSet}
   */
  public XBitSet separatedComponent(XBitSet separator, int v) {
    assert !separator.get(v);

    XBitSet c = (XBitSet) neighborSet[v].clone();
    XBitSet toBeScanned = c.subtract(separator);
    c.set(v);
    while (!toBeScanned.isEmpty()) {
      XBitSet save = (XBitSet) c.clone();
      for (int w = toBeScanned.nextSetBit(0); w >= 0;
          w = toBeScanned.nextSetBit(w + 1)) {
        c.or(neighborSet[w]);
      }
      toBeScanned = c.subtract(save);
      toBeScanned.andNot(separator);
    }
    return c.subtract(separator);
  }

  /**
   * Compute the connected component of the subgraph of this target induced by
   * the given vertex set and contains the give vertex. The neighborhood of
   * this component is also computed and set in the {@code toBeNeighbor} parameter
   * @param v the vertex
   * @param vs the vertex set
   * @param toBeNeighbors the {@code XBitSet} to be set to the neighborhood of 
   * the resulting component. Must be initially empty 
   * @return the computed component
   */
  public XBitSet componentOf(int v, XBitSet vs, XBitSet toBeSeparator) {
    XBitSet c = toBeSeparator;
    XBitSet toBeScanned = new XBitSet(n);
    c.set(v);
    toBeScanned.set(v);
    while (!toBeScanned.isEmpty()) {
      XBitSet save = (XBitSet) c.clone();
      for (int w = toBeScanned.nextSetBit(0); w >= 0;
          w = toBeScanned.nextSetBit(w + 1)) {
        c.or(neighborSet[w]);
      }
      toBeScanned = c.subtract(save);
      toBeScanned.and(vs);
    }
    XBitSet result = c.intersectWith(vs);
    toBeSeparator.andNot(vs);

    return result;
  }
  
  /**
   * Compute the connected component of the subgraph of this target, with 
   * the vertices in the given separator removed, that contains the given component
   * @param component the given component
   * @param separator the given separator
   * @return the resulting component
   */
  
  public XBitSet extendComponent(XBitSet component, XBitSet separator) {
    XBitSet c = (XBitSet) component.clone();
    XBitSet toBeScanned = (XBitSet) c.clone();
    while (!toBeScanned.isEmpty()) {
      XBitSet save = (XBitSet) c.clone();
      for (int w = toBeScanned.nextSetBit(0); w >= 0;
          w = toBeScanned.nextSetBit(w + 1)) {
        c.or(neighborSet[w]);
      }
      toBeScanned = c.subtract(save);
      toBeScanned.andNot(separator);
    }
    c.andNot(separator);
    return c;
  }

  /**
   * Compute connected components of the subgraph of this target
   * induced by the given vertex set
   * by means of iterated bit operations
   * Introduced: Nov 18, 2019 by Hisao Tamaki
   * @param vs the vertex set
   * @return the arrayList of connected components,
   * the vertex set of each component represented by a {@code XBitSet}
   */
  public ArrayList<XBitSet> componentsOf(XBitSet vs) {
    vs = (XBitSet) vs.clone();
    ArrayList<XBitSet> result = new ArrayList<>();

    for (int v = vs.nextSetBit(0); v >= 0;
        v = vs.nextSetBit(v + 1)) {
      XBitSet c = (XBitSet) neighborSet[v].clone();
      XBitSet toBeScanned = c.intersectWith(vs);
      c.set(v);
      while (!toBeScanned.isEmpty()) {
        XBitSet save = (XBitSet) c.clone();
        for (int w = toBeScanned.nextSetBit(0); w >= 0;
            w = toBeScanned.nextSetBit(w + 1)) {
          c.or(neighborSet[w]);
        }
        toBeScanned = c.subtract(save);
        toBeScanned.and(vs);
      }
      result.add(c.intersectWith(vs));
      vs.andNot(c);
    }

    return result;
  }

  /**
   * Compute connected components of this target graph after
   * the removal of the vertices in the given separator,
   * by means of iterated bit operations
   * Renamed from {@code getComponents}: Nov 18, 2019 by Hisao Tamaki
   * @param separator set of vertices to be removed
   * @return the arrayList of connected components,
   * the vertex set of each component represented by a {@code XBitSet}
   */
  public ArrayList<XBitSet> separatedComponents(XBitSet separator) {
    ArrayList<XBitSet> result = new ArrayList<XBitSet>();
    XBitSet rest = all.subtract(separator);
    for (int v = rest.nextSetBit(0); v >= 0;
        v = rest.nextSetBit(v + 1)) {
      XBitSet c = (XBitSet) neighborSet[v].clone();
      XBitSet toBeScanned = c.subtract(separator);
      c.set(v);
      while (!toBeScanned.isEmpty()) {
        XBitSet save = (XBitSet) c.clone();
        for (int w = toBeScanned.nextSetBit(0); w >= 0;
            w = toBeScanned.nextSetBit(w + 1)) {
          c.or(neighborSet[w]);
        }
        toBeScanned = c.subtract(save);
        toBeScanned.andNot(separator);
      }
      result.add(c.subtract(separator));
      rest.andNot(c);
    }
    return result;
  }
  
  /**
   * Compute the components of a given set that are
   * full components of a specified separator
   * @param separator separator
   * @param scope the vertex set from which the components are obtained
   * @return the arrayList of the full components,
   * the vertex set of each component represented by a {@code XBitSet}
   */
  
  public ArrayList<XBitSet> fullComponentsFrom(XBitSet separator, XBitSet scope) {
    ArrayList<XBitSet> result = new ArrayList<XBitSet>();
    XBitSet rest = (XBitSet) scope.clone();
    for (int v = rest.nextSetBit(0); v >= 0;
        v = rest.nextSetBit(v + 1)) {
      XBitSet c = (XBitSet) neighborSet[v].clone();
      XBitSet toBeScanned = c.subtract(separator);
      c.set(v);
      while (!toBeScanned.isEmpty()) {
        XBitSet save = (XBitSet) c.clone();
        for (int w = toBeScanned.nextSetBit(0); w >= 0;
          w = toBeScanned.nextSetBit(w + 1)) {
          c.or(neighborSet[w]);
        }
        toBeScanned = c.subtract(save);
        toBeScanned.andNot(separator);
      }
      if (separator.isSubset(c)) {
        result.add(c.subtract(separator));
      }
      rest.andNot(c);
    }
    return result;
  }

  
  /**
   * Compute the full components associated with the given separator,
   * by means of iterated bit operations
   * @param separator set of vertices to be removed
   * @return the arrayList of full components,
   * the vertex set of each component represented by a {@code XBitSet}
   */
  public ArrayList<XBitSet> fullComponents(XBitSet separator) {
    ArrayList<XBitSet> result = new ArrayList<XBitSet>();
    XBitSet rest = all.subtract(separator);
    for (int v = rest.nextSetBit(0); v >= 0;
        v = rest.nextSetBit(v + 1)) {
      XBitSet c = (XBitSet) neighborSet[v].clone();
      XBitSet toBeScanned = c.subtract(separator);
      c.set(v);
      while (!toBeScanned.isEmpty()) {
        XBitSet save = (XBitSet) c.clone();
        for (int w = toBeScanned.nextSetBit(0); w >= 0;
          w = toBeScanned.nextSetBit(w + 1)) {
          c.or(neighborSet[w]);
        }
        toBeScanned = c.subtract(save);
        toBeScanned.andNot(separator);
      }
      if (separator.isSubset(c)) {
        result.add(c.subtract(separator));
      }
      rest.andNot(c);
    }
    return result;
  }
  
  /**
   * Find a full component associated with the given separator
   * @param separator set of vertices to be removed
   * @return a full components associated with the separator; 
   * {@code null} if none is found 
   */
  public XBitSet aFullComponent(XBitSet separator) {
    XBitSet rest = all.subtract(separator);
    for (int v = rest.nextSetBit(0); v >= 0;
        v = rest.nextSetBit(v + 1)) {
      XBitSet c = (XBitSet) neighborSet[v].clone();
      XBitSet toBeScanned = c.subtract(separator);
      c.set(v);
      while (!toBeScanned.isEmpty()) {
        XBitSet save = (XBitSet) c.clone();
        for (int w = toBeScanned.nextSetBit(0); w >= 0;
          w = toBeScanned.nextSetBit(w + 1)) {
          c.or(neighborSet[w]);
        }
        toBeScanned = c.subtract(save);
        toBeScanned.andNot(separator);
      }
      if (separator.isSubset(c)) {
        return c.subtract(separator);
      }
      rest.andNot(c);
    }
    return null;
  }

  /**
   * Decides if the given vertex set is a minimal separator of
   * this target graph
   * Added: Nov 18, 2019 by Hisao Tamaki
   * @param vs the vertex set
   * @return {@cod true} if the vertex set is a minimal separator
   * {@code false} otherwise
   *    */
  public boolean isMinimalSeparator(XBitSet vs) {
    return fullComponents(vs).size() >= 2;
  }  

  /**
   * Decides if the given vertex set is a potential maixmal clique of
   * this target graph
   * @param vs the vertex set
   * @return {@cod true} if the vertex set is a potential maximal clique
   * {@code false} otherwise
   *    */
  public boolean isPMC(XBitSet vs) {
    int[] va = vs.toArray();
    XBitSet[] toCover = new XBitSet[va.length];
    for (int i = 0; i < va.length; i++) {
      toCover[i] = vs.subtract(neighborSet[va[i]]);
      toCover[i].clear(va[i]);
    }
    ArrayList<XBitSet> components = separatedComponents(vs);
    for (XBitSet compo: components) {
      XBitSet sep = neighborSet(compo);
      if (sep.equals(vs)) {
        return false;
      }
      for (int i = 0; i < va.length; i++) {
        if (sep.get(va[i])) {
          toCover[i].andNot(sep);
        }
      }
    }
    for (int i = 0; i < va.length; i++) {
      if (!toCover[i].isEmpty()) {
        return false;
      }
    }
    return true;
  }
  
  /**
   * Decides if the given connected vertex set is minimally separated, i.e.
   * its neighborhood is a minimal separator
   * Added: Nov 18, 2019 by Hisao Tamaki
   * @param connected the connected vertex set
   * @return {@cod true} if the vertex set is minimally separated
   * {@code false} otherwise
   *    */
  public boolean isMinimallySeparated(XBitSet connected) {
    assert isConnected(connected);
    XBitSet separator = neighborSet(connected);
    XBitSet rest = all.subtract(connected);
    rest.andNot(separator);
    ArrayList<XBitSet> components = componentsOf(rest);
    for (XBitSet compo: components) {
      if (isFullComponent(compo, separator)) {
        return true;
      }
    }
    return false;
  }
  
  /**
   * Decides if the given vertex set is a separator free of full components
   * Added: Nov 18, 2019 by Hisao Tamaki
   * @param separator vertex set to be examined
   * @return {@cod true} if the vertex set is full component free
   * {@code false} otherwise
   *    */
  public boolean isFullComponentFree(XBitSet separator) {
    ArrayList<XBitSet> result = new ArrayList<XBitSet>();
    XBitSet rest = all.subtract(separator);
    for (int v = rest.nextSetBit(0); v >= 0;
        v = rest.nextSetBit(v + 1)) {
      XBitSet c = (XBitSet) neighborSet[v].clone();
      XBitSet toBeScanned = c.subtract(separator);
      c.set(v);
      while (!toBeScanned.isEmpty()) {
        XBitSet save = (XBitSet) c.clone();
        for (int w = toBeScanned.nextSetBit(0); w >= 0;
          w = toBeScanned.nextSetBit(w + 1)) {
          c.or(neighborSet[w]);
        }
        toBeScanned = c.subtract(save);
        toBeScanned.andNot(separator);
      }
      if (separator.isSubset(c)) {
        return true;
      }
      rest.andNot(c);
    }
    return false;
  }
  
  /**
   *  Decides if a connected vertex set {@code component} is a full component
   *  associated with the given {@code separator}.
   *  @param component the connected set of vertices to be examined
   *  @param separator the separator
   */
  public boolean isFullComponent(XBitSet component, XBitSet separator) {
    for (int v = separator.nextSetBit(0); v >= 0; 
          v = separator.nextSetBit(v + 1)) {
      if (component.isDisjoint(neighborSet[v])) {
        return false;
      }
    }
    return true;
  }
  
  /**
   *  Decides if a connected vertex set {@code component} is inbound, given all the full compoents
   *  of the neighborSet of {@code component}
   *  @param component the connected set of vertices to be examined
   *  @param fullComponents the list of full components associated with the neighborhood of {@code component}
   */
   public boolean isInbound(XBitSet component, ArrayList<XBitSet> fullComponents) {
     int v0 = component.nextSetBit(0);
     for (XBitSet full: fullComponents) {
       if (full.nextSetBit(0) < v0) {
         return true;
       }
     }
     return false;
   }
  /**
   * Checks if the given induced subgraph of this target graph is connected.
   * @param vertices the set of vertices inducing the subraph
   * @return {@code true} if the subgrpah is connected; {@code false} otherwise
   */

  public boolean isConnected(XBitSet vertices) {
    int v = vertices.nextSetBit(0);
    if (v < 0) {
      return true;
    }

    XBitSet c = (XBitSet) neighborSet[v].clone();
    XBitSet toScan = c.intersectWith(vertices);
    c.set(v);
    while (!toScan.isEmpty()) {
      XBitSet save = (XBitSet) c.clone();
      for (int w = toScan.nextSetBit(0); w >= 0;
        w = toScan.nextSetBit(w + 1)) {
        c.or(neighborSet[w]);
      }
      toScan = c.subtract(save);
      toScan.and(vertices);
    }
    return vertices.isSubset(c);
  }

  /**
   * Checks if the given induced subgraph of this target graph is biconnected.
   * @param vertices the set of vertices inducing the subraph
   * @return {@code true} if the subgrpah is biconnected; {@code false} otherwise
   */
  public boolean isBiconnected(BitSet vertices) {
//    if (!isConnected(vertices)) {
//      return false;
//    }
    dfCount = 1;
    dfn = new int[n];
    low = new int[n];

    for (int v = 0; v < n; v++) {
      if (!vertices.get(v)) {
        dfn[v] = -1;
      }
    }

    int s = vertices.nextSetBit(0);
    dfn[s] = dfCount++;
    low[s] = dfn[s];

    boolean first = true;
    for (int v = neighborSet[s].nextSetBit(0); v >= 0; 
        v = neighborSet[s].nextSetBit(v + 1)) {
      if (dfn[v] != 0) {
        continue;
      }
      if (!first) {
        return false;
      }
      boolean b = dfsForBiconnectedness(v);
      if (!b) return false;
      else {
        first = false;
      }
    }
    return true;
  }

  /**
   * Depth-first search for deciding biconnectivigy.
   * @param v vertex to be visited
   * @return {@code true} if articulation point is found
   * in the search starting from {@cod v}, {@false} otherwise
   */
  private boolean dfsForBiconnectedness(int v) {
    dfn[v] = dfCount++;
    low[v] = dfn[v];
    for (int w = neighborSet[v].nextSetBit(0); w >= 0; 
        w = neighborSet[v].nextSetBit(w + 1)) {
      if (dfn[w] > 0 && dfn[w] < low[v]) {
        low[v] = dfn[w];
      }
      else if (dfn[w] == 0) {
        boolean b = dfsForBiconnectedness(w);
        if (!b) {
          return false;
        }
        if (low[w] >= dfn[v]) {
          return false;
        }
        if (low[w] < low[v]) {
          low[v] = low[w];
        }
      }
    }
    return true;
  }


  /**
   * Checks if the given induced subgraph of this target graph is triconnected.
   * This implementation is naive and call isBiconnected n times, where n is
   * the number of vertices
   * @param vertices the set of vertices inducing the subraph
   * @return {@code true} if the subgrpah is triconnected; {@code false} otherwise
   */
  public boolean isTriconnected(BitSet vertices) {
    if (!isBiconnected(vertices)) {
      return false;
    }

    BitSet work = (BitSet) vertices.clone();
    int prev = -1;
    for (int v = vertices.nextSetBit(0); v >= 0;
        v = vertices.nextSetBit(v + 1)) {
      if (prev >= 0) {
        work.set(prev);
      }
      prev = v;
      work.clear(v);
      if (!isBiconnected(work)) {
        return false;
      }
    }
    return true;
  }

  /**
   * Compute articulation vertices of the subgraph of this
   * target graph induced by the given set of vertices
   * Assumes this subgraph is connected; otherwise, only
   * those articulation vertices in the first connected component
   * are obtained.
   *
   * @param vertices the set of vertices of the subgraph
   * @return the set of articulation vertices
   */
  public XBitSet articulations(BitSet vertices) {
    articulationSet = new XBitSet(n);
    dfCount = 1;
    dfn = new int[n];
    low = new int[n];

    for (int v = 0; v < n; v++) {
      if (!vertices.get(v)) {
        dfn[v] = -1;
      }
    }

    depthFirst(vertices.nextSetBit(0));
    return articulationSet;
  }

  /**
   * Depth-first search for listing articulation vertices.
   * The articulations found in the search are
   * added to the {@code XBitSet articulationSet}.
   * @param v vertex to be visited
   */
  private void depthFirst(int v) {
    dfn[v] = dfCount++;
    low[v] = dfn[v];
    for (int w = neighborSet[v].nextSetBit(0); w >= 0; 
        w = neighborSet[v].nextSetBit(w + 1)) {
      if (dfn[w] > 0) {
        low[v] = Math.min(low[v], dfn[w]);
      }
      else if (dfn[w] == 0) {
        depthFirst(w);
        if (low[w] >= dfn[v] &&
            (dfn[v] > 1 || !lastNeighbor(v, w))){
          articulationSet.set(v);
        }
        low[v] = Math.min(low[v], low[w]);
      }
    }
  }

  /**
   * Decides if the given neighbor is the effectively
   * last neighbor array of the given vertex,
   * ignoring vertices not in the current subgraph
   * considered, which is known by their dfn being -1.
   * @param v the vertex in question
   * @param w the neighbor vertex in question
   * @return {@code true} if {@code w} is effectively
   * the last neihgbor of vertex {@code v};
   * {@code false} otherwise.
   */

  private boolean lastNeighbor(int v, int w) {
    for (int z = neighborSet[v].nextSetBit(w + 1); z >= 0; 
        z = neighborSet[v].nextSetBit(z + 1)) {
      if (dfn[z] == 0) {
        return false;
      }
    }
    return true;
  }

  /**
   * Decides if the given vertex set is independent in this target graph.
   * @param vs the vertex set
   * @return {@code true} if {@code vs} is an independent set, 
   * {@code false} otherwise.
   */

  public boolean isIndependent(XBitSet vs) {
    for (int v = vs.nextSetBit(0); v >= 0; v = vs.nextSetBit(v + 1)) {
      if (vs.intersects(neighborSet[v])) {
        return false;
      }
    }
    return true;
  }

  /**
   * Decides if the given vertex set is a clique of this target graph.
   * @param vs the vertex set
   * @return {@code true} if {@code vs} is a clique
   * {@code false} otherwise.
   */

  public boolean isClique(XBitSet vs) {
    for (int v = vs.nextSetBit(0); v >= 0; v = vs.nextSetBit(v + 1)) {
      XBitSet nb = (XBitSet) neighborSet[v].clone();
      nb.set(v);
      if (!vs.isSubset(nb)) {
        return false;
      }
    }
    return true;
  }

  
  /**
   * Decides if the given vertex set is an almost-clique of this target graph.
   * @param vs the vertex set
   * @return {@code true} if {@code vs} is an almost-clique
   * {@code false} otherwise.
   */

  public boolean isAlmostClique(XBitSet vs) {
    for (int v = vs.nextSetBit(0); v >= 0; v = vs.nextSetBit(v + 1)) {
      if (isClique(vs.removeBit(v))) {
        return true;
      }
    }
    return false;
  }

  /**
   * Decides if the given pair of vertex sets forms a biclique of this target graph.
   * @param x the first vertex set
   * @param y the second vertex set
   * @return {@code true} if the pair {@code (x, y)} is a biclique
   * {@code false} otherwise.
   */

  public boolean isBiclique(XBitSet x, XBitSet y) {
    for (int v = x.nextSetBit(0); v >= 0; v = x.nextSetBit(v + 1)) {
      if (!y.isSubset(neighborSet[v])) {
        return false;
      }
    }
    return true;
  }

  /**
   * Decides if the given vertex set is a cliquish in this target graph, 
   * in the sense that filling the neighborhood of each component separated
   * by this vertex set makes the vertex set into a clique.
   * @param vs the vertex set
   * @return {@code true} if {@code vs} is cliquish
   * {@code false} otherwise.
   */

  public boolean isCliquish(XBitSet vs) {
    return isCliquish(vs, separatedComponents(vs));
  }
  
  /**
   * Decides if the given vertex set is a cliquish in this target graph, 
   * in the sense that filling the neighborhood of each component separated
   * by this vertex set makes the vertex set into a clique.
   * @param vs the vertex set
   * @param components AraryList of components the target graph separated by vs
   * @return {@code true} if {@code vs} is cliquish
   * {@code false} otherwise.
   */

  public boolean isCliquish(XBitSet vs, ArrayList<XBitSet> components) {
    XBitSet[] neighborhoods = new XBitSet[components.size()];
    for (int i = 0; i < components.size(); i++) {
      neighborhoods[i] = neighborSet(components.get(i));
    }
        
    for (int v = vs.nextSetBit(0); v >= 0; v = vs.nextSetBit(v + 1)) {
      XBitSet na = vs.subtract(neighborSet[v]);
      na.clear(v);
      for (int w = na.nextSetBit(0); w >= 0; w = na.nextSetBit(w + 1)) {
        boolean covered = false;
        for (XBitSet nb: neighborhoods) {
          if (nb.get(v) && nb.get(w)) {
            covered = true;
          }
        }
        if (!covered) {
          return false;
        }
      }
    }
    return true;
  }

  /** 
   * clear edges in the specified vertex set
   * @param vertexSet vertex set to be cleared
   */
  public void clear(XBitSet vertexSet) {
    for (int v = vertexSet.nextSetBit(0); v >= 0;
        v = vertexSet.nextSetBit(v + 1)) {
      neighborSet[v].andNot(vertexSet);
    }
  }

  
  /** 
   * fill the specified vertex set into a clique
   * bug fixed March 17, 2019: was adding self loops
   * @param vertexSet vertex set to be filled 
   */
  public void fill(XBitSet vertexSet) {
    for (int v = vertexSet.nextSetBit(0); v >= 0;
        v = vertexSet.nextSetBit(v + 1)) {
      XBitSet missing = vertexSet.subtract(neighborSet[v]);
      missing.clear(v);
      for (int w = missing.nextSetBit(v + 1); w >= 0;
          w = missing.nextSetBit(w + 1)) {
        addEdge(v, w);
      }
    }
  }
  
  /** 
   * count the number of missing edges in the given vertex set
   * @param vertexSet vertex set for which the missing edges are counted
   * @return the number of missing edges
   */
  public int fillCount(XBitSet vertexSet) {
    int count = 0;
    for (int v = vertexSet.nextSetBit(0); v >= 0;
        v = vertexSet.nextSetBit(v + 1)) {
      XBitSet missing = vertexSet.subtract(neighborSet[v]);
      count += missing.cardinality() - 1;
    }
    return count / 2;
  }
  
  /** 
   * fill the specified vertex set into a clique
   * @param vertices int array listing the vertices in the set
   */
  public void fill(int[] vertices) {
    for (int i = 0; i < vertices.length; i++) {
      for (int j = i + 1; j < vertices.length; j++) {
        addEdge(vertices[i], vertices[j]);
      }
    }
  }

  /** 
   * tests if the target graph is a minimal triangulation of the given graph
   * assumes that the target graph is chordal
   * added Dec 3, 2019, Hisao Tamaki
   * @param g the given graph  
   * @return {@code true} if the target graph is a minimal triangulation of {@code g}
   * {@code false} otherwise.
   */
  public boolean isMinimalTriangulationOf(Graph g) {
    assert n == g.n;
    Graph h = g.copy();
    XBitSet remaining = (XBitSet) all.clone();
    Queue<Integer> queue = new LinkedList<>();
    for (int v= 0; v < n; v++) {
      if (isClique(neighborSet[v])) {
        queue.add(v);
      }
    }
    while (!queue.isEmpty()) {
      int v = queue.remove();
      remaining.clear(v);
      XBitSet forwardNeighbors = neighborSet[v].intersectWith(remaining);
      if (g.isMinimalSeparator(forwardNeighbors)) {
        h.fill(forwardNeighbors);
      }
      for (int w = forwardNeighbors.nextSetBit(0); w >= 0;
          w = forwardNeighbors.nextSetBit(w + 1)) {
        if (isClique(neighborSet[w].intersectWith(remaining))) {
          queue.add(w);
        }
      }
    }
    assert remaining.isEmpty(): "the target graph is not chordal";
    return numberOfEdges() == h.numberOfEdges();
  }

  /**
   * tests if the target graph is a minimal triangulation of the given graph
   * @param g the given graph  
   * @return {@code true} if the target graph is a minimal triangulation of {@code g}
   * {@code false} otherwise.
   */
  public boolean isBrutelyMinimalTriangulationOf(Graph g) {
    if (!isChordal()) {
      return false;
    }
    Graph h = copy();
    for (int v = 0; v < n; v++) {
      XBitSet nb = neighborSet[v].subtract(g.neighborSet[v]);
      for (int w = nb.nextSetBit(v + 1); w >= 0; w = nb.nextSetBit(w + 1)) {
        h.removeEdge(v, w);
        if (h.isChordal()) {
          return false;
        }
        h.addEdge(v, w);
      }
    }
    return true;
  }
  

  /**
   * tests if the target graph is chordal
   * added Dec 3, 2019, Hisao Tamaki
   * @return {@code true} if the target graph is chordal
   * {@code false} otherwise.
   */
  public boolean isChordal() {  
    for (int v = 0; v < n; v++) {
      if (!isLBSimplicial(v)) {
        return false;
      }
    }
    return true;
  }
  
  /** 
  * tests if the given vertex is LB-Simplicial in the target graph
  * added Dec 3, 2019, Hisao Tamaki
  * @param v the vertex for which the test is performed
  * @return {@code true} if v is LB-simplicial in the target graph, 
  * {@code false} otherwise.
  */
  public boolean isLBSimplicial(int v) {
    XBitSet closure = (XBitSet) neighborSet[v].clone();
    closure.set(v);
    ArrayList<XBitSet> components = separatedComponents(closure);
    for (XBitSet compo: components) {
      if (!isClique(neighborSet(compo))) {
        return false;
      }
    }
    return true;
  }
  
  /** list all maximal cliques of this graph
   * Naive implementation, should be replaced by a better one
   * @return
   */
  public ArrayList<XBitSet> listMaximalCliques() {
    ArrayList<XBitSet> list = new ArrayList<>();
    XBitSet subg = new XBitSet(n);
    XBitSet cand = new XBitSet(n);
    XBitSet qlique = new XBitSet(n);
    subg.set(0,n);
    cand.set(0,n);
    listMaximalCliques(subg, cand, qlique, list);
    return list;
  }

  /**
   * Auxiliary recursive method for listing maximal cliques
   * Adds to {@code list} all maximal cliques
   * @param subg
   * @param cand
   * @param clique
   * @param list
   */
  private void listMaximalCliques(XBitSet subg, XBitSet cand,
      XBitSet qlique, ArrayList<XBitSet> list) {
      if(subg.isEmpty()){
        list.add((XBitSet)qlique.clone());
        return;
      }
      int max = -1;
      XBitSet u = new XBitSet(n);
      for(int i=subg.nextSetBit(0);i>=0;i=subg.nextSetBit(i+1)){
        XBitSet tmp = new XBitSet(n);
        tmp.set(i);
        tmp = neighborSet(tmp);
        tmp.and(cand);
        if(tmp.cardinality() > max){
          max = tmp.cardinality();
          u = tmp;
        }
      }
      XBitSet candu = (XBitSet) cand.clone();
      candu.andNot(u);
      while(!candu.isEmpty()){
        int i = candu.nextSetBit(0);
        XBitSet tmp = new XBitSet(n);
        tmp.set(i);
        qlique.set(i);
        XBitSet subgq = (XBitSet) subg.clone();
        subgq.and(neighborSet(tmp));
        XBitSet candq = (XBitSet) cand.clone();
        candq.and(neighborSet(tmp));
        listMaximalCliques(subgq,candq,qlique,list);
        cand.clear(i);
        candu.clear(i);
        qlique.clear(i);
      }
  }

  /**
   * Saves this target graph in the file specified by a path string,
   * in .gr format.
   * A stack trace will be printed if the file is not available for writing
   * @param path the path-string
   */
  public void save(String path) {
    File outFile = new File(path);
    PrintStream ps;
    try {
      ps = new PrintStream(new FileOutputStream(outFile));
      writeTo(ps);
      ps.close();
    } catch (FileNotFoundException e) {
      e.printStackTrace();
    }
  }
  /**
   * Write this target graph in .gr format to the given
   * print stream.
   * @param ps print stream
   */
  public void writeTo(PrintStream ps) {
    ps.println("p tw " + n + " " + numberOfEdges());
    for (int i = 0; i < n; i++) {
      for (int k = neighborSet[i].nextSetBit(0); k >= 0;  
          k = neighborSet[i].nextSetBit(k + 1)) {
        if (i < k) {
          ps.println((i + 1) + " " + (k + 1));
        }
      }
    }
  }

  /**
   * Create a copy of this target graph
   * @return the copy of this graph
   */
  public Graph copy() {
    Graph tmp = new Graph(n);
    for (int v = 0; v < n; v++) {
      tmp.neighborSet[v] = (XBitSet) neighborSet[v].clone();
//      for (int w = neighborSet[v].nextSetBit(0); w >= 0;  
//          w = neighborSet[v].nextSetBit(w + 1)) {
//        tmp.addEdge(v, w);
//      }
    }
    return tmp;
  }

  /**
   * Create the complement this target graph
   * @return the complement of this graph
   */
  public Graph complement() {
    Graph tmp = new Graph(n);
    for (int v = 0; v < n; v++) {
      tmp.neighborSet[v] = all.subtract(neighborSet[v]);
      tmp.neighborSet[v].clear(v);
    }
    return tmp;
  }

  /**
   * Check consistency of this graph
   * 
   */
  public void checkConsistency() throws RuntimeException {
    for (int v = 0; v < n; v++) {
      if (neighborSet[v].get(v)) {
        throw new RuntimeException("self loop on " + v);
      }
      for (int w = 0; w < n; w++) {
        if (neighborSet[v].get(w) &&
            !neighborSet[w].get(v)) {
          throw new RuntimeException("neighborSets inconsistent " + v + ", " + w);
        }
      }
    }
  }
  /**
   * Create a random graph with the given number of vertices and
   * the given number of edges
   * @param n the number of vertices
   * @param m the number of edges
   * @param seed the seed for the pseudo random number generation
   * @return {@code Graph} instance constructed
   */
  public static Graph randomGraph(int n, int m, int seed) {
    Random random = new Random(seed);
    Graph g = new Graph(n);

    int k = 0;
    int j = 0;
    int m0 = n * (n - 1) / 2;
    for (int v = 0; v < n; v++) {
      for (int w = v + 1; w < n; w++) {
        int r = random.nextInt(m0 - j);
        if (r < m - k) {
          g.addEdge(v, w);
          g.addEdge(w, v);
          k++;
        }
        j++;
      }
    }
    return g;
  }
  
  /**
   * Create a random graph with the given number of vertices and
   * the given edge probability
   * @param n the number of vertices
   * @param np the numerator of the probability
   * @param nd the denominator of the probability
   * @param seed the seed for the pseudo random number generation
   * @return {@code Graph} instance constructed
   */
  public static Graph randomGraph(int n, int np, int dp, int seed) {
    Random random = new Random(seed);
    Graph g = new Graph(n);

    for (int v = 0; v < n; v++) {
      for (int w = v + 1; w < n; w++) {
        int r = random.nextInt(dp);
        if (r < np) {
          g.addEdge(v, w);
        }
      }
    }
    return g;
  }

  /**
   * Test if the target graph is isomorphic to the given graph under
   * the given bijection
   * @param g the graph to be tested for the isomorphism
   * @param the bijection from the target graph to the given graph
   * @return {@code true} if and only if the isomorphism holds
   */
  public boolean isIsomorphic(Graph g, int[] bijection) {
    assert n == g.n;
    
    for (int v = 0; v < n; v++) {
//      System.out.println(v + ": " + neighborSet[v]);
//      System.out.println("   ->    " + neighborSet[v].convert(bijection));
//      System.out.println("      " + bijection[v] + ":" + g.neighborSet[bijection[v]]);
      if (!neighborSet[v].convert(bijection).equals(g.neighborSet[bijection[v]])) {
        return false;
      }
    }
    
    return true;
  }

  /**
   * Assert that this graph is symmetric
   * 
   */
  public void assertSymmetric() {
    for (int u = 0; u < n; u++) {
      XBitSet nb = neighborSet[u];
      for (int v = nb.nextSetBit(0); v >= 0; v = nb.nextSetBit(v + 1)) {
        assert neighborSet[v].get(u): v + " is in the nb of " + u + 
        " but not the other way round";
      }
    }
  }
  
  @Override
  public int hashCode() {
    int c = 0;
    for (int v = 0; v < n; v++) {
      c += neighborSet[v].hashCode();
    }
    return c;
  }
  
  @Override
  public boolean equals(Object x) {
    Graph g = (Graph) x;
    for (int v = 0; v < n; v++) {
      if (!neighborSet[v].equals(g.neighborSet[v])) {
        return false;
      }
    }
    return true;
  }
  
  public void printRaw() {
    printRaw(System.out);
  }
  
  public void printRaw(PrintStream ps) {
    ps.println(n + " " + numberOfEdges());
    for (int v = 0; v < n; v++) {
      System.out.println(v + ": " + neighborSet[v]);
    }
  }

  public static void main(String args[]) {
    // an example of the use of random graph generation
    for (int n = 50; n <= 90; n += 10) {
      for (int m = 3 * n; m <= 10 * n; m += n) {
        for (int s = 1; s <= 5; s++) {
          Graph g = randomGraph(n, m, s);
          String ns = Integer.toString(n);
          ns = "000".substring(ns.length()) + ns;
          String ms = Integer.toString(m);
          ms = "000".substring(ms.length()) + ms;
          g.save("instance/random/gnm_" + ns + "_" + ms + "_" + s + ".gr");
        }
      }
    }
  }


}
