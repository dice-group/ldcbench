package org.dice_research.ldcbench.generate;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

import org.dice_research.ldcbench.generate.RandomCloudGraph;
import org.dice_research.ldcbench.graph.GrphBasedGraph;
public class RandomLODTest {
	int N=1069;
	RandomCloudGraph rg;
	GrphBasedGraph g;
	// Number of nodes in each type
    int[] typecounts= {500,400,100,69};
    // which types connect to which
    int[][] typeconn= {{1,1,0,0},{0,1,1,0},{0,0,1,1},{1,1,1,1}};
	//	private static Random generator ;
    @Before
	public void setUp() {
    	g = new GrphBasedGraph();
		rg = new RandomCloudGraph("Barabasi RandomLOD",typecounts,100,typeconn);		
	}

    @Test(expected = IllegalArgumentException.class)
	public void ValidDegree() throws IllegalArgumentException {
		rg.generateGraph(N,0.9,123L,g);
	}

    @Test(expected = IllegalArgumentException.class)
	public void maxDegree() throws IllegalArgumentException {
		rg.generateGraph(4.0,2,123L,g);
	}
    @Test
	public void LOD2017() {
    	System.out.println("Simulating LOD 2017...");
    	double degree=15654.0/N;
		//rg.generateGraph(5.0,200000,123L,g);
		rg.generateGraph(N,degree,123L,g);
		assertEquals("Number of nodes", N, g.getNumberOfNodes());
		assertEquals("Number of edges", 15654, g.getNumberOfEdges());
	   // test type counts
		int[] nodeTypes=rg.getNodeTypes();
		int[] tmptypecounts=new int[typecounts.length];
		for(int i=0; i < nodeTypes.length; i++) tmptypecounts[nodeTypes[i]]++;
		for(int i=0;i<typecounts.length;i++) 
			System.out.println(i+" :"+tmptypecounts[i]);
		
		for(int i=0;i<typecounts.length;i++) {
			assertEquals("Number of nodes in type: "+i, tmptypecounts[i], typecounts[i]);
		}
    }
}