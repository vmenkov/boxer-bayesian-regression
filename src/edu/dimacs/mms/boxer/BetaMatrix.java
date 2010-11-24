package edu.dimacs.mms.boxer;

import java.util.*;

/** This is used to store the matrix of Beta vectors (e.g.,
   representing a number of PLRMs, stored in a single matrix), used in
   the classifier's dot product. A BetaMatrix has meaning only in the
   context of a Suite, since its columns correspond to the classes of
   the Suite's Discriminations.

   The matrix is stored as a sparse matrix, by row; for
   each element, the element value and the column index are
   stored. The column indexes refer to classes positions in the
   suite's master index, which means that all indexes will need to be
   adjusted if a discrimination is removed from the suite.
*/
public class BetaMatrix extends Matrix  {

    /** An auxiliary class, an instance of which contains a matrix
	coeffcient, and the label of the destination class. This is
	how we represent elements of the Beta matrix stored feature-first.
    */
    public static class Coef implements Comparable<Coef>  {
	/** This is the index of the corresponding Discrimination.Cla
	    instance in the discrimination's list of classes (dis.classes)
	*/
	int icla;
	double value;
	public Coef(int i, double v) { icla =i; value=v;}
	public void setValue(Coef c) {
	    value = c.value;
	}

	/** Compares column position */
	public int compareTo(Coef x) {
	    return icla - x.icla;
	}
    }

    /** Maps integer feature IDs (same as FeatureDictionary) to       
     */
    private Vector< Vector<Coef>> matrix = new  Vector< Vector<Coef>>();

    BetaMatrix() {	this(0);    }

    /** Creates a zero matrix. 
     @param d number of features */
    BetaMatrix(int d) {
	matrix = new Vector<  Vector<Coef>>(d); // len=0, capacity=d
	for(int i=0; i<d; i++) {
	    matrix.add( new  Vector<Coef>());
	}
    }

    /** Creates a deep copy of an existing matrix, changing column ids
     * as appropriate to map the old discrimination's default class to
     * the new discriination's one */
    BetaMatrix(BetaMatrix b, int oldDef, int newDef) {
	matrix = new  Vector< Vector<Coef>>( b.matrix.size());
	int m = Math.max(oldDef, newDef);
	for( Vector<Coef> v:  b.matrix) {
	    Vector<Coef> w= null;
	    if (v!=null)  {
		if (oldDef == newDef) w=new Vector<Coef>(v);
		else {
		    // have to reorder properly in all situations
		    double z[] = coefVector2denseArray(v);
		    if (z.length < m+1) z = Arrays.copyOf(z, m+1);
		    double a = z[oldDef];
		    z[oldDef] = z[newDef];
		    z[newDef] = a;
		    w= denseArray2coefVector(z);
		}
	    }
	    matrix.add(w);
	}
    }

    /** Verifying that we indeed have a fallback discrimination's
     * classifier matrix here. The idea is that this matrix ought to
     * have the same values for all non-default-class columns
     */
    boolean looksLikeFallback(Discrimination dis) {
	int n = dis.claCount();
	int defPos = dis.getDefaultCla().getPos();	
	for( Vector<Coef> v:  matrix) {
	    if (v==null) continue;
	    // All non-defaults are zeros
	    if (v.size() == 1 && v.elementAt(0).icla == defPos) continue;
	    // 1 default col, 1 non-default col, and one of them is 0
	    if (v.size() == 1 && n==2) continue;

	    // two possibilities: either (A) or (B) 
	    if (v.size() == n) {
		// (A) all non-defaults and one defaults,
		double nonDef = v.elementAt(defPos == 0? 1:0).value;
		for(int i=0; i<n; i++) {
		    if (v.elementAt(i).icla != i) return false;
		    if (i != defPos && v.elementAt(i).value != nonDef) return false;
		}
	    } else  if (v.size() == n-1) {
		// (B) only all non-defaults (becuase default is 0)
		double nonDef = v.elementAt(0).value;
		for(int i=0; i<n; i++) {
		    int shouldBe = (i<defPos) ? i : i+1;
		    if (v.elementAt(i).icla != shouldBe) return false;
		    if (v.elementAt(i).value != nonDef) return false;
		}
	    } else {
		// Unexpected array length
		return false;
	    }
	}	   
	return true;
    }

    boolean isZero() {
	if (matrix==null) return true;
	for(Vector<Coef> v: matrix) {
	    if (v != null) {
		for(Coef q: v) { 
		    if (q.value != 0) return false;
		}
	    }
	}
	return true;
    }

  /** Returns the number of non-zero values actually stored in the matrix.
	Ignores any stored zeros.
    */	
    public int nzCount() {
	if (matrix==null) return 0;
	int sum = 0;
	for(Vector<Coef> v: matrix) {
	    if (v != null) {
		for(Coef q: v) { 
		    if (q.value != 0) sum ++;
		}
	    }
	}
	return sum;
    }


    public double squareOfNorm() {
	double sum = 0;
	for(Vector<Coef> v: matrix) {
	    if (v != null) {
		for(Coef q: v) { 
		    sum+= q.value * q.value;
		}
	    }
	}
	return sum;
    }



    /** Converts an array of doubles - representing a dense vector - to
	an array of Coef instances, representing a sparse vector */ 
    private Vector<Coef> denseArray2coefVector(double a[]) {
	Vector<Coef> v = new  Vector<Coef>(a.length);
	for(int i=0; i<a.length; i++) {
	    if (a[i] != 0) v.addElement(new Coef(i, a[i]));
	}
	v.trimToSize();
	return v;
    }

    /** Constructing a BetaMatrix from a (possibly sparse) array of dense rows
     */
    BetaMatrix(double a[][]) {
	matrix = new Vector<  Vector<Coef>>(a.length);
	matrix.setSize(a.length);
	for(int i=0; i<a.length; i++) {
	    if (a[i] != null) {
		matrix.set(i,  denseArray2coefVector(a[i]));
	    }
	}
    }

    private static double[] coefVector2denseArray(Vector<Coef> v ) {
	double[] q = null;
	if (v!=null && v.size() > 0) {
	    q = new double[ v.lastElement().icla + 1];
	    for(int h=0; h<v.size(); h++) {
		Coef c = v.elementAt(h);
		q[c.icla] = c.value;
	    }
	}
	return q;
    }

    /* Converts to (sparse) array of dense rows. Each row in the
     * return array will be a double[] of the length just enough for
     * all non-zero elements of this row to fit; thus rows may be of
     * different length.
     */
    public double [][] toArray() {
	double w[][] = new double[matrix.size()][];
	for(int j=0; j< matrix.size(); j++){
	    w[j] = coefVector2denseArray( matrix.elementAt(j));
	}
	return w; // new DenseMatrix(w);
    }

    /** Get a row of the matrix whose columns are beta vectors.
	This gives us all the active classes for a given feature.
    */
    Vector<Coef> getRow(int featureId) {
	return featureId < matrix.size()? matrix.elementAt(featureId) : null;
    }

    /** Drops the row for the specified feature */
    void dropRow(int featureId) {
	if (featureId < matrix.size()) {
	    matrix.set(featureId, null);
	}
    }

    /** Removes zero elements from the specified row */
    void compressRow(int featureId) {
	Vector<Coef> v = getRow(featureId);
	int  to=0;
	for(int i=0; i<v.size(); i++) {
	    if (v.elementAt(i).value != 0) {
		if (to < i) v.set(to, v.elementAt(i));
		to++;
	    }
	}
	if (to==0) {
	    // surprise!
	    //Logging.info("Row " +  featureId + " has become all zeros (surprisingly), and is dropped");
	    dropRow(featureId);
	} else {
	    v.setSize(to);
	}
    }

    /** Number of rows */
    public int getNRows() { return matrix.size(); }

    Collection<Vector<Coef>> rows() {
	return matrix;
    }

    /** Adds all values from a dense vector (array) to a given row of this 
	matrix. As a result, that row of the matrix will in fact become
	dense itself (alhough will still be represented as a Vector<Coef>)
	@param j feature id 
	@param a Values to be multiplied by q and stored in the j-th row 
	@param q The multiplier
     */
    void addDenseRow(int j, final double a[], double q) {
	if ( matrix.size() <=j ) matrix.setSize(j+1);
	Vector<Coef> v = getRow(j);
	if (v==null) {
	    matrix.set(j, v = new  Vector<Coef>());
	}
	if (v.size() == a.length) {
	    // The stored vector is already dense
	    if (v.size()>0 && v.lastElement().icla != a.length-1) {
		throw new IllegalArgumentException("Last-column-id mismatch: "+(a.length-1)+") in the dense array, " +v.lastElement().icla+ " in the sparse one");
	    }
	    for(int i=0; i<a.length; i++) {
		Coef c = v.elementAt(i);
		if (c.icla != i) throw new AssertionError("Matrix row " +j+ " is not dense, despite its length being " + v.size());
		c.value += a[i] *q;
	    }
	} else {
	    // It will be made dense...
	    if (v.size()>0 && v.lastElement().icla >= a.length) {
		throw new IllegalArgumentException("Adding a short dense vector (length="+a.length+") to a sparse row with higher-numbered columns (" +v.lastElement().icla+ ")");
	    }
	    // error (failing to multiply by q) fixed 2009-10-29
	    double b[] = new double[a.length];
	    for(int i=0; i<b.length; i++) b[i] = a[i]*q;
	    for(Coef c: v) b[c.icla] += c.value;
	    v.setSize(b.length);
	    for(int i=0; i<b.length; i++) v.set(i, new Coef(i, b[i]));
	}		     
    }

    /** Sets several elements in the same row (replacing any existing values).
	@param j The common row index
	@param v Vector containing elements to set (with their values and
	column positions). It is assumed that they are already sorted in
	the order of increasing column position. */
    public void setElements(int j, Vector<Coef> v) {
	if ( matrix.size() <=j ) matrix.setSize(j+1);
	Vector<Coef> w = matrix.elementAt(j);
	if (v==null || v.size()==0) {
	    return;
	} else if (w==null || w.size()==0) {
	    matrix.set(j, new Vector<Coef>(v));
	    return;
	}
	
	int iw=0; 
	for(Coef q: v) {
	    while(iw<w.size() && w.elementAt(iw).icla < q.icla) iw++;
	    if (iw>=w.size()) {
		w.add(q);
	    } else if (w.elementAt(iw).icla == q.icla) {
		w.elementAt(iw).setValue(q);
	    } else {
		w.add(iw++, q);
	    }
	}
    }

    public long memoryEstimate() {
	long sum=Sizeof.OBJ;	
	for(Vector<Coef> v: matrix) {
	    sum += Sizeof.OBJREF;
	    if (v!=null) sum += v.size() * (Sizeof.INT + Sizeof.DOUBLE);
	}
	return sum;
    }

    /** Removes from this matrix all columns corresponding to the
	classes from the specified discrimination. This method is
	called (indirectly) from the Suite's {@link
	edu.dimacs.mms.boxer.Suite#deleteDiscrimination} method, before the latter
	wipes out information about the discrimination being deleted
	from the suite's own interior tables.

	@param map Maps old column ids to new column ids. If an element is  -1, it means that the column must be deleted.
     */ 
    void deleteDiscrimination(RenumMap map) {

	for(Vector<Coef> v: matrix) {
	    if (v!=null) {
		for(int k=0; k<v.size(); k++) {
		    Coef c = v.elementAt(k);
		    int oldCol = c.icla;
		    if (map.renumMap[oldCol]<0) {
			v.removeElementAt(k--);
		    } else {
			c.icla = map.renumMap[oldCol];
		    }
		}
	    }
	}
	//System.out.println("... done");
	
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
