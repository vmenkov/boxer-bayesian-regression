package edu.dimacs.mms.accutest;

import java.util.*;
import java.io.*;
import java.text.*;
import org.w3c.dom.Element;

import edu.dimacs.mms.boxer.*;
import edu.dimacs.mms.borj.*;

/**  This is a sample application for measuring classifier quality
     on increasing training-set size, as per discussion 
     with Paul Kantor in October 2009.

    <pre>
java [-Dmodel=tg|eg|trivial] [-Dverbose=true | -Dverbosity={0...3}] [-Drunid=RUN_ID]  test.Driver command:file [command:file ...]
    </pre>

    The command line can contain any number of commands. They are
    executed sequentially, in the order they appear in the command
    line. Each command is followed by the file name (or sometimes file
    names) it applies to <p>

    The type of the learning model, and certain other options or it, 
    can be specified as Java system properties, with the -D command-line
    flag.
    <p>

    The following command are presently supported:

    <li>read-suite: Read the description of a "{@link edu.dimacs.mms.boxer.Suite
    suite}" (basically, a list of discriminations, and the classes of
    each one) from an XML file. Such file can be created manually, or
    generated by a program. This command is useful if you want to
    pre-define your discriminations (instead of having BOXER infer
    their structure from the labels it finds in the first training
    file); if you use it, it makes sense to use it as the first
    command on the command line, so that the suite will be actually used
    in constructing the model.

    <li>read-learner: Reads an XML file with a single <tt>learner</tt>
    element. That creates a new learner in the context of the existing
    suite (if no suite has been read in, an empty one will be created
    to start you on). The <tt>learner</tt> may contain just the
    algorithm parameters, or also (optionally) the initial state of
    the learner.


    <li> train: Read the examples from the XML file specified, and
    train the classifier on it. Any number of the "train" commands may
    appear in any position on the command line, the model being
    incrementally trained on each one. The examples in the file may
    contain both features and labels, or only features; in the latter
    case, the labels should have been pre-loaded with an earlier
    "read-labels" command.

    <li>test: Apply the model to each example from the XML specified
    file.  Any number of the "test" commands may appear in any
    position on the command line, although of course you may not want
    to use one before you've trained the model (or have read in a
    pre-computed model). As with the "train" command, the examples in
    the file may contain both features and labels, or only features;
    in the latter case, the labels should have been pre-loaded with an
    earlier "read-labels" command.<br>

    This command can also be used with two arguments,
    <tt>test:<em>test-set.xml</em>:<em>score-output-file.txt</em></tt>,
    to make BORJ save the test examples' scores to a names file.

<h3>Command line examples</h3>

<p>
 (A): Read a suite (ontology) file from suite.xml, read algo params
 from tg-param.xml, train on a train set, test on a test set, and dump
 the current learner state (complete with suite etc) into learner-blob.xml:

<p><tt>
read-suite:suite.xml read-learner:tg-param.xml train:train-set.xml test:test-set.xml write:learner-blob.xml
</tt>
 
<p>
(B): Read an algo description; start with an empty suite, learning on a training file (filling one's suite based on the labels in it); score a training set, saving scores to text file; save just the suite

<p><tt>
read-learner:tg-param.xml  train:train-set.xml test:test-set.xml:save-scores.txt write-suite:suite-out.xml 
</tt>

<h3>See also</h3>

<ul>
<li>
 <a href="../boxer/doc-files/tags.html">Overview of the XML elements used by BOXER</a>
</ul>

 */
public class Driver {

    static void usage() {
	usage(null);
    }

    static void usage(String m) {
	System.out.println("This is the Learner Quality Test Application for the BOXER toolkit (version " + Version.version+ ")");
	System.out.println("Usage: java [options] test.Driver command:file [command:file] ...");
	System.out.println("For example:");
	System.out.println("  java [options] borj.Driver [train:]train.xml [test:]test.xml");
	System.out.println("  java [options] borj.Driver [read-suite:suite-in.xml] train:train1.xml train:train2.xml test:test_a.xml train:train3.xml test:test_b.xml [write:model-out.xml]");
	System.out.println(" ... etc.");
	System.out.println("Optons:");
	System.out.println(" [-Dmodel=eg|tg|trivial] [-Drunid=RUN_ID] [-Dverbose=true|false | -Dverbosity=0|1|2|3] [-DM=1] [-Drandom=100]");
	System.out.println("See Javadoc for borj.Driver for the full list of commands.");
	if (m!=null) {
	    System.out.println(m);
	}
	System.exit(1);
    }

    /** Auxiliary class responsible for parsing command line */
    static private class CmdManager {
        private CMD [] cm;

        /** Pointer to the first not-yet parsed cmd line argument */
        private int h = 0;
	/** Initializes the command manager by parsing the entire command line */
        CmdManager(String [] argv) {
	    // we only have one cmd here, and it takes 2 arg
	    CMD.setTwoArgCmd(CMD.TRAIN); 
            cm = CMD.parse(argv);
        }
	/** Are there more commands?*/
        boolean hasNext() {
            return h < cm.length;
        }
	/** Gets the next command (or null, if none is left) */
        CMD next() {
            return hasNext()? cm[h++]:  null;
        }
    }

    
    /** Produces default run id, if none is supplied */
    static private String mkRunId() {
	Calendar now = Calendar.getInstance();	
	long time = now.getTimeInMillis();
	System.out.println("[TIME][START] " +
			   DateFormat.getDateInstance().format(now.getTime()) +
			   " ("+time+")");
	return "" +time;
    }

    static String out, runid;
    static boolean verbose=false;
    
    static public void main(String argv[]) 
	throws IOException, org.xml.sax.SAXException, BoxerXMLException {

	memory("ACCURACY TEST startup");
	if (argv.length==0) usage();
	ParseConfig ht = new ParseConfig();

	int M =ht.getOption("M", 10);	
	out =ht.getOption("out", ".");	
	if (out.equals("")) out=".";

	System.out.println("Welcome to the BOXER toolkit (version " + Version.version+ "). M="+M +", out="+out);
	System.out.println("[VERSION] " + Version.version);

	Suite.verbosity = ht.getOption("verbosity", 1);
	verbose = ht.getOption("verbose", (Suite.verbosity>=3));

	runid = ht.getOption("runid", mkRunId());
	DataPoint.setDefaultNameBase(runid);

	int nRandom= ht.getOption("random", 0);

	Element suiteXML=null, learnerXML = null;
	String defaultModel=ht.getOption("model", "tg");	

	/** Pointer into argv array */
	//-----------------
	CmdManager cm  = new CmdManager(argv);
	CMD q = cm.next();

	if (q!=null && q.is(CMD.READ_SUITE)) {
	    System.out.println("Reading discrimination suite from file: "+q.f);
	    suiteXML =  ParseXML.readFileToElement(new File(q.f));
	    q = cm.next();
	}

	// Any "read-learner" commmands?
	if (q!=null && q.is(CMD.READ_LEARNER)) {
	    System.out.println("Getting a learner from file: "+q.f);
	    learnerXML = ParseXML.readFileToElement(new File(q.f));
	    q=cm.next(); 
	}

	if (q==null || !q.is(CMD.TRAIN)) {
	    throw new AssertionError("Expected a 'train:' command!");
	}
	String trainFile = q.f;
	String scoreFileBase = (q.f2==null) ? "scores.tmp" : q.f2;
	q = cm.next();

	if (q!=null)  throw new AssertionError("There is an unused command left: " + q);


	Suite suite = (suiteXML!=null)? new Suite(suiteXML): new Suite("Test_suite");

	System.out.println("Reading data set ("+trainFile+")");
	Vector<DataPoint> train = ParseXML.readDataFileXML(trainFile, suite, true);

	//------------------------


	if (nRandom ==0) {
	    runOneOrdering(suite, learnerXML, defaultModel, 
			   trainFile, train, 
			   scoreFileBase,  M, -1);
	} else {
	    System.out.println("Training on " +nRandom+ " random permutations");
	    for(int r=0; r<nRandom; r++) {
		runOneOrdering(suite, learnerXML, defaultModel, 
			       trainFile, train, 
			       scoreFileBase,  M, r);
		suite.deleteAllLearners();
	    }	    
	}

	memory("Finished");
	Calendar now = Calendar.getInstance();	
	System.out.println("[TIME][FINISH] " +
			   DateFormat.getDateInstance().format(now.getTime()) +
			   " ("+	now.getTimeInMillis()+")");

	
    }


    /**
       @param seed The seed for random number generator. If negative,
       don't randomize; use the original sequence.
       
       @param trainFile the name of the train file. It is only used
       for labels, not for actual reading.

       @param origTrain Pre-read training+test set
    */
    static private void runOneOrdering(Suite suite, Element learnerXML, 
				       String defaultModel,
				       String trainFile, 
				       Vector<DataPoint> origTrain, 
				       String scoreFileBase,
				       int M, long seed )
	throws java.io.IOException,  org.xml.sax.SAXException, BoxerXMLException {

	Vector<DataPoint> train = origTrain;
	if (seed >= 0) {
	    int[] perm = randomPermutation(origTrain.size(), seed);
	    train = new Vector<DataPoint>(origTrain.size());
	    for(int i=0; i<perm.length; i++) train.addElement( origTrain.elementAt( perm[i]));
	}
	
	// Any "read-learner" commmands?
	if (learnerXML != null) {
	    suite.addLearner(learnerXML);
	} else {
	    // Creating a "blank" model if none has been read in 
	    String model=defaultModel;
	    System.out.println("Default model name: " + model);
	    if (model.equals("eg")) {
		Learner algo = new ExponentiatedGradient(suite);
		//usage("eg Not supported now");
	    } else 	if (model.equals("tg")) {
		Learner algo = new TruncatedGradient(suite);
	    } else 	if (model.equals("trivial")) {
		Learner algo = new TrivialLearner(suite);
	    } else {
		usage("Unknown model `"+model+"'");
	    }
	}


	int nLearners =  suite.getLearnerCount();
	if (nLearners != 1) throw new AssertionError("nLearners="+nLearners+" There must be exactly one learner!");

	Learner algo= suite.getAllLearners().elementAt(0);

	if (Suite.verbosity>0) {
	    System.out.println("Describing the learner:");
	    algo.describe(System.out, false);
	    System.out.println("-----------------------------------");
	}

	
	int trainCnt = 0, testCnt=0;

	if (Suite.verbosity>0) System.out.println(suite.describe());
	if (verbose) System.out.println(suite.getDic().describe());
	if (Suite.verbosity>0) System.out.println("Training set ("+trainFile+") contains " + train.size() + " points, memory use=" + Sizeof.sizeof(train) + " bytes");
	for(int i =0; i<train.size(); i++) {
	    if (verbose) System.out.println(train.elementAt(i));
	}

	if (train.size() < 2*M) {
	    throw new AssertionError("Data set is to small ("+train.size()+") to effectively carry out both training and testing. We expect the set to have at least " + 2*M + " examples");
	}

	int[] trainSizes =  new int[ train.size()/(2*M)];

	for(int k=0; k< trainSizes.length; k++)  trainSizes[k] = (k+1)*M;

	// train
	if (Suite.verbosity>0) memory("Read train set; starting to train");

	NumberFormat fmt = new DecimalFormat("0000");

	for(int k=0; k< trainSizes.length; k++)  {

	    // absorb examples i1 thru i2-1
	    int i1= (k==0) ? 0 :  trainSizes[k-1];
	    int i2= trainSizes[k];
	    algo.absorbExample(train, i1, i2);

	    if (Suite.verbosity>1) {
		System.out.println("Describing the Learner after "+i2 + " training examples");
		algo.describe(System.out, false);
		System.out.println("-----------------------------------");
	    } else if (Suite.verbosity>0){
		System.out.println("[NET] Leaner after " + (i2)+ " examples: net memory use=" + algo.memoryEstimate());
	    }
	    // In verbose mode, write out the model after every training file
	    if (verbose) algo.saveAsXML(algo.algoName() + "-out" + trainCnt + ".xml");

	    if (Suite.verbosity>0) memory("Absorbed "+(i2-i1)+" examples from "+trainFile);
	    // Now, score all vectors in the REST of the set 


	    if (Suite.verbosity>0) System.out.println("After training the learner on the first " + i2 + " examples, applying it to the other " + (train.size()-i2) + " examples");
		
	    PrintWriter sw = null;
	    if (Suite.verbosity>0 && scoreFileBase != null) {
		String scoreFile = scoreFileBase + "." + fmt.format(i2);
		System.out.println("Scores will go to text file ("+
				   scoreFile+")");
		sw = new PrintWriter( new FileWriter(scoreFile));
	    }
	    Scores scoresTrain = new Scores(suite);
	    Scores scoresTest = new Scores(suite);
	  
	    // Score all the training examples used so for, to measure the
	    // log-likelyhood
	    for(int i=0; i< i2; i++) {
		DataPoint x = train.elementAt(i);
		// overcoming underflow...
		double [][] probLog = algo.applyModelLog(x);
		double [][] prob = expProb(probLog);
		
		if (Suite.verbosity>1) {
		    System.out.println("Scored training vector "+i+"; scores=" +
				       x.describeScores(prob, suite));
		}

		if (sw!=null) x.reportScoresAsText(prob,algo,runid,sw);

		scoresTrain.evalScores(x, suite, prob);
		x.addLogLinLik(probLog, prob, suite, 
			       scoresTrain.likCnt,
			       scoresTrain.logLik, scoresTrain.linLik);			
	    }

	    // score all the *remaining* (i.e., not yet seen) examples,
	    // to measure the "accuracy" (as per PK, 2009-10)
	    for(int i=i2; i< train.size(); i++) {
		DataPoint x = train.elementAt(i);
		// overcoming underflow...
		double [][] probLog = algo.applyModelLog(x);
		double [][] prob = expProb(probLog);
		if (Suite.verbosity>1) {
		    System.out.println("Scored test vector "+i+"; scores=" +
				       x.describeScores(prob, suite));
		}
		if (sw!=null) x.reportScoresAsText(prob,algo,runid,sw);
		scoresTest.evalScores(x, suite, prob);
		/** Adding prob, instead of probLog, because it's not 
		    logarithmized */
		x.addLogLinLik(probLog, prob, suite, 
			    scoresTest.likCnt, 
			    scoresTest.logLik,  scoresTest.linLik);

	    }


	    if (sw != null) sw.close();
	    int ts = train.size() - i2;

	    // Print report on scores so far
	    if (Suite.verbosity>=0) {
		String label = trainFile+": " + seed + " : " + i2 +  "+" + ts;

		System.out.println("Scoring report (file "+trainFile+", seed="+seed+"):");
		
		System.out.print(scoresTrain.scoringReport(suite, "[TRAIN SCORES]["+label+"]"));
		System.out.print(scoresTrain.likReport2(suite, "[TRAIN LOG LIN]["+label+"]"));
		System.out.println(scoresTrain.wAvgRecallReport(suite, "[TRAIN WARECALL]["+label+"]"));


		System.out.print(scoresTest.scoringReport(suite, "[TEST SCORES]["+label+"]"));
		System.out.print(scoresTest.likReport2(suite, "[TEST LOG LIN]["+label+"]"));
		System.out.println(scoresTest.wAvgRecallReport(suite, "[TEST WARECALL]["+label+"]"));
		
	    }
	

	    if (Suite.verbosity > 0) memory("Scored "+ts+" examples from "+trainFile);

	    if (Suite.verbosity>=0) {
		/*
		for(int i=0; i<se.length; i++) {
		    if (se[i].size()==0) continue; // skip empties
		    System.out.println("Scoring report (GLOBAL) for learner no. "+i+":");
		    System.out.println(se[i].scoringReport(suite, "[SCORES][GLOBAL] "));
		    System.out.println(se[i].loglikReport(suite, "[LOGLIK][GLOBAL]"));
		}
		*/
	    }

	    if (Suite.verbosity>=0 && seed<0) {
		suite.serializeLearnerComplex(out + "/out-suite."+ fmt.format(i2) +".xml"); // save the entire model
	    }

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

    /** Returns an array of integers, from 0 to n-1, randomly permuted.
	@param seed The seed for the random number generator. 
     */
    static int[] randomPermutation(int n, long seed) {
	Random gen = new Random(seed); 
	boolean[] listed=new boolean[n];
	int p[] = new int[n];
	for(int i=0; i<n; i++) {
	    int v;
	    do {
		v = gen.nextInt(n);
	    } while (listed[v]);
	    p[i] = v;
	    listed[v]= true;
	}
	return p;
    }

    static int[] identityPermutation(int n) {
	int p[] = new int[n];
	for(int i=0; i<n; i++) {
	    p[i] = i;
	}
	return p;
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
