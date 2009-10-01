package blackbook.ejb.server.datamanager;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.CharBuffer;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import security.ejb.client.User;
import blackbook.ejb.client.exception.BlackbookSystemException;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;

import edu.dimacs.mms.boxer.BoxerXMLException;
import edu.dimacs.mms.boxer.ParseXML;
import edu.dimacs.mms.boxer.Suite;
import edu.dimacs.mms.boxer.XMLUtil;
import edu.dimacs.mms.tokenizer.RDFNames;
import edu.dimacs.mms.tokenizer.XMLtoRDF2;

public class BOXERCreateComplexFromLearnerAndSuite extends AbstractAlgorithmMultiModel2Model {
	/**
	 * 
	 */
	private static final long serialVersionUID = 6012403820334558207L;
	
	private static final String DESCRIPTION = "Takes a learner and a suite and turns it into a learner complex";

    private static final String LABEL = "BOXER Create Complex from Learner and Suite";

	public static void main (String[] args) throws ParserConfigurationException, SAXException, IOException, TransformerException, BlackbookSystemException {
		Model m_learner = BOXERTools.readFileToModel(BOXERTerms.TEST_DIR + "monterey-learner.rdf");
		Model m_suite = BOXERTools.readFileToModel(BOXERTerms.TEST_DIR + "monterey-suite.rdf");
		
		BOXERCreateComplexFromLearnerAndSuite test = new BOXERCreateComplexFromLearnerAndSuite();
		
		Model m_complex = test.executeAlgorithm(null,m_learner,m_suite);
		
		Document d_complex = BOXERTools.convertToXML(m_complex);
		System.out.print(BOXERTools.convertToString(d_complex));
		
		BOXERTools.saveModelAsFile(m_complex, BOXERTerms.TEST_DIR + "monterey-complex.rdf");
	}

	public Model executeAlgorithm(User user, Model m_learner, Model m_suite)
			throws BlackbookSystemException {
		Document d_learner = BOXERTools.convertToXML(m_learner);
		Document d_suite = BOXERTools.convertToXML(m_suite);
		Document d_complex = null;
		Model m = null;
		
		try {
			/* Read the suite */
			Suite suite = new Suite(d_suite.getDocumentElement());
			
			/* Read the learner */
			suite.addLearner(d_learner.getDocumentElement());
			
			/* Write everything */
			d_complex = suite.serializeLearnerComplex();
			
			/* Convert to dumb model */
			m = BOXERTools.convertToRDF(d_complex);
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
		
		return m;
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