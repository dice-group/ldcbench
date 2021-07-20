package org.dice_research.ldcbench.generate;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

import org.dice_research.ldcbench.graph.GrphBasedGraph;
import org.junit.Ignore;

@Ignore
public class RandomG2File {
	public static void main(String[] args){
		int N;
		double degree;
		RandomRDF rg;
		rg = new RandomRDF("Barabasi Random RDF");
		GrphBasedGraph g;
		g = new GrphBasedGraph();
		/*ParallelBarabasiRDF rg;
		rg = new ParallelBarabasiRDF("Barabasi Random RDF");*/
		//DBpedia en scale
//		N = 1000000;    	degree=117.5;
//		N =43497; degree = 4.096;
//		N=40000;degree = 20;
        degree=100;

		//Yeast
//		N=82481;
//		degree=223684.0/N;

		//Toy
//		N=100;		degree=2.7;

//		rg.generateGraph(N,degree,123L,g);
		//g.getNumberOfNodes();
		rg.generateGraph(10.0, 100, 496, g);
//		rg.generateGraph( 40000,2.5, 123, g);
		N=g.getNumberOfNodes();
		System.out.println("N="+N);
		//public void saveToFile(String fname) {
//		String fname=String.format("D:\\RandParSWDF_inDeg_N%s_%.1f_123.txt",N,degree);
		String fname=String.format("D:\\RandomRDF_emptyIssue_N%d_%.1f_496.txt",N,degree);
			try {
				PrintWriter oout = new PrintWriter(new FileWriter(fname));
				for (int i = 0; i < N; i++) {
					int[] targets=g.outgoingEdgeTargets(i);
					for(int dist:targets)
						oout.println((i+1) + " " + (dist+1));
				}

				oout.close();
			} catch (IOException e) {
				System.out.println("Failed to open output file. ");
				e.printStackTrace();
			}
		//}
	}
}
