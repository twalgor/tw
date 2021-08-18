This repository is for implementations of treewidth algorithms.

Currently, it contains the source code of the algorithms 
described in the following manuscript:

@misc{tamaki2021heuristic,
      title={A heuristic for listing almost-clique minimal separators of a graph}, 
      author={Hisao Tamaki},
      year={2021},
      eprint={2108.07551},
      archivePrefix={arXiv},
      primaryClass={cs.DS}
}

It includes an implementation of the exact algorithm for treewidth described in:

@inproceedings{tamaki2019computing,
  title={Computing treewidth via exact and heuristic lists of minimal separators},
  author={Tamaki, Hisao},
  booktitle={International Symposium on Experimental Algorithms},
  pages={219--236},
  year={2019},
  organization={Springer}
}

The main purposes of this repository are to make the published experimental results reproducible 
and to make the code available for research use. I plan to develop a production level implementations
in the coming few years. If you use the code in this repository in your research and
publish results from that research, 
please cite this repository and/or a relevant one of the above papers.

## How to use
The code is written in Java. You need JDK1.8 or higher to compile and run it.  
The current entry points of the code are the following classes.

io.github.twalgor.main.ACSD
* compute an almost-clique separator decomposition of a given graph as described in the first paper above
* two arguments must be provided
 * the first argument is the path to the graph file in the PACE gr format. 
 * the second argument is the path to the output file in the PACE td format. 
 * see https://pacechallenge.org/2017/treewidth/ for these formats

io.github.twalgor.main.ExactTW
* compute an optimal tree-decomposition of a given graph, using an implementation of the algorithm described in the second paper above
* there is an option to use preprocessing based on almost-clique separator decompositions described in the first paper above
* two arguments are mandatory
 * the first argument is the path to the graph file in the PACE gr format. 
 * the second argument is the path to the output file in the PACE td format. 
 * the third argument, which must be -acsd, specifies if provided that the preprocessing is used.




 


