package edu.dimacs.mms.tokenizer;

import edu.dimacs.mms.boxer.*;

import java.util.*;
import java.io.*;
import javax.swing.text.html.parser.*;

/** This is a simple parser/tokenizer built so that I could get some data sets... and to test I/O
 */
public class Tokenizer {

    FeatureDictionary dic;    

    static void usage() {
	usage(null);
    }

    static void usage(String m) {
	/*
	System.out.println("Usage: java [-Dmodel=eg|tg] boxer.Driver train.xml test.xml");
	System.out.println("Usage: java [-Dmodel=eg|tg] boxer.Driver train:train1.xml train:train2.xml test:test_a.xml train:train3.xml test:test_b.xml");
	System.out.println(" ... etc.");
	*/
	if (m!=null) {
	    System.out.println(m);
	}
	System.exit(1);
    }
    
    static Vector<Discrimination.Cla> parseClassesOpt(String s, Suite suite) {
	Vector<Discrimination.Cla> clav = new Vector<Discrimination.Cla>();
	if (s==null) return clav;
	String z[] = s.split(",");
	for(String q: z) {
	    if (q.equals("")) continue;
	    String [] pair = q.split(":");
	    if (pair.length != 2 || pair[0].equals("") || pair[1].equals("")) 
		throw new IllegalArgumentException("Can't parse class list " + q);
	    clav.add(suite.getClaAlways(pair[0], pair[1], true));
	}
	return clav;
    }

    static public void main(String argv[]) throws IOException, org.xml.sax.SAXException, BoxerXMLException {

	//memory();
	if (argv.length==0) usage();

	ParseConfig ht = new ParseConfig();

	String outfile =ht.getOption("out", "out.xml");

	// dis:class[,dis:class][,...]
	String classesOpt=ht.getOption("classes", "");	
	
	Suite suite = new Suite("tmp");
	Vector<Discrimination.Cla> clav = parseClassesOpt(classesOpt, suite);

	
	Tokenizer t = new Tokenizer();
	t.dic = new FeatureDictionary();

	Vector<DataPoint> v = new Vector<DataPoint>();

	for(int i=0; i<argv.length; i++) {
	    DataPoint p = t.tokenizeFile(argv[i]);
	    p.setClasses(clav, suite);
	    v.add(p);
	}

	DataPoint.saveAsXML( v, outfile, outfile);
    }

    class MyParser extends javax.swing.text.html.parser.Parser {

	MyParser(DTD dtd) { super(dtd); }

	HashMap<String, Integer> h = new HashMap<String, Integer>();

	protected void handleText(char[] text) {
	    String s= new String(text);
	    if (s==null) return;
	    //System.out.println("Text-1: " + s);
	    s = s.toLowerCase().replace("&nbsp;", " ");
	    s = s.replace("&nbsp", " "); // they lose semicolons...
	    s = s.replaceAll("[^a-zA-Z]", " ").trim();
	    //System.out.println("Text-2: " + s);
	    String q[] = s.split("\\s+");
	    for(String a: q) {
		if (a.equals("")) continue;
		Integer x= h.get(a);
		x = (x==null) ? new Integer(1) : new Integer( x.intValue()+1);
		h.put(a,x);
	    }
	}

	/** @param name The name to assign to the new data point */
	DataPoint toDataPoint(String name) throws BoxerXMLException {
	    String[] keys = h.keySet().toArray(new String[0]);
	    Arrays.sort(keys);
	    Vector <DataPoint.FVPair> v =new Vector <DataPoint.FVPair>();
	    for(String a: keys) {
		int id = dic.getIdAlways(a);
		v.add( new DataPoint.FVPair(id, h.get(a).intValue()));
	    }
	    return new DataPoint(v, dic, name);
	}
    }

    DataPoint tokenizeFile(String fname)	throws IOException, BoxerXMLException
    {
	DTD dtd = DTD.getDTD("http://www.w3.org/TR/html4/strict.dtd");
	System.out.println("DTD="+dtd);
	MyParser parser = new MyParser(dtd); //javax.swing.text.html.parser.Parser(dtd);
	parser.parse(new FileReader(fname));
	DataPoint dp =  parser.toDataPoint(fname);
	return dp;
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
