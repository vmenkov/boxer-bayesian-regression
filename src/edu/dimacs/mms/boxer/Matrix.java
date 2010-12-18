package edu.dimacs.mms.boxer;

import java.util.*;
import java.io.*;
// for XML generation
import org.w3c.dom.*;
import org.apache.xerces.dom.DocumentImpl;

/** This is a common abstract parent of our matrix classes (such as
  DenseMatrix and BetaMatrix). It declares some methods that all BOXER
  matrices have to support.  */
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
    abstract public double [][] toArray();

    /** Removes from this matrix all columns corresponding to the
      classes from the specified discrimination. This method is called
      (indirectly) from the Suite's {@link
      edu.dimacs.mms.boxer.Suite#deleteDiscrimination} method, before the latter
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


    /** Number of rows */
    abstract public int getNRows();

    /** Returns the number of non-zero values actually stored in the matrix.
	Ignores any stored zeros.
    */	
    abstract public int nzCount();

    /** Saves the matrix into a separate XML file. */
    final public void saveAsXML(String fname, Discrimination dis, FeatureDictionary dic, String name)  {
	Document xmldoc= new DocumentImpl();
	Element e = saveAsXML(xmldoc, dis, dic, name);
	xmldoc.appendChild(e);
	XMLUtil.writeXML(xmldoc, fname);
    }

    /** Serializes  the matrix as an XML element */
    public Element saveAsXML( Document xmldoc, Discrimination dis, FeatureDictionary dic, String name) {
	return createMatrixElement(xmldoc, dis, dic, name, toArray());
    }

    /** Creates an XML element describing a sparse matrix. Skips empty
	rows to save space	
    */
    static Element createMatrixElement( Document xmldoc,  Discrimination dis, FeatureDictionary dic,  String name, 
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
		    b.append( dis.classes.elementAt(j).name + 
			      BXRReader.PAIR_SEPARATOR_STRING + v[i][j]);
		}
	    }
	    re.appendChild(xmldoc.createTextNode(b.toString()));
	    e.appendChild(re);
	}
	return e;	
    }

    /** Drops the row for the specified feature */
    abstract void dropRow(int featureId);

    /** Parses a matrix element that has been written by {@link
	#createMatrixElement() createMatrixElement()}. The values read
	are inserted into this existing matrix (which may, but does not
	have to, be initially empty).
	
	All class labels occurring in describing matrix value must
	be already existing classes (declared in the suite
	description earlier). This is not the time to declare new
	classes.

	@param me Element (<matrix ...>) to parse
	    
    */
    void readMatrix(Element me, Suite suite, Discrimination dis) throws BoxerXMLException  {
	readMatrix(me, suite, dis,false );

    }
    void readMatrix(Element me, Suite suite, Discrimination dis, boolean canAddFeatures) throws BoxerXMLException {
	String matName = me.getAttribute(ParseXML.ATTR.NAME_ATTR);
	int rowCnt=0, elCnt=0;
	for(Node x=me.getFirstChild(); x!=null; x=x.getNextSibling()) {
	    if ( x.getNodeType() == Node.ELEMENT_NODE &&
		 x.getNodeName().equals(XMLUtil.ROW)) {
		rowCnt++;
		Element re = (Element)x;
		String feature = re.getAttribute(XMLUtil.FEATURE);
		if (!XMLUtil.nonempty(feature)) throw new IllegalArgumentException("Missing feature attribute in a matrix row element!");
		String[] pairs = re.getFirstChild().getNodeValue().split("\\s+");
		Vector<BetaMatrix.Coef> v = new Vector<BetaMatrix.Coef>(pairs.length);
		for(String p: pairs) {
		    if (p.length()==0) continue;
		    String q[] = p.split(BXRReader.PAIR_SEPARATOR_REGEX);
		    if (q.length!=2) throw new IllegalArgumentException("While reading matrix row element for feature '"+feature+"', could not parse element '"+p+"' as class"+BXRReader.PAIR_SEPARATOR_STRING+"value!");
		    Discrimination.Cla c = dis.getCla(q[0]);
		    if (c==null)  throw new IllegalArgumentException("While reading matrix row element for feature '"+feature+"', found previously undeclared class name: " + q[0]);
		    elCnt++;
		    v.add(new BetaMatrix.Coef(c.getPos(),
					      Double.parseDouble(q[1])));
		}
		int fid = canAddFeatures? suite.getDic().getIdAlways(feature) :
		    suite.getDic().getId(feature);
		setElements(fid, v);
	    }
	}
	if (Suite.verbosity>1) Logging.info("Read " + rowCnt + " rows, "+elCnt + " elements for matrix "+ matName + " for discrimination " + dis);
    }

    /** Parses a "matrix" XML element, creating a new BetaMatrix out
	of it. The names of classes and features are matched against
	the specified discrimination, and the feature dictionary of
	the specified suite.
     */
    public static Matrix readNewMatrix(Element me, Suite suite, Discrimination dis, boolean canAddFeatures)  throws BoxerXMLException {
	Matrix w = new BetaMatrix();
	w.readMatrix(me, suite, dis,canAddFeatures );
	return w;
    }

    /** Computes the square of the vector 2-norm of this matrix, i.e.
	the sum of squares of all matrix elements. 
     */
    abstract public double squareOfNorm();


    /** The square of the 2-norm (Euclidean) distance between the two matrices.
     */
    public double squareEuclideanDistance(Matrix other) {
	double [][] a = toArray(), b = other.toArray();
	double sum=0;
	int i=0;
	final double [] empty = new double[0];

	for(; i<a.length && i<b.length; i++) {
	    double [] x = a[i], y=b[i];
	    int j=0;
	    if (x==null) x =empty;
	    if (y==null) y =empty;
	    for(; j< x.length && j<y.length; j++) {
		double d = x[j]-y[j];
		sum += d*d;
	    }
	    sum += squareOfNorm(x,j) + squareOfNorm(y,j);
	}
	for(int k=i; k<a.length; k++) sum += squareOfNorm(a[k], 0);
	for(int k=i; k<b.length; k++) sum += squareOfNorm(b[k], 0);
	return sum;
    }

    private static double squareOfNorm(double [] x, int j) {
	double sum=0;
	for(; j<x.length; j++) sum += x[j]*x[j];
	return sum;	
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