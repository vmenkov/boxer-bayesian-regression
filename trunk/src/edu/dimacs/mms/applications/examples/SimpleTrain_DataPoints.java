/* The first statement in a Java file is the package statement.  The
one above declares that the code in this file is part of the package
edu.dimacs.mms.applications.examples. */ 
package edu.dimacs.mms.applications.examples;

import java.util.*;
import java.io.*;

import edu.dimacs.mms.boxer.*;
import edu.dimacs.mms.boxer.util.*;


/**

   This application demonstrates how BOXER can process training data
   that it receives in the form of a Vector of BOXER DataPoint
   objects.  

   BOXER is designed to input labeled and unlabeled records in XML and
   other convenient formats.  Internally it converts a set of examples
   in any format to a Vector of DataPoint objects, which are more
   efficient to work with.  For efficiency, an application using BOXER
   can also provide data to BOXER in the form of DataPoint objects
   directly.  The downside is that, since DataPoint objects are
   closely linked to the Suite with which they will be used, this
   approach ties the preparation of examples more closely with the
   details of BOXER's internal processing than preparing examples in
   XML would.

   The training data in this application is assembled as a Vector of
   DataPoint objects, directly using operations from
   edu.dimacs.mms.boxer.DataPoint.  The resulting Vector of DataPoint
   objects is identical to Vector of DataPoint objects produced when

              1) the sample data file tiny1.train.boxer.xml is used
      with the application SimpleTrain_BOXERfiles, 

              2) the sample data file tiny1.train.bxrdata is used with
      the application SimpleTrain_BXRfiles, or 

              3) when the application SimpleTrain_DOM is run. 

              4) when the application SimpleTrain_strings is run.

<p>
       Usage:
<pre>
            java edu.dimacs.mms.applications.learning.SimpleTrain_DataPoints [OUTFILE]
</pre>
       where OUTFILE is optional.  

       If OUTFILE is present, the XML representation of the Suite
       (which includes the Discrimination definition and the trained
       model for that Discrimination) is written to that file.
       Otherwise it is written to standard output (usually the
       screen).
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

    /* In contrast to the other SimpleTrain_* applications, this one
       declares my_suite outside of main().  DataPoint objects can
       only be created in the context of the Suite with which they
       will be used, so that Suite must be available to the code that
       creates DataPoint objects. */ 
    private static Suite my_suite;


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
	   default characteristics and give it the name "demo_suite". 
	   Note that we defined the my_suite variable outside of main(). */ 
	my_suite = new Suite("demo_suite");	    

	/* See prepareDataSet() below. */
	Vector<DataPoint> prepared_data = prepareDataSet();

	/* Add a simple TruncatedGradient learner with default
	   properties to my_suite.  In real applications it's more
	   common to initialize the learner with specifications read
	   from an XML file */ 
	Learner my_learner = new TruncatedGradient(my_suite);


        /* The BOXER method Learner.absorbExample makes one pass of
	   online learning over all the labeled examples, in the order
	   they appear in parsed_data.  

	   Note that each class label within a BOXER format labeled
	   data file specifies both the class the example belongs to,
	   and the discrimination that that class is drawn from, so
	   there would be no ambiguity about which Discriminations are
	   updated for each training example, even if the suite
	   included multiple Discriminations. */ 
	my_learner.absorbExample(prepared_data);



	/* We write the Suite and the state of the associated Learner
	    in BOXER's XML format. If an output file is specified, we
	    use the BOXER method serializeLearnerComplex to write that
	    information directly to a file.  Otherwise we used the
	    BOXER method serializeLearnerComplex to serialize to an
	    in-memory XML document, and dump that document to standard
	    output (usually the screen) using BOXER method
	    writeXML. */ 
	if (outfile != null) {
	    my_suite.serializeLearnerComplex(outfile); 
	} 
	else {
	    org.w3c.dom.Document doc = my_suite.serializeLearnerComplex();
	    XMLUtil.writeXML(doc, System.out);
	}
    }
    /* *********************  END OF main() *******************************/


    /* This method calls DataPoint.makeDataPoint to create a single
       DataPoint object for a record.  It is supplied with an ID for
       the record, a set of feature ID/value pairs, and the
       discrimination name and class name for a label for the
       record. */
    private static DataPoint makePoint(String record_id, String[][] pairs, String disName, String className) {

	HashMap <String, Double> pairs_table = new HashMap <String, Double>();

        /* The feature/value pairs were provided to us as an array of
	   (length 2) arrays of strings. We convert to a HashMap where
	   the string for the feature id is paired with the Double for
	   the feature value. */ 
       	for (String [] pair: pairs) {
	    pairs_table.put(pair[0], new Double( pair[1]));
	}

        /* We add to the feature/value pairs for the vector a pairing
	   for "dummy" feature (named @constant) with the value 1.
	   The dummy feature corresponds to the constant term (aka
	   "bias term") of the model.  Normally this is added during
	   the parsing of the XML representation of a record. */ 
	pairs_table.put(FeatureDictionary.DUMMY_LABEL, new Double(1.0));

        /* Create the DataPoint for this record. Note that any previously unseeen
	   feature IDs are added to the feature ID dictionary in the Suite */ 
	DataPoint p=null;
	try {
	    p = DataPoint.makeDataPoint(pairs_table, my_suite.getDic(), record_id);
	} 
	catch (BoxerXMLException ex) {}

	/* Get the Discrimination.Cla (class) object corresponding to
	   the discrimination name and class name we were given.  The
	   Discrimination and Class are created in the Suite if not
	   already present. */ 
	Discrimination.Cla c = my_suite.getClaAlways(disName, className, true);
	p.addClass(c, true);

	return p; 
    }


    /* We prepare a Vector of DataPoints identical to that produced by
      parsing various forms of data in the other SimpleTrain*
      applications (except SimpleTrain_BBRfiles).  */
    private static Vector<DataPoint> prepareDataSet()  {

	Vector<DataPoint> v=new	Vector<DataPoint>();
	v.add(makePoint("TinyDoc1",
			new String [][] {},
			"Tiny1",
			"WIDGET"));
	v.add(makePoint("TinyDoc2",
			new String [][] {{"Size","2.1"}},
			"Tiny1",
			"GADGET"));
	v.add(makePoint("TinyDoc3",
			new String [][] {{"Size","3.1"},{"Heat","3.2"}},
			"Tiny1",
			"WIDGET"));
	v.add(makePoint("TinyDoc4",
			new String [][] {{"Size","4.1"}},
			"Tiny1",
			"GADGET"));
	v.add(makePoint("TinyDoc5",
			new String [][] {{"Size","5.1"},{"Heat","5.2"},{"Mass","5.3"}},
			"Tiny1",
			"WIDGET"));
	v.add(makePoint("TinyDoc6",
			new String [][] {{"Size","6.1"}},
			"Tiny1",
			"GADGET"));
	v.add(makePoint("TinyDoc7",
			new String [][] {{"Heat","7.2"},{"Mass","7.3"}},
			"Tiny1",
			"WIDGET"));
	return v;
    }

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
