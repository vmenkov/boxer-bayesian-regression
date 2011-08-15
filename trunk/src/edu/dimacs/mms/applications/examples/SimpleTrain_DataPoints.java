package edu.dimacs.mms.applications.examples;


import java.util.*;
import java.io.*;

import org.w3c.dom.Element;

import edu.dimacs.mms.boxer.*;
import edu.dimacs.mms.boxer.util.*;


/**
   Here we show how to use BOXER with an application that, perhaps for
   efficiency reasons, wants to create BOXER DataPoint objects
   directly.

<ol>
<li>
            The training data in this version is assembled as a
            Vector of DataPoint objects, directly using operations
            from edu.dimacs.mms.boxer.DataPoint.  The resulting
            Vector of DataPoint objects is identical to that produced
            in SimpleTrain_BOXERfiles, SimpleTrain_BXRfiles,
            SimpleTrain_strings, and SimpleTrain_DOM.

	<li>
	We use BOXER method Learner.absorbExample() to train a
	Learner associated with the Suite (in this case a Suite
	containing only a single binary PLRM). 

	<li> Since there's no representation of a Suite as a Vector of
	DataPoints, we just use BOXER method
	Suite.serializeLearnerComplex() to write a serialized version
	of the trained Suite as a printed XML element to the specified
	output.
</ol>

<p>
       Usage:
<pre>
            java edu.dimacs.mms.applications.learning.SimpleTrain_DataPoints [OUTFILE]
</pre>
       where OUTFILE is optional.  If omitted the output is written
       to standard output, typically your screen.
*/

public class SimpleTrain_DataPoints {

    static void usage() {
	usage(null);
    }

    static private void usage(String m) {
	String name = (new SimpleTrain_DataPoints()).getClass().getName();
	System.out.println("Usage: java [options] "+name+" [input [output]]");
	if (m!=null) {
	    System.out.println(m);
	}
	System.exit(1);
    }

    private static Suite suite;
    
    static public void main(String argv[]) 
	throws IOException, org.xml.sax.SAXException, BoxerException {

	String outfile = (argv.length>0) ? argv[0] : null;
	if (argv.length > 1) usage("Too many arguments");
	
	suite = new Suite("A_default_suite");	    

	Vector<DataPoint> train = prepareDataSet();

	/** Add a simple TruncatedGradient learner. Normally, you'd
	    read the learner's specification from an XML file. */
	Learner algo = new TruncatedGradient(suite);
	algo.absorbExample(train);

	/** save the entire model */
	if (outfile!=null) {
	    suite.serializeLearnerComplex(outfile); 
	} else {
	    org.w3c.dom.Document doc = suite.serializeLearnerComplex();
	    XMLUtil.writeXML(doc, System.out);
	}
    }

    /** Calls DataPoint.makeDataPoint to create a DataPoint object from 
	a list of features and values
     */
    private static DataPoint makePoint( String name, String[][] pairs, String label) {
	HashMap <String, Double> words=new HashMap <String, Double>();
	// Here we must explicitly adding "dummy" (@constant) features, because
	// normally this is done during the parsing of the XML
	words.put(  FeatureDictionary.DUMMY_LABEL, new Double(1.0));
	for(String [] pair: pairs) {
	    words.put(pair[0], new Double( pair[1]));
	}
	DataPoint p=null;
	try {
	    p = DataPoint.makeDataPoint(words, suite.getDic(), name);
	} catch (BoxerXMLException ex) {}
	final String disName="Tiny1";
	Discrimination.Cla c = suite.getClaAlways(disName, label, true);
	p.addClass(c, true);
	return p; 
    }

    /** Prepares a data set, an equivalent of what's described as XML in
	 tiny1.train.boxer.xml
     */
    private static Vector<DataPoint> prepareDataSet()  {

	Vector<DataPoint> v=new	Vector<DataPoint>();
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
	return v;
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
