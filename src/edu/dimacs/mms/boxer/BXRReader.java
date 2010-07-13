package edu.dimacs.mms.boxer;

import java.util.Vector;
import java.util.regex.*;
import java.io.*;

/** Class for reading input files in the format that was used in BXR
    and BMR, as well as for parsing compact-format lists of features
    and class labels in XML input files.

    <p>Input file line format:
    <pre>
    label feature_id:feature_value feature_id:feature_value ...
    </pre>
*/
public class BXRReader {

    /** The character that separate the discrimination name from the
	class name, or the feature label and the feature value, in the
	compact format.  Originally a colon (inherited from BXR), it
	was changed to caret since version 0.7
     */
    static final char PAIR_SEPARATOR = '^';    
    static final String PAIR_SEPARATOR_STRING = ""+PAIR_SEPARATOR;
    static final String PAIR_SEPARATOR_REGEX = 
	(PAIR_SEPARATOR == '^' ? "\\":"") + PAIR_SEPARATOR;

    /** To support BMR data, where the separator is still a colon, not
     * a caret... */
    static final char BMR_PAIR_SEPARATOR = ':';    


    private static Pattern nonWsPat = Pattern.compile("\\s*(\\S+)");

    /** 
	@param s "label feature_id:value feature_id:value ...." 
	@param  PAIR_SEPARATOR the character (typically, colon or caret) that is expected to separate feature IDs and feature values
    */
    public static DataPoint readDataPoint(String s, Suite suite,  boolean isDefinitional, char PAIR_SEPARATOR) throws BoxerXMLException {
	Matcher m = nonWsPat.matcher(s);
	if (!m.lookingAt()) {
	    throw new IllegalArgumentException("This string has no class label: " +s);
	} 
	String label = m.group(1);
	
	DataPoint p=new DataPoint( readFeatureVector
				   (s.substring(m.end()),suite.getDic(), PAIR_SEPARATOR),
				   suite.getDic());

	Vector<Discrimination.Cla> v=new Vector<Discrimination.Cla>();
	Discrimination.Cla c = suite.getClaAlways(null, /*  "default"*/
						  label, isDefinitional);
	v.add( c );
	p.setClasses( v, suite);
	return p;
    }

    
    static private Vector<DataPoint.FVPair> 
	readFeatureVector(String s, FeatureDictionary dic, char PAIR_SEPARATOR)
	throws BoxerXMLException {

	String ps_regex = 
	    (PAIR_SEPARATOR == '^' ? "\\":"") + PAIR_SEPARATOR;
	// Pattern for feature names and values 
	Pattern fpat = Pattern.compile("\\s*([^\\s:]+)\\s*"+
				       ps_regex +
				       "\\s*([0-9\\.eE]+)");
    
	int pos=0;
	Vector<DataPoint.FVPair> v = new Vector<DataPoint.FVPair>();	    
	dic.addDummyCompoIfRequired(v);

	while( pos < s.length() ) {
	    
	    Matcher m = fpat.matcher(s.subSequence(pos, s.length()));
	    boolean b = m.lookingAt();
	    if (!b) {
		// end of line?
		String rem = s.substring(pos).trim();
		if (rem.length()>0) {
		    // The rest of the line is unparsable!
		    throw new IllegalArgumentException("Could not parse string `"+s+"' as a list of feature:value pairs. Unparsable remainder: " + rem);
		}
		break;
	    } 
	    String fs = m.group(1), vals = m.group(2);
	    // Are we reading feature IDs, or do we need to map labels to IDs?
	    int fid = (dic == null)? Integer.parseInt(fs) :dic.getIdAlways(fs);
	    double fval = Double.parseDouble(vals);
	    v.add(new DataPoint.FVPair(fid, fval));
	    pos += m.end();
	}
	return v;
    }

    /** Parses a string in format "a1:b1 a2:b2 a3:b3 ...", where the
     current PAIR_SEPARATOR is actually used instead of the semicolon.
     Adds results to v, a vector of String pairs. If v is not
     supplied, allocates a new vector.*/
    static Vector<String[]> readPairs(String s, Vector<String[]> v) {
    
	if (v==null)   v = new Vector<String[]>();

	String tokens[] = s.split("\\s+");
	for(String token: tokens) {
	    if (token.equals("")) continue;
	    String[] pair = token.split(PAIR_SEPARATOR_REGEX);
	    if (pair.length == 1) {
		// implicit class
		pair = new String[] { null, pair[0] };
	    } else if (pair.length != 2) {
		throw new IllegalArgumentException("Could not parse string '"+s+"' as a list of dis"+PAIR_SEPARATOR+"class pairs. Too many colons/carets? Unparsable token: " + token);
	    }
	    v.add(pair);
	}

	return v;
    }

    /** Builds a vector of DataPoints out of the content of an BBR/BMR
	file. We support it so that one can easily run BOXER on
	BBR/BMR/BXR data files.<p>

	<p> The Data file format for training or testing is similar to
	that used by Joachims' <a
	href="http://svmlight.joachims.org/">SVMlight software</a> for
	training support vector machines (SVM). Each line represents
	an instance. The line format is:

<pre> 
         <label>{ <feature_id>:<value>}
 </pre> 

         The label here may be any integer; feature_id must be a
	 positive integer; each value is a number in double float
	 notation. Lines starting with '#' are ignored and can be used
	 for comments.

	@param fname The name of the file to read
	@param isDefinitional Set this flag to true if you're reading the training set. This will affect the way "new categories" encountered in the file are processed
    */
    public static Vector <DataPoint> readDataFileBMR(String fname,    
						     Suite suite, 
						     boolean isDefinitional)
	throws IOException, BoxerXMLException {


	LineNumberReader r = new LineNumberReader( new FileReader(fname));
	Vector<DataPoint> v = new Vector<DataPoint>();
	String s=null;
	int cnt=0;
	while( (s=r.readLine()) != null) {
	    s=s.trim();
	    if (s.equals("")) continue;
	    if (s.startsWith("#")) continue;
	    cnt++;
	    DataPoint p = readDataPoint(s, suite, isDefinitional, BMR_PAIR_SEPARATOR);
	    v.add(p);	
	}
	return v;
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