/**   This application demonstrates using BOXER with training data
      supplied as XML in the form of a standard Java string.  The
      trained model is also produced as a standard Java string
      containing the XML for a learner complex in standard BOXER
      format.  That string is then written to a file, but one could
      imagine passing it to some other application.

      The XML stored in the string is the same XML that appears
      in the sample file        "tiny1.train.boxer.xml" that is the default argument to  SimpleTrain_BOXERfiles. 

<p>
       Usage:
<pre>
            java edu.dimacs.mms.applications.examples.SimpleTrain_strings [OUTFILE]
</pre>
       where OUTFILE is optional.  

       If OUTFILE is present, the XML representation of the Suite
       (which includes the Discrimination definition and the trained
       model for that Discrimination) is written to that file.
       Otherwise it is written to standard output (usually the
       screen).
*/


/* The first statement in a Java file is the package statement.  The
one above declares that the code in this file is part of the package
edu.dimacs.mms.applications.examples. */ 
package edu.dimacs.mms.applications.examples;

import java.util.*;
import java.io.*;

import org.w3c.dom.Element;

import edu.dimacs.mms.boxer.*;
import edu.dimacs.mms.boxer.util.*;


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


    /* *********************  START OF main() *******************************/

    /* main() does the work of this class.  It is the method executed
    when you run 
             java SimpleTrain_DOM
    from the command line. */

    static public void main(String argv[]) 
	throws IOException, org.xml.sax.SAXException, BoxerException {

	String outfile = (argv.length>0) ? argv[0] : null;
	if (argv.length > 1) usage("Too many arguments");

        /* A Suite in BOXER contains a set of Discriminations and a
	   set of Learners that can produce predictive models for
	   those Discriminations.  Here we create a new Suite with
	   default characteristics and give it the name "demo_suite". */ 
	Suite my_suite = new Suite("demo_suite");	    

	/* We convert the XML in the string into an object that
	   implements the Element interface from the org.w3c.dom
	   package */ 
	Element e = ParseXML.parseStringToElement(xmlText);

        /*  We use BOXER method ParseXML.parseDatasetElement to parse
            the Element we just created and convert the training
            examples in it to a vector of DataPoint objects.  The
            final argument 'true' indicates that the data should be
            treated as *definitional*, i.e. that discrimination and
            class names encountered in the data should be added to
            my_suite. */
 	Vector<DataPoint> parsed_data = ParseXML.parseDatasetElement(e, my_suite, true);

	/* Add a simple TruncatedGradient learner with default
	   properties to my_suite.  In real applications it's more
	   common to initialize the learner with specifications read
	   from an XML file */ 
	Learner my_learner = new TruncatedGradient(my_suite);


        /* The BOXER method Learner.absorbExample makes one pass of
	   online learning over all the labeled examples, in the order
	   they appear in parsed_data.  

	   Note that each class label within BOXER format data (as
	   provided in the string in this case) specifies both the
	   class the example belongs to, and the discrimination that
	   that class is drawn from. So even if the suite included
	   multiple Discriminations, there would be no ambiguity about
	   which Discriminations are updated for each training
	   example. */ 
	my_learner.absorbExample(parsed_data);

        /* DDL : i've written vladimir about creating a string first */ 
	if (outfile != null) {
	    /** Write the LearnerComplex in XML form to the specified file. */
	    my_suite.serializeLearnerComplex(outfile); 
	} 
	else {
	    /** Convert the LearnerComplex to an XML document, and write it to standard output */ 
	    org.w3c.dom.Document doc = my_suite.serializeLearnerComplex();
	    XMLUtil.writeXML(doc, System.out);
	}
    }
    /* *********************  END OF main() *******************************/



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
Copyright 2011, Rutgers University, New Brunswick, NJ, and David D. Lewis, 
David D. Lewis Consulting, Chicago, IL. 

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
