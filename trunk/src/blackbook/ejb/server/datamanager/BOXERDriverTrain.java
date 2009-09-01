/**
 *  Compare with BOXERTrain
 */
package blackbook.ejb.server.datamanager;

import java.io.IOException;
import java.io.StringWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import security.ejb.client.User;

import blackbook.ejb.client.exception.BlackbookSystemException;
import blackbook.ejb.server.metadata.MetadataManager;
import blackbook.ejb.server.metadata.MetadataManagerFactory;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.util.iterator.ExtendedIterator;

import edu.dimacs.mms.borj.*;
import edu.dimacs.mms.boxer.BoxerXMLException;
import edu.dimacs.mms.boxer.DataPoint;
import edu.dimacs.mms.boxer.Learner;
import edu.dimacs.mms.boxer.ParseConfig;
import edu.dimacs.mms.boxer.ParseXML;
import edu.dimacs.mms.boxer.Sizeof;
import edu.dimacs.mms.boxer.Suite;
import edu.dimacs.mms.boxer.Version;
import edu.dimacs.mms.tokenizer.XMLtoRDF;
import edu.dimacs.mms.tokenizer.XMLtoRDF2;

public class BOXERDriverTrain extends AbstractAlgorithmModel2Model {
	
    /**
	 * 
	 */
	private static final long serialVersionUID = 1869786217915436769L;

	private static final String DESCRIPTION = "Inputs the appropriately-classified documents as a Jena model and uses this as a foundation to train a new BOXER model.";

    private static final String LABEL = "BOXER Driver Train";
    
	/* File usage in blackbook is done through the %BLACKBOOK_HOME% directory. */
    
	private Document rcv_doc		=	null;
	private Document qrel_doc		=	null;
	private Document learner_doc	=	null;
	private Document suite_doc		= 	null;
	
	/* In this version, we do away with files. */
	
//	private File rcv_file			=	null;
//	private File qrel_file			=	null;
//	private File suite_file			=	null;
//	private File learner_file		=	null;
	
	private void setupSuiteAndLearner() throws ParserConfigurationException, TransformerException, IOException {
		
		learner_doc = createLearner();
		suite_doc	= createSuite();
		
//		TransformerFactory transfac = TransformerFactory.newInstance();
//		
//		/*
//		For some reason blackbook does not like this method call!
//		transfac.setAttribute("indent-number", new Integer(4));
//		*/
//		
//        Transformer trans = transfac.newTransformer();
//        trans.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
//        trans.setOutputProperty(OutputKeys.INDENT, "yes");
//
//        StringWriter suite_sw = new StringWriter();
//        StreamResult suite_result = new StreamResult(suite_sw);
//        DOMSource suite_source = new DOMSource(suite_doc);
//        trans.transform(suite_source, suite_result);
//        String suite_string = suite_sw.toString();
//        
//        StringWriter learner_sw = new StringWriter();
//        StreamResult learner_result = new StreamResult(learner_sw);
//        DOMSource learner_source = new DOMSource(learner_doc);
//        trans.transform(learner_source, learner_result);
//        String learner_string = learner_sw.toString();
        
        /* In this version, we do away with files. */
//        suite_file = new File(SUITE_FILENAME);
//        learner_file = new File(LEARNER_FILENAME);
//        
//        FileWriter suite_fw = new FileWriter(suite_file);
//        FileWriter learner_fw = new FileWriter(learner_file);
//	     
//	    suite_fw.write(suite_string);
//	    suite_fw.close();
//	    
//	    learner_fw.write(learner_string);
//	    learner_fw.close();
	}
	
	/** createSuite
	 *  This method creates a basic suite file by hand. Just for practice.
	 * @return
	 * @throws ParserConfigurationException 
	 */
	
	public Document createSuite() throws ParserConfigurationException {
		DocumentBuilderFactory dbfac = DocumentBuilderFactory.newInstance();
	    DocumentBuilder docBuilder = dbfac.newDocumentBuilder();
	    Document doc = docBuilder.newDocument();
	    
	    Element suite = doc.createElement("suite");
	    suite.setAttribute("name","RCV_Small_20081206");
	    suite.setAttribute("nctest","IGNORE");
	    suite.setAttribute("nctrain","ADD");
	    suite.setAttribute("version", "0.5.006");
	    
	    doc.appendChild(suite);
	    
	    Element discrimination = doc.createElement("discrimination");
	    discrimination.setAttribute("defaultclass","other");
	    discrimination.setAttribute("leftoverclass","other");
	    discrimination.setAttribute("name", "region");
	    discrimination.setAttribute("qrel","null");
	    
	    suite.appendChild(discrimination);
	    
	    Element classes = doc.createElement("classes");
	    classes.setTextContent("USA UK JAP other");
	    
	    discrimination.appendChild(classes);
	    
	    return doc;      
	}


	/** 
	 *  This method creates a basic learner file with no parameters.
	 *  It will be an EG learner. 
	 * @return A basic learner.
	 * @throws ParserConfigurationException
	 */
	
	public Document createLearner() throws ParserConfigurationException {
		DocumentBuilderFactory dbfac = DocumentBuilderFactory.newInstance();
	    DocumentBuilder docBuilder = dbfac.newDocumentBuilder();
	    Document doc = docBuilder.newDocument();
	    
	    Element learner = doc.createElement("learner");
	    learner.setAttribute("name","boxer.ExponentiatedGradient");
	    learner.setAttribute("version", "0.6.003");
	    
	    doc.appendChild(learner);
	    
	    Element parameters = doc.createElement("parameters");
	    parameters.setTextContent(" ");
	    
	    learner.appendChild(parameters);
	    
	    return doc;
	}
	
	/** 
	 * This method creates a more involved learner document that can be fed
	 * into BOXER. You specify the name of the learner you want, and you 
	 * also supply the parameters as a String->String mapping. 
	 * 
	 * <br>
	 * 
	 * <b>Note:</b> No checking against BOXER is done here. BOXER may very well not 
	 * like the inputs you gave it via this method.
	 * @param learnername The name of the learner - e.g. boxer.ExponentiatedGradient
	 * @param parameters Parameters associated with this learner.
	 * @return The DOM Document representing this learner.
	 * @throws ParserConfigurationException 
	 */
	
	public Document createLearner(String learnername, Map<String,String> parameters) throws ParserConfigurationException {
		DocumentBuilderFactory dbfac = DocumentBuilderFactory.newInstance();
	    DocumentBuilder docBuilder = dbfac.newDocumentBuilder();
	    Document doc = docBuilder.newDocument();
	    
	    Element learner = doc.createElement("learner");
	    learner.setAttribute("name",learnername);
	    learner.setAttribute("version", "0.6.003");
	    
	    doc.appendChild(learner);
	    
	    Element params = doc.createElement("parameters");
	    
//	    params.setTextContent(" ");
	    
	    learner.appendChild(params);
	    
	    Set<String> keys = parameters.keySet();
	    for (String k : keys) {
	    	Element p = doc.createElement("parameter");
	    	p.setAttribute("name",k);
	    	p.setAttribute("value",parameters.get(k));
	    	params.appendChild(p);
	    }
	    return doc;
	}
	
//	/** createQRELDoc
//	 *  This method achieves a simple goal - it fills in the private
//	 *  Document qrel_doc with useful information. It only takes one of the
//	 *  documents from the input model, and it is not guaranteed which
//	 *  one specifically will be picked!
//	 * @param m		-	a Jena model
//	 * @throws ParserConfigurationException 
//	 */
//	
//	public void createQRELDoc(Model m) throws ParserConfigurationException {
//		ExtendedIterator resources = BOXERFilter.listURIResources(m);
//		
//		/* Extract the region */
//		if (resources.hasNext()) {
//			Resource r = (Resource) resources.next();
//			createQRELDoc(m,r);
//		}
//	}
	
	/** 
	 *  This creates a QREL document from ALL of the documents in the model.
	 * @param m A Jena model properly formatted with BOXER region/industry data.
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
	    dataset.setAttribute("version","0.6.003");
	    
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
	
	/** createQRELDoc
	 *  This is made
	 *  so the user has control over which Resource is used, for Jena does not
	 *  always give the same resource if you ask it for the first one in a model!
	 * @param m	A Jena Model properly formatted with BOXER region/industry data.
	 * @param r	A Resource (presumably in m)
	 * @throws ParserConfigurationException
	 */
	
	public void createQRELDoc(Model m, Resource r) throws ParserConfigurationException {
		String region = BOXERFilter.findRegion(m,r);
		DocumentBuilderFactory dbfac = DocumentBuilderFactory.newInstance();
	    DocumentBuilder docBuilder = dbfac.newDocumentBuilder();
	    qrel_doc = docBuilder.newDocument();
	    
	    Element dataset = qrel_doc.createElement("dataset");
	    dataset.setAttribute("name","sample");
	    dataset.setAttribute("version","0.6.003");
	    
	    qrel_doc.appendChild(dataset);
	    
	    Element datapoint = qrel_doc.createElement("datapoint");
	    datapoint.setAttribute("name",r.toString());
	    
	    dataset.appendChild(datapoint);
	    
	    Element labels = qrel_doc.createElement("labels");
	    labels.setTextContent("region:"+region);
	    
	    datapoint.appendChild(labels);	
	}
	

	
	/** createRCVDoc
	 *  Same as createQRELDoc, except, of course, we create the RCV doc. 
	 *  This creates the RCV document with all documents that are in it.
	 * @param m A Jena Model properly formatted with region/industry data.
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
	    dataset.setAttribute("version","0.6.003");
	    
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
	    dataset.setAttribute("version","0.6.003");
	    
	    rcv_doc.appendChild(dataset);
	    
	    Element datapoint = rcv_doc.createElement("datapoint");
	    datapoint.setAttribute("name",r.toString());
	    
	    dataset.appendChild(datapoint);
	    
	    Element features = rcv_doc.createElement("features");
	    features.setTextContent(BOXERFilter.findFeatures(m,r));
	    
	    datapoint.appendChild(features);	
	}

//	public void main (String[] argv) throws ParserConfigurationException, TransformerException, IOException, SAXException {
//         
//         Model m = ModelFactory.createDefaultModel();
//         FileInputStream file = new FileInputStream("c:\\integration\\RDFXMLresults.rdf");
//         m.read(file,"");
//         file.close();
//         
//         createQRELDoc(m);
//         System.out.println(XMLtoString(qrel_doc));
//         createRCVDoc(m);
//         System.out.println(XMLtoString(rcv_doc));
//         
//         rcv_file = new File(RCV_FILENAME);
//         qrel_file = new File(QREL_FILENAME);
//         suite_file = new File(SUITE_FILENAME);
//         learner_file = new File(LEARNER_FILENAME);
//         
//         FileWriter rcv_fw = new FileWriter(rcv_file);
//         FileWriter qrel_fw = new FileWriter(qrel_file);
//         FileWriter suite_fw = new FileWriter(suite_file);
//         FileWriter learner_fw = new FileWriter(learner_file);
//         
//         rcv_fw.write(XMLtoString(rcv_doc));
//         qrel_fw.write(XMLtoString(qrel_doc));
//         suite_fw.write(XMLtoString(suite_doc));
//         learner_fw.write(XMLtoString(learner_doc));
//         
//         rcv_fw.close();
//         qrel_fw.close();
//         suite_fw.close();
//         learner_fw.close();
//         
//         String[] driver_args = {"read-suite:"+SUITE_FILENAME,
//        		 				 "read-learner:"+LEARNER_FILENAME,
//        		 				 "read-labels:"+QREL_FILENAME,
//        		 				 "train:"+RCV_FILENAME,
//        		 				 "write:complex.xml"};
//         
//         Driver.main(driver_args);
//	}
	
	/** 
	 *  Takes in a DOM Document and returns it in string form, presumably
	 *  to write to a file.
	 *  @param doc The DOM Document to convert to a string.
	 *  @return A string representing the input DOM Document.
	 */
	
	public static String XMLtoString(Document doc) throws TransformerException {
		TransformerFactory transfac = TransformerFactory.newInstance();
//        transfac.setAttribute("indent-number", new Integer(4));
        Transformer trans = transfac.newTransformer();
        trans.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
        trans.setOutputProperty(OutputKeys.INDENT, "yes");
		StringWriter sw = new StringWriter();
        StreamResult result = new StreamResult(sw);
        DOMSource source = new DOMSource(doc);
        trans.transform(source, result);
        return sw.toString();
	}
	
	public Document getQRELDoc() {
		return qrel_doc;
	}

	/** 
	 *  Executes the BOXER Driver algorithm, consisting of the following steps:
	 *  <ol>
	 *  <li> Creates the QREL, RCV, and Learner DOM Documents.</li> 
	 *  <li> Runs BORJ.Driver starting with an empty Suite.</li>
	 *  <li> Reads in the default learner.</li>
	 *  <li> Reads the QREL labels given in the QREL DOM Document.</li>
	 *  <li> Trains on the RCV DOM Document</li>
	 *  <li> Writes everything to a learner complex, which is then converted to RDF.</li>
	 *  </ol>
	 *  @param user The blackbook user (not used in the algorithm).
	 *  @param model The Jena Model contaning the information about the documents.
	 *  @return The learner complex in RDF format representing the learner after
	 *  training on the documents.
	 */

	public Model executeAlgorithm(User user, Model model)
			throws BlackbookSystemException {
		try {
			setupSuiteAndLearner();
			createQRELDoc(model);
			createRCVDoc(model);
			
//			rcv_file = new File(RCV_FILENAME);
//			qrel_file = new File(QREL_FILENAME);
//			
//			FileWriter rcv_fw = new FileWriter(rcv_file);
//			FileWriter qrel_fw = new FileWriter(qrel_file);
//			FileWriter suite_fw = new FileWriter(suite_file);
//			FileWriter learner_fw = new FileWriter(learner_file);
//			
//			rcv_fw.write(XMLtoString(rcv_doc));
//			qrel_fw.write(XMLtoString(qrel_doc));
//			suite_fw.write(XMLtoString(suite_doc));
//			learner_fw.write(XMLtoString(learner_doc));
//			
//			rcv_fw.close();
//			qrel_fw.close();
//			suite_fw.close();
//			learner_fw.close();
			
//			String[] driver_args = {"read-suite:"+SUITE_FILENAME,
//	        		 				 "read-learner:"+LEARNER_FILENAME,
//	        		 				 "read-labels:"+QREL_FILENAME,
//	        		 				 "train:"+RCV_FILENAME,
//	        		 				 "write:complex.xml"};
//	         
//			Driver.main(driver_args);
			
			/* At this point, we need to mimic the behavior of Driver.main */
			memory("BORJ startup");

			ParseConfig ht = new ParseConfig();

			System.out.println("Welcome to the BOXER toolkit (version " + Version.version+ ")");
			System.out.println("[VERSION] " + Version.version);


			Suite.verbosity = ht.getOption("verbosity", 1);
			boolean verbose = ht.getOption("verbose", (Suite.verbosity>=3));

			String runid = ht.getOption("runid", mkRunId());
			DataPoint.setDefaultNameBase(runid);

			Suite suite = null; //new Suite();
		 
			// Labels that arrive from separate label files will be stored here
			// until use
			LabelStore qrelStore = new LabelStore();
			
			/* We start with the hard-coded suite */
			
			suite = new Suite(suite_doc.getDocumentElement());
			
			/* We have the learner, which is hard-coded for now */
			
			suite.addLearner(learner_doc.getDocumentElement());
			
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
			String DSname = "BOXER "+getTimestamp();
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
		} catch (TransformerException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SAXException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (BoxerXMLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return null;
		
	}
	
    /** Produces default run id, if none is supplied. */
    static private String mkRunId() {
    	Calendar now = Calendar.getInstance();	
    	long time = now.getTimeInMillis();
    	System.out.println(	"[TIME][START] " +
    						DateFormat.getDateInstance().format(now.getTime()) +
    						" ("+time+")");
    	return "" + time;
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
    
    public static String getTimestamp() {
		Calendar cal = Calendar.getInstance();
	    SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
	    return sdf.format(cal.getTime());
	    
    }
	
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
	
}