package edu.dimacs.mms.applications.ontology;

import java.util.*;
import java.io.*;
import java.text.*;
import org.w3c.dom.Element;

import edu.dimacs.mms.boxer.*;
import edu.dimacs.mms.borj.CMD;

/** The main class for the Bayesian Ontology Aligner.


   Usage:
<pre>
   java [options] edu.dimacs.mms.applications.ontology.Driver  dataFile1 dataFile2

   java [options] edu.dimacs.mms.applications.ontology.Driver  train:dataSource1.csv  test:dataSource2.csv  

   java [options] edu.dimacs.mms.applications.ontology.Driver  train:dataSource1.csv:precomputedLearner.xml  test:dataSource2.csv  


</pre>
   Options:
</pre>
     -Dverbosity=0 : verbosity level (0 or higher)

     -Dlearner=learner.xml  : learner description
     -Dlearn.rep=1 : how many times repeat training

 */
public class Driver {
    
   static void usage() {
	usage(null);
    }


   static void usage(String m) {
	System.out.println("This is Ontology Matcher, using BOXER Toolkit (version " + Version.version+ ")");
	if (m!=null) {
	    System.out.println("-- MESSAGE --");
	    System.out.println(m);
	}
	System.exit(1);
    }

    static boolean emulateSD = false, adaptiveSD=false;
   
    static public void main(String argv[]) throws IOException, BoxerXMLException , org.xml.sax.SAXException {
	//if (argv.length != 2) usage();

	ParseConfig ht = new ParseConfig();
	Suite.verbosity = ht.getOption("verbosity", 0);
	emulateSD = ht.getOption("sd", false);
	adaptiveSD = ht.getOption("adaptive", false);

	if (adaptiveSD && !emulateSD) usage("-Dadaptive=true may only be used with -Dsd=true");

	// configure input data parsing options
	DataSourceParser.inputOptions.init(ht);

	// How many times repeat training (unless a more sophisticated
	// termination criterion is used)
	int learnRep = ht.getOption( "learn.rep" , 1);
	if (learnRep < 1) usage();

	System.out.println("verbosity="+Suite.verbosity+", SD=" + emulateSD + " adaptiveSD"+adaptiveSD);

	// Stage 1: either read the old data source (and plan for
	// future training), or read both the data source and the
	// pre-computed learner.

	DataSourceParser p1; 
	Suite suite;
	String in1=null;
	boolean alreadyTrained = false;	
	CMD.setTwoArgCmd(CMD.READ);
	CmdManager cm  = new CmdManager(argv);
	CMD q = cm.next();


	if (q!=null && q.is(CMD.TRAIN)) {
	    in1 = q.f;
	    System.out.println("Reading the Old DataSource from file: "+in1);
	    p1 =  DataSourceParser.parseFile(in1);
	    suite =  p1.suite;
	    q = cm.next();
	} else 	if (q!=null && q.is(CMD.READ)) {
	    // Reading a complete learner complex (i.e., a pre-computed
	    // model), and then reading the
	    // orignal data source  (which we'll need for normalization)

	    String inMatrix= q.f2;
	    in1=q.f;

	    if (inMatrix==null) usage("The 'read' command must supply both the learner file and the data source file, as in read:ds.cvs:model.xml");

	    System.out.println("Reading pre-computed learner(s) from file: "+
			       inMatrix);
	    suite =  Learner.deserializeLearnerComplex(new File(inMatrix));
	    alreadyTrained = true;
	    Vector <Learner> algos = suite.getAllLearners();
	    
	    Logging.info("Read "+algos.size()+" learners from the 'learner complex' file " + inMatrix );
	    if (algos.size()==0) {
		//usage("The file " + q.f + " did not specify even a single learner");
		Logging.info("The 'learner complex' file " + inMatrix + " did not specify even a single learner. We expect that one will be added with a separate read-learner command");
	    }

	    int d0 = suite.getDic().getDimension();


	    System.out.println("Reading the Old DataSource from file: "+in1);
	    p1 =  DataSourceParser.parseFile(in1,  suite);

	    int d1 = suite.getDic().getDimension();
	    if (d1 < d0) usage("The pre-computed learner and the old data source read in by the 'read' command must be consistent with each other. This wasn't the case here, as indicated by the dictionary growth after reading the data source: d0=" + d0 +", d1="+d1);
	    
	    //p1.replaceSuiteWithEquivalent(suite);

	    q = cm.next();
	} else {
	    usage("There must be a 'train' or 'read' command going first");
	    return;
	}


	// the only non-trivial dis
	int did = suite.getDid( p1.dis);
	// reporting
	if (Suite.verbosity>=1) {
	    suite.saveAsXML("out-suite.xml");
	    String outName = "out-" + DataSourceParser.baseName(in1);
	    DataPoint.saveAsXML(p1.data, outName, outName+".xml");
	}

	// The second (and any other) data sources must be interpreted in
	// terms of the same FeatureDictionary
	FeatureDictionary dic = suite.getDic();


	// Any "read-priors" command?
	String priorsFile = ht.getOption("priors", null);
	if (priorsFile!=null) {
	    System.out.println("Reading priors from file: "+priorsFile);
	    Priors p = Priors.readPriorsFileMultiformat(new File(priorsFile), suite);
	    suite.setPriors(p);
	}

	Learner algo= null;
	if (alreadyTrained) {
	    algo= suite.getAllLearners().elementAt(0);
	} else {
	    // initializing the learner
	    String learnerFile = ht.getOption("learner", null);
	    if (learnerFile==null) usage("Must specify -Dlearner=name.xml");
	    System.out.println("Getting a learner from file: "+learnerFile);
	    Element learnerXML = ParseXML.readFileToElement(new File(learnerFile));
	    suite.addLearner(learnerXML);
	    
	    int nLearners =  suite.getLearnerCount();
	    if (nLearners != 1) throw new AssertionError("nLearners="+nLearners+" There must be exactly one learner!");
	    algo= suite.getAllLearners().elementAt(0);
	    if (Suite.verbosity>0) {
		System.out.println("Describing the learner:");
		algo.describe(System.out, false);
		System.out.println("-----------------------------------");
	    }
	    
	    // training the learner on the cells from the first data source
	    if (adaptiveSD) {
		algo.runAdaptiveSD(p1.data, 0, p1.data.size());
	    } else {	
		for(int k=0; k<learnRep; k++) {		
		    if (emulateSD) {
			algo.absorbExamplesSD(p1.data, 0, p1.data.size());
		    } else {
			algo.absorbExample(p1.data, 0, p1.data.size());
		    }		
		}
	    }
	    alreadyTrained = true;	   
	}

	// Compute square roots of self-probs (for normalization)
	double[] sqrtSP =  sqrtSelfProb(p1);


	if (q!=null && q.is(CMD.WRITE)) {
	    // save the entire model
	    String out=".";
	    String path = out + "/out-giant-model.xml"; 
	    System.out.println("Saving the model to: " + path);	    
	    suite.serializeLearnerComplex(path);
	    // Save just the W matrix of the learner
	    // String g = out +  "/out-matrix."+ fmt.format(i2) +".xml"; 
	    // saveMatrix( (PLRMLearner)algo, g, i2, matWriter);
	    q = cm.next();
	}

	// Scoring
	while(q!=null) {
	    if (!q.is(CMD.TEST)) {
		usage("A 'test' command was expected now!");
	    }

	    String in2 = q.f;
	    String in2base = DataSourceParser.baseName(in2);

	    DataSourceParser p2 =  DataSourceParser.parseFile(in2, dic);
	    if (Suite.verbosity>=1 && !in2.equals(in1)) {
		String outName = "out-" + in2base;
		DataPoint.saveAsXML(p2.data, outName, outName+".xml");
	    }

	    int M1 = p1.dis.claCount(), M2 = p2.dis.claCount();
	    // [newOnto.class][oldOnto.class]
	    double[][] sumProb = new double[M2][], sumLogProb = new double[M2][];	    for(int i=0; i<M2; i++) {
		sumProb[i] = new double[M1];
		sumLogProb[i] = new double[M1];
	    }
	    // [newOnto.class]
	    int[] count = new int[M2];
	    	    
	    // score    
	    for( DataPoint p: p2.data) {
		int newCid = p.getClasses(p2.suite).elementAt(0).getPos();
		// overcoming underflow...
		double[] logProb = algo.applyModelLog(p)[did];
		double[] prob = expProb(logProb);
		for(int j=0; j<M1; j++) {
		    sumProb[newCid][j] += prob[j];
		    sumLogProb[newCid][j] += logProb[j];
		}
		count[newCid] ++;
	    }
	    
	    //System.out.print("Cell counts:");
	    //for(int i=0; i<M2; i++) System.out.print(" " + count[i]);	
	    //System.out.println();

	    // average scores
	    for(int i=0; i<M2; i++) {
		if (count[i] > 0) {
		    for(int j=0; j<M1; j++) {
			sumProb[i][j] /= count[i];
			sumLogProb[i][j] /= count[i];
		    }
		}
	    }
	    
	    // reporting	    	    
	    reportConfusionMatrix(p1, p2,sumProb, false,"Arithmetic  mean, " + in2base);
	    reportConfusionMatrix(p1, p2,sumLogProb, true,"Geometric mean, "+ in2base);
	    
	    // normalized scores
	    for(int i=0; i<M2; i++) {
		if (count[i] > 0) {
		    for(int j=0; j<M1; j++) {
			sumProb[i][j] /= sqrtSP[j];
		    }
		}
	    }
	    reportConfusionMatrix(p1, p2,sumProb, false,"Arithmetic mean normalized, "+ in2base);

	    q = cm.next();
	}
    }

    /** Probabilities of the old data source's columns'
	self-assignment (under the simple arithmetic-mean model).
     */
    static double[] sqrtSelfProb(DataSourceParser p1) {
	int M1 = p1.dis.claCount();

	double[] sumProb = new double[M1];
	int[] count = new int[M1];

	Learner algo= p1.suite.getAllLearners().elementAt(0);
	int did = p1.suite.getDid( p1.dis);

	for( DataPoint p: p1.data) {
	    int newCid = p.getClasses(p1.suite).elementAt(0).getPos();
	    // overcoming underflow...
	    double[] logProb = algo.applyModelLog(p)[did];
	    double[] prob = expProb(logProb);
	    sumProb[newCid] += prob[newCid];
	    count[newCid] ++;
	}

	for(int i=0; i<M1; i++) {
	    sumProb[i] = Math.sqrt( sumProb[i]/count[i]);
	}

	return sumProb;
    }


    private static void reportConfusionMatrix(DataSourceParser p1, DataSourceParser p2,
					      double prob[][],boolean isLog, 
					      String name)    {
	NumberFormat fmt = new DecimalFormat("0.0000");
	int M1 = p1.dis.claCount();
	int M2 = p2.dis.claCount();
	System.out.println("=== Confusion matrix - "+name+" ===");
	for(int i=0; i<M2; i++) {
	    for(int j=0; j<M1; j++) {
		if (j>0) System.out.print("\t");
		double q = isLog? Math.exp(prob[i][j]): prob[i][j];
		System.out.print("P(" + p1.dis.getClaById(j).getName() + "|" + 
				 p2.dis.getClaById(i).getName() + ")=" + q);
	    }
	    System.out.println();
	}

	System.out.println("=== Top matches - "+name+" ===");
	for(int i=0; i<M2; i++) {
	    ScoreWrapper[] w=sortScores( prob[i] );
	    System.out.print( p2.dis.getClaById(i).getName() + " :");
	    for(int k=0; k<3 && k<w.length; k++) {
		double q = isLog? Math.exp(w[k].value): w[k].value;
				
		System.out.print("\t{"+p1.dis.getClaById(w[k].i).getName() + 
				 "=" + q + "}");
	    }
	    System.out.println();
	}
    }


    /** Computes exponent of each array element */
    private static double [] expProb(double[] logProb) {
	double[] prob = new double[logProb.length];
	for( int k=0; k< logProb.length; k++) {
	    prob[k] = Math.exp(logProb[k]);
	}
	return prob;
    }


    public  static class ScoreWrapper implements Comparable<ScoreWrapper> {
	int i; double value; 
	public ScoreWrapper( int f, double v) { i=f; value=v;} 
	/** Used for sorting */
	public int compareTo(ScoreWrapper x)  {
	    return x.value > value ? 1 : x.value==value? 0 : -1 ;
	}
    }

    /** descending sort */
    static ScoreWrapper[] sortScores(double[] scores) {
	ScoreWrapper[] w = new ScoreWrapper[scores.length];
	for( int k=0; k< scores.length; k++) {
	    w[k]=new ScoreWrapper(k, scores[k]);
	}
	Arrays.sort(w);
	return w;
    }


    /** Auxiliary class responsible for parsing command line */
    static private class CmdManager {
        private CMD [] cm;

        /** Pointer to the first not-yet parsed cmd line argument */
        private int h = 0;
	/** Initializes the command manager by parsing the entire command line */
        CmdManager(String [] argv) {
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


}