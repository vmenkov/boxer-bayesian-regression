package edu.dimacs.mms.applications.ontology;


import java.util.*;
import java.util.regex.*;
import java.io.*;
import java.text.*;
import org.w3c.dom.Element;

import edu.dimacs.mms.boxer.*;
import edu.dimacs.mms.borj.*;


class CSVDataSourceParser extends LineBasedDataSourceParser {


    static private Pattern pAll = Pattern.compile("(\".+?\",)*(\".+?\")\\s*");
    static private Pattern p = Pattern.compile("\"(.+?)\"");

    /** Parses one line of the CSV file */
    Vector<String>  parseLine(String s) throws BoxerXMLException {
        Matcher mAll = pAll.matcher(s);
	if (!mAll.matches()) throw new BoxerXMLException("Can't parse the following line as CSV: " + s);

	Matcher m = p.matcher(s);
	Vector<String> v =  new Vector<String>();
	while(m.find()) {
	    String g = m.group(1);
	    //System.out.println("["+g+"]");
	    v.add(g);
	}
	return v;
    }

}