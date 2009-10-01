package blackbook.ejb.server.datamanager;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import security.ejb.client.User;
import blackbook.ejb.client.exception.BlackbookSystemException;

import com.hp.hpl.jena.rdf.model.Model;

import edu.dimacs.mms.tokenizer.RDFtoXML2;
import edu.dimacs.mms.tokenizer.XMLtoRDF2;


/**
 * In this simple algorithm, we assume that both models are dumb holders of
 * XML documents. We then combine the two into one XML document, both as children
 * of one main node, which we give a fake name for. 
 * @author praff
 *
 */
public class BOXERCombineModels extends AbstractAlgorithmMultiModel2Model {

	/**
	 * 
	 */
	private static final long serialVersionUID = 493193411245558980L;

	public Model executeAlgorithm(User user, Model model1, Model model2)
			throws BlackbookSystemException {
		Document doc1 = RDFtoXML2.convertToXML(model1);
		Document doc2 = RDFtoXML2.convertToXML(model2);
		
		DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder docBuilder = null;
		Document d_combined = null;
		Model m = null;
		
		try {
		    docBuilder = docBuilderFactory.newDocumentBuilder();
		    d_combined = docBuilder.newDocument();
		    Element root = d_combined.createElement(BOXERTerms.COMBINED);
		    
		    Element e1 = (Element) d_combined.importNode(doc1.getDocumentElement(),true);
		    Element e2 = (Element) d_combined.importNode(doc2.getDocumentElement(), true);
		    
			root.appendChild(e1);
			root.appendChild(e2);
			
			d_combined.appendChild(root);
			
			m = XMLtoRDF2.convertToRDF(d_combined);
		}
		catch (ParserConfigurationException e) {
		    e.printStackTrace();
		} catch (TransformerException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		};
		
		return m;

	}

}
