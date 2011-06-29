package edu.dimacs.mms.applications.ontology;

import java.util.*;

import edu.dimacs.mms.boxer.*;

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
	FrequencyColumn(int cap) { super(cap); }
	/** 2-norm */
	double norm;
	/** must be called once the vector has been filled */
	void computeNorm() {
	    double s=0;
	    for(Pair p: this) s += p.value * p.value;
	    norm = Math.sqrt(s);
	}
	double dotProduct(FrequencyColumn b) {
	    int i=0, pi=0;
	    double sum=0;
	    while(i < size() && pi< b.size()) {
		Pair ca=elementAt(i), cb=b.elementAt(pi);
		if (ca.feature ==  cb.feature) {
		    sum += ca.value * cb.value;
		    i++;
		    pi++;
		} else if (ca.feature <  cb.feature) {
		    i++;
		} else {
		    pi++;
		}
	    }
	    return sum;
	}
	/** See explanation e.g. at 
http://en.wikipedia.org/wiki/Jensen%E2%80%93Shannon_divergence

          <p>For distribution functions A and B, we compute
	  <div align=center>
          d(A,B)=(KL(A,M)+KL(B,M))/2, where M=(A+B)/2
	  </div>
	  and the underlying Kullback–Leibler divergence KL(P|Q) is
	  <div align=center>
	  KL(P|Q) = sum( P(i) ln ( P(i)/Q(i)))
	  </div>
	  
	  <p>
          The algorithm is as per the discussion with Paul Kantor,
	  2011-02-28 thru 2011-03-01. 

	*/
	double jensenShannonDivergence(FrequencyColumn b) {
	    int i=0, pi=0;
	    double suma=0, sumb=0;
	    while(i < size() && pi< b.size()) {
		Pair ca=elementAt(i), cb=b.elementAt(pi);
		if (ca.feature ==  cb.feature) {
		    double mid =  (ca.value + cb.value)/2;
		    suma += KL( ca.value, mid);
		    sumb += KL( cb.value, mid);
		    i++;
		    pi++;
		} else if (ca.feature <  cb.feature) {
		    suma += KL0(ca.value);
		    i++;
		} else {
		    sumb += KL0(cb.value);
		    pi++;
		}
	    }
	    while(i < size()) suma += KL0( elementAt(i++).value );
	    while(pi < b.size()) sumb += KL0( b.elementAt(pi++).value );

	    return (suma+sumb)/2;	    
	}

	static final double log2 = Math.log(2);

	static private double KL(double p, double q) {
	    return p * Math.log(p/q);
	}
	/** Same as KL(c, c/2) */
	static private double KL0(double c) {
	    return c * log2;
	}

    }

    /** Data for one column, in the process of being assembled */
    private static class FrequencyColumnBuilder {
	/** Feature ID to (raw) frequency. Sorted by feature id.
	 */
	private TreeMap<Integer, Double> h = new TreeMap<Integer, Double>();
	private double sumCnt;
	/** increments the frequency for a given feature id, and the sum */
	private void increment(int fid, double val) {
	    Integer key =new Integer(fid);
	    Double old = h.get(key);
	    double x = (old==null ? 0 : old.intValue()) + val;
	    h.put(key, new Double(x));
	    sumCnt += val;
	}
	void add(DataPoint p,  Mode mode) {
	    int features[]  = p.getFeatures();
	    double values[] = p.getValues();
	    for(int i=0;i<features.length; i++) {
		double val = (mode==Mode.TF ? values[i] : 
			      (values[i]==0 ? 0:1));
		increment( features[i], val);
	    }
	}

	FrequencyColumn toFrequencyColumn() {
	    FrequencyColumn w= new FrequencyColumn(h.size());
	    Integer prev = null;
	    for(Map.Entry<Integer,Double> e: h.entrySet()) {
		Integer key=e.getKey();
		if (prev!=null && prev.compareTo(key)>=0) {
		    throw new AssertionError("Features not in order!");
		}
		prev = key;
		w.add(new Pair( key.intValue(), ((double)e.getValue())/sumCnt));
	    }
	    w.computeNorm();
	    return w;
	}
    

    }

    FrequencyColumn[] columns;
    int claCnt() { return columns.length; }

    public static enum Mode { 
	/** Overall term frequency in the entire text of the column (i.e.,
	 all cells in the column merged together) */
	TF, 
	    /** Number of cells in the column that contain the term */
	    PREVALENCE;
    }


    FrequencyTable(DataSourceParser ds, Mode mode) {
	int M1 = ds.dis.claCount();

	FrequencyColumnBuilder builder[] = new FrequencyColumnBuilder[M1];
	for(int i=0; i<M1; i++) builder[i] = new FrequencyColumnBuilder();

	for(DataPoint p: ds.data) {
	    int cid = p.getClasses(ds.suite).elementAt(0).getPos();
	    builder[cid].add(p, mode);
	}

	columns = new FrequencyColumn[M1];
	for(int i=0; i<M1; i++) columns[i] = builder[i].toFrequencyColumn();
    }

    /** Computes cosine similarities between class-description vectors 
	of this data source and another data source.
	@return [this.claCnt][other.claCnt]
     */
    double [][] cosineSim(FrequencyTable other) {
	int M2 =  claCnt(), M1 = other.claCnt();
	double p[][] = new double[M2][];
	for(int i=0; i<M2; i++) {
	    p[i] = new double[M1];
	    for(int j=0; j<M1; j++) {
		p[i][j] = columns[i].dotProduct( other.columns[j]) /
		    (columns[i].norm * other.columns[j].norm);
	    }
	}
	return p;
    }

    double [][] jensenShannonDivergence(FrequencyTable other) {
	int M2 =  claCnt(), M1 = other.claCnt();
	double p[][] = new double[M2][];
	for(int i=0; i<M2; i++) {
	    p[i] = new double[M1];
	    for(int j=0; j<M1; j++) {
		p[i][j] = columns[i].jensenShannonDivergence(other.columns[j]);
	    }
	}
	return p;
    }


}