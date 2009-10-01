/**
 * 
 */
package blackbook.ejb.server.datamanager;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
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
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import security.ejb.client.User;

import blackbook.ejb.client.exception.BlackbookSystemException;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.util.iterator.ExtendedIterator;

import edu.dimacs.mms.borj.*;
import edu.dimacs.mms.boxer.BoxerXMLException;

public class BOXERTrain extends AbstractAlgorithmModel2Model {
	
    private static final String DESCRIPTION = "Inputs the appropriately-classified documents as a Jena model and uses this as a foundation to train a new BOXER model.";

    private static final String LABEL = "BOXER Train";
    
	/* File usage in blackbook is done through the %BLACKBOOK_HOME% directory. */
    
	private final String RCV_FILENAME = "rcv.xml";
	private final String QREL_FILENAME = "qrel.xml";
	private final String SUITE_FILENAME = "suite.xml";
	private final String LEARNER_FILENAME = "learner.xml";
	
	private Document rcv_doc		=	null;
	private Document qrel_doc		=	null;
	private Document suite_doc		=	null;
	private Document learner_doc	=	null;
	
	private File rcv_file			=	null;
	private File qrel_file			=	null;
	private File suite_file			=	null;
	private File learner_file		=	null;
	
	private void setupSuiteAndLearner() throws ParserConfigurationException, TransformerException, IOException {
		
		suite_doc = createSuite();
		learner_doc = createLearner();
		
		TransformerFactory transfac = TransformerFactory.newInstance();
		
		/*
		For some reason blackbook does not like this method call!
		transfac.setAttribute("indent-number", new Integer(4));
		*/
		
        Transformer trans = transfac.newTransformer();
        trans.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
        trans.setOutputProperty(OutputKeys.INDENT, "yes");

        StringWriter suite_sw = new StringWriter();
        StreamResult suite_result = new StreamResult(suite_sw);
        DOMSource suite_source = new DOMSource(suite_doc);
        trans.transform(suite_source, suite_result);
        String suite_string = suite_sw.toString();
        
        StringWriter learner_sw = new StringWriter();
        StreamResult learner_result = new StreamResult(learner_sw);
        DOMSource learner_source = new DOMSource(learner_doc);
        trans.transform(learner_source, learner_result);
        String learner_string = learner_sw.toString();
        
        suite_file = new File(SUITE_FILENAME);
        learner_file = new File(LEARNER_FILENAME);
        
        FileWriter suite_fw = new FileWriter(suite_file);
        FileWriter learner_fw = new FileWriter(learner_file);
	     
	    suite_fw.write(suite_string);
	    suite_fw.close();
	    
	    learner_fw.write(learner_string);
	    learner_fw.close();
	}
	
	/** createSuite
	 *  This method creates a basic suite file by hand. Just for practice.
	 * @return
	 * @throws ParserConfigurationException 
	 */
	
	public Document createSuite() throws ParserConfigurationException {
		DocumentBuilderFactory dbfac = DocumentBuilderFactory.newInstance();
	    DocumentBuilder docBuilder = dbfac.newDocumentBuilder();
	    Document doc = docBuilder.newDocument();
	    
	    Element suite = doc.createElement("suite");
	    suite.setAttribute("name","RCV_Small_20081206");
	    suite.setAttribute("nctest","IGNORE");
	    suite.setAttribute("nctrain","ADD");
	    suite.setAttribute("version", "0.6.003");
	    
	    doc.appendChild(suite);
	    
	    Element discrimination = doc.createElement("discrimination");
	    discrimination.setAttribute("defaultclass","other");
	    discrimination.setAttribute("leftoverclass","other");
	    discrimination.setAttribute("name", "region");
	    discrimination.setAttribute("qrel","null");
	    
	    suite.appendChild(discrimination);
	    
	    Element classes = doc.createElement("classes");
	    classes.setTextContent("USA UK JAP GFR FRA AUSTR INDIA EEC CHINA other");
	    
	    discrimination.appendChild(classes);
	    
	    return doc;      
	}
	
	/** createLearner
	 *  This method creates a basic learner file. 
	 * @return	-	A basic learner.
	 * @throws ParserConfigurationException
	 */
	
	public Document createLearner() throws ParserConfigurationException {
		DocumentBuilderFactory dbfac = DocumentBuilderFactory.newInstance();
	    DocumentBuilder docBuilder = dbfac.newDocumentBuilder();
	    Document doc = docBuilder.newDocument();
	    
	    Element learner = doc.createElement("learner");
	    learner.setAttribute("name","boxer.ExponentiatedGradient");
	    learner.setAttribute("version", "0.6.003");
	    
	    doc.appendChild(learner);
	    
	    Element parameters = doc.createElement("parameters");
	    parameters.setTextContent(" ");
	    
	    learner.appendChild(parameters);
	    
	    return doc;
	}
	
//	/** createQRELDoc
//	 *  This method achieves a simple goal - it fills in the private
//	 *  Document qrel_doc with useful information. It only takes one of the
//	 *  documents from the input model, and it is not guaranteed which
//	 *  one specifically will be picked!
//	 * @param m		-	a Jena model
//	 * @throws ParserConfigurationException 
//	 */
//	
//	public void createQRELDoc(Model m) throws ParserConfigurationException {
//		ExtendedIterator resources = BOXERFilter.listURIResources(m);
//		
//		/* Extract the region */
//		if (resources.hasNext()) {
//			Resource r = (Resource) resources.next();
//			createQRELDoc(m,r);
//		}
//	}
	
	/** createQRELDoc
	 *  Same as before, except different inputs (model is implicit). This is made
	 *  so the user has control over which Resource is used, for Jena does not
	 *  always give the same resource if you ask it for the first one in a model!
	 * @param f		-	a BOXERFilter
	 * @param r		-	a Resource (presumably in f)
	 * @throws ParserConfigurationException
	 */
	
	public void createQRELDoc(Model m, Resource r) throws ParserConfigurationException {
		String region = BOXERFilter.findRegion(m,r);
		DocumentBuilderFactory dbfac = DocumentBuilderFactory.newInstance();
	    DocumentBuilder docBuilder = dbfac.newDocumentBuilder();
	    qrel_doc = docBuilder.newDocument();
	    
	    Element dataset = qrel_doc.createElement("dataset");
	    dataset.setAttribute("name","sample");
	    dataset.setAttribute("version","0.6.003");
	    
	    qrel_doc.appendChild(dataset);
	    
	    Element datapoint = qrel_doc.createElement("datapoint");
	    datapoint.setAttribute("name",r.toString());
	    
	    dataset.appendChild(datapoint);
	    
	    Element labels = qrel_doc.createElement("labels");
	    labels.setTextContent("region:"+region);
	    
	    datapoint.appendChild(labels);	
	}
	
	/** createQRELDoc
	 *  This creates a QREL document from ALL of the documents in the model.
	 * @param f
	 * @param m
	 * @throws ParserConfigurationException
	 */
	
	public void createQRELDoc(Model m) throws ParserConfigurationException {
		
		ExtendedIterator resources = BOXERFilter.listURIResources(m);
		Resource r;
		
		DocumentBuilderFactory dbfac = DocumentBuilderFactory.newInstance();
	    DocumentBuilder docBuilder = dbfac.newDocumentBuilder();
	    qrel_doc = docBuilder.newDocument();

	    Element dataset = qrel_doc.createElement("dataset");
	    dataset.setAttribute("name","sample");
	    dataset.setAttribute("version","0.6.003");
	    
	    qrel_doc.appendChild(dataset);
	    
		while (resources.hasNext()) {
			r = (Resource) resources.next();
			String region = BOXERFilter.findRegion(m,r);
		    
		    Element datapoint = qrel_doc.createElement("datapoint");
		    datapoint.setAttribute("name",r.toString());
		    
		    dataset.appendChild(datapoint);
		    
		    Element labels = qrel_doc.createElement("labels");
		    labels.setTextContent("region:"+region);
		    
		    datapoint.appendChild(labels);
		}
	}
	
	/** createRCVDoc
	 *  Same as createQRELDoc, except, of course, we create the RCV doc. 
	 *  This creates the RCV document with all documents that are in it.
	 * @param f
	 * @param r
	 * @throws ParserConfigurationException
	 */
	
	public void createRCVDoc(Model m) throws ParserConfigurationException {
		
		ExtendedIterator resources = BOXERFilter.listURIResources(m);
		Resource r;
		
		DocumentBuilderFactory dbfac = DocumentBuilderFactory.newInstance();
	    DocumentBuilder docBuilder = dbfac.newDocumentBuilder();
	    rcv_doc = docBuilder.newDocument();
	    
	    Element dataset = rcv_doc.createElement("dataset");
	    dataset.setAttribute("name","sample");
	    dataset.setAttribute("version","0.6.003");
	    
	    rcv_doc.appendChild(dataset);
	    
	    while (resources.hasNext()) { 
	    
	    	r = (Resource) resources.next();
	    	
		    Element datapoint = rcv_doc.createElement("datapoint");
		    datapoint.setAttribute("name",r.toString());
		    
		    dataset.appendChild(datapoint);
		    
		    Element features = rcv_doc.createElement("features");
		    features.setTextContent(BOXERFilter.findFeatures(m,r));
		    
		    datapoint.appendChild(features);
	    }
	}
	
	public void createRCVDoc(Model m, Resource r) throws ParserConfigurationException {
		DocumentBuilderFactory dbfac = DocumentBuilderFactory.newInstance();
	    DocumentBuilder docBuilder = dbfac.newDocumentBuilder();
	    rcv_doc = docBuilder.newDocument();
	    
	    Element dataset = rcv_doc.createElement("dataset");
	    dataset.setAttribute("name","sample");
	    dataset.setAttribute("version","0.6.003");
	    
	    rcv_doc.appendChild(dataset);
	    
	    Element datapoint = rcv_doc.createElement("datapoint");
	    datapoint.setAttribute("name",r.toString());
	    
	    dataset.appendChild(datapoint);
	    
	    Element features = rcv_doc.createElement("features");
	    features.setTextContent(BOXERFilter.findFeatures(m,r));
	    
	    datapoint.appendChild(features);	
	}

//	public void main (String[] argv) throws ParserConfigurationException, TransformerException, IOException, SAXException {
//         
//         Model m = ModelFactory.createDefaultModel();
//         FileInputStream file = new FileInputStream("c:\\integration\\RDFXMLresults.rdf");
//         m.read(file,"");
//         file.close();
//         
//         createQRELDoc(m);
//         System.out.println(XMLtoString(qrel_doc));
//         createRCVDoc(m);
//         System.out.println(XMLtoString(rcv_doc));
//         
//         rcv_file = new File(RCV_FILENAME);
//         qrel_file = new File(QREL_FILENAME);
//         suite_file = new File(SUITE_FILENAME);
//         learner_file = new File(LEARNER_FILENAME);
//         
//         FileWriter rcv_fw = new FileWriter(rcv_file);
//         FileWriter qrel_fw = new FileWriter(qrel_file);
//         FileWriter suite_fw = new FileWriter(suite_file);
//         FileWriter learner_fw = new FileWriter(learner_file);
//         
//         rcv_fw.write(XMLtoString(rcv_doc));
//         qrel_fw.write(XMLtoString(qrel_doc));
//         suite_fw.write(XMLtoString(suite_doc));
//         learner_fw.write(XMLtoString(learner_doc));
//         
//         rcv_fw.close();
//         qrel_fw.close();
//         suite_fw.close();
//         learner_fw.close();
//         
//         String[] driver_args = {"read-suite:"+SUITE_FILENAME,
//        		 				 "read-learner:"+LEARNER_FILENAME,
//        		 				 "read-labels:"+QREL_FILENAME,
//        		 				 "train:"+RCV_FILENAME,
//        		 				 "write:complex.xml"};
//         
//         Driver.main(driver_args);
//	}
	
	public static String XMLtoString(Document doc) throws TransformerException {
		TransformerFactory transfac = TransformerFactory.newInstance();
//        transfac.setAttribute("indent-number", new Integer(4));
        Transformer trans = transfac.newTransformer();
        trans.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
        trans.setOutputProperty(OutputKeys.INDENT, "yes");
		StringWriter sw = new StringWriter();
        StreamResult result = new StreamResult(sw);
        DOMSource source = new DOMSource(doc);
        trans.transform(source, result);
        return sw.toString();
	}
	
	public Document getQRELDoc() {
		return qrel_doc;
	}


	public Model executeAlgorithm(User user, Model model)
			throws BlackbookSystemException {
		try {
			setupSuiteAndLearner();
			createQRELDoc(model);
			createRCVDoc(model);
			
			rcv_file = new File(RCV_FILENAME);
			qrel_file = new File(QREL_FILENAME);
			
			FileWriter rcv_fw = new FileWriter(rcv_file);
			FileWriter qrel_fw = new FileWriter(qrel_file);
			FileWriter suite_fw = new FileWriter(suite_file);
			FileWriter learner_fw = new FileWriter(learner_file);
			
			rcv_fw.write(XMLtoString(rcv_doc));
			qrel_fw.write(XMLtoString(qrel_doc));
			suite_fw.write(XMLtoString(suite_doc));
			learner_fw.write(XMLtoString(learner_doc));
			
			rcv_fw.close();
			qrel_fw.close();
			suite_fw.close();
			learner_fw.close();
			
			String[] driver_args = {"read-suite:"+SUITE_FILENAME,
	        		 				 "read-learner:"+LEARNER_FILENAME,
	        		 				 "read-labels:"+QREL_FILENAME,
	        		 				 "train:"+RCV_FILENAME,
	        		 				 "write:complex.xml"};
	         
			Driver.main(driver_args);
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ParserConfigurationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (TransformerException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SAXException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (BoxerXMLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return ModelFactory.createDefaultModel();
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