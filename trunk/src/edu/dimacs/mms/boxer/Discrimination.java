package edu.dimacs.mms.boxer;

import java.util.Vector;
// for XML generation
import org.w3c.dom.*;

/** A discrimination is a list of classes into which a classifier may
    partition the universe of examples (e.g., documents). Besides the
    list of classes (the class set) the discrimination is
    characterized by a few other properties. Those include:

    <ul>

    <li>Discirmination class structure type ({@link Suite.DCS DCS}),
    which controls how classes can be added to this discrimination (if
    at all) 

    <li> the default class

    <li>and the leftovers class.
    </ul>

 */

public class Discrimination /* extends DiscriminationFallback */{

    /** Discrimination class structure (DCS) */
    Suite.DCS dcs = Suite.DCS.Bounded;

    /** A Cla instance describes one class of a Discrimination (a
	polytomous [multi-class] classification of examples). A Cla
	instance only exists in the context of an enclosing
	Discrimination; therefore, it's not a static class.
     */
    public class Cla {
	/** Human-readable symbolic name of this class. It will occur in 
	    input files. */
	String name;

	/** A zero-based index of the position of this class entry in
	    its Discrimination's vector of "recognized" classes.  In a
	    provisional class, we store -1 for this value, because
	    provisional classes are not entered into the
	    discrimination's vector of classes. */
	private int pos;
	/** Position of this class within the discrimination's class list
	 */
	public int getPos() { return pos; }

	private Cla(String s,  int i) {
	    name =s;
	    pos = i;
	    if (i<0) throw new IllegalArgumentException("No prov classes anymore: can't create " + this + " with id=" +pos);
	}

	/** There is no need to compare names, as the (dis, pos) pairs
	    uniquely identifies the class. 
	*/
	public boolean equals(Object obj) {
	    if (!(obj instanceof Cla)) return false;
	    Cla c = (Cla)obj;
	    return c.getDisc() == getDisc() && c.pos == pos;
	}

	
	/** The hash code is based on the position of this class
	    within the discrimination (meant to be permanent, since
	    classes are never deleted) but not the class name
	    (which may change, in the special case of "pre-reserved"
	    classes)
	*/
	public int hashCode() {
	    return (Discrimination.this.hashCode() << 8) + pos;
	}

	/** An easily human readable fully qualified name of the class  */
	public String toString() {
	    return Discrimination.this.name + ":" + name;
	}

	/** Get the discrimination id */
	//public int getDid() {
	//    return Discrimination.this.getDid();
	//}

	/** Back pointer to the Discrimination this class is a part of */
	public Discrimination getDisc() { return Discrimination.this; }
	
	/** Makes this class the default class in its disrimination.
	    Replaces any default class that may have been set
	    previously */
	public void makeDefault() {
	    getDisc().setDefaultClass( this);
	}

	/** Is this the default class of the discrimination of which
	 * it is a part? */
	public boolean isDefault() {
	    return (this == getDisc().defaultCla);
	}

	/** Makes this class the leftover class in its disrimination.
	    Replaces any leftover class that may have been set
	    previously */
	public void makeLeftovers() {
	    getDisc().setLeftoversClass( this);
	}

	static final String prrPrefix =  "@UNNAMED_";

	/** Returns true if this is one of the still-unnamed classes
	  (as used in DCS2).  */
	boolean isPreReserved() {
	    return name.startsWith(prrPrefix);
	}

	/** Returns the name of the class (not including the name of
	    the discrimination) */
	public String getName() { return name; }

	synchronized void setName( String s) {
	    if ( isPreReserved()) {
		name = s;
	    } else {
		throw new IllegalArgumentException("Can't change class name from '"+name+"' to '"+s+"'. Class names must not be changed, unless it's a preserved class");
	    }
	}

    }

    /** Generates a name for a "pre-reserved"  class */
    static String makePreReservedName(int i) {
	return Cla.prrPrefix  + i;
    }

    /** Refers to the suite in whose context the discrimination has
     * been created. It's mostly useful to be able to access the
     * fallback discrimination structure */
    private Suite suite;
	
    /** The human-readable name of this discrimination. It will occur in input
     * files */
    String name;
    /** Returns the human-readable name of the Discrimination */
    public String getName() { return name; }

    /** The name of the TREC-style QREL file from which labels for
	this discrimination need to be extracted. This variable is
	not used by boxer itself, but only by the auxiliary data
	conversion utility, borj.rcv.QrelToXML. */
    private String qrel;

    /** The name of the TREC-style QREL file from which labels for
	this discrimination need to be extracted. This variable is
	not used by boxer itself, but only by the auxiliary data
	conversion utility, borj.rcv.QrelToXML. */
    public String getQrelFileName() { return qrel; }

    /** Only used by borj.rcv.QrelToXML */
    public void setQrelFileName(String _qrel) { qrel=_qrel; }
       
    /** The classes of this discrimnation, in order. Internally,
	classes of this discrimination can be identified by 0-based
	integer indexes into this array. (This can be done safely
	because no elements are ever removed from this vector; thus, a
	class' position stays constant). Using Vector is less
	efficient than a hash map for lookups, but it's probably not a
	big a deal as we usually won't have too many classes in a
	discrimination.
    */
    Vector<Cla> classes = null; // new  Vector<Cla>();    

    Vector<Cla> getAllClasses() { 
	return classes==null? new  Vector<Cla>():classes; 
    }

    /** If set, this class will be assigned to training and test
     * examples for which no labels have been found in the input
     * file */
    Cla defaultCla = null;
    
    /** If set, class labels (in XML dataset files) referring to
     * unknown classes will be converted to this class.  */
    Cla leftoversCla = null;

    /** Marks this discrimination as one whose classes can be described
	by "simple labels" in XML input */
    //private Suite.SupportsSimpleLabels simpleNameSituation = Suite.SupportsSimpleLabels.No;

    /*
    boolean isSituation1() {
	return (simpleNameSituation==Suite.SupportsSimpleLabels.MultipleBinary);
    }
    void markAsSituation1() {
	simpleNameSituation  =  Suite.SupportsSimpleLabels.MultipleBinary;
    }
    boolean isSituation2() {
	return (simpleNameSituation == Suite.SupportsSimpleLabels.Polytomous);
    }
    void markAsSituation2() {
	simpleNameSituation  = Suite.SupportsSimpleLabels.Polytomous;
    }
    */

    /** How many classes are there in this discrimination? In the case
      of DCS2, the number also includes still unnamed classes (i.e.,
      it is the MaxNumberOfClasses). If the class set is not defined yet,
      0 is returned.
     */
    public int claCount() { return classSetDefined()? classes.size() : 0; }
    /** Has the class set been defined? */
    public boolean classSetDefined() { return classes!=null; }

    /** If the class list is still undefined, defines it as an empty
     * set; otherwise, no effect */
    void ensureClassSetDefined() { 
	if (!classSetDefined()) classes = new Vector<Cla>(); 
    }

    /** Retrieves the recognized class for the specified name, if one exists 
	already. 

	FIXME: linear search isn't efficient for a large set - but we
	don't want to bother with a hash table yet, due to class name
	changes in DCS2

	@return A Cla object, or null (if no class with this name
	exists in this discrimination)
    */
    synchronized public Cla getCla(String cname) { 
	// Can we find a recognized class?
	if (!classSetDefined()) return null;
	for(Cla c: classes) {
	    if (!c.isPreReserved() &&  c.name.equals(cname)) return c;
	}
	return null;
    }

    /** Retrieves the i-th class of this discrimination */
    public synchronized Cla getClaById(int i) {
	return classes.elementAt(i);
    }


    /** Finds the first pre-reserved class of this discrimination
      (i.e., one that has been created, but not assigned any.
      @return The first pre-reserved class found, or null if there aren't any.
     */
    synchronized Cla findPreReservedClass() {
	for(Cla c: classes) { 
	    if (c.isPreReserved()) return c;
	}
	return null;
    }

   /** Creates a new class (or "appropriates" a formerly unnamed
    class) in the existing Discrimination in response to an API
    call. 
    
    Calling this method is equivalent to addClass(cname, false).

    @param cname The class name. No class with such name should exist in this
    discrimination yet.

    @return A class object (a new one, or a formerly unnamed class)
    @throws IllegalArgumentException If a class with the specified
    name already exists in the discrimination, or if a new class
    cannot be added
    */
    synchronized public Cla addClass(String cname/*, Suite.NC ncMode*/) { 
	return addClass(cname, false, Suite.NC.API); 
    }

    
  /** The main API call for creating a new class (or "appropriating" a
      formerly unnamed class) in the existing Discrimination.  If a
      class with the specified name already exists, the method either
      returns this class or errors out, depending on the "reuse" parameter

      <p align=center>"Add class" call disposition in various situations:</p>

      <table border=1>
      <tr><th align=left>Discrimination Class Structure<th>APINC/OD: addClass(NC.API)<th>TNC/OD (getClaAlways)</tr>
      <tr><th align=left>DCS0 (Uncommitted)
          <td colspan=2>Add
      <tr><th align=left>DCS1 (Fixed)
          <td rowspan=2>Error
          <td rowspan=2>Based on TrainingExampleNewClassOldDiscriminationMode, Error/Ignore/Use LeftoversClass
      <tr><th align=left>DCS2 (Bounded), with no unnamed classes left
      <tr><th align=left>DCS2 (Bounded), with some unnamed classes left
          <td colspan=2>Appropriate an unnamed class
      <tr><th align=left>DCS3 (Unbounded)
          <td colspan=2>Add
       </table>

      @param cname The class name. If reuse=false, no class with such
      name should exist in this discrimination yet.
      
      @param reuse If true, the method is allowed to simply return an already
      existing class with the matching name (instead of erroring out)

      @param ncMode How should we handle new classes?

      @return A class object (either already existing, or a newly
      created, or formerly unnamed class)
      @throws IllegalArgumentException If the class already exists, or
      if it cannot be added

  */
    synchronized public Cla addClass(String cname, boolean reuse, Suite.NC ncMode) { 
	if (cname==null || cname.equals("")) throw new IllegalArgumentException("Should not have empty-named classes! cname='"+cname+"'"); 

	// Does a class with this name already exist?
	Cla c0 = getCla(cname);
	
	// (Lewis's Case 3, APINC/OD)
	// redundant call 
	if (c0!=null) {
	    if (reuse) return c0;
	    else throw new IllegalArgumentException("Class "  + c0 + " already exists, and cannot be added. reuse=" + reuse);
	}

	// DCS0 or DCS3. They always allow additions
	if (!isCommitted() || dcs == Suite.DCS.Unbounded)  {
	    return addClassPhysically(cname);	    
	}

	Cla pc = findPreReservedClass();
	if (pc != null) {
	    if (dcs != Suite.DCS.Bounded) throw new AssertionError("How did I end up with pre-reserved classes and DCS=" + dcs + "!?");
	    // DCS2: change the name of the pre-reserved class
	    pc.setName(cname);
	    return pc;
	} 
	  
	// DCS1, or DCS2 with no unused classes left

	// FIXME: leftover class overrides ERROR/REJECT (VM's
	// proposal, 2009-05-03, but DL's paper wants it the other way.
	if (ncMode != Suite.NC.API && leftoversCla != null) return leftoversCla;

	// TNC/OD, ignore mode
	if (ncMode == Suite.NC.IGNORE) return null;
	
	if (ncMode == Suite.NC.ERROR || ncMode == Suite.NC.API) {
	    
	    String msg = "Cannot add new class "  + cname + " to discrimination " + name; 
	    msg +=  (ncMode == Suite.NC.API) ?  " using an API Call." :
		" from training data.";	    
	    msg += " Reason: discrimination has ClassStructure="+dcs;
	    if (ncMode != Suite.NC.API) msg += ", no leftovers class";
	    msg += (dcs==Suite.DCS.Fixed)?".":", and no pre-reserved classes.";

	    throw new IllegalArgumentException(msg);
	} else {
	    throw new AssertionError("Unexpected NC mode: " + ncMode);
	}
    }

    /** This method is invoked on each label in the test set, to find
     * the matching class, if any */
    synchronized public Cla getClassForTestSet(String cname, Suite.NC ncMode) { 
	if (cname==null || cname.equals("")) throw new IllegalArgumentException("Should not have empty-named classes! cname='"+cname+"'"); 

	// Can we find a recognized class?
	Cla c0 = getCla(cname);
	
	if (c0!=null) return c0;

	// FIXME: leftover class overrides ERROR/REJECT (VM's
	// proposal, 2009-05-03, but DL's paper wants it the other way.
	if (leftoversCla != null) return leftoversCla;

	// TNC/OD, ignore mode
	if (ncMode == Suite.NC.IGNORE) return null;
	else if (ncMode == Suite.NC.ERROR) {
	    
	    String msg = "Cannot add new class "  + cname + " to discrimination " + name + " during processing test set data.";
	    if (ncMode != Suite.NC.API) msg += " Discrimination has no leftovers class.";
	    throw new IllegalArgumentException(msg);
	} else {
	    throw new AssertionError("Unexpected NC mode for test set: " + ncMode);
	}
    }

    /** Just "adds", without no further checking (which should be done
     * by caller), and records it in the local class table
     */
    synchronized private Cla addClassPhysically(String cname) { 
	ensureClassSetDefined();
        Cla c = new Cla(cname, classes.size());
        classes.add(c);
        return c;
    }


   /** Retrieves the existing class with the specified name in this
    Discrimination; and if there isn't one, creates it, if possible. Using this
    method ensures that we don't wastefully create multiple Cla
    instances referring to the same class.
    @param ncMode How should we handle new classes?
    @return A class object, or null (in the ignore mode)
    @throws IllegalArgumentException If the class does not exist, and is not supposed to be added. 
    */
	/*
    public synchronized Cla getClaAlways(String cname, Suite.NC ncMode) { 
	return addClass(cname, true, ncMode);
	if (cname==null || cname.equals("")) throw new IllegalArgumentException("Should not have empty-named classes! cname='"+cname+"'"); 
	
	// Can we find a recognized class?
	Cla c0 = getCla(cname);
	if (c0!=null)  return c0;

	if (ncMode ==  Suite.NC.ERROR) {
	    throw new IllegalArgumentException("Required to report error upon encountering an unrecognized class name: `"+cname+"'");
	} else if (ncMode == Suite.NC.REPLACE) {
	    if (leftoversCla != null) return leftoversCla;
	    else  throw new IllegalArgumentException("ncMode=replace, but there is no leftover class to replace an unrecognized class name: `"+cname+"'");
	} else if (ncMode == Suite.NC.ADD) {
	    return addClassPhysically(cname);
	} else if (ncMode == Suite.NC.IGNORE) {
	    return null;
	} else {
	    throw new IllegalArgumentException("NC mode = " + ncMode + " is not supported");
	}
    }
	*/

    /** Sets the default class for this discrimination.
     @param cname The name of the default class. It may be an already existing 
    class; otherwise, if this is a not-yet-commited discrimination, the new 
    class with this name will be created. */
    public void setDefaultClass(String cname) {
	setDefaultClass( addClass(cname, true, Suite.NC.API));
    }

    public void setDefaultClass(Cla c) {
	if (c.getDisc() != this) throw new IllegalArgumentException("Trying to set the default class of Discrimination " + name + " to an alien class " + c);
	if (defaultCla != null) Logging.warning("Replacing default class " + defaultCla + " with " + c);
	defaultCla  = c;
    }

    /** Sets the leftover class for this discrimination.
     @param cname The name of the leftover class. It may be an already existing 
    class; otherwise, if this is a not-yet-commited discrimination, the new 
    class with this name will be created.  */
    public void setLeftoversClass(String cname) {
	setLeftoversClass( addClass(cname, true, Suite.NC.API));
    }

    public void setLeftoversClass(Cla c) {
	if (c.getDisc() != this) throw new IllegalArgumentException("Trying to set the leftover class of Discrimination " + name + " to an alien class " + c);
	if (leftoversCla != null) Logging.warning("Replacing leftover class " + leftoversCla + " with " + c);
	leftoversCla  = c;
    }

    /** Returns the default class for this discrimination. 
	@return The default class, or null if none has been set. */
    public Cla getDefaultCla() {
	return defaultCla;
    }

    /** Returns the leftover class for this discrimination. 
	@return The leftover class, or null if none has been set. */
    public Cla getLeftoversCla() {
	return leftoversCla;
    }

    public String describe() {
       StringBuffer b=new StringBuffer("Discrimination '"+name+"', DCS="+dcs+" contains " + classes.size() + " classes: {");
       for(Cla c: classes)   b.append(" [" + c.name + "]");
       b.append("};" +
		(defaultCla==null? " no default;": " default=" + defaultCla.name) +
		(leftoversCla==null? " no leftovers;": " leftovers=" + leftoversCla.name) );

       return b.toString();
    }

    /** Retrieves the discrimination name attribute from the XML element
     */
    static String getDiscNameAttr(Element e) {
	String disName =  e.getAttribute(ParseXML.ATTR.NAME_ATTR);
	if (!XMLUtil.nonempty(disName)) throw new IllegalArgumentException("No name found in the XML 'discrimination' element");
	return disName.trim();
    }

    /** Does this <tt>discrimination</tt> element carry a
      <tt>fallback="true"</tt> attribute?
     */
    static boolean isFallback(Element e) {
	String s =  e.getAttribute(ParseXML.ATTR.DISCR.FALLBACK);
	return XMLUtil.nonempty(s) && Boolean.parseBoolean(s);
    }


    /** This constructor should not be called other than from
	Suite.createDisc(), or from other constructors of this class. */
    Discrimination(Suite _suite, String _name) {
	Logging.info("Creating discrimination " + _name);
	suite = _suite;
	name = _name;
	dcs = Suite.DCS.Uncommitted; // FIXME
    }
    
    /** The main discrimination constructor, it parses a
      <tt>discrimination</tt> XML element, creating a new
      discrimination and all its classes. This constructor should only
      be called from inside {@link boxer.Suite#addDiscrimination()},
      which will then validate the name etc and record the
      discrimination and its classes in the Suite's tables.

      When a particular property of the discrimination is not
      specified in the XML element, it will be filled in from the the
      suite's fallback discrimination, unless this discrimination
      itself is the fallback (this is marked by the
      <tt>fallback="true"</tt> attribute in the <tt>discrimination</tt>
      element), in which case the missing properties are initialized
      from sysdefaults.

      @param e The element describing the new discrimination. The new
      discrimination's name should be different from those of all
      already existing discriminations.

    */
    Discrimination(Suite _suite, Element e) {
	this(_suite, getDiscNameAttr(e));
	Logging.info("Getting the rest of data for discrimination " + name + " from XML");

	classes = null; // meaning, "undefined";

	// start with Uncommitted state
	dcs = Suite.DCS.Uncommitted;

	// Add classes
	for(Node n = e.getFirstChild(); n!=null; n = n.getNextSibling()) {
	    if ( n.getNodeType() == Node.ELEMENT_NODE &&
		 n.getNodeName().equals(XMLUtil.CLASSES)) {
		ensureClassSetDefined(); // empty set
		for(Node x=n.getFirstChild(); x!=null; x=x.getNextSibling()) {
		    if (x.getNodeType() == Node.TEXT_NODE) {
			// Parsing the text of the node
			String text = x.getNodeValue().trim();
			for(String cname: text.split("\\s+")) {
			    addClass(cname);
			}
		    }
		}
	    }
	}

	String s;

	boolean isFallback = isFallback(e);
	if (isFallback) {
	    // This discr has been declared the fallback
	    // discrimination of its suite
	    Logging.info("Discrimination " + name+ "; found " + ParseXML.ATTR.DISCR.FALLBACK  + "='" + isFallback + "'");
	}

	s =  e.getAttribute(ParseXML.ATTR.DISCR.DEFAULT_CLASS);
	if (XMLUtil.nonempty(s)) {
	    Logging.info("Discrimination " + name+ "; found " + ParseXML.ATTR.DISCR.DEFAULT_CLASS  + "='" + s + "'");
	    setDefaultClass(s);
	}

	s =  e.getAttribute(ParseXML.ATTR.DISCR.LEFTOVERS_CLASS);
	if (XMLUtil.nonempty(s)) {
	    Logging.info("Discrimination " + name+ "; found " + ParseXML.ATTR.DISCR.LEFTOVERS_CLASS  + "='" + s + "'");
	    setLeftoversClass(s);
	}

	// Set structure (which usually means committing), if supplied in XML
	s = e.getAttribute(ParseXML.ATTR.DISCR.CLASS_STRUCTURE);
	if (XMLUtil.nonempty(s)) {
	    dcs = Suite.DCS.valueOf(s);
	    Logging.info("Set DCS of discrimination "+name+" to " + dcs);
	} else {
	    dcs = isFallback?  Suite.SysDefaults.dcs : suite.fallback.dcs;
	    Logging.info("Set DCS of discrimination "+name+" to the fallback value " + dcs);
	}

	// Set max class size, if supplied and appropriate
	int reqSize = -1; // negative value will mean, "not specified"
	s =  e.getAttribute(ParseXML.ATTR.DISCR.CLASS_COUNT);
	if (XMLUtil.nonempty(s)) {
	    reqSize = Integer.parseInt(s);
	    bumpUpSize(reqSize); // includes DCS validation
	}

	/*
	String committedString =  e.getAttribute(ParseXML.ATTR.DISCR.COMMITTED);
	if (XMLUtil.nonempty(committedString)) {
	    committed =  (new Boolean(committedString)).booleanValue();
	    System.out.println("Committed flag for " + name+ " set to " + committed);
	    if (!committed && reqSize > 0) {
		// No point in setting discrimination size (i.e. declaring DCS2 already) without committing
	       	throw new IllegalArgumentException("XML description of discrimination " + name +" sets reqSize=" + reqSize + " while declaring discrimination uncommitted");
	    }
	} else {
	    committed = true;
	} 
	*/

	qrel =  e.getAttribute(ParseXML.ATTR.DISCR.QREL);
	if (XMLUtil.nonempty(qrel)) {
	    Logging.info("Discrimination " + name+ "; found " + ParseXML.ATTR.DISCR.QREL  + "='" + qrel + "'");
	} else {
	    qrel = null;
	}

    }

    /** Bumps up this discrimination's size by adding "pre-reserved"
     (unnamed) classes, if necessary. Used from the constructors and
     the commit calls.

     @param reqSize The size to increase the class list to. Must be a
     positive numver, no smaller than the number of classes already in
     the list.

     @throws IllegalArgumentException If the DCS is other than DCS2
     ("Bounded"), or if the specified size is non-positive (or smaller than the 
     number of the already stored classes).
    */
    synchronized private void bumpUpSize(int reqSize) {
	int actualSize = claCount();
	if (dcs != Suite.DCS.Bounded) {
	    throw new IllegalArgumentException(ParseXML.ATTR.DISCR.CLASS_COUNT + " should not be specified for discrimination '"+name+"', because its DCS is " + dcs);
	} else if (reqSize <= 0) {
	    throw new IllegalArgumentException("Description of discrimination " + name +", contains invalid value of " + ParseXML.ATTR.DISCR.CLASS_COUNT + "=" + reqSize + ". It should be positive");
	} else if (claCount() > reqSize) {
	    throw new IllegalArgumentException("Cannot set the max class size of discrimination " + name +" because it already has a greater number ("+claCount()+") of classes");
	} else if (claCount() < reqSize) {
	    Logging.info("Allocating 'unnamed' classes to bump the size of discrimination "+name+" from " + actualSize + " to " + reqSize);
	    for(int i=actualSize; i<reqSize; i++) {
		// has to make a "physical" call, to avoid reusal
		addClassPhysically( makePreReservedName(i));
	    }
	}
    }
    
    /** Describe the list of recognized classes as an element of an XML document */
    org.w3c.dom.Element createElement(Document xmldoc) {
	Element e = xmldoc.createElement(XMLUtil.DISCRIMINATION);
	e.setAttribute(ParseXML.ATTR.NAME_ATTR, name);
	if (defaultCla != null) {
	    e.setAttribute(ParseXML.ATTR.DISCR.DEFAULT_CLASS, defaultCla.name);
	}
	if (leftoversCla != null) {
	    e.setAttribute(ParseXML.ATTR.DISCR.LEFTOVERS_CLASS,leftoversCla.name);
	}
	
	Element ce = xmldoc.createElement(XMLUtil.CLASSES);
	StringBuffer b=new StringBuffer();
	for(int i=0; i<classes.size(); i++) {
	    if (i>0) b.append(" ");
	    b.append(classes.elementAt(i).name);
	}
	Text textNode = xmldoc.createTextNode(b.toString());
	ce.appendChild(textNode);
	e.appendChild(ce);
	return e;
    }

    /** Marks this discrimination as a "committed"- that is, more or
      less "fixes" its structure (as DCS1 or DCS3). From this point
      on, adding new classes to the discrimination becomes impossible
      if it's DCS1.

      This call does not specify maxNumberOfClasses, so it can only be
      used for DCS1 or DCS3 commitments (not DCS2).

      @param _dcs The DCS to impose on the discrimination. Null means default.

    */
    public void commitStructure(Suite.DCS _dcs) {
	commitStructure(_dcs, -1);
    }

  /** Marks this discrimination as "committed" - that is, more or less
      "fixes" its structure. From this point on, adding new classes to
      the discrimination becomes either impossible, or only possible
      in limited circumstances (DCS2 with available unnamed classes,
      or DCS3)

      @param _dcs The DCS to impose on the discrimination. Null means default.

      @param  _maxNumberOfClasses An appropriate positive number must be supplied for DCS2; the value of -1 (meaning, "NONE") should be entered otherwise

     @throws IllegalArgumentException If commitment with the iven
     params is impossible (e.g., if no _maxNumberOfClasses, or too
     small _maxNumberOfClasses is supplied with DCS2, or if a positive
     _maxNumberOfClasses is supplied with DCS1/3.

    */
    public void commitStructure(Suite.DCS _dcs, int _maxNumberOfClasses) {
	if (dcs != Suite.DCS.Uncommitted) {
	    // or maybe it's better to have no effect here...
	    throw new IllegalArgumentException("Discrimination "+name+" is already committed!");
	} 

	if (_dcs==null) _dcs = suite.fallback.dcs;
	Logging.info("Committing discrimination '"+name+"' with DCS=" +_dcs);

	if (_dcs == null) {
	    throw new IllegalArgumentException("Can't commit discriminations, because neither the commit call nor the fallback specifies the structure");
	} 

	dcs = _dcs;

	if (!classSetDefined()) {
	    // go for fallback
	    Logging.info("Using fallback class set when committing discrimination '"+name+"'");
	    Vector<Cla> v  = suite.fallback.getAllClasses();
	    if (v==null) throw new AssertionError("Fallback discr should have had its classes defined!");
	    for(Cla c: v) {
		if (c.name.indexOf('@')>=0) {
		    // It is fine to have metachar, but only if we're
		    // in the all-binary mode!
		    throw new  IllegalArgumentException("Cannot use fallback class set in the committment of discrimination " + name + ", because the fallback class name '" + c.name + "' contains special character");
		}
		addClass(c.name);
	    }
	    Cla c = suite.fallback.getDefaultCla();
	    if (c!=null) setDefaultClass(c.name);
	    c = suite.fallback.getLeftoversCla();
	    if (c!=null) setLeftoversClass(c.name);
	}

	if (dcs == Suite.DCS.Bounded && claCount()==0) {
	    throw new  IllegalArgumentException("Discrimination " + name + " cannot be committed as " + dcs + " because it has 0 classes");
	}

	if (dcs == Suite.DCS.Bounded) {
	    int maxC = _maxNumberOfClasses;
	    // if not set, try to use the fallback (only if the
	    // fallback is DCS2; otherwise, an exception will be thrown)
	    if (maxC < 0) maxC = suite.fallback.getDCS2MaxNumberOfClasses();
	    // validate size and allocate unnamed classes if appropriate
	    bumpUpSize(_maxNumberOfClasses);
	} else if ( _maxNumberOfClasses >= 0) {
	    throw new IllegalArgumentException("MaxNumberOfClasses should not be supplied when commiting as DCS=" + dcs);
	} 


    }

    /* Gets the discrimination's MaxNumberOfClasses if its DCS2, or
     * throws exception otherwise. Used for accessing fallback's
     * MaxNumberOfClasses as discussed on 2009-05-15.
     */
    private int getDCS2MaxNumberOfClasses() {
	if (dcs == Suite.DCS.Bounded) return claCount();
	else throw new IllegalArgumentException("The discrimination " + name + " has DCS=" + dcs+", rather than " +  Suite.DCS.Bounded +", and MaxNumberOfClasses makes no sense in it");
    }

    public void ensureCommitted() {
	if (dcs == Suite.DCS.Uncommitted) {
	    commitStructure(Suite.DCS.Fixed); // FIXME
	}
    }

   /** This flag is initially false, and set to true once a
     * LearningModel for this Discrimination modifies its internal
     * state. The main use of this flag is that we can easily add classes
     * to the discrimination that has not been used for training yet.
     */
    public boolean isCommitted() { return dcs != Suite.DCS.Uncommitted; }


   /** Returns an array of booleans, aligned with suite.id2cla, in
     * which the elements corresponding to the listed classes are
     * set.
     */
    boolean [] getY( Vector<Discrimination.Cla> classes) {
	int r= claCount();
	boolean b[] = new boolean[r];
	for( Discrimination.Cla c: classes) {
	    b[ c.getPos()] =  (c.getDisc() == this); 
	}
	return b;
    }


    public String toString() {
	return name;
    }

    static final String NOT_PREFIX = "NOT-";
    
    static String notName(String s) { return  NOT_PREFIX + s; }
    static String notName2name(String s) { 
	return s.startsWith(NOT_PREFIX) ? s.substring( NOT_PREFIX.length()) :
	    null;
    }

    /** Is this a suitable fallback discrimination for Situation 1, as per
	boxer-make-common-case-trivial-20090430.pdf ?
     */
    boolean isSimpleBinaryFallback() {
	return isSimpleBinary(Suite.NOT_DIS_NAME, Suite.DIS_NAME);
    }

    /** Is this a suitable binary discrimination for Situation 1, as per
	boxer-make-common-case-trivial-20090430.pdf ?
     */
    boolean isSimpleBinary() {
	return isSimpleBinary(notName(name), name);
    }

    /** Is this is a binary discrimination with these two particular
	class names?
	@param defName the name of the default class
	@param otherName the name of the other (non-default) class
     */
    private boolean isSimpleBinary(String defName, String otherName) {
	return
	    (dcs == Suite.DCS.Fixed || dcs == Suite.DCS.Bounded) &&
	    claCount()==2 &&
	    getDefaultCla() != null &&
	    getCla(otherName)!=null &&
	    getCla(defName)==getDefaultCla() &&
	    getLeftoversCla() == null; // leftovers not allowed, as per DL (2009-05-12 msg)
    }
    

}