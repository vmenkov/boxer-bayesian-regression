package edu.dimacs.mms.applications.ontology;

import edu.dimacs.mms.boxer.ParseConfig;
import edu.dimacs.mms.applications.util.TokenizerOptions;

/** An instance of this class stores various flags controlling the
    processing of input data source files. */
class InputOptions extends TokenizerOptions {
    /** 1-based position of the column that's used as the record
	ID. If 0, then no such column exists in the data source,
	and we use generated IDs instead. The existence of the
	Record ID column by itself does not mean that we will exclude
	it from the ontology.
    */
    int ridColumn=0;
    /** 1-based positions of any columns that need to be excluded from
	the ontology used for matching. The list may or may not 
	include the record ID column.
    */ 
    int [] excludableColumns=new int[0];
    /** @param s "col1[:col2[:...]]
     */
    void setExcludableColumns(String s) {
	if (s==null || s.length()==0) return;
	String[] q= s.split(":");
	excludableColumns=new int[q.length];
	int i=0;
	for(String z: q) {
	   excludableColumns[i++] = Integer.parseInt(z);
	}
    }

    /** @param k 1-based col id */
    boolean isExcludableCol(int k) {
	for(int z:  excludableColumns) {
	    if (k==z) return true;
	}
	return false;
    }

    /** Initializes config params from the Java system properties
     */
    public void init(ParseConfig ht) {
	super.init(ht); // init TokenizerOptions 
	ridColumn =  ht.getOption( "input.rid", ridColumn );
	setExcludableColumns( ht.getOption("input.exclude", ""));
    }

    String describe() {
	return "Empty cells: " + 
	    (emptySkip ? "ignored": emptySpecial ? "special feature":  "zero vector") +
	    (useWords? "; words" : "; no words") +
	    "; nGrams length<=" + maxCharSeqLen;		  
    }

}

