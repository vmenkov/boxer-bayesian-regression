/* The first statement in a Java file is the package statement.  The
one above declares that the code in this file is part of the package
edu.dimacs.mms.applications.examples. */ 

package edu.dimacs.mms.applications.examples;

import java.util.*;
import java.io.*;

import edu.dimacs.mms.boxer.*;
import edu.dimacs.mms.boxer.util.*;



/**    This application demonstrates how to use BOXER when reading
       labeled training data from a flat file in BXR format, and
       writing a trained binary logistic regression file to a model
       file in BXR format.

<p>
       By "BXR format" we mean the file formats used by the Bayesian
       logistic regression programs BXRtrain and BXRclassify
       (http://www.bayesianregression.org/). 

<p>	    
       Usage:
<pre>
            java edu.dimacs.mms.applications.examples.SimpleTrain_BXRfiles [INPUT [BXR_OUTFILE [BOXER_OUTFILE]]]
</pre>
       where INFILE,  BXR_OUTFILE, and BOXER_OUTFILE are optional. 

<p>
       If INFILE is present, the training data is read from that
       file. Otherwise it is read from the file "tiny1.train.bxrdata"
       in the current working directory.

<p>
       If BXR_OUTFILE is present, the trained logistic regression
       model is written to that file. Otherwise it is written to
       standard output (usually the screen).

<p>
       If BOXER_OUTFILE is present, the XML representation of the
       Suite (which includes the Discrimination definition and the
       trained model for that Discrimination) is written to that file.
       Otherwise it is written to standard output.
*/


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


    /* *********************  START OF main() *******************************/

    /* main() does the work of this class.  It is the method executed
    when you run 
             java SimpleTrain_BXRfiles 
    from the command line. */
    static public void main(String argv[]) 
	throws IOException, org.xml.sax.SAXException, BoxerException {


        /* See package-info.java for a discussion of the default input
	   file, tiny1.train.bxrdata. */ 
	String infile = (argv.length>0) ? argv[0] : "tiny1.train.bxrdata";
	String bxrOutfile = (argv.length>1) ? argv[1] : infile + ".model";
	String boxerOutfile = (argv.length>2) ? argv[2] : null;
	if (argv.length > 3) usage("Too many arguments");


        /* A Suite in BOXER contains a set of Discriminations and a
	   set of Learners that can produce predictive models for
	   those Discriminations.  Here we create a new Suite and give
	   it the name "demo_suite". */ 
        /* DDL to self: I wrote Vladimir about why the extra arguments are needed here. */ 	
	Suite my_suite = new Suite("demo_suite",	    
				Suite.SupportsSimpleLabels.Polytomous,
				Suite.SysDefaults.createNDMode);



	/* Each record in a BXR format labeled data file specifies
           which of two classes to which a particular vector belongs,
           but not specify a discrimination name.  Therefore, in
           contrast to the sample applications that work with BOXER
           format data, we need to create and name the Discrimination
           explicitly.  We name it "Tiny1" to correspond to the
           Discrimination name that's encoded in the BOXER format data
           in other examples. */ 
	Discrimination dis = my_suite.addDiscrimination("main");


        /* infile should be a BXR format labeled data file.  We use
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

	Vector<DataPoint> parsed_data = 
	    BXRReader.readDataFileBXR(infile, my_suite, true);


	/* Add a simple TruncatedGradient learner with default
	   properties to my_suite.  In real applications it's more
	   common to initialize the learner with specifications read
	   from an XML file */ 
	PLRMLearner my_learner = new TruncatedGradient(my_suite);


        /* Run the learner on the training data.  For Learners such
	    as TruncatedGradient that implement online algorithms,
	    absorbExample takes only one pass over the training
	    data. (DDL to self: see my questions to vladimir about how
	    the bbr file gets associated with a particular
	    discrimination.) */ 
	my_learner.absorbExample(parsed_data);

        /* We write the trained model in BBR format.  This is done in 
	    in four steps. */ 

	/* We use BOXER method
            PLRMLearner.PLRMLearnerBlock.saveAsBXRModel() to write the
	    Suite.serializeLearnerComplex(outfile) to write the
	    single binary logistic regression model in the trained
	    Suite to output in the proper format for models used by
	    BXRclassify.*/ 

        /* 1. Get from my_learner the PLRMLearnerBlock for the single
	    Discrimination ("Tiny1") that we did training for.  A
	    LearnerBlock in general stores the state of a Learner with
	    respect to a single Discrimination.  For the
	    TruncatedGradient learner we know that the LearnerBlock is
	    more specifically a PLRMLearnerBlock. */ 
	PLRMLearner.PLRMLearnerBlock block =
	    (PLRMLearner.PLRMLearnerBlock) my_learner.findBlockForDis(dis);

        /* 2. A standard Java idiom for preparing to write to a file. */ 
	PrintWriter w = new PrintWriter(new FileWriter(bxrOutfile));


        /* 3. Part of the state of a PLRMLearnerBlock is the
	    coefficient vector for the last version of the PLRM
	    produced.  PLRMLearnerBlock's method saveAsBBRModel writes 
	    that coefficient vector to BXR format file.  Note that
	    other information stored in a PLRMLearnerBlock has no
	    equivalent in a BXR format file, and so is not written. */ 
	block.saveAsBXRModel(w, bxrOutfile);

        /* 4. Having written the file, we close down the writer. */ 
	w.close();


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
	} else {
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
