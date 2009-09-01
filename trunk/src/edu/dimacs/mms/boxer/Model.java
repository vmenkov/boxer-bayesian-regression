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
	different classes. 

     */
    public double [][] applyModel( DataPoint p);

}
