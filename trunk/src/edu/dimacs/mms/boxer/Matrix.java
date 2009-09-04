package edu.dimacs.mms.boxer;

import java.util.*;
import java.io.*;
// for XML generation
import org.w3c.dom.*;

/** This is a common abstract parent of our matrix classes (such as DenseMatrix
 * and BetaMatrix). It declares some methods that all BOXER matrices have to support.
*/
public abstract class Matrix implements Measurable {

   /** Sets several elements in the same row (replacing any existing values).
	@param j The common row index
	@param v Vector containing elements to set (with their values and
	column positions). It is assumed that they are already sorted in
	the order of increasing column position. */
    abstract public void setElements(int j, Vector<BetaMatrix.Coef> v);

    /* Converts to (sparse) array of dense rows. Each row in the
     * return array will be a double[] of the length just enough for
     * all non-zero elements of this row to fit; thus rows may be of
     * different length.
     */
    abstract double [][] toArray();

    /** Removes from this matrix all columns corresponding to the
      classes from the specified discrimination. This method is called
      (indirectly) from the Suite's {@link
      boxer.Suite#deleteDiscrimination} method, before the latter
      wipes out information about the discrimination being deleted from the
      suite's own interior tables.

      @param map Maps old column ids to new column ids. If an element is  -1, it means that the column must be deleted.
     */ 
    abstract void deleteDiscrimination( RenumMap map); 

    void describe(PrintWriter out, FeatureDictionary dic ) {
	describeMatrix(toArray(), dic, out);
    }

    /** Auxiliary function for printing out the classifier state */

    private static String describeMatrix(double [][] aw, FeatureDictionary dic) {
	StringWriter sw = new StringWriter();
	describeMatrix(aw, dic, new PrintWriter(sw));
	return sw.toString();
    }


    private static void describeMatrix(double [][] aw, FeatureDictionary dic, PrintWriter out) {
	for(int j=0; j<aw.length; j++) {
	    double v[] = aw[j];
	    if (v==null || allZeros(v)) continue; // save space
	    out.printf("%3d(%7s) |" , j, dic.getLabel(j));
	    for(int k=0; k<v.length; k++) {
		if (k>0) out.print(" ");
		out.printf("%3f", v[k]);
	    }
	    out.println("|");
	}
    }

    /** Returns true if v[] contains only zero elements (or has no
     * elements at all) */
    static private boolean allZeros(double v[]) {
	for(double c: v) { 
	    if (c!=0) return false;
	}
	return true;
    }


    public Element saveAsXML( Document xmldoc, Discrimination dis, FeatureDictionary dic, String name) {
	return createMatrixElement(xmldoc, dis, dic, name, toArray());
    }

    /** Creates an XML element describing a sparse matrix. Skips empty
	rows to save space
	
    */
    Element createMatrixElement( Document xmldoc,  Discrimination dis, FeatureDictionary dic,  String name, 
				 double[][] v) {
	Element e = xmldoc.createElement("matrix");
	e.setAttribute("name", name);
	int d = v.length;
	for(int i=0;i<d; i++) {
	    if (v[i] == null || allZeros(v[i]) ) continue; 
	    Element re =  xmldoc.createElement(XMLUtil.ROW);
	    re.setAttribute(XMLUtil.FEATURE, dic.getLabel(i));
	    StringBuffer b = new StringBuffer();
	    for(int j=0; j<v[i].length; j++) {
		if (v[i][j] != 0) {
		    if (b.length()>0) b.append(" ");
		    b.append( dis.classes.elementAt(j).name + ":" + v[i][j]);
		}
	    }
	    re.appendChild(xmldoc.createTextNode(b.toString()));
	    e.appendChild(re);
	}
	return e;	
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