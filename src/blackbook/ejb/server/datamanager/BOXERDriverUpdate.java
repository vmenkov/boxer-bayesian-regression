/**
 *  Compare with BOXERUpdate
 */
package blackbook.ejb.server.datamanager;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
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

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
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
import edu.dimacs.mms.tokenizer.RDFtoXML;
import edu.dimacs.mms.tokenizer.RDFtoXML2;
import edu.dimacs.mms.tokenizer.XMLtoRDF;
import edu.dimacs.mms.tokenizer.XMLtoRDF2;

public class BOXERDriverUpdate extends AbstractAlgorithmMultiModel2Model {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	/** logger */
    private static Log logger = LogFactory.getLog(BOXERDriverUpdate.class);
	
    private static final String DESCRIPTION = "Inputs the appropriately-classified documents as a Jena model and uses this as a foundation to train a new BOXER model.";

    private static final String LABEL = "BOXER Driver Update";

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
    
    public static void main (String[] args) throws IOException, ParserConfigurationException, TransformerException {
    	BOXERDriverUpdate test = new BOXERDriverUpdate();
    	FileInputStream in = new FileInputStream( new File( "C:\\boxerfillin.rdf"));
		Model data = ModelFactory.createDefaultModel();
		data.read(in,null);
		in.close();
		
		test.createQRELDoc(data);
		test.createRCVDoc(data);
		
		System.out.println(XMLtoRDF2.convertToString(test.qrel_doc));
		System.out.println(XMLtoRDF2.convertToString(test.rcv_doc));
    }

	public Model executeAlgorithm(User user, Model learnercomplex, Model data)
			throws BlackbookSystemException {
		try {
			/* Extract QREL and RCV data */
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
		    if (algos.size()==0) logger.error("The model did not specify even a single learner");
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

			/* The next thing to do would be to read the labels (QREL file) */
			System.out.println("Reading labels from input model");
			qrelStore.readXML(qrel_doc);
			
			/* Now we train on the datapoints in the model (RCV) */
			
			trainCnt++;
			
			// read training set
			System.out.println("Reading training set no. " +trainCnt+ " (Jena model)");
			Vector<DataPoint> train = 
			    ParseXML.parseDatasetElement(rcv_doc.getDocumentElement(), suite, true);
			
			// Update labels from LabelStore
			qrelStore.applyTo(train, suite, true);

			// Do this in case new discriminations (with default classes)
			// have been added during reading the XML file. (since ver 0.6)
			//for(DataPoint p: train) p.addDefaultClasses(suite);

			if (Suite.verbosity>0) 
				System.out.println(suite.describe());
			if (verbose) 
				System.out.println(suite.getDic().describe());
			if (Suite.verbosity>0) 
				System.out.println("Training set no. " +trainCnt+ " (model) contains " + train.size() + " points, memory use=" + Sizeof.sizeof(train) + " bytes");
			for(int i =0; i<train.size(); i++) {
			    if (verbose) System.out.println(train.elementAt(i));
			}

			// train
			memory("Read train set; starting to train");
			cnt=0;
			for(Learner algo: suite.getAllLearners()) {
			    algo.absorbExample(train);

			    if (Suite.verbosity>=0) {
			    	System.out.println("Describing Learner No. "+(cnt++));
			    	algo.describe(System.out, false);
			    	System.out.println("-----------------------------------");
			    } 
			    else {
			    	System.out.println("[NET] Leaner no. " + (cnt++)+ " net memory use=" + algo.memoryEstimate());
			    }
			    
//			    // In verbose mode, write out the model after every training file
//			    if (verbose) 
//			    	algo.saveAsXML(algo.algoName() + "-out" + trainCnt + ".xml");
			}
			int ts = train.size();
			train = null;		
			memory("Absorbed "+ts+" examples from model");
			
			/* Now we write the learner complex to a new model, which is returned 
			 * We have to modify serializeLearnerComplex to "intercept" the data and
			 * keep things in Document form, not via any file whatsoever.
			 */
			
			System.out.println("Saving all "+nLearners+"  learner(s) to Jena model");
			
			Document xmldoc = suite.serializeLearnerComplex(); // save the entire model
			
			/* Now we have the entire model in Document form. Perfect for XMLtoRDF! */
			MetadataManager meta = new MetadataManager();
			String DSname = "BOXER"+getTimestamp();
			meta.createNewAssertionsDS(DSname);
			/* Don't just create it, PERSIST it! */
			JenaAndLuceneReplaceOrAdd j = new JenaAndLuceneReplaceOrAdd();
			Model m = XMLtoRDF2.convertToRDF(xmldoc);
			j.executeAlgorithm(user,DSname,m);
			return m;		
			
			
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
	
    public static String getTimestamp() {
		Calendar cal = Calendar.getInstance();
	    SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
	    return sdf.format(cal.getTime());
	    
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
	    dataset.setAttribute("version","0.6.007");
	    
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
	
}