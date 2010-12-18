package edu.dimacs.mms.applications.ontology;

import java.util.*;
import java.io.*;
import java.text.*;
import org.w3c.dom.Element;

import edu.dimacs.mms.boxer.*;
import edu.dimacs.mms.borj.*;

/**
   Usage:
<pre>
   java [options] edu.dimacs.mms.applications.ontology.Driver  dataFile1 dataFile2
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
	if (argv.length != 2) usage();
	String in1 = argv[0], in2 = argv[1];
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

	DataSourceParser p1 =  DataSourceParser.parseFile(in1, null);
	Suite suite = p1.suite;
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
	FeatureDictionary dic = p1.suite.getDic();
	DataSourceParser p2 =  DataSourceParser.parseFile(in2, dic);


	if (Suite.verbosity>=1 && !in2.equals(in1)) {
	    String outName = "out-" + DataSourceParser.baseName(in2);
	    DataPoint.saveAsXML(p2.data, outName, outName+".xml");
	}



	// Any "read-priors" command?
	String priorsFile = ht.getOption("priors", null);
	if (priorsFile!=null) {
	    System.out.println("Reading priors from file: "+priorsFile);
	    Priors p = Priors.readPriorsFileMultiformat(new File(priorsFile), suite);
	    suite.setPriors(p);
	}


	// initializing the learner
	String learnerFile = ht.getOption("learner", null);
	if (learnerFile==null) usage("Must specify -Dlearner=name.xml");
	System.out.println("Getting a learner from file: "+learnerFile);
	Element learnerXML = ParseXML.readFileToElement(new File(learnerFile));

	suite.addLearner(learnerXML);

	int nLearners =  suite.getLearnerCount();
	if (nLearners != 1) throw new AssertionError("nLearners="+nLearners+" There must be exactly one learner!");
	Learner algo= suite.getAllLearners().elementAt(0);
	if (Suite.verbosity>0) {
	    System.out.println("Describing the learner:");
	    algo.describe(System.out, false);
	    System.out.println("-----------------------------------");
	}


	if (adaptiveSD) {
	    algo.runAdaptiveSD(p1.data, 0, p1.data.size());
	} else {

	    // training the learner on the cells from the first data source
	    for(int k=0; k<learnRep; k++) {		
		if (emulateSD) {
		    algo.absorbExamplesSD(p1.data, 0, p1.data.size());
		} else {
		    algo.absorbExample(p1.data, 0, p1.data.size());
		}		
	    }
	}


	final boolean saveMatrix = true;
	if (saveMatrix) {
	    // save the entire model
	    String out=".";
	    suite.serializeLearnerComplex(out + "/out-giant-model.xml"); 
	    // Save just the W matrix of the learner
	    // String g = out +  "/out-matrix."+ fmt.format(i2) +".xml"; 
	    // saveMatrix( (PLRMLearner)algo, g, i2, matWriter);
	    //System.exit(0);
	}



	int M1 = p1.dis.claCount();

	int M2 = p2.dis.claCount();
	// [newOnto.class][oldOnto.class]
	double[][] sumProb = new double[M2][], sumLogProb = new double[M2][];
	for(int i=0; i<M2; i++) {
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

	System.out.print("Cell counts:");
	for(int i=0; i<M2; i++) System.out.print(" " + count[i]);	
	System.out.println();


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

	NumberFormat fmt = new DecimalFormat("0.0000");


	System.out.println("=== Confusion matrix - Arithmetic mean ===");
	for(int i=0; i<M2; i++) {
	    for(int j=0; j<M1; j++) {
		if (j>0) System.out.print("\t");
		System.out.print("P(" + p1.dis.getClaById(j).getName() + "|" + 
				 p2.dis.getClaById(i).getName() + ")=" + 
				 sumProb[i][j]);
	    }
	    System.out.println();
	}
	
	System.out.println("=== Top matches - Arithmetic mean ===");
	for(int i=0; i<M2; i++) {
	    ScoreWrapper[] w=sortScores( sumProb[i]);
	    System.out.print( p2.dis.getClaById(i).getName() + " :");
	    for(int k=0; k<3 && k<w.length; k++) {
		System.out.print("\t{"+p1.dis.getClaById(w[k].i).getName()+
				 "=" +  fmt.format(w[k].value) + "}" );
	    }
	    System.out.println();
	}
	

	System.out.println("=== Confusion matrix - Geometric mean ===");
	for(int i=0; i<M2; i++) {
	    for(int j=0; j<M1; j++) {
		if (j>0) System.out.print("\t");
		System.out.print("P(" + p1.dis.getClaById(j).getName() + "|" + 
				 p2.dis.getClaById(i).getName() + ")=" + 
				 Math.exp(sumLogProb[i][j]));
	    }
	    System.out.println();
	}

	System.out.println("=== Top matches - Geometric mean ===");
	for(int i=0; i<M2; i++) {
	    ScoreWrapper[] w=sortScores( sumLogProb[i]);
	    System.out.print( p2.dis.getClaById(i).getName() + " :");
	    for(int k=0; k<3 && k<w.length; k++) {
		System.out.print("\t{"+p1.dis.getClaById(w[k].i).getName() + 
				 "=" + fmt.format(Math.exp(w[k].value)) + "}");
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


}