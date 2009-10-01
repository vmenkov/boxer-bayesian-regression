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
import edu.dimacs.mms.tokenizer.XMLtoRDF2;

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