package edu.dimacs.mms.applications.util;

import java.util.*;
import java.io.*;
import java.text.*;
import org.w3c.dom.Element;

import edu.dimacs.mms.boxer.*;
import edu.dimacs.mms.borj.*;

/**  This is a sample utility tool for "scaling" or "normalizing" a
     dataset. There are two modes available: 

     <ul> 

     <li>"Scaling" the data, i.e. multiplying all example vectors by
     the same constant.
     
     <li>"Normalizing" the date, i.e. multiplying each example by an
     appropriate factor to make its 2-norm (not including the dummy
     component)equal to 1).
     </ul>

     <p>
     Usage:<br>
     java [-Dby=0.01] [-Dnormalize=true] Scale input-data-set.xml output-data-set.xml


 */
public class Scale {


    static void usage() {
	usage(null);
    }

    static void usage(String m) {
	System.out.println("This is the Scale tool for the BOXER toolkit (version " + Version.version+ ")");
	if (m!=null) {
	    System.out.println(m);
	}
	System.exit(1);
    }

    static public void main(String argv[]) 
	throws IOException, org.xml.sax.SAXException, BoxerXMLException {

	if (argv.length!=2) usage();
	ParseConfig ht = new ParseConfig();

	boolean normalize = ht.getOption("normalize", false);

	if (normalize && ht.getOption("by", null)!=null) {
	    usage( "Cannot use 'normalize=true' and 'by=...' at the same time");
	}

	double by=ht.getOptionDouble("by", 0.0);	


	DataPoint.setDefaultNameBase("scaled");

	Suite suite =  new Suite("Test_suite");

	//	String dicFileName =ht.getOption("dic", null);	
	//	suite.setDic(new  FeatureDictionary(new File(dicFileName)));

	String inFile = argv[0];
	String outFile = argv[1];

	Vector<DataPoint> points = ParseXML.readDataFileMultiformat(inFile, suite, true);
	if (normalize) {
	    System.out.println("Normalizing...");
	    normalize(points);
	} else {
	    System.out.println("Scaling by "+by+"...");
	    scale(points,by);
	}

	System.out.println("Saving to: " + outFile);
	DataPoint.saveAsXML(points, "scaled", outFile); 


    }

    static public void normalize(Vector<DataPoint> points) {
	for(DataPoint p: points) {
	    double norm = Math.sqrt( p.normSquareWithoutDummy());
	    if (norm !=0) 	    p.multiplyBy( 1.0/norm);
	}
    }

    /** Multiplies each vector by the same factor 
	@param by Multiply by this number
     */
    static public void scale(Vector<DataPoint> points, double by) {
	for(DataPoint p: points) {
	    p.multiplyBy( by);
	}
    }


    static void memory() {
	memory("");
    }

    static void memory(String title) {
	Runtime run =  Runtime.getRuntime();
	String s = (title.length()>0) ? " ("+title+")" :"";
	run.gc();
	long mmem = run.maxMemory();
	long tmem = run.totalMemory();
	long fmem = run.freeMemory();
	long used = tmem - fmem;
	System.out.println("[MEMORY]"+s+" max=" + mmem + ", total=" + tmem +
			   ", free=" + fmem + ", used=" + used);	
    }

}

/*
Copyright 2009, Rutgers University, New Brunswick, NJ.

All Rights Reserved

Permission to use, copy, and modify this software and its documentation for any purpose 
other than its incorporation into a commercial product is hereby granted without fee, 
provided that the above copyright notice appears in all copies and that both that 
copyright notice and this permission notice appear in supporting documentation, and that 
the names of Rutgers University, DIMACS, and the authors not be used in advertising or 
publicity pertaining to distribution of the software without specific, written prior 
permission.

RUTGERS UNIVERSITY, DIMACS, AND THE AUTHORS DISCLAIM ALL WARRANTIES WITH REGARD TO 
THIS SOFTWARE, INCLUDING ALL IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR 
ANY PARTICULAR PURPOSE. IN NO EVENT SHALL RUTGERS UNIVERSITY, DIMACS, OR THE AUTHORS 
BE LIABLE FOR ANY SPECIAL, INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER 
RESULTING FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT, 
NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION WITH THE USE OR 
PERFORMANCE OF THIS SOFTWARE.
*/
