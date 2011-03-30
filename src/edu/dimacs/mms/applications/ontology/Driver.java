package edu.dimacs.mms.applications.ontology;

import java.util.*;
import java.io.*;
import java.text.*;
import org.w3c.dom.Element;

import edu.dimacs.mms.boxer.*;
import edu.dimacs.mms.borj.CMD;

/** The main class for the Bayesian Ontology Aligner.

    <h2>Algorithms</h2> 
    
    <P>
    For an overview of the algorithms available
    for ontology matching, please see the following PDF document: <a
    href="../../../../../../../pdf/boa-01.pdf">On choosing a suitable
    score function for the Bayesian Ontology Alignment tool</a>.

    <P>In a nutshell, the usual plan of operations for this tool is
    the following: 
    
    <ol> 

    <li>Read in a data file describing a <b>data source</b> (DS1), i.e.,
    basically, a table of data where each row (record) corresponds to
    something like a structured document, and each column, to a
    particular field of the document (e.g., "Author", "Dateline",
    "Subject", etc.). 

    <li><b>"Tokenization"</b>: Convert each cell of the table (the data
    source) into a vector in a feature space. Features may correspond
    to words, characters, or groups of characters (n-grams).

    <li><b>"Training"</b>: Build some kind of Bayesian model, i.e. a
    function which maps an arbitrary vector from the feature space to
    a vector of "scores": real numbers in the [0,1] range, summing to 1.0,
    describing the putative probabilities of assigning that vector to
    various classes introduced above (i.e., columns of the data source).

    <li><b>"Alignment"</b>: Read in another data file, describing
    another data source (DS2). Tokenize its cells' contents in a
    similar way as that applied to the first data source. Apply the
    stored model to each cell of the new data source. For every pair
    of columns (<em>C<sub>i</sub></em> from DS1,
    <em>C'<sub>j</sub></em> from DS2) compute the confusion matrix
    coefficient <em>f_<sub>ij</sub></em> by combining, in some way,
    scores for the cells of <em>C'<sub>j</sub></em>.
    </ol>

    <p>There are also "symmetric" methods, where, instead of separate
    "Training" and "Alignment" steps, a matching algorithm is applied
    to both data sources simultaneously,

    <p>The tokenization process and the training process are both
    controlled by command-line arguments.

    <p>Optionally, you can <b>save</b> the model to a file for later
    re-use, and on a later BOA run read it back, instead of carrying
    out the training process again.    

    <h2>Usage</h2>

   Usage:
   <ol>

   <li>To build a model based on one data source and then apply it to
   another data source (or, in a sequence, to several data sources):

<pre> java [options] edu.dimacs.mms.applications.ontology.Driver train:dataSource1.csv test:dataSource2.csv [test:dataSource3.csv ...]
</pre>

<li>To load a pre-computed model and then apply it to
   another data source (or, in a sequence, to several data sources):

<pre>
   java [options] edu.dimacs.mms.applications.ontology.Driver  train:dataSource1.csv:precomputedLearner.xml  test:dataSource2.csv  [test:dataSource3.csv ...]
</pre>

<li>To build a model based on one data source and then save the model to a file:
<pre> java [options] edu.dimacs.mms.applications.ontology.Driver train:dataSource1.csv write:precomputedLearner.xml  
</pre>

<li>To use a "symmetric" method (sym1), which builds a model on the union of two data sources, and uses that model:
<pre> java [options] edu.dimacs.mms.applications.ontology.Driver sym1:dataSource1.csv:dataSource2.csv
</pre>

<li>To use another "symmetric" method (sym2), which builds a model on each of the two data sources, and then applies each model to the other data source:
<pre> java [options] edu.dimacs.mms.applications.ontology.Driver sym2:dataSource1.csv:dataSource2.csv
</pre>
</ol>

There are also two "non-Bayesian" methods. Instead of laboriously
training a Bayesian model, they map each field (column) of each data
source to a feature vector, and directly compute similarity of these
vectors. Sample commands are as follows:

<ol>
<li>To use cosine similarity of feature vectors:
<pre> java [options] edu.dimacs.mms.applications.ontology.Driver vec_cos:dataSource1.csv:dataSource2.csv
</pre>

<li>To use Jensen-Shannon divergence between feature vectors:
<pre> java [options] edu.dimacs.mms.applications.ontology.Driver vec_js:dataSource1.csv:dataSource2.csv
</pre>
</ol>

<p>A particularly recommended set of options for training Bayesian learners is 
<pre>
   -Dlearn.sd=true -Dlearn.adaptive=true -Dlearn.eps=1e-8
</pre> 
This will run steepest descent with the adaptive-learning rate  on the training set, to a fairly high degree of convergence. Although slow, it should produce a model that's fairly close to  optimal, with respect to log-likelihood on the training set. In practice, the precision 1e-6 or even 1e-4 is sufficient on many tasks; so one may want to start with  -Dlearn.eps=1e-4, and then reduce eps if needed.

<P>An auxiliary script, <tt> sample-data/ontology/matrix2html.pl
</tt>, is available to extract confusion matrix(es) from the output of
BOA, and to convert it into pleasing-to-eye HTML and CVS formats.

   <h3>File formats</h3>

   <p> The names of the data source files supplied to the <tt>train</tt> and
   <tt>test</tt> commands must have the extension ".csv" or ".txt". 

   <p>If   the file name has the ".csv" extension, it must be in the
   comma-separated-values format with double quotes, i.e. each line must be in the form.
<pre>
"val1","val2",...,"val_N"
</pre>
   A data file in such format can be exported for example from <a href="https://wits.nctc.gov/FederalDiscoverWITS/index.do?t=Records&Rcv=Incident&Nf=p_IncidentDate|GTEQ+20060301||p_IncidentDate|LTEQ+20060331&N=0">this page</a> at the WITS site, and choosing the option for sacing as CSV from that page.

   <P>If the file name has the ".txt" extension, the values in each line must be separated by the '|' character, and no double-quotes should be used:
<pre>
val1|val2|...|val_N
</pre>

   <p>The learner and model XML files are in the usual 
   <a   href="../../../../../../xml.html">BOXER XML format.</a>.

   <h3>Command semantics</h3>

   <ul> 

   <li>train:<em>datasource_file</em> : BOA will build a model
   describing the fields (i.e. columns) of the data source described
   in the data file.

   <li>test:<em>datasource_file</em> : BOA will apply the model it
   currently has to the data source described in the file, and will
   output the confusion matrix describing the alignment of the fields 
   of the new data source's ontology against those of the old data source.

   <li>read:<em>datasource_file</em>:<em>model_file</em> : BOA
   will read the data source file and then, instead of learning on
   that data source, will read the pre-computed model. This command is
   not compatible with the <tt>train</tt> command.

   <li>write:<em>model_file</em> : BOA will save its current model to
   an XML file, so that it can be read some time later with a
   <tt>read</tt> command.

   <li>sym1:<em>datasource_file_1</em>:<em>datasource_file_2</em> : a
   "symmetric" aligner, where both data sources are used in training
   or testing. The sym2 command is similar.

   </ul>

   <p>There are two families of ontology-alignment algorithms:
   asymmetric (train on one data source, then apply the resulting
   model to the other data source) and symmetric (when both data
   sources are used during the training and the testing). If a
   symmetric algorithm is used in a situation when the two data
   sources are identical, it is guaranteed to produce a symmetric
   confusion matrix.

   <p>On every BOA run involving asymmetric alignment algorithms, the
   command line must start with a <tt>read</tt> or <tt>train</tt>
   command, so that BOA will either compute a model, or read a
   pre-computed model saved on an earlier run. Any number of
   <tt>train</tt> commands may follow, to apply this model to the
   specified other data source(s). One can also save the model with
   the <tt>write</tt> command; this will allow you to save time on
   later runs, if you expect to apply the same model to more data
   sources in the future.

   <p>To use a symmetric algorithm, you only need one command on the
   command line: <tt>sym1</tt> or <tt>sym2</tt>, with 2 arguments.

   <h3>Options</h3>
   The options available with the BOA tool can be divided into several groups.
   
   <p>
   (a) <b>Data source parsing options</b> control the processing of input files. They, in turn, can be divded into subgroups.

   <p> <b>Interpretation of columns:</b>
   <ul>
   <li>-Dinput.rid=0 : A 1-based number of the column that should be understood as containing the record each for each record. E.g., use -Dinput.rid=1 if you want the 1st column of the data source file to be interpreted as containing the record ID. If the option is omitted, or is 0, then IDs will be automatically generated. This option does not affect the ontology alignment results; record IDs are used for information purposes only.
   
   <li>-Dinput.exclude = col1[:col2...] : A colon-separated list of 1-based numbers of the column that you want to <em>exclude</em> from parsing and alignment. By default, no column is excluded.
   </ul>

   <p> <b>Tokenization process</b> - these options control how the
   contents of each cell of the data source (i.e., a text string) are
   converted to a data point, i.e. a feature vector. A feature vector
   for a given cell may contain tokens of any of the three types:
   <ul>
   <li><b>word</b>-based; 
   <li><b>character-sequence</b>-based;
   <li><b>special</b>.
   </ul> A vector generated for a cell may contain no
   feature at all (a zero vector); this must be distingished from 
   not generating a vector for that cell at all.

   <ul>

   <li>-Dinput.words=true : If true (which is the default), words are
   used as features.

   <li>-Dinput.gram=2 : An integer indicating the maximum length of
   "<em>n</em>-grms" (character sequences) used as features. E.g.,
   -Dinput.gram=0 means that no character sequences are used as features;
   -Dinput.gram=1 means that features for individual characters are used;
   -Dinput.gram=2 means that, BOA also creates  features for 2-grams; etc.
   
   <li>-Dinput.empty.skip=false|true If true, empty cells are ignored
   (no vectors are created for them). The default is false.

   <li>-DemptySpecial=true|false If true, a special feature is used to
   mark empty cells. The default is true.

   </ul>

   <p> 
   (b) <b>Learning options</b> tell BOA how to construct a Bayesian model on the set of data points generated from the data source.
<ul>
<li>
     -Dlearner=learner.xml  : Learner description file, 
<li>
     -Dlearn.rep=1 : How many times repeat training. This option is ignored if adaptive SD is used.

<li> -Dlearn.sd=true|false : If true, use Steepest Descent instead of
Stochastic Gradient Descent (which is simply Truncated Gradient w/o
truncation). You can only use this option if the learner file
describes a TruncatedGradient learner (which may be, and usually is,
TG with theta=0, i.e. SGD).

<li>
     -Dlearn.adaptive=true|false : Only can be used in combination with learn.sd=true. If learn.adaptive=true, BOA ignores <tt>learn.rep</tt>, and tried to build a log-likelihood-optimizing model by running {@link edu.dimacs.mms.boxer.Learner#runAdaptiveSD Steepest Descent with adaptive learning rate (ASD)} until convergence, as determined by learn.eps. The learner file in this situation should specify "TruncatedGradient with theta=0", i.e. Steepest Descent, as adpative learning is currently only supported for optimizing non-penalized log-likelihood. No priors should be specified.
<li>
     -Dlearn.priors=priors.xml : An optional priors file (modifies the penalty term for the function being optimized)
<li>
     -Dlearn.eps=1e-8 : The convergence criterion for adaptive SD (in terms of log-likelihood).
<li>
     -Dlearn.bxr=true|false : If true, BOA piggybacks on BXR, instead of using BOXER's built-in learning methods. If this option is used, the only other learning option that should be supplied is -Dlearn.eps (typically, -Dlearn.eps=0.01 or thereabout); this is interpreted as BXR's eps. Please see {@link edu.dimacs.mms.boxer.BXRLearner} for additional important details on using this option.
</ul>

<P>Note: No learner options need to be used when using a non-Bayesian method (cosine similarity of JSD).

 <p>
   (c) Special options for non-Bayesian methods (cosine similarity and JSD)

<ul>
<li>
 -Dvec.mode=TF|PREVALENCE : This controls how the feature vector describing a field (column) of a data source is assembled from the fature vectors for individual cells. The default mode, TF, adds term frequencies from each cell within the column; the PREVALENCE mode simply counts the number of cells within the column in which the term is present.
</ul>

   <p>
   (d) Miscellaneous options
<pre>
     -Dverbosity=0 : verbosity level (0 or higher)
</pre>
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

    /** Additional commands (not listed in CMD.java) */
    static final String CMD_SYM1="sym1", CMD_SYM2="sym2",
       CMD_VEC_COS="vec_cos", CMD_VEC_JS="vec_js";

    /** Learning options (initialized in main()) */
    static private boolean emulateSD = false, adaptiveSD=false, useBXR=false;
    static double eps;
    static int learnRep;

    static private ParseConfig ht = null;


    static public void main(String argv[]) throws IOException, BoxerException , org.xml.sax.SAXException {
	//if (argv.length != 2) usage();

	ht = new ParseConfig();
	Suite.verbosity = ht.getOption("verbosity", 0);
	useBXR =  ht.getOption("learn.bxr", false);
	emulateSD = ht.getOption("learn.sd", false);
	adaptiveSD = ht.getOption("learn.adaptive", false);
	eps = ht.getOptionDouble("learn.eps", 1e-8);
	eps = ht.getOptionDouble("learn.eps", 1e-8);

	if (useBXR && (emulateSD || adaptiveSD)) usage("SD options, or priors, are not compatible with BXR");

	if (adaptiveSD && !emulateSD) usage("-Dadaptive=true may only be used with -Dsd=true");

	// configure input data parsing options
	DataSourceParser.inputOptions.init(ht);
	if (DataSourceParser.inputOptions.emptySkip && 
	    DataSourceParser.inputOptions.emptySpecial) {
	    usage("Cannot combine emptySkip=true and emptySpecial=true!");
	}

	// How many times repeat training (unless a more sophisticated
	// termination criterion is used)
	learnRep = ht.getOption( "learn.rep" , 1);
	if (learnRep < 1) usage();

	System.out.println("This is Ontology Matcher, using BOXER Toolkit (version " + Version.version+ ")");
	System.out.println("Verbosity="+Suite.verbosity);
	System.out.println("Input options: " + DataSourceParser.inputOptions.describe());
	System.out.print("Learner options: BXR="+useBXR+", SD=" + emulateSD + ", adaptiveSD="+adaptiveSD);
	if (adaptiveSD) System.out.print(" with eps=" + eps);
	else  System.out.print(" rep=" + learnRep);

	System.out.println();

	// Stage 1: either read the old data source (and plan for
	// future training), or read both the data source and the
	// pre-computed learner.

	DataSourceParser p1=null; 
	Suite suite=null;
	String in1=null;
	boolean alreadyTrained = false;	

	CMD.setTwoArgCmd(new String[] {CMD.READ, CMD_SYM1, CMD_SYM2,
				       CMD_VEC_COS, CMD_VEC_JS
	    });

	CmdManager cm  = new CmdManager(argv);
	CMD q = cm.next();

	if (q==null) {
	    usage(); // no args
	} else if (q.is(CMD_SYM1) || q.is(CMD_SYM2) ||
		   q.is(CMD_VEC_COS) || q.is(CMD_VEC_JS)   ) {
	    // One of the symmetric methods
	    doSymmetric( q);
	    CMD q0 = q;
	    q = cm.next();
	    if (q!=null) {
		String msg="All commands that follow " + q0 + " were ignored";
		System.out.println(msg);
		Logging.error(msg);
	    }
	    return;
	} else if (q.is(CMD.TRAIN)) {
	    in1 = q.f;
	    System.out.println("Reading DS1 from file: "+in1);
	    p1 =  DataSourceParser.parseFile(in1);
	    suite =  p1.suite;
	    q = cm.next();
	} else 	if (q.is(CMD.READ)) {
	    String inMatrix= q.f2;
	    in1=q.f;
		
	    if (useBXR) {
		p1 =  DataSourceParser.parseFile(in1);
		suite =  p1.suite;
		suite.addBXRLearner(new String[] {inMatrix}, eps);
		Logging.info("Reusing BXR model file " + in1);
	    } else {
		// Reading a complete learner complex (i.e., a
		// pre-computed model), and then reading the orignal data
		// source on which that learner has been trained (which
		// we'll need for normalization)
		
		if (inMatrix==null) usage("The 'read' command must supply both the learner file and the data source file, as in read:ds.cvs:model.xml");
		
		System.out.println("Reading pre-computed learner(s) from file: "+
			       inMatrix);
		suite =  Learner.deserializeLearnerComplex(new File(inMatrix));
		Vector <Learner> algos = suite.getAllLearners();
		
		Logging.info("Read "+algos.size()+" learners from the 'learner complex' file " + inMatrix );
		if (algos.size()==0) {
		    //usage("The file " + q.f + " did not specify even a single learner");
		    Logging.info("The 'learner complex' file " + inMatrix + " did not specify even a single learner. We expect that one will be added with a separate read-learner command");
		}

		int d0 = suite.getDic().getDimension();
		
		System.out.println("Re-reading DS1 from file: "+in1);
		p1 =  DataSourceParser.parseFile(in1,  suite);

		int d1 = suite.getDic().getDimension();
		if (d1 < d0) usage("The pre-computed learner and the old data source read in by the 'read' command must be consistent with each other. This wasn't the case here, as indicated by the dictionary growth after reading the data source: d0=" + d0 +", d1="+d1);	    
	    }
	    alreadyTrained = true;
	    q = cm.next();
	} else {
	    usage("There must be a 'train' or 'read' command going first");
	    return;
	}


	// reporting
	if (Suite.verbosity>=1) {
	    suite.saveAsXML("out-suite.xml");
	    String outName = "out-" + DataSourceParser.baseName(in1);
	    DataPoint.saveAsXML(p1.data, outName, outName+".xml");
	}

	// The second (and any other) data sources must be interpreted in
	// terms of the same FeatureDictionary
	FeatureDictionary dic = suite.getDic();


	if (!alreadyTrained) {
	    train(p1);
	    alreadyTrained = true;	   
	}
	Learner algo= suite.getAllLearners().elementAt(0);

	// Compute  self-probs (for normalization in cosine formula)
	Aux1 sp =  selfProb(p1);

	if (q!=null && q.is(CMD.WRITE)) {
	    // save the entire model to the specified file
	    String path = q.f;
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
	    scoreTestSet( p1, in2, sp);
	    q = cm.next();
	}
    }

    /** Initializes and trains the learner associated with p1 */
    private static void train(DataSourceParser p1) throws BoxerXMLException, IOException, org.xml.sax.SAXException  {
	Suite suite = p1.suite;

	// Any "read-priors" command?
	String priorsFile = ht.getOption("learn.priors", null);
	if (priorsFile!=null) {
	    if (useBXR) usage("Priors not yet supported with BXR");
	    System.out.println("Reading priors from file: "+priorsFile);
	    Priors p = Priors.readPriorsFileMultiformat(new File(priorsFile), suite);
	    suite.setPriors(p);
	}

	// initializing the learner
	String learnerFile = ht.getOption("learner", null);

	if (useBXR) {
	    // FIXME: eventually, the creation of BXR learners should also
	    //  be controlled by XML, as with "normal" learners
	    if (learnerFile!=null) usage("Cannot use -Dlearner=... with BXR");
	    System.out.println("Creating a BXR-based learner");
	    suite.addBXRLearner(eps);
	} else {
	    if (learnerFile==null) usage("Must specify -Dlearner=name.xml");
	    System.out.println("Getting a learner from file: "+learnerFile);
	    Element learnerXML = ParseXML.readFileToElement(new File(learnerFile));
	    suite.addLearner(learnerXML);
	}

	int nLearners =  suite.getLearnerCount();
	if (nLearners != 1) throw new AssertionError("nLearners="+nLearners+" There must be exactly one learner!");
	Learner algo= suite.getAllLearners().elementAt(0);
	if (Suite.verbosity>0) {
	    System.out.println("Describing the learner:");
	    algo.describe(System.out, false);
	    System.out.println("-----------------------------------");
	}
	    
	// training the learner on the cells from the first data source
	if (useBXR) {
	    // call BXR once; it knows how to iterate
	    algo.absorbExample(p1.data, 0, p1.data.size());
	} else 	if (adaptiveSD) {
	    algo.runAdaptiveSD(p1.data, 0, p1.data.size(), eps);
	} else {	
	    for(int k=0; k<learnRep; k++) {		
		if (emulateSD) {
		    algo.absorbExamplesSD(p1.data, 0, p1.data.size());
		} else {
		    algo.absorbExample(p1.data, 0, p1.data.size());
		}		
	    }
	}
    }

    /** Scores examples from p2 with the learner that has been trained in 
	p1 */
    private static void scoreTestSet( DataSourceParser p1,
				      String in2,  Aux1 sp) 
 throws IOException, BoxerException , org.xml.sax.SAXException {

	String in2base = DataSourceParser.baseName(in2);

	System.out.println("Reading DS2 from file: "+in2);	
	DataSourceParser p2 =  DataSourceParser.parseFile(in2, 
							  p1.suite.getDic());

	int M1 = p1.dis.claCount(), M2 = p2.dis.claCount();
	AvgScores avg = new AvgScores( p1, p2);
	// reporting	    	    
	reportConfusionMatrix(p1, p2, avg.avgProb, false,"Arithmetic  mean, " + in2base);
	reportConfusionMatrix(p1, p2, avg.avgLogProb, true,"Geometric mean, "+ in2base);
	    
	// normalized scores (the cosine measure)
	int maxCnt=0;
	for(int c: sp.count) { maxCnt = (c>maxCnt)? c: maxCnt;}
	
	for(int i=0; i<M2; i++) {
	    if (avg.count[i] > 0) {
		for(int j=0; j<M1; j++) {
		    double f = 
			Math.sqrt( sp.selfProb[j] * sp.count[j]/(double)maxCnt);
		    avg.avgProb[i][j] *= (f==0 ? 0 : 1/f);
		}
	    }
	}


	reportConfusionMatrix(p1, p2, avg.avgProb, false,"Cosine similarity, "+ in2base);
    }

    /**
     */
    static private class AvgScores {
	/**  avgProb[i][j] is what's called R(i,j) in boa-01.pdf. The
	   meaning of indexes: [newOnto.class][oldOnto.class].  */
	double [][] avgProb, avgLogProb;
	/** [newOnto.class]  */
	int [] count;

	/** Allocates a probability array */
	static double [][] array(int M1, int M2) {
	    double[][] p = new double[M2][];
	    for(int i=0; i<M2; i++) {
		p[i] = new double[M1];
	    }    
	    return p;
	}

	AvgScores(DataSourceParser p1, DataSourceParser p2) 
	    throws BoxerException{
	// the only non-trivial dis
	int did = p1.did();

	int M1 = p1.dis.claCount(), M2 = p2.dis.claCount();
	double[][] sumProb = array(M1,M2), sumLogProb = array(M1,M2);
	// [newOnto.class]
	count = new int[M2];
	
	// score    
	Learner algo= p1.suite.getAllLearners().elementAt(0);

	double [][] vprob =  algo.applyModelLog(p2.data, 0, p2.data.size(),did);
	if (vprob==null) {
	    throw new BoxerException(" algo.applyModelLog failed");
	}

	for(int i=0; i<p2.data.size(); i++) {
	    DataPoint p = p2.data.elementAt(i);
	    int newCid = p.getClasses(p2.suite).elementAt(0).getPos();
	    // overcoming underflow...
	    double[] logProb = vprob[i];
	    double[] prob = expProb(logProb);
	    for(int j=0; j<M1; j++) {
		sumProb[newCid][j] += prob[j];
		sumLogProb[newCid][j] += logProb[j];
	    }
	    count[newCid] ++;
	    
	}
	/*
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
	*/

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

	avgProb = sumProb;
	avgLogProb = sumLogProb;
    }
    }

    /** Processes the "symmetric-matching" command line 
     */
    static void doSymmetric(CMD q)  throws IOException, BoxerException , org.xml.sax.SAXException {
	String in1 = q.f, in2 = q.f2;
	if (in2==null) {
	    usage("'Symmetric' commands expect two arguments'");
	}

	String in1base = DataSourceParser.baseName(in1);
	String in2base = DataSourceParser.baseName(in2);

	if (q.is(CMD_SYM1)) { 
	    // building a joint model on the union of the two ontologies   
	    System.out.println("Matching method: SYM1");

	    final String  prefix1 = "DS1_", prefix2= "DS2_";
	    
	    // Create a single suite for bothe data sources, and read
	    // the first data source in
	    System.out.println("Reading DS1 from file: "+in1);
	    DataSourceParser pJoint = 
		DataSourceParser.parseFile(in1, null, null,prefix1);
	    // how many examples in the first data set?
	    int ds1size = pJoint.data.size();
	    int M1 = pJoint.dis.claCount(); 
	    // read the second data source into the same suite
	    System.out.println("Reading DS2 from file: "+in2);
	    pJoint.readData(in2, true, prefix2);
	    int M2 = pJoint.dis.claCount() - M1; 
	    System.out.println("Found "+M1 + " columns in DS1, "+M2+" columns in DS2");	    

	    // train the joint learner on the examples from both data sources
	    train(pJoint);
	    // apply the joint model to all columns from both DS
	    AvgScores avg = new AvgScores( pJoint, pJoint);

	    double[][] p = AvgScores.array(M1,M2);
	    for(int i0=0; i0<M2; i0++) {
		int i = M1 + i0;
		if (avg.count[i] > 0) {
		    for(int j=0; j<M1; j++) {
			double f =  avg.avgProb[i][i]* avg.avgProb[j][j];
			f = (f==0 ? 0 : 1/f);
			p[i0][j] =  
			    Math.sqrt(avg.avgProb[i][j]*avg.avgProb[j][i]*f);
		    }
		}
	    }
	    reportConfusionMatrix(pJoint, pJoint, 0, M1,  M1, M1+M2,
				  prefix1, prefix2,
				  p, false,
				  "Sym1, "+ in1base + " | " + in2base);
 
	} else if (q.is(CMD_SYM2)) {
	    // Two separate models, one for each DS
	    System.out.println("Reading DS1 from file: "+in1);
	    DataSourceParser p1 = DataSourceParser.parseFile(in1);
	    System.out.println("Reading DS2 from file: "+in2);
	    DataSourceParser p2 = DataSourceParser.parseFile(in2, p1.suite.getDic());
	    int M1 = p1.dis.claCount(); 
	    int M2 = p2.dis.claCount(); 
	    Logging.info("Sym2: M1="+M1+", M2=" +M2);


	    // train both learners
	    train(p1);
	    train(p2);


	    // Compute  self-probs (for normalization in cosine formula)
	    Aux1 sp1 =  selfProb(p1);
	    Aux1 sp2 =  selfProb(p2);

	    // Apply the model trained on each DS to the other DS
	    AvgScores s12 = new AvgScores( p1, p2);
	    AvgScores s21 = new AvgScores( p2, p1);
	    
	    double[][] p = AvgScores.array(M1,M2);
	    for(int i=0; i<M2; i++) {
		for(int j=0; j<M1; j++) {
		    if (s12.count[i] > 0 && s21.count[j] > 0) {
			double f =  sp1.selfProb[j] * sp2.selfProb[i];
			f = (f==0 ? 0 : 1/f);
			p[i][j] =  
			    Math.sqrt(s12.avgProb[i][j]*s21.avgProb[j][i]*f);
		    }
		}
	    }

	    reportConfusionMatrix(p1, p2,  p, false,
				  "Sym2, "+ in1base + " | " + in2base);
 
	} else if (q.is(CMD_VEC_COS) || q.is(CMD_VEC_JS)) {
	    
	    // Vector representation (on the same dictionary)
	    // for each data source
	    System.out.println("Reading DS1 from file: "+in1);
	    DataSourceParser p1 = DataSourceParser.parseFile(in1);
	    System.out.println("Reading DS2 from file: "+in2);
	    DataSourceParser p2 = DataSourceParser.parseFile(in2, p1.suite.getDic());
	    int M1 = p1.dis.claCount(); 
	    int M2 = p2.dis.claCount(); 

	    String vms = ht.getOption("vec.mode",null);
	    FrequencyTable.Mode  mode = (vms==null? FrequencyTable.Mode.TF :
					 FrequencyTable.Mode.valueOf(vms));

	    Logging.info(q + " (mode="+mode+"): M1="+M1+", M2=" +M2);

	    FrequencyTable ft1 = new FrequencyTable(p1, mode);
	    FrequencyTable ft2 = new FrequencyTable(p2, mode);

	    if (q.is(CMD_VEC_COS)) {
		double [][] p = ft2.cosineSim(ft1);
		reportConfusionMatrix(p1, p2,  p, false,
				      "CosineSimilarity(mode="+mode+"), "+ in1base + " | " + in2base);
 

	    } else if  (q.is(CMD_VEC_JS)) {
		double [][] p = ft2.jensenShannonDivergence(ft1);
		reportConfusionMatrix(p1, p2,  p, false,
				      "JensenShannonDivergence(mode="+mode+"), "+ in1base + " | " + in2base);
 	    } 

	} else {
	    throw new IllegalArgumentException(q.toString());
	}  
    }



    /** Result type for a function below */
    static private class Aux1 {
	double[] selfProb;
	int[] count;
	Aux1(int M1) {
	    selfProb = new double[M1];
	    count = new int[M1];
	}
    }

    /** The averaged probabilities of the old data
	source's columns' self-assignment (under the simple
	arithmetic-mean model).
     */
    static  Aux1 selfProb(DataSourceParser p1) throws BoxerException {

	int M1 = p1.dis.claCount();

	Aux1 r = new Aux1(M1);
	Learner algo= p1.suite.getAllLearners().elementAt(0);
	int did = p1.suite.getDid( p1.dis);

	double [][] vprob =  algo.applyModelLog(p1.data, 0, p1.data.size(),did);

	for(int i=0; i<p1.data.size(); i++) {
	    DataPoint p = p1.data.elementAt(i);
	    int newCid = p.getClasses(p1.suite).elementAt(0).getPos();
	    // overcoming underflow...
	    double[] logProb = vprob[i];
	    double[] prob = expProb(logProb);
	    r.selfProb[newCid] += prob[newCid];
	    r.count[newCid] ++;
	}

	/*
	for( DataPoint p: p1.data) {
	    int newCid = p.getClasses(p1.suite).elementAt(0).getPos();
	    // overcoming underflow...
	    double[] logProb = algo.applyModelLog(p)[did];
	    double[] prob = expProb(logProb);
	    r.selfProb[newCid] += prob[newCid];
	    r.count[newCid] ++;
	}
	*/

	for(int i=0; i<M1; i++) {
	    r.selfProb[i] = (r.count[i]==0)? 0 :
		 r.selfProb[i]/r.count[i];
	}

	return r;
    }


    private static void reportConfusionMatrix(DataSourceParser p1, DataSourceParser p2,
					      double prob[][],boolean isLog, 
					      String name)    {
	reportConfusionMatrix(p1,p2, 0, p1.dis.claCount(), 0, p2.dis.claCount(),
			      "", "", 
			      prob, isLog, name);

    }

    /**
       Prints the content of the confusion matrix

       @param p1 The DSP instance whose columns [p1from .. p1to)
       (typically, all columns) correspond to DS1

       @param p2 The DSP instance whose columns [p2from .. p2to)
       (typically, all columns) correspond to DS2

       @param prefix1 Prefix (typically, an empty string) to be found
       in and removed from column names in DS1

       @param prefix2 Prefix (typically, an empty string) to be found
       in and removed from column names in DS1

       @param prob The confusion matrix sized[ p2to-p2from][p1to - p1from]

       @param isLog True means that prob contains logs of scores, rather
       than scores themselves
     */
    private static void reportConfusionMatrix(DataSourceParser p1, DataSourceParser p2,
					      int p1from, int p1to,
					      int p2from, int p2to,
					      String prefix1, String prefix2,
					      double prob[][],boolean isLog, 
					      String name)    {

	NumberFormat fmt = new DecimalFormat("0.0000");
	System.out.println("=== Confusion matrix - "+name+" ===");
	Logging.info("" + p2from + " <=i< " + p2to + "; " +
		     p1from + " <=j< " + p1to);
	for(int i=p2from; i<p2to; i++) {
	    for(int j=p1from; j<p1to; j++) {
		if (j>0) System.out.print("\t");
		double p = prob[i-p2from][j-p1from];
		double q = isLog? Math.exp(p): p;
		System.out.print("P(" + 
				 p1.getColName(j, prefix1) + "|" + 
				 p2.getColName(i, prefix2) + ")=" + q);
	    }
	    System.out.println();
	}

	System.out.println("=== Top matches - "+name+" ===");
	for(int i=p2from; i<p2to; i++) {
	    ScoreWrapper[] w=sortScores( prob[i-p2from]);
	    System.out.print( p2.getColName(i, prefix2) + " :");
	    for(int k=0; k<3 && k<w.length; k++) {
		double q = isLog? Math.exp(w[k].value): w[k].value;
				
		System.out.print("\t{"+p1.getColName(p1from+w[k].i, prefix1)+ 
				 "=" + fmt.format(q) + "}");
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


    private static class ScoreWrapper implements Comparable<ScoreWrapper> {
	int i; double value; 
	public ScoreWrapper( int f, double v) { i=f; value=v;} 
	/** Used for sorting */
	public int compareTo(ScoreWrapper x)  {
	    return x.value > value ? 1 : x.value==value? 0 : -1 ;
	}
    }

    /** descending sort */
    private static ScoreWrapper[] sortScores(double[] scores) {
	return sortScores(scores, 0, scores.length);
    }
    private static ScoreWrapper[] sortScores(double[] scores, int from, int to) {
	ScoreWrapper[] w = new ScoreWrapper[to-from];
	for( int k=from; k< to; k++) {
	    w[k-from]=new ScoreWrapper(k, scores[k]);
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


/*
Copyright 2010, Rutgers University, New Brunswick, NJ.

All Rights Reserved

Permission to use, copy, and modify this software and its
documentation for any purpose other than its incorporation into a
commercial product is hereby granted without fee, provided that the
above copyright notice appears in all copies and that both that
copyright notice and this permission notice appear in supporting
documentation, and that the names of Rutgers University, DIMACS, and
the authors not be used in advertising or publicity pertaining to
distribution of the software without specific, written prior
permission.

RUTGERS UNIVERSITY, DIMACS, AND THE AUTHORS DISCLAIM ALL WARRANTIES
WITH REGARD TO THIS SOFTWARE, INCLUDING ALL IMPLIED WARRANTIES OF
MERCHANTABILITY AND FITNESS FOR ANY PARTICULAR PURPOSE. IN NO EVENT
SHALL RUTGERS UNIVERSITY, DIMACS, OR THE AUTHORS BE LIABLE FOR ANY
SPECIAL, INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER
RESULTING FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF
CONTRACT, NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN
CONNECTION WITH THE USE OR PERFORMANCE OF THIS SOFTWARE.  */
