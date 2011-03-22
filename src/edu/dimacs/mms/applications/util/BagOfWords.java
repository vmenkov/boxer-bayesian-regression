package edu.dimacs.mms.applications.util;

import java.util.*;
import java.util.regex.*;
import java.io.*;
import java.text.*;
//import org.w3c.dom.Element;

import edu.dimacs.mms.boxer.*;

/** An auxiliary class used for converting text into a DataPoint */
public class BagOfWords extends HashMap<String, Integer> {
    BagOfWords() { super(); }
    void add(String w) {
	int x = containsKey(w) ? get(w).intValue() : 0;
	put( w, x+1);
    }
    
    public Vector<DataPoint.FVPair> toVector(FeatureDictionary dic) throws BoxerXMLException {
	Vector<DataPoint.FVPair> v = new Vector<DataPoint.FVPair>();
	for( Map.Entry<String, Integer> e: entrySet()) {
	    v.add(new DataPoint.FVPair( dic.getIdAlways(e.getKey()),  e.getValue().intValue()));
	}
	return v;    
    }

    
    /** Returns a DataPointBagOfWords based on the content of a
	cell. Depending on {@link InputOptions}, the BagOfWords will
	include features corresponding to words and/or n-grams of the
	cell's text, or a special feature indicating an empty
	cell. The method will return null if if the cell is empty, and
	the emptySkip flag is on.
	@param cell Text to convert to a "bag of words"
     */
    public static BagOfWords mkBagOfWords(String cell,
					  TokenizerOptions options)     {
	// condense white space, remove the special char
	cell = cell.trim().replaceAll("[\\s\\^]+", " ");

	BagOfWords bag = new BagOfWords();

	// words (i.e., tokens separated by "word boundaries")
	if (options.useWords) {
	    String[] words = cell.split("\\b");
	    for(String w: words) {
		w=w.replaceAll("\\s", "");
		if (w.length()>0) bag.add("@@w." + w);
	    }
	}

	// single chars, 2-grams, etc. 2-grams etc don't include 
	// spaces, and don't go across spaces

	String[] words = cell.split("\\s+");
	for(String w: words) {
	    for(int len=1; len<=options.maxCharSeqLen; len++) {
		for(int start=0; start+len <= w.length(); start++) {
		    String gram = w.substring( start, start+len);
		    // to save space, we only add the "char" prefix
		    // in one special case.
		    if (gram.startsWith("@")) gram = "@@c." + gram;
		    bag.add( gram);
		}
	    }
	}

	if (bag.size()==0) {
	    if (options.emptySkip) return null;
	    else if (options.emptySpecial) bag.add("@@empty");
	}
	return bag;

    }
    
	
}


