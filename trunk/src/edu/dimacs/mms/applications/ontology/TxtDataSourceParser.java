package edu.dimacs.mms.applications.ontology;

import java.util.*;
import java.util.regex.*;
//import java.io.*;

import edu.dimacs.mms.boxer.*;

class TxtDataSourceParser extends LineBasedDataSourceParser {

  static private Pattern p1 = Pattern.compile("(.*?)\\|");


    /** Parses one line of the file. The column sep is the '|' char.

	We don't use String []w =s.split("\\|"); because split() ignores
	the trailing empty fields!
    */
    Vector<String>  parseLine(String s) throws BoxerXMLException {

	Matcher m = p1.matcher(s);
	Vector<String> v =  new Vector<String>();
	int from=0;
	while(m.find()) {
	    from=m.end();
	    String g = m.group(1);
	    //System.out.println("["+g+"]");
	    v.add(g);
	}

	v.add( s.substring(from));
	return v;
    }

 
}