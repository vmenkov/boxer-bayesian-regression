package edu.dimacs.mms.tokenizer;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.Calendar;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import com.hp.hpl.jena.rdf.model.Literal;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.ResIterator;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;

/**
 *  This class will convert RDF that has been created via XMLtoRDF back to the original XML format. 
 *  Ideally, if D is a document, then RDFtoXML ( XMLtoRDF ( D ) ) = D (in structure only, not in order). 
 *  We can't convert any RDF document willy-nilly as RDF is a proper superset of XML.
 *  
 *  One would need to dictate the ontology first before a successful decoding can happen.
 * @author praff
 *
 */

public class RDFtoXML2 {
	
	private static final String URI = "urn:boxer:learnercomplex";
	private static final String PROPERTY = "http://www.blackbook.com/boxer/terms#";
	
	private static final String BOXER_DATA_IDENTIFIER_PROPERTY = "http://purl.org/dc/elements/1.1/identifier";
	private static final String BOXER_DATA_FEATURES_PROPERTY = "http://blackbook.com/terms/FEATURES";
	
	private static final String BOXER_VERSION = "0.6.007";
	
	public static void main (String[] args) throws IOException, TransformerException {
		
		FileInputStream in = new FileInputStream( new File( "C:\\boxerdata.rdf"));
		Model m = ModelFactory.createDefaultModel();
		m.read(in,null);
		in.close();

		Document doc = convertBOXERDataToRCVXML(m);
		System.out.print(convertToString(doc));
		
	}
	
	public static String convertToString(Document doc) throws TransformerException {
		TransformerFactory transfac = TransformerFactory.newInstance();
				
				/*
				For some reason blackbook does not like this method call!
				transfac.setAttribute("indent-number", new Integer(4));
				*/
				
		        Transformer trans = transfac.newTransformer();
		        trans.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
		        trans.setOutputProperty(OutputKeys.INDENT, "yes");
		        
		        StringWriter sw = new StringWriter();
		        StreamResult result = new StreamResult(sw);
		        DOMSource source = new DOMSource(doc);
		        trans.transform(source, result);
		        return sw.toString();
			}
	
	private static String getGeneric(Model m, Resource r, String s) {
		Property p = m.getProperty(s);
		StmtIterator iter = m.listStatements(r,p,(RDFNode) null);
		if (iter.hasNext()) {
			Statement stmt = (Statement) iter.next();
			Literal l = (Literal) stmt.getObject();
			return l.getString();
		}
		else {
			return "";
		}
	}
	
	public static String getFeatures(Model m, Resource r) {
		return getGeneric(m,r,BOXER_DATA_FEATURES_PROPERTY);
	}
	
	public static String getIdentifier(Model m, Resource r) {
		return getGeneric(m,r,BOXER_DATA_IDENTIFIER_PROPERTY);
		
	}
	
	public static Document convertBOXERDataToRCVXML(Model m) {
		DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder docBuilder = null;
		try {
			docBuilder = docBuilderFactory.newDocumentBuilder();
		} catch (ParserConfigurationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		};
		Document doc = docBuilder.newDocument();
		Element dataset = doc.createElement("dataset");
		dataset.setAttribute("name","BOXER"+getTimestamp());
		dataset.setAttribute("version",BOXER_VERSION);
		doc.appendChild(dataset);
		
		ResIterator iter = m.listSubjects();
		while (iter.hasNext()) {
			Resource r = iter.nextResource();
			if (r.isURIResource()) {
				Element datapoint = doc.createElement("datapoint");
				Element features = doc.createElement("features");
				features.setTextContent(getFeatures(m,r));
				datapoint.setAttribute("name",getIdentifier(m,r));
				datapoint.appendChild(features);
				dataset.appendChild(datapoint);
			}
		}
		
		return doc;
		
	}
	
    private static String getTimestamp() {
		Calendar cal = Calendar.getInstance();
	    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss");
	    return sdf.format(cal.getTime());
	    
    }
	
	/**
	 * We have a different method of storing the learner complex.
	 * It is in its own Assertions datasource and it is the literal
	 * of the only statement in the model. It's then a simple
	 * extraction. It's good to know that Jena does all the character
	 * encoding/decoding seamlessly.
	 * @param m
	 */
	
	public static Document convertLearnerComplexToXML(Model m) {

		String doc_string = null;
		
		Resource r = m.createResource(URI);
		Property p = m.getProperty(PROPERTY,"is");

		
		StmtIterator iter = m.listStatements(r,p,(RDFNode) null);
		
		if (iter.hasNext()) {
			doc_string = ((Statement) iter.next()).getLiteral().getString();
			StringReader sr = new StringReader(doc_string);
			DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
			Document doc = null;
	        try {
				DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();
				doc = docBuilder.parse(new InputSource(sr));			
			} catch (ParserConfigurationException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (SAXException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return doc;
		}
		else {
			return null;
		}
		
		
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