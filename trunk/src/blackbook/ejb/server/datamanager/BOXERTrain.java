package blackbook.ejb.server.datamanager;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Vector;

import javax.xml.transform.TransformerException;

import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import security.ejb.client.User;
import blackbook.ejb.client.exception.BlackbookSystemException;
import blackbook.ejb.server.metadata.MetadataManager;

import com.hp.hpl.jena.rdf.model.Model;

import edu.dimacs.mms.borj.LabelStore;
import edu.dimacs.mms.boxer.BoxerXMLException;
import edu.dimacs.mms.boxer.DataPoint;
import edu.dimacs.mms.boxer.Learner;
import edu.dimacs.mms.boxer.ParseXML;
import edu.dimacs.mms.boxer.Suite;

public class BOXERTrain extends AbstractAlgorithmMultiModel2Model {
	
	private static final String DESCRIPTION = "Given a learner complex and documents, it trains and updates the learner complex.";

    private static final String LABEL = "BOXER Train";
    

	/**
	 * 
	 */
	private static final long serialVersionUID = -6421206163445155471L;

	/* For testing purposes ONLY */
	public static void main (String[] args) throws BlackbookSystemException, TransformerException, IOException {
		Model m_complex = BOXERTools.readFileToModel(BOXERTerms.TEST_DIR + "monterey-complex-after-train.rdf");
		Model m_trainset = BOXERTools.readFileToModel(BOXERTerms.TEST_DIR + "monterey-trainset.rdf");
		
		BOXERTrain test = new BOXERTrain();
		
		Model m = test.executeAlgorithm(new User(),m_complex,m_trainset);
		Document d = BOXERTools.convertToXML(m);
		
		BOXERTools.saveModelAsFile(m, BOXERTerms.TEST_DIR + "monterey-complex-after-train.rdf");
		BOXERTools.saveDocumentAsFile(d, BOXERTerms.TEST_DIR + "monterey-complex-after-train.xml");
	}

	/* We follow borj.Driver() - we first read in the learner complex,
	 * then we train on the documents. 
	 * The two are stored in a dumb manner, and can be retrieved easily. 
	 * (non-Javadoc)
	 * @see blackbook.ejb.client.datamanager.AlgorithmMultiModel2Model#executeAlgorithm(security.ejb.client.User, com.hp.hpl.jena.rdf.model.Model, com.hp.hpl.jena.rdf.model.Model)
	 */
	public Model executeAlgorithm(User user, Model m_learnercomplex, Model m_trainingset)
			throws BlackbookSystemException {
		
		Document d_learnercomplex = BOXERTools.convertToXML(m_learnercomplex);
		Document d_trainingset = BOXERTools.convertToXML(m_trainingset);
		
		/* We read in the learner complex */
		try {
			/* Read in the learner complex */
			Suite suite =  Learner.deserializeLearnerComplex(d_learnercomplex.getDocumentElement());
			
			/* Now we train */
			Vector<DataPoint> train = ParseXML.parseDatasetElement(d_trainingset.getDocumentElement(), suite, true);
			
			LabelStore qrelStore = new LabelStore();
			qrelStore.applyTo(train, suite, true);
			
			// Do this in case new discriminations (with default classes)
			// have been added during reading the XML file. (since ver 0.6)
			//for(DataPoint p: train) p.addDefaultClasses(suite);

			if (Suite.verbosity>0) 
				System.out.println(suite.describe());
//			if (verbose) 
//				System.out.println(suite.getDic().describe());
//			if (Suite.verbosity>0) 
//				System.out.println("Training set no. " +trainCnt+ " ("+q.f+") contains " + train.size() + " points, memory use=" + Sizeof.sizeof(train) + " bytes");
//			for(int i =0; i<train.size(); i++) {
//			    if (verbose) System.out.println(train.elementAt(i));
//			}

			// train
			memory("Read train set; starting to train");
			int cnt=0;
			for(Learner algo : suite.getAllLearners()) {
			    algo.absorbExample(train);

			    if (Suite.verbosity>=0) {
				System.out.println("Describing Learner No. "+(cnt++));
				algo.describe(System.out, false);
				System.out.println("-----------------------------------");
			    } else {
				System.out.println("[NET] Leaner no. " + (cnt++)+ " net memory use=" + algo.memoryEstimate());
			    }
			    // In verbose mode, write out the model after every training file
//			    if (verbose) algo.saveAsXML(algo.algoName() + "-out" + trainCnt + ".xml");
			}
			int ts = train.size();
			train = null;		
			memory("Absorbed "+ts+" examples from " + BOXERTools.getDocumentElementName(d_trainingset));
			
			/* Now we want to save the new learner complex */
			Document d_new_learnercomplex = suite.serializeLearnerComplex();
			
			/* Now we have the entire model in Document form. Perfect for XMLtoRDF! */
			MetadataManager meta = new MetadataManager();
			String DSname = "BOXER " + BOXERTools.getDocumentElementName(d_new_learnercomplex) + getTimestamp();
			meta.createNewAssertionsDS(DSname);
			
			/* Don't just create it, PERSIST it! */
			JenaAndLuceneReplaceOrAdd j = new JenaAndLuceneReplaceOrAdd();
			Model m = BOXERTools.convertToRDF(d_new_learnercomplex);
			j.executeAlgorithm(user,DSname,m);
			
			return m;		
			
		} catch (BoxerXMLException e) {
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
    
    
    public static String getTimestamp() {
		Calendar cal = Calendar.getInstance();
	    SimpleDateFormat sdf = new SimpleDateFormat("yyyy MM dd HH mm ss");
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
