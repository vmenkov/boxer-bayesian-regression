/* The first statement in a Java file is the package statement.  The
one above declares that the code in this file is part of the package
edu.dimacs.mms.applications.examples. */ 
package edu.dimacs.mms.applications.examples;

import java.util.*;
import java.io.*;

import edu.dimacs.mms.boxer.*;
import edu.dimacs.mms.boxer.util.*;

/**    This application demonstrates how to use BOXER with an
       application that reads labeled training data from a flat file
       in BBR format, and writes a trained binary logistic regression
       file to a model file in BBR format.

       By "BBR format" we mean the file formats used by the Bayesian
       logistic regression programs BBRtrain and BBRclassify.  BBR
       format is also compatible with BMRtrain, BMRclassify, BXRtrain
       and BXRclassify. All six of these programs are discussed at
       http://www.bayesianregression.org/.)

       Because the BBR data and model formats only supports numeric
       class labels and feature IDs, the trained logistic regression
       model that this application produces has different class labels
       and feature IDs than the models produced by the other
       SimpleTrain_* applications.

       The BBR model format also allows nonzero coefficients for only
       one of the two classes, so the coefficients in the BBR model
       file produced by this application are different from those in
       the BOXER Suite file.  In fact the coefficient for a feature in
       the BBR model file is equal to the coefficient of that feature
       for class +1 in the BOXER file, minus the coefficient for that
       feature for class -1 in the BOXER file. The result is a model
       that makes exactly the same predictions as the model in the
       BOXER file.

<p>
       Usage:
<pre>
            java edu.dimacs.mms.applications.examples.SimpleTrain_BBRfiles [INFILE [BBR_OUTFILE [BOXER_OUTFILE]]]
</pre>
       where INFILE, BBR_OUTFILE, and BOXER_OUTFILE are optional. 

<p> 
       If INFILE is present, the training data is read from that
       file. Otherwise it is read from the file "tiny1.train.bbrdata"
       in the current working directory.

<p> 
       If BBR_OUTFILE is present, the trained logistic regression
       model is written to that file. Otherwise it is written to
       standard output (usually the screen).

<p>
       If BOXER_OUTFILE is present, the XML representation of the
       Suite (which includes the Discrimination definition and the
       trained model for that Discrimination) is written to that file.
       Otherwise it is written to standard output.

*/


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


    /* *********************  START OF main() *******************************/

    /* main() does the work of this class.  It is the method executed
    when you run 
             java SimpleTrain_BBRfiles 
    from the command line. */
    static public void main(String argv[]) 
	throws IOException, org.xml.sax.SAXException, BoxerException {


        /* See package-info.java for a discussion of the default input
	   file, tiny1.train.bbrdata. */ 
	String infile       = (argv.length > 0) ? argv[0] : "tiny1.train.bbrdata";
	String bbrOutfile   = (argv.length > 1) ? argv[1] : null; 
	String boxerOutfile = (argv.length > 2) ? argv[2] : null;
	if (argv.length > 3) usage("Too many arguments");


        /* A Suite in BOXER contains a set of Discriminations and a
	   set of Learners that can produce predictive models for
	   those Discriminations.  Here we create a new Suite and give
	   it the name "demo_suite".

	   The Suite constructor has the parameter
	   _supportsSimpleLabels =Suite.SupportsSimpleLabels.Polytomous.
	   This means that when BOXER will read a data set file that
	   contains discrimination names without class names (and that's
	   exactly what BBR files are like!), it will interpret all 
	   names as belonging to one large, multi-class ("polytomous")
	   discrimination.

	   The third parameter, _createNDMode, we keep at its default
	   value.  Since we won't be attempting to create a new 
	   discrimination, it isn't relevant. 
	*/
	Suite my_suite = new Suite("demo_suite",	    
				Suite.SupportsSimpleLabels.Polytomous,
				Suite.SysDefaults.createNDMode);


	/* Each record in a BBR format labeled data file specifies
           which of two classes to which a particular vector belongs,
           but not specify a discrimination name.  Therefore, in
           contrast to the sample applications that work with BOXER
           format data, we need to create and name the Discrimination
           explicitly.  We name it "Tiny1" to correspond to the
           Discrimination name that's encoded in the BOXER format data
           in other examples. */ 
	Discrimination dis = my_suite.addDiscrimination("Tiny1");


        /* infile should be a BBR format labeled data file.  We use
            BOXER method BXRReader.readDataFileBXR to read that file
            and convert the training examples in the file to a vector
            of DataPoint objects. (Note that this method is able to
            read data files in BBR, BMR, and BXR format.)  The final
            argument 'true' indicates that the data should be treated
            as *definitional*, i.e. that discrimination and class
            names encountered in the data should be added to
            my_suite. */

	/* (DDL to self: see my questions to Vladimir about when the
	    class labels become part of the Discrimination, and how we
	    know which Discrimination provides the context to interpret
	    the labels.) */

	Vector<DataPoint> parsed_data = BXRReader.readDataFileBXR(infile, my_suite, true); 

	    
	/* Add a simple TruncatedGradient learner with default
	   properties to my_suite.  In real applications it's more
	   common to initialize the learner with specifications read
	   from an XML file */ 
	Learner my_learner = new TruncatedGradient(my_suite);


        /* Run the learner on the training data.  For Learners such
	    as TruncatedGradient that implement online algorithms,
	    absorbExample takes only one pass over the training
	    data. (DDL to self: see my questions to vladimir about how
	    the bbr file gets associated with a particular
	    discrimination.) */ 
	my_learner.absorbExample(parsed_data);




        /* We write the trained model in BBR format. First we get from
            my_learner the PLRMLearnerBlock for the single
            Discrimination ("Tiny1") that we did training for.  A
            LearnerBlock in general stores the state of a Learner with
            respect to a single Discrimination.  For the
            TruncatedGradient learner we know that the LearnerBlock is
            more specifically a PLRMLearnerBlock. */ 
	PLRMLearner.PLRMLearnerBlock block =
	    (PLRMLearner.PLRMLearnerBlock) my_learner.findBlockForDis(dis);


        /*  Part of the state of a PLRMLearnerBlock is the
	    coefficient vector for the last version of the PLRM
	    produced.  PLRMLearnerBlock's method saveAsBBRModel writes 
	    that coefficient vector to a specified output.  Note that
	    other information stored in a PLRMLearnerBlock has no
	    equivalent in a BBR format file, and so is not written. 

	    Also note that the BBR model file format only supports
	    binary logistic regression models where the two classes
	    are named "1" ("+1" is treated as equivalent to 1) and
	    "-1". The method saveAsBBRModel() can write any binary
	    logistic regression model to BBR model file format. It
	    simply requires that it is told which of the two classes
	    become the class "+1" in the BBR model file. The other
	    class, whatever its name, becomes the class "-1" in the
	    BBR model file.  In our case this requirement of
	    saveAsBBRModel may seem redundant since the classes were
	    already named "-1" and "+1", but that's how saveAsBBRModel
	    works. */

	 /* If an output file was specified, we write to that file.
	   Otherwise we write to standard output. */ 
	final String outModelName = "Tiny1"; 
	if (bbrOutfile != null) {
	    PrintWriter w = new PrintWriter(new FileWriter(bbrOutfile));
	    block.saveAsBBRModel(w, outModelName, "+1");
	    w.close();
	} 
	else {
            block.saveAsBBRModel(new PrintWriter(System.out), outModelName, "+1");
	}

	/* We also write the Suite and the state of the associated Learner
	    in BOXER's XML format. If an output file is specified, we
	    use the BOXER method serializeLearnerComplex to write that
	    information directly to a file.  Otherwise we used the
	    BOXER method serializeLearnerComplex to serialize to an
	    in-memory XML document, and dump that document to standard
	    output (usually the screen) using BOXER method
	    writeXML. */ 
	if (boxerOutfile != null) {
	    my_suite.serializeLearnerComplex(boxerOutfile); 
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
