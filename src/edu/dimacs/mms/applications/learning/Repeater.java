package edu.dimacs.mms.applications.learning;

import java.util.*;
import java.io.*;
import java.text.*;
import org.w3c.dom.Element;

import edu.dimacs.mms.boxer.*;
import edu.dimacs.mms.borj.*;

/**  This is a sample application for measuring classifier quality
     after repeatingly feeding the same examples to the learner, in
     a fixed or random order.

     <p>
     Usage:<br>
     java  Repeater [-Dsd=true|false] [-Dr=1000] [-DM=10] [-Ddic=dic.xml] [read-suite:suite.xml] [read-learner:learner-param.xml] train:train-set.xml[:train-scores.dat] test:test-set.xml[:test-scores.dat]

     <p>
     Sample usage:

     <pre>
     set driver=edu.dimacs.mms.applications.learning.Repeater
     java $opt -Dout=${out} -DM=10 -Dr=5000 -Drandom=$nr -Dsd=false -Dverbosity=0 $driver \
       read-suite: SimpleTestSuite.xml    read-learner: $learner  \
       train: SimpleTestData-part-1.xml  test: SimpleTestData-part-2.xml 
     </pre>

     <p>Options: 

     <ul> 

     <li>r: The length of the training sequence that will be presented
	to the learner. This value may be (and typically will be) greater
	than the size of the training set, as examples may be
	presented repeatedly.


     <li>random: Number of random sequences (1 or more) with which to
     run experiments. If 0 is given instead, it means that there will
     be only one sequence, and it will be <strong>non-random</strong>,
     cyclic one.

     <li>M: frequency of checkpoints. (I.e., how often to pause
     training and test the classifier against the training and test set)

     <li>sd: if true, we emulate the Steepest Descent (SD), a batch method. The default is false.

     <li>adaptive: if true (and sd is also true), run SD with adaptive learning rate (ASD) to convergence

     <li>eps=1e-8: convergence criterion for ASD

     </ul>


 */
public class Repeater {

    
    static boolean emulateSD = false, adaptiveSD=false;
    static double eps;
 

    static void usage() {
	usage(null);
    }

    static void usage(String m) {
	System.out.println("This is the Repeater Test Application for the BOXER toolkit (version " + Version.version+ ")");
	/*
	System.out.println("Usage: java [options] test.Driver command:file [command:file] ...");
	System.out.println("For example:");
	System.out.println("  java [options] borj.Driver [train:]train.xml [test:]test.xml");
	System.out.println("  java [options] borj.Driver [read-suite:suite-in.xml] train:train1.xml train:train2.xml test:test_a.xml train:train3.xml test:test_b.xml [write:model-out.xml]");
	System.out.println(" ... etc.");
	System.out.println("Optons:");
	System.out.println(" [-Dmodel=eg|tg|trivial] [-Drunid=RUN_ID] [-Dverbose=true|false | -Dverbosity=0|1|2|3] [-DM=1] [-Drandom=100] [-Dsd=false]");
	System.out.println("See Javadoc for borj.Driver for the full list of commands.");
	*/
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
	    CMD.setTwoArgCmd(new String[]{CMD.TRAIN, CMD.TEST}); 
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

	memory("REPEATER startup");
	if (argv.length==0) usage();
	ParseConfig ht = new ParseConfig();


	emulateSD = ht.getOption("sd", false);
	adaptiveSD = ht.getOption("adaptive", false);
	eps = ht.getOptionDouble("eps", 1e-8);

	if (adaptiveSD && !emulateSD) usage("-Dadaptive=true may only be used with -Dsd=true");

	int M =ht.getOption("M", 1);	
	int r =ht.getOption("r", 1000);
	int nRandom= ht.getOption("random", 0);
	boolean cyclic = (nRandom <= 0);  // cyclic (non-random) mode
	if (cyclic) nRandom = 1;
	
	long start= (long)ht.getOption("start", 0);

	
	out =ht.getOption("out", ".");	
	if (out.equals("")) out=".";

	System.out.println("Welcome to the BOXER toolkit (version " + Version.version+ "). "+
			   (cyclic? "Cyclic mode" :
			    "Random mode with "+nRandom+"repeats")+
			   ", M="+M +", sd="+emulateSD+", adaptiveSD="+adaptiveSD+", out="+out);
	System.out.println("[VERSION] " + Version.version);

	Suite.verbosity = ht.getOption("verbosity", 1);
	verbose = ht.getOption("verbose", (Suite.verbosity>=3));

	runid = ht.getOption("runid", mkRunId());
	DataPoint.setDefaultNameBase(runid);

	Element suiteXML=null, learnerXML = null;
	String defaultModel=ht.getOption("model", "tg");	

	CmdManager cm  = new CmdManager(argv);
	CMD q = cm.next();

	if (q!=null && q.is(CMD.READ_SUITE)) {
	    System.out.println("Reading discrimination suite from file: "+q.f);
	    suiteXML =  ParseXML.readFileToElement(new File(q.f));
	    q = cm.next();
	}

	Suite suite = (suiteXML!=null)? new Suite(suiteXML): new Suite("Test_suite");

	String dicFileName =ht.getOption("dic", null);	
	
	if ( dicFileName != null) {
	    suite.setDic(new  FeatureDictionary(new File(dicFileName)));
	}


	// Any "read-priors" command?
	if (q!=null && q.is(CMD.READ_PRIORS)) {
	    System.out.println("Reading priors from file: "+q.f);
	    //Priors p = new Priors(new File(q.f), suite);
	    Priors p = Priors.readPriorsFileMultiformat(new File(q.f), suite);

	    suite.setPriors(p);
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
	String scoreFileBaseTrain = (q.f2==null) ? null : q.f2;
	q = cm.next();


	if (q==null || !q.is(CMD.TEST)) {
	    throw new AssertionError("Expected a 'test:' command!");
	}
	String testFile = q.f;
	String scoreFileBase = (q.f2==null) ? "scores.tmp" : q.f2;
	q = cm.next();

	String dumpLearnerFile = null;
	if  (q!=null && q.is(CMD.WRITE)) {
	    dumpLearnerFile = q.f;
	    q=cm.next(); 
	}

	if (q!=null)  throw new AssertionError("There is an unused command left: " + q);

	System.out.println("Reading data set ("+trainFile+")");
	Vector<DataPoint> train = ParseXML.readDataFileMultiformat(trainFile, suite, true);
	Vector<DataPoint> test = ParseXML.readDataFileMultiformat(testFile, suite, false);

	for(long seed=start; seed< nRandom; seed++) {

	    runOneOrdering(suite, learnerXML, 	defaultModel,
			   trainFile,   train,    testFile, 	test,
			   scoreFileBaseTrain,    scoreFileBase,    
			   M, r,  (cyclic? -1: seed));

	    if (seed == start && dumpLearnerFile != null) {
		System.out.println("Saving the learner(s) from the 1st run to file: "+dumpLearnerFile );
		suite.serializeLearnerComplex(dumpLearnerFile ); // save the entire model
	    }

	    suite.deleteAllLearners();
	}

	memory("Finished");
	Calendar now = Calendar.getInstance();	
	System.out.println("[TIME][FINISH] " +
			   DateFormat.getDateInstance().format(now.getTime()) +
			   " ("+	now.getTimeInMillis()+")");

	
    }


    /** Creates a learner as per the specified description, and trains
	it on increasingly long sequences of examples from the
	training set. At regular intervals ("checkpoints") during the
	training, scores the entrie training set (including
	never-presented-yet examples) and the test set by the learner
	as it stands at the moment.

	<p> During the training, examples are presented to the learner
	either in random order, or in cyclic deterministic order.

	@param r The length of the training examples sequence to be
	presented to the learner. This value may be (and typically is)
	greater than the size of the training set, as examples may be
	presented repeatedly.

	@param seed The seed for random number generator, determining
	the order in which training examples are presented to the
	learner. If negative, don't randomize; use the original
	sequence, cycling repeatedly over the training set.
       
	@param learnerXML The description of the learner to be created.
	
	@param defaultModel Controls the learner type if learnerXML==null.
	
	@param trainFile the name of the train file. It is only used
	for labels, not for actual reading.
	
	@param origTrain Pre-read training+test set
    */
    static private void runOneOrdering(Suite suite, Element learnerXML, 
				       String defaultModel,
				       String trainFile, 
				       Vector<DataPoint> train, 
				       String testFile, 
				       Vector<DataPoint> test,
				       String scoreFileBaseTrain,
				       String scoreFileBase,
				       int M, int r, long seed )
	throws java.io.IOException,  org.xml.sax.SAXException, BoxerXMLException {

	if (emulateSD && !adaptiveSD && M%train.size() != 0) {
		throw new IllegalArgumentException("In the 'emulate SD' mode the checkpoint distance M must be a multiple of the data set size ("+train.size()+")");
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
	    System.out.println("Describing the Learner:");
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

	int[] trainSizes =  new int[ r / M ];

	Random gen = (seed<0) ? null : new Random(seed); 

	if (emulateSD && gen != null) {
	    throw new IllegalArgumentException("In the 'emulate SD' mode we CANNOT randomize!");
	}


	for(int k=0; k< trainSizes.length; k++)  trainSizes[k] = (k+1)*M;

	// train
	if (Suite.verbosity>0) memory("Read train set; starting to train");

	NumberFormat fmt = new DecimalFormat("0000");

	// Matrix is only saved when it is the only run of a cyclic
	// repeater (seed<0), or the first run of the random repeater (seed=0)	
	boolean saveMatrix = (Suite.verbosity>0 && seed<=0 && algo instanceof PLRMLearner);

	PrintWriter matWriter = null;
	if (saveMatrix) {
	    matWriter = new PrintWriter(new FileWriter(new File( out + "/matrix.dat")));
	}

	for(int k=0; k< trainSizes.length; k++)  {

	    // absorb examples i1 thru i2-1
	    int i1= (k==0) ? 0 :  trainSizes[k-1];
	    int i2= trainSizes[k];

	    if (emulateSD) {
		// This is essentially a batch method, which must swallow the
		// entire training set at once.

		if (!(algo instanceof TruncatedGradient)) throw new  IllegalArgumentException("In the 'emulate SD' mode only TG is supported");

		if (adaptiveSD) {
		    algo.runAdaptiveSD(train, 0, train.size(), eps);
		} else {
		    if (i1 % train.size() != 0 ||i2 % train.size() != 0) throw new AssertionError("emulateSD: i1, i2 not multiple of the train set size");
		    int repeat = (i2-i1) / train.size();
		    for(int j=0; j<repeat; j++) {
			algo.absorbExamplesSD(train, 0, train.size());
		    }
		}
	    } else {
		for(int i=i1; i<i2; i++) {
		    int j = (gen==null)? i%train.size(): gen.nextInt(train.size());
		    algo.absorbExample(train, j, j+1);
		}
	    }

	    if (Suite.verbosity>1) {
		System.out.println("Describing the Learner after "+i2 + " training examples");
		algo.describe(System.out, false);
		System.out.println("-----------------------------------");
	    } else if (Suite.verbosity>0){
		System.out.println("[NET] Leaner after " + (i2)+ " examples: net memory use=" + algo.memoryEstimate());
	    }
	    // In verbose mode, write out the model at each checkpoint
	    if (verbose) algo.saveAsXML(algo.algoName() + "-out" + trainCnt + ".xml");

	    if (Suite.verbosity>0) memory("Absorbed "+(i2-i1)+" examples from "+trainFile);
	    // Now, score all vectors in the REST of the set 


	    if (Suite.verbosity>0) System.out.println("After training the learner on the first " + i2 + " examples, applying it to the other " + (train.size()-i2) + " examples");
		
	    PrintWriter sw1 = null, sw2=null;
	    if (Suite.verbosity>0 && scoreFileBaseTrain != null) {
		String scoreFile = scoreFileBaseTrain + "." + fmt.format(i2);
		System.out.println("Train set scores will go to text file ("+
				   scoreFile+")");
		sw1 = new PrintWriter( new FileWriter(scoreFile));
	    }
	    if ( scoreFileBase != null) {
		String scoreFile = scoreFileBase + "." + fmt.format(i2);
		System.out.println("Test set scores will go to text file ("+
				   scoreFile+")");
		sw2 = new PrintWriter( new FileWriter(scoreFile));
	    }


	    Scores scoresTrain = new Scores(suite);
	    Scores scoresTest = new Scores(suite);
	  
	    // Score the entire training set (including even examples
	    // not seen so far)
	    for(int i=0; i< train.size(); i++) {
		DataPoint x = train.elementAt(i);
		double [][] prob, probLog;
		if (algo instanceof PLRMLearner) {
		    // overcoming underflow...
		    probLog = algo.applyModelLog(x);
		    prob = expProb(probLog);
		} else {
		    prob = algo.applyModel(x);
		    probLog = logProb(prob);
		}

		
		if (Suite.verbosity>1) {
		    System.out.println("Scored training vector "+i+"; scores=" +
				       x.describeScores(prob, suite));
		}

		if (sw1!=null) x.reportScoresAsText(prob,algo,runid,sw1);

		scoresTrain.evalScores(x, suite, prob);
		x.addLogLinLik(probLog, prob, suite, 
			       scoresTrain.likCnt,
			       scoresTrain.logLik, scoresTrain.linLik);			
	    }

	    // score the test set
	    for(int i=0; i< test.size(); i++) {
		DataPoint x = test.elementAt(i);

		double [][] prob, probLog;
		if (algo instanceof PLRMLearner) {
		    // overcoming underflow...
		    probLog = algo.applyModelLog(x);
		    prob = expProb(probLog);
		} else {
		    prob = algo.applyModel(x);
		    probLog = logProb(prob);
		}
		if (Suite.verbosity>1) {
		    System.out.println("Scored test vector "+i+"; scores=" +
				       x.describeScores(prob, suite));
		}
		if (sw2!=null) x.reportScoresAsText(prob,algo,runid,sw2);
		scoresTest.evalScores(x, suite, prob);
		/** Adding prob, instead of probLog, because it's not 
		    logarithmized */
		x.addLogLinLik(probLog, prob, suite, 
			    scoresTest.likCnt, 
			    scoresTest.logLik,  scoresTest.linLik);

	    }


	    if (sw1 != null) sw1.close();
	    if (sw2 != null) sw2.close();

	    // Print report on scores so far
	    if (Suite.verbosity>=0) {
		String label = trainFile+": " + seed + " : " + i2 + "+000";

		System.out.println("Scoring report (file "+trainFile+", seed="+seed+"):");
		
		System.out.print(scoresTrain.scoringReport(suite, "[TRAIN SCORES]["+label+"]"));
		System.out.print(scoresTrain.likReport2(suite, "[TRAIN LOG LIN]["+label+"]"));
		System.out.println(scoresTrain.wAvgRecallReport(suite, "[TRAIN WARECALL]["+label+"]"));

		label = testFile+": " + seed + " : " + i2 + "+000";

		System.out.print(scoresTest.scoringReport(suite, "[TEST SCORES]["+label+"]"));
		System.out.print(scoresTest.likReport2(suite, "[TEST LOG LIN]["+label+"]"));
		System.out.println(scoresTest.wAvgRecallReport(suite, "[TEST WARECALL]["+label+"]"));
		
	    }
	

	    if (Suite.verbosity > 0) memory("Scored "+i2+" randomly selected examples from "+trainFile);

	    if (saveMatrix) {
		// save the entire model
		//suite.serializeLearnerComplex(out + "/out-suite."+ fmt.format(i2) +".xml"); 
		// Save just the W matrix of the learner
		String g = out +  "/out-matrix."+ fmt.format(i2) +".xml"; 
		saveMatrix( (PLRMLearner)algo, g, i2, matWriter);
	    }
	}
	if (saveMatrix) matWriter.close();
    }

    /** Gets a pointer to the PLRM learner's coeff matrix (W), and
	saves the matrix into an XML file
     */
    private static void saveMatrix(PLRMLearner algo, String fname, int t, PrintWriter matWriter) {
	Suite suite = algo.getSuite();
	Discrimination dis = null;
	try {
	    dis = suite.lookupSimpleDisc();
	} catch(Exception ex) {
	    System.out.println("Can't identify the 'only' discrimination");
	    return;
	}
	Learner.LearnerBlock block = algo.findBlockForDis(dis);
	Matrix w = ((PLRMLearner.PLRMLearnerBlock)block).getW();
	w.saveAsXML( fname, dis, suite.getDic(), "W");
	double[][] v = w.toArray();
	
	matWriter.print(t);
	double [] empty = new double[0];
	for(int fid = 0; fid <  suite.getDic().getDimension(); fid++) {
	    double [] q = (fid<v.length && v[fid]!=null)? v[fid] : empty;
	    for(int cid=0; cid<dis.claCount(); cid++) {
		double z =  (cid<q.length)? q[cid]: 0.0;
		matWriter.print("\t" + z);
	    }
	}
	matWriter.println();

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


    /** Computes log of each array element */
   private static double [][] logProb(double[][] prob) {
       final double M = -100;
       double [][] probLog = new double[prob.length][];
       for(int j=0; j<prob.length;j++) {
	    double [] v = prob[j];
	    probLog[j] = new double[v.length];
	    for( int k=0; k< v.length; k++) {
		probLog[j][k] = (v[k]==0) ? M : Math.log(v[k]);
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
