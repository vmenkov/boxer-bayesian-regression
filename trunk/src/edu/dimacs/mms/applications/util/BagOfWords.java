package edu.dimacs.mms.applications.util;

import java.util.*;
import java.util.regex.*;
import java.io.*;
import java.text.*;
//import org.w3c.dom.Element;

import edu.dimacs.mms.boxer.*;

/** An auxiliary class used for converting text into a DataPoint */
public class BagOfWords extends HashMap<String, Double> {
    BagOfWords() { super(); }
    /*
    private void add(String w) {
	add(w, 1.0);
    }
    */
    private void add(String w, double weight) {
	double x = containsKey(w) ? get(w).doubleValue() : 0;
	put( w, x+weight);
    }
    
    public Vector<DataPoint.FVPair> toVector(FeatureDictionary dic) throws BoxerXMLException {
	Vector<DataPoint.FVPair> v = new Vector<DataPoint.FVPair>();
	for( Map.Entry<String, Double> e: entrySet()) {
	    v.add(new DataPoint.FVPair( dic.getIdAlways(e.getKey()),  e.getValue().doubleValue()));
	}
	return v;    
    }

    
    /** Returns a DataPointBagOfWords based on the content of a
	cell. Depending on the {@link TokenizerOptions}, the BagOfWords will
	include features corresponding to words and/or n-grams of the
	cell's text, or a special feature indicating an empty
	cell. The method will return null if if the cell is empty, and
	the emptySkip flag is on.
	@param cell Text to convert to a "bag of words"
     */
    public static BagOfWords mkBagOfWords(String cell,
					  TokenizerOptions options)     {

	final double wordWeight = 1;

	// condense white space, remove the special char
	cell = cell.trim().replaceAll("[\\s\\^]+", " ");

	BagOfWords bag = new BagOfWords();

	// words (i.e., tokens separated by "word boundaries")
	if (options.useWords) {
	    String[] words = cell.split("\\b");
	    for(String w: words) {
		w=w.replaceAll("\\s", "");
		if (w.length()>0) bag.add("@@w." + w, wordWeight);
	    }
	}

	// If this flag is true, '^' and '$' are recorded 1-grams, too
	final boolean USE_EOW_MARKERS=true;


	/** Lengths of n-grams we want to work with */
	int gramLengths[] = new int[options.maxCharSeqLen];
	for(int len=1; len<=options.maxCharSeqLen; len++) {
	    gramLengths[len-1]=len;
	}
	final double gramWeight = 1;

	/*
	int gramLengths[]={4};
	final double gramWeight = 0.01;
	*/

	// single chars, 2-grams, etc. 2-grams etc don't include 
	// spaces, and don't go across spaces

	String[] words = cell.split("\\s+");
	for(String w: words) {
	    for(int len:  gramLengths) {
		for(int start=0; start+len <= w.length(); start++) {
		    String gram = w.substring( start, start+len);
		    // to save space, we only add the "char" prefix
		    // in one special case.
		    if (gram.startsWith("@")) gram = "@@c." + gram;
		    bag.add( gram, gramWeight);
		}
		if (len > w.length() + 1) continue;
		if (len==1 && USE_EOW_MARKERS ||  len>1) {
		    // special beginning-of-word and end-of-word n-grams
		    bag.add( "@@cb." + w.substring(0, len-1), gramWeight);
		    bag.add( "@@ce." + w.substring(w.length()-(len-1)), gramWeight);
		}
	    }
	}

	if (bag.size()==0) {
	    if (options.emptySkip) return null;
	    else if (options.emptySpecial) bag.add("@@empty", wordWeight);
	}
	return bag;

    }
    
	
}


