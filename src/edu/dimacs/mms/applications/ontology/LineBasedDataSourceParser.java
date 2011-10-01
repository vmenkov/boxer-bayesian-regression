package edu.dimacs.mms.applications.ontology;


import java.util.*;
import java.util.regex.*;
import java.io.*;
import java.text.*;
import org.w3c.dom.Element;

import edu.dimacs.mms.boxer.*;

/** Parses data from a file with one record per line: CSV,
 * pipe-separated, etc. */
abstract class LineBasedDataSourceParser extends DataSourceParser {


    /** Parses one line of the CSV or ismilar file */
    abstract Vector<String>  parseLine(String s) throws BoxerXMLException;

    /** Parses a line-based file (CVS etc) representing a data source;
	adds words and "*-grams" to the feature dictionary, fills the
	Suite representing the ontology (column names) and the vector
	of DataPoint object representing the content of the cells.
 
       @param addColumns If true, will add a class for each column
       in the file's header; if false, expects to find corresponding
       classes already.
       @param colPrefix A prefix (usually, an empty string)
       which should be concatenated with each column names to obtain
       the corresponding class name.
     */
    void readData(String fname, boolean addColumns, String colPrefix) throws BoxerXMLException {

	Logging.info("LineBasedDataSourceParser.readData("+fname+", "+addColumns+", "+colPrefix+")");

	if (dis==null) throw new AssertionError("initDis() had to be called before readData()");

	try {
	    LineNumberReader r = new LineNumberReader( new FileReader(fname));
	    String s;
	    // Read the first line
	    s=r.readLine();
	    // typical header; the first column is interpreted as an ID,
	    // the rest, as class names
	    //"ICN","Incident Date","Subject","Country","City","D","W","H","T"
	    Vector<String> hv = parseLine(s);  
	    if (hv.size() < (inputOptions.ridColumn>0? 2:1)) {
		throw new BoxerXMLException("Only found " + hv.size() + " values in the first CSV line: " + s);
	    }
	    // replace illegal chars
	    for(int i=0; i<hv.size(); i++) {
		String colName = legalName(hv.elementAt(i));
		hv.set( i, colName);
	    }

	    // interpret column names as class names
	    //String disName = baseName(fname);
	    String disName = dis.getName();

	    for(int i=0; i<hv.size(); i++) {
		if (inputOptions.isExcludableCol(i+1)) continue;
		String colName =  colPrefix + hv.elementAt(i);
		

		if (addColumns) {
		    // Record the new class in the discrimination AND in the suite
		    // Logging.info("disName="+ disName+"; Parser: for col="+i+", add class named " + colName);	
		    suite.addClass( disName, colName);
		    //Discrimination.Cla c = suite.getClaAlways( disName, colName, true);
		    //if (c==null) throw new AssertionError("Failed to create class for " + disName + ":" + colName);
		} else {
		    
		    // Verify the existence of each named column
		    if (dis.getCla( colPrefix )==null) {
			throw new IllegalArgumentException("Data mismatch: class " +  hv.elementAt(i) + " found in the data source file " +fname+", but not found in the pre-reas suite!");
		    }
		}
	    }

	    NumberFormat fmt = new DecimalFormat("000000");

	    // the remaining lines, as data. Each cell will be a data point,
	    // assigned to the class corresponding to its  columns.
	    int rowCnt=0;
	    while( (s=r.readLine()) != null) {
		Vector<String> v = parseLine(s);
		String rowName = (inputOptions.ridColumn>0) ?
		    legalName(v.elementAt(inputOptions.ridColumn-1)) :
		    disName + "_" + fmt.format(++rowCnt);
		//		System.out.println("row="+rowCnt+", cell cnt=" + v.size());
		for(int i=0; i<v.size(); i++) {
		    if (inputOptions.isExcludableCol(i+1)) continue;
		    String colName =  colPrefix + hv.elementAt(i);
		    DataPoint p = mkDataPoint(v.elementAt(i), rowName,
					      colName, suite.getDic(), inputOptions);
		    if (p!=null) {
			data.add(p);
		    }
		}
	
	    }
	    r.close();
	} catch(IOException e) {
	    throw new BoxerXMLException("IOException on file "+fname+": " + e.getMessage());
	}

    }
    
}