package edu.dimacs.mms.applications.examples;


import java.util.*;
import java.io.*;

import org.w3c.dom.Element;
import org.w3c.dom.Document;
import org.apache.xerces.dom.DocumentImpl;

import edu.dimacs.mms.boxer.*;
import edu.dimacs.mms.boxer.util.*;


/**
   Here we show how to use BOXER with an application that
       is comfortable working with the org.w3c.dom packages.
<ol>
<li>

            The training data in this version is assembled as an object of
            class Element, using XML operations from the package
            org.w3c.dom.Element.  The resulting Element is identical
            to the one produced by SimpleTrain_BOXERfiles,
            SimpleTrain_BXRfiles, and SimpleTrain_strings.

	<li>
	We use BOXER method Learner.absorbExample() to train a
	Learner associated with the Suite (in this case a Suite
	containing only a single binary PLRM). 
  
	<li> We produce an XML element that describes the Suite and
	the learnr as a "learnercomplex" element.

        <li> We write render the element in XML to the specified output.
</ol>
<p>
       Usage:
<pre>
            java edu.dimacs.mms.applications.learning.SimpleTrain_DOM [OUTFILE]
</pre>
       where OUTFILE is optional.  If omitted the output is written
       to standard output, typically your screen.

*/

public class SimpleTrain_DOM {

    static void usage() {
	usage(null);
    }

    static private void usage(String m) {
	String name = (new SimpleTrain_DOM()).getClass().getName();
	System.out.println("Usage: java [options] "+name+" [input [output]]");
	if (m!=null) {
	    System.out.println(m);
	}
	System.exit(1);
    }

    /** Used for assembling the dataset element */
    private static Document xmldoc;

    static public void main(String argv[]) 
	throws IOException, org.xml.sax.SAXException, BoxerException {

	String outfile = (argv.length>0) ? argv[0] : null;
	if (argv.length > 1) usage("Too many arguments");
	
	Suite suite = new Suite("A_default_suite");	    

	/** Assembling a "dataset" element  */
	xmldoc= new DocumentImpl();
	Element e =  prepareDataSetElement();
	/** Parse the element int DataPoint objects */
	Vector<DataPoint> train = ParseXML.parseDatasetElement(e, suite, true);

	//DataPoint.saveAsXML(train, 0, train.size(), "dom", "train-out.dom.xml");

	/** Add a simple TruncatedGradient learner. Normally, you'd
	    read the learner's specification from an XML file. */
	Learner algo = new TruncatedGradient(suite);
	algo.absorbExample(train);

	// This element contains the "learnercomplex" element, which
	// includes the suite and the learner
	Element lcElement =suite.serializeLearnerComplex().getDocumentElement();

	/** save the entire model */
	if (outfile!=null) {
	    FileOutputStream os = new FileOutputStream(outfile);    
	    XMLUtil.writeXML(lcElement, os);
	    os.close();
	} else {
	    XMLUtil.writeXML(lcElement , System.out);
	}



    }


    private static void  addFeatureToElement(Element e, String pair[]) {
	 Element efeature = xmldoc.createElement( ParseXML.NODE.FEATURE);
	 efeature.setAttribute(ParseXML.ATTR.FEATURE_ATTR[0], pair[0]);
	 efeature.setAttribute(ParseXML.ATTR.FEATURE_ATTR[1], pair[1]);
	 e.appendChild(efeature);
	
    }
   
    /** Calls DataPoint.makeDataPoint to create a DataPoint object from 
	a list of features and values
     */
    private static Element makePoint( String name, String[][] pairs, String label) {
	final String disName="Tiny1";
	
	HashMap <String, Double> words=new HashMap <String, Double>();
	
	Element e = xmldoc.createElement( ParseXML.NODE.DATAPOINT);
	e.setAttribute(ParseXML.ATTR.NAME_ATTR, name);
	
	Element elabels = xmldoc.createElement( ParseXML.NODE.LABELS);
	e.appendChild(elabels);
	
	Element elabel = xmldoc.createElement( ParseXML.NODE.LABEL);
	elabels.appendChild(elabel);
	elabel.setAttribute(ParseXML.ATTR.LABEL_ATTR[0], disName);
	elabel.setAttribute(ParseXML.ATTR.LABEL_ATTR[1], label);
	
	Element ef = xmldoc.createElement( ParseXML.NODE.FEATURES);
	e.appendChild(ef);

	for(String [] pair: pairs) {
	    addFeatureToElement(ef, pair);
	}
	return e; 
    }
    
    /** Prepares a data set, an equivalent of what's described as XML in
	 tiny1.train.boxer.xml
     */
    private static Element prepareDataSetElement()  {

	Vector<Element> v=new	Vector<Element>();
	v.add(makePoint("TinyDoc1",
			new String [][] {},
			"WIDGET"));
	v.add(makePoint("TinyDoc2",
			new String [][] {{"Size","2.1"}},
			"GADGET"));
	v.add(makePoint("TinyDoc3",
			new String [][] {{"Size","3.1"},{"Heat","3.2"}},
			"WIDGET"));
	v.add(makePoint("TinyDoc4",
			new String [][] {{"Size","4.1"}},
			"GADGET"));
	v.add(makePoint("TinyDoc5",
			new String [][] {{"Size","5.1"},{"Heat","5.2"},{"Mass","5.3"}},
			"WIDGET"));
	v.add(makePoint("TinyDoc6",
			new String [][] {{"Size","6.1"}},
			"GADGET"));
	v.add(makePoint("TinyDoc7",
			new String [][] {{"Heat","7.2"},{"Mass","7.3"}},
			"WIDGET"));

	Element e = xmldoc.createElement( ParseXML.NODE.DATASET);
	e.setAttribute(ParseXML.ATTR.NAME_ATTR, "a_sample_dataset");
	e.setAttribute("version", Version.version);	

	for(Element p: v) {
	    e.appendChild(p);
	}
	return e;
    }
 


}


/*
Copyright 2011, Rutgers University, New Brunswick, NJ.

All Rights Reserved

Permission to use, copy, and modify this software and its
documentation for any purpose other than its incorporation into a
commercial product is hereby granted without fee, provided that the
above copyright notice appears in all copies and that both that
copyright notice and this permission notice appear in supporting
documentation, and that the names of Rutgers University, DIMACS, and
the authors not be used in advertising or publicity pertaining to
distribution of the software without specific, written prior
permission.

RUTGERS UNIVERSITY, DIMACS, AND THE AUTHORS DISCLAIM ALL WARRANTIES
WITH REGARD TO THIS SOFTWARE, INCLUDING ALL IMPLIED WARRANTIES OF
MERCHANTABILITY AND FITNESS FOR ANY PARTICULAR PURPOSE. IN NO EVENT
SHALL RUTGERS UNIVERSITY, DIMACS, OR THE AUTHORS BE LIABLE FOR ANY
SPECIAL, INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER
RESULTING FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF
CONTRACT, NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN
CONNECTION WITH THE USE OR PERFORMANCE OF THIS SOFTWARE.  */
