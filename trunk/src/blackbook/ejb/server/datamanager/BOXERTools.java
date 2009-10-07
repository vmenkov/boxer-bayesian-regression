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
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Random;
import java.util.Set;

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

import blackbook.ejb.server.metadata.MetadataManager;

import com.hp.hpl.jena.rdf.model.Literal;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;

import edu.dimacs.mms.boxer.ParseXML;
import edu.dimacs.mms.tokenizer.RDFNames;

/** This class simply contains a lot of useful tools in a static context.
 * 
 * @author praff
 *
 */
public class BOXERTools {
	
	/* For testing purposes ONLY */
	public static void main (String[] args) throws Exception {
		Model m_complex = readFileToModel(BOXERTerms.TEST_DIR + "blackbook-complex-workflow1.rdf");
		StmtIterator statements = m_complex.listStatements();
		int count = 0;
		while (statements.hasNext()) {
			count++;
			Statement s = (Statement) statements.next();
			if (s.isReified())
				System.out.print("(REIFIED) ");
			System.out.println(s.toString());
		}
		System.out.println(count + " statements");
		Document d_complex = null;
		try {
			d_complex = convertToXML(m_complex);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		System.out.print(convertToFlatString(d_complex));
		
		MetadataManager m = new MetadataManager();
		Set<String> datasources = m.getQueryableDataSourceNames();
		
		for (String d : datasources) {
			System.out.println(d);
		}


	}

	/** Gives a string representation of the input XML Document.
	 * 
	 * @param 	doc	The DOM Document
	 * @return	A string representation of this Document in XML syntax. Newlines and tabs are used.
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
	 * @param 	doc	The DOM Document to convert
	 * @return	A string representation in a "flat" format - i.e. no newlines.
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
     * via {@link convertToRDF}. Since blackbook reifies statements in models
     * that are ingested/stored, this method works in a very specific manner:
     * <ol>
     * <li>First, it looks for a reified statement. If it finds one, it assumes
     * it's the one and returns the text content of the Literal of that statement.</li>
     * <li>If no reified statements exist, then it takes the first statement and again
     * returns the text content of the Literal of that statement.
     * </ol>
     * Note that this method will have unknown results if the model contains 
     * multiple reified statements.
     * @throws BOXERBlackbookException if the method finds a reified statement with a
     * non-literal object, or if the model contains no reified statements but more
     * than one statement.
     */
	public static Document convertToXML(Model m) throws BOXERBlackbookException {

		/* If we did this appropriately, then there should only be
		 * one statement in the model. So we don't really care 
		 * what the resource and the property is.
		 */
		String doc_string = null;

		/* We simply take the first statement */
		/* TODO: Throw an exception if there are multiple statements? */
		/* FIX 10/2/2009: blackbook modifies with reified statements.
		 * As a result, we should only look at the statements that are REIFIED */
		 
		StmtIterator iter = m.listStatements();
		
		while (iter.hasNext()) {
			Statement s = (Statement) iter.next();
			if (s.isReified()) {
				if (s.getObject().isLiteral()) {
					doc_string = s.getLiteral().getString();
				}
				else {
					throw new BOXERBlackbookException("Model contains a reified statement with a non-Literal Object");
				}
				StringReader sr = new StringReader(doc_string);
				DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
				Document doc = null;
				try {
					DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();
					doc = docBuilder.parse(new InputSource(sr));			
				} catch (ParserConfigurationException e) {
					// 	TODO Auto-generated catch block
					e.printStackTrace();
				} catch (SAXException e) {
					// 	TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			
				return doc;
			}
		}
		
		/* Now we do it over again, in case nothing is reified. 
		 * In this case, we expect only one statement!
		 */
		iter = m.listStatements();
		
		if (iter.hasNext()) {
			Statement s = (Statement) iter.next();
			if (s.getObject().isLiteral()) {
				doc_string = s.getLiteral().getString();
				StringReader sr = new StringReader(doc_string);
				DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
				Document doc = null;
				try {
					DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();
					doc = docBuilder.parse(new InputSource(sr));			
				} catch (ParserConfigurationException e) {
					// 	TODO Auto-generated catch block
					e.printStackTrace();
				} catch (SAXException e) {
					// 	TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			
				if (iter.hasNext()) {
					throw new BOXERBlackbookException("Model has too many (>1) statements!");
				}
				else {
					return doc;
				}
			}
			else {
				throw new BOXERBlackbookException("Model has statement without literal");
			}
		}
		else {
			throw new BOXERBlackbookException("No statements in model!");
		}
		
	}
	
	/**
	 * Takes in a file path which contains XML, and reads it into a DOM Document.
	 * @param 	filename	The file path
	 * @return	The DOM Document that represents the file.
	 * @throws 	IOException
	 * @throws 	ParserConfigurationException
	 * @throws 	SAXException
	 */
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
	 * @param 	keyword		The text information that will be kept in the Literal of the one-statement model.
	 * @param 	root_name	The desired third part of the URI of the resource of the returned model.
	 * @return	A one-statement model holding the desired information.
	 */
	public static Model createDumbModel(String keyword, String root_name) {
		Model m_model = ModelFactory.createDefaultModel();
		m_model.add(m_model.createStatement(m_model.createResource(RDFNames.URI_PREFIX + root_name),
											m_model.createProperty(RDFNames.PROP_PREFIX + RDFNames.PROP_IS),
											m_model.createLiteral(keyword)));
		return m_model;
	}
	
	/**
	 * A specialization that stores the whole Document as text and uses the root tagname.
	 * @param 	The DOM Document to be stored.
	 * @return	A one-statement model containing the Document in text form.
	 * @throws TransformerException
	 */
	public static Model createDumbModel(Document d) throws TransformerException {
		return createDumbModel(convertToString(d),getDocumentElementName(d));
	}
	
	/** Retrieves the value of the name attribute of the root element, if it exists. 
	 * Otherwise, it returns the tagname of the root element.
	 * 
	 * @param 	doc	The DOM Document
	 * @return	The name attribute of the root element, if it exists. If not, the empty string.
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
    
    /**
     * Convenience method to save the model in a file specified.
     * @param 	m			The Jena model to be saved.
     * @param 	filename	The file path to save the file.
     * @throws	IOExcecption if Java is unable to write to the file specified.
     */
    public static void saveModelAsFile(Model m, String filename) {
    	try {
			FileWriter fw = new FileWriter(new File(filename));
			m.write(fw);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }
    
    /**
     * Convenience method to save the Document in a file specified.
     * @param doc		The DOM Document that will be stored.
     * @param filename	The file path of where the DOM Document will be stored.
     * @throws TransformerException
     * @throws IOException
     */
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
    
	/**
	 * Convenience method to input a filename and attempt to read the model in.
	 * @param 	filename	The file path containing the RDF.
	 * @return	The Jena model consisting of statements in the file.
	 */
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
    
    /**
     * Convenience method to read in the text content of a file.
     * @param 	filename	The path of the file.
     * @return	The string content of the file.
     * @throws IOException
     */
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
    
    /**
     * Returns a random alpha-numeric string of the specified length. The string
     * will only contain characters in [0-9A-Za-z].
     * @param 	len	The length desired.
     * @return	A random string of length len.
     */
    public static String getRandomString(int len) {
    	Random ra = new Random();
    	String name = "";
    	for (int i=0; i<len; i++) {
    		int num = ra.nextInt(62);
    		/* Will be an integer */
    		if (0 <= num && num <= 9)
    			name += (char)(num+48);
    		/* Will be an uppercase letter */
    		else if (10 <= num && num <= 35)
    			name += (char)(num+55);
    		/* Will be a lowercase letter */
    		else 
    			name += (char)(num + 61);
    	}
    	return name;
    }
    
    
    /**
     * Returns the current timestamp of the form <i>yyyy MM dd HH mm ss</i>, where the terms
     * are years, months, day, hours, minutes, and seconds, respectively.
     * @return	The timestamp
     */
    public static String getTimestamp() {
		Calendar cal = Calendar.getInstance();
	    SimpleDateFormat sdf = new SimpleDateFormat("yyyy MM dd HH mm ss");
	    return sdf.format(cal.getTime());
	    
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