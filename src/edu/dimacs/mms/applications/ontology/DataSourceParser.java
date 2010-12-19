package edu.dimacs.mms.applications.ontology;

import java.util.*;
import java.io.*;
import java.text.*;
import org.w3c.dom.Element;

import edu.dimacs.mms.boxer.*;
import edu.dimacs.mms.borj.*;

/** Methods for converting a data source file into an internal
    representation suitable for use with BOXER. The list of columns is
    converted into a discrimination; cells of a table, into data
    points, assigned to the classes corresponding to the columns.

    This is an abstract class; its concrete derived classes handle
    particular data file formats (CVS etc).
 */
abstract class DataSourceParser {
    /** Results */
    Suite suite;
    /** The discrimination that matters */
    Discrimination dis;
    Vector<DataPoint> data = new Vector<DataPoint>();

    /** Parsing options */
    static InputOptions inputOptions = new InputOptions();


    /** Converts a string (a column name, etc) into something that can be used
	as a legal BOXER ID (a class name etc)
     */
    static String legalName(String s) {
	return s.replaceAll("[@\\s\\^]", "_");
    }

    static  DataSourceParser parseFile(String fname)
	throws BoxerXMLException {
	return parseFile( fname,  null, null);
    } 

    static  DataSourceParser parseFile(String fname, FeatureDictionary dic)
	throws BoxerXMLException {
	return parseFile( fname,  dic, null);
    }

    static  DataSourceParser parseFile(String fname, Suite reuseSuite)
	throws BoxerXMLException {
	return parseFile( fname,  reuseSuite.getDic(), reuseSuite);
    }


    /**
       @param reuseSuite - use this suite (expecting to find all
       classes already defined in it) instead of creating a new one
     */
    static private DataSourceParser parseFile(String fname, FeatureDictionary dic,
				       Suite reuseSuite) throws BoxerXMLException {
	DataSourceParser p = null;

	if (fname.toLowerCase().endsWith(".csv")) {
	    p = new CSVDataSourceParser();
	} else if (fname.toLowerCase().endsWith(".txt")) {
	    p = new TxtDataSourceParser();
	} else {
	    throw new IllegalArgumentException("Don't know what data file format this file name refers to: " + fname);
	}

	if (reuseSuite==null) {
	    p.suite =  new Suite(baseName(fname));
	    if (dic != null) p.suite.setDic(dic);
	} else {
	    // dic is ignored, since there's one in reuseSuite already
	    p.suite = reuseSuite;
	}

	p.init(fname, reuseSuite!=null);
	return p;
    }
    
    /** @param fname "path/xxx.txt"
	@return "xxx"
    */
    static String baseName(String fname) {
	File f = new File(fname);
	return f.getName().replaceAll("\\.[a-zA-Z]+$", "");
    }


    abstract void init(String fname, boolean reuseSuiteOn)  throws BoxerXMLException;
 

    private class BagOfWords extends HashMap<String, Integer> {
	BagOfWords() { super(); }
	void add(String w) {
	    int x = containsKey(w) ? get(w).intValue() : 0;
	    put( w, x+1);
	}

	Vector<DataPoint.FVPair> toVector() throws BoxerXMLException {
	    Vector<DataPoint.FVPair> v = new Vector<DataPoint.FVPair>();
	    for( Map.Entry<String, Integer> e: entrySet()) {
		v.add(new DataPoint.FVPair( suite.getDic().getIdAlways(e.getKey()),  e.getValue().intValue()));
	    }
	    return v;    
	}
	
    }


    DataPoint mkDataPoint(String cell, String rowName, String colName, FeatureDictionary dic) throws BoxerXMLException  {
	// condense white space, remove the special char
	cell = cell.trim().replaceAll("[\\s\\^]+", " ");

	BagOfWords bag = new BagOfWords();

	// words (i.e., tokens separated by "word boundaries")
	String[] words = cell.split("\\b");
	for(String w: words) {
	    w=w.replaceAll("\\s", "");
	    if (w.length()>0) bag.add("@@w." + w);
	}

	// single chars, 2-grams, etc. 2-grams etc don't include 
	// spaces, and don't go across spaces

	words = cell.split("\\s+");
	for(String w: words) {
	    for(int len=1; len<=inputOptions.maxCharSeqLen; len++) {
		for(int start=0; start+len <= w.length(); start++) {
		    String gram = w.substring( start, start+len);
		    // to save space, we only add the "char" prefix
		    // in one special case.
		    if (gram.startsWith("@")) gram = "@@c." + gram;
		    bag.add( gram);
		}
	    }
	}

	if (bag.size()==0) bag.add("@@empty");

	try {
	    DataPoint p=
		new DataPoint(bag.toVector(), suite.getDic(), rowName+"_"+colName);
	    Vector<Discrimination.Cla> vc = new Vector<Discrimination.Cla>();
	    vc.add( dis.getCla(colName));
	    p.setClasses(vc, suite);
	    return p; 
	} catch(BoxerXMLException ex) {
	    // report the problem
	    Logging.error("Error when parsing the following cell: " + cell);
	    throw ex;
	}

    }

    /** Replaces the suite stored here with an equivalent suite that
      was obtained from another source (e,g., a pre-computed learner
      complex), and is essentially equivalent. This method is
      primarily used when we read a pre-computed learner (instead of
      computing one "in-house"), and want to "merge" it with the
      data source description built from the CSV file.
     */
    /*
    void replaceSuiteWithEquivalent(Suite other) {
	if (suite.disCnt() != other.disCnt()) throw new IllegalArgumentException("Suite sizes do not match");
	for(int i=0; i<suite.disCnt(); i++) {
	    if (!suite.getDisc(i).equivalent(other.getDisc(i))) {
		 throw new IllegalArgumentException("Discrimination " +
						    suite.getDisc(i) +
						    " different from " + 
						    other.getDisc(i));
	    }
	}
	int did = suite.getDid( dis);
	suite = other;
	dis = other.getDisc(did);
    }
    */
    
}