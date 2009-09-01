package borj.rcv;

import boxer.*;

import java.util.*;
import java.io.*;

/** Given Rcv1-v2 files with the document content, and a list of IDs,
    prepare a BOXER XML file containing the features, and, optionally,
    labels, for the specified data points.

    Sample usage:
 <pre> 
    java  -Dfrom=1 -Dto=100 [-Dqrel=true] [-Dout=../rcv-out/out.xml] [-Dd=/home/vmenkov/rcv] borj.rcv.RcvToXML  id-list.txt lyrl2004_tokens_test_pt*.dat
</pre>

   The file listing the document IDs (data point IDs) is specified
   with the "list" option.  The "from" and "to" options (either or
   both can be omitted) specify what sections of the file will be
   used.

   If qrel=true, the output XML file will include both data points'
   features and their labels. Otherwise (qrel=false by default),
   only the features will be written to the XML file; this file can
   then be used with multiple Qrel XML files (which can be produced
   using QrelToXML)

   The "d" options specifies the location of the original QREL files, and the
   "out" option, the path for the output file.
   

*/

public class RcvToXML {


    static void usage() {
	System.out.println("Usage:");
	System.out.println("     java  -Dfrom=1 -Dto=100 [-Dqrel=true] [-Dout=../rcv-out/out.xml] [-Dd=/home/vmenkov/rcv] borj.rcv.RcvToXML  id-list.txt lyrl2004_tokens_test_pt*.dat");
	System.out.println("Read the Javadoc pages for more details.");
	usage(null);
    }

    static void usage(String m) {
	if (m!=null) {
	    System.out.println(m);
	}
	System.exit(1);
    }

    final static String REGION = "region", INDUSTRY = "industry", 
	TOPIC="topic", NONE="none", OTHER="other";    

    static public void main(String argv[]) throws IOException, org.xml.sax.SAXException{
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
	    fillTable(docs, argv[j], dic);
	}
	// Add classes now...
	Suite suite = new Suite("tmp_suite");

	// Create 2 unommitted discriminations, and make the
	// "industry:none" class default in the "industry"
	// discirmination0
	Discrimination disReg = suite.addDiscrimination(REGION);
	Discrimination disInd = suite.addDiscrimination(INDUSTRY);
	disInd.setDefaultClass(NONE);

	String d = "/home/vmenkov/rcv";
	d =ht.getOption("d", d);

	if (addQrel) {
	    System.out.println("Reading QREL files...");
	    String f = d + File.separator + "rcv1-v2.regions.qrels";	    
	    readQREL(docs, f, disReg);
	    f = d + File.separator + "rcv1-v2.industries.qrels";
	    readQREL(docs, f, disInd);
	} else {
	    System.out.println("Ignoring QREL files");
	}

	suite.commitAllDiscriminations(Suite.DCS.Fixed);

	String out = 	ht.getOption("out", "out.xml");	
	System.out.println("Checking if we have all " + a.length + " docs...");
	Vector<DataPoint> v = setToVector(a,docs,suite /*, addQrel*/);

	String setName = fnameToSetname(idFile, from, to);

	System.out.println("Writing data set " + setName + " with " + v.size() +
			   " documents to file " + out);
	DataPoint.saveAsXML( v, setName, out);

    }

    enum STATE { 
	WANT_I, WANT_W, READING_BODY, SKIPPING_BODY;
    }


    /** Reads a Rcv1-v2 document file, and adds docs with matching IDs
     * to the HashMap

	The input file looks like this:

<pre>
.I 2286
.W
recov recov recov recov excit excit bring ....
econom econom back back back track ....
...
move fact play felix boni boni ...

.I 2287
.W
more data ...
</pre>

@param docs Add document here
@param f File name


    */
    static void fillTable(HashMap<Integer, DataPoint> docs, String fname, 
			  FeatureDictionary dic) throws IOException {
 	LineNumberReader r = new LineNumberReader( new FileReader(fname));
	String s=null;
	int cnt=0, useCnt=0;
	Vector<Integer> v = new	Vector<Integer>();

	// a very primitive state machine...
	STATE state = STATE.WANT_I;
	int docid = -1;
	HashMap <String, Integer> words = new HashMap <String, Integer>();

	while( (s=r.readLine()) != null) {
	    s=s.trim();
	    
	    if (state==STATE.WANT_I) {
		String pat = ".I";
		if (s.startsWith(pat)) {
		    // found doc id
		    String q[] = s.split("\\s+");
		    if (q.length != 2) throw new IOException("Misformatted "+pat+" line: " + s);
		    docid = Integer.parseInt(q[1]);
		    state = STATE.WANT_W;
		    //System.out.println("Found .I for doc id=" + docid);
		} else { 
		    throw new IOException("Expected, but not found "+pat+" in line: " + s);
		}
	    } else if ( state == STATE.WANT_W) {
		String pat = ".W";
		if (s.equals(pat)) {
		    cnt++;
		    Integer key =  new Integer(docid);
		    if (docs.containsKey(key)) {
			System.out.println("Found text for doc id=" + key);
			useCnt++;
			if (docs.get(key)==null) {
			    state = STATE.READING_BODY;
			} else {
			     throw new IOException("Duplicate doc ID: " + docid + ", file=" + fname);
			}
		    } else {
			//System.out.println("Found text for doc id=" + key + ", don't need it");
			state = STATE.SKIPPING_BODY;
		    }
		    words.clear();
		} else throw new IOException("Expected, but not found "+pat+" in line: " + s);
	    } else if ( state == STATE.SKIPPING_BODY) {
		if (s.equals("")) {
		    state = STATE.WANT_I;
		}			
	    } else if ( state == STATE.READING_BODY) {
		if (s.equals("")) {
		    Integer key =  new Integer(docid);
		    System.out.println("Saving doc for id=" + key);
		    docs.put(key, DataPoint.makeDataPoint(words, dic, ""+docid));
		    state = STATE.WANT_I;
		} else {
		// reading doc body
		    String q[] = s.split("\\s+");
		    for(String z: q) {
			if (z.equals("")) continue;
			Integer a = words.get(z);
			int c =  (a==null) ? 1 : a.intValue()+1;
			words.put( z, new Integer(c));
		    }
		}
	    }
	}
	// Anything left?
	if ( state == STATE.READING_BODY) {
	    Integer key =  new Integer(docid);
	    docs.put(key, DataPoint.makeDataPoint(words, dic, ""+docid));
	}
	System.out.println("Found " + cnt + " docs in the input file, used " + useCnt);
    }

    /** Reads a QREL file, adding classes to the documents that appear
     * in the map */
    static void readQREL(HashMap<Integer, DataPoint> docs, String fname, 
			 Discrimination dis)  throws IOException
    {
	for(QrelIterator it = new QrelIterator(fname); it.hasNext(); ) {
	    QrelEntry q= it.next();
	    Integer key=new Integer(q.docid);

	    DataPoint doc = docs.get(key);
	    if (doc == null) continue;

	    // look up or create class for label
	    Discrimination.Cla cla = dis.addClass(q.cat, true, Suite.NC.API);
	    doc.addClass(cla, false); // on duplication, report an error
	}
    }

    /** Copies values whose keys are listed in a specified array from
     * the hashmap to the vector, and verifies that there is indeed a
     * value for each key
     * @param addDefault If true, the default classes of each discrimination will be added whenever needed
     */
    static Vector<DataPoint> setToVector(int a[], HashMap<Integer, DataPoint> docs, Suite suite/*, boolean addDefault*/) {
	
	Vector<DataPoint> v = new Vector<DataPoint>(a.length);
	int errcnt = 0;
	for(int q: a) {
	    DataPoint p =docs.get(new Integer(q)); 
	    if (p == null) {
		System.out.println("Warning: missing text for document " + q +
				   " in input files! Skipping.");
		errcnt ++;
		
	    } else {
		//if (addDefault) p.addDefaultClasses( suite);
		v.add(p); 
	    }
	}
	return v;
    }

    static String fnameToSetname(String idFile, int from, int to) {
	String setName = (new File(idFile)).getName();
	if (setName.endsWith(".xml") || setName.endsWith(".txt")) 
	    setName = setName.substring(0, setName.length()-4);
	if (from > 0 || to > 0) {
	    setName += "-" + ( from==0 ? 1 : from) + "-" + 
		(to==0 ? "end" : "" + to);
	}
	return setName;
    }


}