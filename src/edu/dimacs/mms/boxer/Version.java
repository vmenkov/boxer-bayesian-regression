package edu.dimacs.mms.boxer;

/** Contains the version name of this BOXER toolkit, for encoding in
  XML files etc. Also used by the Javadoc script for inserting into the 
  API doc pages.

 <h3>Recent history</h3>
 <pre>
    0.6.003 - late May 2009
    0.6.004 - 2009-06-18. Borj: respect quotes in cmd line
    0.6.005 - 2009-06-19. Boxer: introduced (and use) BoxerXMLException 
    0.6.006 - 2009-06-24. Boxer: improved DataPoint name management; 
       ParseXML enhancements (in connection with standard-scenarios.html)
    0.6.007 - 2009-06-28. Only updated docs

    0.7.001 - 2009-09-24. New ID validation process. Using caret instead of
       colon for compact XML format. New package tree structure.
    0.7.002 - 2009-09-28. Lazy truncation made correct in TruncatedGradient, and disabled in ExponentiatedGradient
    0.7.003 - 2009-12-08. Test BOXER applications added in edu.dimacs.mms.applications.learning
    0.7.004 - 2009-12-18. Enabled "physical" truncation 
    0.7.005 - 2010-02-18. New installation instructions; moving away RDF converters (to avoid using Jena)
    0.7.006 - 2010-04-19. The semantics of the "name" attribute of the "learner" element is now shifted to the "algorithm" attribute, while "name" has now a new independent meaning. The new (4th) column in the score files.

    0.8.000 - 2010-07-05. Transitionary version to the new "individual priors". I/O for individual priors has been added, but not the actual arithmetic yet
    0.8.001 - 2010-07-07. The individual priors appear to work, although more testing and documenting is still upcoming
    0.8.002 - 2010-12-07. Rearranged location of some files and directories. More experimental tools
    0.8.003 - 2010-12-25. An implementation of  Steepest Descent with adaptive-learning-rate added to TG. A new BOXER application, the Bayesian Antology Aligner(BOA), has been added.
</pre>
 */
public class Version {
    /** The version number of the BOXER library and BOXER applications */
    public final static String version = "0.8.003";

    /** Compares two version number (described as strings). 
	@return a negative number if the first argument is smaller, a positive if the second is smaller; 0 if they are the same.
     */
    static public int compare(String v1, String v2) 
	throws IllegalArgumentException {
	if (v1.equals(v2)) return 0;
	String[] a1=v1.split("\\.");
	String[] a2=v2.split("\\.");
	int i=0;
	for(;i<a1.length && i<a2.length; i++) {
	    int x1, x2;
	    try {
		x1 = Integer.parseInt(a1[i]);
	    } catch ( NumberFormatException ex) {
		throw new IllegalArgumentException("Not a valid version number: " + v1);
	    }
	    try {
		x2 = Integer.parseInt(a2[i]);
	    } catch ( NumberFormatException ex) {
		throw new IllegalArgumentException("Not a valid version number: " + v2);
	    }
	    int d  = x1-x2;
	    if (d!=0) return d;
	}
	// either no string has any non-compared sections left, or only one does
	return  (a1.length - a2.length);
    }
}

/*
Copyright 2009, Rutgers University, New Brunswick, NJ.

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