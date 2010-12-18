package edu.dimacs.mms.boxer;

import java.util.HashMap;
import java.util.Vector;

import org.w3c.dom.Element;
import org.w3c.dom.Node;


/** This class contains methods common for all PLRM learning
 * algorithms. One can design more subclasses of this class,
 * implementing other PLRM-based algorithms.
 */
public abstract class PLRMLearner extends Learner {

    public abstract class PLRMLearnerBlock extends Learner.LearnerBlock {

	/** The matrix of current Beta vectors. They are updated
	  through learning */
	protected BetaMatrix w = new BetaMatrix();

	/** The matrix of current Beta vectors. They are updated
	  through learning */
	public Matrix getW() { return w; }

	/** Returns true if this learning block contains no
	non-trivial information obtained from learning.
	
	// FIXME: strictly speaking, we also should check whetherTruncation.t==0
	*/
	boolean isZero() {
	    return w.isZero();
	}

	/** Copies all data structures of the old block into this
	    block.  This is used in AssumeDefaultHistory. The old
	    block is assumed to be that of a fallback discrimination
	    with a default class; this blocks discrimination should be
	    mappable to it.
	    @param b An old block
	 */
	void initFrom(PLRMLearnerBlock b) {
	    Logging.info("AssumeDefaultHistory: Initializing PLRM Learner block for discrimination " + dis + " from the one for the fallback discrimination " + b.dis);
	    String errmsg = null;
	    if (b.dis.claCount() != dis.claCount()) errmsg = "different class count";
	    else if (b.dis.getDefaultCla() == null) errmsg = "absence of default class in " + b.dis;
	    else if (dis.getDefaultCla() == null) errmsg = "absence of default class in " + dis;
	    else if (!b.w.looksLikeFallback(b.dis)) errmsg = "the values in the matrix for " + b.dis + " not looking like a fallback discrimination's matrix";

	    if (errmsg != null) {
		throw new IllegalArgumentException("Cannot AssumeDefaultHistory for discriminaion " + dis + " based on "+b.dis+", because the two discriminations cannot be matched to each other, due to " + errmsg);
	    }

	    int oldDef = b.dis.getDefaultCla().getPos();
	    int newDef = dis.getDefaultCla().getPos();
	    w = new BetaMatrix(b.w, oldDef, newDef);
	}

	/** Estimates probabilities of a given data point's belonging to
	    various classes, using the exp(DP)/sum(exp(DP)) formula. This
	    is done separately in each Discrimination (set of classes).
	    Source: Figure 1a, PLRM Interpreter.
	    
	    @param p DataPoint to score

	    @return double[], an array of probabilities for all
	    classes of this block's discrimination. It is aligned with
	    the discrimination's list of classes (dis.classes)
	*/
	public double [] applyModel( DataPoint p) {

	    //final double UF = -600;

	    double[] dot = p.dotProducts(w, dis);
	    if (dot.length==0) return dot; //empty discr-probably dummy fallback
	    double z=0, maxDot=dot[0];
	    // We'll normalize the exponent, to reduce the chance of overflow
	    for(int i=1; i<dot.length; i++) {
		if (dot[i]>maxDot) {
		    maxDot = dot[i];
		} 
	    }
	    
	    for(int i=0; i<dot.length; i++) {
		double a  = dot[i] - maxDot;
		dot[i] = /*(a < UF) ? 0 : */ Math.exp(a);
		z += dot[i];
	    }

	    for(int i=0; i<dot.length; i++) {
		dot[i] /= z;
	    }
	    return dot;
	}

	/** Returns logarithms of scores */
	public double [] applyModelLog( DataPoint p) {

	    double[] dot = p.dotProducts(w, dis);
	    if (dot.length==0) return dot; // empty discr, probably dummy fallback
	    double[] e=new double[dot.length];
	    double z=0, maxDot=dot[0];
	    // We'll normalize the exponent, to reduce the chance of overflow
	    for(int i=1; i<dot.length; i++) {
		if (dot[i]>maxDot) {
		    maxDot = dot[i];
		} 
	    }
	    
	    for(int i=0; i<dot.length; i++) {
		dot[i] -= maxDot;
		e[i] = Math.exp(dot[i]);
		z += e[i];
	    }

	    double logz = Math.log(z);

	    for(int i=0; i<dot.length; i++) {
		e[i] /= z;
		dot[i] -= logz;
	    }
	    return dot;
	}

	double logLikelihood(Vector<DataPoint> xvec, int i1, int i2) {
	    return  logLikelihood(xvec, i1, i2,null);
	}

	/**
	   @param zz Output parameter. If it is not null, it must be
	   pre-allocated (as new double[i2-i1][]) before calling the
	   method. Upon return, z[j] will contain the vector (Y-P) for
	   xvec[i1+j] (based on applyModelLog), or null if the data
	   point xvec[i1+j] has no class label and ought to be
	   skipped. If it's null on input, it will be ignored.
	   @return The avg log-likelihood for all labeled examples from
	   xvec[i1:i2-1], or 0 if none is labeled
	 */
	double logLikelihood(Vector<DataPoint> xvec, int i1, int i2,
			     double zz[][]) {
	    if (zz!=null && zz.length != i2-i1) throw new IllegalArgumentException("The log prob array must be pre-allocated to size "+(i2-i1));

	    double logLik = 0;
	    int n=0;
	    for(int i=i1; i<i2; i++) {
		DataPoint x = xvec.elementAt(i);
		double [] logProb = applyModelLog(x);
		Discrimination.Cla trueC = x.claForDisc(dis);
		
		if (trueC==null) {
		    if (zz!=null) zz[i-i1]=null;
		    continue; // example not labeled for this discr
		}

		n++;
		int trueCPos =  trueC.getPos();
		if (zz!=null) {
		    double z[] = new double[logProb.length];
		    for(int k=0; k<logProb.length; k++) {
			z[k] =  (k==trueCPos? 1: 0) - Math.exp(logProb[k]);
		    }
		    zz[i-i1] = z;
		}
		logLik += logProb[trueCPos];
	    }
	    if (n==0) {
		// no labeled examples given!
		return 0;
	    }
	    return logLik/n;
	}


	/** Computes weights with which this data point's vector
	    should be added to various columns of the classifier's
	    coefficient matrix. It is (1-P) for the classes to which
	    the example should belong, and (-P) for others, where p is
	    the probability score returned by the current classifier.

	    This method also ensures that the columns for discriminations
	    for which the example x is not labeled won't be updated, and
	    marks all other discriminations as "used in training".
	    
	    @return Array of weights, or null if the example is not
	    labeled for the relevant discrimination

	*/
	double[] adjWeights(DataPoint x) {
	    
	    double prob[] = applyModel(x);
	    if (Suite.verbosity>1) System.out.println("Scored train vector " + x.name +" for dis="+dis.name+"; scores=" +
			   x.describeScores(prob, dis));

	    Discrimination.Cla trueC = x.claForDisc(dis);

	    int r = prob.length;
	    
	    // No update will be done for discriminations for which this
	    // example is not labeled
	    if (trueC==null) {
		return null;
	    } else {
		double z[] = new double[prob.length];
		int trueCPos =  trueC.getPos();

		for(int k=0; k<r; k++) {
		    z[k] =  (k==trueCPos? 1: 0) - prob[k];
		}
		return z;
	    }
	}

	/** Parses the element for discrimination-specific parameters,
	    that may be supplied in the learner's description. The
	    method in PLRMLearner does nothing, but may be overridden
	    by Learners that have discrimination-specific params.

	    @param a "parameters" XML element
	*/
	void parseDSP(Element e)  throws BoxerXMLException{};

	/** Returns a list of matrix objects and their names that we
	    should look for in the input XML
	 */
	abstract HashMap<String, Matrix> listMatrices();

	/** Parses a "classifier" element of an XML file, adding the
	    and the appropriate matrix section(s) to this model's
	    matrices, and setting the discrimination-specific
	    algorithm parameters, if any.
	*/
	final void parseDisc(Element e) throws BoxerXMLException {
	    HashMap<String, Matrix> h =  listMatrices();// subclass-defined
	    for(Node n = e.getFirstChild(); n!=null; n = n.getNextSibling()) {
		if ( n.getNodeType() == Node.ELEMENT_NODE) {
		    if ( n.getNodeName().equals(XMLUtil.MATRIX)) {
			Element me = (Element)n;
			String matName = me.getAttribute(ParseXML.ATTR.NAME_ATTR);
			Matrix mat = h.get(matName);
			if (mat==null) {
			    throw new IllegalArgumentException("Matrix name is " + matName + " is not expected in the XML file");
			} else {
			    mat.readMatrix(me, suite, dis);
			}
		    } else  if ( n.getNodeName().equals(PARAMETERS)) {
			parseDSP((Element)n);// subclass-defined
		    } else {
			throw new IllegalArgumentException("Element " + e.getTagName() + " is not supposed to contain child element " +  n.getNodeName());
		    }
		}
	    }
	}

    } // end of inner class

    /** Truncation modailities, if any. Should be set in the
     * constructor (after reading any params from the XML input, if
     * given). This object is used as the default pattern for all
     * learner blocks, although they may have their own individual
     * rules, if provided for in the XML definition. */
    Truncation commonTrunc; 
    /** Creates the default {@link edu.dimacs.mms.boxer.Truncation
     * Truncation} object that will be used by all blocks of the
     * learner if no discirmination-specific truncation rules are
     * supplied in the learner's XML description. This meathod is
     * called (directly or indirectly) from the learner's
     * constructor. */
    abstract Truncation defaultCommonTrunc();
    

    /** Common initialization procedures for all derived classes. The
      values are initialized based on the content of an XML element
      (which may be the top-level element of an XML file), or more
      often, an element nested within a "learners" element within a
      "learner complex" element.
      
      <p> This method should not be invoked other than from child
      class constructors'.

      @param _suite in the context of which the learner will exist. Must be non-null
      @param e XML element ("learner") to read the learner's description for. Optional; may be null, in which case all defaults are used.

    */
 
    protected void init(Suite _suite, Element e) throws
	org.xml.sax.SAXException, BoxerXMLException{
	setSuite( _suite);
	if (e==null) {
	    name = suite.makeNewAnonLearnerName();
	    commonTrunc = defaultCommonTrunc();
	    createAllBlocks();
	} else {
	    parseLearner(e); 
	}
	initName(e);
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

	// ensure that all blocks exist, and that they inherit any
	// applicable "common" parameters (such as commonTrunc)
	createAllBlocks();

	for(Node n = e.getFirstChild(); n!=null; n = n.getNextSibling()) {
	    int type = n.getNodeType();
	    String val = n.getNodeValue();
	    if (type == Node.TEXT_NODE && val.trim().length()>0) {
		Logging.warning("Warning: found an unexpected non-empty text node, val="  + val.trim());
	    } else if (type == Node.ELEMENT_NODE) {
		Element ce = (Element)n;
		String name = n.getNodeName();
		if (name.equals( PARAMETERS)) {
		    // parsed already
		} else if (name.equals(XMLUtil.CLASSIFIER)) {
		    String disName = XMLUtil.getAttributeOrException(ce,XMLUtil.DISCRIMINATION);
		    Discrimination dis = suite.getDisc(disName);
		    if (dis==null) {
			throw new IllegalArgumentException("Classifier element contains referecence to discrimination name '"+disName+"', which does not exist in the current suite");
		    }
		    int did = suite.getDid(dis);
		    blocks[did].parseDisc(ce);
		    //System.out.println("[DEBUG]: done parseDisc["+did+"]");
		} else {
		    throw new IllegalArgumentException("Unexpected element node name: " +name + " within a " + e.getTagName() + " element");
		}		
	    }
	}
    }

    /** Names of parameters, as they appear in XML files */
    static class PARAM {
	final static String f = "f", U = "u" , maxInfNorm = "maxInfNorm",
	    classSizes = "classSizes",
	    K = "k", t = "t";
    }

    /** Derived classes must have their own implementation, looking
     * for parameters they need */
    abstract void parseParams(Element e) throws BoxerXMLException;


    /** Finds the "parameters" element enclosed into the given "learner" element
	@param e A "learner" XML element
	@return A "parameters" element, or null if none is found
     */
    private Element findParameters( Element e) throws org.xml.sax.SAXException,  BoxerXMLException {
	XMLUtil.assertName(e, XMLUtil.LEARNER);

	for(Node n = e.getFirstChild(); n!=null; n = n.getNextSibling()) {
	    if (n.getNodeType() == Node.ELEMENT_NODE) {
		Element ce = (Element)n;		
		if (n.getNodeName().equals( PARAMETERS)) return ce;
	    }
	}
	return null;
    }


    /** This is invoked from {@link Suite.deleteDiscrimination()},
     * before the discrimination is purged from the Suite. Child
     * classes may override it, to delete more structures.
     */
    void deleteDiscrimination( RenumMap map) {
	super.deleteDiscrimination(map); 
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