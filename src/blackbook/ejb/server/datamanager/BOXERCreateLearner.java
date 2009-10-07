package blackbook.ejb.server.datamanager;

import java.io.ByteArrayInputStream;
import java.io.IOException;

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

import edu.dimacs.mms.boxer.ParseXML;
import edu.dimacs.mms.boxer.XMLUtil;
import edu.dimacs.mms.tokenizer.RDFNames;

/**
 * Workflow algorithm, similar to {@link BOXERCreateSuite} and {@link BOXERCreateComplex} that
 * takes in flat XML and turns it into a dumb model containing a Learner.
 * @author praff
 *
 */
public class BOXERCreateLearner extends AbstractAlgorithmKeyword2Model {
	/**
	 * 
	 */
	private static final long serialVersionUID = 6012403820334558207L;
	
	private static final String DESCRIPTION = "Inputs the raw XML text (no newlines!) and stores this as a learner that can be read in blackbook";

    private static final String LABEL = "BOXER Create Learner";

    /* For testing purposes only */
	public static void main (String[] args) throws ParserConfigurationException, SAXException, IOException, TransformerException, BlackbookSystemException {
		String s_suite = BOXERTools.readFileToString(BOXERTerms.TEST_DIR + "EG-learner.xml");
		BOXERCreateLearner test = new BOXERCreateLearner();
		Model m = test.executeAlgorithm(null,null,s_suite);
		
		BOXERTools.saveModelAsFile(m, BOXERTerms.TEST_DIR + "monterey-learner.rdf");
		
		System.out.print(m.toString());
	}

	/**
	 * Takes in flat XML in the keyword field and converts it to a DOM Document
	 * that is then saved in a dumb model.
	 * @param	user		The user
	 * @param	dataSource	The datasource (ignored)
	 * @param	keyword		The flat XML
	 * @return	A dumb model containing a DOM Document that is represented by the input XML.
	 */
	public Model executeAlgorithm(User user, String dataSource, String keyword)
			throws BlackbookSystemException {
		/* Convert string to buffer for the purposes of parsing */
		ByteArrayInputStream buf = new ByteArrayInputStream(keyword.getBytes());
		/* Set up the document and parse the string */
		DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
		Document doc = null;
		try {
			DocumentBuilder doc_builder = docBuilderFactory.newDocumentBuilder();
			doc = doc_builder.parse(buf);
		} catch (SAXException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ParserConfigurationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		/* Get the root element and its name */
		Element root = doc.getDocumentElement();
		/* May be an empty string! */
		String root_name = XMLUtil.LEARNER + root.getAttribute(ParseXML.ATTR.NAME_ATTR);
				
		Model m_model = ModelFactory.createDefaultModel();
		m_model.add(m_model.createStatement(m_model.createResource(RDFNames.URI_PREFIX + root_name),
											m_model.createProperty(RDFNames.PROP_PREFIX + RDFNames.PROP_IS),
											m_model.createLiteral(keyword)));
		return m_model;
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