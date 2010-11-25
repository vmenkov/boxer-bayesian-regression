package edu.dimacs.mms.applications.util;

import java.util.*;
import java.io.*;
import java.text.*;
import org.w3c.dom.Element;

import edu.dimacs.mms.boxer.*;
import edu.dimacs.mms.borj.*;

/**  This is a sample utility tool for "re-centering" a dataset.

     <p>
     Usage:<br>
     java  Recenter input-data-set.xml output-data-set.xml


 */
public class Recenter {


    static void usage() {
	usage(null);
    }

    static void usage(String m) {
	System.out.println("This is the Recenter tool for the BOXER toolkit (version " + Version.version+ ")");
	if (m!=null) {
	    System.out.println(m);
	}
	System.exit(1);
    }

    static public void main(String argv[]) 
	throws IOException, org.xml.sax.SAXException, BoxerXMLException {

	if (argv.length!=2) usage();
	ParseConfig ht = new ParseConfig();

	DataPoint.setDefaultNameBase("recentered");

	Suite suite =  new Suite("Test_suite");

	//	String dicFileName =ht.getOption("dic", null);	
	//	suite.setDic(new  FeatureDictionary(new File(dicFileName)));

	String inFile = argv[0];
	String outFile = argv[1];

	Vector<DataPoint> points = ParseXML.readDataFileMultiformat(inFile, suite, true);
	Vector<DataPoint> recentered = recenter(points);

	System.out.println("Saving to: " + outFile);
	DataPoint.saveAsXML(recentered, "recentered", outFile); 


    }

   
    static public Vector<DataPoint> recenter(Vector<DataPoint> points) {
	if (points.size()==0) return new Vector<DataPoint>();
	DataPoint s = points.elementAt(0);
	for(int i=1; i<points.size(); i++) {
	    s = s.plus( points.elementAt(i));
	}
	s.multiplyBy( -1.0/ points.size());
	System.out.println(" -center= " + s);

	Vector<DataPoint> v= new Vector<DataPoint>( points.size());
	for(DataPoint p: points) {
	    DataPoint r =  p.plus(s);
	    r.linkLabels(p);
	    v.add(r);
	}
	return v;
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
