package edu.dimacs.mms.borj.rcv;

import edu.dimacs.mms.boxer.*;

import java.util.*;
import java.io.*;

/** Prepares list of all documents (from the original RCV1-v2 corpus)
  with exactly one region and 0 or 1 industry. These are what we can
  call the "eligible" documents for use in experiments according to
  David Lewis' 2008-12-06 plan.

Usage: 
<pre>
  java -cp $cp -Dd=$in -Dout=$out borj.rcv.PrepareRCV  > $outdir/prepare.log
</pre>
   Here, $in is the directory where the original RCV files have been
   installed, and $out is the directory to which the document lists
   will be written.

   This program produces the following files:
<ul>
<li>rcv-boxer-ids-sorted.txt - list of eligible document IDs, in the original order
<li>rcv-boxer-ids-random.txt - list of eligible document IDs, in a randomized order
<li>rcv-suite.xml - An XML file describing a Suite that includes two Discriminations - one with all regions, the other with all industries (+ none); it can be used for a test with a really large number of categories, in combination with data files that carry all original industry and region labels.
</ul>

 */
public class PrepareRCV {


    static void usage() {
	usage(null);
    }

    static void usage(String m) {
	if (m!=null) {
	    System.out.println(m);
	}
	System.exit(1);
    }
    
    static public void main(String argv[]) throws IOException, org.xml.sax.SAXException{

	//memory();
	//if (argv.length==0) usage();

	ParseConfig ht = new ParseConfig();

	/** The directory where the original rv files are */
	String d = "/home/vmenkov/rcv";
	d =ht.getOption("d", d);

	String out = ht.getOption("out", ".");

	String freg = d + File.separator + "rcv1-v2.regions.qrels";
	System.out.println("Reading region QREL file " + freg);
	PairList rv = readQREL(freg);

	System.out.println("Distinct doc ids: " + rv.size());
	int rcnt[]= getCatCounts(rv);
	for(int i=1; i<rcnt.length; i++) {
	    System.out.println("  With " + i + " region(s) : " + rcnt[i]);
	}
	
	// "rcv1-v2.topics.qrels";
	String find = d + File.separator + "rcv1-v2.industries.qrels";
	System.out.println("Reading industry QREL file " + find);
	PairList iv = readQREL(find);

	System.out.println("Distinct doc ids: " + iv.size());
	rcnt= getCatCounts(iv);
	for(int i=1; i<rcnt.length; i++) {
	    System.out.println("  With " + i + " industrie(s) : " + rcnt[i]);
	}


	// Docs with exactly 1 region
	PairList reg1 =  rv.filterByCnt( 1);
	// Docs with exactly 1 industry
	PairList ind1 =  iv.filterByCnt( 1);

	//ind1.write("ind1.tmp");


	// only those docs which have count=1 in both lists
	PairList w = reg1.intersect( ind1);
	//w.write("w.tmp");

	// Docs that have exactly one region but no industry
	PairList rNoInd = reg1.difference(  iv);
	//rNoInd.write("rNoInd.tmp");

	System.out.println("Doc ids with unique region and unique industry: " + w.size());
	System.out.println("Doc ids with unique region and no industry: " + rNoInd.size());

	PairList u = w.union(rNoInd);
	System.out.println("Union: " + u.size());

	String g = out + File.separator + "rcv-boxer-ids-sorted.txt";
	System.out.println("Writing file " + g);
	u.write(g);

	PairList r = u.randomize();
	g = out + File.separator +"rcv-boxer-ids-random.txt";
	System.out.println("Writing file " + g);
	r.write(g);

	// Prepare a suite?
	Suite suite = new Suite("PrepareRCV_aux_suite");
	
	IntSet docids = u.idSet();
	String[] regs = listCat(docids, freg).toArray(new String[0]);
	Arrays.sort(regs);
	suite.addDiscrimination(RcvToXML.REGION, regs);

	String[] inds = listCat(docids, find).toArray(new String[0]);
	Arrays.sort(inds);
	suite.addDiscrimination(RcvToXML.INDUSTRY);	
	suite.addClass(RcvToXML.INDUSTRY, RcvToXML.NONE).makeDefault();
	for(String cat: inds) suite.addClass(RcvToXML.INDUSTRY, cat);

	System.out.println("Suite of Discriminations: " + suite.describe());
	suite.saveAsXML(out + File.separator +"rcv-suite.xml");

    }

    static int[] getCatCounts( Vector<Pair> v) {
	int rcnt[]=new int[10];
	for( Pair p: v) { 
	    if (p.catCnt>=rcnt.length) rcnt = Arrays.copyOf(rcnt, p.catCnt+1);
	    rcnt[p.catCnt]++; 
	}
	return rcnt;
    }


    static class Pair { 
	int id, catCnt;
	Pair(int _id, int _cnt) { id=_id; catCnt=_cnt; }
	void incCatCnt() { catCnt++; }
    }

    static class PairList extends Vector<Pair> {

	//PairList() { super(); }
	//PairList( PairList x) { super(x); }

	PairList filterByCnt( int cnt) {
	    PairList b = new PairList();
	    for( Pair p: this) { 
		if (p.catCnt==cnt) b.add(p);
	    }
	    return b;	
	}

	/** Pairs from v whose IDs in both lists */
	PairList intersect( Vector<Pair> t) {
	    PairList w = new PairList();
	    int i=0, it=0;
	    while(i<size() && it<t.size()) {
		if (elementAt(i).id < t.elementAt(it).id) i++;
		else if (elementAt(i).id == t.elementAt(it++).id) {
		    w.add( elementAt(i++));
		}
	    }
	    return w;
	}


	/** Pairs from r whose IDs is not in t */
	PairList difference(Vector<Pair> t) {
	    PairList w = new PairList();
	    int i=0, it=0;
	    while(i<size() && it<t.size()) {
		if (elementAt(i).id < t.elementAt(it).id) {
		    w.add( elementAt(i++));
		} else if (elementAt(i).id == t.elementAt(it++).id) {
		    i++;
		}
	    }
	    // any leftovers 
	    while(i<size()) w.add( elementAt(i++));
	    return w;
	}

	/** */
	PairList union(Vector<Pair> t) {
	    PairList w = new PairList();
	    int i=0, it=0;
	    while(i<size() && it<t.size()) {
		if (elementAt(i).id < t.elementAt(it).id) {
		    w.add( elementAt(i++));
		} else if (elementAt(i).id == t.elementAt(it).id) {
		    w.add( elementAt(i++));
		    it++;
		} else {
		    w.add( t.elementAt(it++));
		}
	    }
	    // Now, only one vector has unused elements left
	    while(i<size())  w.add( elementAt(i++));
	    while(it<t.size())  w.add( t.elementAt(it++));

	    return w;
	}

	/** Write ids to file */
	void write(String f) throws IOException { 
	    PrintWriter w = new PrintWriter(new File(f));
	    for(Pair p: this) {
		w.println("" + p.id);
	    }
	    w.close();
	}

	/** Creates a new array with random ordering of elements */
	PairList randomize() {
	    int n = size();
	    // array of indexes into this[]
	    int a[] = new int[ n];
	    for(int i=0; i<n; i++) a[i]=i;

	    PairList w = new PairList();

	    Random r = new Random();
	    int zeroCnt=0;
	    while( w.size() < n) {
		int pos = 0;
		do {
		    pos=(int)(r.nextDouble() *  a.length);
		} while( a[pos]<0 );
		w.add(elementAt(a[pos]));
		a[pos]= -1;
		zeroCnt++;
		if (zeroCnt * 2 > a.length) {
		    // delete all zeros from a[] every now and then
		    int b[] = new int[a.length - zeroCnt];
		    int j=0;
		    for(int q: a) {
			if (q>=0) b[j++] = q;
		    }
		    a = b;
		    zeroCnt=0;
		}
	    }
	    return w;
	}

	IntSet idSet() {
	    IntSet h=new IntSet( lastElement().id+1 );
	    for(Pair p: this) h.add( new Integer(p.id));
	    return h;
	}

    }
    
    /** More efficient for fairly dense sets than HashSet<Integer> */
    static class IntSet {
	BitSet b;
	/** Creates a set that will be able hold elements with values
	 * from 0 thru n-1 */
	IntSet(int n) { b = new BitSet(n); }
	void add(int x) { b.set(x); }
	boolean contains(int x) { return b.get(x); }
    }

    /** Reads a QREL file
       @return A lits of (docidCnt) pairs
	*/
    static  PairList  readQREL(String fname)	throws IOException    {
	PairList v = new PairList();
	int cnt=0;
	for(QrelIterator it = new QrelIterator(fname); it.hasNext(); ) {
	    QrelEntry q = it.next();
	    cnt++;
	    if (v.size() == 0 || 
		v.lastElement().id < q.docid) {
		v.add( new Pair(q.docid, 1));
	    } else if (v.lastElement().id == q.docid) {
		v.lastElement().incCatCnt();
	    } else {
		throw new IllegalArgumentException("Doc id " + q.docid + " out of order");
	    }
	}
	System.out.println("Line count=" + cnt);
	return v;
    }
   
    /** Returns a set of all categories that occur in the QREL file
     *  in association with document ids from a specified set
     @param fname QREL File to read
     @docs List of doc ids. The function only looks at the lines in the QREL file where the doc id is in this list
     */

    static HashSet<String> listCat(IntSet docs, String fname)
	throws IOException
    {
	HashSet<String> cats = new HashSet<String>();
	for(QrelIterator it = new QrelIterator(fname); it.hasNext(); ) {
	    QrelEntry q = it.next();
	    if (docs.contains(new Integer(q.docid))) cats.add(q.cat); 
	}
	return cats;
    }

}	

