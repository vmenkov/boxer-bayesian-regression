package edu.dimacs.mms.boxer;

import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Vector;

import org.apache.xerces.dom.DocumentImpl;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

/** A Suite is a set of {@link edu.dimacs.mms.boxer.Discrimination
    Discriminations}. It also keeps track of the list of {@link
    edu.dimacs.mms.boxer.Learner Learners} using the Suite.

    <p> Most of BOXER operations are associated with a particular
    suite. A suite contains a reference to {@link
    edu.dimacs.mms.boxer.FeatureDictionary FeatureDictionary}, used to map internal
    feature IDs to feature names. Data points are parsed (converted
    from the external XML representation to the internal {@link
    edu.dimacs.mms.boxer.DataPoint} representation) in the context of a suite,
    possibly triggering creation of new discriminations/classes in the
    suite. Each learner is associated with a particular suite as well.

    <p>Typically, a BOXER application would only need one suite
    objects. However, one can create multiple suites - e.g., one used
    for actual learning, and another one, for data validation. Or one
    may even decide to create several indpendent learners, each one
    with its own group of learners associated with it. In that case,
    one has to be careful to make sure that DataPoints parsed in
    the context of one suite are only used with that suite.

    <p>Within a suite, the individual discriminations it contains can
    be accessed by their zero-based integer ID using the method {@link
    #getDisc(int did)}, or by name, using  {@link
    #getDisc(String disName)}

    <p>It is important to note that the numeric discrimination ID is
    not guaranteed to stay constant during a BOXER application
    run. That is, it's not really an "ID", but merely a sequential
    number of the given discrimination in the array of all
    dscriminations of the suite; and since discriminations can be
    deleted, the "IDs" of the remaining discriminations will change!
    On the other hand, the string "discrimination name" is both unique
    within the suite and is guaranteed to remain constant.

 */
public class Suite {

    /** The name of this suite (for reporting purposes) */
    final String name;
    /** Returns the name of this suite. The name is set at the suite's
     * creation, and never changes. */
    public String getName() { return name; }

    /** The feature dictionary associated with this suite */
    FeatureDictionary dic = new FeatureDictionary();

    /** The priors set associated with the suite. It should be set
	before any learners are created; it will affect all TG learners
	associated with this suite. */
    private Priors priors=null;

    /** Associates a set of priors with the suite. This method should
	be called before any learners are created on this suite. The priors will
	affect all {@link TruncatedGradient} learners that will be
	later created in this suite. Learners of other types (e.g. EG or kNN) won't be
	affected.

	@param p A Priors object; typically, it has been created with
	a Priors constructor using this suite.
    */
    public void setPriors(Priors p) { priors = p; }

    /** Returns the Priors object associated with this suite, or null
	if none is.
     */
    public Priors getPriors() { return priors; }

    /** All discriminations we've got in this suite, accessible by their name.
     */
    HashMap <String, Discrimination> discr = 
	new HashMap<String, Discrimination>();
    /** All discriminations ordered by numeric id. Note that elements
     * can be removed from this array, when discriminations are
     * deleted. This means that the discrimination ID may not stay
     * constant throughout a run. */
    Vector<Discrimination> did2discr = new  Vector<Discrimination>();

    /** Pointer to one of the discriminations in did2discr, which is
	used as the fallback discrimination. All learners are trained
	on it, just as they do on other discriminations.

	//FIXME: currently (2009-05-01) only initialized if supplied via XML
    */
    Discrimination fallback=null;

    /** Checks whether the specified discrimination is the fallback
     * discrimination of this suite
     */
    public boolean isFallback(Discrimination disc) { return disc==fallback; }
    public boolean isFallback(int did) { 
	return did2discr.elementAt(did)==fallback; 
    }
    public Discrimination getFallback() { return fallback; }

    /** The labels of all recognized (not provisional) classes from
     * all discriminations. This list is really redundant, considering
     * that all classes are already listed inside Discriminations, but
     * we have it for faster access. */
    private HashMap<Discrimination.Cla, Integer> cla2id = 
	new  HashMap<Discrimination.Cla, Integer>();
    /** The labels of all classes from all discriminations. A class'
     *	position in this list is treated as its numeric ID. This list
     *	is really redundant ('non-normalized'), considering that all
     *	classes are already listed inside Discriminations, but we have
     *	it for faster access. */
    Vector<Discrimination.Cla> id2cla = new  Vector<Discrimination.Cla>();

    /** Verbosity level. 0=silent; 1=few message; 2=many message; 3=everything */
    static public int verbosity = 1;

    /** Discrimination Class Structure types. These constants (in a
     * string form) appear as the value of the "classstructure"
     * attribute of the "discrimination" element */
    public static enum DCS {
	Uncommitted, Fixed, Bounded, Unbounded;
    }

    /** A flag of this type (which may occur in Suite definition)
     * controls whether "simple labels" (without explicitly given
     * discrimination names) are acepted in XML datasets, and if so,
     * how they are interpreted
     */
    public static enum SupportsSimpleLabels {
	No,
	    /** formerly Situation 1 */
	    MultipleBinary,
	    /** formerly Situation 2 */
	    Polytomous;
    }

     /** This flag, which only should be set in the constructor,
     * controls whether "simple labels" (without explicitly given
     * discrimination names are acepted in XML datasets, and if so,
     * how they are interpreted.
     */
    final SupportsSimpleLabels  supportsSimpleLabels;

    /** Modalities for handling a new discrimination's learner when a
	new discrimination is created, either by an API call (APIND) or
	during parsing training data (TND).

	List as per alter-discrimination-while-training-20090425.pdf */
    public static enum CreateNewDiscriminationMode {
	Disallowed,
	    StartFromZero,
	    StartFromZeroLabeled,
	    /** sysdefault */
	    AssumeDefaultHistory;
    }

    final CreateNewDiscriminationMode createNDMode;
  

    /** List as per  alter-discrimination-while-training-20090425.pdf */
    //public static enum TrainingExampleNewDiscriminationMode {
	/** sysdefault */
	//TriggerNewDiscriminations, 
	  //    IgnoreNewDiscriminations,
	  //  RejectOnNewDiscrimination;	    
    //}

    //TrainingExampleNewDiscriminationMode ndTrain;

    /** Types of the parameter ndTrain, determining actions upon
	discovery of a label for a new (previously) unknown
	discrimination in the training data. This is the equivalent of
	TrainingExampleNewDiscriminationMode, as outlined in
	alter-discrimination-while-training-20090425.pdf

        This is also the type of the parameter ndTest, determining
        actions upon discovery of a new discrimination label in the
        test data. There, the only legal values are IGNORE and ERROR.
     */
    public static enum ND { 
	/** = TriggerNewDiscriminations; Create a new discrimination
	 *  (pursuant to the rules set by CreateNewDiscrimination mode
	 *  - which may be equal to StartNewDiscriminationsFromZero,
	 *  or AssumeDefaultHistoryOnNewDiscriminations)
	 */
	ADD, 
	    /** = IgnoreNewDiscriminations; Ignore new discriminations,
	     * but otherwise absorb the example normally.
	     */
	    IGNORE,
	    /** = RejectOnNewDiscrimination; Raise an exception, and not
	     * absorb the example at all 
	     */
	    ERROR;
    }
    
    /*ADD = add the class immediately (which is equivalent to
     adding a column for it into the classifier). This mode should
     note be used when reading the test set, since "we don't let test
     examples affect the set of classes that are defined for a
     discrimination." (As per D.Lewis, 2008-12-17).
    */

    /** Options for dealing with new classes encountered in the
     description of training or test vectors. Classes become known to
     the Suite either when the suite has been constructed by reading
     its description from an XML file (which specifies the list of
     classes for each discirmination), or when a getClaAlways()
     method is invoked with the ADD mode. A "new class" is a class
     that is known to this suite yet.

     IGNORE = ignore the label

     REPLACE = replace the class with the leftover class of the discrimination; but if the discrimination has no leftover class, report an error.

     ERROR = report an error.
     */
    public static enum NC { 
	/** When a new class label is encountered in reading a data point file, it will be immediately added to the list of classes, and will be used in any future classifier training. */
	//ADD, 
	    /** When a new class label is encountered in reading a data point file, it will be added to the "provisional" list of classes, without affecting any classifiers etc. */
	    IGNORE, 
	    /** Replace with the leftover class */
	    REPLACE,
	    /** An error would be thrown if a new class label is encountered in reading a data point file */
	    ERROR,
	    /** Class adding upon an user-driven API call */
	    API
	    ; }

    /** These flags control the suite's behavior when we encounter a
     * previously unknown class label (from an already existing
     * discrimination) on a data point in the training or test set
     * file.
     */
    NC ncTrain=NC.IGNORE, ncTest=NC.IGNORE;
    /** These flags control the suite's behavior when we encounter a
     * previously unknown discrimination on a data point in the
     * training or test set file.
     */
    ND ndTrain=ND.ADD, ndTest=ND.IGNORE;

    public void setNcTrain( NC nc) {  ncTrain = nc; }
    public void setNcTest( NC nc) {  ncTest = nc; }
    public void setNdTrain( ND nd) {  ndTrain = nd; }
    public void setNdTest( ND nd) {  ndTest = nd; }

    public static enum HowToHandleMissingIDs {
	Error , Create; }

    HowToHandleMissingIDs  howToHandleMissingIDs = HowToHandleMissingIDs.Error;

    /** Gets the {@link edu.dimacs.mms.boxer.FeatureDictionary feature dictionary}
       associated with this suite */
    public FeatureDictionary getDic() { return dic;}

    /** Sets the suite's FeatureDictionary. This method should not be
      called if the suite already has a nonempty dictionary.

      <P>A BOXER application would normally have little need to use
      this method, since a dictionary is typically created by BOXER as
      it reads data points. However, one can use it if you have a
      dictionary prepared in advance, and want to ensure a particular order
      in which features are arranged, e.g. for display purposes.

      @throws IllegalArgumentException If the suite already has a non-empty dictionary
     */
    public void setDic( FeatureDictionary _dic) { 
	if (dic != null &&  !getDic().isPracticallyEmpty()) {
	    throw new IllegalArgumentException("One ought not call Suite.setDic() once the suite already has a non-empty dictionary! Current dictionary size=" +  getDic().getDimension());
	}
	if (!_dic.hasDummyIfRequired()) {
	    throw new IllegalArgumentException("Can't set the suite's dictionary, because the new dictionary does not contain the required dummy variable in no. 0 position");
	}
	dic = _dic;
    }
 
    /** Returns the total number of classes in all discriminations of this
	suite */
    public int totalClaCnt() {
	return id2cla.size();
    }

    /** How many Discriminations are in the Suite? */
    public int disCnt() {
	return did2discr.size();
    }
    
    /** Retrieves a Discrimination with the specified integer ID. */
    public Discrimination getDisc(int did) {
	return  did2discr.elementAt(did);
    }

    
    /** Retrieves a Discrimination with the specified name. 
     @return The existing Discrimination with the specified name, or
     null, if no such discrimination exists in this Suite.  */
    public Discrimination getDisc(String disName) {
	return discr.get(disName);
    }

    /** Records the new discrimination object, and each of its classes
     * in the Suite's tables */
    synchronized private void recordDiscFull(Discrimination d) {
	discr.put( d.getName(), d);
	did2discr.addElement(d);	
	for(Discrimination.Cla c: d.getAllClasses()) {
	    recordCla(c);
	}
    }

    /** Records a recently created class in the suite's master table,
      if it has not been recorded there yet.
      @param The class to be recorded. If null is passed, or if the
      class has already been recorded, the method does nothing
     */
    synchronized void recordCla( Discrimination.Cla c) {

 	if (c!=null && !cla2id.containsKey(c)) {
	    if (verbosity>1) Logging.info("Recording: " + c);
	    id2cla.add(c);
	    cla2id.put( c, new Integer(id2cla.size()-1));
	}
    }

    /** Adds a binary discrimination of the kind that we create for
     * simple labels 
     */
    private  Discrimination addSimpleBinaryDiscrimination(String className) {
	if (className.startsWith(Discrimination.NOT_PREFIX)) {
	    throw new IllegalArgumentException("Although simple-label sitution 1 is in effect, we aren't supposed to create a discrimination for label " + className+", because it begins with " + Discrimination.NOT_PREFIX);
	}		
	String notName = Discrimination.notName(className);
	Discrimination d = addDiscrimination(className, 
					     new String[] {className, notName},
					     notName, null, DCS.Fixed, false);

	return d;
    }

    /** Is there a polytomous discrimination designated for
     * accommodating simple labels?
     */
    private Discrimination obsolete_lookupSimplePolytomousDiscr() {

	if (supportsSimpleLabels!=SupportsSimpleLabels.Polytomous) return null;

	Discrimination d = did2discr.elementAt(0);
	if (d==fallback) d =   did2discr.elementAt(1);

	if (d.claCount() > 0 && d.getDefaultCla() != null)  {
	    return d;
	} else {
	    throw new AssertionError("Where's my simple polytomous discrimination? I need a non-fallback discrimination with a default class!");
	}
    }	

    /** If we're in the Simple Labels Polytomous mode, returns the
     * suite's only non-fallback discrimination.
     */
    Discrimination lookupSimplePolytomousDisc() {
	if (supportsSimpleLabels!=SupportsSimpleLabels.Polytomous) throw new IllegalArgumentException("This suite is not set up with the SupportsSimpleLabels.Polytomous mode");

	Discrimination d = lookupSimpleDisc();

	if (d.claCount() > 0 && d.getDefaultCla() != null)  {
	    return d;
	} else {
	    throw new AssertionError("Where's my simple polytomous discrimination? I need a non-fallback discrimination with a default class!");
	}

    }

    /** In a suite with only  one non-trivial discrimination, this
	method returns that discrimination.
     */
    public Discrimination lookupSimpleDisc() {

	int did = 0;
	while(did<disCnt()  && isFallback(getDisc(did))) did ++;

	if (did >= disCnt()) throw  new IllegalArgumentException("This suite  has no non-fallback discrimination");
	else if (did < disCnt()-1) throw  new IllegalArgumentException("This suite  has more than one non-fallback discrimination");

	return getDisc(did);
    }


    /** Only reports recognized classes; so a Discrimination that has



    /** Gets the class by name, only if it already exists.
	@return The matching class, or null, if none is found.
    */
    public  synchronized Discrimination.Cla getCla(String disName, 
						   String className) {
	Discrimination d = discr.get(disName);
	return d==null? null : d.getCla(className);
    }

    /** This method retrieves an existing class with matching
      discrimination name and class name, or creates a new one, if
      appropriate under the circumstances. If no matching class
      exists, and cannot be created, the method will return null or
      throw an exception, depending on the settings.

      This method can be conveniently used for converting a
      discrimination:class label from the data set into a class
      instance of this suite. 

      For details, refer to the documents 
      alter-discrimination-while-training-20090425.pdf

      @param disName Discrimination name (could be null, if a "simple label" has been encountered)
      @param className Class name
      @param isDefinitional Is the label encountered in the training set or
      test set? The difference is important, since we often can create
      new discriminations and classes when parsing the training set,
      but we never do so when reading the test set.
    */
    public synchronized Discrimination.Cla getClaAlways(String disName, 
						 String className, 
						 boolean isDefinitional) {

	Discrimination d=null;
	boolean needBinary = false;

	if (disName==null && 
	    supportsSimpleLabels==SupportsSimpleLabels.MultipleBinary) {
	    // Situation 1 - this can be treated as an implied discrimination
	    // name, and handled as your typical discrimination creation
	    String altName = Discrimination.notName2name(className);
	    disName = (altName != null) ? altName: className;
	    needBinary = true;
	}
	        

	if (disName==null) {
	    if (supportsSimpleLabels==SupportsSimpleLabels.Polytomous) {
		// "simple label" special situation 2	    
		d = lookupSimplePolytomousDisc();
		if (d == null) {
		    throw new AssertionError("Even though supportsSimpleLabels="+supportsSimpleLabels+", the unique polytomous discrimination appears not to have been created!");
		}
	    } else {
		throw new IllegalArgumentException("Discrimination name should not be null unless SupportsSimpleLabels is set. (discr="+disName+"), class=("+className+")");
	    }
	} else if (disName.equals("")) {
	    throw new IllegalArgumentException("Discrimination name should never be an empty string");
	} else {  // Look for (or create) discr by name 
	    d = discr.get(disName);
	    if (d == null) {
		if (isDefinitional) {
		    ND ndMode = ndTrain;
		    // Create and record new discrimination, if allowed
		    if (ndMode == ND.ADD) {
			d = needBinary?
			    addSimpleBinaryDiscrimination(disName):
			    addDiscrimination(disName, null);
		    } else if (ndMode==ND.IGNORE) return null;
		    else if (ndMode==ND.ERROR) throw new IllegalArgumentException("Not allowed to create discrimination for name '"+ disName+"' in train set, because ndMode=" + ndMode);
		    else throw new IllegalArgumentException("Not a legal train set ndMode=" + ndMode);
		} else {
		    // Test set - new discr not allowed
		    ND ndMode = ndTest;
		    if (ndMode == ND.IGNORE) return null;
		    else if (ndMode == ND.ERROR) throw new IllegalArgumentException("Not allowed to create discrimination for name '"+ disName+"' in test set, because ndMode=" + ndMode);
		    else throw new IllegalArgumentException("Not a legal test set ndMode=" + ndMode);
		}
	    }	
	}

	Discrimination.Cla c= isDefinitional?
	    d.addClass(className,  true, ncTrain) :
	    d.getClassForTestSet(className, ncTest);
	    
	recordCla(c);
	return c;
    }


    /** Adds a new class to an existing discrimination in the Suite,
	and in any linked Learner objects. This method is explicitly
	invoked by the user (as opposed to getClaAlways, which is
	automatically called on each label. This is "Case 3
	(APINC/OD)" in the 2009-01-05 "Altering task..." document.
	@param disName Name of an existing discrimination
	@param className Name of the new class to be created.
    */
    public synchronized Discrimination.Cla addClass(String disName, 
						    String className) {

	if (disName==null || disName.equals("")) throw new IllegalArgumentException("Empty discrimination name");
	Discrimination d = discr.get(disName);
	if (d == null) throw new IllegalArgumentException("No discrimination named '" + disName + "' exists");

	Discrimination.Cla c= d.addClass(className);
	Logging.info("Add class " + c.getName());
	recordCla(c);
	return c;
    }

    /** Adds a discrimination, so far with no classes. Discrimination
      will be created without committing its structure, which means
      that the API user can use addClass calls freely to add more
      classes to the new discrimination, until its structure is
      committed (via API, or because training starts).

      This method is the equivalent of  addDiscrimination( disName, null).
     */
    public synchronized Discrimination addDiscrimination(String disName){
	return addDiscrimination(disName, null);
    }


    /** Creates a new discrimination with a given name and a given
	list of classes, and registers them with this suite.  This is
	the short-hand for the main (5-param) method for creating a
	discrimination directly from API, with nulls for the last 3
	values.

	When this method is called, no discrimination with that
	name should exist.

	@param disName The name of the new discrimination to be created
	@param claNames The list of classes, which can be empty. It
	also can be null, in which case the discrimination will be
	created as DCS0.

	@return The new Discrimination that has been created and added to the suite by the call
	@throws IllegalArgumentException If a discrimination with the
	specified name already exists
     */
    public synchronized Discrimination addDiscrimination(String disName, String[] claNames) {
	return addDiscrimination(disName, claNames, null, null, null, false);
    }

   /** Creates a new discrimination with a given name and (optionally)
	a given list of classes, and registers them with this suite.
	This is the main method for creating a discrimination directly
	from API, with the longest list of parameters. There are
	alternative methods, with fewer params; they all expand to
	a call to this method, with null in place of the missing params.

	<p>
	When a discrimination is created automatically upon the
	encountering of a new label of a training example, it is done
	by a BOXER internall call to this methods as well, from
	Suite.getClaAlways().

	<p>
	When this method is called, no discrimination with that
	name should exist.

	<p> The new discrimination's structure will be committed (that
	is, aditional addClass() calls will be allowed in the future
	only to a limited extent; see
	alter-discrimination-while-training-20090425.pdf for details)
	unless claNames is null (in which case we create a DCS0 discr,
	for later class addition).

	@param disName The name of the new discrimination to be created
	@param claNames The list of names of classes that you want to add at this time. For DCS1 (Fixed) and DCS2 (Bounded) the array size should be equal to the desired final (or, respectively, maximum) size of the discrimination. In the latter case, it is permissible for some or all array elements to be nulls; those will be interreted as slots for classes to be added in the future.<br>

	This parameter can be null, in which case the
	discrimination will be created as DCS0. 

	@param defClaName The name of the default class of the new
	discrimination. It should also appear among the names in
	claName. If null is supplied, the new discrimination won't
	have a default class.

	@param leftoversClaName The name of the leftovers class of the new
	discrimination. It should also appear among the names in
	claName. If null is supplied, the new discrimination won't
	have a leftovers class.

	@param dcs The discriminaion class structure type for the new
	discrimination. This parameter is applicable only if the class
	list has been supplied (because otherwise we create a DCS0
	discr, and commit it later, explicitly or implicitly). If null
	is given for dcs, and the class list is supplied, the type
	will be DCS1 (Fixed).

	@return The new Discrimination that has been created and added to the suite by the call

	@throws IllegalArgumentException If a discrimination with the
	specified name already exists
     */    public synchronized Discrimination addDiscrimination(String disName, String[] claNames, String defClaName, String leftoversClaName, DCS dcs) {
	return addDiscrimination(disName, claNames, defClaName, leftoversClaName, dcs, false);
    }

    /** All flavors of addDiscirmination(...), other than the one with
     * the XML argument, eventually get here. This particular method
     * is private, because fallback discr can only be created from the Suite
     * constructor anyway (thus, the user can only specify it via XML)
     */
    private synchronized Discrimination addDiscrimination(String disName, String[] claNames, String defClaName, String leftoversClaName, DCS dcs, boolean isFallback) {
	if (discr.get(disName) != null) {	    
	    throw new IllegalArgumentException("Cannot add discrimination `" +disName+ "', because a discrimination with that name already exists"); 
	} if (!IDValidation.validateDiscName(disName)) {	
	    throw new IllegalArgumentException("Cannot add discrimination named `" +disName+ "', because it is not a legal name");     
	}

	Logging.info("Starting new discrimination `"+disName+"', dcs="+dcs+" no. "+disCnt());
	Discrimination d = new Discrimination(this, disName);

	if (claNames != null) {
	    // DL has distinction between a (defined) empty ClassSet and an
	    // undefined ClassSet
	    if (claNames.length==0) d.ensureClassSetDefined();
	    for(String name: claNames) 	{
		if (name==null && dcs==DCS.Bounded) continue; // allowed to pass nulls in this mode, just to indicate size
		if (!IDValidation.validateClaName(name)) {
		    throw new IllegalArgumentException("Can't add class '"+name+"', because it's not a legal class name");
		}		
		d.addClass(name);
	    }
	    Logging.info("Classes added to discrimination `"+d+"'; now has " + d.claCount() + " classes");

	    if (defClaName != null) d.setDefaultClass(defClaName);
	    if (leftoversClaName != null) d.setLeftoversClass(leftoversClaName);
	    if (dcs == DCS.Uncommitted) d.dcs = dcs;
	    else if (dcs== DCS.Bounded) d.commitStructure(dcs, claNames.length);
	    else d.commitStructure( dcs==null? DCS.Fixed : dcs);
	} else {
	    if (isFallback) 	throw new IllegalArgumentException("The fallback discrimination must include a class list");
	    if (defClaName != null || leftoversClaName != null) {
		throw new IllegalArgumentException("Must supply a class list to addDiscrmination() if also supplying the default or leftover class");
	    }
	    // no class list supplied - we'll expect later addClass call and commitments
	    d.dcs = DCS.Uncommitted; //  (dcs == null) SysDefaults.dcs : dcs;
	} 

	verifySimpleLabelConditions(d, isFallback);
	recordDiscFull(d);
	if (isFallback) setFallback(d);
	return d;
    }

    /** Parses a "discrimination" element of an XML file, adding the
     * discrimination and its categories to this suite.
     @param e The element describing the new discrimination. The new
     discrimination's name should be different from those of all
     already existing discriminations.
    */
    synchronized public Discrimination addDiscrimination(Element e) throws BoxerXMLException {
	Discrimination d = new Discrimination(this, e);
	if ( discr.get(d.getName()) != null) {  
	    throw new IllegalArgumentException("Can't create discrimination from this XML element, because a discrimination named '" + d.getName() + "' already exists");
	} else if (!IDValidation.validateDiscName(d.getName())) {	
	    throw new IllegalArgumentException("Cannot add discrimination named `" +d.getName()+ "', because it is not a legal name");     
	}

	boolean isFallback = Discrimination.isFallback(e);
	verifySimpleLabelConditions(d,isFallback);
	recordDiscFull(d);
	if (isFallback) setFallback(d);
	return d;
    }

    /** Every addDiscrimination() call, of any flavor, calls this
	method, in order to verify, before recording the new discrimination,
	that its creation does not violate any rules that may be in
	effect due to the suite's SupportsSimpleLabels mode

	@throws IllegalArgumentException When the tested
	discrimination is not appropriate for the suite's
	SupportsSimpleLabels mode.
    */
    private void verifySimpleLabelConditions(Discrimination d, boolean isFallback) {
	if (supportsSimpleLabels ==  SupportsSimpleLabels.No) {
	    // no restrictions
	} else if (supportsSimpleLabels==SupportsSimpleLabels.MultipleBinary){
	    if (isFallback) {
		if (!d.isSimpleBinaryFallback()) {
		    throw new IllegalArgumentException("Cannot create discrimination " + d + " as the suite's first (fallback) discrimination, because this suite has the SupportsSimpleLabels="+supportsSimpleLabels+ " property, and this discrimination does not match the requirements for the simple binary fallback");
		} 
	    } else {
		if (!d.isSimpleBinary()) {
		    throw new IllegalArgumentException("Cannot create discrimination " + d + " in this suite, because this suite has the SupportsSimpleLabels="+supportsSimpleLabels+ " property, and this discrimination does not match the requirements for the simple-label binary discrimination");
		} 
	    }
	} else if (supportsSimpleLabels==SupportsSimpleLabels.Polytomous) {
	    if (isFallback) {
		// fallback is irrelevant here, since no new new discr
		// will be created anyway
		Logging.info("Notices creating fallback discrimination in suite " + name +". Although a fallback discirmination is always created, it is irrelevant in this suite because  SupportsSimpleLabels="+supportsSimpleLabels);
	    } else {
		if (disCnt() > (fallback==null? 0:1)) {
		    throw new IllegalArgumentException("Cannot add discrimination " + d + " to this suite, because this suite has the SupportsSimpleLabels="+supportsSimpleLabels+ " property, and only one non-fallback discrimination is allowed in this mode");		    
		} else {
		    if (d.claCount() == 0 && d.getDefaultCla() == null)  {
			throw new IllegalArgumentException("Cannot add discrimination " + d + " to this suite, because this suite has the SupportsSimpleLabels="+supportsSimpleLabels+ " property, but the new discrimination has no default class");
		    }
		}
	    }
        } else {
	    throw new AssertionError("Unknown type supportsSimpleLabels=" +supportsSimpleLabels);
	}
    }


    /** Only reports recognized classes; so a Discrimination that has
     * only provisional classes will be shown as empty */
    public String describe() {
	StringBuffer b=new StringBuffer("The suite contains " + discr.size() + 
					" discriminations, with the total of "+
					totalClaCnt()+" classes.\n");
	
	int i=0;
	for(Discrimination d: did2discr) {
	    b.append("D[" + (i++) + "]=" + d.describe() + "\n");
	}
	if (Suite.verbosity>0) {
	    b.append("--- Complete ordered list of classes ---\n");
	    NumberFormat fmt = new DecimalFormat("###");
	    for(i=0; i<totalClaCnt(); i++) {
		b.append(fmt.format(i)+" " + id2cla.elementAt(i) + "\n");
	    }
	}
	b.append("-----------------------------------");
	return b.toString();
    }


    /** Returns an array of booleans, aligned with suite.id2cla, in
     * which the elements corresponding to the classes listed in the
     * array are set.
     */
    boolean [] getY( Vector<Discrimination.Cla> classes) {
	int r= totalClaCnt();
	boolean b[] = new boolean[r];
	for( Discrimination.Cla c: classes) {
	    Integer idObj = cla2id.get(c);
	    if (idObj == null) {
		/*
		String msg="id2cla=[";
		for(Discrimination.Cla x: id2cla ) msg+= " "+x.getName();
		msg+="]";
		Logging.info(msg);
		msg = "cla2id.size=" + cla2id.size();
		Logging.info(msg);
		*/
		Logging.info("Suite description: " + describe());
		Logging.info("id2cla.size()="+id2cla.size()+", cla2id.size=" + cla2id.size());

		// message...
		/*
Exception in thread "main" java.lang.IllegalArgumentException: Array of classes contained a class label 'safeWITS_2006_03.1-100^DataSource_UniqueURI' not registered in cla2id
        at edu.dimacs.mms.boxer.Suite.getY(Suite.java:746)
        at edu.dimacs.mms.boxer.DataPoint.getY(DataPoint.java:528)
        at edu.dimacs.mms.boxer.NormalizedKnnLearner.absorbExample(NormalizedKnnLearner.java:382)
        at edu.dimacs.mms.applications.ontology.Driver.train(Driver.java:509)
        at edu.dimacs.mms.applications.ontology.Driver.main(Driver.java:429)
		*/
	       
		throw new IllegalArgumentException("Array of classes contained a class label '"+c+"' not registered in cla2id");
	    }
	    int id = idObj.intValue();
	    b[id] = true;
	}
	return b;
    }

    /** Increments counters y[k] for all classes k from the given array.
     */
    int [] addY( Vector<Discrimination.Cla> classes, int y[]) {
	int r= totalClaCnt();
	if (y==null) y = new int[r];
	else if (y.length < r) y=Arrays.copyOf(y,r);
	for(int i=0; i<classes.size(); i++) {
	    Integer idObj = cla2id.get(classes.elementAt(i));
	    if (idObj == null) {
		throw new IllegalArgumentException("Array of classes contained a class label not registered in cla2id");
	    } else {
		int id = idObj.intValue();
		y[id]++;
	    }
	}
	return y;
    }

    /** Used to store certain values that don't need to be recomputed
     * too often */
    private static class Cached { 
	int cid2did[] = null;
	void reset() {  cid2did = null;}
    }

    private Cached cached = new Cached();

    /** Prepares a table, aligned with id2cla, mapping class id to
	discrimination id. Note that while the data in the returned
	table are the snapshot of the current state of the Suite, but
	the future states of the suite will not necessarily be the
	same, since, on the one hand, new classes and discriminations
	can be added, while, on the other hand, discriminations can be
	deleted (which will change discrimination IDs of the surviving
	discriminations).
    */
    public int[] getClaid2Disid() {
	int n = totalClaCnt();
	if (cached.cid2did == null || cached.cid2did.length != n) { 
	    cached.cid2did =  new int[n];
	    for(int i=0; i<n; i++) {
		cached.cid2did[i] = getDid(id2cla.elementAt(i));
	    }
	}
	return  cached.cid2did;
    }

    public int getClaid(Discrimination.Cla c) {
	Integer x = cla2id.get(c);
	if (x == null) throw new IllegalArgumentException("Class " + c + " does not have a global ID. Is that a provisional class perhaps?");
	return x.intValue();
    }

    /** Returns the class which appears in position i in the "master
     *	list" of all classes from all discriminations. This can be
     *	used to e.g. interpret the array of probabilities returned by
     *	{@link edu.dimacs.mms.boxer.Learner#applyModel(DataPoint)}
     */
    public Discrimination.Cla getCla(int i) {
	return id2cla.elementAt(i);
    }


    /** Separates a given matrix into columnar sections pertaining to 
	individual discriminations. In the returned 3-d matrix the first index
	corresponds to the discriminaton id. 
     */
    double [][][] splitMatrixByDiscrimination(double vp[][]) {
	int d = vp.length;
	int disCnt = disCnt();
	int[] claid2disid = getClaid2Disid();
    	double [][][] sec = new double [disCnt][][];
	for(int did=0; did<disCnt; did++) {
	    sec[did] = new double[d][];
	}
	for(int j=0; j<d; j++) {
	    double v[] = vp[j];
	    int pos[] = new int[disCnt];
	    if (v != null) {
		for(int k=0; k<v.length; k++) {
		    int did =  claid2disid[k]; 
		    if (sec[did][j]==null) {
			// how many classes in that discr?
			int dim=did2discr.elementAt(did).claCount();
			sec[did][j] =new double[dim];
		    }
		    sec[did][j][ pos[did]++ ] =  v[k];
		}
	    }
	}	
	return sec;
    }
    
    double [][] splitVectorByDiscrimination(double v[]) {

	int[] claid2disid = getClaid2Disid();
    	double [][] sec = new double [disCnt()][];

	for(int did=0; did< sec.length; did++) {
	    int dim=did2discr.elementAt(did).claCount();
	    sec[did] = new double[dim];
	}

	int pos[] = new int[sec.length];
	for(int k=0; k<v.length; k++) {
	    int did =  claid2disid[k]; 
	    sec[did][ pos[did]++ ] =  v[k];
	}
	return sec;
    }
     
    boolean [][] splitVectorByDiscrimination(boolean v[]) {

	int[] claid2disid = getClaid2Disid();
    	boolean [][] sec = new boolean [disCnt()][];

	for(int did=0; did<sec.length; did++) {
	    int dim=did2discr.elementAt(did).claCount();
	    sec[did] = new boolean[dim];
	}

	int pos[] = new int[sec.length];
	for(int k=0; k<v.length; k++) {
	    int did =  claid2disid[k]; 
	    sec[did][ pos[did]++ ] =  v[k];
	}
	return sec;
    }


   /** Saves just the suite (discriminations with their classes) as if
      it were a complete classifier into in an XML file.
     
      @param fname Name of the file  to write the XML to.
     */
    public void saveAsXML(String fname) {

	Document xmldoc= new DocumentImpl();
	Element e = saveAsXML(xmldoc);
	xmldoc.appendChild(e);
	XMLUtil.writeXML(xmldoc, fname);
    }

   /** Produces an XML document describing just the suite
     (discriminations with their classes) as if it were a complete
     classifier. It can then be saved into an XML file.

     @return An XML "suite-of-classifiers" Element 
     element describes just the suite (discriminations with their
     classes) 
     */
    public Element saveAsXML(Document xmldoc) {
	
	Element root = xmldoc.createElement(XMLUtil.SUITE);
	root.setAttribute(ParseXML.ATTR.NAME_ATTR, name);
	root.setAttribute(ParseXML.ATTR.VERSION_ATTR, Version.version);	
	//root.setAttribute(ParseXML.ATTR.SUITE_NAME_ATTR, name);
	root.setAttribute(ParseXML.ATTR.SUITE.NC_TRAIN, ncTrain.toString());
	root.setAttribute(ParseXML.ATTR.SUITE.NC_TEST, ncTest.toString());
	root.setAttribute(ParseXML.ATTR.SUITE.SUPPORTS_SIMPLE_LABELS, supportsSimpleLabels.toString());
	root.setAttribute(ParseXML.ATTR.SUITE.HOW_TO_HANDLE_MISSING_IDS,
			  howToHandleMissingIDs.toString());

	int disCnt = disCnt();
       
	for(int did=0; did< disCnt; did++) {
	    Discrimination dis =  did2discr.elementAt(did);
	    Element de =  dis.createElement(xmldoc);
	    if (dis==fallback) de.setAttribute(ParseXML.ATTR.DISCR.FALLBACK, "true");

	    root.appendChild(de);
	}

	return root;
    }

    public void serializeLearnerComplex(String fname) {
	Document xmldoc=  serializeLearnerComplex();
	XMLUtil.writeXML(xmldoc, fname);
    }

    /** Creates an XML document made of a LEARNERCOMPLEX element,
      which will describe all the learners for this suite.
     */
    public Document serializeLearnerComplex() {
	
	Document xmldoc= new DocumentImpl();
	Element root = xmldoc.createElement(XMLUtil.LEARNER_COMPLEX);
	//root.setAttribute(ParseXML.ATTR.NAME_ATTR, "");
	root.setAttribute(ParseXML.ATTR.VERSION_ATTR, Version.version);	

	root.appendChild( saveAsXML(xmldoc));
	root.appendChild(dic.createFeaturesElement( xmldoc));
	if (priors!=null) {
	    Logging.info("Saving priors into the LearnerComplex XML");
	    root.appendChild(priors.saveAsXML( xmldoc));
	} else {
	    Logging.info("No priors to save");
	}

	Element le = xmldoc.createElement(XMLUtil.LEARNERS);
	for(Learner algo: usedByLearners) {
	    le.appendChild(algo.saveAsXML(xmldoc));
	}
	root.appendChild(le);

	xmldoc.appendChild(root);
	return xmldoc;
    }


    /** Empty default constructor - creates a suite with default
      properties and no discriminations (yet). One can change some
      properties and add discriminations later; however, the fallback
      discrimination is initialized right away, from sysdefaults. */
    //public Suite() { this("nameless_suite"); }

    /** A minimalist constructor - creates a suite with default
      properties, a fallback discrimination, and no other discriminations
      (yet). One can change some properties and add discriminations
      later; however, the fallback discrimination is initialized right
      away, from sysdefaults.
    @param _name The name to give to the new suite */
    public Suite(String _name) {
	name = _name;
	supportsSimpleLabels =  SysDefaults.supportsSimpleLabels;
	createNDMode = SysDefaults.createNDMode;

	// Since there is no XML suite definition (which may contain a fallback 
	// discrimination too), we must create fallback from sysdefaults.
	initFallback();
    }

    /** Lightweight copy constructor */
    public Suite(String _name, Suite old) {
	name = _name;

	dic = old.dic; // sharing
	verbosity = old.verbosity;
	priors = old.priors; // sharing

	supportsSimpleLabels =  old.supportsSimpleLabels;
	createNDMode = old.createNDMode;

	ncTrain=old.ncTrain;
	ndTrain=old.ndTrain;
	ncTest=old.ncTest;
	ndTest=old.ndTest;
	// do NOT copy usedByLearners

	for(int i=0; i<old.disCnt(); i++) {
	    Discrimination d = old.getDisc(i);
	    Vector<Discrimination.Cla> v  = d.getAllClasses();
	    String cnames[] = new String[v.size()];
	    for(int j=0; j<v.size(); j++) cnames[j] = v.elementAt(j).name;
	    // FIXME: what about DCS2?
	    addDiscrimination(d.name, cnames, 
			      d.getDefaultCla()==null? null:d.getDefaultCla().name,
			      d.getLeftoversCla()==null? null:d.getLeftoversCla().name,
			      d.dcs,
			      (d == old.fallback));
	}

    }

    /** Creates a complete suite based on the XML suite definition
     * read from the specified file. An XML file like this can be
     * created by hand, or saved by saveAsXML. */
    public Suite(File f) throws IOException, SAXException, BoxerXMLException {
	this(ParseXML.readFileToElement(f));
    }

    /** The main constructor of the Suite class, it reads complete
      details of the Suite from the "suite" element (the root element
      of our XML file). The "suite" element would normally contain a
      number of "discrimination" elements nested in it. The first
      <tt>discrimination</tt> element may be tagged with the
      <tt>fallback="true"</tt> tag, in which case it will be
      interpreted as the fallback discrimination for this suite.

     This constructor is also used by the "convenience consructor" 
     {@link  #Suite(File) Suite(File)} 
    */
    public Suite(Element e)	throws SAXException, BoxerXMLException {
	XMLUtil.assertName( e, XMLUtil.SUITE);

	String a;

	a = e.getAttribute(ParseXML.ATTR.NAME_ATTR);
	if (XMLUtil.nonempty(a)) {
	    name = a;
	    if (!IDValidation.validateBasic(name)) {
		throw new BoxerXMLException("The suite name '"+name+"' contained in the XML suite definition is invalid");
	    }

	} else {
	    throw new BoxerXMLException("XML suite definition contains no name");
	}

	a = e.getAttribute(ParseXML.ATTR.SUITE.NC_TRAIN);
	if (XMLUtil.nonempty(a)) {
	    if (a.equals("ADD")) {
		Logging.warning("Obsolete attribute value: " + ParseXML.ATTR.SUITE.NC_TRAIN + "=" + a);
	    } else {
		ncTrain= NC.valueOf( a);
	    }
	}

	a = e.getAttribute(ParseXML.ATTR.SUITE.NC_TEST);
	if (XMLUtil.nonempty(a)) ncTest= NC.valueOf( a);
	if (!(ncTest==NC.IGNORE || ncTest==NC.ERROR)) 	throw new BoxerXMLException(ParseXML.ATTR.SUITE.NC_TEST + " may only be set to IGNORE or ERROR");

	a = e.getAttribute(ParseXML.ATTR.SUITE.ND_TRAIN);
	if (XMLUtil.nonempty(a)) ndTrain= ND.valueOf( a);

	a = e.getAttribute(ParseXML.ATTR.SUITE.ND_TEST);
	if (XMLUtil.nonempty(a)) ndTest= ND.valueOf( a);
	if (!(ndTest==ND.IGNORE || ndTest==ND.ERROR)) 	throw new BoxerXMLException(ParseXML.ATTR.SUITE.ND_TEST + " may only be set to IGNORE or ERROR");

	a = e.getAttribute(ParseXML.ATTR.SUITE.CREATE_ND_MODE);
	createNDMode = (XMLUtil.nonempty(a)) ?
	    CreateNewDiscriminationMode.valueOf(a) :  SysDefaults.createNDMode;

	a = e.getAttribute(ParseXML.ATTR.SUITE.SUPPORTS_SIMPLE_LABELS);
	supportsSimpleLabels = (XMLUtil.nonempty(a)) ? 
	    SupportsSimpleLabels.valueOf(a) :  SysDefaults.supportsSimpleLabels;

	a = e.getAttribute(ParseXML.ATTR.SUITE.HOW_TO_HANDLE_MISSING_IDS);
	howToHandleMissingIDs = (XMLUtil.nonempty(a)) ? 
	    HowToHandleMissingIDs.valueOf(a): HowToHandleMissingIDs.Error;

	for(Node n = e.getFirstChild(); n!=null; n = n.getNextSibling()) {
	    int type = n.getNodeType();
	    String val = n.getNodeValue();

	    if (type == Node.TEXT_NODE && val.trim().length()>0) {
		throw new BoxerXMLException("Found an unexpected non-empty text node, val="  + val.trim());
	    } else if (type == Node.ELEMENT_NODE) {
		Element ne = (Element)n;
		XMLUtil.assertName( ne, XMLUtil.DISCRIMINATION);
		
		boolean isFallback = Discrimination.isFallback(ne);
		if (disCnt()==0) {
		    // Unless the first discr of the suite if marked
		    // as the fallback discr, we must initialize the
		    // suite's fallback from sysdefaults
		    if (isFallback) {
			Logging.info("The first discrimination of the suite is marked as fallback discrimination, and will be used accordingly");
		    } else {
			Logging.info("The first discrimination of the suite is not marked as fallback discrimination; therefore, will use sydefaults as fallback");
			initFallback();
		    }
		} else if (isFallback) {
		    throw new BoxerXMLException("Found a fallback discrimination (named "+ Discrimination.getDiscNameAttr(ne)+") in the suite definition after " + disCnt() + " other discriminations. This is not correct format: the fallback discrimination must always appear first");
		}

		Discrimination disc = addDiscrimination(ne);
	    }
	}
    }

    
    /** Gets the Discrimination ID for the discrimination to which a
	particular class (from a discrimination from this suite) belongs

	FIXME: using linear search - not terribly efficient, if we
	have lots of discriminations
	@param c Class whose ID 
	@throws IllegalArgumentException If the class does not belong
	to a discrimination from this suite.
     */
    public int getDid(Discrimination.Cla c) {
	return getDid( c.getDisc());
    }

    /** Gets the Discrimination ID for a particular discrimination from
	this suite.

	FIXME: using linear search - not terribly efficient, if we
	have lots of discriminations
	@param d Discrimination whose ID we want to look up
	@throws IllegalArgumentException If the discrimination does
	not belong from this suite. This can be triggered if e.g. the
	discrimination has been deleted from the suite.
     */
     public int getDid(Discrimination d) {
	int pos = did2discr.indexOf(d);
	if (pos < 0) throw new IllegalArgumentException("Discrimination " + d + " is not listed in this suite!");
	return pos;
    }


    /** List of all Learners that use this Suite. We maintain it to be
     * able to support deletion of a discrimination.
     */
    Vector<Learner> usedByLearners = new Vector<Learner>();
    
    /** Records one more user of this Suite. This should be invoked from
	the Learner's constructor */
    synchronized void addLearner(Learner algo) {
	if (!usedByLearners.contains(algo)) usedByLearners.add(algo);
    }

    /** Returns the list of all learners registered as using this suite
     */
    public Vector<Learner> getAllLearners() {
	return usedByLearners;
    }

    /** Returns the number of all learners registered as using this suite
     */
    public int getLearnerCount() {
	return usedByLearners.size();
    }


    public void deleteAllLearners() {
	usedByLearners.setSize(0);
    }

    /** Deletes a discrimination from the Suite and from all Learners
       that use this suite.
       @param delDisName The name of the discrimination to be deleted
     */
    synchronized public void deleteDiscrimination(String delDisName) {
	Discrimination d = getDisc(delDisName);
	if (d==null) throw new IllegalArgumentException("Cannot delete discrimination named '" + delDisName + "', because it does not exist in this suite");

	// Delete the discrimination from all Learners using this suite
	RenumMap map = new RenumMap(id2cla, getDid(d), d);
	for(Learner algo: usedByLearners) algo.deleteDiscrimination(map);

	// Adjust our own arrays of classes, and rebuild the hash map
	cla2id.clear();
	for(int i=0; i<id2cla.size(); i++) {	    
	    if (id2cla.elementAt(i).getDisc()==d) id2cla.removeElementAt(i--);
	    else cla2id.put(id2cla.elementAt(i), new Integer(i));
	}

	// Adjust our own arrays of discriminations...
	discr.remove(delDisName);
	did2discr.remove(d);
	cached.reset();
    }

    /** Creates a Learner object and link it to the Suite so that it is
       operating on all Discriminations associated with the Suite.
       Configuration is an XML structure (a "learner" element) that
       specifies what learning algorithm to use and what parameters to
       use with it. Configuration includes a unique ID that will be
       used to distinguish this Learner instance from others (see
       Suite.EmitModel).
       
       <p> The current implementation of this method is simply a
       wrapper around Learner.deserializeLearner()
    */
    public Learner addLearner( Element e) throws SAXException, BoxerXMLException {
	return deserializeLearner( e);
    }

    public Learner addBXRLearner(double eps) {
	return new BXRLearner(this, eps );
    }


    public Learner addBXRLearner(String[] modelfiles, double eps) {
	return new BXRLearner(this, modelfiles, eps );
    }

    /** Parses the "learners" element, which is found nested inside
      "learnercomplex"; adds each found learner to the list of
      learners using the suite.

      New learners will be created in the context of this suite, and
      will be added to the list of learners using that suite

      @param e The "learners" element to parse. Each children should
      all be "learner" elements.
     */
    void deserializeLearners(Element e) throws org.xml.sax.SAXException, BoxerXMLException {
	XMLUtil.assertName(e, XMLUtil.LEARNERS);	
	for(Node n = e.getFirstChild(); n!=null; n = n.getNextSibling()) {
	    int type = n.getNodeType();
	    String val = n.getNodeValue();
	    if (type == Node.TEXT_NODE && val.trim().length()>0) {
		throw new IllegalArgumentException("Found an unexpected non-empty text node, val="  + val.trim());
	    } else if (type == Node.ELEMENT_NODE) {
		XMLUtil.assertName(n, XMLUtil.LEARNER);	
		deserializeLearner( (Element)n);
	    }
	}
    }


    /** Parses the "learner" XML element, which is typically
      encountered within the "learner complex" element, and creates a
      Learner object described thereby.

      The element (which may be the top-level element of an XML file,
      or more often, may be encountered within the "learner complex"
      element) may contain a complete description of a learner that
      may have been created on an earlier run with {@link
      #saveAsXML(String) saveAsXML}, in which case full
      deserialization takes place. Or the element may just describe
      the algorithm parameters, in which case the state is initialized
      with the defaults.
      
      The new learner will be created in the context of this suite,
      and will add the new learner to the list of the learners using
      this suite.

      @param e The "learner" element to parse. It should have an
      "algorithm" property, with a value such as "boxer.TruncatedGradient" etc.
     */
    public Learner deserializeLearner( Element e) throws  org.xml.sax.SAXException, BoxerXMLException {
	XMLUtil.assertName(e, XMLUtil.LEARNER);	

	// saved version
	String version= XMLUtil.getAttributeString(e, ParseXML.ATTR.VERSION_ATTR, Version.version);

	String name = XMLUtil.getAttributeOrException(e, ParseXML.ATTR.NAME_ATTR);
	//	String algoName = XMLUtil.getAttributeOrException(e, ParseXML.LEARNER.ALGORITHM);
	String algoName = XMLUtil.getAttributeString( e, ParseXML.ATTR.LEARNER.ALGORITHM, null);
	if (algoName==null) {
	    if (Version.compare(version, "0.7.006") < 0 &&
		name != null) {
		// Backward compatibility: use the value of the "name" attrib as
		// if it was the "algorithm" prop
		Logging.warning("XML learner element with name='"+name+"' has no 'algorithm' property. Using 'name' as if it was 'algorithm', for backward compatibility reasons (input XML version="+version+")");
		algoName = name;
	    } else {
		throw new IllegalArgumentException("Learner element with name='" + name +"' has no 'algorithm' property, which now is mandatory.");
	    }
	}

	//Class algoClass = Class.forName(a);
	
	if (algoName.endsWith("boxer.TrivialLearner")) {
	    return new TrivialLearner(this, e );
	} else if (algoName.endsWith("boxer.TruncatedGradient")) {
	    return new TruncatedGradient(this, e );
	} else if (algoName.endsWith("boxer.ExponentiatedGradient")) {
	    return new ExponentiatedGradient(this, e );
	} else if (algoName.endsWith("boxer.NormalizedKnnLearner")) {
	    return new NormalizedKnnLearner(this, e );
	} else {
	    throw new IllegalArgumentException("Model reading for algorithm='"+algoName+"' is not supported.");
	}

    }

    /** Trains all learners using this suite on a particular example (or
      several examples).      
      @param xvec A training set (list of data points to train the classifier on). Training will only be done on elements in the range i1 &le; i &lt; i2.
      
    */
    public void absorbExample(Vector<DataPoint> xvec, int i1, int i2) {
	for(Learner algo: getAllLearners()) {
	    algo.absorbExample(xvec, i1, i2);
	}
    }

    /** Trains all learners using this suite on all examples from a
      specified list.
      @param xvec A training set (list of data points to train the
      classifier on). Training will only be done on all elements from
      this vector.      
    */
    final public void absorbExample(Vector<DataPoint> xvec) {
	absorbExample( xvec, 0, xvec.size());
    }

    /** Incrementally trains all learner's associated with this suite
      on all data points from the "dataset" XML element e. Assumes
      that the element contains all relevante labels (so that no
      separate "label store") is required.

      @param e A "dataset" XML element. See also the <a href="../../../../../tags.html">Overview of the XML elements used by BOXER</a>
     */
    final public void absorbExample(Element e) 
	throws BoxerXMLException, SAXException{
	Vector<DataPoint> xvec = ParseXML.parseDatasetElement(e, this, true); 
	absorbExample( xvec, 0, xvec.size());
    }


    /** Declares a particular discrimination of this suite as its
     * fallback discrimination. Verifies that the discrimination as
     * suitable as the fallback, i.e. it has a default class (which is
     * important for learners' AssumeDefaultHistory mode)
     */
    synchronized void setFallback(Discrimination discr) {
	if (fallback != null) {
	    throw new IllegalArgumentException("Cannot make discr '"+discr.name+"' the fallback discrimination of the suite '"+name+"', because the suite already has one!");
	}
	int did = -1;
	try {	did=getDid(discr); } catch (Exception e) {}
	if (did < 0) {
	    throw new IllegalArgumentException("Cannot make discr '"+discr+"' the fallback discrimination of the suite '"+name+"', because it is not part of this suite!");
	}
	if (discr.getDefaultCla()==null) {
	    //throw new IllegalArgumentException("Cannot make discr '"+discr+"' the fallback discrimination of the suite '"+name+"', because it has no default class, and won't serve us well");
	    Logging.warning("Making discr '"+discr+"' the fallback discrimination of the suite '"+name+"', even though it has no default class, and won't serve us well");
	}
	fallback = discr;
    }

    /** Default values of discrimination properties are stored here */
    static class SysDefaults {
	final static Suite.DCS dcs = Suite.DCS.Fixed;
	final static  CreateNewDiscriminationMode createNDMode =
	    CreateNewDiscriminationMode.AssumeDefaultHistory;
	final static SupportsSimpleLabels  supportsSimpleLabels =
	    SupportsSimpleLabels.No;
    }

    /** Names that can be used as the discrimination names in the
     * default discrimination in the MultipleBinary mode. */
    static final String DIS_NAME = "@DiscriminationName",
	NOT_DIS_NAME =  "@NotDiscriminationName";

    static public final String SYSDEFAULTS = "sysdefaults";

    /** Creates a fallback discrimination in this suite, and initializes it with sysdefaults.  */
    private void initFallback() {
	String[] cnames = 
	    (supportsSimpleLabels==SupportsSimpleLabels.MultipleBinary) ?
	    new String[] {DIS_NAME, NOT_DIS_NAME} : new String[0];
	String defName = (cnames.length==0) ? null : NOT_DIS_NAME;
	Discrimination f = addDiscrimination(SYSDEFAULTS, cnames,
					     defName, null,
					     SysDefaults.dcs, true);

    }

    /** Commits structure of all still un-committed
     * discriminations. No action is taken on those discriminaions
     * that are alrady committed. It is convenient to use this methods
     * after you've created an empty suite and added a number of
     * discriminations and classes to it with getClaAlways().  */
    public void commitAllDiscriminations(Suite.DCS dcs) {
	for(Discrimination dis: did2discr) {
	    if (!dis.isCommitted()) dis.commitStructure(dcs);
	}
    }

    /** Creates a "lightweight copy" of this suite. The new suite and
	will have the same policies (regarding adding new
	discriminations and classes etc) as the current suite; its
	discriminations will have the same names as those of this
	suite, the same structure and the same list of
	classes. However, the new suite won't have any learners
	associated with it. 
	<p>
	
	The new suite will be a completely separate "organism" from
	this suite, and the two will live independent lives. (With the
	exception of sharing the FeatureDictionary; this, however, is
	something that does not affect the API user). This means, for
	one, that if you {@link edu.dimacs.mms.boxer.ParseXML parse} any {@link
	edu.dimacs.mms.boxer.DataPoint data point} description XML elements in the
	context of the new suite, causing the creation of new
	discriminations or classes in the new suite, this won't affect
	the old suite. More over, the {@link edu.dimacs.mms.boxer.DataPoint
	DataPoint} objects obtained from the parsing in the context of
	the new suite won't be usable with the old suite, and vice
	versa, because each DataPoint object makes use of a particular
	suite's {@link edu.dimacs.mms.boxer.FeatureDictionary FeatureDictionary}, and
	contains references to a particular suite's classes.  <p>

	One use of creating a lightweight copy of your "main" suite is
	for validating doubtful data. You can parse data sets (or
	sequences of data sets) in the context of the new suite, and
	catch any exceptions (e.g., caused by an attempt to create too
	many classes in a given DCS2 discirmination). The absence of
	exceptions will mean that the same XML data file can also be
	successfully parsed within the context of your "main" suite.

     */
    public Suite lightweightCopyOf(String newName) {
	return new Suite(newName, this);
    }

    /** Only used by anon learner name generator */
    static private int anonLearnerCnt = 0;
    /** Gets a new name to use for an "anonymous" learner  */
    String makeNewAnonLearnerName() {
	return "AnonymousLearner_" + (anonLearnerCnt++);
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