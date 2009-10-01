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

import edu.dimacs.mms.boxer.ParseXML;
import edu.dimacs.mms.boxer.XMLUtil;
import edu.dimacs.mms.tokenizer.RDFNames;
import edu.dimacs.mms.tokenizer.XMLtoRDF2;

public class BOXERCreateComplex extends AbstractAlgorithmKeyword2Model {
	/**
	 * 
	 */
	private static final long serialVersionUID = 6012403820334558207L;
	
	private static final String DESCRIPTION = "Inputs the raw XML text (no newlines!) and stores this as a complex that can be read in blackbook";

    private static final String LABEL = "BOXER Create Complex";

	public static void main (String[] args) throws ParserConfigurationException, SAXException, IOException, TransformerException, BlackbookSystemException {
		String filename = BOXERTerms.TEST_DIR + "monterey-complex-after-train.xml";
		File f = new File(filename);
		
		BufferedReader reader = new BufferedReader(new FileReader(f));
		
		StringBuffer sb = new StringBuffer();
		
		String line = "";
	
		while (line != null) {
			if (line.length() > 0)
				sb.append(line + "\n");
			line = reader.readLine();
		}
		
		String complex = sb.toString();

		BOXERCreateComplex test = new BOXERCreateComplex();
		Model m = test.executeAlgorithm(null,null,complex);
		System.out.print(m.toString());
	}

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
		String root_name = XMLUtil.LEARNER_COMPLEX + root.getAttribute(ParseXML.ATTR.NAME_ATTR);
				
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