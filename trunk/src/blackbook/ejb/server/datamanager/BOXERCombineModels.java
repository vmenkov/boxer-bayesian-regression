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
	
	private static final String DESCRIPTION = "Simple method to combine two XML documents into one, as dumb models.";

    private static final String LABEL = "BOXER Combine Models";
    

	/**
	 * 
	 */
	private static final long serialVersionUID = 493193411245558980L;

	/**
	 * This algorithm takes in two dumb models, and returns one dumb model
	 * containing one DOM Document that has, as children, the two input
	 * Documents.
	 * @param	user	The user
	 * @param	model1	The first dumb model
	 * @param	model2	The second dumb model
	 * @return	A dumb model containing a Document that is the combination of 
	 *			the two input Documents.
	 */
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
