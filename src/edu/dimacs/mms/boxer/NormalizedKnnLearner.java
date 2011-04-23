package edu.dimacs.mms.boxer;

import java.io.*;
import java.util.*;

import org.apache.xerces.dom.DocumentImpl;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

/** A naive implementation of kNN. Before measuring distances, vectors are
    normalized. Thus, |a-b|^2 = |a|^2 + |b|^2 - 2(a,b) = 2 (1-(a,b)),
    and ordering vectors in the increasing-distance order is 
    equivalent to ordering them in decreasing-cosine sim order.
 */
public class NormalizedKnnLearner extends Learner {

    //final Double Zero = new Double(0);

    /** Do we even need this?
     */
    public class KnnLearnerBlock extends LearnerBlock {

	KnnLearnerBlock(Discrimination _dis) { dis=_dis;}

	/** Returns true if this learning block contains no
	non-trivial information obtained from learning.
	*/
	boolean isZero() { return points.size()==0; }

	/** Estimates probabilities of a given data point's belonging to
	various classes in this block's discrimination.

	@param p DataPoint to score

	@return double[], an array of probabilities for all classes of
	the specified discriminations. 
	*/
	public double [] applyModel( DataPoint p) {
	    throw new UnsupportedOperationException("N/a");
	}

	/** Computes logarithms of the scores returned by
	    applyModel. May be redefined by child classes for better
	    precision (preventing underflow) */
	public double [] applyModelLog( DataPoint p) {
	    double [] q= applyModel(p);
	    double [] qlog = new double[q.length];
	    for(int i=0; i<q.length; i++) qlog[i] = Math.log(q[i]);
	    return qlog;
	}

	/** Training for this particular block */
	public void absorbExample(Vector<DataPoint> xvec, int i1, int i2) throws  BoxerException {
	    throw new UnsupportedOperationException("N/a");
	}

	/** Reads in the data - parameters and the classifier's state
	    - pertaining to this particular discrimination
	 */
	void parseDisc(Element e) throws BoxerXMLException {
	    throw new UnsupportedOperationException("N/a");
	}

	/** Saves the data - parameters and the classifier's state -
	    pertaining to this particular discrimination
	 */
	public Element saveAsXML(Document xmldoc) {
	    throw new UnsupportedOperationException("N/a");
	}

	/** Prints a human-readable description of the classifier for
	  this particular discrimination. The method in the root class
	  prints nothing; it should be overriden by concrete classes
	  as needed
	  @param verbose If true, print a lot more data, including the
	  full content of coefficient matrices
	*/
	void describe(PrintWriter out, boolean verbose) {
	}

	public long memoryEstimate() {return 0;}

    }; // End of class KnnLearnerBlock 

    private Vector<DataPoint> points=new  Vector<DataPoint>();
    //private Vector<Double> norms = new Vector<Double>();

    /** This is the <em>k</em> in  <em>k</em>-NN.
     */
    int neighborhoodSize= 3;
    /** Only training examples with the cosine greater than this value (i.e., within r&lt;sqrt(2*(1-mincos))) are considered "neighbors".
     */
    double mincos=0;

    enum Weighting {
	EQUAL, INVERSE_DISTANCE;
    };
    final Weighting weighting = Weighting.INVERSE_DISTANCE;


    /** Applies the classifiers to the example p and returns the
	probabilities of membership in different classes.

	@param p An example to apply the model to
	@return An array of the probabilities of membership in
	different classes. For details, see {@link Model#applyModel( DataPoint p)}

     */
    final public double [][] applyModel( DataPoint p) {
	TopValues toplist= new  TopValues(neighborhoodSize, mincos);

	double pNorm = Math.sqrt(p.normSquareWithoutDummy());
	for(Map.Entry<DataPoint, PointInfo> e: pointInfoMap.entrySet()) {
	    DataPoint q=e.getKey();
	    double cos = p.dotProductWithoutDummy(q) / (pNorm * e.getValue().norm);
	    toplist.offer(q, e.getValue().multiplicity, cos);
	}

	double [][] s = new double[blocks.length][];
	double [] sum = new double[blocks.length];
	for(int did=0; did<s.length; did++) {
	    s[did] = new double[ blocks[did].dis.claCount()];
	}


	final double minDist=1e-6;
	final int  SHOW_VAL_CNT=5;

	if (Suite.verbosity>0) {
	    System.out.print(p.getName() + " : toplist size=" + toplist.getCnt());
	    if (toplist.map.size()>SHOW_VAL_CNT) {
		System.out.print(" [skip "+(toplist.map.size()-SHOW_VAL_CNT)+" vals]");
	    }
	}

	// dot products (cosine similarities) in ascending order
	int valCnt=0;
	for( Map.Entry<Double, TopValues.VectorDP> e:  toplist.map.entrySet()) {
	    TopValues.VectorDP points = e.getValue();
	    // when processing the 1st array, tie-breaking may be involved 
	    double dw = 1;
	    if (valCnt==0) {
		int excess = toplist.getCnt() - neighborhoodSize;
		if (excess > points.multi) throw new AssertionError( "(neighborhoodSize="+neighborhoodSize+") - (toplist.cnt="+toplist.getCnt()+") + (points.multi="+points.multi+") < 0");
		if (excess > 0) {
		    dw =  ((double)(points.multi - excess))/points.multi;
		}
		if (dw < 0) {
		    Logging.warning("dw=" + dw + "= (neighborhoodSize="+neighborhoodSize+") - (toplist.cnt="+toplist.getCnt()+") + (points.multi="+points.multi+")");
		}
	    }
	    double cos = e.getKey().doubleValue();
	    if (weighting == Weighting.INVERSE_DISTANCE) {
		double dist = (cos>1)? 0 : Math.sqrt( 2*(1-cos));
		dw /= Math.max(dist, minDist);
	    }

	    if (Suite.verbosity>0 && toplist.map.size()-valCnt<=SHOW_VAL_CNT ) {
		System.out.print(" [cos="+cos+" :");
		if (points.multi > 1) System.out.print(" cnt=" + points.multi);
		if (points.size()>3) System.out.print( " ...");
		else  for(DataPoint q: points) System.out.print(" " + q.getName());
		System.out.print("]");
		//	System.out.print(p.getName() + " : toplist size=" + toplist.getCnt());
	    }

	    for(DataPoint q: points) {
		double [][] ysec = pointInfoMap.get(q).ysec;
		for(int did=0; did<s.length; did++) {
		    for(int i=0; i<ysec[did].length; i++) {	
			s[did][i] += ysec[did][i] * dw;
			sum[did] += ysec[did][i] * dw;
		    }	
		}
	    }
	    valCnt++;
	}

	if (Suite.verbosity>0) {
	    System.out.println();
	}


	for(int did=0; did<s.length; did++) {
	    for(int i=0; i< s[did].length;i++) {
		if (sum[did]!=0) {
		    s[did][i] /= sum[did];
		} else {
		    // there were no neighbors; use defaults
		    s[did][i] = sDefault[did][i] / sumDefault[did];
		}
	    }
	}
	return s;	
    }


    /** Similar to {@link #applyModel( DataPoint p)}, but returns
     * <em>logarithms</em> of probabilities, rather than probabilities
     * themselves. In this class, this method only exists for
     * compatibility; it simply calls applyModel
     */
    final public double [][] applyModelLog( DataPoint p) {
	final double M = -100;
	double [][] s = applyModel(p);
	for(int did=0; did<s.length; did++) {
	    for(int j=0; j<s[did].length; j++) {
		s[did][j] = (s[did][j]==0) ? M: Math.log( s[did][j]);
	    }
	}
	return s;	
    }

    /** Applies the model for a particular discrimination to all data
       points in a given Vector section. This is implemented here as a
       wrapper over a single-datapoint method, but may be overriden by
       a learner such as BXRLearner. */
    public double [][] applyModelLog(Vector<DataPoint> v, int i0, int i1, int did) throws BoxerException {
	double [][] s = new double[i1-i0][];
	for(int i=i0; i<i1; i++) {
	    double q[][] = applyModelLog(v.elementAt(i)); 
	    s[i-i0] = q[did];
	}
	return s;	
    }

  public double [][] applyModel(Vector<DataPoint> v, int i0, int i1, int did) throws BoxerException {
	double [][] s = new double[i1-i0][];
	for(int i=i0; i<i1; i++) {
	    double q[][] = applyModel( v.elementAt(i)); 
	    s[i-i0] = q[did];
	}
	return s;	
    }

    /** Stores the norm and the sum-of-labels of a data point or a family of data points with an identical featur vector */
    private static class PointInfo {
	/** The norm of underlying feature vector */
	double norm;
	/** How many data points have this feature vector? */
	int multiplicity;
	/** Sum of labels from these vectors */
	double ysec[][];
	PointInfo(double[][] _ysec, double _norm) {
	    multiplicity = 1;
	    ysec=_ysec; 
	    norm=_norm;
	}
	PointInfo(boolean[][] _ysec, double _norm) {
	    multiplicity = 1;
	    ysec=new double[_ysec.length][];
	    for(int i=0; i<ysec.length; i++) {
		ysec[i]=new double[_ysec[i].length];
		int j=0;
		for(boolean w: _ysec[i]) {
		    ysec[i][j++] =  (w ?  1: 0);
		}
	    }	    
	    norm=_norm;
	}

	void addYSec(double [][] z) {
	    multiplicity++;
	    for(int i=0; i<ysec.length; i++) {
		int j=0;
		for(double w: z[i]) {
		    ysec[i][j++] += w;
		}
	    }
	}
 	void addYSec(boolean [][] z) {
	    multiplicity++;
	    for(int i=0; i<ysec.length; i++) {
		int j=0;
		for(boolean w: z[i]) {
		    ysec[i][j++]+= (w ? 1:0);
		}
	    }
	}
    }

    /** The training set is stored like this. */
    private HashMap<DataPoint, PointInfo> pointInfoMap=new HashMap<DataPoint, PointInfo>();

    /** If true, the learner will check if some data points have
     * identical feature vectors, and try to save on this */
    boolean pragmaCacheDataPoints=false;

    public void setPragmaCacheDataPoints(boolean x) {
	pragmaCacheDataPoints=x;
    }

    /** Incrementally trains this classifier on data points xvec[i],
     * i1&le;i&lt;i2. 

     A particular algorithm may process these vectors in a purely
     sequential ("online") way (i.e., when it performs training on
     xvec[i1], it makes use of no knowledge whatsoever about xvec[j]
     with j&gt;i), or it may make use of all (i2-i1) training data
     points at once (e.g., by computing certain aggregate properties
     of the set first). Thus if you desire to carry out training in a
     "purely online" fashion, one data point at time, you may want to
     invoke this method for one data point at a time.

     This is the main absorbExample() method; other methods of the
     same name are just "syntactic sugar" for it.

     @param xvec A vector of data points for training. Only a section of this vector (from i1 thru i2-1) will be used.

     // FIXME should throw BoxerException

     */
    public void absorbExample(Vector<DataPoint> xvec, int i1, int i2) {
	createMissingBlocks();
	for(LearnerBlock block: blocks) {
	    block.dis.ensureCommitted(); 
	}
	for(int i=i1; i<i2; i++) {
	    DataPoint p=xvec.elementAt(i);

	    boolean[] y = p.getY(suite);
	    boolean ysec[][] = suite.splitVectorByDiscrimination(y);

	    // create a local copy, so that we can look for identical vectors
	    DataPoint key = pragmaCacheDataPoints ? p.shallowCopyWithoutLabels(p.getName()) : p;

	    if (pointInfoMap.containsKey(key)) {
		pointInfoMap.get(key).addYSec(ysec);
	    } else {
		points.add(key);
		pointInfoMap.put(key, new PointInfo( ysec, Math.sqrt(key.normSquareWithoutDummy())));	    
	    }

	    // Adjust counts of examples in each class
	    for(int did=0; did<sDefault.length; did++) {
		for(int j=0; j<ysec[did].length; j++) {
		    if (ysec[did][j]) {
			sDefault[did][j] += 1;
			sumDefault[did] += 1;
		    }
		}	
	    }
	}


    }

    public void describe(PrintWriter out, boolean verbose) {
	out.println("kNN learner with k=" + neighborhoodSize + "; cos>"+mincos+"; weighting="+weighting);
    }

 
    /** Saves the complete internal state of the classifier (with the
     * current values of all parameters, any latent coefficients or
     * whatever, as well as its {@link edu.dimacs.mms.boxer.Suite
     * Suite}) as an XML Document object, which can later be written
     * into an XML file. The file can be read in later on to re-create
     * the classifier.

     FIXME: the data aren't actually saved.

     @return An XML document that can be saved to the file

     @see #describe() describe()
     */
    public Element saveAsXML(Document xmldoc) {
		
	Element root = xmldoc.createElement( XMLUtil.LEARNER);
	root.setAttribute(ParseXML.ATTR.NAME_ATTR, getName());
	root.setAttribute(ParseXML.ATTR.LEARNER.ALGORITHM, algoName());
	root.setAttribute("version", Version.version);
	root.appendChild( saveParamsAsXML(xmldoc));

	for(LearnerBlock b: blocks)  {
	    root.appendChild(b.saveAsXML(xmldoc));
	}

	Element e = xmldoc.createElement( ParseXML.NODE.DATASET);
	for(int i=0; i<points.size(); i++) {
	    e.appendChild( points.elementAt(i).toXML( xmldoc));
	}
	root.appendChild( e);

	xmldoc.appendChild(root);
	return root;
    }


    /** Returns the estimated size, in bytes, of the classifier's main
	data structures. Does not try to include the feature dictionary
	or the Suite data. Child classes may override as needed,
	probably piggybacking on the parent class's method.  */
    public long memoryEstimate() {
	long sum = 2*Sizeof.OBJ +  Sizeof.OBJREF;
	if (blocks!=null) { 
	    for(LearnerBlock block: blocks) {
		sum += Sizeof.OBJREF + block.memoryEstimate();
	    }
	}
	for( DataPoint p: points) sum+= p.memoryEstimate();
	// FIXME: plenty more per entry, in fact...
	sum += pointInfoMap.size() * Sizeof.DOUBLE;
	return sum;
    }

 

    /** Creates a new learner block of a suitable type for the
	specified discrimination, based on the learner's general
	parameters. Each concrete learner class defines this method to
	return an object of its appropriate LearnerBlock subclass.

	If a "model" LearnerBlock is supplied (i.e., not null), the
	method verifies that it's a suitable fallback-discrimination
	learner block, and creates the new block based on that
	model. Proper care is taken to map the fallback
	discrimination's default class to the fallback class of this
	block's discriminations.

	@param model The learner block to be copied into the new
	block. This is only used in DefaultHistoryMode, and is null if
	we are to create a block from scratch
     */
    LearnerBlock createBlock(Discrimination dis, LearnerBlock model) {
	return new KnnLearnerBlock(dis);
    }

    /** This is used to keep track of the numbers of training examples
     * in each class */
    double [][] sDefault;
    double [] sumDefault;


    NormalizedKnnLearner(Suite _suite, Element e) throws org.xml.sax.SAXException,  BoxerXMLException  {
	setSuite( _suite);
	if (e==null) {
	    name = suite.makeNewAnonLearnerName();
	    createAllBlocks();
	} else {
	    parseLearner(e); 
	}
	initName(e);

	sDefault = new double[blocks.length][];
	sumDefault = new double[blocks.length];
	for(int did=0; did<sDefault.length; did++) {
	    sDefault[did] = new double[ blocks[did].dis.claCount()];
	}


    }

   /** Initializes various parts of the learner from an XML
     * element. This method is invoked from the XML-based constructors
     * of the derived classes.
     */    
    private final void parseLearner( Element e) throws	org.xml.sax.SAXException,  BoxerXMLException{
	XMLUtil.assertName(e, XMLUtil.LEARNER);

	// First, find and parse the "parameters" tag, if it exists
	Element pe = findParameters(e);
	if (pe!=null) {
	    parseParams(pe); // subclass-defined	    
	}
	createAllBlocks();
	// no block-specific params need to be set in kNN
    }

   /** Names of parameters, as they appear in XML files */
    static class PARAM {
	final static String 
	    K = "k", MINCOS="mincos";
    }
  
    private void parseParams(Element e) throws BoxerXMLException  {
	XMLUtil.assertName(e, Learner.PARAMETERS);

	HashMap<String,Object> h = makeHashMap
	    ( new String[] { PARAM.K, PARAM.MINCOS},
	      new Object[] {   new Integer(neighborhoodSize), new Double(mincos)	      });

	h = parseParamsElement(e,h);
	neighborhoodSize = ((Number)(h.get(PARAM.K))).intValue();
	if (neighborhoodSize<1) throw  new BoxerXMLException("k must be >=1");
	mincos = ((Double)(h.get(PARAM.MINCOS))).doubleValue();
	if (mincos>=1 || mincos < 0) throw  new BoxerXMLException("mincos must be in the range 0<=mincos<1");
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
