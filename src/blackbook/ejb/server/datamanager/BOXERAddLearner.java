/**
 * 
 */
package blackbook.ejb.server.datamanager;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.StringReader;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import security.ejb.client.User;
import blackbook.ejb.client.exception.BlackbookSystemException;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.shared.JenaException;

import edu.dimacs.mms.boxer.BoxerXMLException;
import edu.dimacs.mms.boxer.Learner;
import edu.dimacs.mms.boxer.Suite;
import edu.dimacs.mms.boxer.XMLUtil;
import edu.dimacs.mms.tokenizer.RDFtoXML2;
import edu.dimacs.mms.tokenizer.XMLtoRDF2;

/* This mimics the functionality of the read-learner command,
 * which can be done to suites OR complexes - we look
 * at the root element of the document to check whether it is 
 * a suite or a complex - if it is something else, then we will
 * throw an exception!
 */
public class BOXERAddLearner extends AbstractAlgorithmMultiModel2Model {

	/* For testing purposes ONLY */
	public static void main (String[] args) throws IOException, SAXException, ParserConfigurationException, TransformerException, BlackbookSystemException {
		String filename = "C:\\Users\\praff\\Desktop\\2009-2010\\BOXER\\examples\\learner1.xml";
		String EG_filename = "C:\\Users\\praff\\Desktop\\2009-2010\\BOXER\\examples\\EG-learner.xml";
		
		Document d_complex = BOXERTools.readFileToDocument(filename);
		Document d_EG = BOXERTools.readFileToDocument(EG_filename);
		
		Model m_complex = BOXERTools.convertToRDF(d_complex);
		Model m_EG = BOXERTools.convertToRDF(d_EG);

		BOXERAddLearner test = new BOXERAddLearner();
		Model m = test.executeAlgorithm(null,m_complex,m_EG);
		System.out.print(m.toString());
	}

	/**
	 * 
	 */
	private static final long serialVersionUID = -834527032213447027L;

	/* m_complex is the old suite/complex
	 * m_learner is the new learner to be added
	 * @see blackbook.ejb.client.datamanager.AlgorithmMultiModel2Model#executeAlgorithm(security.ejb.client.User, com.hp.hpl.jena.rdf.model.Model, com.hp.hpl.jena.rdf.model.Model)
	 */
	
	public Model executeAlgorithm(User user, Model m_complex, Model m_learner)
			throws BlackbookSystemException {

		// First, extract the document from the first model.
		Document d_complex = null;
		Document d_learner = null;
		
		Suite suite = null;
		Model final_result = null;
		
		try {
			d_complex = RDFtoXML2.convertToXML(m_complex);
			d_learner = RDFtoXML2.convertToXML(m_learner);
			
			String root_tagname = getRootTagname(d_complex);
			
			/* In this case, we have a suite */
			if (root_tagname.equals(XMLUtil.SUITE)) {
				suite = new Suite(d_complex.getDocumentElement());
				suite.addLearner(d_learner.getDocumentElement());
			}
			else if (root_tagname.equals(XMLUtil.LEARNER_COMPLEX)) {
				suite =  Learner.deserializeLearnerComplex(d_complex.getDocumentElement());
				/* TODO: Is this check required if we are about to add a learner?
			     * Vector <Learner> algos = suite.getAllLearners();
			     * if (algos.size()==0) { 
			     * throw new Exception();
			     * UPDATE 09/21 - Vladimir says no.
			     * Seems legit - who cares at this point if we don't
			     * have a learner or not?
			     */
				suite.addLearner(d_learner.getDocumentElement());
			}
			/* Neither a suite nor a learner, so bad! */
			else {
				throw new Exception();
			}
			final_result = XMLtoRDF2.convertToRDF(suite.serializeLearnerComplex());
		} catch (JenaException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (BoxerXMLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SAXException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (TransformerException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 
		
		return final_result;
	}
	
	private static String getRootTagname(Document doc) {
		Element e = doc.getDocumentElement();
		return e.getTagName();
	}
	
	
	
}