package boxer;

import java.util.*;
import java.text.*;
import java.io.*;

/** An auxiliary class for TruncatedGradient or for any other learners
 that apply truncation to some coefficient matrices. A Truncation
 object is created and associated with one or several matrices whose
 elements need to be periodically truncated. */
class Truncation /*implements Cloneable*/ {

    public enum MODE { 
	/* No truncation will be done, all calls will be ignored */
	NONE, 
	    /* Truncation only of matrix elements with abs value less or equal than theta */
	    BY_THETA, 
	    /* Truncation of all matrix elements */
	    ALWAYS; 
    }

    MODE mode;

    /** Algorithm parameters. basicTo corresponds to eta*g*K in
      TruncatedGradient writeup  */
    double theta=0, basicTo;

    /** How often truncation is exercised? After every K training
     * vectors. The TG default is 10 (truncate after each 10 vectors),
     * but until Ver 0.4 (Mar 2009) we had 1 (after each vector) */
    int K=10;

    /** Keeps track of how many training vectors have been processed
     (to know when to request truncation the next time) */
    int t=0;

    /** Reduce the coefficient value by this much at each truncation  */
    public double getBasicTo() {return basicTo; }

    /** Implementation mode */
    static boolean lazy=true;

    /** The matrix (BetaMatrix or DenseMatrix), or matrices, whose
      elements we will truncate. 
    */
    Matrix matrices[] = null;
     
    double truncToApply[];

    /** No-truncation constructor */
    Truncation() {
	this((Object)(new Double(0)), 0.0, 1, new Matrix[]{});
    }

    /** Creates a truncation object for truncation of elements of a single 
	marix */
    Truncation(Object _theta, double to, int _K, Matrix w) {
	this( _theta, to, _K, new Matrix[] {w});
    }

    Truncation(Truncation orig, Matrix[] _matrices) {	
	this( orig.reportTheta(), orig.basicTo, orig.K,  _matrices);
    }

    Truncation(Object _theta, double to, int _K, Matrix[] _matrices) {	
	if (_theta.equals(Param.INF)) {
	    mode = MODE.ALWAYS;
	    theta = -1;
	} else {
	    theta = ((Double)_theta).doubleValue();
	    mode = 	(theta==0)? MODE.NONE: MODE.BY_THETA;
	}

	basicTo = to;
	K = _K;
	matrices = _matrices;
	for(Matrix w: matrices) {
	    if (!(w instanceof BetaMatrix || w instanceof DenseMatrix )) {
		throw new IllegalArgumentException("Unknown matrix type: " + w.getClass());
	    }
	}

	if (lazy) truncToApply= new double[0]; // dic.getDimension()];
    }

    /** Creates a copy of this Truncation, which copies not only
	params but also the current state. This can be used when
	creating a copy of a living Learner (e.g., in
	AssumeDefaultHistory mode)
     */
    Truncation liveCopy( Matrix[] _matrices) {	
	applyTruncationToAllRows(); // making sure nothing remains not-yet-applied
	Truncation trunc = new Truncation(this, _matrices);
	trunc.setT( t );
	return trunc;
    }

    /** Sets t (the number of previously processed vectors). Normally used in
	deserialization only.
     */
    void setT(int _t) {
	t = _t;
    }
    
    /** Reports the (mode, theta) pair as a single number. 
	@return Param.INF instead of infinity, or a Double otherwise */
    Object reportTheta() { 
	return mode==MODE.ALWAYS ? Param.INF : 
	    new Double(mode==MODE.NONE? 0:theta);
    }

    /** Applies "truncation", by a specified amount, to a particular
	single value */
    double truncateValue(double value, double to) {
	if (mode==MODE.ALWAYS || 
	    mode==MODE.BY_THETA && Math.abs(value)<=theta) {
	    if (value > to) value -= to;
	    else  if (value < -to) value += to;
	    else value = 0;
	}
	return value;
    }

    /** Truncates all elements of W right now
	FIXME: may want to delete elements that have become 0s.
     */
    void truncateNow() {
	if (mode ==MODE.NONE) return;
	for( Matrix _w : matrices) {
	    if (_w instanceof BetaMatrix) {
		BetaMatrix w = (BetaMatrix)_w;
		for( Vector<BetaMatrix.Coef> v: w.rows()) {
		    if (v == null) continue;
		    for( BetaMatrix.Coef c: v) c.value=truncateValue(c.value, basicTo);
		}	
	    } else if (_w instanceof DenseMatrix) {
		DenseMatrix m = (DenseMatrix)_w;
		for(double[] v: m.data) {
		    if (v == null) continue;
		    for(int i=0;i<v.length; i++) v[i]=truncateValue(v[i], basicTo);
		}
	    } else throw new AssertionError("Unknown matrix class");
	}
    }


    /** This call is made by the learner before each training vector;
	only each K-th call if effective. The truncation is only done
	</em>before</em> each K-th training vector (them numbered with
	base-1), i.e. if K=10, it's done after the 9th, 19th
	etc. vector.
	@param d  feature count
    */
    void requestTruncation(int d) {
	if (mode == MODE.NONE) return;
	t++;
	if (t%K != 0) return;

	if (lazy) {
	    if (truncToApply.length < d) {
		truncToApply= Arrays.copyOf( truncToApply, d);
	    }
	    for(int i=0; i<truncToApply.length; i++) {
		truncToApply[i] += basicTo;
	    }
	}  else  truncateNow();
    }

    /** Looks at the not-applied-yet truncation amount for the j-th row of
	the matrix, and if it's non-zero, applies it now
     */
    void applyTruncation(int j) {
	if (mode == MODE.NONE) return;
	if (!lazy) return;
	if ( truncToApply == null ||  j>=truncToApply.length ) return;
	double to =  truncToApply[j];
	if (to==0) return;
	truncToApply[j] = 0;
	for( Matrix _w : matrices) {
	    if (_w instanceof BetaMatrix) {
		BetaMatrix w = (BetaMatrix)_w;
		Vector<BetaMatrix.Coef> v= w.getRow(j);
		if (v==null) return;
		for( BetaMatrix.Coef c: v)  c.value=truncateValue(c.value, to);
	    } else if (_w instanceof DenseMatrix) {
		DenseMatrix m = (DenseMatrix)_w;
		double[] v= m.data[j];
		if (v==null) return;
		for(int i=0;i<v.length; i++) v[i]=truncateValue(v[i], to);
	    }
	}
    }

    /** Applies not-yet-applied truncation amounts to all rows */
    void applyTruncationToAllRows() {
	if (mode == MODE.NONE) return;
	if (!lazy) return;
	for(int j=0; j<	truncToApply.length; j++)  applyTruncation(j);
    }


    String describe() {
	return mode==MODE.NONE ? 
	    "No truncation" :
	    "Truncation every "+K+" steps (done "+t+" steps so far) by "+
	    basicTo+" with theta="+ reportTheta();
    }
}