/**
 * 
 */
package blackbook.ejb.server.datamanager;

import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
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

/**
 * This class mimics the functionality of the "add-learner" command in BORJ,
 * which adds a new learner to a pre-existing suite OR learner complex.
 * The output is a dumb model containing a learner complex. We know for sure
 * that we will have a learner complex since we will have at least one learner.
 * @author praff
 *
 */
public class BOXERAddLearner extends AbstractAlgorithmMultiModel2Model {
	
	private static final String DESCRIPTION = "Add a learner to a previously-existing learner complex.";

    private static final String LABEL = "BOXER Add Learner";
    

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
	
	/**
	 * Takes in the learner complex and the learner as a dumb model
	 * and returns the dumb model containing the learner complex with the
	 * new learner in it.
	 * @param	user		The user
	 * @param	m_complex 	The dumb model containing the learner complex
	 * @param	m_learner	The dumb model containing the new learner
	 * @return	The dumb model containing the learner complex with the new learner.
	 *  
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
				throw new BOXERBlackbookException("Input data is neither a BOXER suite or a BOXER learner");
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
		} catch (BOXERBlackbookException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 
		
		return final_result;
	}
	
	/**
	 * This convenience method retrieves the name of the root tag element
	 * in the Document.
	 * @param doc the DOM Document
	 * @return
	 */
	private static String getRootTagname(Document doc) {
		Element e = doc.getDocumentElement();
		return e.getTagName();
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

/*
Copyright 2009, Rutgers University, New Brunswick, NJ.

All Rights Reserved

Permission to use, copy, and modify this software and its documentation for any purpose 
other than its incorporation into a commercial product is hereby granted without fee, 
provided that the above copyright notice appears in all copies and that both that 
copyright notice and this permission notice appear in supporting documentation, and that 
the names of Rutgers University, DIMACS, and the authors not be used in advertising or 
publicity pertaining to distribution of the software without specific, written prior 
permission.

RUTGERS UNIVERSITY, DIMACS, AND THE AUTHORS DISCLAIM ALL WARRANTIES WITH REGARD TO 
THIS SOFTWARE, INCLUDING ALL IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR 
ANY PARTICULAR PURPOSE. IN NO EVENT SHALL RUTGERS UNIVERSITY, DIMACS, OR THE AUTHORS 
BE LIABLE FOR ANY SPECIAL, INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER 
RESULTING FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT, 
NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION WITH THE USE OR 
PERFORMANCE OF THIS SOFTWARE.
*/