package edu.dimacs.mms.borj.rcv;

import edu.dimacs.mms.boxer.*;

import java.util.*;
import java.io.*;

/** Given Rcv1-v2 files with the document content, and a list of IDs,
    prepare a BOXER XML file containing the features, and, optionally,
    labels, for the specified data points.

    Sample usage:
 <pre> 
    java  -Dfrom=1 -Dto=100 [-Dqrel=true] [-Dout=../rcv-out/out.xml] [-Dd=/home/username/rcv] borj.rcv.RcvToXML  id-list.txt lyrl2004_tokens_test_pt*.dat
</pre>

<p>
   The file listing the document IDs (data point IDs) is specified
   with the "list" option.  The "from" and "to" options (either or
   both can be omitted) specify what sections of the file will be
   used.

<p>
   If qrel=true, the output XML file will include both data points'
   features and their labels. Otherwise (qrel=false by default),
   only the features will be written to the XML file; this file can
   then be used with multiple Qrel XML files (which can be produced
   using QrelToXML)

<p>
   The "d" options specifies the location of the original QREL files, and the
   "out" option, the path for the output file.
   

*/

public class RcvToLAD {


    static void usage() {
	System.out.println("Usage:");
	System.out.println("     java  -Dfrom=1 -Dto=100 [-Dqrel=true] [-Dout=../rcv-out/out.xml] [-Dd=/home/username/rcv] borj.rcv.RcvToLAD  id-list.txt lyrl2004_tokens_test_pt*.dat");
	System.out.println("Read the Javadoc pages for more details.");
	usage(null);
    }

    static void usage(String m) {
	if (m!=null) {
	    System.out.println(m);
	}
	System.exit(1);
    }

    final static String REGION = "Region";
    //, INDUSTRY = "industry", 	TOPIC="topic", NONE="none", OTHER="other";    

    static public void main(String argv[]) throws IOException, org.xml.sax.SAXException, BoxerXMLException {
	ParseConfig ht = new ParseConfig();
	// 0 means: no restriction
	int from = ht.getOption("from", 0),  to = ht.getOption("to", 0);
	boolean addQrel = ht.getOption("qrel", false);

	if (argv.length < 2) usage();
	String idFile = argv[0];
	System.out.println("Reading doc IDs from " + idFile + 
			   (from == 0? " beginning" : " line " + from) + " to"+
			   (to == 0? " end" : " line " + to) );
			   
	// list of doc ids
	int a[] = RcvUtil.readIds(idFile, from, to);
	// The keys are all ids; the values, initially null.
	FeatureDictionary dic = new FeatureDictionary();
	HashMap<Integer, DataPoint> docs = new HashMap<Integer, DataPoint>();
	for(int q: a) docs.put(new Integer(q), (DataPoint)null); 
	for(int j=1; j<argv.length; j++) {
	    System.out.println("Reading doc file " + argv[j]);
	    RcvToXML.fillTable(docs, argv[j], dic);
	}
	// Add classes now...
	Suite suite = new Suite("tmp_suite");

	// Create 2 uncommitted discriminations, and make the
	// "industry:none" class default in the "industry"
	// discirmination0
	Discrimination disReg = suite.addDiscrimination(REGION);

	//Discrimination disInd = suite.addDiscrimination(INDUSTRY);
	//disInd.setDefaultClass(NONE);

	String d = null; // "/home/vmenkov/rcv";
	d =ht.getOption("d", d);
	if (d==null) usage("Option -Dd=... must be set");

	if (addQrel) {
	    System.out.println("Reading QREL files...");
	    String f = d + File.separator + "regions.sampled.qrels";	    
	    RcvToXML.readQREL(docs, f, disReg);
	    //f = d + File.separator + "rcv1-v2.industries.qrels";
	    //RcvToXML.readQREL(docs, f, disInd);
	} else {
	    System.out.println("Ignoring QREL files");
	}

	suite.commitAllDiscriminations(Suite.DCS.Fixed);

	String out = 	ht.getOption("out", "out.xml");	
	System.out.println("Checking if we have all " + a.length + " docs...");
	Vector<DataPoint> v = RcvToXML.setToVector(a,docs,suite /*, addQrel*/);

	//String setName = RcvToXML.fnameToSetname(idFile, from, to);

	System.out.println("Writing data set with " + v.size() +
			   " documents to file " + out);
	//DataPoint.saveAsXML( v, setName, out);
	saveAsLAD(v, suite, out);
    }

    static void saveAsLAD(Vector<DataPoint> v, Suite suite, String fname) throws IOException {
	PrintWriter pw = new PrintWriter(new FileWriter(fname));
	for(DataPoint p: v) saveAsLAD(p, suite, pw);
	pw.flush();
	pw.close();
    }

    static void saveAsLAD(DataPoint p,  Suite suite, PrintWriter w) {
	Vector<Discrimination.Cla> vc = p.getClasses( suite);
	for(Discrimination.Cla cla: vc) {
	    w.println(p.getName() + " " + cla.getDisc().getName() +
		      " " + cla.getName());
	}
	int[] features = p.getFeatures();
	double[] values = p.getValues();
	FeatureDictionary dic=p.getDic();
	for(int i=0;i<features.length; i++) {
	    w.println(p.getName() + " " + dic.getLabel(features[i]) + " " 
		      + (int)values[i]);
	}
  
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