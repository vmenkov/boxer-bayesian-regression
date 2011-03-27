package edu.dimacs.mms.applications.util;

import java.util.*;
import java.util.regex.*;
import java.io.*;
//import java.text.*;
//import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import edu.dimacs.mms.boxer.*;
//import edu.dimacs.mms.borj.*;

/** A utility for converting a CSV file (one data point per line) into
 * a BOXER-format XML dataset file.

 <pre>
 -DdicIn=...
 -DdicOut=...
 </pre>
 
 */
public class CsvToXml {

    static void usage() {
	usage(null);
    }

    static void usage(String m) {
	System.out.println("This is the CsvToXml tool for the BOXER toolkit (version " + Version.version+ ")");
	System.out.println("Usage:");
	System.out.println("java [options] CsvToXml input.csv input-schema.txt out-suite.xml out-data.xml");
	System.out.println("Please read the man pages for more details.");	
	if (m!=null) {
	    System.out.println(m);
	}
	System.exit(1);
    }

    /** @param fname "path/xxx.txt"
	@return "xxx"
    */
    static private String baseName(String fname) {
	File f = new File(fname);
	return f.getName().replaceAll("\\.[a-zA-Z]+$", "");
    }


    static public void main(String argv[]) throws BoxerException, BoxerXMLException, IOException, SAXException {

	// process options
	ParseConfig ht = new ParseConfig();
	Suite.verbosity = ht.getOption("verbosity", 0);
	TokenizerOptions options = new TokenizerOptions();
	options.init(ht); 

	// command-line arguments
	if (argv.length != 4) usage();
	String inputFile=argv[0], inputSchemaFile=argv[1], 
	    outSuiteFile=argv[2], outDataFile=argv[3];
	CsvSchema schema  = new CsvSchema( inputSchemaFile);

	String suiteName = baseName(inputFile);
	DataPoint.setDefaultNameBase(suiteName); // for naming points
	Suite suite = new Suite(suiteName);	
   
	String dicFileName =ht.getOption("dicIn", null);	
	
	if ( dicFileName != null) {
	    suite.setDic(new  FeatureDictionary(new File(dicFileName)));
	}


	Vector<DataPoint> data=readInputFile(inputFile, schema, suite, options);

	suite.saveAsXML(outSuiteFile);


	String dicOutName =ht.getOption("dicOut", null);	
	
	if ( dicOutName != null) {
	    suite.getDic().saveAsXML(dicOutName);
	}


	final String BMR_EXT = ".bmr";
	if (outDataFile.endsWith(BMR_EXT)) {
	    // Save as BMR, in several files (one per each "real" disc)
	    String base = outDataFile.substring(0, outDataFile.length()-BMR_EXT.length());
	    int disCnt = suite.disCnt();
	    for(int i=0; i<disCnt; i++) {
		Discrimination dis = suite.getDisc(i);
		if (dis==suite.getFallback()) continue;
		String outName = base + "_" + dis.getName() + BMR_EXT;
		final boolean numericCla=false;
		DataPoint.saveAsBMR(data, dis, numericCla, outName); 
	    }

	} else {
	    // XML
	    DataPoint.saveAsXML(data, suiteName, outDataFile); 
	}

	
    }

    /** Ways of interpreting columns, as found in the schema file */
    enum FieldMode {
	IGNORE, NAME, LABEL, DATA;
    };

    /** The schema, read from a schema file, controls how different
	fields of a CSV file are interpreted.
     */
    static class CsvSchema extends HashMap<String, FieldMode>  {
  

	static private Pattern p = Pattern.compile("\\s*([a-zA-Z0-9]+)\\s+([a-zA-Z0-9]+)\\s*");

    /** Reads the schema file that controls how different fields are 
	interpreted
     */
	CsvSchema (String fname) 
	    throws IOException, BoxerCSVException {

	    LineNumberReader r = new LineNumberReader( new FileReader(fname));
	    String s;
	    while( (s=r.readLine()) != null) {
		s=s.trim();
		if (s.equals("") || s.startsWith("#")) continue; // skip empty
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
		put(name, mode);
	    }
	    r.close();
	}
    }


    static Vector<DataPoint> readInputFile(String fname, CsvSchema schema,
					   Suite suite, TokenizerOptions options)
	throws BoxerCSVException, BoxerXMLException, IOException {
	LineNumberReader r = new LineNumberReader( new FileReader(fname));	
	String s = r.readLine();
	if (s== null) throw new BoxerCSVException("Empty CSV file (not even a header line!): "+ fname);
	String [] headers = parseCSVLine(s);
	int dataFieldsCnt=0, labelFieldsCnt=0, nameFieldsCnt=0;
	for(String f: headers) {
	    if (!schema.containsKey(f)) {
		throw new BoxerCSVException("Schema file contains no instructions for the field '"+f+"'");
	    }
	    FieldMode m = schema.get(f);	
	    if (m==FieldMode.DATA) dataFieldsCnt++;
	    if (m==FieldMode.LABEL) {
		labelFieldsCnt++;
		suite.addDiscrimination( f );
	    }
	    if (m==FieldMode.NAME) nameFieldsCnt++;
	}
	if (dataFieldsCnt != 1) {
	    throw new BoxerCSVException("According to the schema file,  "+dataFieldsCnt+" fields from the data file "+fname+" have to be interpreted as DATA. This is not allowed; there must be exactly 1 DATA field");
	}
	if (nameFieldsCnt>1) {
	    throw new BoxerCSVException("According to the schema file,  "+nameFieldsCnt+" fields from the data file "+fname+" have to be interpreted as NAME. This is not allowed; there must be no more than 1 DATA field");
	}
	
	Vector<DataPoint> v = new Vector<DataPoint>();

	int lineCnt=1;
	while((s = r.readLine()) !=null) {
	    lineCnt++;
	    s = s.trim();
	    // skip empty lines and comments
	    if (s.equals("") || s.startsWith("#")) continue; 
	    String q [] = parseCSVLine( s );
	    if (q.length > headers.length) {
		throw new BoxerCSVException("File "+fname+", line "+lineCnt+": line has more fields ("+q.length+") than the header lines does ("+headers.length+")");
	    }

	    BagOfWords bag = null;
	    String pointName = null;
	    Vector<Discrimination.Cla> vc = new Vector<Discrimination.Cla>();

	    for(int i=0; i<q.length; i++) {
		//if (lineCnt==2) System.out.println("["+q[i]+"]");
		FieldMode m = schema.get(headers[i]);	
		if (m==FieldMode.DATA) {	    
		    bag = BagOfWords.mkBagOfWords(q[i], options);
		} else if (m == FieldMode.NAME) {
		    pointName = q[i];
		} else if (m == FieldMode.LABEL) {
		    String disName = headers[i];
		    String claName = q[i];
		    claName = claName.replaceAll(" ", "_").replaceAll("\\^", "_");
		    Discrimination.Cla cla = 
			suite.getClaAlways(disName, claName, true);
		    vc.add( cla );
		}
	    }
	
	    if (bag==null) continue;

	    if (pointName==null) {
		pointName = DataPoint.autoGeneratedName();
	    }

	    try {
		FeatureDictionary dic = suite.getDic();
		DataPoint p= new DataPoint(bag.toVector(dic), dic, pointName);
		p.setClasses(vc, suite);
		v.add(p);
	    } catch(BoxerXMLException ex) {
		// report the problem
		Logging.error("Error when trying to convert line " + lineCnt + " into a DataPoint");
		throw ex;
	    }
	}
	return v;
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
    static String [] parseCSVLine(String s ) throws BoxerCSVException {
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