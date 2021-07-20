package org.dice_research.ldcbench.generate;

import java.util.Arrays;
import java.util.Random;
import java.util.stream.IntStream;
import java.util.LinkedList;
import java.util.Queue;

import org.dice_research.ldcbench.graph.GraphBuilder;

public class RandomCloudGraph implements GraphGenerator {
//	protected Random generator;

    public String name;
    protected int[] nodetypes;
    protected boolean[] ishub;
    protected int[][] typeconnectivity;
    protected int hcount;
    protected int[] typecounts;

    public RandomCloudGraph(String gname, int[] nt, boolean[] ih, int[][] tc) {
        name = gname;
        nodetypes = nt;
        ishub = ih;
        typeconnectivity = tc;
    }

    public RandomCloudGraph(String gname, int[] typecounts, int hcount, int[][] tc) {
        name = gname;
        this.typecounts = typecounts;
        nodetypes = getNodeSequence(typecounts);
        // ishub=ih;
        typeconnectivity = tc;
        this.hcount = hcount;
    }

    public boolean[] getHubs() {
        return (ishub);
    }

    public int[] getNodeTypes() {
        return (nodetypes);
    }
    // -------------------------------------------------------------------------------------------

    @Override
    public void generateGraph(int numberOfNodes, double avgDegree, long seed, GraphBuilder builder) {
        /*
         * rules of which node types are allowed to link to which (maybe based on
         * weights), e.g., {{1,1,0},{0,1,1},{1,1,1}} which means that the following
         * links are allowed: 0 --> 0, 0 --> 1, 1 --> 1, 1 --> 2, 2 --> 0, 2 --> 1, 2
         * --> 2 while 0 --> 2 and 1 --> 0 are not allowed
         */

//        getRandomLOD(numberOfNodes, avgDegree, seed, 0.7, builder);//not justified
        getRandomLOD(numberOfNodes, avgDegree, seed, 0.5, builder);
    }

    @Override
    public void generateGraph(double avgDegree, int numberOfEdges, long seed, GraphBuilder builder) {
        this.generateGraph((int) Math.ceil(numberOfEdges / avgDegree / 2), avgDegree, seed, builder);
    }

//--------------------------------------------------------------------------------------------

    protected void getRandomLOD(int N, double degree, long seed, double outlinkspct, GraphBuilder builder) {
        /* nodes are numbered from 1 to N */
        /* TreeSet is faster practically than HashSet */

        if (degree < (2 - 2 / N)) {
            throw new IllegalArgumentException("Degree must be more than (2-2/N).");
        }

        if (degree > (N - 1)) {// max links created at any step is N-1
            throw new IllegalArgumentException("Degree can NOT be more than (N-1).");
        }

        int indexToEdgeList;// index to edge list
//        long t0 = System.currentTimeMillis();
//        long ti0 = System.currentTimeMillis();

        int nE = (int) Math.ceil(N * degree / 2);
        int[] subj = new int[nE];
        int[] obj = new int[nE];
        int[] inDeg = new int[N + 1];
        int[] typewt = new int[N + 1];
        Arrays.fill(inDeg, 1);
        inDeg[0] = 0;// reserved
        int m = (int) Math.floor(degree / 2);// average degree of graph

        Random generator = new Random(seed);
        /*
         * -init:until the number of nodes to connect to for each type is at least d
         * (check if <=d =>conn2all), -source node should be of type able to connect to
         * all others -in-link should be from type able to connect to current node:if
         * all links are not eligible -- better make in two steps: select out links from
         * possible destinations and select one coming from possible sources
         */
        /*
         * start by one node of each type with all possible connections (root able to
         * connect to all) --- or mst (min sp tree)
         */
        // indexToEdgeList = getInitGraph(N,degree,seed,subj, obj,inDeg,m,generator);
        int il = 0;
        /*
         * for(int i=0; i < typeconnectivity.length;i++) {// start by all possible
         * connections for(int j = 0; j < typeconnectivity.length; j++) { if(i==j)
         * continue; if(typeconnectivity[i][j]==1) { subj[il] = i+1; obj[il] = j+1;
         * il++; } } }
         */

//	MR (12.10.2019): Disabled connectivity check since we have a search for seed nodes which will handle unconnected parts of the graph
//	// only one out connection: initialize graph
	for(int i=0; i < typeconnectivity.length;i++) {
		for(int j = 0; j < typeconnectivity.length; j++) {
			if(i==j) continue;
			if(typeconnectivity[nodetypes[i]][nodetypes[j]]==1) {
			//	System.out.println(i+" "+j+" "+il+" "+nE);
				subj[il] = i+1; obj[il] = j+1;
				il++;
				break;
			}
		}
		if(il==nE) break;
	}

//	if(il < typeconnectivity.length && nE>il) {
//		throw new IllegalArgumentException("type connectivity matrix represent unconnected graph "
//				+ "               (each row must contain at least one nonzero other than diagonal).");
//	}

        // type of node
        int ctype;
        int ndests, nsrcs;

        // --------------------------------------
        indexToEdgeList = il;

        int P1 = (m + 1) * (N - m) - (nE - indexToEdgeList) + m;
        if (P1 <= m)
            P1 = m + 1;
        //
        try {
//		for(int node=typeconnectivity.length+1;node<=N;node++) {//next node
            int node = typeconnectivity.length + 1;
            while (node <= N) {// next node

                if (node == P1) {
                    m = m + 1;// second part
                }

                int m1, mi, mo;// number of inlinks and number of out links mi+mo=m1
                if(m > 0) {
                    m1 = 1 + generator.nextInt(2 * m - 1);// to use uniform distribution from 1 to 2m
                } else {
                    // Special case in which the new node is only allowed to have one single incoming edge
                    m1 = 0;
                }
                if (m1 >= node)
                    m1 = node - 1;
                if (node == N) {// last node
                    m1 = nE - indexToEdgeList;
                }
                // if(m1 > 2*m) m1=2*m;

                if ((N - node) >= (nE - indexToEdgeList - m1 + 1))
                    m1 = 1;// to have the same number of nodes

                if (m1 == 1) {
                    if (generator.nextDouble() > outlinkspct) {
                        mo = 0;
                    } else {
                        mo = 1;
                    }
                } else {
                    mo = (int) Math.round(m1 * outlinkspct);
                }
                mi = m1 - mo;

                ctype = nodetypes[node - 1];
                // get possible out connections
                if (mo > 0) {
                    ndests = 0;
                    for (int i = 1; i < node; i++) {// only use weights of nodes that accepts connections from current
                                                    // node.
                        if (typeconnectivity[ctype][nodetypes[i - 1]] == 1) {
                            typewt[i] = inDeg[i];
                            ndests++;
                        } else
                            typewt[i] = 0;
                    }
                    int[] outlinks;
                    if (ndests > mo) {// sample out-links
                        outlinks = weightedSampleWithoutReplacement(node - 1, mo, typewt, generator);

                        for (int k = 0; k < mo; k++) {
                            inDeg[outlinks[k]] = inDeg[outlinks[k]] + 1;
                            subj[indexToEdgeList] = node;
                            obj[indexToEdgeList] = outlinks[k];
                            indexToEdgeList++;
                            if (indexToEdgeList >= nE)
                                break;
                        }
                    } else {// connect to all if count less than m
                        for (int i = 1; i < node; i++) {// only use weights of nodes that accepts connections from
                                                        // current node.
                            if (typewt[i] > 0) {
                                inDeg[i] = inDeg[i] + 1;
                                subj[indexToEdgeList] = node;
                                obj[indexToEdgeList] = i;
                                indexToEdgeList++;
                                if (indexToEdgeList >= nE)
                                    break;
                            }
                        }
                    }
                }
                // ---- in-link: there can be in and out links between two nodes[DBpedia&Revyu]
                if (indexToEdgeList >= nE)
                    break;
                if (mi > 0) {
                    nsrcs = 0;
                    for (int i = 1; i < node; i++) {// only use weights of nodes that accepts connections from current
                                                    // node.
                        if (typeconnectivity[nodetypes[i - 1]][ctype] == 1) {
                            typewt[i] = inDeg[i];
                            nsrcs++;
                        } else
                            typewt[i] = 0;
                    }
                    int[] inlinks;
                    if (nsrcs > mi) {// sample in-links
                        inlinks = weightedSampleWithoutReplacement(node - 1, mi, typewt, generator);
                        inDeg[node] = inDeg[node] + mi;
                        for (int k = 0; k < mi; k++) {
                            subj[indexToEdgeList] = inlinks[k];
                            obj[indexToEdgeList] = node;
                            indexToEdgeList++;
                            if (indexToEdgeList >= nE)
                                break;
                        }
                    } else {// connect to all if count less than m
                        inDeg[node] = inDeg[node] + nsrcs;
                        for (int i = 1; i < node; i++) {// only use weights of nodes that accepts connections from
                                                        // current node.
                            if (typewt[i] > 0) {
                                // inDeg[node] = inDeg[node] + 1;
                                subj[indexToEdgeList] = i;
                                obj[indexToEdgeList] = node;
                                indexToEdgeList++;
                                if (indexToEdgeList >= nE)
                                    break;
                            }
                        }
                    }
                }

                // The dataset currently contains 1,239 datasets with 16,147 links (as of March
                // 2019): d~=15
//                if (node % 10000 == 0) {
//                    long ti1;
//                    ti1 = System.currentTimeMillis();
//                    ti0 = System.currentTimeMillis();
//                }
                if (indexToEdgeList >= nE)
                    break;
                node++;
            }
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException(
                    "N=" + N + ", seed=" + seed + ", degree=" + degree + " msg from sampling:" + ex.getMessage(), ex);
        }
        // 5/7/2019 remapping ids to make nodes of same type having adjacent ids.
        // type is 0 to n0-1, type1 n to n+n1-1 .. etc

        int nTypes = typeconnectivity.length;
        int[] crnt_indx = new int[nTypes];
        crnt_indx[0] = 0;
        for (int i = 1; i < nTypes; i++) {
            crnt_indx[i] = crnt_indx[i - 1] + typecounts[i - 1];
        }
        int[] remappedIds = new int[N];
        for (int i = 0; i < N; i++) {
            remappedIds[i] = crnt_indx[nodetypes[i]];
            crnt_indx[nodetypes[i]]++;
        }

        // Grph structure ----------------------------------------------
        int nEdges = indexToEdgeList;
        int[] idRange = builder.addNodes(N);// Range
        for (int i = 0; i < nEdges; i++) {
            if (!builder.addEdge(remappedIds[subj[i] - 1] + idRange[0], remappedIds[obj[i] - 1] + idRange[0], 0)) {
                // if(!builder.addEdge(subj[i]-1 + idRange[0], obj[i]-1+idRange[0], 0)) {
                System.out.println("Failed to add edge : i=" + i + " subj=" + subj[i] + " idRange[0]=" + idRange[0]
                        + " obj[i]=" + obj[i] + "remappedIds[obj[i]-1]" + remappedIds[subj[i] - 1]);
            }
            ;
        }
// adjust node types
        int nodetype_index = 0;
        for (int i = 0; i < nTypes; i++) {
            for (int j = 0; j < typecounts[i]; j++) {
                this.nodetypes[nodetype_index++] = i;
            }
        }

        int[] entranceNodes;
        entranceNodes = findEnteranceNodes(builder);
        if (entranceNodes.length == 0) {
            System.out.println("There is no entranceNodes, Graph is not connected.");
        } // else System.out.println("#EnterenceNodes:"+entranceNodes.length+"
          // First:"+entranceNodes[0]);
        builder.setEntranceNodes(entranceNodes);
    }
//----------------------------------------------------------

    public int[] findEnteranceNodes(GraphBuilder g) {
        int N = g.getNumberOfNodes();
        boolean[] visited = new boolean[N], isEnterance = new boolean[N];
        Arrays.fill(visited, false);
        Arrays.fill(isEnterance, false);
        int cntENodes = 0;

        Queue<Integer> processQ = new LinkedList<>();
        for (int i = 0; i < N; i++) {
            int[] sources = g.incomingEdgeSources(i);
            if (sources.length == 0) {
                processQ.add(i);
                visited[i] = isEnterance[i] = true;
                cntENodes++;
            }
        }

        boolean finished = false;
        int node;
        if (cntENodes == 0) {
            node = 0;// to be processed
            processQ.add(0);
            visited[0] = isEnterance[0] = true;
            cntENodes++;
        }

        while (!finished) {
            node = processQ.poll();
            int[] targets = g.outgoingEdgeTargets(node);
            // processed[node]=true;
            for (int dist : targets) {
                if (!visited[dist]) {
                    visited[dist] = true;
                    processQ.add(dist);
                }
            }

            if (processQ.isEmpty()) {
                finished = true;
                for (int j = 0; j < N; j++) {
                    if (!visited[j]) {
                        isEnterance[j] = true;
                        visited[j] = true;
                        finished = false;
                        cntENodes++;

                        processQ.add(j);
                        break;
                    }
                }
            }
        }

        int[] enteranceNodes = new int[cntENodes];
        int j = 0;
        for (int i = 0; i < N; i++) {
            if (isEnterance[i]) {
                // System.out.println(i);
                enteranceNodes[j++] = i;
            }
        }
        return (enteranceNodes);
    }

    public int[] getNodeSequence(int[] typeCnts) {
        int totalNodes = IntStream.of(typeCnts).sum();
        int nTypes = typeCnts.length;
        int[] nodeTypes = new int[totalNodes + 1];// start from 0
        int n = nTypes;
        int[] crntCnts = new int[nTypes];
        for (int i = 0; i < nTypes; i++) {// init
            crntCnts[i] = 1;
            nodeTypes[i] = i;
        }
        
         while (n < totalNodes) {
            for (int y = 0; y < nTypes; y++) {
                if (((double) crntCnts[y] / n) <= ((double) typeCnts[y] / totalNodes)) {
                    n++;
                    nodeTypes[n] = y;
                    crntCnts[y]++;
                }
                if (n >= totalNodes)
                    break;
            }
        }
//         for(int x:crntCnts) System.out.println(x);
        int[] nodeTypesout;
        nodeTypesout = Arrays.copyOf(nodeTypes, totalNodes);
        return (nodeTypesout);
    }
//----------------------------------------------------------------

    protected int[] weightedSampleWithoutReplacement(int n, int m, int[] wt, Random generator) {
        /* without accumulated weights */

        int[] Res = new int[m];

        int rand;
        int i;
        int Sum = 0;
        int cntNNz = 0;// count of non zero values in wt, should be at least m :23.9.19

        for (i = 1; i <= n; i++) {
            Sum += wt[i];
            if (wt[i] > 0)
                cntNNz++;
        }

        if (Sum <= 0) {
            throw new IllegalArgumentException("Error, Sum<=0," + n + " m=" + m + " wt0=" + wt[0] + " Sum=" + Sum);
        }

        if (cntNNz < m) {
            throw new IllegalArgumentException("Error, count nonzeros in weights: " + cntNNz + " < " + m + ", n=" + n
                    + " wt0=" + wt[0] + " Sum=" + Sum);
        }

        int[] wt1 = Arrays.copyOf(wt, n + 1);

        for (int j = 0; j < m; j++) {
            rand = generator.nextInt(Sum);

            // find interval
            for (i = 1; i <= n; i++) {
                rand -= wt1[i];
                if (rand < 0) {// to avoid getting nodes set to zero degree, must be less than
                    Res[j] = i;
                    Sum -= wt1[i];
                    wt1[i] = 0;// to avoid replacement

                    break;
                }
            }

        }

        return (Res);
    }

}