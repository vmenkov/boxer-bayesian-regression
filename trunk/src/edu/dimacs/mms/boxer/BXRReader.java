package edu.dimacs.mms.boxer;

import java.util.Vector;
import java.util.regex.*;

/** Class for reading input files in the format that was used in BXR,
    as well as for parsing compact-format lists of features and class
    labels in XML input files.*/
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

    private static Pattern nonWsPat = Pattern.compile("\\s*(\\S+)");

    /** Pattern for feature names and values */
    private static Pattern fpat = Pattern.compile("\\s*([^\\s:]+)\\s*"+
					  PAIR_SEPARATOR_REGEX +
					  "\\s*([0-9\\.eE]+)");

    /** 
	@param s "label feature_id:value feature_id:value ...." 
	Feature IDs are positive integers 
    */
    public DataPoint readDataPoint(String s, Suite suite, boolean isDefinitional) throws BoxerXMLException {
	Matcher m = nonWsPat.matcher(s);
	if (!m.lookingAt()) {
	    throw new IllegalArgumentException("This string has no class label: " +s);
	} 
	String label = m.group(1);
	
	DataPoint p=new DataPoint( readFeatureVector(s.substring(m.end()),suite.getDic()),
				   suite.getDic());

	Vector<Discrimination.Cla> v=new Vector<Discrimination.Cla>();
	Discrimination.Cla c = suite.getClaAlways(null /*"default"*/, label, isDefinitional);
	v.add( c );
	p.setClasses( v, suite);
	return p;
    }

    
    static private Vector<DataPoint.FVPair> readFeatureVector(String s, 
						      FeatureDictionary dic)
    throws BoxerXMLException {
    
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
		    throw new IllegalArgumentException("Could not parse string s as a list of feature:value pairs. Unparsable remainder: " + rem);
		}
		break;
	    } 
	    String fs = m.group(1), vals = m.group(2);
	    // Are we reading feature IDs, or do we need to map labels to IDs?
	    int fid = (dic == null)? Integer.parseInt(fs) :dic.getIdAlways(fs);
	    double fval = Double.parseDouble(vals);
	    v.add(new DataPoint.FVPair(fid, fval));
	    pos = m.end();
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