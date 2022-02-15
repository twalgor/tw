This repository is for implementations of treewidth algorithms.

Currently, it contains the source code of the algorithms 
described in the following manuscripts:

1. @misc{tamaki2021heuristic,
      title={A heuristic for listing almost-clique minimal separators of a graph}, 
      author={Hisao Tamaki},
      year={2021},
      eprint={2108.07551},
      archivePrefix={arXiv},
      primaryClass={cs.DS}
}

2. @misc{tamaki2022heuristic,
      title={Heuristic computation of exact treewidth}, 
      author={Hisao Tamaki},
      year={2022},
      note={submitted for publication, will be available at arXiv soon}
}

It includes an implementation of the exact algorithm for treewidth described in the following paper:

A. @inproceedings{tamaki2019computing,
  title={Computing treewidth via exact and heuristic lists of minimal separators},
  author={Tamaki, Hisao},
  booktitle={International Symposium on Experimental Algorithms},
  pages={219--236},
  year={2019},
  organization={Springer}
}



The main purposes of this repository are to make the published experimental results reproducible and to make the code available for research use. 
I plan to develop a production level implementations
in the coming few years. If you use the code in this repository in your research and
publish results from that research, 
please cite this repository and/or a relevant one of the above papers.

## How to use
The code is written in Java. You need JDK1.8 or higher to compile and run it.  
The current entry points of the code are the following classes.

io.github.twalgor.main.UpLow
* compute upper and lower bounds of a given graph using algorithms described in the
manuscript 2 above. Two threads are used: one for the upper bound and the other for the lower bound. Both bounds are iteratively improved and the solver terminates when the upper and lower bounds become equal to each other.
* three argument must be provided
 * the first argument is the path to the graph file in the PACE gr format. 
 * the second argument is the path to the output file in the PACE td format, in which the tree-decomposition constructed by the upper bound algorithm is written. The content is overwritten every time a new upper bound is found. 
 * see https://pacechallenge.org/2017/treewidth/ for these formats
 * the third argument is the path to the out file in which the minors certifying the 
 computed lower bounds are written. The standard file extension for this file is ".mnr".
 The format of mnr files is described below.
 * mnr file format
 * Each line start with a keyword, unless it is a part of a certificate description. A keyword is one of the following.
  * title this line describes the title of the certificate
  * param this line describes values of some parameters used by the solver
  * graph_file this line shows the path to the input graph file
  * graph_size this line shows the number of vertices (n) and the number of edges (m) of the instance
  * largest_atom this line shows the number of vertices of the largest atom in the ACS-decomposition
  * certificate this line marks a beginning of a certificate, and shows 
   the width of the minor (width), the number of vertices of the minor (n), and the time
   in milliseconds spent for obtaining this lower bound since the beginning of the    computation (time).
  * For the certificate of n vertices, n lines follow the line with the "certificate" keyword, each describing a vertex of the minor.
  * Each line describing a minor vertex consists of the vertex number (0 -- n - 1), the    number of vertices in the original graph that are contracted into this vertex, and the list of those contracted vertices parenthesized by {}. **The vertex number of the original graph also starts from 0** unlike the gr format in which the vertex number starts from 1.
  * certificates are appended every time the solver finds a new lower bound.
 
io.github.twalgor.main.ACSD
* compute an almost-clique separator decomposition of a given graph as described in the manuscript 1 above
* two arguments must be provided
 * the first argument is the path to the graph file in the PACE gr format. 
 * the second argument is the path to the output file in the PACE td format. 
 * see https://pacechallenge.org/2017/treewidth/ for these formats

io.github.twalgor.main.ExactTW
* compute an optimal tree-decomposition of a given graph, using an implementation of the algorithm described in paper A above
* there is an option to use preprocessing based on almost-clique separator decompositions described in the first paper above
* two arguments are mandatory
 * the first argument is the path to the graph file in the PACE gr format. 
 * the second argument is the path to the output file in the PACE td format. 
 * the third argument, which must be -acsd, specifies if provided that the preprocessing is used.




 


