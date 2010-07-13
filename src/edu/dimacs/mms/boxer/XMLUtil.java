package edu.dimacs.mms.boxer;

import java.io.FileOutputStream;
import java.io.IOException;

import org.apache.xml.serialize.OutputFormat;
import org.apache.xml.serialize.XMLSerializer;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/** Constants and auxiliary classes for producing and parsing XML files 
 */
public class XMLUtil {

    /** Names of various elements */
    static public final String 
	SUITE = "suite",//top-level, or nested in LEARNER_COMPLEX
	LEARNER_COMPLEX = "learnercomplex",  // top-level
	LEARNERS= "learners", // nested in LEARNER_COMPLEX
	LEARNER= "learner",  // nested in LEARNERS
	CLASSIFIER = "classifier",  // nested in LEARNER
	DISCRIMINATION="discrimination", // nested in SUITE
	CLASSES="classes",
	MATRIX = "matrix", ROW="row", FEATURE="feature";
    
    /** Returns true if x is neither a null nor an empty string (nor a
     * string of all blanks) */
    static boolean nonempty(String x) { 
	return x!=null && x.trim().length()>0;
    }

    /** An auxiliary method for Learner.saveAsXML() etc;
     * saves a prepared XML doc into a file */
    public static void writeXML(Document xmldoc, String fname) {
	try {
	    FileOutputStream fos = new FileOutputStream(fname);
	    OutputFormat of = new OutputFormat("XML","utf-8",true);
	    of.setIndent(1);
	    of.setIndenting(true);
	    //of.setDoctype(null,"bxr-eg.dtd");
	    XMLSerializer serializer = new XMLSerializer(fos,of);
	    // As a DOM Serializer
	    serializer.asDOMSerializer();
	    serializer.serialize( xmldoc.getDocumentElement() );
	    fos.close();
	} catch (IOException ex) {
	    System.err.println("Exception when trying to write XML file " + fname + "\n" + ex);
	}
    }

    /** Verifies that the specified XML Node is indeed an element with
     a particular name
     
     @throws  BoxerXMLException if the Node is not an Element node at all,
     or if the name of the element is * not what's expected
     */
    static public void assertName(Node /*Element*/ e, String expectedName) 
	throws BoxerXMLException {
	if (!(e instanceof Element))  throw new BoxerXMLException("Found a node of a non-element type where element was expected: " + e);
	String name = ((Element)e).getTagName();
	if (!name.equals(expectedName)) 
	    throw new BoxerXMLException("Found element is '"+ name +"' where '" + expectedName + "' was expected" );	
    }

    static public boolean isNamedElement(Node /*Element*/ e, String expectedName) 
	throws BoxerXMLException {
	if (!(e instanceof Element)) return false;	    
	String name = ((Element)e).getTagName();
	return  (name.equals(expectedName)) ;
    }

    /** Can this node be just ignored? Empty text nodes can.
     */
    static public boolean isIgnorable(Node n) {
	int type= n.getNodeType();
	String val= n.getNodeValue();

	return (type == Node.TEXT_NODE && val.trim().length()==0) ||
	    (type== Node.COMMENT_NODE);
    }


    /** Gets an attribute, as a string. If absent, throws an exception */
    static String getAttributeOrException(Element e, String aname) throws BoxerXMLException {
	String a = e.getAttribute(aname);
	if (!XMLUtil.nonempty(a)) throw new BoxerXMLException("There is no '"+aname+"' attribute in the XML element '"+e.getTagName()+"'");
	return a;
    }

    /** Gets an attribute as a string value. If absent,
      returns the specified default value. */
    static String getAttributeString(Element e, String aname, String defValue) throws BoxerXMLException {
	String a = e.getAttribute(aname);
	return  (XMLUtil.nonempty(a)) ?  a: defValue;
    }


    /** Gets an attribute, and converts it to an int value. If absent,
      throws an exception */
    static int getAttributeInt(Element e, String aname) throws BoxerXMLException {
	return  Integer.parseInt( getAttributeOrException(e,aname));
    }

    /** Gets an attribute, and converts it to an int value. If absent,
      returns the specified default value. */
    static int getAttributeInt(Element e, String aname, int defValue) throws BoxerXMLException {
	String a = e.getAttribute(aname);
	return  (XMLUtil.nonempty(a)) ?  Integer.parseInt(a) : defValue;
    }

    /** Gets an attribute, and converts it to a double value. 
	The value "Infinity" or "INF" is converted to Double.POSITIVE_INFINITY (the former, by Double.valueOf(), the latter by our own rule). 
	@throws BoxerXMLException If no attribute with the expected name is found.
    */
    static double getAttributeDouble(Element e, String aname) throws BoxerXMLException {
	String a = getAttributeOrException(e,aname);
	return  a.equalsIgnoreCase(Param.INF.toString())  ?
	    Double.POSITIVE_INFINITY :	    Double.parseDouble(a);
    }

    /** Gets an attribute, and converts it to a double value. 
	The value "Infinity" or "INF" is converted to Double.POSITIVE_INFINITY (the former, by Double.valueOf(), the latter by our own rule). 
	If absent,  returns the specified default value. */
    static double getAttributeDouble(Element e, String aname, double defValue) throws BoxerXMLException {
	String a = e.getAttribute(aname);
	return  !XMLUtil.nonempty(a) ?   defValue :
	    a.equals(Param.INF.toString()) ?	 Double.POSITIVE_INFINITY :
	    Double.parseDouble(a);
    }

  /** Gets an attribute, and converts it to a boolean value. (The
      expected raw value of the attribute is "true" or "false".)
      If absent, throws an      exception */
    static boolean getAttributeBoolean(Element e, String aname) throws BoxerXMLException {
	return  Boolean.parseBoolean( getAttributeOrException(e,aname));
    }

  /** Gets an attribute, and converts it to a boolean value. (The
      expected raw value of the attribute is "true" or "false".)
      If absent,  returns the specified default value. */
    static boolean getAttributeBoolean(Element e, String aname, boolean defValue) throws BoxerXMLException {
	String a = e.getAttribute(aname);
	return  (XMLUtil.nonempty(a)) ?  Boolean.parseBoolean(a) : defValue;
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