package boxer;

import java.util.Vector;
import java.util.regex.*;

/** Class for reading input files in the format that was used in BXR */
public class BXRReader {

    static Pattern nonWsPat = Pattern.compile("\\s*(\\S+)");

    //static Pattern wsPat = Pattern.compile("\\s*");
    /** Pattern for feature names and values */
    static Pattern fpat = Pattern.compile("\\s*([^\\s:]+)\\s*:\\s*([0-9\\.eE]+)");
    /** Pattern for discrimination and class labels */
    //static Pattern cpat = Pattern.compile("\\s*([^\\s:]+)\\s*:\\s*([^\\s:]+)");

    /** 
	@param s "label feature_id:value feature_id:value ...." 
	Feature IDs are positive integers 
    */
    public DataPoint readDataPoint(String s, Suite suite, boolean isDefinitional) {
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
						      FeatureDictionary dic) {
    
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

    /** Parses a string in format "a1:b1  a2:b2 a3:b3 ..." 
     Adds results to v, a vector of String pairs. If v is not supplied,
    allocates a new vector.*/
    static Vector<String[]> readPairs(String s, Vector<String[]> v) {
    
	if (v==null)   v = new Vector<String[]>();

	/*
	for(int pos=0;  pos < s.length(); ) {
	    Matcher m = cpat.matcher(s.subSequence(pos, s.length()));
	    if (!m.lookingAt()) { 		// end of line?
		String rem = s.substring(pos).trim();
		if (rem.length()==0) break;
		// The rest of the line is unparsable!
		throw new IllegalArgumentException("Could not parse string s as a list of dis:class pairs. Unparsable remainder: " + rem);
	    } 
	    String c[] = new String[] {m.group(1), m.group(2)};
	    v.add(c);
	    pos += m.end();
	}
	*/
	
	String tokens[] = s.split("\\s+");
	for(String token: tokens) {
	    if (token.equals("")) continue;
	    String[] pair = token.split(":");
	    if (pair.length == 1) {
		// implicit class
		pair = new String[] { null, pair[0] };
	    } else if (pair.length != 2) {
		throw new IllegalArgumentException("Could not parse string '"+s+"' as a list of dis:class pairs. Too many colons? Unparsable token: " + token);
	    }
	    v.add(pair);
	}

	return v;
    }



}