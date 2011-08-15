package edu.dimacs.mms.applications.examples;


import java.util.*;
import java.io.*;

import edu.dimacs.mms.boxer.*;
import edu.dimacs.mms.boxer.util.*;

/**
      This application shows the basics of using BOXER with training
       data residing in a flat file in the format used by the
       Bayesian logistic regression software BBRtrain, BBRclassify.
       The format is also usable by BMRtrain, BMRclassify, BXRtrain
       and BXRclassify. All these programs are discussed at
       http://www.bayesianregression.org/.  The format is also
       compatible with SVMlight (http://svmlight.joachims.org/) and a
       number of other learning programs.  Because this format only
       supports numeric class labels and feature IDs the trained PLRM
       we produce in this case has different class labels and feature
       IDs from the models produced by the other SimpleTrain_*
       applications.  The coefficients of the model are identical,
       however.

<p>
       We do the following:

<ol>

<li> We use BOXER method BXRReader.readDataFileBMR to read the input
            file "tiny1.train.bbrdata" (supplied with the BOXER
            distribution) and convert the training examples in the
            file to a vector of DataPoint objects. (Note that this is
            the same method that is used to read data in BXR format).
            The file contains training examples in BBR format that
            have been labeled with respect to a single binary
            discrimination.

<li> 
            We use BOXER method TruncatedGradient.absorbExample() to
            train a learner model associated with the suite (in this case 
	    a Suite containing only a single binary PLRM).

	    <li> 


	    <li> We use BOXER method
             PLRMLearner.PLRMLearnerBlock.saveAsBBRModel() to write
             the single binary logistic regression model in the
             trained Suite to output in the proper format for models
             used by BBRclassify.  Note that the BBR format only
             supports binary logistic regression models with a
             reference class.  </ol>

<p>
       Usage:
<pre>
            java edu.dimacs.mms.applications.examples.SimpleTrain_BBRfiles [INPUT [BBR_OUTFILE [BOXER_OUTFILE]]]
</pre>
       where INFILE,  BBR_OUTFILE, and BOXER_OUTFILE are optional. 

<p>
       If INFILE is ommitted, the training set is read from "tiny1.train.bbrdata".

<P>
       If BBR_OUTFILE is omitted, the BBR model is written to standard

<P>
       If BOXER_OUTFILE is omitted, the XML output is written to standard
       output, typically your screen.  */


public class SimpleTrain_BBRfiles {

    static void usage() {
	usage(null);
    }

    static private void usage(String m) {
	String name = (new SimpleTrain_BBRfiles()).getClass().getName();
	System.out.println("Usage: java [options] "+name+" [input [output]]");
	if (m!=null) {
	    System.out.println(m);
	}
	System.exit(1);
    }

    static public void main(String argv[]) 
	throws IOException, org.xml.sax.SAXException, BoxerException {

	String infile = (argv.length>0) ? argv[0] : "tiny1.train.bbrdata";
	String bbrOutfile = (argv.length>1) ? argv[1] : infile + ".model";
	String boxerOutfile = (argv.length>2) ? argv[2] : null;
	if (argv.length > 3) usage("Too many arguments");
	
	Suite suite = new Suite("Simple_polytomous",	    
				Suite.SupportsSimpleLabels.Polytomous,
				Suite.SysDefaults.createNDMode);

	/** We will be parsing a BBR file (not too different from BXR)
	    that has no discrimination names, just class names (which
	    happen to be "+1" and "-1", this being BBR). Thus the
	    suite should have exactly one discrimination, in whose
	    term labels will be interpreted.
	 */
	Discrimination dis = suite.addDiscrimination("main");

	/** Reading training data from file. Since the input file has
	    the .xml extension, readDataFileMultiformat() will invoke
	    ParseXML.readDataFileXML(infile, suite, true);
	 */
	Vector<DataPoint> train = 
	    //	    ParseXML.readDataFileMultiformat(infile, suite, true);
	    BXRReader.readDataFileBMR(infile, suite, true);

	//DataPoint.saveAsXML(train, 0, train.size(), "bbr",  "train-out.bbr.xml");


	/* Add a simple TruncatedGradient learner. Normally, you'd
	    read the learner's specification from an XML file, instead
	    of using the defaults. */
	Learner algo = new TruncatedGradient(suite);
	algo.absorbExample(train);

	/* // this is how to get a much better learner
	String learnerFile = "../learners/tg0-learner-param-eta=0.01.xml";
	Learner algo = suite.addLearner(ParseXML.readFileToElement(new File( learnerFile )));
	algo.runAdaptiveSD(train, 0, train.size(), 1e-6, 0);
	*/

	/* Save the model in BBR format, same as produced by BBRtrain */
	PLRMLearner.PLRMLearnerBlock block =
	    (PLRMLearner.PLRMLearnerBlock) algo.findBlockForDis(dis);
	PrintWriter w = new PrintWriter(new FileWriter(bbrOutfile));
	// the "positive" class is named "+1" in the input
	block.saveAsBBRModel(w, bbrOutfile, "+1");
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
