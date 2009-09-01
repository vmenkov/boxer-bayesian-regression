package blackbook.ejb.server.datamanager;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import security.ejb.client.User;

import blackbook.ejb.client.exception.BlackbookSystemException;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.util.iterator.ExtendedIterator;

import edu.dimacs.mms.borj.Driver;

public class BOXERApply extends AbstractAlgorithmModel2Model {
	
    private static final String DESCRIPTION = "Inputs the Jena documents as the model. Takes in a hardcoded serialized complex and tests the model.";

    private static final String LABEL = "BOXER Apply";
	
	private final String RCV_FILENAME = "rcv.xml";
	private final String QREL_FILENAME = "qrel.xml";
	private final String LEARNER_COMPLEX_FILENAME = "complex.xml";
	
	private Document rcv_doc		=	null;
	private Document qrel_doc		= 	null;
	private Document complex_doc	=	null;
	
	private File rcv_file			=	null;
	private File qrel_file			=	null;
	private File complex_file		=	null;
	
	public BOXERApply() {
		
		rcv_file = new File(RCV_FILENAME);
		complex_file = new File(LEARNER_COMPLEX_FILENAME);
		
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
	    dataset.setAttribute("version","0.5.007");
	    
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
	
	public void createRCVDoc(Model m) throws ParserConfigurationException {
		
		ExtendedIterator resources = BOXERFilter.listURIResources(m);
		Resource r;
		
		DocumentBuilderFactory dbfac = DocumentBuilderFactory.newInstance();
	    DocumentBuilder docBuilder = dbfac.newDocumentBuilder();
	    rcv_doc = docBuilder.newDocument();
	    
	    Element dataset = rcv_doc.createElement("dataset");
	    dataset.setAttribute("name","sample");
	    dataset.setAttribute("version","0.5.007");
	    
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

	/**
	 * @param args
	 * @throws IOException 
	 * @throws ParserConfigurationException 
	 * @throws TransformerException 
	 * @throws SAXException 
	 */
	public static void main(String[] args) throws IOException, ParserConfigurationException, TransformerException, SAXException {
		
        BOXERApply test = new BOXERApply();
        
        Model m = ModelFactory.createDefaultModel();
        FileInputStream file = new FileInputStream("c:\\integration\\RDFXMLresults.rdf");
        m.read(file,"");
        file.close();
        
        test.createRCVDoc(m);
        test.createQRELDoc(m);
        
        test.rcv_file = new File(test.RCV_FILENAME);
        test.qrel_file = new File(test.QREL_FILENAME);
        
        FileWriter rcv_fw = new FileWriter(test.rcv_file);
        
        rcv_fw.write(BOXERTrain.XMLtoString(test.rcv_doc));
        
        rcv_fw.close();
        
        String[] driver_args = {"read:"+"/blackbook-2.7/"+test.LEARNER_COMPLEX_FILENAME,
        						"read-labels:"+"/blackbook-2.7/"+test.QREL_FILENAME,
       		 				 	"test:"+"/blackbook-2.7/"+test.RCV_FILENAME,
       		 				 	"write:complex2.xml"};
        
        Driver.main(driver_args);

	}

	public Model executeAlgorithm(User user, Model model)
			throws BlackbookSystemException {
		// TODO Auto-generated method stub
		try {
			createRCVDoc(model);
			createQRELDoc(model);
	        
	        rcv_file = new File(RCV_FILENAME);
	        qrel_file = new File(QREL_FILENAME);
	        
	        FileWriter rcv_fw = new FileWriter(rcv_file);
	        
	        rcv_fw.write(BOXERTrain.XMLtoString(rcv_doc));
	        
	        rcv_fw.close();
	        
	        String[] driver_args = {"read:/blackbook-2.7/"+LEARNER_COMPLEX_FILENAME,
	        						"read-labels:/blackbook-2.7/"+QREL_FILENAME,
	       		 				 	"test:/blackbook-2.7/"+RCV_FILENAME,
	       		 				 	"write:/blackbook-2.7/complex2.xml"};
	        
	        Driver.main(driver_args);
		} catch (ParserConfigurationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (TransformerException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SAXException e) {
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
