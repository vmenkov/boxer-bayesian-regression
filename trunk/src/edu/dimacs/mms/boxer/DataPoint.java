package edu.dimacs.mms.boxer;

import java.util.*;
import java.io.*;
import java.text.*;

// for XML output
import org.w3c.dom.*;
import org.apache.xerces.dom.DocumentImpl;


/** A DataPoint represents a document, or some other object
    ("example") being classified, converted to a feature vector.  Once
    read in and stored, DataPoint objects can be passed to learners
    for training (be "absorbed") or scoring.

    <p>For the convenience of reporting etc, each DataPoint carries an
    identifier (name). The identifier is immutable, i.e. it is given
    to the DataPoint object upon creation, and should not be changed later
    on. The API user is advised to provide each DataPoint with a
    unique name, to avoid confusion; however, currently (ver 0.6.*)
    Boxer does not enforce uniqueness, due to performance
    considerations.

    <p>
    The information about the example is stored inside the DataPoint
    object as a sparse vector of features.  The representation is by
    an array of feature ids and a corresponding array of feature
    values. The ids are ordered in increasing order, for quick dot
    product and binary search.

    <p>The feature ids stored inside a DataPoint object can be mapped
    to the human-readable feature names using the {@link
    edu.dimacs.mms.boxer.FeatureDictionary} of the suite in whose context the
    DataPoint has been created.

    <p>
    A DataPoint object also contains a list of references to actually
    existing {@link edu.dimacs.mms.boxer.Discrimination.Cla class objects} (which, in
    their turn, belong to actually existing {@link
    edu.dimacs.mms.boxer.Discrimination discrimination objects}), rather than merely
    strings with discr/class names. These class lists associated with
    the DataPoint can be used by BOXER depending on the context in
    which the DataPoint is used (i.e., BOXER would interpret them as
    the training exmaple's labels when absorbing the example, or would
    ignore them when scoring the example as a test example).
  
    <p>
    A BOXER application would typically read a list of data points (a
    data set) from an XML file, using the methods of the {@link edu.dimacs.mms.boxer.ParseXML}
    class.  During this conversion, the text discr/class labels
    (containing in the XML document) are converted into class
    references.  This process can be affected by the user's intended
    purpose of reading the examples (expressed by the isDefinitional param to
    that method), and the applicable suite's flags (e.g.,
    TrainingExampleNewDiscriminationMode, when parsing the data for a
    prospective training example).

    <p>
    It is also possible to create DataPoint objects directly (i.e.,
    not from an XML element).

    <p>Since the DataPoint stores feature ids (resolvable via a
    particular feature dictionary), and references to class objects
    (which only make sense within a particular suite), a DataPoint
    object only makes sense in the context of a particular suite.

    <p>

 */
public class DataPoint implements Measurable  {
    /** The feature dictionary associated with this (and, if fact, all
     * other) data points. We use it for printing the content of the
     * vector in the human-readable form. */
    FeatureDictionary dic;
    public FeatureDictionary getDic() { return dic; }

    /** Feature ids, as per the FeatureDictionary, in increasing order */
    int[] features;
    /** List of feature ids */
    public int[] getFeatures() { return features; }
    /** Feature values. Application programs should not modify this array. */    
    double[] values;
    /** List of values (in the same order as features). Application programs should not modify this array. */
    public double[] getValues() { return values; }
    /** Name of this data point. It is used for human readability, as
      well as for matching dataset files with label files, and for identifying
      examples in output score files.

      FIXME: we could have wanted to ensure uniqueness (within a
      suite), but it was decided (DL, 2009-06-22) not to do it, due to
      performance (storage) considerations
     */
    final String name;

    /** The list of classes (in various discriminations) to which this
      DataPoint has been assigned to. Only labels explicitly supplied
      (via XML or API calls) are stored; default classes of all
      exisitng dsicriminaions will be "implied in" by methods
      accessing this array. The semantics of this list may be
      different for the training and test vectors. */
    private Vector<Discrimination.Cla> classList = new Vector<Discrimination.Cla>();

    /** Computes the dot product of this vector and another vector */
    public double dotProduct( DataPoint p) {
	if (p.dic != dic) throw new IllegalArgumentException("Two vectors are in different feature spaces!");
	int i=0, pi=0;
	double sum=0;
	while(i < features.length && pi< p.features.length) {
	    if (features[i] ==  p.features[pi]) {
		sum += values[i++] * p.values[pi++];
	    } else if (features[i] <=  p.features[pi]) { 
		i++;
	    } else {
		pi++;
	    }
	}
	return sum;
    }


    public double dotProductWithoutDummy( DataPoint p) {
	if (p.dic != dic) throw new IllegalArgumentException("Two vectors are in different feature spaces!");
	int i=0, pi=0;
	double sum=0;
	while(i < features.length && pi< p.features.length) {
	    if (features[i] ==  p.features[pi]) {
		if (!dic.isDummy(features[i])) sum += values[i]*p.values[pi];
		i++;
		pi++;
	    } else if (features[i] <=  p.features[pi]) { 
		i++;
	    } else {
		pi++;
	    }
	}
	return sum;
    }

    /** Computes the vector sum of this vector and another vector. Does <em>not</em> modify this vector. 

	The labels are <strong>lost</strong>, i.e. the result comes out without labels. If needed, they can be copied later separately (see  {@link #linkLabels(DataPoint)}).
     */
    public DataPoint plus( DataPoint p) {
	if (p.dic != dic) throw new IllegalArgumentException("Two vectors are in different feature spaces!");
	int i=0, pi=0;
	Vector <FVPair> w = new 	Vector <FVPair>();
	while(i < features.length && pi< p.features.length) {
	    int f;
	    double q;
	    if (features[i] ==  p.features[pi]) {
		f= features[i];
		q = values[i++] + p.values[pi++];
	    } else if (features[i] <=  p.features[pi]) { 
		f= features[i];
		q = values[i++];
	    } else {
		f= p.features[pi];
		q = p.values[pi++];
	    }
	    w.add(new FVPair( f,q));
	}
	try {
	    return new DataPoint( w, dic);
	} catch( BoxerXMLException ex) {
	    // the exception should not happen
	    return null;
	}
    }


    /** Modifies this vector, multiplying every feature's value by a constant */
    public void multiplyBy(double c) {
	for(int i=0; i<values.length; i++) {
	    values[i] *= c;
	}
    }


    /** Modifies this vector, multiplying every feature's value by a
	specified factor, which is stored (as the value of the
	corresponding feature) in the specified DataPoint
	objects. Features for which no factor is supplied are not affected.

	@param factors A DataPoint object whose features store factors
	by which we will multiply features of <tt>this</tt>
	DataPoint. If zero is stored here for a particular feature, the
	corresponding feature in "this" DataPoint will indeed by multiplied by zero;
	but if no value is stored, then the feature will not be affected.
    */

    public void multiplyFeaturesBy(DataPoint factors) {
	int i=0, pi=0;
	while(i < features.length && pi< factors.features.length) {
	    if (features[i] ==  factors.features[pi]) {
		values[i++] *= factors.values[pi++];
	    } else if (features[i] <  factors.features[pi]) { 
		i++;
	    } else {
		pi++;
	    }
	}
    }


    /** Makes this DataPoint object contain the same labels list as
     * the other object. The list is not copied, just referenced. One may want to use this  method after {@link #plus(DataPoint)} 
     */
    public void linkLabels(DataPoint other) {
	classList = other.classList;
    }



    /** Compute the dot products with all vectors from Beta, add the values to
	the result table.
	@param result The array, aligned with suite.id2label
     */
    /*
    double[] dotProducts(BetaMatrix mat, Suite suite, double result[]) {
	
        if (result==null || result.length < suite.totalClaCnt() ) 
	    result = new  double[ suite.totalClaCnt()];
	
	for(int i=0; i < features.length;i++) {
	    Vector<BetaMatrix.Coef> v = mat.getRow(features[i]);
	    if (v==null) continue; // no active classes for this feature
	    for(BetaMatrix.Coef b: v) {
		result[b.icla] += values[i] * b.value;
	    }
	}
	return result;
    }
    */

    /** Compute the dot products with all vectors from BetaMatrix, add
	the values to the result table.
	@param result The array of dot products, aligned with the discrimination's list of classes (dis.classes)
     */
    double[] dotProducts(BetaMatrix mat, Discrimination dis) {
	
	double result[] = new  double[dis.claCount() ];
	
	for(int i=0; i < features.length;i++) {
	    Vector<BetaMatrix.Coef> v = mat.getRow(features[i]);
	    if (v==null) continue; // no active classes for this feature
	    for(BetaMatrix.Coef b: v) {
		result[b.icla] += values[i] * b.value;
	    }
	}
	return result;
    }


    /** The dot product of this vector to itself, i.e. the square of
     * the 2-norm of this vector */
    public double normSquare() {
	double sum=0;
	for(double v: values)   sum += v * v;
	return sum;
    }

    /** The square of the norm of the vector without the dummy component
     */
    public double normSquareWithoutDummy() {
	double sum=0;
	for(int i=0; i < features.length;i++) {
	    if (!dic.isDummy(features[i])) sum += values[i]*values[i];
	}
	return sum;
    }


    /** The infinity norm of this vector, i.e. the abs max value of a
     * component */
    double infNorm() {
	double z = 0;
	for(double v: values)   z = Math.max(z, Math.abs(v));
	return z;	
    }


    /** Retrieves the feature value for the specified feature id. Implemented
     * using binary search */
    double getValueForFeature(int f) {
	// f is somewhere in features[imin : imax-1]
	int imin = 0, imax = features.length;
	while(imin < imax) {
	    int mid = (imin + imax) / 2;
	    if (features[mid] == f) return values[mid];
	    else if (features[mid] > f) imax = mid;
	    else imin = mid + 1;
	}
	return 0;
    }

    double getValueForFeature(String f) {
	return  getValueForFeature(dic.getId(f));
    }

    /** A convenience class for constructors. An instance of FVPair
     * represents a (feature ID, feature value pair), where feature ID
     * can be mapped to a human-readable feature name via the
     * currently used Suite's FeatureDictionary */
   public  static class FVPair implements Comparable<FVPair> {
	int feature; double value; 
	public FVPair( int f, double v) { feature=f; value=v;} 
	/** Used for sorting */
	public int compareTo(FVPair x)  {
	    return feature - x.feature;
	}
    }

    /** A constructor used in parsers etc. It will also sort pairs
      according to the feature id. 

      When using this constructor, the API user does not provide BOXER
      with a name (ID) for the new data point. Instead, BOXER will
      auto-generate a (likely unique) name.

      @param _v A Vector of feature/value pairs describing the new data point
      @param _dic the FeatureDictionary of the suite in whose context
      the DataPoint will be used.

      @throws BoxerXMLException Never
     */
    public DataPoint(Vector <FVPair> _v, FeatureDictionary _dic) throws BoxerXMLException {
	this(_v, _dic, autoGeneratedName());
    }

    /** A constructor used in parsers etc. It will also sort pairs
     * according to the feature id, so that dot product, binary search, and
     other operations relying on the right order will work. 

      @param _v A Vector of feature/value pairs describing the new data point
      @param _dic the FeatureDictionary of the suite in whose context
      the DataPoint will be used.
      @param _name The name (example ID) to be assigned to the new data point

      @throws BoxerXMLException If the name is invalid
    */
     public DataPoint(Vector <FVPair> _v, FeatureDictionary _dic,String _name)
    throws BoxerXMLException {
	 name = _name;
	 if (!IDValidation.validateBasic(name)) {
	     throw new  BoxerXMLException("'"+name + "' is not a legal datapoint name. Please see Boxer log messages for details.");
	 }
	 dic = _dic;
	 FVPair va[] = _v.toArray(new FVPair[_v.size()]);
	 Arrays.sort(va);
	 
	 features = new int[va.length];
	 values = new double[va.length];
	 
	 for(int i=0; i<va.length; i++) {
	     features[i] = va[i].feature;
	     values[i] = va[i].value;
	 }
     }

    /** Constructs a data point from the data in the hash map.

	@param words The feature name / feature value map describing the new
	data point
	@param dic The FeatureDictionary of the suite in whose context
	the DataPoint will be used.
	@param name The name to assign the new data point

	@throws BoxerXMLException If the data point name is
	inappropriate, as per {@link edu.dimacs.mms.boxer.IDValidation}
    */
    static public DataPoint makeDataPoint(HashMap <String, Double> words, 
			       FeatureDictionary dic, String name) throws BoxerXMLException {
       
	Vector <FVPair> v=new  Vector <FVPair>();
	for( String z : words.keySet()) {
	    v.add(new FVPair(dic.getIdAlways(z), words.get(z).doubleValue()));
	}
	return new DataPoint(v, dic, name);
   }

    /** Converts a class list into an array aligned with suite.did2discr.
	@param _classes Class list to analyze
	@return An array aligned with  suite.did2discr. The i-th element is the class corresponding to the discrimination with did=i. It may be null if _classes contained no class from the i-th discrimination.
	@throws IllegalArgumentException If the array had more than one class for some discr
     */
    private Discrimination.Cla[] uniqueClassList( Suite suite) {
	Discrimination.Cla unique[] = new Discrimination.Cla[suite.disCnt()];
	String errmsg = "";
	for(Discrimination.Cla c: classList) {
	    // FIXME: must provide proper support in case of a deleted discr!
	    int did = -1;
	    try {
		did = suite.getDid(c);
	    } catch(Exception e) { 
		Logging.warning("No record found for class " + c + " in the suite");

		e.printStackTrace(System.err);

		continue; //deleted discr, probably
	    }
	    if (unique[did] == null) unique[did] = c;
	    else {
		errmsg += " (" + unique[did] + " and " + c + ")";
	    }
	} 
	if (!errmsg.equals("")) {
	    errmsg = "Illegal attempt to label the datapoint '"+name+"' as belonging to multiple classes of the same discrimination, viz. " + errmsg; 
	    throw new IllegalArgumentException(errmsg);
	} 
	return unique;
    }

    /** Sets the lists of classes for this data point, overriding any
	list that may already has been stored. Verifies that no more
	than one class has been specified in each Discrimination. Adds
	default classes (if set) when class labels are missing  in the input.
	@param _classes The new list of classes
	@param suite The suite in the conext of which we operate. (This param is just used for data validation)
    */
     public void setClasses( Vector<Discrimination.Cla> _classes, Suite suite) {
	 classList = _classes;
	 // validate uniqueness; exception will be thrown on error
	 /*Discrimination.Cla unique[] = */ uniqueClassList(suite);
     }

     /** Returns a vector of class labels for this DataPoint's; it
       will include both the stored labels and all aplicable default-class
       labels.
       @return A vector of classes, including both stored and added
       default classes. For efficiency's sake, when no default classes
       need to be added, this would be just the stored vector of
       classes - which means that the caller should never modify
       this vector later on.
      */
     public  Vector<Discrimination.Cla> getClasses(Suite suite) {
	 Discrimination.Cla unique[] = uniqueClassList( suite);
	 // Add default classes whenever needed
	 int nzCnt=0;
	 for(int i=0; i<unique.length; i++) {
	     if (unique[i]==null) {
		unique[i] = suite.getDisc(i).getDefaultCla();
		//System.out.println("Completing " +name+ " with " + unique[i]);
		if (unique[i]!=null) nzCnt ++;	    
	     }
	 }
	 if (nzCnt > 0) {
	     // some default classes have been added
	     Vector<Discrimination.Cla> v=new Vector<Discrimination.Cla>(nzCnt);
	     for(Discrimination.Cla c: unique) if (c!=null) v.add(c);
	     return v;
	 } else {
	     return classList;
	 }
     }

     /** Scans the list of already-assigned classes of this data
       point, and whenever no class has been assigned for a
       discrimination, adds that discrimination's default class.  If
       this method is applied to a data point to which no classes had
       been assigned yet, the effect will be assigning the default
       class to it in each discrimination. 

       In most cases, there is no need to use this method, since all
       class labels, including the defaults, are set by setClasses,
       which is called during reading the data point from XML
       file. However, if there is a chance that new discriminations
       have been created since then (e.g., because discrimination
       creation may have been triggered by labels found on the "later"
       data points in the XML file), calling this method may be
       advisable to ensure that data points that were not labeled with
       respect to those "latter-day" discriminations get their default
       classes.

       Note also that this method would be of no help if the default
       classes of some discriminations have changed since the data
       point has been first read. This is because during the initial
       parsing the data point already was assigned the default classes
       of all the discriminations for which it was not explicitly
       labeled, and these assignements are here to stay.
     */
    /*
     public void addDefaultClasses( Suite suite) {
	 // list of already-assigned classes for each discrimination 
	 Discrimination.Cla unique[] = new Discrimination.Cla[suite.disCnt()];
	 for(Discrimination.Cla c: classList) unique[ suite.getDid(c) ] = c;
	 // whenever none is assigned, add the default, if available
	 for(int i=0; i<unique.length; i++) {
	     if (unique[i]==null) {
		 Discrimination.Cla defCla = suite.getDisc(i).getDefaultCla();
		 if (defCla !=null) classList.add( defCla );
	     }
	 }
     }
    */

     /** Adds a new class to the class list for this data point.
      * Checks if this data point's class list already has a class assigned 
      * from the same discrimination, and if it does, either reports an error
      * or replaces the old class.
      @param replace If this flag is true, the new class would replace any class from the same discrimination already assigned to this data point. If false, this non-uniqueness will cause an exception.
      */
     public void addClass(Discrimination.Cla cla, boolean replace) {
	 String errmsg = "";

	 Discrimination newDisc = cla.getDisc();

	 for(int i=0; i<classList.size(); i++) {
	     Discrimination.Cla c = classList.elementAt(i);
	     if (c.getDisc() == newDisc) {
		 if (replace) {
		     classList.set(i, cla);
		     return;
		 } else {
		     throw new IllegalArgumentException("Faield to add class " + cla + " to this data point " + this + "\nbecause the data point already has been assigned a class from the same discrimination");
		 }
	     }
	 }
	 classList.add(cla);
     }
     

   /** Retrieves the data point's identifier (name) */
    public String getName() { return name; }

    /** Creates a human-readable representation of the data point,
     * containing explicitly stored features and class labels.
     */
    public String toString() {
	StringBuffer b = new StringBuffer();
	if (name != null) b.append("{"+name+"} ");
	if (classList != null) {
	    b.append("[");
	    for(int i=0; i<classList.size(); i++) {
		b.append(" " +classList.elementAt(i));
	    }
	    b.append("]");
	}
	b.append(" (");
	for(int i=0; i < features.length;i++) {
	    b.append(" " + dic.getLabel(features[i]) + BXRReader.PAIR_SEPARATOR_STRING);
	    if ((double)(int)values[i] == values[i]) {
		b.append( "" + (int)values[i]);
	    } else {
		b.append( values[i]);
	    }
	}
	b.append(")");
	return b.toString();
    }
    
    /** Returns an array, aligned with suite.id2cla, of booleans, in
     * which the elements corresponding to the classes in this
     * datapoint (explicitly stored, or default) are set.
     */
     public boolean[] getY(Suite suite) {
	 return suite.getY(getClasses(suite));
    }

    /** Returns an array of booleans which has a true value for each
     * Discrimination that the given vector has an explicitly stored label on
     */
    /*
      private boolean [] labeledDiscr( Suite suite) {
	boolean has[] = new boolean[ suite.disCnt() ];
	for( Discrimination.Cla c: classList) {
	    has[ suite.getDid(c) ] = true; 
	}	
	return has;
    }
    */

    /** Returns the class label associated with this data point in the
      specified discrimination (explicitly or implicitly).
      @param dis A discrimination
      @return The class that is explicitly associated with the
      specified discrimination in this data point, or, if none is, the
      discrimination's default class; if there is no default class
      either, then null.
     */
    public Discrimination.Cla claForDisc(Discrimination dis) {
	//System.out.println("claForDisc(" + name + ", " + dis.name+")");
	for( Discrimination.Cla c: classList) {
	    //System.out.println("check class " + c);
	    if (c.getDisc()==dis) return c;
	}	
	return dis.getDefaultCla();
    }


    /** Describes the data point as an element of an XML document. The
	dummy feature is ignored. As far as class labels go, only
	explicitly stored labels (not discirmination defaults) are
	written into the XML.
     */
    public org.w3c.dom.Element toXML(Document xmldoc) {
	return toXML(xmldoc, false);
    }
    
   /** Describes the data point as an element of an XML document. The
	dummy feature is ignored, unless keepDummy==true. As far as
	class labels go, only explicitly stored labels (not
	discirmination defaults) are written into the XML.
     */
    public org.w3c.dom.Element toXML(Document xmldoc, boolean keepDummy) {
	 final boolean skipEmpty = true;

	 Element e = xmldoc.createElement( ParseXML.NODE.DATAPOINT);
	 if (name != null && !name.equals("")) {
	     e.setAttribute(ParseXML.ATTR.NAME_ATTR, name);
	 }

	 if (classList != null && (classList.size()>0 || !skipEmpty)) {
	    Element elabels = xmldoc.createElement( ParseXML.NODE.LABELS);
	    StringBuffer b=new StringBuffer();
	    for(Discrimination.Cla c: classList) {
		if (b.length()>0) b.append(" ");
		b.append( c);
	    }
	    elabels.appendChild( xmldoc.createTextNode(b.toString()));
	    e.appendChild(elabels);
	 }

	 if (features.length>0 ||  !skipEmpty) {
	     Element ef = xmldoc.createElement( ParseXML.NODE.FEATURES);
	     StringBuffer b=new StringBuffer();
	     for(int i=0; i < features.length;i++) {
		 if (!keepDummy && dic.isDummy(features[i])) continue; // skip the dummy feature
		 if (b.length()>0) b.append(" ");
		 b.append(dic.getLabel(features[i]));
		 b.append(BXRReader.PAIR_SEPARATOR);
		 b.append((double)(int)values[i] == values[i] ? 
			  "" + (int)values[i] :
			  "" + values[i] );
	     }
	     ef.appendChild(xmldoc.createTextNode(b.toString()));
	     e.appendChild(ef);
	 }

	return e;
    }

    /** Describes the data point in BMR format, only carrying the label for a
	specified discrimination.

	@param numeric Print classes' numeric IDs, rather than symbolic names
     */
    public String toBMR(Discrimination dis, boolean keepDummy, boolean numeric) throws BoxerException {
	Discrimination.Cla cla = claForDisc(dis);
	StringBuffer b = new StringBuffer();
	if (cla == null) {
	    // Unknonw class: this is a possibility for the test set, anyway
	    //throw new BoxerException("Data point "+name+" is not labeled with respect to discrimination " + dis + ", nor does the discrimination have a default class");
	    b.append("?");
	} else 	if (numeric) {
	    b.append(cla.getPos());
	} else {
	    b.append(cla.getName());
	}
	for(int i=0; i < features.length;i++) {
	    if (!keepDummy && dic.isDummy(features[i])) continue; // skip the dummy feature
	    b.append(" ");
	    b.append(features[i]);
	    b.append(BXRReader.BMR_PAIR_SEPARATOR);
	    b.append((double)(int)values[i] == values[i] ? 
		     "" + (int)values[i] :   "" + values[i] );
	}
	return b.toString();
    }
    
    /** Saves a single DataPoint (this one) as an XML file */ 
    public void saveAsXML( String fname) {
	Document xmldoc= new DocumentImpl();
	Element root = toXML(xmldoc); 
	root.setAttribute("version", Version.version);	
	xmldoc.appendChild(root);
	XMLUtil.writeXML(xmldoc, fname);
    }



   /** Saves a list of data points as an XML file.
       @param v Saves all DataPoints from this vector
       @param name The name that will be written as <dataset name="..."> 
       @param fname The name of the output file to create
     */
    public static void saveAsXML(Vector<DataPoint> v, String name, String fname) {
	saveAsXML(v, 0, v.size(), name, fname);
    }

   /** Saves a list of data points (a dataset) as an XML file.
       @param v Saves DataPoints v[i1:i2-1] from this vector
       @param name The name that will be written as <dataset name="..."> 
       @param fname The name of the output file to create
     */
    public static void saveAsXML(Vector<DataPoint> v, int i1, int i2, String name, 
			  String fname) {
	Document xmldoc= new DocumentImpl();
	Element root = xmldoc.createElement(ParseXML.NODE.DATASET);
	
	root.setAttribute(ParseXML.ATTR.NAME_ATTR, name);
	root.setAttribute("version", Version.version);	
	
	for(int i=i1; i<i2; i++) {
	    root.appendChild( v.elementAt(i).toXML( xmldoc));
	}

	xmldoc.appendChild(root);
	XMLUtil.writeXML(xmldoc, fname);
    }

    public static void saveAsBMR(Vector<DataPoint> v,
				 Discrimination dis,
				 boolean numericCla,
				 String fname) 
	throws IOException, BoxerException {
	saveAsBMR( v, 0, v.size(), dis, numericCla, fname);
    }

   /** Saves a list of data points (a dataset) as a BBR/BMR file, for
       use with BBR/<a href="http://www.bayesianregression.com/bmr.html">BMR</a>/BXR.<p>

       <p>
       BBR/BMR/BXR is not set up for dealing with multiple discriminations
       at a time; therefore, when saving a dataset, one must indicate 
       what discrimination's labels should go into the BMR file.

       <p> The BMR file produced by this method always encodes
       features by their numeric IDs. This is different from BOXER's
       own XML format, where symboliuc feature names are
       used. Therefore, if multuiple datasets that are (conceptually,
       at least) viewed to be in the same feature space are to be
       converted to BMR format, it is essential that the same
       FeatureDictionary was associated with the data points from all
       sets. 

       <p>On the other hand, one can control how the classes are
       represented. One can use symbolic class names, which is more
       flexible and easier to understand for a human reader. On the
       other hand, one needs to use numeric class IDs in order to
       provide better compatibility with BXR's various modes, e.g. to
       be able to run "BXRclassify --classic". If multiple datasets
       are converted to BMR format with numeric class IDs, care should
       be taken to ensure that Discriminations from the same Suite
       object is used on them all, to ensure proper numbering of
       classes.       

       @param v Saves DataPoints v[i1:i2-1] from this vector
       @param dis Print labels with respect to this discrimination
       @param fname The name of the output file to create
       @param numericCla If true, classes are encoded by IDs, rather than names. This is necessary e.g. to be able to run "BXRclassify --classic".
     */
    public static void saveAsBMR(Vector<DataPoint> v, int i1, int i2, 
				 Discrimination dis,
				 boolean numericCla,
				 String fname) throws IOException, BoxerException {
	PrintWriter w = new PrintWriter(new FileWriter(fname));
	saveAsBMR(v,i1,i2, dis,  numericCla,w);
	w.close();
    }

   public static void saveAsBMR(Vector<DataPoint> v, int i1, int i2, 
				 Discrimination dis,
				 boolean numericCla,
				 PrintWriter w) throws IOException, BoxerException {
	for(int i=i1; i<i2; i++) {  
	    w.println( v.elementAt(i).toBMR(dis, false, numericCla));
	}	
    }


    public long memoryEstimate() {
	return Sizeof.OBJ + 2* Sizeof.OBJREF +
	    Sizeof.sizeof(values) + Sizeof.sizeof(features);
    }

     /** An auxiliary class used in generating compact human-readable
      * representation of the score vector */
     public static class ScoreRun {
	 double lastVal=0;
	 int lastCnt=0;
	 boolean sameRun(double p) { return (lastCnt==0 || p==lastVal); }
	 void add( double p) { lastVal=p; lastCnt++; }
	 String dump() {
	     String s = String.format(" %4.3f",lastVal);
	     if (lastCnt>1) s += "*" + lastCnt;
	     lastCnt=0;
	     return s;
	 }
	 String dumpAny() {
	     return isEmpty() ? "" : dump();
	 }
	 boolean isEmpty() { return lastCnt==0; }
	 
     }
     
     /** Returns a string listing the scores with
      * annotations. Specially marks scores for the classes to which
      * this data point is known to be assigned according to its
      * classes array.

      <p> This method is designed to produce a compact, but still
      readable representation, so it does not print class names. Each
      lines contains scores for all classes in one discrimination; the
      discrimination name appears first, and all classes' scores
      (probabilities) follow. The probability for the supposed
      "correct" class (when known) are marked with square brackets, so
      that one could see at a glance if the classifier's predictions
      are "correct" or not.
 */
     public String describeScores(double prob[][], Suite suite) {
    	 boolean y[] = getY(suite);
    	 boolean ysec[][] = suite.splitVectorByDiscrimination(y);

    	 StringBuffer  b = new StringBuffer();

    	 for(int did = 0; did < ysec.length; did ++) {
    		 b.append(" "+suite.getDisc(did).name +"(");

    		 ScoreRun run = new ScoreRun();

    		 for(int i=0; i<prob[did].length; i++) {
    			 double p= prob[did][i];
    			 if (ysec[did][i]) {
    				 b.append(run.dumpAny());
    				 b.append(String.format(" [%4.3f]",p));
    			 } 
    			 else {
    				 if (!run.sameRun(p)) b.append( run.dumpAny());
    				 run.add(p);
    			 }
    		 }
    		 b.append(run.dumpAny());
    		 b.append(")");
    	 }
    	 return b.toString();
     }
     
     /** Returns a string listing the scores with
      * annotations. Specially marks scores for the classes to which
      * this data point is known to be assigned according to its
      * classes array */
     public String describeScoresLong(double prob[][], Suite suite) {
    	 boolean y[] = getY(suite);
    	 boolean ysec[][] = suite.splitVectorByDiscrimination(y);

    	 StringBuffer  b = new StringBuffer();

    	 for(int did = 0; did < ysec.length; did ++) {
    		 b.append(" "+suite.getDisc(did).name +"(");

    		 ScoreRun run = new ScoreRun();

    		 for(int i=0; i<prob[did].length; i++) {
    			 double p= prob[did][i];
    			 if (ysec[did][i]) {
    				 b.append(run.dumpAny());
    				 b.append(String.format(" [%4.3f]",p));
    			 } 
    			 else {
    				 if (!run.sameRun(p)) 
    					 b.append( run.dumpAny());
    				 run.add(p);
    			 }
    		 }
    		 b.append(run.dumpAny());
    		 b.append(")");
    	 }
    	 return b.toString();
     }

    /* Probability assigned by the classifier to the "true" class in
     * each discr. This value can be later used to compute the
     * log-likelyhood, viz.:

     <P>
       To compute it for a discrimination on a test set do this:

       <OL>
       <li> For each test example find the probability the model assigns to the single class that is correct for that example.  Take the log base e of that probability.
       
       <li> Sum this quantity across the test set.

       <li> Divide by number of test examples to get the mean log likelikelihood.
       
       </ol>

       <p>
       This is a negative number with maximum value 0.0, so graphs of this value for models trained from nested training sets will show a line more or less climbing toward 0.

       <p>
       This is in some sense the most natural measure of generalization for logistic regression.

       @param probLog - logarithms of proababilities, obtained with
       some learner's classifier (whose quality we wish to measure)

       @param logLikCnt Array whose elements will be incremented by 1,
       counting the number of data points for each discrimination

       @param logLik Array to whose elements the logLik contributions
       (for each discr) will be added

    */
    public void addLogLik(double probLog[][], Suite suite, int logLikCnt[], double logLik[]) {
	addLogLinLik(probLog, null, suite, logLikCnt, logLik, null);
    }

    public void addLogLinLik(double probLog[][], double prob[][],
			  Suite suite, int likCnt[], 
			  double logLik[], double linLik[]) {
	 for(int j=0;j<probLog.length; j++) {
	     Discrimination dis = suite.getDisc(j);
	     Discrimination.Cla trueC = claForDisc(dis);
	     if (trueC==null) {
		 if (!dis.getName().equals(Suite.SYSDEFAULTS)) {
		     Logging.warning("No 'true class' label is stored for dis=" + dis.name + " in data point x="+name);
		 }
		 continue;
		 //throw new IllegalArgumentException("No 'true class' label is stored for dis=" + dis.name + " in data point x="+name);
	     } else {
		 likCnt[j]++;
		 int trueCPos = trueC.getPos();
		 if (probLog != null) 	 logLik[j] += probLog[j][ trueCPos];
		 if (prob != null)       linLik[j] += prob[j][ trueCPos];
	     }
	 }
     }

    
    /** Returns a string listing the scores with
      * annotations. Specially marks scores for the classes to which
      * this data point is known to be assigned according to its
      * classes array */
     public String describeScores(double prob[], Discrimination dis) {
	 Discrimination.Cla trueC = claForDisc(dis);
	 int trueCPos = (trueC==null) ? -1 :  trueC.getPos();

	 StringBuffer  b = new StringBuffer();

	 b.append(" "+dis.name +"(");

	 ScoreRun run = new ScoreRun();

	 for(int i=0; i<prob.length; i++) {
	     double p= prob[i];
	     if (trueCPos == i) {
		 b.append(run.dumpAny());
		 b.append(String.format(" [%4.3f]",p));
	     } else {
		 if (!run.sameRun(p)) b.append( run.dumpAny());
		 run.add(p);
	     }
	 }
	 b.append(run.dumpAny());
	 b.append(")");

	 return b.toString();
     }

     /** Prints information about the scores of a data point with
       respect to all classes in a plain text form, one score per
       line, complete with all other information (run id, dis name,
       class name etc).
       

       <p>
       Column details:
       
       <ol>
       <li>  RUN_ID : This is something that the user is required to supply to BORJ.

       <li>  EXAMPLE_ID: a unique ID that either is taken from the example or, if the example doesn't have one, BORJ creates one. (Perhaps by concatenating a number to the RUN_ID.)

       <li>  SUITE_NAME: Name of the Suite.

       <li>  LEARNER_NAME: Name of the Learner (since ver. 0.7.006)

       <li>  DISCRIMINATION_NAME : Name of the discrimination.

       <li>  LINE_CLASS_NAME : Name of the class that this line is reporting on.

       <li>  PROB : The probability, p, that the model predicts for the example EXAMPLE_ID belonging the class LINE_CLASS_NAME from discrimination DISCRIMINATION_NAME.  BOXER provides this and it's always in the closed interval [0.0, 1.0].

       <li>  PREDICTED_CLASS_NAME : the class that BOXER would pick for the example if forced to choose.  The main purpose of this is to allow consistent handling of tied scores.  An application using BOXER can always override its choice.

       <li>  TRUE_CLASS_NAME :  inserted by BORJ when known.
  </ol>

  <p> Normally, one would invoke this method after the DataPoint has
  been scored with a learner's {@link Learner#applyModel( DataPoint
  p)} method.

  @param prob the probability array, e.g. as returned by {@link Learner#applyModel( DataPoint p)}
  @param learner Learner with which the data point has been scored. It is mostly passed in so that the method can access the suite associated with the learner, in whose context the data point has been scored
  @param out PrintWriter to which the text will be printed.
  
      */
     public void reportScoresAsText(double prob[][], Learner learner, String runid, PrintWriter out) {
	 Suite suite = learner.getSuite();
	 Discrimination.Cla[] chosen=interpretScores(prob,suite);

	 for(int did = 0; did < prob.length; did ++) {	     
	     Discrimination dis = suite.getDisc(did);
	     for(int i=0; i<prob[did].length; i++) {		 
		 Discrimination.Cla trueCla = claForDisc(dis);
		 String q[] = {runid,  name,  suite.name, learner.getName(),
			       dis.name, 
			       dis.getClaById(i).name, ""+  prob[did][i],
			       chosen[did].name, (trueCla==null? "null" : trueCla.name)};
		 for(int j=0; j<q.length; j++) {
		     if (j>0) out.print(" ");
		     out.print(q[j].replace(" ", "%20"));
		 }
		 out.println();
	     }
	 }
     }

     /** Which classes in each discriminaion have the largest
	 probability value?  If the same probability score is assigned
	 by the classifier to multiple classes, the "tie-breaking"
	 rule is to pick "the first class", i.e. class with the lowest
	 class id.

	 @return The array that includes the "best" (highest-score)
	 class for each discrimination. It is ordered in the
	 discrimination ID order. 
      */
     public Discrimination.Cla[] interpretScores(double prob[][], Suite suite) {

	 Discrimination.Cla v[] = new Discrimination.Cla[prob.length];
	 for(int did =0; did<prob.length; did++) {
	     if (prob[did].length == 0) continue; // empty discr; keep null
	     int sel=0;
	     for(int j=1; j<prob[did].length; j++) {
		 if ( prob[did][j] >  prob[did][sel]) sel = j;
	     }
	     v[did] =  suite.getDisc(did).getClaById(sel);
	 }
	 return v;
     }

    private static String defaultNameBase = "Boxer_Autogenerated_Name";

    /** Sets the prefix used for generating names for data points
	whose XML entries lack names
     */
    public static void setDefaultNameBase(String _defaultNameBase) {
    	defaultNameBase = _defaultNameBase;
    }

    private static int autoNumberedDPCount = 0;
    private static NumberFormat autoNumberFmt = new DecimalFormat("000000000");
    /** Proposes a (likely - though not guaranteed - unique) name for a new
	DataPoint, if the user does not have enough fantasy to come up 
	with a name of his own. The API user usually does not need to
	use this method, as it is invoked automatically when a name-less
	DataPoint constructor is  used.
    */
    public static synchronized String autoGeneratedName() {
	return  defaultNameBase + "_" +
	    autoNumberFmt.format( autoNumberedDPCount ++);
    }


    /** Assembles a DataPoint object by pointing to the three arrays involved.
     */
    private DataPoint(String _name, int[] _features, double[] _values, 
		      Vector<Discrimination.Cla> _classList, FeatureDictionary _dic) {
	name = _name;
	features = _features;
	values = _values;
	classList = _classList;
	dic=_dic;
    }

    /** This is used for DataPointArray.
     */
    DataPoint shallowCopyWithoutLabels() {
	return  shallowCopyWithoutLabels(autoGeneratedName());
    }

    DataPoint shallowCopyWithoutLabels(String newName) {
	return new DataPoint( newName, features, values, 
			      new Vector<Discrimination.Cla>(), dic);
    }

    /** 

	As per the standard Object API:
	<ol>
     <li> If two objects are equal according to the equals(Object) method, then calling the hashCode method on each of the two objects must produce the same integer result.
     <li> It is not required that if two objects are unequal according to the equals(java.lang.Object) method, then calling the hashCode method on each of the two objects must produce distinct integer results. However, the programmer should be aware that producing distinct integer results for unequal objects may improve the performance of hashtables. 
     </ol>

     <p>
     In our case, we base the hash code on the feature vector only, for use
     in DataPointArray

    */
    public int hashCode() {
	int h=0;
	int r=1;
	for(int i=0; i< features.length ; i++) {
	    h += (features[i] + 1) * r * values[i];
	    r = (r << 1);
	}
	return h;
    }


    public boolean equalsFeatures(DataPoint o) {
	if (features.length != o.features.length) return false;
	for(int i=0; i< features.length ; i++) {
	    if (features[i]!=o.features[i] ||values[i]!=o.values[i]) {
		return false;
	    }
	}
	return true;
    }

    /** Full comparison (on features, values, AND labels) */
    public boolean equals(Object _o) {
	if (!(_o instanceof DataPoint)) return false;
	DataPoint o =  (DataPoint)_o;
	return  equalsFeatures(o) &&  classList.equals(o.classList);
    }


    /*
    static public class Pair { 
	public int feature;
	public double value;
    }
  
    Iterator<Pair> iterator() ....
    */
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