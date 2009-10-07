package blackbook.ejb.server.datamanager;

import javax.xml.transform.TransformerException;

import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import security.ejb.client.User;
import blackbook.ejb.client.exception.BlackbookSystemException;

import com.hp.hpl.jena.rdf.model.Model;

import edu.dimacs.mms.boxer.BoxerXMLException;
import edu.dimacs.mms.boxer.Suite;

/**
 * Workflow algorithm for mimicking the BORJ methods for combining a learner
 * and a suite into a learner complex. 
 * @author praff
 *
 */
public class BOXERCreateComplexFromLearnerAndSuite extends AbstractAlgorithmMultiModel2Model {
	/**
	 * 
	 */
	private static final long serialVersionUID = 6012403820334558207L;
	
	private static final String DESCRIPTION = "Takes a learner and a suite and turns it into a learner complex";

    private static final String LABEL = "BOXER Create Complex from Learner and Suite";

    /* For testing purposes only */
	public static void main (String[] args) throws Exception {
		Model m_learner = BOXERTools.readFileToModel(BOXERTerms.TEST_DIR + "monterey-learner.rdf");
		Model m_suite = BOXERTools.readFileToModel(BOXERTerms.TEST_DIR + "monterey-suite.rdf");
		
		BOXERCreateComplexFromLearnerAndSuite test = new BOXERCreateComplexFromLearnerAndSuite();
		
		Model m_complex = test.executeAlgorithm(null,m_learner,m_suite);
		
		Document d_complex = BOXERTools.convertToXML(m_complex);
		Document d_learner = BOXERTools.convertToXML(m_learner);
		Document d_suite = BOXERTools.convertToXML(m_suite);
		System.out.print(BOXERTools.convertToString(d_complex));
		
		BOXERTools.saveModelAsFile(m_complex, BOXERTerms.TEST_DIR + "monterey-complex.rdf");
		System.out.println(BOXERTools.convertToFlatString(d_complex));
		System.out.println(BOXERTools.convertToFlatString(d_learner));
		System.out.println(BOXERTools.convertToFlatString(d_suite));
	}

	/**
	 * Inputs a learner and a suite, both as dumb models, and combines them into 
	 * a learner complex.
	 * @param	user		The user
	 * @param	m_learner	Dumb model containing the learner
	 * @param	m_suite		Dumb model containing the suite
	 * @return	The BORJ learner complex, in a dumb model.
	 */
	public Model executeAlgorithm(User user, Model m_learner, Model m_suite)
			throws BlackbookSystemException {
		Document d_learner = null;
		Document d_suite = null;
		try {
			d_learner = BOXERTools.convertToXML(m_learner);
			d_suite = BOXERTools.convertToXML(m_suite);
		} catch (Exception e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
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