/**
 *  Compare with BOXERApply
 */
package blackbook.ejb.server.datamanager;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.DateFormat;
import java.util.Calendar;
import java.util.Vector;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import security.ejb.client.User;

import blackbook.ejb.client.exception.BlackbookSystemException;
import blackbook.ejb.server.metadata.MetadataManager;

import com.hp.hpl.jena.rdf.model.Literal;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.util.iterator.ExtendedIterator;

import edu.dimacs.mms.borj.*;
import edu.dimacs.mms.boxer.DataPoint;
import edu.dimacs.mms.boxer.Learner;
import edu.dimacs.mms.boxer.ParseConfig;
import edu.dimacs.mms.boxer.ParseXML;
import edu.dimacs.mms.boxer.Sizeof;
import edu.dimacs.mms.boxer.Suite;
import edu.dimacs.mms.boxer.Version;
import edu.dimacs.mms.boxer.DataPoint.ScoreRun;
import edu.dimacs.mms.tokenizer.RDFtoXML;
import edu.dimacs.mms.tokenizer.RDFtoXML2;
import edu.dimacs.mms.tokenizer.XMLtoRDF;
import edu.dimacs.mms.tokenizer.XMLtoRDF2;

public class BOXERDriverApply extends AbstractAlgorithmMultiModel2Model {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	/** logger */
    private static Log logger = LogFactory.getLog(BOXERDriverUpdate.class);
	
    private static final String DESCRIPTION = "Inputs the appropriately-classified documents as a Jena model and uses this as a foundation to train a new BOXER model.";

    private static final String LABEL = "BOXER Driver Apply";
    
    /* "Ontology" */
    private static final String URN = "urn:blackbook:sampleDocumentsRDB";
    private static final String REGION_PROPERTY_ROOT = "http://www.blackbook.com/boxer/assertion/region#";
    private static final String REGION_PROPERTY_ANALYST = "http://www.blackbook.com/analyst/assertion/region";

	private Document rcv_doc		=	null;
	private Document qrel_doc		=	null;
    
	 /**
     * @see blackbook.ejb.server.datamanager.AbstractAlgorithmImpl#getDescription()
     */
    @Override
    public String getDescription() {
        return DESCRIPTION;
    }

    /**
     * @see blackbook.ejb.server.datamanager.AbstractAlgorithmImpl#getLabel()
     */
    @Override
    public String getLabel() {
        return LABEL;
    }
    
    public static void main (String[] args) throws IOException, BlackbookSystemException {
    	FileInputStream in = new FileInputStream( new File( "C:\\complex.rdf"));
		Model learnercomplex = ModelFactory.createDefaultModel();
		learnercomplex.read(in,null);
		in.close();
		
		FileInputStream in2 = new FileInputStream( new File( "C:\\boxershar.rdf"));
		Model data = ModelFactory.createDefaultModel();
		data.read(in2,null);
		in2.close();
		
		BOXERDriverApply test = new BOXERDriverApply();
		
		test.executeAlgorithm(null,learnercomplex,data);
    }

	public Model executeAlgorithm(User user, Model learnercomplex, Model data)
			throws BlackbookSystemException {
		try {
			
			Model m = ModelFactory.createDefaultModel();
			
			/* Extract QREL and RCV data
			 * We may not need QREL data since we are applying the model,
			 * and hence it is the model's job to guess the QREL data.
			 */
			createQRELDoc(data);
			createRCVDoc(data);
			
			/* At this point, we need to mimic the behavior of Driver.main */
			memory("BORJ startup");

			ParseConfig ht = new ParseConfig();

			System.out.println("Welcome to the BOXER toolkit (version " + Version.version+ ")");
			System.out.println("[VERSION] " + Version.version);


			Suite.verbosity = ht.getOption("verbosity", 1);
			boolean verbose = ht.getOption("verbose", (Suite.verbosity>=3));

			String runid = ht.getOption("runid", mkRunId());
			ParseXML.setDefaultNameBase(runid);

			Suite suite = null; //new Suite();
		 
			// Labels that arrive from separate label files will be stored here
			// until use
			LabelStore qrelStore = new LabelStore();
			
			/* We read the learner complex. */
			
		    System.out.println("Reading learner(s) from model");
		    Document learnercomplex_document = RDFtoXML2.convertLearnerComplexToXML(learnercomplex);
		    suite =  Learner.deserializeLearnerComplex(learnercomplex_document.getDocumentElement());
		    Vector <Learner> algos = suite.getAllLearners();
		    if (algos.size()==0) 
		    	logger.error("The model did not specify even a single learner");
		    //algo = Learner.deserializeLearner(new File(q.f));
			
			int nLearners =  suite.getLearnerCount();
			System.out.println("The suite is used by " +  nLearners + " learners");
			int cnt=0;
			for(Learner algo : suite.getAllLearners()) {
			    if (Suite.verbosity>=0) {
			    	System.out.println("Describing Learner No. "+(cnt++));
			    	algo.describe(System.out, false);
			    	System.out.println("-----------------------------------");
			    }
			}
			
			int trainCnt = 0,
				testCnt=0;
			
			testCnt=testCnt+testCnt;

			Scores  se[] = new Scores[nLearners];       
			for( int i=0; i<se.length; i++) se[i] = new Scores(suite);

			
			/* Now we apply on the datapoints in the model (RCV) */
			
	    	testCnt++;
	    	// read test set
	    	System.out.println("Reading test set (Jena model)");
	    	Document datadoc = RDFtoXML2.convertBOXERDataToRCVXML(data);
	    	Vector<DataPoint> test=
	    		ParseXML.parseDatasetElement(datadoc.getDocumentElement(),suite,false);
	    	qrelStore.applyTo(test, suite, false);
	    	// score each test vector
	    	if (Suite.verbosity>0) 
	    		System.out.println("Test set no. " +testCnt+ " (Jena model) contains " + test.size() + " points, memory use=" + Sizeof.sizeof(test) + " bytes");

	    	cnt=0;
	    	for(Learner algo: suite.getAllLearners()) {
	    		if (Suite.verbosity>0) System.out.println("Scoring test set (Jena model) using learner " + cnt);
				
	    		Scores seLocal = new Scores(suite);
  
	    		for(int i=0; i<test.size(); i++){
	    			DataPoint x = test.elementAt(i);
	    			// overcoming underflow...
	    			double [][] probLog = algo.applyModelLog(x);
	    			double [][] prob = expProb(probLog);
	    			
	    			String score_description = x.describeScores(prob,suite); 
	    			String score_description_long = x.describeScoresLong(prob,suite);
	    			if (Suite.verbosity>0) {
	    				System.out.println("Scored test vector "+i+"; scores=" +
	    									score_description);
	    			}
	    			System.out.println(score_description_long);
	    			String[][] parsed = parseScores(score_description_long);
	    			int parsed_len = parsed.length;
	    			System.out.println("Details for datapoint "+x.getName());
	    			for (int ii=0; ii<parsed_len; ii++) {
	    				System.out.println(parsed[ii][0]+"\t->\t"+parsed[ii][1]);
	    			}
	    			
	    			addBOXERAssertions(m,x.getName(),parsed);
	    			addAnalystAssertion(m,x.getName());
	    			
	    			
	    			
//	    			x.reportScoresAsText(prob,suite,runid,System.out);

	    			seLocal.evalScores(x, suite, prob);
	    			se[cnt].evalScores(x, suite, prob);
	    			x.addLogLik(probLog, suite, 
	    						seLocal.logLikCnt, seLocal.logLik);	
	    			x.addLogLik(probLog, suite, 
	    						se[cnt].logLikCnt, se[cnt].logLik);	

	    		}

	    		// Print report on scores so far
	    		if (Suite.verbosity>=0) {
	    			System.out.println("Scoring report (STDOUT):");
		    
	    			System.out.println(seLocal.scoringReport(suite, "[SCORES][]"));
	    			System.out.println(seLocal.loglikReport(suite, "[LOGLIK][]"));
			
	    		}
	    		cnt++;
	    	}
	    	int ts = test.size();
	    	test = null;		
	    	memory("Scored "+ts+" examples from Jena model");
			
			/* Now we write the learner complex to a new model, which is returned 
			 * We have to modify serializeLearnerComplex to "intercept" the data and
			 * keep things in Document form, not via any file whatsoever.
			 */
			
			System.out.println("Saving all "+nLearners+"  learner(s) to Jena model");
			
			Document xmldoc = suite.serializeLearnerComplex(); // save the entire model
			
			/* Now we have the entire model in Document form. Perfect for XMLtoRDF! */
			
			MetadataManager meta = new MetadataManager();
			String DSname = "BOXER Assertions "+BOXERDriverTrain.getTimestamp();
			meta.createNewAssertionsDS(DSname);
			/* Don't just create it, PERSIST it! */
			JenaAndLuceneReplaceOrAdd j = new JenaAndLuceneReplaceOrAdd();
			Model new_learnercomplex = null;
			new_learnercomplex = XMLtoRDF2.convertToRDF(xmldoc);
			j.executeAlgorithm(user,DSname,m);
			return new_learnercomplex;
			
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ParserConfigurationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SAXException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (TransformerException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return null;
		
	}
	
	/** createQRELDoc
	 *  Same as before, except different inputs (model is implicit). This is made
	 *  so the user has control over which Resource is used, for Jena does not
	 *  always give the same resource if you ask it for the first one in a model!
	 * @param f		-	a BOXERFilter
	 * @param r		-	a Resource (presumably in f)
	 * @throws ParserConfigurationException
	 */
	
	public void createQRELDoc(Model m, Resource r) throws ParserConfigurationException {
		String region = BOXERFilter.findRegion(m,r);
		DocumentBuilderFactory dbfac = DocumentBuilderFactory.newInstance();
	    DocumentBuilder docBuilder = dbfac.newDocumentBuilder();
	    qrel_doc = docBuilder.newDocument();
	    
	    Element dataset = qrel_doc.createElement("dataset");
	    dataset.setAttribute("name","sample");
	    dataset.setAttribute("version","0.5.007");
	    
	    qrel_doc.appendChild(dataset);
	    
	    Element datapoint = qrel_doc.createElement("datapoint");
	    datapoint.setAttribute("name",r.toString());
	    
	    dataset.appendChild(datapoint);
	    
	    Element labels = qrel_doc.createElement("labels");
	    labels.setTextContent("region:"+region);
	    
	    datapoint.appendChild(labels);	
	}
	
	/** createQRELDoc
	 *  This creates a QREL document from ALL of the documents in the model.
	 * @param f
	 * @param m
	 * @throws ParserConfigurationException
	 */
	
	public void createQRELDoc(Model m) throws ParserConfigurationException {
		
		ExtendedIterator resources = BOXERFilter.listURIResources(m);
		Resource r;
		
		DocumentBuilderFactory dbfac = DocumentBuilderFactory.newInstance();
	    DocumentBuilder docBuilder = dbfac.newDocumentBuilder();
	    qrel_doc = docBuilder.newDocument();

	    Element dataset = qrel_doc.createElement("dataset");
	    dataset.setAttribute("name","sample");
	    dataset.setAttribute("version","0.5.007");
	    
	    qrel_doc.appendChild(dataset);
	    
		while (resources.hasNext()) {
			r = (Resource) resources.next();
			String region = BOXERFilter.findRegion(m,r);
		    
		    Element datapoint = qrel_doc.createElement("datapoint");
		    datapoint.setAttribute("name",r.toString());
		    
		    dataset.appendChild(datapoint);
		    
		    Element labels = qrel_doc.createElement("labels");
		    labels.setTextContent("region:"+region);
		    
		    datapoint.appendChild(labels);
		}
	}
	
	/** createRCVDoc
	 *  Same as createQRELDoc, except, of course, we create the RCV doc. 
	 *  This creates the RCV document with all documents that are in it.
	 * @param f
	 * @param r
	 * @throws ParserConfigurationException
	 */
	
	public void createRCVDoc(Model m) throws ParserConfigurationException {
		
		ExtendedIterator resources = BOXERFilter.listURIResources(m);
		Resource r;
		
		DocumentBuilderFactory dbfac = DocumentBuilderFactory.newInstance();
	    DocumentBuilder docBuilder = dbfac.newDocumentBuilder();
	    rcv_doc = docBuilder.newDocument();
	    
	    Element dataset = rcv_doc.createElement("dataset");
	    dataset.setAttribute("name","sample");
	    dataset.setAttribute("version","0.5.007");
	    
	    rcv_doc.appendChild(dataset);
	    
	    while (resources.hasNext()) { 
	    
	    	r = (Resource) resources.next();
	    	
		    Element datapoint = rcv_doc.createElement("datapoint");
		    datapoint.setAttribute("name",r.toString());
		    
		    dataset.appendChild(datapoint);
		    
		    Element features = rcv_doc.createElement("features");
		    features.setTextContent(BOXERFilter.findFeatures(m,r));
		    
		    datapoint.appendChild(features);
	    }
	}
	
	public void createRCVDoc(Model m, Resource r) throws ParserConfigurationException {
		DocumentBuilderFactory dbfac = DocumentBuilderFactory.newInstance();
	    DocumentBuilder docBuilder = dbfac.newDocumentBuilder();
	    rcv_doc = docBuilder.newDocument();
	    
	    Element dataset = rcv_doc.createElement("dataset");
	    dataset.setAttribute("name","sample");
	    dataset.setAttribute("version","0.5.007");
	    
	    rcv_doc.appendChild(dataset);
	    
	    Element datapoint = rcv_doc.createElement("datapoint");
	    datapoint.setAttribute("name",r.toString());
	    
	    dataset.appendChild(datapoint);
	    
	    Element features = rcv_doc.createElement("features");
	    features.setTextContent(BOXERFilter.findFeatures(m,r));
	    
	    datapoint.appendChild(features);	
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
    	System.out.println(	"[MEMORY]"	+
    						s			+
    						" max=" 	+
    						mmem 		+
    						", total=" 	+
    						tmem 		+
    						", free=" 	+
    						fmem 		+
    						", used=" 	+
    						used);	
    }
    
    /** Produces default run id, if none is supplied */
    static private String mkRunId() {
    	Calendar now = Calendar.getInstance();	
    	long time = now.getTimeInMillis();
    	System.out.println(	"[TIME][START] " +
    						DateFormat.getDateInstance().format(now.getTime()) +
    						" ("+time+")");
    	return "" + time;
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
	
    private String[][] parseScores (String scores) {
    	String[] parts = scores.split(" ");
    	int len = parts.length/2;
    	String[][] result = new String[len][2];
    	for (int i=0; i<len; i++) {
    		result[i][0] = parts[2*i];
    		result[i][1] = parts[2*i+1];
    	}
    	return result;
    }
    
    public void addBOXERAssertions(Model m, String datapoint_name, String[][] scores) {
    	Resource r = m.createResource(URN+datapoint_name);
    	int len = scores.length;
    	for (int i=0; i<len; i++) {
    		Property p = m.createProperty(REGION_PROPERTY_ROOT+scores[i][0]);
    		Literal l = m.createLiteral(scores[i][1]);
    		m.add(m.createStatement(r,p,l));
    	}
    }
    
    public void addAnalystAssertion(Model m, String datapoint_name) {
    	Resource r = m.createResource(URN+datapoint_name);
    	Property p = m.createProperty(REGION_PROPERTY_ANALYST);
    	Literal l = m.createLiteral("");
    	m.add(m.createStatement(r,p,l));
    }
}