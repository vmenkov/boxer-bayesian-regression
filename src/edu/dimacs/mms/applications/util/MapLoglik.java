package edu.dimacs.mms.applications.util;

import java.util.*;
import java.io.*;
import java.text.*;
import org.w3c.dom.Element;

import edu.dimacs.mms.boxer.*;
import edu.dimacs.mms.borj.*;

/**  This application produces a file that shows the values of the
   log-likelyhood at various values of the PLRM matrix. That file
   then can be used in gnuoplot to map log-likelihood as the function
   of the PLRM matrix.

   This application can only be used with binary problems (2 classes
   only). Thus, it generates matrices of two columns, the second
   column being the negative of the first.

     <p>
     Usage:<br>
     java [-Drange=1.0] [-Dcells=10] [-Ddic=dic.xml] [-Dfeature.name1=fixed_value1 -Dfeature.name2=from_va2:to_val2 ...] MapLoglik suite.xml training_set.xml out-file.txt


 */
public class MapLoglik {

    
    static void usage() {
	usage(null);
    }

    static void usage(String m) {
	System.out.println("This is the MapLoglik Application for the BOXER toolkit (version " + Version.version+ ")");
	if (m!=null) {
	    System.out.println(m);
	}
	System.exit(1);
    }

    static boolean verbose=false;
    
    static public void main(String argv[]) 
	throws IOException, org.xml.sax.SAXException, BoxerXMLException {

	if (argv.length!=3) usage();
	ParseConfig ht = new ParseConfig();

	double range = ht.getOptionDouble("range", 1.0);
	int cells =ht.getOption("cells", 10);	
	
	String suiteFile=argv[0], dataFile=argv[1], outFile=argv[2];
	Element suiteXML =  ParseXML.readFileToElement(suiteFile);

	Suite suite = (suiteXML!=null)? new Suite(suiteXML): new Suite("Test_suite");

	// A dictionary may be provided explicitly, to ensure the
	// desired ordering of features
	String dicFileName =ht.getOption("dic", null);		
	if ( dicFileName != null) {
	    suite.setDic(new  FeatureDictionary(new File(dicFileName)));
	}

	Vector<DataPoint> train = ParseXML.readDataFileMultiformat(dataFile, suite, true);
	
	// FIXME: Properly, we just need to create a model, not a full learner
	TruncatedGradient algo = new  TruncatedGradient(suite);

	// "train" the learner. This is done just to make sure we
	// absorb all features into our dictionary. The learner's
	// matrix won't matter, as it will be erased later.
	algo.absorbExample(train);
		    

	Discrimination dis = null;
	try {
	    dis = suite.lookupSimpleDisc();
	} catch(Exception ex) {
	    System.out.println("Can't identify the 'only' discrimination");
	    return;
	}
	int did = suite.getDid(dis);

	Learner.LearnerBlock block = algo.findBlockForDis(dis);
	BetaMatrix w = (BetaMatrix)(((PLRMLearner.PLRMLearnerBlock)block).getW());

	FeatureDictionary dic = suite.getDic();
	int d=dic.getDimension();

	int[] indexes = new int[d], first = new int[d], last = new int[d];
	// centers of range
	double [] center = new double[d];
	double[] vec= new double[d], step= new double[d];

	int firstVaryingIndex = -1;

	for(int i=0; i<indexes.length; i++) {
	    String specs = ht.getOption("feature." + dic.getLabel(i), null);
	    double min = -range, max= range;
	    if (specs==null) {
		center[i] = 0;
		first[i] = -cells;
		last[i] = cells;
	    } else {
		String[] z= specs.split(":");
		if (z.length == 1) {
		    center[i] = min = max = Double.parseDouble(z[0]);
		    first[i] = last[i]= 0;
		} else if (z.length==2) {
		    min =Double.parseDouble(z[0]);
		    max =Double.parseDouble(z[1]);
		    center[i] = 0.5 * (min + max);
		    first[i] = -cells;
		    last[i] = cells;		    
		} else {
		    usage("Range specs should be of the form -Dfeature.name=val or -Dfeature.name=val1:val2"); 
		}
	    }
	    indexes[i] = first[i];
	    step[i] = (max-min) / (2*cells);
	    if ( firstVaryingIndex < 0 && min!=max) {
		firstVaryingIndex = i;    
		System.out.print("(*) ");
	    }
	    System.out.println("Feature["+i+"], '"+dic.getLabel(i)+"', range=" + min + " : " + max);
	}

	
	PrintWriter sw = new PrintWriter( new FileWriter(outFile));

	sw.print("#");
	for(int  i=0; i<indexes.length; i++) {
	    sw.print( dic.getLabel(i) + "\t");
	}
	sw.println(	  "Loglik");    

	// Compute the log-lik for all points of the grid in the Beta-vector space
	int prev = indexes[ firstVaryingIndex];
	do {
	    // line breaks between sets are needed by gnuplot
	    boolean needLineBreak = (prev != indexes[ firstVaryingIndex]);
	    prev = indexes[ firstVaryingIndex];
	    if (needLineBreak) sw.println("");

	    for(int  i=0; i<indexes.length; i++) {
		vec[i] = center[i] + indexes[i] * step[i];
	    }
	    // set all rows of the matrix
	    for(int  i=0; i<indexes.length; i++) {
		Vector<BetaMatrix.Coef> row = new Vector<BetaMatrix.Coef>();
		row.add(new BetaMatrix.Coef(0, vec[i]));
		row.add(new BetaMatrix.Coef(1, -vec[i]));
		w.setElements(i, row);
	    }
	    Scores scores = score(algo, train);
	    for(int  i=0; i<indexes.length; i++) {
		sw.print("" + vec[i] + "\t");
	    }
	    sw.println(	    scores.logLik[did]/scores.likCnt[did]);    
	} while ( increment(indexes, first, last));
	sw.close();

    }

    /** "Increments" a position (a set of indexes) in the multi-dim space.
	@return true if increment was possible, false if it
	was already the last value
     */
    static boolean increment(int[] indexes,final int[] first,final int[] last) {
	for(int j=indexes.length-1; j>=0; j--) {
	    indexes[j]++;
	    if (indexes[j] <= last[j]) return true;
	    indexes[j]= first[j];
	}
	return false;
    }

    static private Scores score(Learner algo,
			       Vector<DataPoint> train)
	throws java.io.IOException,  
    //org.xml.sax.SAXException, 
	       BoxerXMLException {

	Suite suite = algo.getSuite();

	Scores scoresTrain = new Scores(suite);
	  
	// Score the entire training set
	for(int i=0; i< train.size(); i++) {
	    DataPoint x = train.elementAt(i);
	    // overcoming underflow...
	    double [][] probLog = algo.applyModelLog(x);
	    double [][] prob = expProb(probLog);
	    
	    if (Suite.verbosity>1) {
		System.out.println("Scored training vector "+i+"; scores=" +
				   x.describeScores(prob, suite));
	    }
	    
	    //if (sw!=null) x.reportScoresAsText(prob,algo,runid,sw);
	    
	    scoresTrain.evalScores(x, suite, prob);
	    x.addLogLinLik(probLog, prob, suite, 
			   scoresTrain.likCnt,
			   scoresTrain.logLik, scoresTrain.linLik);			
	}
	return scoresTrain;
		
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

    /** Computes exponent of each array element */
    private static double [][] expProb(double[][] probLog) {
	double [][] prob = new double[probLog.length][];
	for(int j=0; j<prob.length;j++) {
	    double [] v = probLog[j];
	    prob[j] = new double[v.length];
	    for( int k=0; k< v.length; k++) {
		prob[j][k] = Math.exp(v[k]);
	    }
	}
	return prob;
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
