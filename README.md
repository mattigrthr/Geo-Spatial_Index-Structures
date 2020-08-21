In this repository, we created different benchmarks and a workload generator to
compare several spatio-textual index structures.

The implemented index structures are a B-tree utilizing Google's S2 Geometry 
Library, the bottom-up approach for R-trees and GeoBroker.

We included some workloads as an example. You can create your own workloads
using our workload generator.

To start the benchmarks with the generated workloads, run the main method of
"BenchmarkManager.java".