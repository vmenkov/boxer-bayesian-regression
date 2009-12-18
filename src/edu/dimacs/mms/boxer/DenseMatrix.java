package edu.dimacs.mms.boxer;

import java.util.*;

 /** A sparse matrix of dense rows. For all-zero rows, null is
	stored. */
public class DenseMatrix extends Matrix  {
    double data[][]=new double[0][];

    //DenseMatrix( double _data[][]) { data= _data; }

    DenseMatrix() {}

    /** Copy constructor */
    DenseMatrix(DenseMatrix a) {
	data = new double[a.data.length][];
	for(int i=0; i<a.data.length; i++) {
	    data[i] = (a.data[i]==null)? null :
		Arrays.copyOf(a.data[i], a.data[i].length);
	}
    }

    /** Ensure that the matrix has at least d rows */
    void resize( int d) {
	if (data == null) data = new double [d][];
	else if (data.length < d) {
	    double [][] newv = new double[d][];
	    for(int i=0; i<data.length; i++) newv[i] = data[i];
	    data = newv;
	} 
    }

    /** Ensure that the j-th row is allocated, and has the size of no
     * less than r */
    void enable(int j, int r) {
	resize(j+1);
	if ( data[j] == null) data[j] = new double[r];	
	else if (data[j].length < r) data[j] = Arrays.copyOf(data[j],r);
    }


    /** Sets several elements in the same row (replacing any existing values).
	@param j The common row index
	@param v Vector containing elements to set (with their values and
	column positions). It is assumed that they are already sorted in
	the order of increasing column position. */
    public void setElements(int j, Vector<BetaMatrix.Coef> v) {
	int maxCol = 0;
	for(BetaMatrix.Coef q:v) { if (q.icla > maxCol) maxCol = q.icla; }
	enable(j,maxCol+1);
	for(BetaMatrix.Coef q:v) { 
	    data[j][q.icla] = q.value;
	}
    }

    public void setElements(int j, double v[]) {
	int maxCol = v.length;
	enable(j,maxCol);
	for(int i=0; i<v.length; i++) {
	    data[j][i] = v[i];
	}
    }


    double [][] toArray() { return data; }

    public long  memoryEstimate() {
	return Sizeof.OBJ +   Sizeof.sizeof(data);
    }


   /** Removes from this matrix all columns corresponding to the
	classes from the specified discrimination. This method is
	called (indirectly) from the Suite's {@link
	edu.dimacs.mms.boxer.Suite#deleteDiscrimination} method, before the latter
	wipes out information about the discrimination being deleted
	from the suite's own interior tables.
	@param suite The suite in the context of which this matrix is interpreted
	@param map Maps old column ids to new column ids. If an element is  -1, it means that the column must be deleted.
     */ 
    void deleteDiscrimination(RenumMap map) {

	for(int j=0;j<data.length; j++) {
	    data[j] = map.applyTo(data[j]);
	}
    }

   void dropRow(int j) {
       if (j < data.length) data[j]=null;
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