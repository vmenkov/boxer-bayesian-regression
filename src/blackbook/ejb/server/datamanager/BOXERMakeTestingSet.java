package blackbook.ejb.server.datamanager;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.HashMap;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import security.ejb.client.User;
import blackbook.ejb.client.exception.BlackbookSystemException;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.util.iterator.ExtendedIterator;

import edu.dimacs.mms.boxer.ParseXML;
import edu.dimacs.mms.boxer.Version;
import edu.dimacs.mms.tokenizer.RDFNames;

/**
 * This workflow algorithm takes in a model containing the parameters and a model 
 * containing documents, and it crafts a dumb model containing an 
 * appropriately-formatted testing set for use with BOXER. 
 * @author praff
 *
 */
public class BOXERMakeTestingSet extends AbstractAlgorithmMultiModel2Model {
	
	private static final String DESCRIPTION = "Given parameters and documents, makes the testing set. Passes through the parameters, for it is needed in the future.";

    private static final String LABEL = "BOXER Make Testing Set";
    
	
	public static final String COMBINED_ROOT = "combined";
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -780074092240353151L;

	/* For testing purposes only */
	public static void main (String[] args) {
		String filepath = BOXERTerms.TEST_DIR + "monterey-time.rdf";
		
		Model m = ModelFactory.createDefaultModel();
		try {
			m.read(new FileInputStream(new File(filepath)),null);

			Document doc_params = BOXERMakeParameters.getParametersDocument("monterey_set features incidentDescription labels MED http://www.blackbook.com/terms#STAT_TGT_MED");
			Model m_params = BOXERTools.convertToRDF(doc_params);
			
			BOXERMakeTestingSet testing = new BOXERMakeTestingSet();
			Model m_combined = testing.executeAlgorithm(null, m_params, m);
			
			BOXERTools.saveModelAsFile(m_combined, BOXERTerms.TEST_DIR + "combined-testdocs.rdf");
			
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (BlackbookSystemException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}


	/**
	 * We assume we have input a well-formed parameter document, and we
	 * also have the model that contains all of our data.
	 * @param 	parameters	The parameters DOM Document
	 * @param	m			The model containing the documents
	 * @return	A DOM Document containing a properly-formatted BOXER testing set.
	 */
	public static Document getTestingDataset(Document parameters, Model m) throws BOXERBlackbookException {
		Element dataset_template = parameters.getDocumentElement();
		
		DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder docBuilder = null;
		Document doc = null;
		try {
		    docBuilder = docBuilderFactory.newDocumentBuilder();
		    doc = docBuilder.newDocument();
		}
		catch (ParserConfigurationException e) {
		    e.printStackTrace();
		}
		
		String dataset_name = "";
		String feature_properties = "";
		
		/* Gather the information */
		if (dataset_template.getTagName().equals(RDFNames.XML_DATASET_TEMPLATE)) {
			dataset_name = dataset_template.getAttribute(ParseXML.ATTR.NAME_ATTR);
		}
		else {
			// TODO: Make a better exception
			throw new BOXERBlackbookException("Malformed parameters file - root element must be " + RDFNames.XML_DATASET_TEMPLATE);
		}
		
		
		Node e = dataset_template.getFirstChild();
		while (e != null) {
			/* The easy case */
			if (e.getNodeName().equals(ParseXML.NODE.FEATURES)) {
				/* Just in case we have multiple labels */
				feature_properties += " " + e.getTextContent();
			}
			e = e.getNextSibling();
		}
		
		/* Start building the document */
		Element dataset = doc.createElement(ParseXML.NODE.DATASET);
		dataset.setAttribute(ParseXML.ATTR.NAME_ATTR, dataset_name);
		dataset.setAttribute(ParseXML.ATTR.VERSION_ATTR, Version.version);
		doc.appendChild(dataset);
		
		String[] properties_to_look_for_features = feature_properties.split(BOXERTerms.SPLIT_REGEX);
		
		/* Now we look through the model and craft the document dataset */
		ExtendedIterator URIResources = BOXERFilter.listURIResources(m);
		while (URIResources.hasNext()) {
			Resource r = (Resource) URIResources.next();
			Element datapoint = doc.createElement(ParseXML.NODE.DATAPOINT);
			datapoint.setAttribute(ParseXML.ATTR.NAME_ATTR, r.getURI());
			
			Element features = doc.createElement(ParseXML.NODE.FEATURES);
			features.setTextContent(getWordCounts(cleanUpString(aggregateFromProperties(m,r,properties_to_look_for_features))));
		
			datapoint.appendChild(features);
			dataset.appendChild(datapoint);
		}
		
		return doc;
	}
	
	/** Prints a list of strings in list form 
	 * @param	strings	The list of strings
	 * @return	A text representation of the list of strings.
	 */
	static void printList(String[] strings) {
		BOXERMakeTrainingSet.printList(strings);
	}
	
	/**
	 * Takes in text and returns the word counts, in string form.
	 * @param 	everything	All of the text.
	 * @return	A string representation of the word counts from the text.
	 */
	static String getWordCounts(String everything) {
		return BOXERMakeTrainingSet.getWordCounts(everything);
	}
	
	/**
	 * Convenience method to get rid of multiples - sorts them in the process
	 * @param	strings	A list of strings
	 * @return	The sorted list of strings, with multiples removed.
	 */
	public static String[] filterUnique(String[] strings) {
		return BOXERMakeTrainingSet.filterUnique(strings);
	}
	
	/**
	 * Gets all Property URIs of the model.
	 * @param	m	The Jena model.
	 * @return	A list of strings of all of the Property URIs of the model.
	 */
	public static String[] getProperties(Model m) {
		return BOXERMakeTrainingSet.getProperties(m);
	}
	
	/**
	 * This takes a model and a resource, and finds the literal that
	 * is a direct descendant of the resource that satisfies the property.
	 * @param	m	The global Jena model
	 * @param	r	The resource considered
	 * @param	p	The property considered
	 * @return	The text content of the literal, if it exists, that is the answer to the question (?,r,?)
	 * 			for all statements that can be accessed from the resource.
	 */
	static String aggregateFromProperty(Model m, Resource r, String p) {
		return BOXERMakeTrainingSet.aggregateFromProperty(m, r, p);
	}
	
	/** 
	 * Just like {@link aggregateFromProperty}, but this takes in multiple properties
	 * @param	m			The global Jena model
	 * @param	r			The resource considered
	 * @param	properties	The properties considered
	 * @return	The concatenated text content of the literals, if they exist, of
	 * answers to the question (?,p,?), where p can be any property in the input list.
	 */
	static String aggregateFromProperties(Model m, Resource r, String[] properties) {
		return BOXERMakeTrainingSet.aggregateFromProperties(m, r, properties);
	}
	
	/**
	 * We have the mapping from discriminations to property names,
	 * which we need to translate from discriminations to label names,
	 * which are found by searching through the model and seeing what
	 * comes out by searching for the appropriate property.
	 * <p>
	 * In this, we assume that the analyst knows that the appropriate
	 * label will be in the input HashMap that maps discriminations to properties.
	 * <p>
	 * Are maps inefficient?
	 * 
	 * @param	m								The global Jena model
	 * @param	r								The resource being considered
	 * @param	discrimination_property_pairs	The pairing of discriminations to the properties in the model to look for
	 * @return	A HashMap containing discrimination^label pairings that are used in the creation
	 * 			of the testing set.
	 */
	
	static HashMap<String,String> getDiscriminationLabelPairs(Model m, Resource r, HashMap<String,String> discrimination_property_pairs) {
		return BOXERMakeTrainingSet.getDiscriminationLabelPairs(m, r, discrimination_property_pairs);
	}
	
	/**
	 * This converts our map into the DISC^LABEL format that's needed.
	 * Note that we omit if there is no LABEL available; this is
	 * because there is no harm in not giving labels; simply nothing
	 * will be learned, then!
	 * @param	map	The HashMap containing the discrimination/label information.
	 * @return	The string giving the DISC^LABEL pairings for use in the testing Document.
	 */
	static String convertToPairs(HashMap<String,String> map) {
		return BOXERMakeTrainingSet.convertToPairs(map);
	}
	
	/**
	 * Removes any characters that aren't alphanumeric, and replaces them with a space.
	 * @param	s	The String to clean up.
	 * @return	The String obtained by replacing any non-alphanumeric character with a space.
	 */
	static String cleanUpString(String s) {
		return BOXERMakeTrainingSet.cleanUpString(s);
	}

	/**
	 * Takes in the dumb model containing the parameters and the model of documents, and
	 * crafts a dumb model containing the properly-formatted testing set with
	 * information obtained from the documents.
	 * @param	user			The user
	 * @param	m_parameters	The parameters given by the analyst
	 * @param	m_documents		The Jena model containing the documents
	 * @return	A dumb model containing a properly-formatted testing dataset to be fed into BORJ.
	 */
	public Model executeAlgorithm(User user, Model m_parameters, Model m_documents)
			throws BlackbookSystemException {
		
		/* First, we extract the parameters */
		Document d_parameters = null;
		try {
			d_parameters = BOXERTools.convertToXML(m_parameters);
		} catch (Exception e3) {
			// TODO Auto-generated catch block
			e3.printStackTrace();
		}
		Document d_testingset = null;
		Document d_combined = null;
		
		Model final_result = null;
		
		DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder docBuilder = null;

		try {
			d_testingset = getTestingDataset(d_parameters,m_documents);
			
			/* Now we need to "merge" the testing set with the parameters */
			docBuilder = docBuilderFactory.newDocumentBuilder();
		    d_combined = docBuilder.newDocument();
		    Element root = d_combined.createElement(COMBINED_ROOT);
		   		    
		    Element e1 = (Element) d_combined.importNode(d_parameters.getDocumentElement(),true);
		    Element e2 = (Element) d_combined.importNode(d_testingset.getDocumentElement(), true);
		    
		    root.appendChild(e1);
		    root.appendChild(e2);
		    d_combined.appendChild(root);
		    
		    /* Turn into a model in the dumb way */
		    final_result = BOXERTools.convertToRDF(d_combined);
		} catch (BOXERBlackbookException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (TransformerException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ParserConfigurationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return final_result;
		
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