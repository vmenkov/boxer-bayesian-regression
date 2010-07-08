package edu.dimacs.mms.boxer;

import java.util.*;
import java.io.*;
//import org.w3c.dom.Element;
//import org.w3c.dom.Document;


/** An individual "prior", which may be applicable only to a single
    (discrimination.class, feature) pair, or for a whole class of them.
    A single prior may be represented as a (mode,variance) pair.
*/
public class LaplacePrior extends Prior {

    /** Constructs the built-in default prior.  */
    LaplacePrior () {
	try {
	    complete(null);
	} catch (BoxerXMLException ex) { throw new AssertionError(); }
    }

    Type getType() { return Type.l; }


    /** This is like the gravity parameter in plain truncation. It
     * is based on the variance */
    double lambda=0;
    /** This is true if lambda is meant to be infinity. In this case,
     * the variable lambda is ignored. */
    boolean lambdaIsInf;

    /** Computes the Lambda from the (absolute) variance.
	FIXME: where's the right formula for lambda? I just use 1/sqrt(var) as a stopgag now.
    */
    void complete(Prior base) throws BoxerXMLException {
	super.complete(base);
	lambdaIsInf = (avar == 0);
	if (!lambdaIsInf) 	lambda =  1/Math.sqrt(avar);
    }


    /** Applies the prior to a particular element 
	@param trunc Encodes the cur-off threshold theta (directly,
	and via trunc.mode)
     */
    double apply(double val, Truncation trunc, int mult) {
	if (!completed) throw new AssertionError("LaplacePrior not properly initialized");
	double diff = val - mode;
	if (trunc.withinTheta(diff)) {
	    if (lambdaIsInf) diff = 0;
	    else {
		double to = lambda * mult;
		if (skew * diff < 0) diff = 0;
		else if (diff > to) diff -= to;
		else if (diff < -to) diff += to;
		else diff = 0;
	    }
	    val = mode + diff;
	} 
	return val;
    }



}