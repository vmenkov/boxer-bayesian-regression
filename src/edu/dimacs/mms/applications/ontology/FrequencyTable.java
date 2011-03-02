package edu.dimacs.mms.applications.ontology;

import java.util.*;
//import java.io.*;
//import java.text.*;

import edu.dimacs.mms.boxer.*;
//import edu.dimacs.mms.borj.*;

/** Used as part of Jensen–Shannon divergence computation.
 */
class FrequencyTable {

    static class Pair //implements Comparable<Pair>
    {
	int feature;
	double value;
	Pair(int f, double v) { feature = f; value=v; }
	/** Used for sorting by feature id*/
	/*
	public int compareTo(Pair x)  {
	    return x.feature - feature;
	}
	*/
    }
      
    /** Data for one column (= field) of the database */
    static class FrequencyColumn extends Vector<Pair> {
	FrequencyColumn(int size) { super(size); }
    }

    /** Data for one column, in the process of being assembled */
    private static class FrequencyColumnBuilder {
	/** Feature ID to (raw) frequency. Sorted by feature id.
	 */
	private TreeMap<Integer, Integer> h = new TreeMap<Integer, Integer>();
	private int sumCnt;
	/** increments the frequency for a given feature id, and the sum */
	void increment(int fid) {
	    Integer key =new Integer(fid);
	    Integer old = h.get(key);
	    int x = (old==null) ? 1 : old.intValue()+1;
	    h.put(key, new Integer(x));
	    sumCnt++;
	}
	void add(DataPoint p,  Mode mode) {

	}

	FrequencyColumn toFrequencyColumn() {
	    FrequencyColumn w= new FrequencyColumn(h.size());
	    for(Map.Entry<Integer,Integer> e: h.entrySet()) {
		w.add(new Pair( e.getKey(), ((double)e.getValue())/sumCnt));
	    }
	    return w;
	}
    

    }

    FrequencyColumn[] columns;

    public static enum Mode { 
	/** Overall term frequency in the entire text of the column (i.e.,
	 all cells in the column merged together) */
	TF, 
	    /** Number of cells in the column that contain the term */
	    PREVALENCE;
    }


    FrequencyTable(DataSourceParser ds, Mode mode) {

	int M1 = ds.dis.claCount();
	columns = new FrequencyColumn[M1];
	FrequencyColumnBuilder builder[] = new FrequencyColumnBuilder[M1];
	for(int i=0; i<M1; i++) builder[i] = new FrequencyColumnBuilder();

	for(DataPoint p: ds.data) {
	    int cid = p.getClasses(ds.suite).elementAt(0).getPos();
	    builder[cid].add(p, mode);
	}
    }

}