//28/1/2019
//ToDo: add one method to generate graphs that takes the algorithm as a parameter. Done!

//package org.dice_research.ldcbench;
package org.dice_research.ldcbench.generate;

import java.util.Arrays;
import java.util.Random;

import org.dice_research.ldcbench.graph.GraphBuilder;

public class RandomRDF implements GraphGenerator{
 	protected Random generator;
//	protected XorShift1024StarRandom generator;
	public String name;

	public RandomRDF(String gname) {
		name = gname;
	}

/**
 * Samples numbers from 1 to n; m times without replacement using weights in wt
 * @param n number of values to choose from.
 * @param m number of values to choose.
 * @param wt the weights for the values in index from 1 to n (0 is ignored).
 * @return int array containing indexes of sampled nodes.
 */
	protected int[] weightedSampleWithoutReplacementwithaw(int n, int m, int[] wt) {
		/* with accumulated weight array (less efficient)*/
		int[] Res = new int[m];
		int[] aw = new int[n + 1];// accumulated weights
		aw[0] = 0;

		int last = aw.length - 1;
		int rand;
		int i;

		for (i = 1; i <= n; i++) {
			aw[i] = aw[i - 1] + wt[i];
		}

		for (int j = 0; j < m; j++) {
			rand = 1 + generator.nextInt(aw[last]);

			// find interval
			for (i = 1; i < n; i++) {
				if (rand <= aw[i]) {
					Res[j] = i;
					break;
				}
			}

			if (Res[j] == 0)
				Res[j] = n;

			// update probabilities to exclude taken
			if (j < m - 1) {
				int Difference = wt[Res[j]];
				for (i = Res[j]; i < aw.length; i++) {
					aw[i] -= Difference;
				}
			}
		}

		return (Res);
	}

protected int[] weightedSampleWithoutReplacementbswithawBS(int n, int m, int[] wt) {
/* with accumulated weight array and binary search (less efficient)*/
		int[] Res = new int[m];
		int[] aw = new int[n + 1];// accumulated weights
		aw[0] = 0;

		int last = aw.length - 1;
		int rand;
		int i;

		for (i = 1; i <= n; i++) {
			aw[i] = aw[i - 1] + wt[i];
		}

		for (int j = 0; j < m; j++) {
			rand =  generator.nextInt(aw[last]);

			// find interval
			Res[j] = Math.abs(Arrays.binarySearch(aw, rand)+1);

			//if (Res[j] == 0)
				//Res[j] = n;

			// update probabilities to exclude taken
			if (j < m - 1) {
				int Difference = wt[Res[j]];
				for (i = Res[j]; i < aw.length; i++) {
					aw[i] -= Difference;
				}
			}
		}

		return (Res);
	}

/**
 * Samples numbers from 1 to n; m times without replacement using weights in wt
 * @param n number of values to choose from.
 * @param m number of values to choose.
 * @param wt the weights for the values in index from 1 to n (0 is ignored).
 * @return int array containing indexes of sampled nodes.
 */
protected int[] weightedSampleWithoutReplacement(int n, int m, int[] wt) {
	/* without accumulated weights, select m from n without replacement */

	int[] Res = new int[m];

	int rand;
	int i;
    int Sum = 0;//=IntStream.of(wt).sum();
    int cntNNz = 0;//count of non zero values in wt, should be at least m :23.9.19
    
    for(i = 1; i <= n; i++) {
    	   Sum += wt[i];
    	   if(wt[i] > 0 ) cntNNz++;
    }
    
    if(Sum<=0) {
    	throw new IllegalArgumentException("Error, Sum<=0," + n +" m=" + m + " wt0=" + wt[0] + " Sum=" + Sum);    	
    }
    
    if(cntNNz < m) {
    	throw new IllegalArgumentException("Error, count nonzeros in weights: " + cntNNz + " < " + m +", n=" + n +
    			" wt0=" + wt[0] + " Sum=" + Sum);    	
    }
    
	int[] wt1=Arrays.copyOf(wt,n+1);
	
	for (int j = 0; j < m; j++) {
		rand =  generator.nextInt(Sum);

		// find interval
		for (i = 1; i <= n; i++) {
			rand-=wt1[i];
			if (rand < 0) {//to avoid getting nodes set to zero degree, must be less than
				Res[j] = i;
				Sum -= wt1[i];
				wt1[i] = 0;// to avoid replacement

				break;
			}
		}

		//System.out.println("Sample j="+ j + " i="+i+" Res[j]=" + Res[j]+ " rand="+rand+" Sum="+Sum+" n1="+n1);

	}

	return (Res);
}

	/**
	 * generate a random RDF graph using Barabasi algorithm and
	 * inverting some edges to be sure that the graph is connected (reachable from a single source).
	 * @param N the number of nodes
	 * @param degree the average degree of the nodes in the graph
	 * @param seed the random seed to be able to reproduce the results.
	 */
		protected void getBarabasiRDF(int N, double degree, long seed, GraphBuilder builder) {
			/* nodes are numbered from 1 to N */

			int indexToEdgeList ;// index to edge list
//			long t0 = System.currentTimeMillis();
//			long ti0 = System.currentTimeMillis();

			int nE=(int) Math.ceil(N*degree);
			int[] subj = new int[nE];
			int[] obj = new int[nE];
			int[] inDeg = new int[N + 1];
			Arrays.fill(inDeg, 1);
			inDeg[0] = 0;// not used
			int m = (int) Math.floor(degree);// average degree of graph

			indexToEdgeList=getInitGraph(N,degree,seed,subj,obj,inDeg,m);

			int P1 = (m + 1)*(N-m) - (nE - indexToEdgeList) + m ;
			if (P1<=m ) P1 = m+1;

			double biasedCoin=((m/2.0-1)/(m-1));
			//long ts10k=0;
			for (int i = m + 1; i <= N; i++) {
//				long ts1=System.currentTimeMillis();
				int[] tmp = weightedSampleWithoutReplacement((i - 1), m, inDeg);// #new links
		//		ts10k+=(System.currentTimeMillis()-ts1);

				 //in- link
				int vin_ix = generator.nextInt(m) ;
				if (vin_ix == m)
					vin_ix--;

				/*
				 * 8/2/2019: choose randomly the number of in-links [1,m],
				 * 	 1. select randomly one link to be in-link then
				 *   2. toss a biased coin ((m/2)-1 inlink,(m/2) outlink)
				 */
				for (int k = 0; k < m; k++) {
					if (k != vin_ix && (generator.nextDouble() > biasedCoin) ) {
						inDeg[tmp[k]] = inDeg[tmp[k]] + 1;
						subj[indexToEdgeList] = i;
						obj[indexToEdgeList] = tmp[k];
					} else {// inverted link
						inDeg[i] = inDeg[i] + 1;
						subj[indexToEdgeList] = tmp[k];
						obj[indexToEdgeList] = i;
					}
					indexToEdgeList++;
				}
				if(i==P1) {
					    m = m + 1;//second part
				        biasedCoin=((m/2.0-1)/(m-1));
				}

//				if (i % 10000 == 0) {
//					long ti1;
//					ti1= System.currentTimeMillis();
//					//ts10k=0;
//					ti0= System.currentTimeMillis();
//				}
			}

//			long t_barabasi = System.currentTimeMillis();

			int nEdges = indexToEdgeList;
			int[] idRange=builder.addNodes(N);//Range
			for(int i=0; i < nEdges; i++) {
				if(!builder.addEdge(subj[i]-1 + idRange[0], obj[i]-1+idRange[0], 0)) {
					System.out.println("Failed to add edge : i="+i+" subj="+subj[i]+" idRange[0]=" + idRange[0]+" obj[i]="+ obj[i]);
				};
			}
			int[] entranceNodes= {0};
			builder.setEntranceNodes(entranceNodes);

		//	long t_gbuilder = System.currentTimeMillis();
		}

/*-----------------------------------------------------------------------*/
		int getOneNodeLinks(int node,int m1,int indexToEdgeList,int[] subj,int[] obj,int[] inDeg){		
			/* finds m1 connections from/to to node*/
			
			int[] tmp = weightedSampleWithoutReplacement((node - 1), m1, inDeg);// #new links			
			double biasedCoin=((m1/2.0-1)/(m1-1));
			//in- link
			int vin_ix = generator.nextInt(m1) ;
			if (vin_ix == m1)
				vin_ix--;

			/*
			 * 8/2/2019: choose randomly the number of in-links [1,m],
			 * 	 1. select randomly one link to be in-link then
			 *   2. toss a biased coin ((m/2)-1 inlink,(m/2) outlink)
			 */
			biasedCoin=((m1/2.0-1)/(m1-1));
			for (int k = 0; k < m1; k++) {
				if (k != vin_ix && (generator.nextDouble() > biasedCoin) ) {
					inDeg[tmp[k]] = inDeg[tmp[k]] + 1;
					subj[indexToEdgeList] = node;
					obj[indexToEdgeList] = tmp[k];
				} else {// inverted link
					inDeg[node] = inDeg[node] + 1;
					subj[indexToEdgeList] = tmp[k];
					obj[indexToEdgeList] = node;
				}
				indexToEdgeList++;
			}
			return(indexToEdgeList);
		}
		
//		----------------------------------
		
		protected void getBarabasiRDFum(int N, double degree, long seed, GraphBuilder builder) {
			/* nodes are numbered from 1 to N */
	        /* uniform distribution for in/out typing*/

			int indexToEdgeList ;// index to edge list
//			long t0 = System.currentTimeMillis();
//			long ti0 = System.currentTimeMillis();

			int nE=(int) Math.ceil(N*degree/2);//19/8/2019
			int[] subj = new int[nE];
			int[] obj = new int[nE];
			int[] inDeg = new int[N + 1];
			Arrays.fill(inDeg, 1);
			inDeg[0] = 0;// not used
			int m = (int) Math.floor(degree/2);// average degree of graph

			indexToEdgeList=getInitGraph(N,degree,seed,subj,obj,inDeg,m);
            // redefine m after init
			//tail phase: fixed degree to get exact nE
			int P1 = (m + 1)*(N-m) - (nE - indexToEdgeList) + m ;
			if (P1<=m ) P1 = m + 1;

			
//			long ts10k=0;
			int cntE=indexToEdgeList;
			int i;
			try {
			for ( i = m + 1; i < N; i++) {
//				long ts1=System.currentTimeMillis();
				int m1= 1+ generator.nextInt(2*m-1);
				if(m1 >= i) m1=i-1;
				//if(m1 > 2*m) m1=2*m;
				if((N-i)>=(nE-indexToEdgeList-m1+1)) m1=1;// to have the same number of nodes
				cntE += m1;
				if(cntE >= nE) {
					m1-=(cntE-nE)-1;
				    cntE=nE-1;//reserve one for last node
				}
	           //System.out.println(i+": "+m1);
				indexToEdgeList=getOneNodeLinks(i,m1,indexToEdgeList,subj,obj,inDeg);
				if(i==P1) {
					    m = m + 1;//second part
				}

//				if (i % 10000 == 0) {
//					long ti1;
//					ti1= System.currentTimeMillis();
//					ts10k=0;
//					ti0= System.currentTimeMillis();
//				}
				//if(indexToEdgeList >= (nE-1)) break;
			}
			
			//---------- Links for last node ---------------------------//
//			System.out.println("crnt nE:"+indexToEdgeList+" required nE="+nE);
			// trailing node have a degree such that to get exact number of edges
			int mf=nE-indexToEdgeList;
			
			if(mf<=0) {
				System.out.println("No remaining edges for last node mf=" + mf);	
			}	else {
			   indexToEdgeList = getOneNodeLinks(i,mf,indexToEdgeList,subj,obj,inDeg);
			}
			
//			long t_barabasi = System.currentTimeMillis();
//            ##4/7/2019
			if(i < (N-1) ) {
            	System.out.println("Actual number of nodes:" + i + " instead of: " + N );
            	N = i;
            }

			//Grph structure ----------------------------------------------
			int nEdges = indexToEdgeList;
			int[] idRange=builder.addNodes(N);//Range
			for( i=0; i < nEdges; i++) {
				if(!builder.addEdge(subj[i]-1 + idRange[0], obj[i]-1+idRange[0], 0)) {
					System.out.println("Failed to add edge : i="+i+" subj="+subj[i]+" idRange[0]=" + idRange[0]+" obj[i]="+ obj[i]);
				};
			}
			int[] entranceNodes= {0};
			builder.setEntranceNodes(entranceNodes);

//			long t_gbuilder = System.currentTimeMillis();
			}
			catch(IllegalArgumentException ex) {
				throw new IllegalArgumentException("N=" + N + ", seed=" + seed + ", degree=" + degree +
						" msg from sampling:"+ex.getMessage());
			}
		}

//-------------------------------------------------------------------------------------------

		protected int getInitGraph(int N, double degree, long seed,int[] subj,int[]obj,int[] inDeg,int m) {
			int indexToEdgeList = 0;

//			if (degree < 1 && N>1) {
//				throw new IllegalArgumentException("When N>1, degree must be more than 1.");
//			}
			if (degree < (2-2/N) && N>1) {
				throw new IllegalArgumentException("When N>1, degree must be more than (2-2/N).");
			}
			
			if (degree > (N-1)) {// max links created at any step is N-1
				throw new IllegalArgumentException("Degree can NOT be more than (N-1).");
			}

			generator = new Random(seed);// seed

			if(m >1 ) {
				subj[indexToEdgeList] = 1;
				obj[indexToEdgeList] = 2;// first edge
				indexToEdgeList++;
				inDeg[2] = 2;
			}

			// initial part
			if (m > 2) {
				for (int i = 3; i <= m; i++) {
					int[] tmp = weightedSampleWithoutReplacement(i - 1, 2, inDeg);// new links
					//int[] tmp = weightedSampleWithoutReplacementbswithawBS(i - 1, 2, inDeg);// new links
					
					boolean randIndex = generator.nextBoolean();
					int vto;
					if (randIndex) {
						vto = 0;
					} else {
						vto = 1;
					}
					subj[indexToEdgeList] = i;
					obj[indexToEdgeList] = tmp[1 - vto];
					indexToEdgeList++;
					subj[indexToEdgeList] = tmp[vto];
					obj[indexToEdgeList] = i;
					indexToEdgeList++;// inverted link randomly
				}
			}

			return(indexToEdgeList);
		}

//--------------------------------------------------------------------------------------------
//		protected void getBarabasiRDFumChkRep(int N, double degree, long seed, GraphBuilder builder) {
//			/* nodes are numbered from 1 to N */
//			/* TreeSet is faster practically than HashSet */
//			/* overall performance worse than using sampling*/
//
//			int indexToEdgeList ;// index to edge list
//			long t0 = System.currentTimeMillis();
//			long ti0 = System.currentTimeMillis();
//
//			int nE=(int) Math.ceil(N*degree);
//			int[] subj = new int[nE];
//			int[] obj = new int[nE];
//			int[] inDeg = new int[N + 1];
//			Arrays.fill(inDeg, 1);
//			inDeg[0] = 0;// not used
//			int m = (int) Math.floor(degree);// average degree of graph
//
//			indexToEdgeList=getInitGraph(N,degree,seed,subj,obj,inDeg,m);
//
//			int P1 = (m + 1)*(N-m) - (nE - indexToEdgeList) + m ;
//			if (P1<=m ) P1 = m+1;
//
////			HashSet<Integer> tmp=new HashSet<>(2*(m+1));
//			TreeSet<Integer> tmp=new TreeSet<Integer>();
//			double biasedCoin=((m/2.0-1)/(m-1));
//			long ts10k=0;
//			int cntE=indexToEdgeList,ki,lastNode = 0;
//			long totalrepeats=0;
//			for (int i = m + 1; i <= N; i++) {
//				long ts1=System.currentTimeMillis();
//				int m1= 1+ generator.nextInt(2*m);
//				if(m1 >= i-1) m1=i-2;
//				if(m1 > 2*m) m1=2*m;
//
//				cntE += m1;
//				if(cntE > nE) {m1-=(cntE-nE);
//				      cntE=nE;
//				}
////				int[] tmp = weightedSampleWithoutReplacement((i - 1), m1, inDeg);// #new links
//
//				ts10k += (System.currentTimeMillis()-ts1);
//
//				 //in- link
//				int vin_ix = generator.nextInt(m1) ;
//				if (vin_ix == m1)
//					vin_ix--;
//
////				System.out.println("i="+i+" m1="+m1+" cntE="+cntE+" vin_ix="+vin_ix+" nE="+nE);
//
//				/*
//				 * 8/2/2019: choose randomly the number of in-links [1,m],
//				 * 	 1. select randomly one link to be in-link then
//				 *   2. toss a biased coin ((m/2)-1 inlink,(m/2) outlink)
//				 */
//				//if(m1==i-1)
//
//				biasedCoin=((m1/2.0-1)/(m1-1));
//				for (int k = 0; k < m1; k++) {
//					boolean isRep=true;
//					int distNode=0;
//					while(isRep) {
//						int rand =  generator.nextInt(i+indexToEdgeList-1);//Number of nodes (since all in Deg is initialized by 1) + nE
//						// find interval
//						for (int ni = 1; ni < i; ni++) {
//							rand -= inDeg[ni];
////							if(i==20) System.out.println("rand="+rand+" ni="+ni+" inDeg[ni]="+inDeg[ni]);
//							if (rand <= 0) {
//								distNode = ni;
//								break;
//							}
//						}
////						if(i==20) System.out.println("i="+ i +" k="+k+" distNode="+distNode);
//						isRep=!tmp.add(distNode);
//						if(isRep)totalrepeats++;
//					}
//
//					if (k != vin_ix && (generator.nextDouble() > biasedCoin) ) {	//check if it will be in or out
//						inDeg[distNode] = inDeg[distNode] + 1;
//						subj[indexToEdgeList] = i;
//						obj[indexToEdgeList] = distNode;
//					} else {// inverted link
//						inDeg[i] = inDeg[i] + 1;
//						subj[indexToEdgeList] = distNode;
//						obj[indexToEdgeList] = i;
//					}
//					indexToEdgeList++;
//				}
//				if(i==P1) {
//					    m = m + 1;//second part
//				}
//				if (i % 10000 == 0) {
//					long ti1;
//					ti1= System.currentTimeMillis();
//					ts10k=0;
//					ti0= System.currentTimeMillis();
//				}
//				if(indexToEdgeList >= nE) {
//					lastNode=i;
//					break;
//				}
//				tmp.clear();
//			}
//
//			long t_barabasi = System.currentTimeMillis();
//
//			//Grph structure ----------------------------------------------
//			int nEdges = indexToEdgeList;
//			int[] idRange=builder.addNodes(N);//Range
//			for(int i=0; i < nEdges; i++) {
//
//				if(!builder.addEdge(subj[i]-1 + idRange[0], obj[i]-1+idRange[0], 0)) {
//					System.out.println("Failed to add edge : i="+i+" subj="+subj[i]+" idRange[0]=" + idRange[0]+" obj[i]="+ obj[i]);
//				};
//			}
//			int[] entranceNodes= {0};
//			builder.setEntranceNodes(entranceNodes);
//
//			long t_gbuilder = System.currentTimeMillis();
//		}

//--------------------------------------------------------------------------------------------
		@Override
	public void generateGraph(int numberOfNodes, double avgDegree, long seed, GraphBuilder builder) {
        if(numberOfNodes ==1) {//special case

				builder.addNodes(numberOfNodes);
				int[] entranceNodes= {0};
				builder.setEntranceNodes(entranceNodes);
				return;
			}
//		this.getBarabasiRDF(numberOfNodes, avgDegree, seed,builder);//fixed value of degree
		this.getBarabasiRDFum(numberOfNodes, avgDegree, seed,builder);//use uniform distribution for degree
//		this.getBarabasiRDFumChkRep(numberOfNodes, avgDegree, seed,builder);//Slowest
	}

	@Override
	public void generateGraph(double avgDegree, int numberOfEdges, long seed, GraphBuilder builder) {
//		this.getBarabasiRDF((int)Math.ceil(numberOfEdges/avgDegree), avgDegree, seed,builder);
		this.getBarabasiRDFum((int)Math.ceil(2*numberOfEdges/avgDegree), avgDegree, seed,builder);	}
}
