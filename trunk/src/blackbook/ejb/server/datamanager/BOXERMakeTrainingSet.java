package blackbook.ejb.server.datamanager;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import security.ejb.client.User;
import blackbook.ejb.client.exception.BlackbookSystemException;

import com.hp.hpl.jena.rdf.model.Literal;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.util.iterator.ExtendedIterator;

import edu.dimacs.mms.boxer.BXRReader;
import edu.dimacs.mms.boxer.ParseXML;
import edu.dimacs.mms.boxer.Version;
import edu.dimacs.mms.tokenizer.RDFNames;

public class BOXERMakeTrainingSet extends AbstractAlgorithmMultiModel2Model {

	/**
	 * 
	 */
	private static final long serialVersionUID = 2839973294880710637L;

	private static final String DESCRIPTION = "Given parameters and documents, constructs a BOXER training set as a dumb model.";

	private static final String LABEL = "BOXER Make Training Set";

	/* For testing purposes only */
	public static void main(String[] args) {
		String dir = "C://Users//praff//Desktop//2009-2010//BOXER//examples//";
		String filepath = dir + "monterey-blood.rdf";

		Model m = ModelFactory.createDefaultModel();
		try {
			m.read(new FileInputStream(new File(filepath)), null);
			String[] properties = getProperties(m);
			ExtendedIterator resources = BOXERFilter.listURIResources(m);
			Resource r = (Resource) resources.next();

			Document d_params = BOXERMakeParameters
					.getParametersDocument("monterey_set features incidentDescription labels AGRO STAT_TGT_AGRO BUS STAT_TGT_BUS ED STAT_TGT_ED GOVT STAT_TGT_GOVT IND STAT_TGT_IND INDISCRIM STAT_TGT_INDISCRIM MED STAT_TGT_MED NA STAT_TGT_NA ORG STAT_TGT_ORG UNK STAT_TGT_UNK");
			Model m_params = BOXERTools.convertToRDF(d_params);
			System.out.println(BOXERTools.convertToString(d_params));
			BOXERTools.saveModelAsFile(m_params, dir + "monterey-params.rdf");

			Document training_set = getTrainingDataset(d_params, m);
			System.out.println(BOXERTools.convertToString(training_set));

			BOXERMakeTrainingSet test = new BOXERMakeTrainingSet();
			Model m_trainset = test.executeAlgorithm(null, m_params, m);
			Document d_trainset = BOXERTools.convertToXML(m_trainset);
			System.out.println(BOXERTools.convertToString(d_trainset));
			BOXERTools.saveDocumentAsFile(d_trainset, BOXERTerms.TEST_DIR
					+ "monterey-trainset.xml");

			BOXERTools.saveModelAsFile(m_trainset, dir
					+ "monterey-trainset.rdf");

			Model m2 = BOXERTools
					.readFileToModel(dir + "monterey-trainset.rdf");
			System.out.print(m2.toString());
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
	 * Prints a list of strings in list form
	 * 
	 * @param strings
	 *            The list of strings
	 * @return A text representation of the list of strings.
	 */
	static void printList(String[] strings) {
		int len = strings.length;
		if (len == 0) {
			System.out.println("[]");
			return;
		}
		String final_result = "[" + strings[0];
		for (int i = 1; i < len; i++) {
			final_result += "," + strings[i];
		}
		System.out.println(final_result += "]");
	}

	/**
	 * Removes any elements of the array that are empty strings.
	 * 
	 * @param strings
	 *            The array of Strings.
	 * @return An array of Strings such that all elements are non-empty.
	 */
	static String[] removeEmpties(String[] strings) {
		int len = strings.length;
		int count = 0;
		for (int i = 0; i < len; i++) {
			if (strings[i].length() > 0)
				count++;
		}

		String[] goodies = new String[count];
		count = 0;
		for (int i = 0; i < len; i++) {
			if (strings[i].length() > 0)
				goodies[count++] = strings[i];
		}

		return goodies;
	}

	/**
	 * Takes in text and returns the word counts, in string form.
	 * 
	 * @param everything
	 *            All of the text.
	 * @return A string representation of the word counts from the text.
	 */
	static String getWordCounts(String everything) {
		String new_everything = everything.replace(
				BXRReader.PAIR_SEPARATOR_STRING, " ");
		String[] parsed = removeEmpties(new_everything.split("[ \t]+"));
		int parsed_len = parsed.length;
		if (parsed_len == 0)
			return "";
		Arrays.sort(parsed);
		String cur_word = parsed[0];
		int count = 1;
		int i = 1;

		String final_result = "";

		while (i < parsed_len) {
			if (!cur_word.equals(parsed[i])) {
				final_result += cur_word + BXRReader.PAIR_SEPARATOR
						+ Integer.toString(count) + " ";
				cur_word = parsed[i];
				count = 1;
			} else {
				count++;
			}
			i++;
		}
		final_result += cur_word + BXRReader.PAIR_SEPARATOR
				+ Integer.toString(count);
		return final_result;
	}

	/**
	 * Convenience method to get rid of multiples - sorts them in the process
	 * 
	 * @param strings
	 *            A list of strings
	 * @return The sorted list of strings, with multiples removed.
	 */
	static String[] filterUnique(String[] strings) {
		int len = strings.length;
		if (len == 0)
			return strings;
		String[] strings_copy = Arrays.copyOf(strings, len);
		Arrays.sort(strings_copy);
		String cur_string = strings_copy[0];
		int unique_count = 1;
		for (int i = 1; i < len; i++) {
			if (!strings_copy[i].equals(cur_string)) {
				unique_count++;
				cur_string = strings_copy[i];
			}
		}
		String[] final_result = new String[unique_count];
		cur_string = strings_copy[0];
		final_result[0] = cur_string;
		unique_count = 1;
		for (int i = 1; i < len; i++) {
			if (!strings_copy[i].equals(cur_string)) {
				/* A new one */
				cur_string = strings_copy[i];
				final_result[unique_count] = cur_string;
				unique_count++;
			}
		}
		return final_result;
	}

	/**
	 * Gets all Property URIs of the model.
	 * 
	 * @param m
	 *            The Jena model.
	 * @return A list of strings of all of the Property URIs of the model.
	 */
	static String[] getProperties(Model m) {
		StmtIterator statements = m.listStatements();
		ArrayList<String> property_names = new ArrayList<String>();
		while (statements.hasNext()) {
			Statement s = (Statement) statements.next();
			property_names.add(s.getPredicate().getURI());
		}

		int size = property_names.size();
		String[] properties = new String[size];
		for (int i = 0; i < size; i++) {
			properties[i] = property_names.get(i);
		}
		return filterUnique(properties);
	}

	/**
	 * This takes a model and a resource, and finds the literal that is a direct
	 * descendant of the resource that satisfies the property.
	 * 
	 * @param m
	 *            The global Jena model
	 * @param r
	 *            The resource considered
	 * @param p
	 *            The property considered
	 * @return The text content of the literal, if it exists, that is the answer
	 *         to the question (?,r,?) for all statements that can be accessed
	 *         from the resource.
	 */
	static String aggregateFromProperty(Model m, Resource r, String p) {
		ArrayList<Statement> statements = BOXERFilter.BFS(m, r);
		String final_results = "";
		for (Statement s : statements) {
			if (s.getPredicate().getURI().endsWith(p)
					&& s.getObject().isLiteral()) {
				final_results += ((Literal) s.getObject()).getString() + " ";
			}
		}

		return final_results;
	}

	/**
	 * Just like {@link aggregateFromProperty}, but this takes in multiple
	 * properties
	 * 
	 * @param m
	 *            The global Jena model
	 * @param r
	 *            The resource considered
	 * @param properties
	 *            The properties considered
	 * @return The concatenated text content of the literals, if they exist, of
	 *         answers to the question (?,p,?), where p can be any property in
	 *         the input list.
	 */
	static String aggregateFromProperties(Model m, Resource r,
			String[] properties) {
		ArrayList<Statement> statements = BOXERFilter.BFS(m, r);
		String final_results = "";
		for (String p : properties) {
			for (Statement s : statements) {
				if (s.getPredicate().getURI().endsWith(p)
						&& s.getObject().isLiteral()) {
					final_results += ((Literal) s.getObject()).getString()
							+ " ";
				}
			}
		}

		return final_results;
	}

	/**
	 * We have the mapping from discriminations to property names, which we need
	 * to translate from discriminations to label names, which are found by
	 * searching through the model and seeing what comes out by searching for
	 * the appropriate property.
	 * <p>
	 * In this, we assume that the analyst knows that the appropriate label will
	 * be in the input HashMap that maps discriminations to properties.
	 * <p>
	 * Are maps inefficient?
	 * 
	 * @param m
	 *            The global Jena model
	 * @param r
	 *            The resource being considered
	 * @param discrimination_property_pairs
	 *            The pairing of discriminations to the properties in the model
	 *            to look for
	 * @return A HashMap containing discrimination^label pairings that are used
	 *         in the creation of the testing set.
	 */
	static HashMap<String, String> getDiscriminationLabelPairs(Model m,
			Resource r, HashMap<String, String> discrimination_property_pairs) {
		Set<String> discriminations = discrimination_property_pairs.keySet();
		HashMap<String, String> discrimination_label_pairs = new HashMap<String, String>();
		for (String s : discriminations) {
			discrimination_label_pairs.put(s, BOXERFilter
					.genericFinderEndsWith(m, r, discrimination_property_pairs
							.get(s)));
		}
		return discrimination_label_pairs;
	}

	/**
	 * We assume we have input a well-formed parameter document, and we also
	 * have the model that contains all of our data.
	 * 
	 * @param parameters
	 *            The parameters DOM Document
	 * @param m
	 *            The model containing the documents
	 * @return A DOM Document containing a properly-formatted BOXER training
	 *         set.
	 */
	static Document getTrainingDataset(Document parameters, Model m)
			throws Exception {
		Element dataset_template = parameters.getDocumentElement();

		DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory
				.newInstance();
		DocumentBuilder docBuilder = null;
		Document doc = null;
		try {
			docBuilder = docBuilderFactory.newDocumentBuilder();
			doc = docBuilder.newDocument();
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		}

		String dataset_name = "";
		String feature_properties = "";
		HashMap<String, String> discrimination_property_map = new HashMap<String, String>();

		/* Gather the information */
		if (dataset_template.getTagName().equals(RDFNames.XML_DATASET_TEMPLATE)) {
			dataset_name = dataset_template
					.getAttribute(ParseXML.ATTR.NAME_ATTR);
		} else {
			// TODO: Make a better exception
			throw new BOXERBlackbookException(
					"Malformed parameters file - root element must be "
							+ RDFNames.XML_DATASET_TEMPLATE);
		}

		Node n = dataset_template.getFirstChild();
		while (n != null) {

			/* The easy case */
			if (n.getNodeName().equals(ParseXML.NODE.FEATURES)) {
				/* Just in case we have multiple labels */
				feature_properties += " " + n.getTextContent();
			} else if (n.getNodeName().equals(ParseXML.NODE.LABELS)) {
				String[] parts = n.getTextContent().split("[ \t]+");
				int parts_len = parts.length;
				if (parts_len % 2 == 1)
					throw new BOXERBlackbookException(
							"Malformed parameters file - element "
									+ ParseXML.NODE.LABELS
									+ " must have an even number of whitespace-separated words");
				for (int i = 0; i < parts_len; i += 2) {
					discrimination_property_map.put(parts[i], parts[i + 1]);
				}
			}
			n = n.getNextSibling();
		}

		/* Start building the document */
		Element dataset = doc.createElement(ParseXML.NODE.DATASET);
		dataset.setAttribute(ParseXML.ATTR.NAME_ATTR, dataset_name);
		dataset.setAttribute(ParseXML.ATTR.VERSION_ATTR, Version.version);
		doc.appendChild(dataset);

		String[] properties_to_look_for_features = feature_properties
				.split("[ \t]+");

		/* Now we look through the model and craft the document dataset */
		ExtendedIterator URIResources = BOXERFilter.listURIResources(m);
		while (URIResources.hasNext()) {
			Resource r = (Resource) URIResources.next();
			Element datapoint = doc.createElement(ParseXML.NODE.DATAPOINT);
			datapoint.setAttribute(ParseXML.ATTR.NAME_ATTR, r.getURI());

			Element features = doc.createElement(ParseXML.NODE.FEATURES);
			features
					.setTextContent(getWordCounts(cleanUpString(aggregateFromProperties(
							m, r, properties_to_look_for_features))));

			HashMap<String, String> discrimination_label_pairs = getDiscriminationLabelPairs(
					m, r, discrimination_property_map);
			Element labels = doc.createElement(ParseXML.NODE.LABELS);
			labels.setTextContent(convertToPairs(discrimination_label_pairs));

			datapoint.appendChild(features);
			datapoint.appendChild(labels);
			dataset.appendChild(datapoint);
		}

		return doc;
	}

	/**
	 * This converts our map into the DISC^LABEL format that's needed.
	 * Note that we omit if there is no LABEL available; this is
	 * because there is no harm in not giving labels; simply nothing
	 * will be learned, then!
	 * @param	map	The HashMap containing the discrimination/label information.
	 * @return	The string giving the DISC^LABEL pairings for use in the testing Document.
	 */
	static String convertToPairs(HashMap<String, String> map) {
		String result = "";
		Set<String> firsts = map.keySet();
		int len = firsts.size();
		if (len == 0) {
			return result;
		}

		for (String s : firsts) {
			String label = map.get(s);
			if (label.length() > 0)
				result += s + BXRReader.PAIR_SEPARATOR + label + " ";
		}

		/* Remove the last space, just for convenience */
		if (result.length() > 0)
			return result.substring(0, result.length() - 1);
		else
			return result;
	}

	/**
	 * Removes any characters that aren't alphanumeric, and replaces them with a space.
	 * @param	s	The String to clean up.
	 * @return	The String obtained by replacing any non-alphanumeric character with a space.
	 */
	static String cleanUpString(String s) {
		return s.replaceAll("[^a-zA-Z0-9]", " ");
	}

	/**
	 * Takes in the dumb model containing the parameters and the model of documents, and
	 * crafts a dumb model containing the properly-formatted training set with
	 * information obtained from the documents.
	 * @param	user			The user
	 * @param	m_parameters	The parameters given by the analyst
	 * @param	m_documents		The Jena model containing the documents
	 * @return	A dumb model containing a properly-formatted training dataset to be fed into BORJ.
	 */
	public Model executeAlgorithm(User user, Model m_parameters,
			Model m_documents) throws BlackbookSystemException {
		// TODO Auto-generated method stub
		Document d_parameters = null;
		try {
			d_parameters = BOXERTools.convertToXML(m_parameters);
		} catch (BOXERBlackbookException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		Document d_trainingset = null;
		Model m_trainingset = null;

		try {
			d_trainingset = getTrainingDataset(d_parameters, m_documents);
			m_trainingset = BOXERTools.convertToRDF(d_trainingset);
		} catch (TransformerException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return m_trainingset;
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
 * Copyright 2009, Rutgers University, New Brunswick, NJ.
 * 
 * All Rights Reserved
 * 
 * Permission to use, copy, and modify this software and its documentation for
 * any purpose other than its incorporation into a commercial product is hereby
 * granted without fee, provided that the above copyright notice appears in all
 * copies and that both that copyright notice and this permission notice appear
 * in supporting documentation, and that the names of Rutgers University,
 * DIMACS, and the authors not be used in advertising or publicity pertaining to
 * distribution of the software without specific, written prior permission.
 * 
 * RUTGERS UNIVERSITY, DIMACS, AND THE AUTHORS DISCLAIM ALL WARRANTIES WITH
 * REGARD TO THIS SOFTWARE, INCLUDING ALL IMPLIED WARRANTIES OF MERCHANTABILITY
 * AND FITNESS FOR ANY PARTICULAR PURPOSE. IN NO EVENT SHALL RUTGERS UNIVERSITY,
 * DIMACS, OR THE AUTHORS BE LIABLE FOR ANY SPECIAL, INDIRECT OR CONSEQUENTIAL
 * DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR
 * PROFITS, WHETHER IN AN ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS
 * ACTION, ARISING OUT OF OR IN CONNECTION WITH THE USE OR PERFORMANCE OF THIS
 * SOFTWARE.
 */