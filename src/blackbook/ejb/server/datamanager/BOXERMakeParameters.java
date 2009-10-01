package blackbook.ejb.server.datamanager;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.TransformerException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import security.ejb.client.User;
import blackbook.ejb.client.exception.BlackbookSystemException;

import com.hp.hpl.jena.rdf.model.Model;

import edu.dimacs.mms.boxer.ParseXML;
import edu.dimacs.mms.tokenizer.RDFNames;
import edu.dimacs.mms.tokenizer.XMLtoRDF2;

/** 
 * This method will take in information from an analyst to create a 
 * well-formed Document that can be fed into BOXER for training in the
 * next step.
 * A training Document has the following format:
 * <dataset name="">
 * <datapoint name="">
 * 	<labels>DISC1:LABEL1 DISC2:LABEL2 . . . </labels>
 * 	<features>word1:count1 word2:count2 word3:count3 . . . </labels>
 * </datapoint>
 * more datapoints . . . 
 * </dataset>
 * So what we need to do is extract the appropriate information. This will be
 * supplied by the analyst. This is how the syntax will work - the analyst will
 * supply a long string of the form
 * <dataset name> feature <feature 1> <feature 2> . . . <feature n>
 * labels <discr1> <property1> <discr2> <property2>  . . . 
 * where we first supply the dataset name. Then the analyst specifies
 * which properties have the features we're looking for. Finally, the analyst
 * writes "labels" followed by pairs, which are mappings of discrimination names
 * to properties which lead to the label for that discrimination.
 * It must be emphasized that the analyst does not specify explicity what the
 * features and labels are; rather, the analyst specifies the RDF property(ies)
 * that lead to the features and labels.
 * 
 * Things are slightly different when the objective is to test. In this 
 * case, the structure is exactly the same, except this time the analyst
 * must provide a FULL property URI, which will be used when saving
 * the assertions. A default will be used if a property name is not
 * specified for a discrimination.
 * 
 * @author praff
 *
 */
public class BOXERMakeParameters extends AbstractAlgorithmKeyword2Model {
	
	private static final String DESCRIPTION = "Makes the parameters dumb model based on the text input.";

    private static final String LABEL = "BOXER Make Parameters";
    

	/**
	 * 
	 */
	private static final long serialVersionUID = 7926225108124552613L;

	/* For testing purposes only */
	public static void main (String[] args) throws BlackbookSystemException, TransformerException {
		String tester = "monterey_set features incidentDescription labels AGRO STAT_TGT_AGRO BUS STAT_TGT_BUS";
		BOXERMakeParameters test = new BOXERMakeParameters();
		Model m = test.executeAlgorithm(null,null,tester);
		Document d = BOXERTools.convertToXML(m);
		System.out.print(BOXERTools.convertToString(d));
	}
	
	public static Document getParametersDocument(String keyword)
			throws BlackbookSystemException {
		String[] words = parseKeyword(keyword);
		int words_len = words.length;
		
		DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder docBuilder = null;
		Document doc = null;
		int i = 0;
		
		try {
			/* First - sanity check. */
			if (words_len < 6) {
				throw new Exception();
			}
			/* Now, the first one will be the name */
			docBuilder = docBuilderFactory.newDocumentBuilder();
			doc = docBuilder.newDocument();
			Element dataset_template = doc.createElement(RDFNames.XML_DATASET_TEMPLATE);
			dataset_template.setAttribute(ParseXML.ATTR.NAME_ATTR,words[0]);
			doc.appendChild(dataset_template);
			
			/* The next tag should be specific */
			
			if (words[1].equals(ParseXML.NODE.FEATURES) && !words[2].equals(ParseXML.NODE.LABELS)) {
				Element features = doc.createElement(ParseXML.NODE.FEATURES);
				String features_string = "";
				features_string += words[2];
				i = 3;
				while (!words[i].equals(ParseXML.NODE.LABELS) && i < words_len) {
					features_string += " " + words[i];
					i++;
				}
				features.setTextContent(features_string);
				dataset_template.appendChild(features);
			}
			else {
				if (!words[1].equals(ParseXML.NODE.FEATURES))
					throw new Exception("The second term must be " + ParseXML.NODE.FEATURES);
				if (words[2].equals(ParseXML.NODE.LABELS))
					throw new Exception("Nothing specified after " + ParseXML.NODE.FEATURES);
			}
			
			/* Now we should be seeing the labels tagname, followed by pairs */
			
			if (i < words_len-1 && words[i].equals(ParseXML.NODE.LABELS) && (words_len-i-1)%2==0) {
				Element labels = doc.createElement(ParseXML.NODE.LABELS);
				String label_string = "";
				label_string += words[i+1] + " " + words[i+2];
				i = i+3;
				while (i < words_len) {
					label_string += " " + words[i] + " " + words[i+1];
					i+=2;
				}
				labels.setTextContent(label_string);
				dataset_template.appendChild(labels);
			}
			else {
				if (i >= words_len-1)
					throw new Exception("Reached end of string without seeing " + ParseXML.NODE.LABELS);
				if (!words[i].equals(ParseXML.NODE.LABELS))
					throw new Exception("Expected, but did not get, " + ParseXML.NODE.LABELS);
				if ((words_len-i-1)%2==1)
					throw new Exception("Strings must be in pairs after " + ParseXML.NODE.LABELS);
			}
			//finalmodel = XMLtoRDF2.convertToRDF(doc);
			
			//System.out.print(XMLtoRDF2.convertToString(doc));
		}
		catch (Exception e) {
			System.out.println(e.getMessage());
		}
		
		return doc;
	}
	
	public Model executeAlgorithm(User user, String dataSource, String keyword) {
		Document doc = null;
		Model m = null;
		try {
			doc = getParametersDocument(keyword);
			m = XMLtoRDF2.convertToRDF(doc);
		} catch (BlackbookSystemException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (TransformerException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return m;
	}
	
	public static String[] parseKeyword(String keyword) {
		return keyword.split("[ \t]+");
	}
	
	public static void printStrings(String[] strings) {
		int len = strings.length;
		for (int i=0; i<len; i++) 
			System.out.println(strings[i]);
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
