package edu.dimacs.mms.boxer;

import java.util.*;
import java.io.*;


/** An individual "{@link Prior}" of the Gaussian kind,
    1/sqrt(2*pi*sigma^2) exp( - (x-mu)^2 / (2*sigma^2)).
    
    <p>The variance of the distribution is equal to sigma^2.    
*/
public class GaussianPrior extends Prior {

    /** Constructs the built-in default prior.  */
    GaussianPrior () {
	complete(null);
    }

    /** Returns the type of this prior, i.e Type.l
     */
    public Type getType() { return Type.g; }


    /** This is like the gravity parameter in plain truncation. It
      is based on the variance. The value can hold a "real" number, or
      Double.POSITIVE_INFINITY.  */
    double getSigma() {
	return Math.sqrt(avar);
    }

    /** Sets all necessary parameters not set in the parent's
      construction phase. 
    */
    void complete(Prior base) {
	super.complete(base);
    }

    /** An auxiliary class for computing x^m in O(log(m)) time.
     */
    private static class Pow {
	private static double lastX=1;
	private static int lastM = 0;
	private static double lastRes = 1;

	/** x^m, Recursive version.
	    @param m m &gt; 0 */
	/*
	static private double p(double x, int m) {
	    if (m==1) return x;
	    else {
		double c = p(x, m/2);
		return  (m%2 == 0) ?  c*c : c*c*x;
	    }
	}
	*/

	/** x^m, non-recursive version, working as follows:
	    <p>
	    If  m = 1*z_0 + 2*_z1 + 4*z2 + ... + 2^k*zk,
	    then x^m = Product_{k: z_k == 1}{ q_k },
	    with q_k = x^{2^k}. That is, q_0=x, q_{k+1} = q_k ^ 2.
 
	    @param m m &ge; 0 */
	static private double p(double x, int m) {
	    double res=1;
	    for(;  m != 0;  m= (m>>1),  x *= x) {
		if ((m & 1) != 0) {
		    res *= x;
		}
	    }
	    return res;
	}

	/** Computes an integer power, x^m */
	static synchronized double pow(double x, int m) {
	    if (x==lastX && m==lastM) return lastRes;
	    lastX=x;
	    lastM=m;
	    lastRes = (m<0)?  1/p(x, -m): p(x, m);
	    return lastRes;
	}
    
    }

    public double apply(double val, Truncation trunc, int mult, double eta) {
	if (!completed) throw new AssertionError("GaussianPrior not properly initialized");
	final double diff = val - mode;
	if (skew * diff < 0) val = mode;
	else if (trunc==null || trunc.withinTheta(diff)) {
	    if (Double.isInfinite(avar)) {
		// no effect
	    } else if (avar==0) {
		val = mode;
	    } else {
		double q = eta/avar;
		if (q>1) throw new IllegalArgumentException("Learning rate eta="+eta+" so high that Gaussian prior with sigma="+getSigma()+" can't be used");
		val = mode + diff * Pow.pow( 1-q, mult);
	    }
	} 

	return val;
    }


}