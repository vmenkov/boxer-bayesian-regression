package edu.dimacs.mms.tokenizer;

import java.io.InputStream;
import java.io.StringWriter;
import java.util.Hashtable;

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

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.ResIterator;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.util.FileManager;

/**
 *  This class will convert RDF that has been created via XMLtoRDF back to the original XML format. 
 *  Ideally, if D is a document, then RDFtoXML ( XMLtoRDF ( D ) ) = D (in structure only, not in order). 
 *  We can't convert any RDF document willy-nilly as RDF is a proper superset of XML.
 *  
 *  One would need to dictate the ontology first before a successful decoding can happen.
 * @author praff
 *
 */

public class RDFtoXML {

	/* Our "ontology" */
	
	private String NAMESPACE = "http://www.blackbook.com/boxer/";
	private String ATTRIBUTE = "http://www.blackbook.com/boxer/attribute/";
	private String HAS = "has";
	private String TEXT = "text";
	private String DOCUMENT = "document";
	private String SEPARATOR = "-";
	
	private String RDF_TYPE = "http://www.w3.org/1999/02/22-rdf-syntax-ns#type";
	private String RDF_OBJECT = "http://www.w3.org/1999/02/22-rdf-syntax-ns#object";
	private String RDF_PREDICATE = "http://www.w3.org/1999/02/22-rdf-syntax-ns#predicate";
	private String RDF_SUBJECT = "http://www.w3.org/1999/02/22-rdf-syntax-ns#subject";
	
	private String URI = "urn:blackbook:";
	
	private Hashtable<String,Element> hash;
	
	private Document doc;
	
	public RDFtoXML() {
		hash = new Hashtable<String,Element>();
		DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder docBuilder;
		try {
			docBuilder = docBuilderFactory.newDocumentBuilder();
			doc = docBuilder.newDocument();
		} catch (ParserConfigurationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	
	public static void main(String[] args) throws TransformerException {
		RDFtoXML test = new RDFtoXML();

//		XMLtoRDF test2 = new XMLtoRDF("C:\\Documents and Settings\\praff\\workspace\\example.xml");
//		test2.BFS();
//        while (statements.hasNext()) {
//        	Statement s = (Statement) statements.next();
//        	System.out.println(s);
//        	/* IMPORTANT: Use RDFNodes whenever possible since you may not know whether you are dealing with
//        	 * a resource or not.
//        	 */
//        	RDFNode r = s.getSubject();
//        	Property p = s.getPredicate();
//        	RDFNode l = s.getObject();
//        	System.out.println("\t"+r.toString());
//        	System.out.println("\t"+p.toString());
//        	System.out.println("\t"+l.toString());
//        	System.out.print("Parsing . . . ");
//        	test.parseRDFStatement(s);
//        	System.out.println("parsed");
//        }
        
     // create an empty model
        Model model = ModelFactory.createDefaultModel();

        // use the FileManager to find the input file
        String inputFileName = "C:\\boxershar.rdf";
        InputStream in = FileManager.get().open(inputFileName);
        if (in == null) {
        	throw new IllegalArgumentException(
                                        "File: " + inputFileName + " not found");
        }

        // read the RDF/XML file
        model.read(in, null);
        //model.write(System.out);

//		StmtIterator statements = model.listStatements();
//		while (statements.hasNext()) {
//			System.out.println(statements.next().toString());
//		}
        
        test.parseModel(model);
        
		TransformerFactory transfac = TransformerFactory.newInstance();
		
		/* For some reason blackbook does not like this method call! */
		//transfac.setAttribute("indent-number", new Integer(4));
		
        Transformer trans = transfac.newTransformer();
        trans.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
        trans.setOutputProperty(OutputKeys.INDENT, "yes");
        
        StringWriter sw = new StringWriter();
        StreamResult result = new StreamResult(sw);
        DOMSource source = new DOMSource(test.doc);
        trans.transform(source, result);
        String string = sw.toString();
        
        System.out.print(string);
        
        ResIterator iter = model.listSubjects();
        while (iter.hasNext()) {
        	RDFNode r = (RDFNode) iter.next();
        	if (r.isURIResource()) {
        		System.out.println(r.toString());
        	}
        }
	}
	
	public String getXMLTagname(String RDF_resource_tagname) {
		if (RDF_resource_tagname.startsWith(NAMESPACE)) {
			String name = RDF_resource_tagname.substring(NAMESPACE.length());
			return name.substring(0,name.lastIndexOf(SEPARATOR));
		}
		else
			return null;
	}
	
	public String getXMLAttributeName(String RDF_property) {
		if (RDF_property.startsWith(ATTRIBUTE)) {
			return RDF_property.substring(ATTRIBUTE.length());
		}
		else
			return null;
	}
	
	public void parseRDFStatement(Statement s) {
		RDFNode r = s.getSubject();
		Property p = s.getPredicate();
		RDFNode l = s.getObject();
		
		/* These represent the XML Elements for the subject and object */
		Element xml_r = null;
		Element xml_l = null;
		
		boolean is_document_node = false;
		
		/* First, we parse the resource and see if that node is already in the hashtable */
		String resource_name = r.toString();
		String r_name = null;
		String l_name = null;
		if (resource_name.startsWith(NAMESPACE)) {
			r_name = resource_name.substring(NAMESPACE.length());
			/* Element already exists in hashtable */
			if (hash.containsKey(r_name))
				xml_r = hash.get(r_name);
			else {
				/* If this is the document node, we need to treat it as a special case */
				if (!r_name.equals(DOCUMENT))
					xml_r = doc.createElement(getXMLTagname(resource_name));
				else
					is_document_node = true;
			}
		}
		/* TODO : Throw exception? */
		else
			xml_r = null;
		
		/* We parse the property, doing different things depending on what it is */
		String property_name = p.toString();
		/* In this case, the literal will be an Element also */
		if (property_name.startsWith(NAMESPACE+HAS)) {
			String literal_name = l.toString();
			if (literal_name.startsWith(NAMESPACE)) {
				l_name = literal_name.substring(NAMESPACE.length());
				/* Element already exists in hashtable */
				if (hash.containsKey(l_name))
					xml_l = hash.get(l_name);
				else
					xml_l = doc.createElement(getXMLTagname(literal_name));
			}
			/* TODO : Throw exception? */
			else
				xml_l = null;
			
			/* blackbook automatically adds a lot of junk to RDF that we make.
			 * We need a better way to parse this out or deal with it.
			 */
			
			if (xml_r != null || is_document_node) {
			
				/* We know that we have a parent-child situation here */
				if (r_name.equals(DOCUMENT)) 
					doc.appendChild(xml_l);
				else
					xml_r.appendChild(xml_l);
			}
		}
		else if (property_name.startsWith(NAMESPACE+TEXT)) {
			String text_content = l.toString();
			
			/* We don't need to add to the hashtable since text nodes are the end of the line */
			xml_r.appendChild(doc.createTextNode(text_content));
		}
		/* Only thing left is an attribute */
		else if (xml_r != null) {
			String attribute_name = getXMLAttributeName(p.toString());
			xml_r.setAttribute(attribute_name,l.toString());
		}
		else {
			
		}
		
		/* At the end of the day, we need to put our nodes back in the hashtable */
		if (r_name != null && !r_name.equals(DOCUMENT)) 
			hash.put(r_name, xml_r);
		if (l_name != null) {
			hash.put(l_name,xml_l);
		}				
	}
	
	public void parseModel(Model m) {
		StmtIterator statements = m.listStatements();
		while (statements.hasNext()) {
			parseRDFStatement((Statement) statements.next());
		}
	}
	
	public Document getDocument() {
		return doc;
	}
}

