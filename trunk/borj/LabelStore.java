package borj;

import java.io.*; 
import java.util.*;
import org.apache.xerces.parsers.DOMParser;
import org.w3c.dom.*;
import org.xml.sax.SAXException;
import boxer.*;

/** Handles label files separate from the dataset files.

    Normally, BORJ/BOXER reads data points from a dataset files in
    which each data point element contains both the features of the
    data point (example), and labeles assigned to it. However, in some
    applications one may want to store labels separately. One
    situation when one may choose this approach is when there several
    versions of labels assignments to the same examples, and it is
    desirable not to duplicate data files needlessly.

    In this case, one's application can create a LabelStore instance,
    initialize it from reading labels from one or more separate files
    using the readXML method, and then apply these stored labels to
    data points as one reads them from feature-only files.

    The label files are XML files whose format is similar to that of
    data set files (see <a
    href="../boxer/doc-files/sample.xml">src/sample-data/sample.xml</a>); the
    difference between the two is that in label files the
    <tt>datapoint</tt> element contains no <tt>features</tt> element.

 */
public class LabelStore {

    /** Stores strings in disname:cname format */
    private HashMap<String, Vector<String>> table = 
	new HashMap<String, Vector<String>>();

    /** An auxiliary suite for storing labels from the separate label file(s) */
    //private Suite qrelSuite = new Suite("LabelStore_aux_suite");

    

    /** Reads all labels from an XML file, adding them to this LabelStore instance.
	@param fname The name of the file to read
     */
    public void readXML(String fname) throws IOException, SAXException, 
					     BoxerXMLException {
	if (!(new File(fname)).exists()) {
	    throw new IllegalArgumentException("Input file " + fname + 
					       " does not exist");
	}

	DOMParser parser = new DOMParser();
	parser.parse(fname);
	Document doc = parser.getDocument();
	
	Element e = doc.getDocumentElement();
	String name = e.getTagName();
	if (!name.equals(ParseXML.NODE.DATASET)) {
	    System.out.println("Warning: top-level document element is not " + 
			       ParseXML.NODE.DATASET + ", parsing anyway");
	}
	
	for(Node n = e.getFirstChild(); n!=null; n = n.getNextSibling()) {
	    int type = n.getNodeType();
	    String val = n.getNodeValue();
	    //System.out.println("Node Name  = " + n.getNodeName()+ 				   ", type=" + type + ", val= " + val);

	    if (type == Node.TEXT_NODE && val.trim().length()>0) {
		System.out.println("Warning: found an unexpected non-empty text node, val="  + val.trim());
	    } else if (type == Node.ELEMENT_NODE) {
		if (!n.getNodeName().equals(ParseXML.NODE.DATAPOINT)) {
		    System.out.println("Warning: element node name is not " + ParseXML.NODE.DATAPOINT + ", ignoring");		    
		} else {
		    // parse a data point (vector), and its labels
		    parseDataPoint((Element)n);
		}		
	    }
	}
    }

    /** Gets the class list (only) from a "datapoint" element of the
     * XML file, as if reading a QREL file. 
     */
    void parseDataPoint(Element node) throws BoxerXMLException {

	String dpName = node.getAttribute("name");
	if (dpName==null) throw new IllegalArgumentException("In an XML labels file, all data points must be named!");
	if (table.containsKey(dpName)) throw new IllegalArgumentException("Duplicate labels entry entry for data point named '" + dpName  + "'");


	Vector<String> clav =  new Vector<String>();
	for(Node n=node.getFirstChild(); n!=null; n=n.getNextSibling()) {
	    if (n.getNodeType() ==  Node.ELEMENT_NODE &&
		n.getNodeName().equals(ParseXML.NODE.LABELS)) {
		Vector<String[]> v=ParseXML.parseList(n,ParseXML.NODE.LABEL,
						      ParseXML.ATTR.LABEL_ATTR);
		for(String[] pair: v) {
		    //Discrimination.Cla c = 
		    //	qrelSuite.getClaAlways(pair[0], pair[1], Suite.ND.ADD,   Suite.NC.ADD);
		    // Efficient storage:
		    clav.add( (pair[0] + ":" + pair[1]).intern());
		}    
	    }
	}
	table.put(dpName, clav);
    }

  /** Goes through a vector of labels extracted from the labels file,
      and compiles the list of class labels for the data point
      with a particular name.

      Each stored label will be converted to a class entry with the
      real suite. An exception can be triggered (if the suite is set
      not to accept new labels), or some labels may be skipped (if the
      suite is set to ignore inconsistent classes)


      @param suite The "real" suite into which labels will be
      converted (from our internal storage)
     */
    Vector<Discrimination.Cla> getLabels(String dpName,
					 Suite suite,
					 boolean isTrain) {

	Vector<String> v=table.get( dpName);
	if (v==null) return null;
	Vector<Discrimination.Cla> w=new Vector<Discrimination.Cla>(v.size());

	for(String q: v) {
	    String[] pair = q.split(":");
	    Discrimination.Cla c1=suite.getClaAlways(pair[0], pair[1], isTrain);
	    //System.out.println("Converting " + q + " to " + c1);
	    if (c1 != null)     w.add(c1);
	}
	return w;
    }

    /** Searches this LabelStore for a list of labels associated with
	the names of the given data points, and, whenever found,
	replaces any labels already contained in the data point with
	these stored labels.

	@param v A Vector (Java array) of data points whose class
	label lists we want to update from this LabelStore

	@param suite The suite in whose context the data points will be used.

	@param isTrain Specifies if the data point in questioned are
	destined for use in a training set. If this is the case, and
	the suite's settings are appropriate, then new discriminations
	and/or classes may be created in the suite when new labels are
	encountered in the label store.
     */
    public void applyTo(Vector <DataPoint> v, Suite suite, boolean isTrain) {
	for(DataPoint p: v) applyTo(p, suite, isTrain);
    }

    /** Searches this LabelStore for a list of labels associated with
	the specified data point name, and, if found, replaces any labels
	already contained in the data point with these stored labels.
     */
    public  void applyTo(DataPoint p, Suite suite, boolean isTrain) {
	Vector<Discrimination.Cla> lab = getLabels(p.getName(), suite, isTrain);
	if (lab != null) p.setClasses(lab, suite);
    }


}
