package edu.dimacs.mms.boxer;

import java.util.*;
import java.io.*;
import org.w3c.dom.Element;
import org.w3c.dom.Document;
//import org.w3c.dom.Node;

/** An individual "prior", which may be applicable only to a single
    (discrimination.class, feature) pair, or for a whole class of them.
    A single prior may be represented as a (mode,variance) pair.
*/
public class Prior //implements Measurable 
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


    double mode=0, var=0;
    /** This is like the gravity parameter in plain truncation. It
     * is based on the variance */
    //double lambda;
    /** If false, the values of mode and lambda should be multiplied by the
	base values (from L1 or L0) */
    boolean absolute=true;
    /** Must be 0, 1, or -1 */
    int skew=0;

    public static enum Type {
	 l, g;
    } 

    Type type;
    /*
    void setType(String s) {
	if (s.equals(Type.L))
    }
    */

    /**
       Reads one "prior" XML element.

       &lt;
       prior
       class="Denmark" feature="mermaid" 
       type="l" mode="2.0" var= "0.1" absolute="false" [skew="0" ]
       /&gt;

       FIXME: need support for "infinity"

     */
    Prior(Element e) throws BoxerXMLException {
	XMLUtil.assertName(e, Priors.NODE.PRIOR);

	type = Type.valueOf( XMLUtil.getAttributeOrException(e, ATTR.TYPE));
	mode = XMLUtil.getAttributeDouble( e, ATTR.MODE);
	var = XMLUtil.getAttributeDouble( e, ATTR.VAR);
	absolute = XMLUtil.getAttributeBoolean( e, ATTR.ABSOLUTE);
	skew = XMLUtil.getAttributeInt( e, ATTR.SKEW, 0);
	if (skew<-1 || skew>1) throw new  BoxerXMLException("Invalid value of skew: " + skew);
    }

    /** Constructs the built-in default prior */
    static Prior makeL0() {
	return new Prior();
    }

    /** Constructs the built-in default prior */
    Prior () {
	type = Type.l;
	mode= 0;
	var = 1.0;
	absolute=true;
	skew=0;
    }

    public Element saveAsXML(Document xmldoc) {
	Element e = xmldoc.createElement(Priors.NODE.PRIOR);
	e.setAttribute(ATTR.TYPE, type.toString());
	e.setAttribute(ATTR.MODE, ""+mode);
	e.setAttribute(ATTR.VAR, ""+var);
	e.setAttribute(ATTR.ABSOLUTE, ""+absolute);
	e.setAttribute(ATTR.SKEW, ""+skew);


	//CLASS = "class", FEATURE = "feature", 

	return e;
    }

}

    