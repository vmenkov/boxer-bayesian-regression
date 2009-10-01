package blackbook.ejb.server.datamanager;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringReader;
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
import org.w3c.dom.Element;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import com.hp.hpl.jena.rdf.model.Literal;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.shared.JenaException;

import edu.dimacs.mms.boxer.ParseXML;
import edu.dimacs.mms.tokenizer.RDFNames;

/** This class simply contains a lot of useful tools in a static context.
 * 
 * @author praff
 *
 */
public class BOXERTools {
	
	/* For testing purposes ONLY */
	public static void main (String[] args) {

	}

	/** Gives a string representation of the input XML Document.
	 * 
	 * @param doc
	 * @return
	 * @throws TransformerException
	 */
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
	
	/**
	 * Just like {@link convertToString} but it returns it as a flat string with no newlines.
	 * @param doc
	 * @return
	 * @throws TransformerException
	 */
	
	public static String convertToFlatString(Document doc) throws TransformerException {
		TransformerFactory transfac = TransformerFactory.newInstance();
	
	/*
			For some reason blackbook does not like this method call!
			transfac.setAttribute("indent-number", new Integer(4));
			*/
			
		Transformer trans = transfac.newTransformer();
		trans.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
		trans.setOutputProperty(OutputKeys.INDENT, "no");
	        
		StringWriter sw = new StringWriter();
		StreamResult result = new StreamResult(sw);
		DOMSource source = new DOMSource(doc);
	    trans.transform(source, result);
	    return sw.toString().replaceAll("\\r\\n", "").replaceAll("\\n","");
	}
	
    /**
     * This method is only to be used after a Document was converted
     * via XMLToRDF2.convertToRDF. This method assumes there is only 
     * one statement in the model, and the text content of the object
     * of this one statement will be returned. Exceptions will be thrown
     * if there are multiple statements in the model or the object
     * of the one statement is not a Literal.
     * @throws Exception 
     */
	public static Document convertToXML(Model m) throws JenaException {

		/* If we did this appropriately, then there should only be
		 * one statement in the model. So we don't really care 
		 * what the resource and the property is.
		 */
		String doc_string = null;

		/* We simply take the first statement */
		/* TODO: Throw an exception if there are multiple statements? */
		StmtIterator iter = m.listStatements();
		
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
			if (iter.hasNext()) {
				throw new JenaException("Model has too many statements!");
			}
			return doc;
		}
		else {
			return null;
		}
		
		
	}
	
	public static Document readFileToDocument(String filename) throws IOException, ParserConfigurationException, SAXException {
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
		
		DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
		Document d_complex = null;

		DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();
		StringReader sr = new StringReader(complex);
		d_complex = docBuilder.parse(new InputSource(sr));	
		
		return d_complex;
	}
	
	/**
	 * This method takes in a Document and creates the following one-statement
	 * model:
	 * If ROOT is the tag of the root element in the Document and ROOTNAME 
	 * is the value of the "name" attribute (dictated by ParseXML.ATTR.NAME_ATTR),
	 * then the one statement is
	 * (RDFNames.URI_PREFIX + ROOT + ROOTNAME) : (RDFNames.PROP_PREFIX + RDFNames.PROP_IS) : (Doc as String)
	 * where the object of this statement is a string representation of the Document.
	 * It is meant as a convenience to store XML in a RDF model, and does not
	 * produce much overhead. RDF takes care of encoding special characters
	 * such as < and >.
	 * @param doc The DOM Document that is to be stored.
	 * @return A RDF model containing one statement.
	 * @throws TransformerException
	 */
	public static Model convertToRDF(Document doc) throws TransformerException {
		Model model = ModelFactory.createDefaultModel();
		
		/* We should first get the head tag and the name, if there */
		Element root = doc.getDocumentElement();
		String root_tagname = root.getTagName();
		String root_name = root.getAttribute(ParseXML.ATTR.NAME_ATTR);
		
		String string = convertToString(doc);
        
        Resource r = model.createResource(RDFNames.URI_PREFIX + root_tagname + root_name);
        Property p = model.getProperty(RDFNames.PROP_PREFIX,RDFNames.PROP_IS);
        Literal l = model.createLiteral(string);
       
        model.add(model.createStatement(r,p,l));
        
        return model;
	}
	
	/** This method takes in string and holds that string as a literal in a one-statement model.
	 * Usually we don't care what the URI is of the resource or the property . . . 
	 * @param keyword
	 * @param root_name
	 * @return
	 */
	public static Model createDumbModel(String keyword, String root_name) {
		Model m_model = ModelFactory.createDefaultModel();
		m_model.add(m_model.createStatement(m_model.createResource(RDFNames.URI_PREFIX + root_name),
											m_model.createProperty(RDFNames.PROP_PREFIX + RDFNames.PROP_IS),
											m_model.createLiteral(keyword)));
		return m_model;
	}
	
	public static Model createDumbModel(Document d) throws TransformerException {
		return createDumbModel(convertToString(d),getDocumentElementName(d));
	}
	
	/** Retrieves the value of the name attribute of the root element, if it exists. 
	 * Otherwise, it returns the tagname of the root element.
	 * 
	 * @param doc
	 * @return
	 */
    public static String getDocumentElementName(Document doc) {
    	String name = doc.getDocumentElement().getAttribute(ParseXML.ATTR.NAME_ATTR);
    	if (name.length() == 0) {
    		return doc.getDocumentElement().getTagName();
    	}
    	else {
    		return name;
    	}
    }
    
    public static void saveModelAsFile(Model m, String filename) {
    	try {
			FileWriter fw = new FileWriter(new File(filename));
			m.write(fw);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }
    
	public static void saveDocumentAsFile(Document doc, String filename) throws TransformerException, IOException {
		TransformerFactory transfac = TransformerFactory.newInstance();
						
		/*
		For some reason blackbook does not like this method call!
		transfac.setAttribute("indent-number", new Integer(4));
		*/
									
		Transformer trans = transfac.newTransformer();
		trans.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
		trans.setOutputProperty(OutputKeys.INDENT, "yes");
		
		FileWriter fw = new FileWriter(new File(filename));

		StreamResult result = new StreamResult(fw);
		DOMSource source = new DOMSource(doc);
		trans.transform(source, result);
	}
    
    public static Model readFileToModel(String filename) {
    	Model m = ModelFactory.createDefaultModel();
    	try {
			m.read(new FileInputStream(new File(filename)),null);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return m;
    }
    
    public static String readFileToString(String filename) throws IOException {
    	BufferedReader reader = new BufferedReader(new FileReader(new File(filename)));
		
		StringBuffer sb = new StringBuffer();
		
		String line = "";
	
		while (line != null) {
			if (line.length() > 0)
				sb.append(line + "\n");
			line = reader.readLine();
		}
		
		return sb.toString();
    }
	
}
