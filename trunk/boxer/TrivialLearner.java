package boxer;

import java.util.*;
import java.text.*;
import java.io.*;

// for XML output
import org.w3c.dom.*;
import org.apache.xerces.dom.DocumentImpl;
import org.apache.xml.serialize.*;


/* A trivial learner always assigns equal probabilities to all classes
 */
public class TrivialLearner extends Learner {

    class TrivialLearnerBlock extends Learner.LearnerBlock {

	TrivialLearnerBlock(Discrimination _dis) {
	    dis = _dis;
	}

	boolean isZero() { return true; }


	public void absorbExample(Vector<DataPoint> xvec, int i1, int i2) {
	}

	/** Estimates probabilities of a given data point's belonging to
	    various classes, using the exp(DP)/sum(exp(DP)) formula. This
	    is done separately in each Discrimination (set of classes).
	    Source: Figure 1a, PLRM Interpreter.
	    
	    @param p DataPoint to score
	    
	    @return double[], an array of probabilities for all classes of all discriminations. If oneDid=-1 (i.e., all discriminations), this array will be aligned with suite.id2label; otherwise, with Discrimination.classes of the selected discrimination */
	public double [] applyModel( DataPoint p) {
	    // select data for just one discrimination
	    int disSize= dis.claCount();
	    double[] s = new double[ disSize];
  	    for(int i=0; i<s.length; i++) {
		s[i] = 1.0/disSize;
	    }
	    return s;	    
	}	

	void parseDisc(Element e) {}

	public Element saveAsXML(Document xmldoc) {		
 	    Element de =   xmldoc.createElement(XMLUtil.CLASSIFIER);
	    de.setAttribute(XMLUtil.DISCRIMINATION, dis.getName());
	    return de;
	}

	public long memoryEstimate() { return 0; }


    }

    public TrivialLearner(Suite _suite) {
	setSuite( _suite);
    }

    public void describe(PrintWriter out, boolean verbose) {
	//System.out.println("=== (S) TrivialLearner Classifier===");
	out.println("===TrivialLearner Classifier===");
	out.println("Has no parameters");
	out.println("Main tables memory estimate=" + memoryEstimate() + " bytes");
	out.println("===============================");
	out.flush();
    }


    /** Creates an instance of TrivialLearner learner based on the
      content of an XML element (which may be the top-level element of
      an XML file), or more often, an element nested within a
      "learners" element within a "learner complex" element.
      
    */
    TrivialLearner(Suite suite, Element e) throws
	org.xml.sax.SAXException {
	this(suite);
	XMLUtil.assertName(e, XMLUtil.LEARNER);
    }


    /** This is invoked from {@link Suite.deleteDiscrimination()},
     * before the discrimination is purged from the Suite. Child
     * classes may override it, to delete more structures.
     */
    void deleteDiscrimination( RenumMap map) {
    } 

    LearnerBlock createBlock(Discrimination dis, LearnerBlock model) {
	return new TrivialLearnerBlock(dis);
    }

    /*
    public Element saveAsXML(Document xmldoc) {
		
	Element root = xmldoc.createElement( XMLUtil.LEARNER);
	root.setAttribute(ParseXML.ATTR.NAME_ATTR, algoName());
	root.setAttribute("version", Version.version);

	root.appendChild(createParamsElement(xmldoc, 
					     new String[] {},
					     new Object[] {}) );

	int disCnt =suite.did2discr.size();
       
	for(int did=0; did< disCnt; did++) {
	    Discrimination dis =  suite.did2discr.elementAt(did);
 	    Element de =   xmldoc.createElement(XMLUtil.CLASSIFIER);
	    de.setAttribute(XMLUtil.DISCRIMINATION, dis.getName());
	    root.appendChild(de);
	}
	return root;
    }
*/



}

