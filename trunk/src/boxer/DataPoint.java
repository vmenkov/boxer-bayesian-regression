package boxer;

import java.util.*;
import java.io.*;
import java.text.*;

// for XML output
import org.w3c.dom.*;
import org.apache.xerces.dom.DocumentImpl;
import org.apache.xml.serialize.*;


/** A DataPoint represents a document, or some other object
    ("example") being classified.  Once read in and stored, DataPoint
    objects can be passed to learners for training (be "absorbed") or
    scoring.

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
    boxer.FeatureDictionary} of the suite in whose context the
    DataPoint has been created.

    <p>
    A DataPoint object also contains a list of references to actually
    existing {@link boxer.Discrimination.Cla class objects} (which, in
    their turn, belong to actually existing {@link
    boxer.Discrimination discrimination objects}), rather than merely
    strings with discr/class names. These class lists associated with
    the DataPoint can be used by BOXER depending on the context in
    which the DataPoint is used (i.e., BOXER would interpret them as
    the training exmaple's labels when absorbing the example, or would
    ignore them when scoring the example as a test example).
  
    <p>
    A BOXER application would typically read a list of data points (a
    data set) from an XML file, using the methods of the {@link boxer.ParseXML}
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
    /** Feature ids, as per the FeatureDictionary, in increasing order */
    int[] features;
    /** Feature values */
    double[] values;
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

    double dotProduct( DataPoint p) {
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
    double normSquare() {
	double sum=0;
	for(double v: values)   sum += v * v;
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

      @param v A Vector of feature/value pairs describing the new data point
      @param _dic the FeatureDictionary of the suite in whose context
      the DataPoint will be used.
     */
    public DataPoint(Vector <FVPair> _v, FeatureDictionary _dic) {
	this(_v, _dic, autoGeneratedName());
    }

    /** A constructor used in parsers etc. It will also sort pairs
     * according to the feature id, so that dot product, binary search, and
     other operations relying on the right order will work. 

      @param v A Vector of feature/value pairs describing the new data point
      @param _dic the FeatureDictionary of the suite in whose context
      the DataPoint will be used.
      @param name The name (example ID) to be assigned to the new data point
    */
     public DataPoint(Vector <FVPair> _v, FeatureDictionary _dic,String _name) {
	 name = _name;
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

	@param the feature name / feature value map describing the new
	data point
	@param dic the FeatureDictionary of the suite in whose context
	the DataPoint will be used.
	@param name The name to assign the new data point
    */
    static public DataPoint makeDataPoint(HashMap <String, Integer> words, 
			       FeatureDictionary dic, String name) {
       
	Vector <FVPair> v=new  Vector <FVPair>();
	for( String z : words.keySet()) {
	    v.add(new FVPair(dic.getIdAlways(z), (double)words.get(z).intValue()));
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
       classes - which means that the caller should never modified
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
       respect to those "latter-day" discriminarions get their default
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
	    b.append(" " + dic.getLabel(features[i]) + ":");
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


    /** Describes the data point as an element of an XML document.  As
	far as class labels go, only explicitly stored labels (not
	discirmination defaults) are written into the XML.
     */
     public org.w3c.dom.Element toXML(Document xmldoc) {
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
		 if (b.length()>0) b.append(" ");
		 b.append(dic.getLabel(features[i]));
		 b.append((double)(int)values[i] == values[i] ? 
			  ":" + (int)values[i] :
			  ":" + values[i] );
	     }
	     ef.appendChild(xmldoc.createTextNode(b.toString()));
	     e.appendChild(ef);
	 }

	return e;
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

    public long memoryEstimate() {
	return Sizeof.OBJ + 2* Sizeof.OBJREF +
	    Sizeof.sizeof(values) + Sizeof.sizeof(features);
    }

     /** An auxiliary class used in generating compact human-readable
      * representation of the score vector */
     private static class ScoreRun {
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
      * classes array */
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
		 } else {
		     if (!run.sameRun(p)) b.append( run.dumpAny());
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

       To compute it for a discrimination on a test set do this:

       1. For each test example find the probability the model assigns to the single class that is correct for that example.  Take the log base e of that probability.
       
       2. Sum this quantity across the test set.

       3. Divide by number of test examples to get the mean log likelikelihood.
       
       This is a negative number with maximum value 0.0, so graphs of this value for models trained from nested training sets will show a line more or less climbing toward 0.

       This is in some sense the most natural measure of generalization for logistic regression.

    */
    public void addLogLik(double probLog[][], Suite suite, int logLikCnt[], double logLik[]) {
	 for(int j=0;j<probLog.length; j++) {
	     Discrimination dis = suite.getDisc(j);
	     Discrimination.Cla trueC = claForDisc(dis);
	     if (trueC==null) {
		 System.out.println("No 'true class' label is stored for dis=" + dis.name + " in data point x="+name);
		 continue;
		 //throw new IllegalArgumentException("No 'true class' label is stored for dis=" + dis.name + " in data point x="+name);
	     } else {
		 logLikCnt[j]++;
		 int trueCPos = trueC.getPos();
		 logLik[j] += probLog[j][ trueCPos];
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
       class name etc)

       <ol>
       <li>  RUN_ID : This is something that the user is required to supply to BORJ.

       <li>  EXAMPLE_ID: a unique ID that either is taken from the example or, if the example doesn't have one, BORJ creates one. (Perhaps by concatenating a number to the RUN_ID.)

       <li>  SUITE_NAME: Name of the Suite.

       <li>  DISCRIMINATION_NAME : Name of the discrimination.

       <li>  LINE_CLASS_NAME : Name of the class that this line is reporting on.

       <li>  PROB : The probability, p, that the model predicts for the example EXAMPLE_ID belonging the class LINE_CLASS_NAME from discrimination DISCRIMINATION_NAME.  BOXER provides this and it's always in the closed interval [0.0, 1.0].

       <li>  "PRED:"PREDICTED_CLASS_NAME : the class that BOXER would pick for the example if forced to choose.  The main purpose of this is to allow consistent handling of tied scores.  An application using BOXER can always override its choice.

       <li>  "TRUE:"TRUE_CLASS_NAME :  inserted by BORJ when known.
  </ol>
  
      */
     public void reportScoresAsText(double prob[][], Suite suite, String runid, PrintWriter out) {
	 Discrimination.Cla[] chosen=interpretScores(prob,suite);

	 for(int did = 0; did < prob.length; did ++) {	     
	     Discrimination dis = suite.getDisc(did);
	     for(int i=0; i<prob[did].length; i++) {		 
		 Discrimination.Cla trueCla = claForDisc(dis);
		 String q[] = {runid,  name,  suite.name, dis.name, 
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

}