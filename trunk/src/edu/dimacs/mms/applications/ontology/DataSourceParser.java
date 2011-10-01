package edu.dimacs.mms.applications.ontology;

import java.util.*;
import java.io.*;
import java.text.*;
import org.w3c.dom.Element;

import edu.dimacs.mms.boxer.*;
import edu.dimacs.mms.applications.util.TokenizerOptions;
import edu.dimacs.mms.applications.util.BagOfWords;

/** A DataSourceParser instance is a wrapper for all the data obtained
    from a data soruce description file. This class contains methods
    for converting a data source file into an internal representation
    suitable for use with BOXER. The list of columns is converted into
    a discrimination; cells of a table, into data points, assigned to
    the classes corresponding to the columns.

    This is an abstract class; its concrete derived classes handle
    particular data file formats (CVS etc).
 */
abstract class DataSourceParser {
    /** Results */
    Suite suite;
    /** The only discrimination that matters */
    Discrimination dis;
    /** The ID of the only discrimination that matters */
    int did() { return  suite.getDid( dis); }
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
	return parseFile( fname,  null, null, "");
    } 

    static  DataSourceParser parseFile(String fname, FeatureDictionary dic)
	throws BoxerXMLException {
	return parseFile( fname,  dic, null, "");
    }

    static  DataSourceParser parseFile(String fname, Suite reuseSuite)
	throws BoxerXMLException {
	return parseFile( fname,  reuseSuite.getDic(), reuseSuite, "");
    }


    /** At most, only one of dic and reuseSuite should be non-null.

       @param reuseSuite - use this suite (expecting to find all
       classes already defined in it) instead of creating a new one
     */
    static DataSourceParser parseFile(String fname, FeatureDictionary dic,
				      Suite reuseSuite, String colPrefix)
	throws BoxerXMLException {
	if (dic!=null && reuseSuite!=null) throw new  IllegalArgumentException("At most, only one of dic and reuseSuite should be non-null");

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

	p.initDis(fname, reuseSuite!=null);
	p.readData(fname, reuseSuite==null, colPrefix);
	return p;
    }
    
    /** @param fname "path/xxx.txt"
	@return "xxx"
    */
    static String baseName(String fname) {
	File f = new File(fname);
	return f.getName().replaceAll("\\.[a-zA-Z]+$", "");
    }


    /**  @param reuseSuiteOn If true, simply verify that the Suite of this DS already
     *  contains a discrimination matching fname. If false, create such a discrimination.
     */
    private void initDis(String fname, boolean reuseSuiteOn) throws BoxerXMLException {

	String disName = baseName(fname);
	if (reuseSuiteOn) {
	    Logging.info("Parser: finding discrimination with disName=" + disName);
	    dis = suite.getDisc(disName);
	    if (dis == null) {
		throw new IllegalArgumentException("No discrimination named " + disName + " was found in the pre-read model file");
	    }       
	} else {
	    Logging.info("Parser: creating discrimination with disName=" + disName);
	    dis = suite.addDiscrimination(disName);
	    if (dis==null) throw new AssertionError("Discrimination lost?");
	}
    }


    /** Reads in data from a data file representing a data source;
	adds words and "*-grams" to the feature dictionary, fills the
	Suite representing the ontology (column names) and the vector
	of DataPoint object representing the content of the cells.
     */
    abstract void readData(String fname, boolean addColumns, 
			   String classNamePrefix
			   ) throws BoxerXMLException;

    /** Returns a DataPoint based on the content of a cell. Based on
	{@link InputOptions}, the DataPoint will include features
	corresponding to words and/or n-grams of the cell's text, or a
	special feature indicating an empty cell. The method will
	return null if if the cell is empty, and the emptySkip flag is on.
     */
    DataPoint mkDataPoint(String cell, String rowName, String colName, FeatureDictionary dic, TokenizerOptions options) throws BoxerXMLException  {

	BagOfWords bag = BagOfWords.mkBagOfWords( cell, inputOptions);

	if (bag==null) return null;

	try {
	    String pointName = rowName+"_"+colName;
	    DataPoint p= new DataPoint(bag.toVector(dic), dic, pointName);
	    Vector<Discrimination.Cla> vc = new Vector<Discrimination.Cla>();
	    vc.add( dis.getCla(colName));
	    p.setClasses(vc, suite);
	    return p; 
	} catch(BoxerXMLException ex) {
	    // report the problem
	    Logging.error("Error when trying to convert the following text into a DataPoint: " + cell);
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
   
    String getColName(int j, String prefix) {
	String s = dis.getClaById(j).getName();
	if (prefix!=null && prefix.length()>0) {
	    if (s.startsWith(prefix)) return s.substring(prefix.length());
	    else throw new IllegalArgumentException("Class no. " + j + " is named '"+s+"', and does not have prefix '"+prefix+"'");
	} else return s;
    }
 
}