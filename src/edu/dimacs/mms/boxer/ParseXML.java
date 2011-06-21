package edu.dimacs.mms.boxer;

import java.io.*; 
import java.util.Vector;
import org.apache.xerces.parsers.DOMParser;
import org.w3c.dom.*;
import org.xml.sax.SAXException;

/** Class supporting conversion of one or more "examples" (training or
  test data points) from the external XML to DataPoints (our internal
  data structure for vectors).

  <p>
  A comprehensive example of the XML format for describing data points
  can be found in the file <a
  href="../../../../../sample.xml">src/sample-data/sample.xml</a>

  <p>
  See also: <a href="../../../../../nd.html">Treatment of new discrimination/class labels when parsing a data set</a>

 */
public class ParseXML {

    /** Used purely for testing */
    public static void main(String[] argv) throws IOException, SAXException, BoxerXMLException {
	if (argv.length != 1) {
	    throw new IllegalArgumentException("Usage: java ParseXML file");
	}
	Suite suite = new Suite("Test_suite");
	boolean isDefinitional = true;
	Vector<DataPoint> v = readDataFileXML(argv[0], suite, isDefinitional);
	System.out.println(suite.describe());
	System.out.println("Data set contains " + v.size() + " data points");
	for(int i =0; i<v.size(); i++) {
	    System.out.println(v.elementAt(i));
	}
    }

    /** XML tags */
    public static class NODE {
	public final static String DATASET = "dataset",
	    DATAPOINT = "datapoint",
	    LABELS = "labels", LABEL = "label",
	    FEATURES = "features",	    FEATURE = "feature";

    }

    /** XML atributes for various XML elements */
    public static class ATTR {
	public final static String LABEL_ATTR[] = {"dis", "class"};
	final static String FEATURE_ATTR[] = {"name", "value"};
	/** Used by many elements, includeing DataPoint and Learner */
	final static String NAME_ATTR = "name";
	/** For the Suite element */	
	final static String
	    VERSION_ATTR = "version";
	//SUITE_NAME_ATTR = "suitename",
	/** For the "suite" element */
	static class SUITE {
	    final static String 
		NC_TRAIN = "nctrain",
		NC_TEST = "nctest",
		ND_TRAIN = "ndtrain",
		ND_TEST = "ndtest",
		CREATE_ND_MODE = "CreateNewDiscriminationMode",
		SUPPORTS_SIMPLE_LABELS="SupportsSimpleLabels",
		HOW_TO_HANDLE_MISSING_IDS = "HowToHandleMissingIDs";	    
	}
	/** Attributes For the Discrimination element */
	static class DISCR {
	    final static String 
		CLASS_STRUCTURE = "classstructure",
		DEFAULT_CLASS = "defaultclass",
		LEFTOVERS_CLASS = "leftoversclass",
		CLASS_COUNT = "maxnumberofclasses",
	    //COMMITTED = "committed",
		QREL = "qrel";
	    /** value is "true" if this is the fallback discr of the suite */
	    final static String FALLBACK = "fallback";
	}
	/** Attributes specifically for the Learner element */
	static class LEARNER {
	    final static String ALGORITHM = "algorithm";	    
	}
    }


    /** Reads an entire file into an XML element. If one wants to use
     * the element later to parse as a dataset, the file must contain
     * a "dataset" element as its top-level element. 

     @throws IOException E.g., if the file can't be opened
     @throws SAXException If the W3C DOM parser finds that the
     document isn't good XML
    */
    public static Element readFileToElement(File f)
 	throws IOException, SAXException
    {
	//Logging.info("Parsing XML file " + f.getPath());
	return readFileToElement(f.getPath());
    }

    /** Reads an entire file into an XML element. If one wants to use
     * the element later to parse as a dataset, the file must contain
     * a "dataset" element as its top-level element.

     @throws IOException E.g., if the file can't be opened
     @throws SAXException If the W3C DOM parser finds that the
     document isn't good XML
 */
    public static Element readFileToElement(String fname) 
	throws IOException, SAXException {
	if (!(new File(fname)).exists()) {
	    throw new IllegalArgumentException("Input file " + fname + 
					       " does not exist");
	}
	DOMParser parser = new DOMParser();
	parser.parse(fname);
	Document doc = parser.getDocument();
	
	Element e = doc.getDocumentElement();
	return e;
    }


    /** Builds a vector of DataPoints out of the content of an XML file 
	@param fname The name of the file to read
	@param isDefinitional Set this flag to true if you're reading the training set. This will affect the way "new categories" encountered in the file are processed
     */
    public static Vector <DataPoint> readDataFileXML(String fname,    
						     Suite suite, 
						     boolean isDefinitional)
	throws IOException, SAXException, BoxerXMLException {

	Element e=readFileToElement(fname);
	return parseDatasetElement(e, suite, isDefinitional);

    }

    /** Reads a file containing a single DataPoint element as a top
     * element. (This is not a common format for BOXER, but is used in some applications)
     */
    public static DataPoint readSingleDataPointFileXML(String fname,    
						       Suite suite,
						       boolean isDefinitional) 
	throws IOException, SAXException, BoxerXMLException {

	Element e=readFileToElement(fname);
	DataPoint p = parseDataPoint(e,  suite, isDefinitional); 
	return p;
    }


    /** Builds a vector of DataPoints out of the content of a data
	file in XML or BMR format. It tries to determine the file type
	from the extension of the file name, ".xml" being XML and
	everything else being BMR. This is basically a wrapper around
	{@link #readDataFileXML( String, Suite, boolean)} and 
	{@link BXRReader#readDataFileBMR( String, Suite, boolean)} 
     */
    public static Vector <DataPoint> readDataFileMultiformat(String fname,    
						     Suite suite, 
						     boolean isDefinitional)
	throws IOException, SAXException, BoxerXMLException {
	if (fname.toLowerCase().endsWith(".xml")) {
	    return readDataFileXML(fname, suite,  isDefinitional);
	} else {
	    return BXRReader.readDataFileBMR(fname, suite,  isDefinitional);
	}
    }

    /** Builds a vector of DataPoints out of the content of an XML
      element (which, typically, is the top-level element of an XML
      file)
	@param e The XML element (named "dataset") to be parsed
	@param isDefinitional Set this flag to true if you're reading the
	training set. This will affect the way "new discriminations"
	and "new classes" encountered in the file are processed
     */
    public static Vector<DataPoint> parseDatasetElement(Element e,
							Suite suite, 
							boolean isDefinitional) 
	throws BoxerXMLException    {

	Vector<DataPoint> v = new Vector<DataPoint>();

	XMLUtil.assertName(e, NODE.DATASET);

	// not actually used
	String dataSetName = e.getAttribute(ParseXML.ATTR.NAME_ATTR);
	if (!IDValidation.validateBasic(dataSetName)) {
	    throw new BoxerXMLException("The data set name '"+dataSetName+"' contained in the XML suite definition is invalid");
	}
	Logging.info("Parsing data set named "  +  dataSetName);
	
	for(Node n = e.getFirstChild(); n!=null; n = n.getNextSibling()) {
	    int type = n.getNodeType();
	    String val = n.getNodeValue();
	    
	    if (type == Node.TEXT_NODE && val.trim().length()>0) {
		Logging.warning("Found an unexpected non-empty text node, val="  + val.trim());
	    } else if (type == Node.ELEMENT_NODE) {
		if (!n.getNodeName().equals(NODE.DATAPOINT)) {
		    Logging.warning("Element node name is not " + NODE.DATAPOINT + ", ignoring");		    
		} else {
		    // parse a data point (vector), and its labels
		    DataPoint p = parseDataPoint((Element)n,  suite, isDefinitional); 
		    v.add(p);
		}
	    }		
	}
	return v;
    }

    /** Creates a DataPoint from the data in the specified
     <tt>datapoint</tt> XML element.
     @param node A <tt>datapoint</tt> XML element to parse
     @throws BoxerXMLException If the element does not contain
     everything a <tt>datapoint</tt> element needs. */
    public static DataPoint parseDataPoint(Element node,  
				    Suite suite, boolean isDefinitional)
	throws BoxerXMLException {
	Vector<DataPoint.FVPair> fv = null;
	String dpName= validateDataPointName(node.getAttribute(ATTR.NAME_ATTR), suite);

	Vector<Discrimination.Cla> clav = null;

	for(Node n=node.getFirstChild(); n!=null; n=n.getNextSibling()) {
	    if (n.getNodeType() ==  Node.ELEMENT_NODE) {
		String name = n.getNodeName();
		if (name.equals(NODE.LABELS)) {
		    Vector<String[]> v=parseList(n,NODE.LABEL, ATTR.LABEL_ATTR);

		    if (clav != null && v.size() > 0) {
			throw new BoxerXMLException("Duplicate non-empty " + NODE.LABELS + " tag in for the data point '"+dpName+"'. Was there a separate labels (QREL) file?");
		    }

		    clav = new Vector<Discrimination.Cla>();
		    for(int i=0; i<v.size(); i++) {
			String [] pair = v.elementAt(i);
			Discrimination.Cla c = suite.getClaAlways(pair[0], pair[1], isDefinitional);
			if (c!=null) {
			    clav.add(c);
			    if (isDefinitional && suite.isFallback(c.getDisc())) {
			    //!c.isDefault()
			    // Fallback discr is meant to ony receive default
			    // updates
				throw new  BoxerXMLException("Data point '"+dpName+"' contains an explicit class label "+c+" for the fallback discrimination, which is prohibited");
			    }
			}
		    }
		} else if  (name.equals(NODE.FEATURES)) {
		    if (fv != null) {
			throw new BoxerXMLException("Duplicate " + NODE.FEATURES + " tag in data point '"+dpName+"'!");
		    }
		    Vector<String[]> v=parseList(n,NODE.FEATURE,
						 ATTR.FEATURE_ATTR);
		    fv = new Vector<DataPoint.FVPair>();
		    // FIXME: is there a better place to set the dummy coeff?
		    // Besides, one should not set this feature (or should
		    // modify it later) if the input DOES contain the dummy
		    // feature already (which is not normally done now, but 
		    // could be imagined in some application)
		    suite.getDic().addDummyCompoIfRequired(fv);

		    for(int i=0; i<v.size(); i++) {
			String [] pair = v.elementAt(i);
			// Are we reading feature IDs, or do we need to map labels to IDs?
			int fid = (suite.getDic() == null)? Integer.parseInt(pair[0]) :
			    suite.getDic().getIdAlways(pair[0]);
			double fval = Double.parseDouble(pair[1]);
			fv.add(new DataPoint.FVPair(fid, fval));
		    }
		}    
		    
	    }
	}
	if (fv == null || fv.size() == 0) {
	    throw new BoxerXMLException("Data point '"+dpName+"' has no features");
	} 
	DataPoint p = new  DataPoint(fv, suite.getDic(), dpName);

	if (clav== null) clav = new Vector<Discrimination.Cla>();
	p.setClasses(clav, suite);

	return p;
    }


    /** An auxiliary method, parsing a node which is supposed to
	contain a number of children nodes named pairName, with
	attributes named aname[0] and aname[1]. It is made public so
	that it can be reused in some XML processing classes outside
	of Boxer.
    */
    public static Vector<String[]> parseList(Node node, String pairName, 
				      String[] aname) 
	throws BoxerXMLException {
	Vector<String[]> v = 	new Vector<String[]>();
	for(Node x=node.getFirstChild(); x!=null; x=x.getNextSibling()) {
	    int type = x.getNodeType();
	    if (type ==Node.ELEMENT_NODE && x.getNodeName().equals(pairName)) {
		// Extracting attributes from <label dis="..." class="....">
		Element e = (Element)x;
		String c[] = new String[2];
		for(int i=0; i<2; i++) {
		    c[i] = e.getAttribute(aname[i]);
		    if (!XMLUtil.nonempty(c[i])) {
			throw new BoxerXMLException("Missing attribute " +aname[i] +
					      " in a "+pairName+" tag");
		    }
		}
		v.add(c);
	    } else if (	type == Node.TEXT_NODE) {
		// Parsing the text of the node
		String s = x.getNodeValue().trim();
		if (s.length()>0) {
		    v = BXRReader.readPairs(s,v);
 		}
	    } else if (	type == Node.COMMENT_NODE) {
		// ignore
	    } else {
		throw new BoxerXMLException("Unexpected element inside a " + 
				      pairName+" node: " + x);		
	    }
	}
	return v;
    }


    /** Creates a more or less unique name for a data point, if none
	has been supplied in the input file.
     */
    private static String validateDataPointName(String dpName, Suite suite) 
    throws BoxerXMLException {
	if (dpName==null) dpName = "";
	if (dpName.equals("")) {
	    if (suite.howToHandleMissingIDs== Suite.HowToHandleMissingIDs.Error) {
		throw new  BoxerXMLException("XML definition of a data point has no name. In this mode, all data points are supposed to have one");
	    } else { // Create
		dpName = DataPoint.autoGeneratedName();
	    }
	}
	return dpName;
    }


    /** This methods allows one to check in advance whether a given
	"dataset" XML element can be successfully "ingested" by BOXER.
	On success (i.e., if the data can be read in without an
	exception being thrown due to e.g. a syntax error, or
	impossibility to add a necessary number of new classes to an
	existing disicrmination), this method returns the number of
	data points found in the data set by parsing. Otherwise, -1 is
	returned, and the exception message is logged as a warning to (via
	java.util.logging.Loggger).

	Regardless of whether the data set is parsed successfully or
	not, this method does not affect the suite passed to it (other
	than, perhaps, adding terms to its feature dictionary, which
	has no effect on the end user).

	@param e The XML element (named "dataset") to be parsed. This,
	typically, is the top-level element of an XML file.

	@param suite Parsing will be done in the context of a specially-prepared copy of this suite. The suite itself will not be affected.

	@param isDefinitional Set this flag to true if you want to validate
	the data set for future reading as a training set. This will
	affect the way "new discriminations" and "new classes"
	encountered in the file are processed
     */
    public static int validateDatasetElement(Element e,
					     Suite suite, 
					     boolean isDefinitional) 
    //throws  SAXException
    {
	Suite validator = suite.lightweightCopyOf("Validator");
	int n = -1;
	try {
	    Vector<DataPoint> v = parseDatasetElement(e, validator, isDefinitional);
	    n = v.size();
	} catch (Exception ex) {
	    Logging.warning("It would not be possible to parse the entire data set in the context of suite '"+suite.getName()+"' , because of the following problem: "  + ex);
	}
	return n;

    }

    /** This methods allows one to check in advance whether a given
	<tt>dataset</tt> XML element can be successfully "ingested" by BOXER.
	Unlike {@link #validateDatasetElement(Element, Suite, boolean)
	validateDatasetElement(Element, Suite, boolean)}, this method
	does not stop after the first error. Instead, it keeps
	processing the 	<tt>dataset</tt> element no matter what, and
	returns a Vector that contains a mix of DataPoint objects (for
	successfully parsed <tt>datapoint</tt> elements) and Exceptions (for 
	<tt>datapoint</tt> elements that could not be properly parsed and
	processed within the context of the specified suite.

	<p> To understand how successful the processing has been, the
	caller would need to scan the entire results array, and to see
	how many Exception objects it contains.

	Regardless of how successfully the data set is parsed, this
	method does not affect the suite passed to it (other than,
	perhaps, adding terms to its feature dictionary, which has no
	effect on the end user).

	@param e The XML element (named <tt>dataset</tt>) to be parsed. This,
	typically, is the top-level element of an XML file.

	@param originalSuite Parsing will be done in the context of a specially-prepared copy this suite. The suite itself will not be affected.

	@param isDefinitional Set this flag to true if you want to validate
	the data set for future reading as a training set. This will
	affect the way "new discriminations" and "new classes"
	encountered in the file are processed

	@return A mixed Vector of DataPoint and Exception objects,
	normally - one for each <tt>datapoint</tt> child element
	within the <tt>dataset</tt> element being parsed.

	//@throws SAXException Is thrown by W3C DOM parser when XML just can't be parsed

     */
    public Vector<Object> validateDatasetElement2(Element e,
					     Suite originalSuite, 
					     boolean isDefinitional) 
	throws BoxerXMLException
    //throws SAXException
    {
	Suite validator = originalSuite.lightweightCopyOf("Validator");
	Vector<Object> v = new 	Vector<Object>();

	XMLUtil.assertName(e, NODE.DATASET);
	
	for(Node n = e.getFirstChild(); n!=null; n = n.getNextSibling()) {
	    int type = n.getNodeType();
	    String val = n.getNodeValue();
	    
	    if (type == Node.TEXT_NODE && val.trim().length()>0) {
		Logging.warning("Found an unexpected non-empty text node, val="  + val.trim());
	    } else if (type == Node.ELEMENT_NODE) {
		if (!n.getNodeName().equals(NODE.DATAPOINT)) {
		    Logging.warning("Element node name is not " + NODE.DATAPOINT + ", ignoring");		    
		} else {
		    // parse a data point (vector), and its labels
		    try {
			DataPoint p=parseDataPoint((Element)n, validator, isDefinitional); 
			v.add(p);
			//} catch(SAXException ex) {
			// This exception - implying poor XML - we re-throw,
			// as per DL's writeup (2009-06-24)
			//throw ex;
		    } catch(Exception ex) {
			v.add(ex);
		    }
		}
	    }		
	}
	return v;
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

