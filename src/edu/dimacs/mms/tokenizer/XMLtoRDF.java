package edu.dimacs.mms.tokenizer;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
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
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.StmtIterator;

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

public class XMLtoRDF {
	
	/* Our "ontology" */
	
	private String NAMESPACE = "http://www.blackbook.com/boxer/";
	private String ATTRIBUTE = "http://www.blackbook.com/boxer/attribute/";
	private String HAS = "has";
	private String TEXT = "text";
	private String DOCUMENT = "document";
	
	private String URI = "urn:blackbook:";
	
	private static final String NODE_DOCUMENT = "#document";
	private static final String NODE_TEXT = "#text";
	
	private Model model;
	private Document doc;
	
	private ArrayList<String> tagnames;
	private ArrayList<Integer> tagname_identifiers;
	
	/* This is an important piece of the class. In XML, different Nodes
	 * may have the same tagname but they fundamentally represent different things.
	 * Hence we give them unique names in RDF. However, we need to make sure that
	 * we remember which Nodes have already been assigned RDF names.
	 */
	private Hashtable<Node,String> hash; 
	
	/**
	 *  Returns the associated document.
	 * @return
	 */
	public Document getDocument() {
		return doc;
	}
	
	public Model getModel() {
		return model;
	}
	
	

	/**
	 * @param args
	 * @throws IOException 
	 * @throws SAXException 
	 * @throws ParserConfigurationException 
	 * @throws TransformerException 
	 */
	public static void main(String[] args) throws SAXException, ParserConfigurationException, TransformerException {

		XMLtoRDF test = new XMLtoRDF("C:\\blackbook-2.7\\example.xml");
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
        DOMSource source = new DOMSource(test.getDocument());
        trans.transform(source, result);
        String string = sw.toString();
        
        System.out.print(string);
        
        test.BFS();
        int tagname_len = test.tagnames.size();
        int tagname_identifier_len = test.tagname_identifiers.size();
        
        for (int i=0; i<tagname_len; i++) {
        	System.out.println(test.tagnames.get(i)+"\t"+Integer.toString(test.tagname_identifiers.get(i)));
        }
        
        StmtIterator statements = test.model.listStatements();
        while (statements.hasNext()) {
        	System.out.println(statements.next());
        }
        
        test.model.write(System.out);
	}
	
	public XMLtoRDF (Document doc) {
		/* Instantiate ArrayLists */
		tagnames 			= new ArrayList<String>();
		tagname_identifiers = new ArrayList<Integer>();
		hash 				= new Hashtable<Node,String>();
		
		model = ModelFactory.createDefaultModel();
		this.doc = doc;
	}
	
	public XMLtoRDF (String filename) {
		/* Instantiate ArrayLists */
		tagnames 			= new ArrayList<String>();
		tagname_identifiers = new ArrayList<Integer>();
		hash 				= new Hashtable<Node,String>();
		
		model = ModelFactory.createDefaultModel();
		DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
        try {
			DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();
			doc = docBuilder.parse (new File(filename));
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
	}
	
	/** 
	 *  Checks to see if the input String is already used as a tagname.
	 * @param str Input String.
	 * @return True if and only if str is in the tagnames ArrayList.
	 */
	
	public boolean existsTagname(String str) {
		int len = tagnames.size();
		for (int i=0; i<len; i++) {
			if ( tagnames.get(i).equals(str) )
				return true;
		}
		return false;
	}
	
	private void incrementTagnameIdentifier(int index) {
		tagname_identifiers.set(index,tagname_identifiers.get(index)+1);
	}
	
	/** 
	 *  Returns the corresponding RDF tagname from the string.
	 *  If the tagname hasn't been used yet, it will be tagname-1.
	 *  If the tagname has been seen before, it will be tagname-#, where
	 *  # is the least integer not used yet. So officially we can't have
	 *  more than 2^32-1 of the same tagname in our Document, but I think
	 *  that's something that we won't deal with!
	 * @param str The XML tagname.
	 * @return The RDF tagname.
	 */
	
	private String getRDFTagname(Node n) {
		/* First, we must check to see if we have already assigned one */
		String n_tagname = hash.get(n);
		if (n_tagname != null) {
			return n_tagname;
		}
		else {
			String str = n.getNodeName();
			if ( existsTagname(str) ) {
				/* Increment tagname Array List */
				int i=0;

				while (!tagnames.get(i).equals(str)) {
					i++;
				}
			
				incrementTagnameIdentifier(i);
				String result = tagnames.get(i)+"-"+tagname_identifiers.get(i).toString();
				hash.put(n,result);
				return result;
			}
			else {
				/* Add new entry */
				tagnames.add(str);
				tagname_identifiers.add(1);
				
				String result = str+"-1";
				hash.put(n,result);
				return result;
			}
		}
	}
	
	/**
	 *  Starts with a Document Node, and returns the corresponding 
	 *  Resource, without any connections made yet. It first looks at the
	 *  name of the Resource, and creates the appropriate RDF name, which
	 *  will be <name>-<#>, where <#> starts at 1 and increments for every
	 *  repeat we have going on.
	 * @param n The Node in our Document.
	 * @return The corresponding RDF Resource.
	 */
	
	public Resource getResource(Node node) {

		String RDFname = getRDFTagname(node);
		
		/* createResource works both ways. If the Resource already exists
		 * it returns it, and if it doesn't exist it creates it and then
		 * returns it. Bam!
		 */
		return model.createResource(NAMESPACE+RDFname);
	}
	
	public void BFS (Node node) {
		
		/* If it's a text node, then it's already taken care of. */
		if (node.getNodeName().equals(NODE_TEXT)) {
			return;
		}
		/* Get relevant information */
		NodeList children = node.getChildNodes();
		int children_len = children.getLength();
		
		/* Could be null */
		NamedNodeMap attributes = node.getAttributes();
		
		/* First create/get the Resource in the model */
		Resource r = getResource(node);
		
		/* Make statements with the attributes - the Predicates ARE Literals */
		if (attributes != null) {
			int attributes_len = attributes.getLength();
			for (int i=0; i<attributes_len; i++) {
				Node n = attributes.item(i);
				model.add(
						model.createStatement(
								r,
								model.getProperty(ATTRIBUTE,n.getNodeName()),
								model.createLiteral(n.getNodeValue(),false)));
			}
		}
		
		/* Make statements for the children */
		for (int i=0; i<children_len; i++) {
			Node n = children.item(i);
			/* We need to peek ahead and see if it's a text node.
			 * If it is, then it's the end of the line.
			 */
			if (n.getNodeName().equals(NODE_TEXT) && n.getNodeValue().trim().length() > 0) {
				model.add(model.createStatement(r,
												model.getProperty(NAMESPACE,TEXT),
												model.createLiteral(n.getNodeValue(),false)));
			}
			if (!n.getNodeName().equals(NODE_TEXT)) {
				model.add(model.createStatement(r,
												model.getProperty(NAMESPACE,HAS),
												getResource(n)));
			}
		}
		
		/* We can still recurse on children; text leafs will be ignored */
		for (int i=0; i<children_len; i++) {
			BFS(children.item(i));
		}
	}
	
	public void BFS() {
		Element root = doc.getDocumentElement();
		/* We first start off with one statement that says the document has . . . 
		 * Lucky for us, XML requires to only have ONE root element!
		 */
		Resource r = model.createResource(NAMESPACE+DOCUMENT);
		
		model.add(model.createStatement(r,
				model.getProperty(NAMESPACE,HAS),
				getResource(root)));
		
		/* Now we do the rest */
		BFS(root);
	}
	
//	public static void addStatementsFromAttributes (Node n, Model m) {
//		ArrayList<Statement> statements = new ArrayList<Statement>();
//		
//		NamedNodeMap attributes = n.getAttributes();
//		int len = attributes.getLength();
//		/* We are safe since only Elements can have attributes */
//		if (attributes != null && len > 0) {
//
//			for (int i=0; i<len; i++) {
//				statements.add(m.createStatement(m.createResource(n.getNodeName()),
//												 m.createProperty(NAMESPACE,"attribute/"+attributes.item(i).getNodeName()),
//												 m.createLiteral(attributes.item(i).getNodeValue(),false)));
//			}
//		}
//	}
	
	/** XMLtoModel
	 *  The key invariant here is that the RDFNode and the Node are both 
	 *  logistically in the same spot. So we take the attributes from n as 
	 *  property/literal counterparts to the RDFNode, and we can further
	 *  recurse with children.
	 * @param m
	 * @param o
	 * @param n
	 */
	
//	public static void XMLtoModel (Model m, RDFNode o, Node n) {
//		
//		System.out.println("Doing it");
//		StmtIterator statements = m.listStatements();
//		int count = 0;
//		while (statements.hasNext()) {
//			statements.next();
//			count++;
//		}
//		System.out.println("Model has "+Integer.toString(count)+" statements");
//
//		String nodeName = n.getNodeName();
//		String text;
//		
//		if (nodeName == NODE_TEXT) {
//			text = n.getNodeValue();
//			/* This is the end of the line, so we just add the statement and return */
//			m = m.add(m.createStatement((Resource) o,
//							  (Property) m.createProperty(NAMESPACE,"text"),
//							  text));
//		}
//		else {
//			/* We first take care of the attributes */
//			NamedNodeMap attributes = n.getAttributes();
//			if (attributes != null && attributes.getLength() > 0) {
//				int len = attributes.getLength();
//				for (int i=0; i<len; i++) {
//					m = m.add(m.createStatement((Resource) o,
//									  (Property) m.createProperty(NAMESPACE,"attribute/"+attributes.item(i).getNodeName()),
//									  attributes.item(i).getNodeValue()));
//				}
//			}
//			
//			/* Now we recurse over the children */
//			NodeList children = n.getChildNodes();
//			int children_len = children.getLength();
//			if (children_len == 0) {
//				return;
//			}
//			else {
//				for (int i=0; i<children_len; i++) {
//					/* Hack: check if we have blank input. Can be fixed with better XML. */
//					/* These are the only children that we "care" about.				 */
//					if (children.item(i).getNodeName() != "#text" || children.item(i).getTextContent().trim().length() > 0) {
//						RDFNode resource;
//						if (children.item(i).getNodeName() == NODE_TEXT) {
//							/* In this case we create an anonymous resource */
//							/* The only child of this will be text */
//							resource = m.createResource();
//						}
//						else {
//							resource = m.createResource(children.item(i).getNodeName());
//						}
//								
//						/* Create the statement */
//						m = m.add(m.createStatement((Resource) o,
//										  m.createProperty(NAMESPACE,HAS),
//										  resource));
//						
//						/* Recurse */
//						XMLtoModel(m,resource,children.item(i));			  
//					}
//				}
//			}
//		}
//	}
	
	public static void DFS_info (Node n, int level) {
		for (int i=0; i<level; i++) {
			System.out.print("\t");
		}
		NamedNodeMap attributes = n.getAttributes();
		System.out.print("Node "+n.getNodeName()+" has value "+n.getNodeValue());
		if (attributes == null || attributes.getLength() == 0) {
			System.out.println(" and no attributes");
		}
		else {
			System.out.print(" and attributes");
			int len = attributes.getLength();
			for (int j=0; j<len; j++) {
				System.out.print(" "+attributes.item(j).getNodeName()+"/"+attributes.item(j).getNodeValue());
			}
			System.out.println("");
		}
		
		NodeList children = n.getChildNodes();
		int children_len = children.getLength();
		if (children_len == 0) {
			return;
		}
		else {
			for (int i=0; i<children_len; i++) {
				// Hack: check if we have blank input. Can be fixed with better XML.
				if (children.item(i).getNodeName() != "#text" || children.item(i).getTextContent().trim().length() > 0)
					DFS_info(children.item(i),level+1);
			}
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