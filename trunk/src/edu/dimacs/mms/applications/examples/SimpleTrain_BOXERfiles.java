/**
      This application demonstrates using BOXER when the input is a
      flat file of labeled training data in standard BOXER format.

<p> 
       Usage:
<pre>
            java edu.dimacs.mms.applications.examples.SimpleTrain_BOXERfiles [INFILE [OUTFILE]]
</pre>

       where INFILE and OUTFILE are optional. 

<p> 
       If INFILE is present, the training data is read from that
       file. Otherwise it is read from the file "tiny1.train.boxer.xml"
       in the current working directory.

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

import edu.dimacs.mms.boxer.*;
import edu.dimacs.mms.boxer.util.*;


public class SimpleTrain_BOXERfiles {

    static void usage() {
	usage(null);
    }

    static private void usage(String m) {
	String name = (new SimpleTrain_BOXERfiles()).getClass().getName();
	System.out.println("Usage: java [options] "+name+" [input [output]]");
	if (m!=null) {
	    System.out.println(m);
	}
	System.exit(1);
    }

    /* *********************  START OF main() *******************************/

    /* main() does the work of this class.  It is the method executed
    when you run 
             java SimpleTrain_BOXERfiles 
    from the command line. */
    static public void main(String argv[]) 
	throws IOException, org.xml.sax.SAXException, BoxerException {

        /* See package-info.java for a discussion of the default input
	   file, tiny1.train.boxer.xml. */ 
	String infile = (argv.length>0) ? argv[0] : "tiny1.train.boxer.xml";
	String outfile = (argv.length>1) ? argv[1] : null;
	if (argv.length > 2) usage("Too many arguments");
	

        /* A Suite in BOXER contains a set of Discriminations and a
	   set of Learners that can produce predictive models for
	   those Discriminations.  Here we create a new Suite with
	   default characteristics and give it the name "demo_suite". */ 
	Suite my_suite = new Suite("demo_suite");	    


        /* The input file should be a BOXER format labeled data file.
            We use BOXER method ParseXML.readDataFileXML to read that
            file and convert the training examples in it to a vector
            of DataPoint objects.  The final argument 'true' indicates
            that the data should be treated as *definitional*,
            i.e. that discrimination and class names encountered in
            the data should be added to my_suite. */
	Vector<DataPoint> parsed_data = 
	    ParseXML.readDataFileXML(infile, my_suite, true);



	/* Add a simple TruncatedGradient learner with default
	   properties to my_suite.  In real applications it's more
	   common to initialize the learner with specifications read
	   from an XML file */ 
	Learner my_learner = new TruncatedGradient(my_suite);


        /* The BOXER method Learner.absorbExample makes one pass of
	   online learning over all the labeled examples, in the order
	   they appear in parsed_data.  

	   Note that each class label within BOXER format data (as
	   provided in the input file in this case) specifies both the
	   class the example belongs to, and the discrimination that
	   that class is drawn from. So even if the suite included
	   multiple Discriminations, there would be no ambiguity about
	   which Discriminations are updated for each training
	   example. */ 
	my_learner.absorbExample(parsed_data);


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
