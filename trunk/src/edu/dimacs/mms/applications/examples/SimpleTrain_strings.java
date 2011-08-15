package edu.dimacs.mms.applications.examples;


import java.util.*;
import java.io.*;

import org.w3c.dom.Element;

import edu.dimacs.mms.boxer.*;
import edu.dimacs.mms.boxer.util.*;


/**
      This application demonstrates using BOXER with flat files in
       standard BOXER formats.  We do the following:

       <ol>
            
       <li> The training data in this version is represented as the
        string of characters for the equivalent XML, and created by
        concatenating several objects of class java.lang.string.  The
        string contains the same XML as occurs in the file
        "tiny1.train.boxer.xml" used in SimpleTrain_BOXERfiles.  We
        use BOXER methods ParseXML.parseStringToElement() and
        ParseXML.parseDatasetElement() to convert the string into a
        vector of DataPoint objects.

	<li>
	We use BOXER method Learner.absorbExample() to train a
	Learner associated with the Suite (in this case a Suite
	containing only a single binary PLRM). 
	
	<li>
	We use BOXER method Suite.serializeLearnerComplex() to
	write a serialized version of the trained Suite as a
	printed XML element to the specified output.

	</ol>

	<p>
       Usage:
<pre>
            java edu.dimacs.mms.applications.examples.SimpleTrain_strings [OUTFILE]
</pre>
       where OUTFILE is optional.  If omitted the output is written
       to standard output, typically your screen.
*/

public class SimpleTrain_strings {

    static void usage() {
	usage(null);
    }

    static private void usage(String m) {
	String name = (new SimpleTrain_strings()).getClass().getName();
	System.out.println("Usage: java [options] "+name+" [input [output]]");
	if (m!=null) {
	    System.out.println(m);
	}
	System.exit(1);
    }

    static public void main(String argv[]) 
	throws IOException, org.xml.sax.SAXException, BoxerException {

	String outfile = (argv.length>0) ? argv[0] : null;
	if (argv.length > 1) usage("Too many arguments");
	
	Suite suite = new Suite("A_default_suite");	    

	/** Reading training data from a string */
	Element e = ParseXML.parseStringToElement( xmlText);
	Vector<DataPoint> train = ParseXML.parseDatasetElement(e, suite, true);

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

    /** An XML document (complete with comments!) as a String */
    static final String xmlText = 
"<?xml version=\"1.0\" encoding=\"UTF-8\"?>"+

"<!-- This is a labeled data file in the format used by "+
"BOXER (http://code.google.com/p/boxer-bayesian-regression/).  "+

"The file contains 7 training examples for a single 2-class discrimination, "+
"Tiny1, where the two classes are WIDGET and GADGET.  Each example is represented "+
"by the value of 3 features, with feature identifiers Size, Heat, Mass. "+
"If an example does not have a (featID, featvalue pair) for a particular"+
"feature ID, the value of that feature for that example should be assumed"+
"to be 0.  Note in particular that the first example has no "+
"(featID, featvalue pairs), and thus is assumed to have a value of "+
"0 for all features.  "+

"To aid the use of this file in debugging, we encode the example"+
"ID and the feature ID into the feature value as well, when the "+
"feature has a nonzero value for an example.  "+

"This file encodes the same set of examples as tiny1.train.bbrdata"+
"(but uses symbolic class and feature IDs), and"+
"tiny.train.bxrdata. "+
"-->"+

"<dataset name=\"tiny1dataset\" boxerversion=\"0.1\">"+

"<datapoint name=\"TinyDoc1\">"+
"  <labels>"+
"    <label dis=\"Tiny1\" class=\"WIDGET\"/>"+
"  </labels>"+
"  <features>"+
"  </features>"+
"</datapoint>"+

"<datapoint name=\"TinyDoc2\">"+
"  <labels>"+
"    <label dis=\"Tiny1\" class=\"GADGET\"/>"+
"  </labels>"+
"  <features>"+
"      <feature name=\"Size\" value=\"2.1\"/> "+
"  </features>"+
"</datapoint>"+


"<datapoint name=\"TinyDoc3\">"+
"  <labels>"+
"    <label dis=\"Tiny1\" class=\"WIDGET\"/>"+
"  </labels>"+
"  <features>"+
"      <feature name=\"Size\" value=\"3.1\"/> "+
"      <feature name=\"Heat\" value=\"3.2\"/> "+
"  </features>"+
"</datapoint>"+

"<datapoint name=\"TinyDoc4\">"+
"  <labels>"+
"    <label dis=\"Tiny1\" class=\"GADGET\"/>"+
"  </labels>"+
"  <features>"+
"      <feature name=\"Size\" value=\"4.1\"/> "+
"  </features>"+
"</datapoint>"+


"<datapoint name=\"TinyDoc5\">"+
"  <labels>"+
"    <label dis=\"Tiny1\" class=\"WIDGET\"/>"+
"  </labels>"+
"  <features>"+
"      <feature name=\"Size\" value=\"5.1\"/> "+
"      <feature name=\"Heat\" value=\"5.2\"/> "+
"      <feature name=\"Mass\" value=\"5.3\"/> "+
"  </features>"+
"</datapoint>"+

"<datapoint name=\"TinyDoc6\">"+
"  <labels>"+
"    <label dis=\"Tiny1\" class=\"GADGET\"/>"+
"  </labels>"+
"  <features>"+
"      <feature name=\"Size\" value=\"6.1\"/> "+
"  </features>"+
"</datapoint>"+

"<datapoint name=\"TinyDoc7\">"+
"  <labels>"+
"    <label dis=\"Tiny1\" class=\"WIDGET\"/>"+
"  </labels>"+
"  <features>"+
"      <feature name=\"Heat\" value=\"7.2\"/> "+
"      <feature name=\"Mass\" value=\"7.3\"/> "+
"  </features>"+
"</datapoint>"+

	"</dataset>" + 
"\n";



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
