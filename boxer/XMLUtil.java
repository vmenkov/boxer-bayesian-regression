package boxer;

import java.util.Vector;
import java.io.*;

// for XML output
import org.w3c.dom.*;
import org.apache.xerces.dom.DocumentImpl;
import org.apache.xml.serialize.*;

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
     
     @throws An exception if the Node is not an Element node at all,
     or if the name of the element is * not what's expected
     */
    static public void assertName(Node /*Element*/ e, String expectedName) {
	String name = ((Element)e).getTagName();
	if (!name.equals(expectedName)) 
	    throw new IllegalArgumentException("Found element is '"+ name +"' where '" + expectedName + "' was expected" );	
    }

    static String getAttributeOrException(Element e, String aname) {
	String a = e.getAttribute(aname);
	if (!XMLUtil.nonempty(a)) throw new IllegalArgumentException("There is no '"+aname+"' attribute in the XML element '"+e.getTagName()+"'");
	return a;
    }


}