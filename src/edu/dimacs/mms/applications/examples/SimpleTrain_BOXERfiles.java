/**
      This application demonstrates using BOXER with flat files in
       standard BOXER formats.  We do the following:

       <ol>
            
       <li>
       We use BOXER method ParseXML.readDataFileXML to read
            the file "tiny1.train.boxer.xml" (supplied with the BOXER
            distribution) and convert the training examples in the
            file to a vector of DataPoint objects. The file contains
            training examples in XML format that have been labeled
            with respect to a single binary discrimination.

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
            java edu.dimacs.mms.applications.examples.SimpleTrain_BOXERfiles [INPUT [OUTFILE]]
</pre>
       where OUTFILE is optional.  If omitted the output is written
       to standard output, typically your screen.
*/

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

    static public void main(String argv[]) 
	throws IOException, org.xml.sax.SAXException, BoxerException {

	String infile = (argv.length>0) ? argv[0] : "tiny1.train.boxer.xml";
	String outfile = (argv.length>1) ? argv[1] : null;
	if (argv.length > 2) usage("Too many arguments");
	
	Suite suite = new Suite("A_default_suite");	    

	/** Reading training data from file. Since the input file has
	    the .xml extension, readDataFileMultiformat() will invoke
	    ParseXML.readDataFileXML(infile, suite, true);
	 */
	Vector<DataPoint> train = 
	    ParseXML.readDataFileMultiformat(infile, suite, true);

	//DataPoint.saveAsXML(train, 0, train.size(), "boxer",  "train-out.boxer.xml");
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
