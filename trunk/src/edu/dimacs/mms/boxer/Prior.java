package edu.dimacs.mms.boxer;

import java.util.*;
import java.io.*;
import org.w3c.dom.Element;
import org.w3c.dom.Document;
//import org.w3c.dom.Node;

/** An individual "prior", which may be applicable only to a single
    (discrimination.class, feature) pair, or for a whole class of them.
    A single prior may be represented as a (mode,variance) pair.

    <p>This class is abstract; concrete classes implement particular
    types of priors (e.g., LaplacePrior), and have appropriate
    methods and data structures for those types.
*/
abstract public class Prior //implements Measurable 
{

  /** XML element names 
      prior class="Denmark" feature="mermaid" type="l" mode="2.0" lambda
= "0.1" absolute="false" skew="0"
  */
    static class ATTR {
	final static String
	    CLASS = "class", FEATURE = "feature", 
	    TYPE = "type", MODE = "mode", 
	    VAR = "var",
	    ABSOLUTE="absolute",
	    SKEW="skew";
    }

    
    /** The mode of the distribution */
    double mode=0;
    /** The variance, as supplied in the input. This can be a relative
     * value. */
    private double var=1.0;

    /** If false, the value of the variance should be multiplied by the
	base values (from L1 or L0) */
    boolean absolute=true;
    /** Must be 0, 1, or -1 */
    int skew=0;

    public static enum Type {
	 l, g;
    } 

    //Type type;

    /** The <em>absolute</em> variance. (If this is an absolute prior,
      this is the same as the stated variance; otherwise, multiplied
      by the base prior's variance) */
    double avar;

    /** This is set once all data bits are computed */
    boolean completed = false;

    /** This is called at the end of the constructor, to initialize
	all derived members of the instance. Child methods override
	this method as needed, but keep calling super.complete()
    */
    void complete(Prior base) throws BoxerXMLException {
	if (absolute) {
	    avar = var;
	} else if (base==null) {
	    throw new BoxerXMLException("The top-level prior must be absolute; it cannot be specified as 'relative' to any other prior");
	} else {
	    avar = var * base.avar;
	}
	completed = true;
    }

    /*
    void setType(String s) {
	if (s.equals(Type.L))
    }
    */

    abstract Type getType();

    /**
       Parses one "prior" XML element into a Prior object (of an
       appropriate derived type, such as LaplacePrior).

       <P>Expected XML element syntax:
       &lt;
       prior
       type="l" mode="2.0" var= "0.1" absolute="false" [skew="0" ]
       /&gt;

       FIXME: need support for "infinity"

	@param e An XML Element of the type  "prior" 

	@param base The Level1 prior which will be used to scale the
	prior now being read if the latter is not absolute. Null can
	be passed here (and, in fact, it is when we're parsing the
	Level1 prior itself!), but when it is, the currently read
	prior must be absolute


     */
    static Prior parsePrior(Element e, Prior base) throws BoxerXMLException {
	XMLUtil.assertName(e, Priors.NODE.PRIOR);

	Type type = Type.valueOf( XMLUtil.getAttributeOrException(e, ATTR.TYPE));
	Prior p;
	if (type == Type.l) {
	    p = new LaplacePrior();
	} else {
	    throw new BoxerXMLException("Prior type " + type + " is not currently supported");
	}

	p.mode = XMLUtil.getAttributeDouble( e, ATTR.MODE);
	p.var = XMLUtil.getAttributeDouble( e, ATTR.VAR);
	p.absolute = XMLUtil.getAttributeBoolean( e, ATTR.ABSOLUTE);
	p.skew = XMLUtil.getAttributeInt( e, ATTR.SKEW, 0);
	if (p.skew<-1 || p.skew>1) throw new  BoxerXMLException("Invalid value of skew: " + p.skew);
	p.complete(base);
	return p;
    }

    /** Constructs the built-in default prior */
    static Prior makeL0() {
	return new LaplacePrior();
    }


    public Element saveAsXML(Document xmldoc) {
	Element e = xmldoc.createElement(Priors.NODE.PRIOR);
	e.setAttribute(ATTR.TYPE, getType().toString());
	e.setAttribute(ATTR.MODE, ""+mode);
	e.setAttribute(ATTR.VAR, ""+var);
	e.setAttribute(ATTR.ABSOLUTE, ""+absolute);
	e.setAttribute(ATTR.SKEW, ""+skew);


	//CLASS = "class", FEATURE = "feature", 
	return e;
    }

    public String toString() {
	return "("+getType().toString() +
	    ", " + ATTR.MODE+"="+mode +
	    ", " +ATTR.VAR +"="+ var+
	    ", " + ATTR.ABSOLUTE+ "="+absolute+
	    ", " + ATTR.SKEW+ "="+skew+")";
    }
    


   /** Applies the prior to a particular element 
	@param trunc Encodes the cur-off threshold theta (directly,
	and via trunc.mode)
     */
    abstract double apply(double val, Truncation trunc, int mult);


}

    