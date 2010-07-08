package edu.dimacs.mms.boxer;

import java.util.*;


import org.apache.xerces.dom.DocumentImpl;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
//import org.w3c.dom.Node;
//import org.xml.sax.SAXException;


/** An auxiliary class for {@link Priors}: stores Priors of several levels for one
    discrimination, or the defaults for all discriminations. */
class DiscPriorSet {
    /* L4 prior; or, when used for "all discriminations", the L1 prior. */
    Prior L_overall;
    /* L5, L6, L7 priors in a single table. When used for "all discriminations", L5(d.*,f) corresponds to L2(*.*,f), and others don't have common correspondences  */
    HashMap<CFKey, Prior> L567 = new  HashMap<CFKey, Prior>();
    /** Retrieves the prior that applies for the given
	(discrimination.class, feature) combination on any of the
	levels L4 thru L7. Higher levels take priority.
	@return The applicable prior, or null if none has been found.	 
    */
    Prior get(Discrimination.Cla c, int fid) {
	if (this instanceof CrossDiscPriorSet && c!=null) throw new AssertionError("DiscPriorSet(c!=null, fid) should not be called on CrossDiscPriorSet");

	Prior  p = L567.get( new CFKey(c, fid));
	if (p!=null) return p;
	// L6: (D.c, *)
	p = L567.get( new CFKey(c));
	if (p!=null) return p;
	// L5: (D.*, f)
	p = L567.get( new CFKey(null, fid));
	if (p!=null) return p;
	return L_overall;
    }

    void put(CFKey key, Prior p) {
	L567.put(key, p);
    }


    public Element saveFeatureSpecificAsXML(Document xmldoc, 
					    FeatureDictionary dic
					    ) {
	Element e = xmldoc.createElement(Priors.NODE.FEATURES);
	Set<CFKey> keys = L567.keySet(); 
	for(Iterator<CFKey> it = keys.iterator() ; it.hasNext(); ) {
	    CFKey key = it.next();
	    if (key.isAllClasses() && !key.isAllFeatures()) {
		Element z = xmldoc.createElement(Priors.NODE.FEATURE_PRIOR);

		z.setAttribute(Prior.ATTR.FEATURE, dic.getLabel(key.getFid()));	

		e.appendChild(z);
		z.appendChild( L567.get(key).saveAsXML(xmldoc));
	    }
	}
	return e;
    }


    /** @param dis The pertinent discrimination, or null (for cross-discr)
     */
    public Element saveClassSpecificAsXML(Document xmldoc, Discrimination dis) {
	if ((dis==null) ^ (this instanceof CrossDiscPriorSet)) {
	    throw  new IllegalArgumentException("dis==null should be only used with cross disc data set");
	}
								  

	Element e = xmldoc.createElement(Priors.NODE.CLASSES);
	Set<CFKey> keys = L567.keySet(); 
	for(Iterator<CFKey> it = keys.iterator() ; it.hasNext(); ) {
	    CFKey key = it.next();
	    if (!key.isAllClasses() && key.isAllFeatures()) {
		Element z = xmldoc.createElement(Priors.NODE.CLASS_PRIOR);
		z.setAttribute(Prior.ATTR.CLASS, key.getCName(dis));
		e.appendChild(z);
		z.appendChild( L567.get(key).saveAsXML(xmldoc));
	    }
	}
	return e;
    }

  /** @param dis The pertinent discrimination
     */
    public Element saveCoeffSpecificAsXML(Document xmldoc,  FeatureDictionary dic, Discrimination dis) {

	if (dis==null) {
	    throw  new IllegalArgumentException("dis==null should be used for coeff specific data");
	}


	Element e = xmldoc.createElement(Priors.NODE.COEFFICIENTS);
	Set<CFKey> keys = L567.keySet(); 
	for(Iterator<CFKey> it = keys.iterator() ; it.hasNext(); ) {
	    CFKey key = it.next();
	    if (!key.isAllClasses() && !key.isAllFeatures()) {
		Element z = xmldoc.createElement(Priors.NODE.COEFF_PRIOR);
		z.setAttribute(Prior.ATTR.FEATURE,dic.getLabel(key.getFid()));
		z.setAttribute(Prior.ATTR.CLASS, key.getCName(dis));
		e.appendChild(z);
		z.appendChild( L567.get(key).saveAsXML(xmldoc));
	    }
	}
	return e;
    }

    /** How many priors are stored? */
    int objectCnt() {
	return  (L_overall==null? 0: 1) + L567.size();
    }

    /** Print all stored priors */
    public String toString() {
	String s= "{ overall: " +  L_overall + "\n";
	for(Map.Entry<CFKey, Prior> en: 	 L567.entrySet() ) {
	    s += en.getKey() + ": " + en.getValue() + "\n";
	}
	return s +"\n";
  	
    }


}

