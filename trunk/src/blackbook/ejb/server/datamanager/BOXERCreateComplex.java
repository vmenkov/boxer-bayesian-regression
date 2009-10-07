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

/**
 * Workflow algorithm for turning flat XML text into a dumb model containing
 * a BOXER Learner Complex for future interaction.
 * @author praff
 *
 */
public class BOXERCreateComplex extends AbstractAlgorithmKeyword2Model {
	/**
	 * 
	 */
	private static final long serialVersionUID = 6012403820334558207L;
	
	private static final String DESCRIPTION = "Inputs the raw XML text (no newlines!) and stores this as a complex that can be read in blackbook";

    private static final String LABEL = "BOXER Create Complex";

    /* For testing purposes ONLY */
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
		
		String complex2 = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?><learnercomplex version=\"0.7.002\"><suite HowToHandleMissingIDs=\"Error\" SupportsSimpleLabels=\"No\" name=\"monterey\" nctest=\"IGNORE\" nctrain=\"IGNORE\" version=\"0.7.002\"><discrimination fallback=\"true\" name=\"sysdefaults\"><classes/></discrimination><discrimination defaultclass=\"0\" name=\"AGRO\"><classes>0 1</classes></discrimination><discrimination defaultclass=\"0\" name=\"BUS\"><classes>0 1</classes></discrimination><discrimination defaultclass=\"0\" name=\"ED\"><classes>0 1</classes></discrimination><discrimination defaultclass=\"0\" name=\"GOVT\"><classes>0 1</classes></discrimination><discrimination defaultclass=\"0\" name=\"IND\"><classes>0 1</classes></discrimination><discrimination defaultclass=\"0\" name=\"INDISCRIM\"><classes>0 1</classes></discrimination><discrimination defaultclass=\"0\" name=\"MED\"><classes>0 1</classes></discrimination><discrimination defaultclass=\"0\" name=\"NA\"><classes>0 1</classes></discrimination><discrimination defaultclass=\"0\" name=\"ORG\"><classes>0 1</classes></discrimination><discrimination defaultclass=\"0\" name=\"UNK\"><classes>0 1</classes></discrimination></suite><features>@dummy</features><learners><learner name=\"edu.dimacs.mms.boxer.ExponentiatedGradient\" version=\"0.7.002\"><parameters><parameter name=\"theta\" value=\"0.0\"/><parameter name=\"to\" value=\"0.0\"/><parameter name=\"k\" value=\"1\"/><parameter name=\"f\" value=\"1.0\"/><parameter name=\"u\" value=\"10.0\"/></parameters><classifier discrimination=\"sysdefaults\"><parameters><parameter name=\"maxInfNorm\" value=\"0.0\"/><parameter name=\"f\" value=\"1.0\"/><parameter name=\"u\" value=\"10.0\"/><parameter name=\"t\" value=\"0.0\"/></parameters><matrix name=\"classSizes\"/><matrix name=\"Vplus\"/><matrix name=\"Vminus\"/><matrix name=\"W\"/></classifier><classifier discrimination=\"AGRO\"><parameters><parameter name=\"maxInfNorm\" value=\"0.0\"/><parameter name=\"f\" value=\"1.0\"/><parameter name=\"u\" value=\"10.0\"/><parameter name=\"t\" value=\"0.0\"/></parameters><matrix name=\"classSizes\"/><matrix name=\"Vplus\"/><matrix name=\"Vminus\"/><matrix name=\"W\"/></classifier><classifier discrimination=\"BUS\"><parameters><parameter name=\"maxInfNorm\" value=\"0.0\"/><parameter name=\"f\" value=\"1.0\"/><parameter name=\"u\" value=\"10.0\"/><parameter name=\"t\" value=\"0.0\"/></parameters><matrix name=\"classSizes\"/><matrix name=\"Vplus\"/><matrix name=\"Vminus\"/><matrix name=\"W\"/></classifier><classifier discrimination=\"ED\"><parameters><parameter name=\"maxInfNorm\" value=\"0.0\"/><parameter name=\"f\" value=\"1.0\"/><parameter name=\"u\" value=\"10.0\"/><parameter name=\"t\" value=\"0.0\"/></parameters><matrix name=\"classSizes\"/><matrix name=\"Vplus\"/><matrix name=\"Vminus\"/><matrix name=\"W\"/></classifier><classifier discrimination=\"GOVT\"><parameters><parameter name=\"maxInfNorm\" value=\"0.0\"/><parameter name=\"f\" value=\"1.0\"/><parameter name=\"u\" value=\"10.0\"/><parameter name=\"t\" value=\"0.0\"/></parameters><matrix name=\"classSizes\"/><matrix name=\"Vplus\"/><matrix name=\"Vminus\"/><matrix name=\"W\"/></classifier><classifier discrimination=\"IND\"><parameters><parameter name=\"maxInfNorm\" value=\"0.0\"/><parameter name=\"f\" value=\"1.0\"/><parameter name=\"u\" value=\"10.0\"/><parameter name=\"t\" value=\"0.0\"/></parameters><matrix name=\"classSizes\"/><matrix name=\"Vplus\"/><matrix name=\"Vminus\"/><matrix name=\"W\"/></classifier><classifier discrimination=\"INDISCRIM\"><parameters><parameter name=\"maxInfNorm\" value=\"0.0\"/><parameter name=\"f\" value=\"1.0\"/><parameter name=\"u\" value=\"10.0\"/><parameter name=\"t\" value=\"0.0\"/></parameters><matrix name=\"classSizes\"/><matrix name=\"Vplus\"/><matrix name=\"Vminus\"/><matrix name=\"W\"/></classifier><classifier discrimination=\"MED\"><parameters><parameter name=\"maxInfNorm\" value=\"0.0\"/><parameter name=\"f\" value=\"1.0\"/><parameter name=\"u\" value=\"10.0\"/><parameter name=\"t\" value=\"0.0\"/></parameters><matrix name=\"classSizes\"/><matrix name=\"Vplus\"/><matrix name=\"Vminus\"/><matrix name=\"W\"/></classifier><classifier discrimination=\"NA\"><parameters><parameter name=\"maxInfNorm\" value=\"0.0\"/><parameter name=\"f\" value=\"1.0\"/><parameter name=\"u\" value=\"10.0\"/><parameter name=\"t\" value=\"0.0\"/></parameters><matrix name=\"classSizes\"/><matrix name=\"Vplus\"/><matrix name=\"Vminus\"/><matrix name=\"W\"/></classifier><classifier discrimination=\"ORG\"><parameters><parameter name=\"maxInfNorm\" value=\"0.0\"/><parameter name=\"f\" value=\"1.0\"/><parameter name=\"u\" value=\"10.0\"/><parameter name=\"t\" value=\"0.0\"/></parameters><matrix name=\"classSizes\"/><matrix name=\"Vplus\"/><matrix name=\"Vminus\"/><matrix name=\"W\"/><classifier discrimination=\"UNK\"><parameters><parameter name=\"maxInfNorm\" value=\"0.0\"/><parameter name=\"f\" value=\"1.0\"/><parameter name=\"u\" value=\"10.0\"/><parameter name=\"t\" value=\"0.0\"/></parameters><matrix name=\"classSizes\"/><matrix name=\"Vplus\"/><matrix name=\"Vminus\"/><matrix name=\"W\"/></classifier></learner></learners></learnercomplex>";

		BOXERCreateComplex test = new BOXERCreateComplex();
		Model m = test.executeAlgorithm(null,null,complex2);
		System.out.print(m.toString());
	}

	/**
	 * This turns the input keyword into a dumb model containing the BORJ Learner Complex.
	 * The input should be flat XML syntax.
	 * @param	user		The user
	 * @param	dataSource	The datasource (ignored)
	 * @param	keyword		The flat XML document
	 * @return	The XML will be converted to a DOM Document, then placed in a dumb model.
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
			throw new BlackbookSystemException("SAXException -- " + e.getMessage());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			throw new BlackbookSystemException("IOException -- " + e.getMessage());
		} catch (ParserConfigurationException e) {
			// TODO Auto-generated catch block
			throw new BlackbookSystemException("ParserConfigurationException -- " + e.getMessage());
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