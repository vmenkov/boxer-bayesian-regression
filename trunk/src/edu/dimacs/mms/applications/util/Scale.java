package edu.dimacs.mms.applications.util;


import java.util.*;
import java.io.*;
import java.text.*;
import org.w3c.dom.Element;

import edu.dimacs.mms.boxer.*;

/**  This is a sample utility tool for "scaling" or "normalizing" a
     dataset. There are three modes available: 

     <ul> 

     <li>"Scaling" the data, i.e. multiplying all example vectors by
     the same constant.
     
     <li>"Normalizing" the date, i.e. multiplying each example by an
     appropriate factor to make its 2-norm (not including the dummy
     component)equal to 1).


     <li>Scaling coordinates separately. A separate multiplier for
     each feature should be supplied; this can be done explicitely, in
     a "datapoint" XML format, or implicitly, be requesting that the
     max abs value for each feature in the data set be found. The
     vector of feature multipliers can also be saved, in order to be
     later applied to other datasets that one wants to scale in the same way.

     </ul>

     <h3>Usage<h3>

     <h4>     To scale by the same factor</h4>
     <pre>
     java -Dby=0.01 edu.dimacs.mms.applications.util.Scale input-data-set.xml output-data-set.xml
     </pre>

     Here, "by" is the factor by which all features of all vectors are multiplied.

     <h4>To normalize each vector</h4>
     <pre>
     java -Dnormalize=true edu.dimacs.mms.applications.util.Scale input-data-set.xml output-data-set.xml
     </pre>

     <h4>To scale each feature separately</h4>
     <pre>
     java -Dfeaturefactors={scale_factors.xml|max}  [-Dfeaturefactors_out=scale_factors_out.xml]i edu.dimacs.mms.applications.util.Scale nput-data-set.xml output-data-set.xml
     </pre>

     A possible way of using the feature-specific scaling may be as follows:
     <pre>
     java -Dfeaturefactors=max  -Dfeaturefactors_out=save_scale_factors.xml edu.dimacs.mms.applications.util.Scale train.xml train-scaled.xml
     java -Dfeaturefactors=save_scale_factors.xml edu.dimacs.mms.applications.util.Scale test.xml test-scaled.xml
     </pre>

     What the above two command will do is the following. First, scale the first data
     set (e.g., your training set) so that each feature's abs value in
     the output file won't exceed 1.0; save the the vector of scale
     factors into a file. Second, scale another data set (e.g., your
     test set) by the same factors that were applied to the first set,
     reading them from the file saved on the first run.

 */
public class Scale {


    static void usage() {
	usage(null);
    }

    static void usage(String m) {
	System.out.println("This is the Scale tool for the BOXER toolkit (version " + Version.version+ ")");
	if (m!=null) {
	    System.out.println(m);
	}
	System.out.println("See the Javadoc API pages for more documentation for this tool.");
	System.exit(1);
    }


    enum Mode {
	UNDEFINED, NORMALIZE, SCALE, FEATURESCALE;
    };
    
    static private class Option {
	static final String
	    NORMALIZE = "normalize",
	    FEATUREFACTORS = "featurefactors",
	    FEATUREFACTORS_OUT = "featurefactors_out",
	    BY = "by";
    };

    static public void main(String argv[]) 
	throws IOException, org.xml.sax.SAXException, BoxerXMLException {

	if (argv.length!=2) usage();
	ParseConfig ht = new ParseConfig();

	Mode mode = (ht.getOption(Option.NORMALIZE, false))?  Mode.NORMALIZE : Mode.UNDEFINED;

	if (ht.getOption(Option.BY, null)!=null) {
	    if (mode == Mode.UNDEFINED) {
		mode = Mode.SCALE;
	    } else {
		usage( "Cannot use options 'normalize=true' and 'by=...' at the same time");
	    }
	}

	String featureFactorFile = ht.getOption(Option.FEATUREFACTORS, null);
	if ( featureFactorFile != null) {
	    if (mode == Mode.UNDEFINED) {
		mode = Mode.FEATURESCALE;
	    } else {
		usage( "Cannot combine feature factors with either  'normalize=true' or 'by=...'");
	    }
	}

	if (mode == Mode.UNDEFINED) {
	    usage("Must supply one of the three options: "+Option.NORMALIZE+", " + Option.BY + ", or "+Option.FEATUREFACTORS);
	}
	

	DataPoint.setDefaultNameBase("scaled");

	Suite suite =  new Suite("Test_suite");

	//	String dicFileName =ht.getOption("dic", null);	
	//	suite.setDic(new  FeatureDictionary(new File(dicFileName)));

	String inFile = argv[0];
	String outFile = argv[1];

	Vector<DataPoint> points = ParseXML.readDataFileMultiformat(inFile, suite, true);
	if (mode==Mode.NORMALIZE) {
	    System.out.println("Normalizing...");
	    normalize(points);
	} else if (mode==Mode.SCALE) {
	    double by = ht.getOptionDouble(Option.BY, 0.0);	
	    System.out.println("Scaling by "+by+"...");
	    scale(points,by);
	} else if (mode==Mode.FEATURESCALE) {
	    String out = ht.getOption(Option.FEATUREFACTORS_OUT, null);
	    featureScale(points,featureFactorFile, suite, out);
	} else {
	    usage("Unknonw mode!");
	}

	System.out.println("Saving to: " + outFile);
	DataPoint.saveAsXML(points, "scaled", outFile); 

    }

    static public void normalize(Vector<DataPoint> points) {
	for(DataPoint p: points) {
	    double norm = Math.sqrt( p.normSquareWithoutDummy());
	    if (norm !=0) 	    p.multiplyBy( 1.0/norm);
	}
    }

    /** Multiplies each vector by the same factor 
	@param points A vector of DataPoint objects to scale
	@param by Multiply by this number
     */
    static public void scale(Vector<DataPoint> points, double by) {
	for(DataPoint p: points) {
	    p.multiplyBy( by);
	}
    }

    /** Scans a vector of data points and finds the max absolute value for each feature.
	@return a DataPoint object which contains, for each feature
	found in at least one of the data points with a non-zero value, the inverse of the max abs value that has been
	found for this feature.
     */
    private static DataPoint inverseAbsMaxValues(Vector<DataPoint> points,
						 FeatureDictionary dic) throws BoxerXMLException {

	Vector<DataPoint.FVPair> v = new 	Vector<DataPoint.FVPair>();
	if (points.size()==0) {
	    return new DataPoint(v, dic, "MaxOfNone");
	}

	int n = dic.getDimension();
	double[] absMax = new double[n];
	for(DataPoint p: points) {
	    if (p.getDic()!=dic) throw new IllegalArgumentException("DataPoints use different dictionaries");
	    int features[] =p.getFeatures();
	    double values[] = p.getValues();
	    for(int i=0; i<features.length; i++) {
		absMax[ features[i]] = Math.max( absMax[ features[i]], Math.abs(values[i]));
	    }
	}
	for(int f=0; f<n; f++) {
	    if (absMax[f]>0) {
		v.add(new DataPoint.FVPair(f, 1.0/absMax[f]));
	    }
	}
	return new DataPoint(v, dic, "InverseMaxAbsValues");
    }

    /** Multiplies each feature of each vector by a specified factor. 
	@param points A vector of DataPoint objects to scale
	@param featureFactorFile The name of an XML file that contains
	a single DataPoint, whose features are interpreted as factors
	to apply to the features of all <tt>points</tt>. If "max" is
	given as a file name, this routine will find the largest
	absolute value for each feature in the input data set, and use
	1/that_value as the factor for that feature.

	@param out If not null, the feature factors will be saved to that file
	(in a DataPoint XML format).
     */
    static public void featureScale(Vector<DataPoint> points, String featureFactorFile, Suite suite,
				    String out)
	throws IOException, org.xml.sax.SAXException, BoxerXMLException {

	DataPoint factors = null;
	if (featureFactorFile.equals("max")) {
	    // special mode: finding absolute max values for each feature
	    factors = inverseAbsMaxValues(points, suite.getDic());
	} else {
	    // read from a file
	    factors = ParseXML.readSingleDataPointFileXML( featureFactorFile, suite, false);
	}
	for(DataPoint p: points) {
	    p.multiplyFeaturesBy( factors);
	}

	// save as the factors, if requested
	if ( out != null) {
	    factors.saveAsXML(out);
	}
    }

    static void memory() {
	memory("");
    }

    static void memory(String title) {
	Runtime run =  Runtime.getRuntime();
	String s = (title.length()>0) ? " ("+title+")" :"";
	run.gc();
	long mmem = run.maxMemory();
	long tmem = run.totalMemory();
	long fmem = run.freeMemory();
	long used = tmem - fmem;
	System.out.println("[MEMORY]"+s+" max=" + mmem + ", total=" + tmem +
			   ", free=" + fmem + ", used=" + used);	
    }

}

/*
Copyright 2009-2011, Rutgers University, New Brunswick, NJ.

All Rights Reserved

Permission to use, copy, and modify this software and its documentation for any purpose 
other than its incorporation into a commercial product is hereby granted without fee, 
provided that the above copyright notice appears in all copies and that both that 
copyright notice and this permission notice appear in supporting documentation, and that 
the names of Rutgers University, DIMACS, and the authors not be used in advertising or 
publicity pertaining to distribution of the software without specific, written prior 
permission.

RUTGERS UNIVERSITY, DIMACS, AND THE AUTHORS DISCLAIM ALL WARRANTIES WITH REGARD TO 
THIS SOFTWARE, INCLUDING ALL IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR 
ANY PARTICULAR PURPOSE. IN NO EVENT SHALL RUTGERS UNIVERSITY, DIMACS, OR THE AUTHORS 
BE LIABLE FOR ANY SPECIAL, INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER 
RESULTING FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT, 
NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION WITH THE USE OR 
PERFORMANCE OF THIS SOFTWARE.
*/
