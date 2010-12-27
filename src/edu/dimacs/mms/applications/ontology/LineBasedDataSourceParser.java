package edu.dimacs.mms.applications.ontology;


import java.util.*;
import java.util.regex.*;
import java.io.*;
import java.text.*;
import org.w3c.dom.Element;

import edu.dimacs.mms.boxer.*;
import edu.dimacs.mms.borj.*;


/** Parses data from a file with one record per line: CSV, pipe-separated, etc */
abstract class LineBasedDataSourceParser extends DataSourceParser {


    /** Parses one line of the CSV or ismilar file */
    abstract Vector<String>  parseLine(String s) throws BoxerXMLException;

    /** Parses a line-based file (CVS etc) representing a data source;
	adds words and "*-grams" to the feature dictionary, fills the
	Suite representing the ontology (column names) and the vector
	of DataPoint object representing the content of the cells.
     */
    void init(String fname,boolean reuseSuiteOn ) throws BoxerXMLException {

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
	    String disName = baseName(fname);
	    Logging.info("Parser: creating discrimination with disName=" + disName);

	    if (reuseSuiteOn) {
		dis = suite.getDisc(disName);
		if (dis == null) {
		    throw new IllegalArgumentException("No discrimination named " + disName + " was found in the pre-read model file");
		}
		for(int i=0; i<hv.size(); i++) {
		    if (inputOptions.isExcludableCol(i+1)) continue;
		    //Logging.info("Parser: for col="+i+", add class named " + hv.elementAt(i));	
		    if (dis.getCla( hv.elementAt(i))==null) {
			throw new IllegalArgumentException("Data mismatch: class " +  hv.elementAt(i) + " found in the data source file " +fname+", but not found in the pre-reas suite!");
		    }
		}
	    } else {
		dis = suite.addDiscrimination(disName);
		for(int i=0; i<hv.size(); i++) {
		    if (inputOptions.isExcludableCol(i+1)) continue;
		    //Logging.info("Parser: for col="+i+", add class named " + hv.elementAt(i));	
		    dis.addClass( hv.elementAt(i));
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
		    DataPoint p = mkDataPoint(v.elementAt(i), rowName,
					      hv.elementAt(i), suite.getDic());
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