package blackbook.ejb.server.datamanager;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Set;
import java.util.Vector;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import security.ejb.client.User;
import blackbook.ejb.client.exception.BlackbookSystemException;
import blackbook.ejb.server.metadata.MetadataManager;

import com.hp.hpl.jena.rdf.model.Literal;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;

import edu.dimacs.mms.borj.Driver;
import edu.dimacs.mms.borj.LabelStore;
import edu.dimacs.mms.borj.Scores;
import edu.dimacs.mms.boxer.BoxerXMLException;
import edu.dimacs.mms.boxer.DataPoint;
import edu.dimacs.mms.boxer.Discrimination;
import edu.dimacs.mms.boxer.Learner;
import edu.dimacs.mms.boxer.ParseXML;
import edu.dimacs.mms.boxer.Sizeof;
import edu.dimacs.mms.boxer.Suite;
import edu.dimacs.mms.tokenizer.RDFtoXML2;

public class BOXERApply extends AbstractAlgorithmMultiModel2Model {

	/* For testing purposes ONLY */
	public static void main (String[] args) throws BlackbookSystemException {
		Model m_combined = BOXERTools.readFileToModel(BOXERTerms.TEST_DIR + "combined-testdocs.rdf");
		System.out.println("Read combined model");
		Model m_complex = BOXERTools.readFileToModel(BOXERTerms.TEST_DIR + "monterey-complex-after-train.rdf");
		System.out.println("Read learner complex");
		
		BOXERApply test = new BOXERApply();
		
		test.executeAlgorithm(new User(),m_complex,m_combined);
	}
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -1275986399236096342L;

	/* We follow borj.Driver() - we first read in the learner complex,
	 * then we test/apply on the documents. 
	 * The two are stored in a dumb manner, and can be retrieved easily. 
	 * Then ALL of the results will be stored in an assertions datasource.
	 * (non-Javadoc)
	 * @see blackbook.ejb.client.datamanager.AlgorithmMultiModel2Model#executeAlgorithm(security.ejb.client.User, com.hp.hpl.jena.rdf.model.Model, com.hp.hpl.jena.rdf.model.Model)
	 */
	public Model executeAlgorithm(User user, Model m_learnercomplex, Model m_combined)
			throws BlackbookSystemException {
		
		Document d_learnercomplex = BOXERTools.convertToXML(m_learnercomplex);
		Document d_combined = BOXERTools.convertToXML(m_combined);
		
		Document d_testset = getTestingDocument(d_combined);
		Document d_parameters = getParametersDocument(d_combined);
		
		Model assertions = ModelFactory.createDefaultModel();
		
		HashMap<String,String> discrimination_property_map = getDiscriminationPropertyMap(d_parameters);
		
		/* We read in the learner complex */
		try {
			/* Read in the learner complex */
			Suite suite =  Learner.deserializeLearnerComplex(d_learnercomplex.getDocumentElement());
			
			LabelStore qrelStore = new LabelStore();
			
			/* Now we test - modified from edu.dimacs.mms.borj.Driver */

			System.out.println("Reading test set from Jena model");
			Vector<DataPoint> test = ParseXML.parseDatasetElement(d_testset.getDocumentElement(),suite,false);
			qrelStore.applyTo(test, suite, false);
			
			// score each test vector
			if (Suite.verbosity>0) 
				System.out.println("Test set (Jena model) contains " + test.size() + " points, memory use=" + Sizeof.sizeof(test) + " bytes");

			for(Learner algo: suite.getAllLearners()) {
			    if (Suite.verbosity>0) 
			    	System.out.println("Scoring test set (Jena model) using learner");
		
			    Scores seLocal = new Scores(suite);
	  
			    for(int i=0; i<test.size(); i++){
			    	DataPoint x = test.elementAt(i);
			    	System.out.println("Testing " + x.getName());
			    	// overcoming underflow...
			    	/* This is where we actually apply */
			    	double [][] probLog = algo.applyModelLog(x);
			    	double [][] prob = Driver.expProb(probLog);
			    	
			    	HashMap<String,String> discrimination_prob_map = getDiscriminationProbabilitiesMap(prob,suite,x);
			    	Set<String> discriminations = discrimination_prob_map.keySet();
			    	
			    	for (String d : discriminations) {
			    		String property = discrimination_property_map.get(d);
			    		String probs = discrimination_prob_map.get(d);
			    		/* This means we have a match, and we should make assertions */
			    		if (property != null && probs != null) {
			    			String[] pairs = probs.split(BOXERTerms.SPLIT_REGEX);
			    			int len = (pairs.length/2)*2;
			    			int twice_len = 2*len;
			    			for (int j=0; j<twice_len; j+=2) {
			    				Resource r = assertions.createResource(x.getName());
			    				/* This property is of form URL#CLASS */
			    				Property p = assertions.createProperty(property + "#",pairs[j]);
			    				Literal l = assertions.createLiteral(String.format("%4.3f",pairs[j+1]));
			    				/* Now we add the statement! */
			    				assertions.createStatement(r,p,l);
			    			}
			    		}
			    	}

			    	if (Suite.verbosity>0) 
			    		System.out.println("Scored test vector "+i+"; scores=" + x.describeScores(prob, suite));

			    	seLocal.evalScores(x, suite, prob);
			    	x.addLogLik(probLog, suite, seLocal.logLikCnt, seLocal.logLik);	

			    }
			    
			    if (Suite.verbosity>=0) {
			    	System.out.println("Scoring report (Jena model):");
			    
			    	System.out.println(seLocal.scoringReport(suite, "[SCORES][Jena model]"));
			    	System.out.println(seLocal.loglikReport(suite, "[LOGLIK][Jena model]"));
				
			    }
			}
			int ts = test.size();
			test = null;		
			memory("Scored "+ts+" examples from Jena Model");
			
			/* The learner complex does not change when testing */
			
			/* Now we have the entire model in Document form. Perfect for XMLtoRDF! */
			MetadataManager meta = new MetadataManager();
			String DSname = "BOXER Assertions " + getDocumentElementName(d_testset) + getTimestamp();
			meta.createNewAssertionsDS(DSname);
			
			/* Don't just create it, PERSIST it! */
			JenaAndLuceneReplaceOrAdd j = new JenaAndLuceneReplaceOrAdd();
			j.executeAlgorithm(user,DSname,assertions);
			
			return assertions;		
			
		} catch (BoxerXMLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SAXException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return null;

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
    
    public static String getDocumentElementName(Document doc) {
    	String name = doc.getDocumentElement().getAttribute(ParseXML.ATTR.NAME_ATTR);
    	if (name.length() == 0) {
    		return doc.getDocumentElement().getTagName();
    	}
    	else {
    		return name;
    	}
    }
    
    public static String getTimestamp() {
		Calendar cal = Calendar.getInstance();
	    SimpleDateFormat sdf = new SimpleDateFormat("yyyy MM dd HH mm ss");
	    return sdf.format(cal.getTime());
	    
    }
    
    /* This method takes in a document and looks at its top-level elements past the root.
     * If one of them matches the tagname we're looking for, then it puts 
     * that element in its own document and returns it. Otherwise, returns null;
     */
    public Document subDocumentFinder(Document total, String tagname) {
    	DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder docBuilder = null;
		Document d_docs = null;
		
		try {
			docBuilder = docBuilderFactory.newDocumentBuilder();
			d_docs = docBuilder.newDocument();
			
			Element root = total.getDocumentElement();
			Node n = root.getFirstChild();
			
			while (n != null) {
				if (n instanceof Element && n.getNodeName().equals(tagname)){
					Element e = (Element) d_docs.importNode(n, true);
					d_docs.appendChild(e);
					return d_docs;
				}
				n = n.getNextSibling();
			}
			
		} catch (ParserConfigurationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		/* If we get here, it means we were not able to extract the correct document */
		return null;
    }
    
    public Document getTestingDocument(Document total) {
    	return subDocumentFinder(total,ParseXML.NODE.DATASET);
    }
    
    public Document getParametersDocument(Document total) {
    	return subDocumentFinder(total,BOXERTerms.DATASET_TEMPLATE);
    }
    
    public HashMap<String,String> getDiscriminationPropertyMap(Document properties) {
    	String content = "";
    	/* We don't care what the name is of this */
    	Element root = properties.getDocumentElement();
    	Node n = root.getFirstChild();
    	
    	/* Gather all feature strings */
    	while (n != null) {
    		if (n instanceof Element && n.getNodeName().equals(ParseXML.NODE.FEATURES)) {
    			content += n.getTextContent() + " ";
    		}
    		n = n.getNextSibling();
    	}
    	
    	/* Make the map */
    	String[] s_list = content.split(BOXERTerms.SPLIT_REGEX);
		int len = (s_list.length/2)*2;
		int twice_len = 2*len;

		HashMap<String,String> map = new HashMap<String,String>();
		for (int i=0; i<twice_len; i+=2) {
			map.put(s_list[i], s_list[i+1]);	
		}
		return map;
    }
    
    /** Returns a string listing the scores with
     * annotations. Specially marks scores for the classes to which
     * this data point is known to be assigned according to its
     * classes array */
    public HashMap<String,String> getDiscriminationProbabilitiesMap(double prob[][], Suite suite, DataPoint x) {
   	 	boolean y[] = x.getY(suite);
   	 	boolean ysec[][] = suite.splitVectorByDiscrimination(y);

   	 	HashMap<String,String> map = new HashMap<String,String>();

   	 	int ysec_len = ysec.length;

   	 	/* Iterate over the discriminations */
   	 	for(int did = 0; did < ysec_len; did ++) {
   	 		Discrimination dis = suite.getDisc(did);
   	 		int prob_len = prob[did].length;
   	 		String values = "";
   	 		/* Iterate over the classes */
   	 		for(int i=0; i<prob_len; i++) {		 
   	 			values += dis.getClaById(i).getName() + " " + String.format("%4.3f",prob[did][i]) + " ";
   	 		}
   	 		/* Gotta get rid of last space, just in case */
   	 		if (values.length() > 0)
   	 			map.put(dis.getName(),values.substring(0,values.length()-1));
   	 	}
   	 	
   	 	return map;
    }
    
}
