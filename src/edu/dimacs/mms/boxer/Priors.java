package edu.dimacs.mms.boxer;

import java.util.*;
import java.io.*;

import org.apache.xerces.dom.DocumentImpl;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

/** A Priors object is used to store individual priors for a
    particular discrimination. An single prior is represented as 
    a (m,lambda) pair.

    <p>The feature name "@dummy" ({@link
    FeatureDictionary#DUMMY_LABEL}) is used for the intercept feature.


*/
public class Priors {
    /** The suite for which these priors are */
    private Suite suite;
    /** Used to look up feature IDs during initialization */
    //private FeatureDictionary dic;

    final String name;

    /** L0: The built-in default. It will be used to initialize L1, if
     * not provided in the input XML file. */
    static Prior L0prior = Prior.makeL0();
    /** Retrieves L1, the universal user-defined default.  */
    Prior getL1() { return crossDisc.L_overall; }
    void setL1(Prior p) { crossDisc.L_overall=p; }

    /** Cross-discrimination priors */
    CrossDiscPriorSet crossDisc = new     CrossDiscPriorSet();

    /** This flag will be set to true once L1 has been used at least once;
	after that, the value must not be changed */
    private boolean usedBase=false;

    /* L4 thru L7 are here, split by discrimination. 

       <p>The discrimination name (a String) is used as the hash map
        key, rather than the numberic discrimination ID, because the
        latter is not guaranteed to be constant (and we don't want to
        bother modifying this data structure whenever a discrimination
        gets deleted. */
    HashMap<String,DiscPriorSet> discPriors = new HashMap<String,DiscPriorSet>();

    /** Stores a prior for a particular discrimination (or
      a cross-discrimination one)
      
      @param dis If null is given, store the cross-discrimination
      prior; otherwised, store the discrimination-specific prior
      @param key Encodes the class id (within the discrimination) and/or the feature id
     */ 
    void storePrior(Discrimination dis, 
		  CFKey key, 		  //int fid
		  Prior p		  ) {	

	if (dis==null) {
	    //System.out.println("store CD(" + key + ", " + p+")");
	    crossDisc.put(key, p);
	} else {
	    DiscPriorSet dps =  discPriors.get( dis.getName());
	    if (dps==null) {
		discPriors.put(dis.getName(), dps=new DiscPriorSet());
	    }
	    //System.out.println("store prior for dis="+dis+", (" + key + ", " + p+")");
	    dps.put(key, p);
	}
    }

    void setL4(Discrimination dis, Prior p) { 
	DiscPriorSet dps =  discPriors.get( dis.getName());
	if (dps==null) {
	    discPriors.put(dis.getName(), dps=new DiscPriorSet());
	}
	dps.L_overall=p; 
    }


    /** Looks up the applicable prior for the specified
	(disc.class,feature) ID combination.
	@param c An actual class (not null)
	@param fid An actual (non-negative) feature id
     */
    public Prior get(Discrimination.Cla c, int fid) {	
	// First, check discrimination-specific priors (L4...L7)
	DiscPriorSet dps =  discPriors.get( c.getDisc().getName());
	if (dps!=null) {
	    Prior p = dps.get(c,fid);
	    //System.out.println("dps["+c.getDisc().getName()+"].get("+c+", "+fid+")=" + p);
	    if (p!=null) return p;
	}

	// If none applies, then use cross-discrimination priors
	return crossDisc.get(c.getName(), fid);
    }

    /** XML element names */
    static class NODE {
	final static String
	    PRIORS="priors", PRIOR="prior",
	    CROSS_DISCRIMINATION="cross-discrimination",
	    DISCRIMINATION_SPECIFIC="discrimination-specific",
	    DISCRIMINATION="discrimination",
	    OVERALL="overall",
	    FEATURES="features",
	    CLASSES="classes",
	    COEFFICIENTS="coefficients";
	final static String
	    FEATURE_PRIOR="feature-prior",
	    CLASS_PRIOR="class-prior",
	    COEFF_PRIOR="coeff-prior";
    }
    static class ATTR {
	final static String NAME="name";
    }


    /** Creates a priors object based  on the XML description

<priors name=...>

<cross-discrimination>
<overall> </overall>    
<features> </features>
<classes> </classes>
</cross-discrimination>

<discrimination-specific>

<discrimination>
<overall> </overall>
<features> </features>
<classes> </classes>
<coefficients> </coefficients>
</discrimination>

:

<discrimination>
<overall> </overall>
<features> </features>
<classes> </classes>
<coefficients> </coefficients>
</discrimination>

</discrimination-specific>
</priors> 


Inside each element:

<prior type="l|g" mode=... variance=... skew=... absolute=... 
   [discrimination="..."] [class="..." ] [feature="..."]
>

Or like this:

<discrimnation name="...">
<overall><prior ...><overall>
<features>
  <feature-prior feature="..."><prior .../></feature-prior>
   ...
</features>
<classes>
  <class-prior class="..."><prior .../></class-prior>
  ...
</classes>
<coefficients> 
   <coefficient-prior feature="..." class="..."><prior .../></coefficient-prior>
</coefficients>
</discrimination>

     */
    public Priors(Element e, Suite _suite) throws BoxerXMLException {
	suite = _suite;
	//dic = suite.getDic();
	XMLUtil.assertName(e, NODE.PRIORS);
	name =  e.getAttribute(ParseXML.ATTR.NAME_ATTR);
	if (!XMLUtil.nonempty(name)) throw new BoxerXMLException("No name found in the XML '"+NODE.PRIORS+"' element");

	boolean foundCD=false, foundDS=false;

	for(Node n = e.getFirstChild(); n!=null; n = n.getNextSibling()) {
	    if (XMLUtil.isIgnorable(n)) continue;
	    if ( XMLUtil.isNamedElement(n,NODE.CROSS_DISCRIMINATION)) {
		if (foundCD)  throw new BoxerXMLException("Duplicate "+NODE.CROSS_DISCRIMINATION+" element");
		foundCD = true;

		// <overall> </overall>
		// <features> </features>
		// <classes> </classes>

		setL1( L0prior);
		for(Node x=n.getFirstChild(); x!=null; x=x.getNextSibling()) {
		    if (XMLUtil.isIgnorable(x)) continue;
		    if ( XMLUtil.isNamedElement(x,NODE.OVERALL)) {
			if (usedBase) throw new BoxerXMLException("An L1 prior ("+NODE.CROSS_DISCRIMINATION + "/"+ NODE.OVERALL+" element) has been found too late, after we may have already used L0 in place of L1 for scaling");
			setL1( parseEnclosedPrior((Element)x, null));
		    } else if  ( XMLUtil.isNamedElement(x,NODE.FEATURES)) {
			parseFeatureSpecific((Element) x, null);
		    } else if  ( XMLUtil.isNamedElement(x,NODE.CLASSES)) {
			parseClassSpecific((Element)x, null);
		    } else {
			throw new BoxerXMLException("Unexpected child type: " + x + " on a " + NODE.CROSS_DISCRIMINATION + " element");
		    }
		}		
	    } else if ( XMLUtil.isNamedElement(n,NODE.DISCRIMINATION_SPECIFIC)) {
		if (foundDS)  throw new BoxerXMLException("Duplicate "+NODE.DISCRIMINATION_SPECIFIC+" element");
		foundDS = true;

		for(Node v=n.getFirstChild(); v!=null; v=v.getNextSibling()) {
		    if (XMLUtil.isIgnorable(v)) continue;
		    if ( XMLUtil.isNamedElement(v,NODE.DISCRIMINATION)) {
			String disName = XMLUtil.getAttributeOrException((Element)v, ATTR.NAME);
			Discrimination dis = suite.getDisc(disName);
			if (dis==null) {
			    throw new BoxerXMLException("There is no discrimination named " + disName);
			}
		// <overall> </overall>
		// <features> </features>
		// <classes> </classes>
		// <coefficients> </coefficients>

			
			for(Node x=v.getFirstChild(); x!=null; x=x.getNextSibling()) {
			    if (XMLUtil.isIgnorable(x)) continue;
			    if ( XMLUtil.isNamedElement(x,NODE.OVERALL)) {
				Prior p = parseEnclosedPriorOrNull((Element)x, getL1());
				if (p!=null) setL4(dis,p);
			    } else if  ( XMLUtil.isNamedElement(x,NODE.FEATURES)) {
				parseFeatureSpecific((Element) x, dis);
			    } else if  ( XMLUtil.isNamedElement(x,NODE.CLASSES)) {
				parseClassSpecific((Element) x, dis);
			    } else if  ( XMLUtil.isNamedElement(x,NODE.COEFFICIENTS)) {
				parseCoeffSpecific((Element) x, dis);
			    } else {
				throw new BoxerXMLException("Unexpected child type: " + x + " on a " + NODE.DISCRIMINATION_SPECIFIC + " element");
			    }			
			}		
		    } else {
			throw new BoxerXMLException("Unepected child type: " + v);
		    }
		}
	    }
	}
    }

    /** Just a convenience wrapper around {@link #Priors(Element,  FeatureDictionary )}

	@throws SAXException If the W3C DOM parser finds that the
     document isn't good XML
     */
    public Priors(File f, Suite suite) 
	throws IOException, SAXException, BoxerXMLException {
	this(ParseXML.readFileToElement(f), suite);
    }


    /** Parses a "features" element.

	<features> 
	<feature-prior> <prior .../>	</feature-prior> 
	...
	</features>	
    */
    void parseFeatureSpecific(Element e, Discrimination dis) throws BoxerXMLException  {
 	XMLUtil.assertName(e, NODE.FEATURES);	
	for(Node x=e.getFirstChild(); x!=null; x=x.getNextSibling()) {
	    if (XMLUtil.isIgnorable(x)) continue;
	    if ( XMLUtil.isNamedElement(x,NODE.FEATURE_PRIOR)) {
		String f =  XMLUtil.getAttributeOrException((Element)x, Prior.ATTR.FEATURE);
		int fid = suite.getDic().getIdAlways(f);
		CFKey key = new CFKey(null, fid);		
		storePrior(dis, key, parseEnclosedPrior((Element)x, getL1()));
	    } else {
		throw new BoxerXMLException("Invalid child type " + x + " in a " + NODE.FEATURES + " element");
	    }
	}
    }

    /** Parses a "classes" element.

	<classes> 
	<class-prior> <prior .../>	</class-prior> 
	...
	</class>	
    */
    void parseClassSpecific(Element e, Discrimination dis) throws BoxerXMLException  {
 	XMLUtil.assertName(e, NODE.CLASSES);	
	for(Node x=e.getFirstChild(); x!=null; x=x.getNextSibling()) {
	    if (XMLUtil.isIgnorable(x)) continue;
	    if ( XMLUtil.isNamedElement(x,NODE.CLASS_PRIOR)) {
		String cname =  XMLUtil.getAttributeOrException((Element)x, Prior.ATTR.CLASS);
		CFKey key;
		if (dis==null) {
		    // FIXME: special handling for L3 is needed
		    if (cname.equals(Suite.NOT_DIS_NAME)) {
			key = new CFKey(cname);
		    } else {
			throw new BoxerXMLException("Name " + cname + " is not allowed for L3 priors. The only allowed name is " + Suite.NOT_DIS_NAME);
		    }
		} else {
		    Discrimination.Cla c = dis.getCla(cname);
		    if (c==null) throw new BoxerXMLException("Discrimination " + dis + " does not have a class named " + cname + ", mentioned in the priors file");
		    key = new CFKey(c);		
		}
		storePrior(dis, key, parseEnclosedPrior((Element)x, getL1()));
	    } else {
		throw new BoxerXMLException("Invalid child type " + x + " in a " + NODE.CLASSES + " element");
	    }
	}
    }


    void parseCoeffSpecific(Element e, Discrimination dis) throws BoxerXMLException  {
 	XMLUtil.assertName(e, NODE.COEFFICIENTS);	
	if (dis==null) throw new IllegalArgumentException();

	for(Node x=e.getFirstChild(); x!=null; x=x.getNextSibling()) {
	    if (XMLUtil.isIgnorable(x)) continue;
	    if ( XMLUtil.isNamedElement(x,NODE.COEFF_PRIOR)) {
		String cname =  XMLUtil.getAttributeOrException((Element)x, Prior.ATTR.CLASS);
		Discrimination.Cla c = dis.getCla(cname);
		if (c==null) throw new BoxerXMLException("Discrimination " + dis + " does not have a class named " + cname + ", mentioned in the priors file");

		String f =  XMLUtil.getAttributeOrException((Element)x, Prior.ATTR.FEATURE);
		int fid = suite.getDic().getIdAlways(f);

		CFKey key = new CFKey(c, fid);		
		storePrior(dis, key, parseEnclosedPrior((Element)x, getL1()));
	    } else {
		throw new BoxerXMLException("Invalid child type " + x + " in a " + NODE.COEFFICIENTS + " element");
	    }
	}
    }


    /** Extracts a Prior from a "prior" element that is a child of the
	specified element. This is used when passing wrapper elements
	that enclose a "prior" element inside, while themselves indicate 
	the prior's applicability scope.

	@param e An XML Element which is expected to have one child, a "prior" element 

	@param base The L1 prior which will be used to scale the prior now being read if the latter is not absolute

	@throws BoxerXMLException If none, or more than one, child of
	the desired type has been found
    */
    private Prior parseEnclosedPrior(Element e, Prior base) throws BoxerXMLException {
	Prior p = parseEnclosedPriorOrNull(e, base);
	if (p==null) throw new  BoxerXMLException("Element " + e + " has no child of the type '"+NODE.PRIOR+"'");
	else return p;
    }

    /**
       Returns null if no child is found

	@throws BoxerXMLException If more than one child of the
	desired type has been found, or if a child node of a different
	type has been found
     */
    private Prior parseEnclosedPriorOrNull(Element e, Prior base) throws BoxerXMLException {
	Prior p = null;
	for(Node x=e.getFirstChild(); x!=null; x=x.getNextSibling()) {
	    if (XMLUtil.isIgnorable(x)) continue;
	    if ( XMLUtil.isNamedElement(x,NODE.PRIOR)) {
		if (p != null) throw new  BoxerXMLException("Element " + e + " has more than one children of the type '"+NODE.PRIOR+"'");
		p = Prior.parsePrior((Element)x, base);
		if (base != null) usedBase = true;
	    } else {
		throw new  BoxerXMLException("Element " + e + " has a child of a type different than '"+NODE.PRIOR+"'");
	    }
	}
	return p;
    }
    

    /** Saves the priors set into in an XML file.
     
      @param fname Name of the file  to write the XML to.
     */
    public void saveAsXML(String fname) {

	Document xmldoc= new DocumentImpl();
	Element e = saveAsXML(xmldoc);
	xmldoc.appendChild(e);
	XMLUtil.writeXML(xmldoc, fname);
    }

   /** Produces an XML document describing just the set of priors.

     @return An XML Element of the "priors" type
     */
    public Element saveAsXML(Document xmldoc) {
	
	Element root = xmldoc.createElement(NODE.PRIORS);
	root.setAttribute(ParseXML.ATTR.NAME_ATTR, name);
	root.setAttribute(ParseXML.ATTR.VERSION_ATTR, Version.version);	

	Element cd = xmldoc.createElement(NODE.CROSS_DISCRIMINATION);
	root.appendChild(cd);
	
	Element e;

	e = xmldoc.createElement(NODE.OVERALL);
	e.appendChild( getL1().saveAsXML(xmldoc));
	cd.appendChild(e);

	cd.appendChild( crossDisc.saveFeatureSpecificAsXML(xmldoc, suite.getDic()));
	cd.appendChild( crossDisc.saveClassSpecificAsXML(xmldoc, null));

	Element ds = xmldoc.createElement(NODE.DISCRIMINATION_SPECIFIC);
	root.appendChild(ds);
	for(int did =0; did < suite.disCnt(); did ++) {
	    Discrimination dis  = suite.getDisc( did);
	    DiscPriorSet dps =  discPriors.get(dis.getName());
	    if (dps==null) continue;

	    Element d = xmldoc.createElement(NODE.DISCRIMINATION);
	    d.setAttribute(ATTR.NAME, dis.getName());
	    ds.appendChild(d);

	    e = xmldoc.createElement(NODE.OVERALL);
	    if (dps.L_overall != null) {
		e.appendChild( dps.L_overall.saveAsXML(xmldoc));
	    }
	    d.appendChild(e);

	    d.appendChild(dps.saveFeatureSpecificAsXML(xmldoc,suite.getDic()));
	    d.appendChild(dps.saveClassSpecificAsXML(xmldoc, dis));
	    d.appendChild(dps.saveCoeffSpecificAsXML(xmldoc, suite.getDic(), dis));   
	}

	return root;
    }

    int objectCnt() {
	int cnt = crossDisc.objectCnt();
	for( DiscPriorSet q: 	 discPriors.values() ) cnt += q.objectCnt();
	return cnt;
    }

    String reportSize() {
	int cnt = crossDisc.objectCnt();
	String s = "Cross-disc: " + cnt + "\n";
	for(Map.Entry<String,DiscPriorSet> en: 	 discPriors.entrySet() ) {
	    int n = en.getValue().objectCnt();
	    s += en.getKey() +": " + n + "\n";
	    cnt += n;
	}
	return s;
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
