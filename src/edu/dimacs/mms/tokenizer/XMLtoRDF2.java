package edu.dimacs.mms.tokenizer;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;

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
import org.xml.sax.SAXException;

import com.hp.hpl.jena.rdf.model.Literal;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;

/** XMLtoRDF
 * 
 *  This package will attempt to convert ANY arbitrary XML to RDF standards. 
 *  This is only a one-way street in the sense that RDF (conceptually) is a 
 *  proper superset of XML and hence something like
 *  
 *  RDFtoXML( XMLtoRDF( blah ) ) = blah, but it probably won't ever be such that
 *  XMLtoRDF( RDFtoXML( blah ) ) = blah
 *  
 *  given any arbitrary RDF starting point blah. The primary reason is that
 *  a Jena model can encapsulate ANY graph, whereas XML can only encapsulate 
 *  trees.
 *  
 *  Nevertheless, since most projects nowadays deal primarily with XML, this
 *  would help. 
 * @author raff
 *
 */

public class XMLtoRDF2 {
	
	private static final String URI = "urn:boxer:learnercomplex";
	private static final String PROPERTY = "http://www.blackbook.com/boxer/terms#";
	
	/**
	 * @param args
	 * @throws IOException 
	 * @throws SAXException 
	 * @throws ParserConfigurationException 
	 * @throws TransformerException 
	 */
	public static void main(String[] args) throws SAXException, ParserConfigurationException, TransformerException {

		DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
		Document doc = null;
        try {
			DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();
			doc = docBuilder.parse (new File("C:\\complex.xml"));
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
		
		System.out.println(convertToString(doc));
		Model m = convertToRDF(doc);
		m.write(System.out);
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
	
	public static Model convertToRDF(Document doc) throws TransformerException {
		Model model = ModelFactory.createDefaultModel();
		
		String string = convertToString(doc);
        
        Resource r = model.createResource(URI);
        Property p = model.getProperty(PROPERTY,"is");
        Literal l = model.createLiteral(string);
       
        model.add(model.createStatement(r,p,l));
        
        return model;
	}
	
}