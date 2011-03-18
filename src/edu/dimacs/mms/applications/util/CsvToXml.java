package edu.dimacs.mms.applications.util;

import java.util.*;
import java.util.regex.*;
import java.io.*;
import java.text.*;
import org.w3c.dom.Element;

import edu.dimacs.mms.boxer.*;
//import edu.dimacs.mms.borj.*;

public class CsvToXml {

    static void usage() {
	usage(null);
    }

    static void usage(String m) {
	System.out.println("This is the CvsToXml tool for the BOXER toolkit (version " + Version.version+ ")");
	System.out.println("Usage:");
	System.out.println("java [options ]CvsToXml input.csv input-schema.txt out-suite.xml out-data.xml");
	System.out.println("Please read the man pages for more details.");	
	if (m!=null) {
	    System.out.println(m);
	}
	System.exit(1);
    }

    static void main(String argv[]) throws BoxerException, IOException {
	if (argv.length != 4) usage();
	String inputFile=argv[0], inputSchemaFile=argv[1], 
	    outSuiteFile=argv[2], outDataFile=argv[3];
	HashMap<String, FieldMode> h = readSchemaFile( inputSchemaFile);
	
	
    }

    /** Ways of interpreting columns, as found in the schema file */
    enum FieldMode {
	IGNORE, LABEL, DATA;
    };


    /** Reads the schema file that controls how different fields are 
	interpreted
     */
    static HashMap<String, FieldMode> readSchemaFile(String fname) 
	throws IOException, BoxerCSVException {
	HashMap<String, FieldMode> h = new  HashMap<String, FieldMode>();
	Pattern p = Pattern.compile("\\s*([a-zA-Z0-9]+)\\s+([a-zA-Z0-9]+)\\s*");

	LineNumberReader r = new LineNumberReader( new FileReader(fname));
	String s;
	while( (s=r.readLine()) != null) {
	    Matcher m = p.matcher(s);
	    if (!m.matches()) throw new BoxerCSVException("Can't parse the following config line: " + s);
	    String name = m.group(1);
	    String q = m.group(2);	    
	    FieldMode mode=null;
	    try {
		mode =  FieldMode.valueOf(q);
	    } catch(Exception ex) {
		throw new BoxerCSVException("Cannot interpret '" + q + "' as either of IGNORE / LABEL / DATA");
	    }
	    h.put(name, mode);
	}
	r.close();

	return h;
    }


    enum QuoteState { 
	/** no non-blank chars has been encountered in this token yet */
	LEAD_SPACE, 
	    /** have found the openinig quote, and keep reading quoted text */
	    IN_QUOTED_TEXT, 
	    /** have found a first non-blank char; it was not a quote; keep reading non-quoted text */
	    IN_NONQUOTED_TEXT, 
	    /** have found the closing quote; expect only white space until the next separator or EOL */
	    TRAIL_SPACE };

    /** @param s A CSV line. Its elements are separated by a comma;
     * some elements may be quoted with double quotes. A quote
     * character inside an element must be escaped by being repeated.
     */
    String [] parseCSVLine(String s ) throws BoxerCSVException {
	final char q = '"'; // may be used to enclose some values
	final char comma = ',';

	Vector<String> fields = new 	Vector<String>();

	s = s.trim();
	StringBuffer assembled = new StringBuffer();
	QuoteState state = QuoteState.LEAD_SPACE;

	for( int i =0; i< s.length(); i++) {
	    char c = s.charAt(i);

	    // comma always triggers end of field, unless escaped
	    if (c==comma && state != QuoteState.IN_QUOTED_TEXT) {
		fields.add(assembled.toString());
		assembled.setLength(0);
		state = QuoteState.LEAD_SPACE;
		continue;
	    }

	    if (state == QuoteState.LEAD_SPACE) {
		if (Character.isWhitespace(c)) continue;
		else if (c == q) {
		    state = QuoteState.IN_QUOTED_TEXT;
		    continue;
		} else {
		    state = QuoteState.IN_NONQUOTED_TEXT;
		}
	    } else if  (state == QuoteState.IN_NONQUOTED_TEXT) {
		if (c == q) {
		    throw new BoxerCSVException("Double-quote in the middle of a token. Line=" + s);
		}
	    } else if  (state == QuoteState.IN_QUOTED_TEXT) {
		if (c == q) {
		    if (i+1 < s.length() && s.charAt(i+1) == q) {
			// doubled - escaped - quote. Add the 1st, skip the 2nd 
			i++;
		    } else {
			state = QuoteState.TRAIL_SPACE;
			continue;
		    }
		}		    
	    }  else if  (state == QuoteState.TRAIL_SPACE) {
		if (Character.isWhitespace(c)) continue;
		else {
		     throw new BoxerCSVException("Extraneous character '"+c+"' after the end of a quoted field! Line=" + s);
		}
	    }
	    assembled.append(c);
	}

	if (state ==  QuoteState.IN_QUOTED_TEXT) {
	    throw new BoxerCSVException("Double-quoted string not closed. Line=" + s);
	}
	fields.add(assembled.toString());
	return fields.toArray(new String[0]);
    }

    


}