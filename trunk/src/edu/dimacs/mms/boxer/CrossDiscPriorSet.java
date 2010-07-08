package edu.dimacs.mms.boxer;

import java.util.*;


import org.apache.xerces.dom.DocumentImpl;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
//import org.w3c.dom.Node;
//import org.xml.sax.SAXException;


/** An auxiliary class for {@link Priors}: stores cross-discrimination
 * priors  */
class CrossDiscPriorSet extends DiscPriorSet {

    /** Looks up the applicable cross-discrimination prior (one of
	L1,L2,L3). This method should be invoked only if this is a
	cross-discrimination priors set.
     */
    Prior get(String cname, int fid) {

	//System.out.println("CDPS dump: " + this);

	// If none applies, then use cross-discrimination priors
	// L3, when appropriate
	Prior p = null;
	if (cname.equals(Suite.NOT_DIS_NAME)) {
	    p =  L567.get(new CFKey(cname));
	}
	if (p!=null) return p;
	// L2
	p = L567.get(new CFKey(null, fid));
	System.out.println("CDPS.get(key=" + new CFKey(null, fid) + ", gives p=" + p);

	if (p!=null) return p;
	return L_overall;
    }

}

