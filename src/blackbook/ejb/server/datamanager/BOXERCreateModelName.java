package blackbook.ejb.server.datamanager;

import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import org.xml.sax.SAXException;

import security.ejb.client.User;
import blackbook.ejb.client.exception.BlackbookSystemException;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;

import edu.dimacs.mms.tokenizer.RDFNames;

/**
 * Simple workflow algorithm that carries the name of a soon-to-be-created
 * assertions datasource. Meant to be fed into {@link BOXERPersistModel} with
 * another model (of the "real" data) that is what will be persisted.
 * @author praff
 *
 */
public class BOXERCreateModelName extends AbstractAlgorithmKeyword2Model {
	/**
	 * 
	 */
	private static final long serialVersionUID = -5736576163094498611L;
	
	private static final String DESCRIPTION = "This simple task has the user input a model name and stores that info in a model. Meant to be joined with the actual model to be persisted.";

    private static final String LABEL = "BOXER Create Model Name";

	/* For testing purposes only */
	public static void main (String[] args) throws ParserConfigurationException, SAXException, IOException, TransformerException {
		
	}

	/**
	 * Takes in the proposed model name in the keyword field and returns a dumb model
	 * holding that information.
	 * @param	user		The user
	 * @param	dataSource	The datasource (ignored)
	 * @param	keyword		The proposed model name
	 * @return	A dumb model containing the proposed model name.
	 */
	public Model executeAlgorithm(User user, String dataSource, String keyword)
			throws BlackbookSystemException {
		/* We create the model */
		Model m_model = ModelFactory.createDefaultModel();
		m_model.add(m_model.createStatement(m_model.createResource(RDFNames.URI_PREFIX + RDFNames.URI_MODELNAME),
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