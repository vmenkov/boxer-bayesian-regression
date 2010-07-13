package edu.dimacs.mms.boxer;

import java.util.*;
import java.io.*;


/** An individual "{@link Prior}" of the Laplacian kind. 
*/
public class LaplacePrior extends Prior {

    /** Constructs the built-in default prior.  */
    LaplacePrior () {
	try {
	    complete(null);
	} catch (BoxerXMLException ex) { throw new AssertionError(); }
    }

    /** Returns the type of this prior, i.e. Type.l
     */
    public Type getType() { return Type.l; }


    /** This is like the gravity parameter in plain truncation. It
      is based on the variance. The value can hold a "real" number, or
      Double.POSITIVE_INFINITY */
    double lambda=0;

    /** Sets all necessary parameters not set in the parent's
      construction phase. In particularly, computes the Lambda from
      the (absolute) variance. The formula, lambda = 2/sqrt(var),
      comes from D.Lewis' 2010-07-08 message. The special cases of 0
      and Double.POSITIVE_INFINITY are handled as appropriate.
    */
    void complete(Prior base) throws BoxerXMLException {
	super.complete(base);
	lambda =  Double.isInfinite(avar)? 0:
	    (avar == 0) ? Double.POSITIVE_INFINITY :   
	    2/Math.sqrt(avar);
    }


    /** Applies the prior to a particular element. 
	@param trunc Encodes the cur-off threshold theta (directly,
	and via trunc.mode)
     */
    public double apply(double val, Truncation trunc, int mult) {
	if (!completed) throw new AssertionError("LaplacePrior not properly initialized");
	final double diff = val - mode;
	if (skew * diff < 0) val = mode;
	else if (trunc.withinTheta(diff)) {
	    if (Double.isInfinite(lambda)) val = mode;
	    else {
		double to = lambda * mult;
		if (diff > to) val -= to;
		else if (diff < -to) val += to;
		else val=mode;
	    }
	} 
	return val;
    }



}