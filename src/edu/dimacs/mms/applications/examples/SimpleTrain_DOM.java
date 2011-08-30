/* The first statement in a Java file is the package statement.  The
one above declares that the code in this file is part of the package
edu.dimacs.mms.applications.examples. */ 
package edu.dimacs.mms.applications.examples;

import java.util.*;
import java.io.*;

import org.w3c.dom.Element;
import org.w3c.dom.Document;
import org.apache.xerces.dom.DocumentImpl;

import edu.dimacs.mms.boxer.*;
import edu.dimacs.mms.boxer.util.*;


/**
      This application demonstrates using BOXER when labeled training
      data is created and supplied using the capabilities of the
      org.w3c.dom packages.  

      We demonstrate assembly of the training data as an object
      supporting the interface Element defined by the package
      org.w3c.dom.  We assemble the object using XML-oriented
      operations from the package org.w3c.dom.Element.  The resulting
      Element is identical to the one produced by other means when

              1) the sample data file tiny1.train.boxer.xml is used
      with the application SimpleTrain_BOXERfiles, 

              2) the sample data file tiny1.train.bxrdata is used with
      the application SimpleTrain_BXRfiles, or 

              3) when the application SimpleTrain_strings is run.

<p>
       Usage:
<pre>
            java edu.dimacs.mms.applications.learning.SimpleTrain_DOM [OUTFILE]
</pre>
       where OUTFILE is optional.  

       If OUTFILE is present, the XML representation of the Suite
       (which includes the Discrimination definition and the trained
       model for that Discrimination) is written to that file.
       Otherwise it is written to standard output (usually the
       screen).
*/


public class SimpleTrain_DOM {

    static void usage() {
	usage(null);
    }

    static private void usage(String m) {
	String name = (new SimpleTrain_DOM()).getClass().getName();
	System.out.println("Usage: java [options] "+name+" [input [output]]");
	if (m!=null) {
	    System.out.println(m);
	}
	System.exit(1);
    }


    /* We will be storing the assembed data set in this object. */ 
    private static Document xmldoc;


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


	/* The first line creates, and sets xmldoc, to an object of
	   the implementation class for the Document interface, using
	   the implementation from org.apache.xerces.dom.DocumentImpl.
	   This makes the application dependent on Xerces, but then
	   much of the XML processing within BOXER itself is dependent
	   on Xerces.  (If there is sufficient interest, we may create
	   sample applications which show how to handle data received
	   as an Element created using a non-Xerces implementation.

           The second line then calls prepareDataSetElement (defined
           below) which uses appropriate method calls on xmldoc to
           create a labeled data set in the form of an object that
           implements the Element interface defined by the org.w3c.dom
           package.  This mimics a situation where an application
           builds (or receives from elsewhere) data sets in BOXER XML
           format (or in some other XML format transformable to BOXER
           format through some XML munging). */ 
	xmldoc = new DocumentImpl();
	Element e =  prepareDataSetElement();


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

	   Note that each class label within BOXER format XML data
	   specifies both the class the example belongs to, and the
	   discrimination that that class is drawn from, so even if
	   the suite included multiple Discriminations there would be
	   no ambiguity about which Discriminations are updated for
	   each training example. */ 
	my_learner.absorbExample(parsed_data);


        /* We use the method
           serializeLearnerComplex().getDocumentElement() to serialize
           the Suite and the state of the associated Learner as an
           Element called lcElement.  */ 
	Element lcElement = my_suite.serializeLearnerComplex().getDocumentElement();


	/* In a real application based on manipulation of XML
           structures, we might pass now lcElement to other code that
           would extract information of interest (e.g. the
           discrimination and class names, the coefficients of the
           trained model, etc.) using DOM-supported operations. 

           For this simple demonstration, however, we just write the
           XML for the LearnerComplex to a file, or to standard
           output, as in our other sample applications.  Note that the
           methods used a slightly different, since we've already
           serialized the LearnerComplex as an Element.*/ 
	if (outfile != null) {
	    FileOutputStream os = new FileOutputStream(outfile);    
	    XMLUtil.writeXML(lcElement, os);
	    os.close();
	} else {
	    XMLUtil.writeXML(lcElement , System.out);
	}

    }
    /* *********************  END OF main() *******************************/


    /* A low level XML method used by prepareDataSetElement. */ 
    private static void  addFeatureToElement(Element e, String pair[]) {
	 Element efeature = xmldoc.createElement( ParseXML.NODE.FEATURE);

         efeature.setAttribute(ParseXML.ATTR.FEATURE_ATTR[0], pair[0]);
         efeature.setAttribute(ParseXML.ATTR.FEATURE_ATTR[1], pair[1]);
         e.appendChild(efeature);
    }

   
    /* A low level XML method used by prepareDataSetElement. */ 
    private static Element makePoint( String name, String[][] pairs, String label) {
	final String disName="Tiny1";
	
	Element e = xmldoc.createElement( ParseXML.NODE.DATAPOINT);
	e.setAttribute(ParseXML.ATTR.NAME_ATTR, name);
	
	Element elabels = xmldoc.createElement( ParseXML.NODE.LABELS);
	e.appendChild(elabels);
	
	Element elabel = xmldoc.createElement( ParseXML.NODE.LABEL);
	elabels.appendChild(elabel);
	elabel.setAttribute(ParseXML.ATTR.LABEL_ATTR[0], disName);
	elabel.setAttribute(ParseXML.ATTR.LABEL_ATTR[1], label);
	
	Element ef = xmldoc.createElement( ParseXML.NODE.FEATURES);
	e.appendChild(ef);

	for(String [] pair: pairs) {
	    addFeatureToElement(ef, pair);
	}
	return e; 
    }
    
    /* Prepares a data set equivalent to the one represented in XML
	 in tiny1.train.boxer.xml */
    private static Element prepareDataSetElement()  {

	Vector<Element> v=new	Vector<Element>();
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

	Element e = xmldoc.createElement( ParseXML.NODE.DATASET);
	e.setAttribute(ParseXML.ATTR.NAME_ATTR, "a_sample_dataset");
	e.setAttribute("version", Version.version);	

	for(Element p: v) {
	    e.appendChild(p);
	}
	return e;
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
