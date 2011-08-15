/**
       This application demonstrates using BOXER with flat files in
       BXR format.  BXRtrain and BXRclassify
       (http://www.bayesianregression.org/), like BOXER, are programs
       for training (using Bayesian priors) and application of
       PLRMs.  We do the following:

<ol>

<li>
            We use BOXER method BXRReader.readDataFileBMR to read the
            input file "tiny1.train.bxrdata" (supplied with the BOXER
            distribution) and convert the training examples in the
            file to a vector of DataPoint objects. The file contains
            training examples in XML format that have been labeled
            with respect to a single binary discrimination.

<li> 
            We use BOXER method TruncatedGradient.absorbExample() to
            train a Suite (in this case a Suite containing only a
            single binary PLRM).

	    <li> 
	    We use BOXER method
            PLRMLearner.PLRMLearnerBlock.saveAsBXRModel() to write the
	    Suite.serializeLearnerComplex(outfile) to write the
	    single binary logistic regression model in the trained
	    Suite to output in the proper format for models used by
	    BXRclassify.

<li>
            We use BOXER method
	    Suite.serializeLearnerComplex(outfile) to write the
	    single binary logistic regression model in the trained
	    Suite to output in the proper format for models used by
	    BXRclassify. suite and the model in the standard BOXER format.

<li>
            The XML for for the trained Suite is then written as a
            list commented lines to the output.
</ol>
<p>	    
       Usage:
<pre>
            java edu.dimacs.mms.applications.examples.SimpleTrain_BXRfiles [INPUT [BXR_OUTFILE [BOXER_OUTFILE]]]
</pre>
       where INFILE,  BXR_OUTFILE, and BOXER_OUTFILE are optional. 

<p>
       If INFILE is ommitted, the training set is read from "tiny1.train.bxrdata".

<P>
       If BXR_OUTFILE is omitted, the BXR model is written to standard

<P>
       If BOXER_OUTFILE is omitted, the XML output is written to standard
       output, typically your screen.  */

package edu.dimacs.mms.applications.examples;


import java.util.*;
import java.io.*;

import edu.dimacs.mms.boxer.*;
import edu.dimacs.mms.boxer.util.*;


public class SimpleTrain_BXRfiles {

    static void usage() {
	usage(null);
    }

    static private void usage(String m) {
	String name = (new SimpleTrain_BXRfiles()).getClass().getName();
	System.out.println("Usage: java [options] "+name+" [input [output]]");
	if (m!=null) {
	    System.out.println(m);
	}
	System.exit(1);
    }

    static public void main(String argv[]) 
	throws IOException, org.xml.sax.SAXException, BoxerException {

	String infile = (argv.length>0) ? argv[0] : "tiny1.train.bxrdata";
	String bxrOutfile = (argv.length>1) ? argv[1] : infile + ".model";
	String boxerOutfile = (argv.length>2) ? argv[2] : null;
	if (argv.length > 3) usage("Too many arguments");
	
	Suite suite = new Suite("Simple_polytomous",	    
				Suite.SupportsSimpleLabels.Polytomous,
				Suite.SysDefaults.createNDMode);

	/** We will be parsing a BXR file that has no discrimination names,
	    just a class name. Thus the suite should have exactly one
	    discrimination, in whose term labels will be interpreted.
	 */
	Discrimination dis = suite.addDiscrimination("main");

	/** Reading training data from file. Since the input file has
	    the .xml extension, readDataFileMultiformat() will invoke
	    ParseXML.readDataFileXML(infile, suite, true);
	 */
	Vector<DataPoint> train = 
	    //	    ParseXML.readDataFileMultiformat(infile, suite, true);
	    BXRReader.readDataFileBMR(infile, suite, true);

	//DataPoint.saveAsXML(train, 0, train.size(), "bxr",  "train-out.bxr.xml");


	/** Add a simple TruncatedGradient learner. Normally, you'd
	    read the learner's specification from an XML file. */
	PLRMLearner algo = new TruncatedGradient(suite);
	algo.absorbExample(train);

	/** Save the model in BXR format, same as produced by BXRtrain */
	PLRMLearner.PLRMLearnerBlock block =
	    (PLRMLearner.PLRMLearnerBlock) algo.findBlockForDis(dis);
	PrintWriter w = new PrintWriter(new FileWriter(bxrOutfile));
	block.saveAsBXRModel(w, bxrOutfile);
	w.close();

	/** save the suite and the model in BOXER's XML format */
	if (boxerOutfile!=null) {
	    suite.serializeLearnerComplex(boxerOutfile); 
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
