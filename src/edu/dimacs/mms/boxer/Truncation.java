package edu.dimacs.mms.boxer;

import java.util.Arrays;
import java.util.Vector;

/** An auxiliary class for TruncatedGradient or for any other learners
 that apply truncation to some coefficient matrices. A Truncation
 object is created and associated with one or several matrices whose
 elements need to be periodically truncated.

 <p>A BOXER API user typically won't need to create, or even access
 directly, Truncation objects; they are created as needed by the
 constructors of {@link Learner} classes.
*/
public class Truncation /*implements Cloneable*/ {

    /** The discrimination this Truncation object is for. This would
      be null if this is a "common" truncation object; a non-null if 
      this is a truncaiton object for a particular learning block. The point
     */
    private Discrimination dis=null;

    /** What does the truncation procedure to the physical stored array elements
     */
    enum PHYSICAL_DROP { 
	/** Zeros just stay there. This was the way until 2009-12-18 */
	DROP_NONE,
	    /** Drop an entire row when it become all zeros. This applies to bothe BetaMatrix and DenseMatrix */
	DROP_ZERO_ROWS,
	    /** Drop an entire row when it become all zeros (this
	     * applies to bothe BetaMatrix and DenseMatrix), and drop
	     * every zero element too (in BetaMatrix only) */
	    DROP_ZERO_ROWS_AND_ELEMENTS;
    }

    /** What does the truncation procedure to the physical stored array elements
     */
    static final PHYSICAL_DROP  dropMode = PHYSICAL_DROP.DROP_ZERO_ROWS;

    /** An algorithm parameter, indicating the threshold, values
	within whose range are subject to truncation. The value theta=0
	means "no truncation", and theta=Double.POSITIVE_INFINITY means,
	"truncate all values".
     */
    double theta=0;
   /** An algorithm parameter, basicTo corresponds to eta*g*K in
      TruncatedGradient writeup  */
    double basicTo;

    /** How often truncation is exercised? After every K training
     * vectors. The TG default is 10 (truncate after each 10 vectors),
     * but until Ver 0.4 (Mar 2009) we had 1 (after each vector) */
    int K=10;

    /** Keeps track of how many training vectors have been processed
     (to know when to request truncation the next time) */
    int t=0;

    /** If the Priors object has been set (in the constructor), we are
	using individual priors instead of the usual truncation toward
	zero */	
    private final Priors priors;
   
    /** Reduce the coefficient value by this much at each truncation  */
    public double getBasicTo() {return basicTo; }

    /** Implementation mode */
    final boolean lazy;

    /** The matrix (BetaMatrix or DenseMatrix), or matrices, whose
      elements we will truncate. 
    */
    Matrix matrices[] = null;
     
    /** The count of not-yet-applied truncation operations for each
	row of the matrix (i.e., for each feature).
     */
    private int truncToApply[];

    /** No-truncation constructor */
    Truncation( boolean  _lazy) {
	this(0.0, 0.0, 1, new Matrix[]{}, _lazy, null, null);
    }

    /** Creates a truncation object for truncation of elements of a single 
	marix */
    public Truncation(double  _theta, double to, int _K, Matrix w, boolean  _lazy, Priors _priors, Discrimination _dis) {
	this( _theta, to, _K, new Matrix[] {w}, _lazy, _priors, _dis);
    }

    Truncation(Truncation orig, Matrix[] _matrices, Discrimination _dis) {	
	this( orig.theta, orig.basicTo, orig.K,  _matrices, orig.lazy,
	      orig.priors, _dis);
    }

    /** Creates a truncation object for truncation of elements of one
      or several specified matrices

       @param _matrices An array of matrices to which this truncation applies
       @param _lazy If true, use lazy truncation
       @param _priors The set of individual priors. Usually this is null, meaning that no indiviudual priors will be used.
     */
    public Truncation(double _theta, double to, int _K, Matrix[] _matrices, boolean _lazy, Priors _priors, Discrimination _dis) {	

	theta= _theta;
	basicTo = to;
	K = _K;

	lazy = _lazy;
	priors = _priors;
	dis = _dis;

	//System.out.println("Truncation(), new basicTo=" + basicTo);


	matrices = _matrices;
	for(Matrix w: matrices) {
	    if (!(w instanceof BetaMatrix || w instanceof DenseMatrix )) {
		throw new IllegalArgumentException("Unknown matrix type: " + w.getClass());
	    }
	}

	if (lazy) truncToApply= new int[0]; // dic.getDimension()];
    }

    /** Creates a copy of this Truncation, which copies not only
	params but also the current state. This can be used when
	creating a copy of a living Learner (e.g., in
	AssumeDefaultHistory mode)
     */
    Truncation liveCopy( Matrix[] _matrices, Discrimination _dis) {	
	applyTruncationToAllRows(); // making sure nothing remains not-yet-applied
	Truncation trunc = new Truncation(this, _matrices, _dis);
	//System.out.println("Truncation.liveCopy, this.basicTo=" + basicTo);
	trunc.setT( t );
	return trunc;
    }

    /** Sets t (the number of previously processed vectors). Normally used in
	deserialization only.
     */
    void setT(int _t) {
	t = _t;
    }
    
    /** Just prints the value of theta, which may be 0, a positive
      number, or "Infinity"
     */
    String reportTheta() { 
	return ""+theta;
	//mode==MODE.ALWAYS ? Param.INF :    new Double(mode==MODE.NONE? 0:theta);
    }

    /** If diff (the distance from 0, or, in general, from the mode value)
	within the threshold theta value? */
    boolean withinTheta(double diff) {
	return Double.isInfinite(theta) ||   Math.abs(diff)<=theta;
    }

    /** Applies "truncation", by a specified amount, to a particular
	single value */
    private double truncateValue(double value, double to) {
	if (withinTheta(value)) {
	    if (value > to) value -= to;
	    else  if (value < -to) value += to;
	    else value = 0;
	}
	return value;
    }
   

    /** Truncates all matrix elements right now.  
     */
    void truncateNow() {
	if (theta ==0) return;
	for( Matrix _w : matrices) {
	    if (_w instanceof BetaMatrix) {
		for(int j=0; j< _w.getNRows(); j++) {
		    truncateRow(_w, j, 1);
		}
	    }
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

	//System.out.println("Requesting truncation, t=" + t);
	if (theta == 0) return;
	t++;
	//System.out.println("t:=" + t +", basicTo=" + basicTo);
	if (t%K != 0) return;

	if (lazy) {
	    if (truncToApply.length < d) {
		truncToApply= Arrays.copyOf( truncToApply, d);
	    }
	    for(int i=0; i<truncToApply.length; i++) {
		truncToApply[i] ++;
	    }
	    
	    //System.out.print("truncToApply=(");
	    //for(int x :truncToApply) System.out.print(" "+x);
	    //System.out.println(")");

	}  else  truncateNow();
    }

    /** Apply truncation immediately to every element of the j-th row of a
	specified matrix 

	@param j The row index

	@param mult The "multiplier", indicating how many times the
	truncation needs to be done now. It may be greater than one if
	we are using lazy truncation, and there are several deferred
	truncations that are finally going to be done now.
     */
    private void truncateRow(Matrix _w, int j, int mult) {
	double to = basicTo * mult;
	int countNZ = 0;

	if (priors!=null && dis==null) throw new AssertionError("We ought not have created a Truncation instance with priors but without a discrimination link!");

	if (_w instanceof BetaMatrix) {
	
	    BetaMatrix w = (BetaMatrix)_w;
	    Vector<BetaMatrix.Coef> v  = w.getRow(j);

	    if (v==null) return;
	    for( BetaMatrix.Coef c: v) {
		if (priors != null) {
		    Prior p = priors.get( dis.getClaById(c.icla), j);
		    c.value = p.apply(c.value, this, mult);
		} else {
		    c.value=truncateValue(c.value, to);
		}
		countNZ += ((c.value == 0) ? 0 : 1);
	    }
	    if (0 < countNZ  && countNZ < v.size() && 
		dropMode == PHYSICAL_DROP.DROP_ZERO_ROWS_AND_ELEMENTS) {
		w.compressRow(j);		    
	    }
	} else if (_w instanceof DenseMatrix) {
	    DenseMatrix m = (DenseMatrix)_w;
	    double[] v= m.data[j];
	    if (v==null) return;
	    for(int i=0;i<v.length; i++) {
		if (priors != null) {
		    Prior p = priors.get( dis.getClaById(i), j);
		    v[i] = p.apply(v[i], this, mult);
		} else {
		    v[i]=truncateValue(v[i], to);
		}
		countNZ += ((v[i] == 0) ? 0 : 1);
	    }
	} else throw new AssertionError("Unknown matrix class");

	if (countNZ == 0 && dropMode != PHYSICAL_DROP.DROP_NONE) {
	    // Drop the entire row if it becomes all-zeros
	    _w.dropRow(j);
	    //Logging.info("Dropped all-zero row " +  j );
	}
    }

    /** Looks at the not-applied-yet truncation amount for the j-th
	row of the matrix(es), and if it's non-zero, applies it
	now. If one uses lazy truncation, it is necessary to call this
	method on the j-th row of the matrix before it is used (e.g.,
	to score a vector with a non-zero j-th component)
     */
    void applyTruncation(int j) {
	//System.out.println("call apply Truncation(row "+j+")" + t);
	if (theta == 0) return;
	if (!lazy) return;
	if ( truncToApply == null ||  j>=truncToApply.length ) return;
	int mult = truncToApply[j];
	if ( mult == 0) return;
	truncToApply[j] = 0;
	for( Matrix _w : matrices) {
	    truncateRow(_w, j, mult);
	}
    }

    /** Applies not-yet-applied truncation amounts to all rows */
    void applyTruncationToAllRows() {
	if (theta == 0) return;
	if (!lazy) return;
	for(int j=0; j<	truncToApply.length; j++)  applyTruncation(j);
    }


    String describe() {
	return theta==0 ? 
	    "No truncation" :
	    "Truncation every "+K+" steps (done "+t+" steps so far) by "+
	    basicTo+" with theta="+ reportTheta() + "\n" +
	    (priors == null? "No priors" : "Priors:\n" + priors.reportSize());
	
    }
}

/*
Copyright 2009, Rutgers University, New Brunswick, NJ.

All Rights Reserved

Permission to use, copy, and modify this software and its documentation for any purpose 
other than its incorporation into a commercial product is hereby granted without fee, 
provided that the above copyright notice appears in all copies and that both that 
copyright notice and this permission notice appear in supporting documentation, and that 
the names of Rutgers University, DIMACS, and the authors not be used in advertising or 
publicity pertaining to distribution of the software without specific, written prior 
permission.

RUTGERS UNIVERSITY, DIMACS, AND THE AUTHORS DISCLAIM ALL WARRANTIES WITH REGARD TO 
THIS SOFTWARE, INCLUDING ALL IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR 
ANY PARTICULAR PURPOSE. IN NO EVENT SHALL RUTGERS UNIVERSITY, DIMACS, OR THE AUTHORS 
BE LIABLE FOR ANY SPECIAL, INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER 
RESULTING FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT, 
NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION WITH THE USE OR 
PERFORMANCE OF THIS SOFTWARE.
*/