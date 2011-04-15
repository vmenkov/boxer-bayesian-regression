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
	    out.print("B"); // just to see if it works...
	}

	public long memoryEstimate() {return 0;}

    }; // End of class KnnLearnerBlock 

    private Vector<DataPoint> points=new  Vector<DataPoint>();
    private Vector<Double> norms = new Vector<Double>();
    /** This is the <em>k</em> in  <em>k</em>-NN.
     */
    int neighborhoodSize= 3;

    enum Weighting {
	EQUAL, INVERSE_DISTANCE;
    };
    final Weighting weighting = Weighting.INVERSE_DISTANCE;

    /*
    static class Pair implements Comparable<Pair> { 
	DataPoint point; double cos; 
	Pair(	DataPoint _point, double _cos) {
	    point = _point;
	    cos = _cos;	      
	} 
	public int compareTo(Pair a) {
	    return (cos < a.cos) ? -1 : cos>a.cos? 1 : 0;
	}
    }
    */
    /** This class stores a specified number of the top values from
	those "offered" to you. It may store more, if it's needed to
	break a tie.
     */
    static private class TopValues {
	/** Each vector contains all DataPoints with the same cos value. Ordered in the order of
	 ascending cos. */
	SortedMap<Double, Vector<DataPoint>> map=new TreeMap<Double, Vector<DataPoint>>();
	/** number of DataPoint objects stored */
	private int cnt=0;
	int getCnt() { return cnt; }
	final int desiredSize;
	TopValues(int n) {
	    desiredSize=n;
	    if (desiredSize<1) throw new IllegalArgumentException();
	}
	/** How many elements with the identical value are at the bottom
	    of the list? */
	/* Insert the value; adjust count.
	*/
	private void insert(DataPoint q, double cos) {
	    Double key = new Double(cos);
	    if (map.containsKey(cos)) map.get(key).add(q);
	    else {
		Vector<DataPoint> v = new 		Vector<DataPoint>(1);
		v.add(q);
		map.put(key, v);
	    }
	    cnt++;
	}

	/** Removes the values (a vector of them, that is) with the lowest key value, if doing this
	    does not make the stored element count less than the desired size. Adjust the count.
	 */
	private void trimIfCan() {
	    if (cnt > desiredSize) {
		Double key = map.firstKey();
		int cnt1 = cnt - map.get(key).size();
		if (cnt1 >= desiredSize) {
		    map.remove(key);
		    cnt=cnt1;
		}
	    }
	}

	void offer(DataPoint q, double cos) {
	    if (cos == 0) return;
	    if (cnt<desiredSize || cos >=  map.firstKey().doubleValue()) {
		insert(q,cos);
		trimIfCan();
		return;
	    } 
	}

	/*
	int tbCnt() {
	    if (size()<=desiredSize) return 0;
	    int tb = lowestValueCount();
	    if (size()-lowestValueCount()>=desiredSize) throw new AssertionError("toplist: we've been storing extra values all the way!");
	    return tb;
	}
	*/
    }



    /** Applies the classifiers to the example p and returns the
	probabilities of membership in different classes.

	@param p An example to apply the model to
	@return An array of the probabilities of membership in
	different classes. For details, see {@link Model#applyModel( DataPoint p)}

     */
    final public double [][] applyModel( DataPoint p) {
	TopValues toplist= new  TopValues(neighborhoodSize);

	double pNorm = Math.sqrt(p.normSquareWithoutDummy());
	for(int i=0; i<points.size(); i++) {	
	    DataPoint q=points.elementAt(i);
	    double cos = p.dotProductWithoutDummy(q) / (pNorm*norms.elementAt(i).doubleValue());
	    toplist.offer(q,cos);
	}

	double [][] s = new double[blocks.length][];
	double [] sum = new double[blocks.length];
	for(int did=0; did<s.length; did++) {
	    s[did] = new double[ blocks[did].dis.claCount()];
	}


	final double minDist=1e-6;
	
	/*
	// are there any tie-breaking values at the bottom of toplist?
	int tbCnt= toplist.tbCnt();
	double tbWt = (tbCnt==0) ? 1 : 
	    ((double)(neighborhoodSize - toplist.getCnt() + tbCnt))/tbCnt;
	if (tbWt<0 || tbWt>1) throw new AssertionError("Tie-breaking error");

	
	if (Suite.verbosity>0) {
	    String msg = "";
	    int skipCnt=Math.max(toplist.size() - 10,0);    
	    if (skipCnt>0) msg += " ... and " + skipCnt + " more!";
	    int cnt=0;
	    for(Pair pair: toplist) {
		if (cnt++ < skipCnt) continue;
		msg = ", " + pair.point.getName() + " : "+ pair.cos + msg;
	    }
	    System.out.println(p.getName() + msg);
	    System.out.println("toplist size=" + toplist.size() + ", tbCnt=" + tbCnt+", wt=" + tbWt);
	}
	*/
	
	final int  SHOW_VAL_CNT=5;

	if (Suite.verbosity>0) {
	    System.out.print(p.getName() + " : toplist size=" + toplist.getCnt());
	    if (toplist.map.size()>SHOW_VAL_CNT) {
		System.out.print(" [skip "+(toplist.map.size()-SHOW_VAL_CNT)+" vals]");
	    }
	}


	// dot products (cosine similarities) in ascending order
	int valCnt=0;
	for( Map.Entry<Double,  Vector<DataPoint>> e:  toplist.map.entrySet()) {
	    Vector<DataPoint> points = e.getValue();
	    // when processing the 1st array, tiebreaking may be involved 
	    double dw = (valCnt==0) ?
		dw =  ((double)(neighborhoodSize - toplist.getCnt() + points.size()))/points.size() :
		1;
	    double cos = e.getKey().doubleValue();
	    if (weighting == Weighting.INVERSE_DISTANCE) {
		double dist = (cos>1)? 0 : Math.sqrt( 2*(1-cos));
		dw /= Math.max(dist, minDist);
	    }

	    if (Suite.verbosity>0 && toplist.map.size()-valCnt<=SHOW_VAL_CNT ) {
		System.out.print(" [cos="+cos+" : ");
		if (points.size()>3) System.out.print( "" + points.size() + " points");
		else for(DataPoint q: points) System.out.print(" " + q.getName());
		System.out.print("]");
		//	System.out.print(p.getName() + " : toplist size=" + toplist.getCnt());
	    }

	    for(DataPoint q: points) {
		boolean[] y = q.getY(suite);
		boolean ysec[][] = suite.splitVectorByDiscrimination(y);
		for(int did=0; did<s.length; did++) {
		    for(int i=0; i<ysec[did].length; i++) {
			if (ysec[did][i]) {
			    s[did][i] += dw;
			    sum[did] += dw;
			}
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
       point in a given Vector section. This is implemented here as a
       wrapper over a single-datapoint method, but may be overriden by
       a learner such as BXRLearner. */
    public double [][] applyModelLog(Vector<DataPoint> v, int i0, int i1, int did) throws BoxerException {
	throw new UnsupportedOperationException("N/a");
	/*
	double [][] s = new double[i1-i0][];
	for(int i=i0; i<i1; i++) {
	    s[i-i0] = blocks[did].applyModelLog(v.elementAt(i));
	}
	return s;	
	*/
    }



    /** Estimates probabilities of a given data point's belonging to
	various classes of a specified discrimination.

	@param p DataPoint to score
	@param did Discrimination id

	@return double[], an array of probabilities for all classes of
	the discrimination in question. It will be aligned with
	Discrimination.classes of the selected discrimination */
    /*
    final public double [] applyModel( DataPoint p, int did) {
	throw new UnsupportedOperationException("N/a");
	//return blocks[did].applyModel(p);
    }
    */

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
	    /*
	    block.validateExamples(xvec, i1,i2);
	    try {
		block.absorbExample(xvec, i1, i2);
	    } catch(BoxerException ex) {
		Logging.error(ex.getMessage());
		// FIXME
		throw new AssertionError(ex.getMessage());
	    }
	    */
	}
	for(int i=i1; i<i2; i++) {
	    DataPoint p=xvec.elementAt(i);
	    points.add(p);
	    norms.add(new Double(Math.sqrt(p.normSquareWithoutDummy())));

	    // Adjust counts of examples in each class
	    boolean[] y = p.getY(suite);
	    boolean ysec[][] = suite.splitVectorByDiscrimination(y);
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
	out.println("kNN learner with k=" + neighborhoodSize + "; weighting="+weighting);
    }

 
    /** Saves the complete internal state of the classifier (with the
     * current values of all parameters, any latent coefficients or
     * whatever, as well as its {@link edu.dimacs.mms.boxer.Suite
     * Suite}) as an XML Document object, which can later be written
     * into an XML file. The file can be read in later on to re-create
     * the classifier.
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
	if (blocks!=null) { // TrivialLearner has no blocks
	    for(LearnerBlock block: blocks) {
		sum += Sizeof.OBJREF + block.memoryEstimate();
	    }
	}
	for( DataPoint p: points) sum+= p.memoryEstimate();
	sum += norms.size() * Sizeof.DOUBLE;
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
	    K = "k";
    }
  
    private void parseParams(Element e) throws BoxerXMLException  {
	XMLUtil.assertName(e, Learner.PARAMETERS);

	HashMap<String,Object> h = makeHashMap
	    ( new String[] { PARAM.K},
	      new Object[] {   new Integer(neighborhoodSize)});

	h = parseParamsElement(e,h);
	neighborhoodSize = ((Number)(h.get(PARAM.K))).intValue();
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
