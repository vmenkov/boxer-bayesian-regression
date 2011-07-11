package edu.dimacs.mms.boxer;

import java.util.HashMap;
import java.util.Vector;

/** A compact representation for an array of data points. When
    multiple data points have the same feature vector, the underlying
    vector is only stored once, accompanied by a list of labels, with
    repeat counts. This is useful e.g. in ontology matching problems,
    when the underlying data tables have lots of identical cells.
    
 */
class DataPointArray {
        
    static class LabelArray {
	/** How many times an identical vector is included into
	 * various classes
	 */
	int cnt[];
	int sumCnt = 0;
	LabelArray(int nCla) {
	    cnt = new int[nCla];
	}
	void  add(int pos) {
	    if (pos < 0 || pos > cnt.length) 
		throw new IllegalArgumentException("class pos "+pos+" out of range");
	    cnt[pos]++;
	    sumCnt++;
	}
    };

    Vector<DataPoint> points;
    Vector<LabelArray> las;
    /** number of geometrically distinct points */
    int length() {
	return points.size();
    }

    /** Sum of multiplicities, i.e. the number of labeled vectors in
	the original input arrays
     */
    int sumCnt;

    private HashMap<DataPoint, LabelArray> h = new HashMap<DataPoint,LabelArray>();


    DataPointArray( Vector<DataPoint> xvec, int i1, int i2, Discrimination dis) {

	final int nCla = dis.claCount();

	for(int i=i1; i<i2; i++) {
	    DataPoint x = xvec.elementAt(i);
	    Discrimination.Cla trueC = x.claForDisc(dis);
	    if (trueC==null) continue; // skip unlabeled
	    DataPoint q = x.shallowCopyWithoutLabels();
	    LabelArray la = h.get(q);
	    if (la == null) h.put( q, la = new LabelArray(nCla));	    
	    la.add(  trueC.getPos());
	}

	// how many unique points?
	points = new Vector<DataPoint>();
	points.addAll( h.keySet() );
	int un = points.size();
	las = new  Vector<LabelArray>(un);
	sumCnt = 0;
	for(DataPoint q: points) {
	    LabelArray a = h.get( q);
	    las.add( a );
	    sumCnt += a.sumCnt;
	}
    }

    void addPoint(DataPoint x, Discrimination dis) {
	final int nCla = dis.claCount();
	Discrimination.Cla trueC = x.claForDisc(dis);
	if (trueC==null) return; // unlabeled
	DataPoint q = x.shallowCopyWithoutLabels();
	LabelArray la = h.get(q);
	if (la == null) {
	    h.put( q, la  = new LabelArray(nCla));	    
	    points.add(q);
	    las.add(la);	   
	}
	la.add(  trueC.getPos());
    }

    /** Sum of squares of the Euclidean norms of all vectors, taking
     * into account multiplicity.
     */
    double sumNormSquare() {
	double sum=0;
	for( int i=0; i<points.size(); i++) {
	    sum += points.elementAt(i).normSquare() * las.elementAt(i).sumCnt;
	}
	return sum;
    }

    /** Computes the average log-likelihood for the given learner on this data set,
	<center>
	(1/n) sum_{j=1..n} sum_{k in AllClasses} y_{jk} log( C_k| x_j)
	</center>

	@param The learner with whose PLRM matrix we compute the
	probabilities P(C_k|x_j) that go into the log-likelihood computation.

       @param zz Output parameter. If it is not null, it must be
       pre-allocated (as new double[this.points.length][]) before
       calling the method. Upon return, z[j] will contain the vector
       (Y-P) for points[j] (based on applyModelLog). If zz is null on
       input, it will be ignored.
       @return The avg log-likelihood for all labeled examples from
       this.points[], or 0 if points[] is empty.
    */
    double logLikelihood(PLRMLearner.PLRMLearnerBlock block,
			 double zz[][]) {
	if (zz!=null && zz.length != points.size()) throw new IllegalArgumentException("The log prob array, if supplied, must be pre-allocated to size "+points.size());

	double logLik = 0;
	for( int i=0; i<points.size(); i++) {
	    DataPoint x = points.elementAt(i);
	    LabelArray la = las.elementAt(i);
	    double [] logProb = block.applyModelLog(x);
	    if (zz!=null) {
		double z[] =  zz[i] = new double[logProb.length];
		for(int k=0; k<logProb.length; k++) {
		    z[k] = la.cnt[k] - la.sumCnt*Math.exp(logProb[k]);
		}
	    }
	    for(int k=0; k<logProb.length; k++) {
		logLik +=  la.cnt[k] * logProb[k];
	    }
	}
	return  (sumCnt==0) ? 0 :  logLik/sumCnt;
    }
}