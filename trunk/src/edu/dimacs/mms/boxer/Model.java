package edu.dimacs.mms.boxer;

//import java.io.*;
//import java.util.*;

/** A Model is an object that can classify examples into classes, but
  (unlike Learner) is not "Learnable". So for example a Learner can
  describe its current classification rules as a static model, which
  can be applied later.
 */
public interface Model {
    /** Applies the model to the example p and returns the
	probabilities of membership in different classes.
	
	@param p An example to apply the model to

	@return An array of the probabilities of membership in
	different classes. It's a two-dimensional array; each element
	of it is a one-dimensional array corresponding to a single
	discirmination. The elements of this one-dim array are the
	probability estimates the DataPoint's belonging to various
	classes of this discriminations, and should sum to 1.0. They
	are ordered in the same way the classes are ordered in the
	discrimination by class id (see {@link Discrimination#getClaById(int)})

     */
    public double [][] applyModel( DataPoint p);

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