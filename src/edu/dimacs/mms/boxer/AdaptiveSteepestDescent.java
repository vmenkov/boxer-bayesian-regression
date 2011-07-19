package edu.dimacs.mms.boxer;

import java.io.PrintWriter;
import java.util.Vector;


/** An instance of this class is created to run one Adaptive Steepest
 Descent (ASD) process, which is invoked as a special mode for the
 TruncatedGradient class.
*/

class AdaptiveSteepestDescent  {

    Prior prior=null;
    final DataPointArray dpa;
    final BetaMatrix w;
    final Discrimination dis;
    final TruncatedGradient.TruncatedGradientLearnerBlock block;

	/** Runs Steepest Descent (a batch method) with adaptive
	    learning rate until it converges.

	    <p>The value of {@link Suite#verbosity} is used to control
	    what, if anything, is reported during the iterative process.

	    <p>The process optimizes the works lof-likelihood (L) as a
	    function of the PLRM model matrix B. Conceptually, it
	    proceeds as follows:

	    <ul> 

	    <li>1. Start with B=0.

	    <li>2. Compute A as the gradient of L as a function B.
	    
	    <li>3. Compute the theoretically known upper bound M of
	    the absolute value of the second derivative of L along the
	    direction A. (That is, a second derivative f''(t) of the
	    scalar function <em>f(t) = L(B + A*t)</em>. (The actual
	    second derivative is always negative, due to the known
	    convexity of -L; that is, we know that for any <em>t</em>,
	    <center>
	    <em> -M &le; f''(t) &lt; 0</em>
	    </center>

	    <li>4. Set the learning rate <em>&eta;</em> for the next
	    step as <em>&eta; = f'(t=0)/M</em>. This will mean that if
	    <em>f''(t)</em> were actually equal to <em>-M</em> at all
	    <em>t</em>, we would converge to the max<em>L</em> for
	    this one-dimensional problem in one step. In reality we
	    are simply guaranteed not to over-shoot the max.

	    <li>5. Perform one step along the direction <em>A</em>
	    with the learning rate <em>&eta;</em> as computed at the
	    previous step.

	    <li>6. Perform one or more "bonus steps" along the same
	    direction <em>A</em>, increasing the learning rate by a
	    factor of 2 at every step. Stop when <em>L</em> stops
	    increasing, undoing the last bonus step if it has been
	    counter-productive. One can show that e.g. on a simple
	    model, where <em>f(t)</em> were a quadratic polynomial,
	    steps 5 and 6 together allow one to obtain at least 3/4 of
	    the maximum possible increase that can be obtained with the
	    one-dimensional optimization.

	    <li>7. If the cumulative change to <em>L</em> since we
	    last were at step 2 step is less than &eps;, assume that
	    the process has converged, and return the answer 

	    <li>8. Go back to step 2, with the current <em>B</em>.

	    </ul>

	    <p>The above process, of course, is quite expensive, since
	    it requires re-computing log-likelihood (a very expensive
	    function!) at every step.

	    @param xvec xvec[i1:i2-1] is interpreted as the training
	    set over which the log-likelihood is maximized.

	    @param eps The convergence criterion. The iterations will
	    stop when the log-likelihood increment will be smaller
	    than this value. Something like 1e-8 is a reasonable value
	    on a data set of a few hundreds data points with a dozen
	    features each. A smaller value will, of course, make the
	    resulting model closer to the ideal Bayesian model (optimizing
	    the log-lik), but a significant computation cost.

	    @param doAdaptive If true, the learning rate will be
	    computed at each step (truly adaptive SD); otherwise, "safe learning rate" will
	    be computed once and used at each step.
	 */

    AdaptiveSteepestDescent(TruncatedGradient.TruncatedGradientLearnerBlock _block,
			    Vector<DataPoint> xvec, int i1, int i2, double eps, 
			    boolean doAdaptive, boolean doBonus)    {
	prior = verifyPriors( _block.trunc);
	block = _block;
	w = block.w;
	dis = block.dis;
    
	final int n=i2-i1;

	// With Laplacian priors, we must use "safe eta"  as the initial eta for each direction,
	// because otherwise the convergence guarantee won't work.
	if (prior !=null && prior instanceof LaplacePrior) doAdaptive=false;

	doBonus=false; // test


	System.out.println("[SD] Adaptive SD with L-based eps=" + eps);
	System.out.println("[SD] Adaptive="+doAdaptive+", bonus=" + doBonus);
	System.out.println("[SD] Maximizing f(B)=L-P, with L=(1/n)*sum_{j=1..n} log(C_{correct(x_j)}|x_j), n="+n);

	// The inverse of the Gaussian prior's variance (if applicable)
	final double ivar = (prior !=null && prior instanceof GaussianPrior) ? 1/prior.avar : 0;

	if (prior==null) {
	    System.out.println("[SD] No penalty, P=0");
	} else 	if (prior instanceof GaussianPrior) {
	    System.out.println("[SD] Gaussian penalty, P=("+ivar+"/2)*|B|_2^2 ");
	} else 	if (prior  instanceof LaplacePrior) {
	    System.out.println("[SD] Laplacian penalty, P="+((LaplacePrior)prior).getLambda() +"*|B|_1 ");
	} else {
	    throw new IllegalArgumentException("Unsupported prior type: " + prior.getType() );
	}
	    
	// compact format for the data
	dpa = new DataPointArray(xvec, i1, i2, dis);
	//int d =  block.TruncatedGradient.suite.getDic().getDimension(); // feature count
	    
	// 0. Give the safe eta estimate
	double sumX2 = dpa.sumNormSquare();
	double safeEta = n/(sumX2 + n*ivar);
	
	if (Suite.verbosity>0) {
	    System.out.println("[SD] SD on " + (i2-i1) + " vectors; universal safe eta=" + safeEta);
	    System.out.println("[SD] Among "+(i2-i1)+" data points, found "+
			       dpa.sumCnt + " labeled ones, " + dpa.length() + " unique ones");
	}

	if (dpa.sumCnt==0) return; // no labeled points, nothing to optimize

	boolean first = true;
	double prevLogLik = 0;
	int t = 0;
	double sumEta = 0;
	
	while(true) {
	    
	    // 1. compute probability predictions, and log-likelyhood
	    double [][] zz = new double[dpa.length()][];
	    double logLik = penalizedLogLik( zz ); // zz := (Y-P), not divided by n
	    
	    // 2. Check termination criterion
	    double delta = first? 0:  logLik - prevLogLik;
	    if (Suite.verbosity>0) {
		System.out.println("[SD] t="+t+", sumEta="+sumEta+", L="+logLik +
				   (first? "" : " (delta L=" + delta + ")" +
				    (delta<0? " [NEGATIVE delta L?]":"")));
	    }

	    if (!first) {
		// Converged (within eps)?
		if (delta < eps) return;
	    }
	    
	    first = false;
	    prevLogLik = logLik;
	    
	    // 2. Compute A=grad L, which is also our increment vector
	    BetaMatrix a = computeGradL( zz);
	    	    
	    // 3. Adaptive safe Eta
	    /* eta = n ||A||^2 / sum_i { max_j { (alpha_j * x_i)^2 }},
	       or, if Gaussian penalty is used:
	       eta = n ||A||^2/((n/var)||A||^2 + sum_i { max_j { (alpha_j * x_i)^2 }})
	    */

	    double sumA2 = a.squareOfNorm();
	    double eta=safeEta;
	    if (doAdaptive) {
		double sumQ2 = 0;
		for(int i=0; i< dpa.length(); i++) {
		    DataPoint p = dpa.points.elementAt(i);
		    double mp = 0;
		    for(double q: p.dotProducts(a, dis)) {
			mp = Math.max(mp, Math.abs(q));
		    }
		    sumQ2 += mp*mp * dpa.las.elementAt(i).sumCnt;
		}
		eta =  (ivar==0) ?
		    dpa.sumCnt * sumA2 / sumQ2 :
		    dpa.sumCnt * sumA2 / (sumQ2 +   dpa.sumCnt * ivar * sumA2);
	    }
	    
	    if (Suite.verbosity>0) {
		System.out.println("[SD] |grad L|=" + Math.sqrt(sumA2) +", eta := " + eta);	
	    }
	    // Zero gradient (may happen with Laplacean prior) also means convergence
	    if (sumA2==0) {
		return;
	    }


	    a.multiplyBy(eta);
	    addAndCap(a);
	    t++;
	    sumEta+=eta;
	    
	    if (!doBonus) continue;
	    
	    // "bonus" steps - try to keep going in the same
	    // direction, with increasingly longer steps, as long
	    // as log-lik keeps increasing
	    
	    double savedLogLik = penalizedLogLik();
	    while(true) {
		BetaMatrix savedW = new BetaMatrix(w);
		final double f = 2;
		eta *= f ;
		a.multiplyBy(f);
		addAndCap(a);
		double newLogLik =  penalizedLogLik();
		double bonusDelta = newLogLik - savedLogLik;
		if (bonusDelta>0) {
		    savedLogLik = newLogLik;
		    t++;
		    sumEta+=eta;
		    if (Suite.verbosity>0) {
			System.out.println("[SD] [BONUS OK] eta=" + eta + ", L=" + newLogLik);
		    }
		    // Stop going along this direction if it's not
		    // too useful anymore.
		    if (bonusDelta < eps) break;
		} else {
		    // A jump too far; undo!
		    w.setMatrixFrom(savedW);
		    if (Suite.verbosity>0) {
			System.out.println("[SD] [BONUS UNDONE] eta=" + eta + ", L=" + newLogLik);
		    }
		    break;
		}
	    }
	}
    }

    /** This is an auxiliary subroutine for the Adaptive Steepest
	    Descent method.  ASD presently does not support any
	    truncation, and the only kind of priors it supports is the
	    Gaussian prior with the same sigma for all matrix
	    elements.  So this method checks if we have one of the two
	    supported situations. If we do, it returns 1/var of this
	    Gaussian prior (or 0 if there are no priors); if we don't,
	    it throws an exception.

	    @return The inverse of the variance of the Gaussian prior,
	    if there is one, i.e. 1/var=1/sigma^2. This will be 0 if
	    there is no such prior. This value is used by ASD in
	    computing the Gaussian penalty.
    */
    private Prior verifyPriors(Truncation trunc) {
	if (trunc.getTheta() == 0) {
	    // Truncation (and priors, if any) are disabled
	    System.out.println("[SD][VP] No truncation of any kind");
	    return null;
	} else if (trunc.getTheta() != Double.POSITIVE_INFINITY) {
	    throw new IllegalArgumentException("ASD is supported only for theta=0 (no truncation) or theta=" + Double.POSITIVE_INFINITY + " (truncation always), and not for any intermediate values");
	}

	Priors priors = trunc.getPriors();
	if (priors==null) {
	    if (trunc.getG()==0) throw new IllegalArgumentException("Why bother using truncation with g=0? If you want to disable truncation, please set theta=0");
	    
	    // Convert g to a Laplacian prior (an absolute prior with
	    // mode=0, skew=0, g=lambda = sqrt(2/var), i,e,
	    // var=2/lambda^2)
	    double g = trunc.getG();
	    System.out.println("[SD][VP] Converting truncation with g="+g+" to a Laplace prior");
	    return Prior.mkPrior( Prior.Type.l, 0, 2.0/(g*g), true, 0, null);
	    //throw new IllegalArgumentException("The learner is configured for truncation with theta="+ trunc.getTheta() + " and no priors, but truncation is presently not supported in Adaptive SD");
	} 
	Prior onlyPrior = priors.getTheOnlyPrior(dis);
	if (onlyPrior==null) {
	    throw new IllegalArgumentException("The learner is configured to use a variaty of priors, but the only type of prior presently supported in Adaptive SD is the uniform Gaussian prior for all matrix elements");
	}
	if (onlyPrior.getType() != Prior.Type.g && onlyPrior.getType() != Prior.Type.l
	    || onlyPrior.skew!=0 || onlyPrior.mode != 0) {
	    throw new IllegalArgumentException("Adaptive SD only supports Gaussian or Laplacian priors, non-skewed, with mode=0");  
	}
	if (onlyPrior.avar==0)  {
	    throw new IllegalArgumentException("It makes little sense to use variance=0 in Adaptive SD... it sort of already tells us where to converge to, right away!");
	}
	return onlyPrior;
    }

    private double penalizedLogLik() {
	return  penalizedLogLik(null);
    }


    /** Computes the average log-likelihood of this learner block's PLRM model w on the data set 
	represented by dpa, minus the optional Laplacian or Gaussian penalty.
	@param p Controls the penalty (if not null)
    */
    private double penalizedLogLik(double [][] zz) {
	double logLik =  dpa.logLikelihood(block, zz);
	if (prior!=null && prior.avar != Double.POSITIVE_INFINITY) {
	    double penalty = (prior instanceof GaussianPrior) ?
		(0.5/ prior.avar)*w.squareOfNorm() : 
		((LaplacePrior)prior).getLambda() * w.L1Norm();
	    logLik -= penalty;
	}
	return logLik;
    }

    /** Computes the gradient of the penalized averaged
      log-likelihood, A=grad L=grad (L_log - Penalty), which is also
      our increment vector. This is supported for both Laplacian and
      Gaussian penalties.

      <p>
      grad L_log =  (1/n) X(Y-P) <br>
      grad Penalty_G =  (1/sigma^2) B <br>
      grad Penalty_Lap =  lambda * sgn(B)


     @param zz Y-P (not divided by n)
     */
    private BetaMatrix computeGradL( double[][] zz) {
	BetaMatrix a = 
	    (prior !=null && prior instanceof GaussianPrior) ?
	    // gradient of the Gaussian penalty term
	    (new BetaMatrix(w)).multiplyBy( - 1/prior.avar) :
	    new BetaMatrix(w.getNRows()); // was: d
	
	for(int i=0; i< dpa.length(); i++) {
	    double [] z = zz[i];
	    DataPoint x = dpa.points.elementAt(i);
	    for(int h=0; h<x.features.length; h++) {
		int j = x.features[h];	
		a.addDenseRow(j, z, x.values[h]/dpa.sumCnt);
	    }
	}

	if (prior instanceof LaplacePrior) {
	    double lambda = ((LaplacePrior)prior).getLambda();
	    double qa[][] = a.toArray();
	    double qb[][] = w.toArray();
	    if (qa.length < qb.length) {
		throw new AssertionError("Expected A to have no fewer rows than B...");
	    }
	    for( int i=0; i<qa.length; i++) {
		double[] arow=qa[i],
		    brow = (i<qb.length && qb[i]!=null)? qb[i] : new double[0];
		if (arow.length < brow.length) {
		    throw new AssertionError("Expected each row A to have no fewer elements than the corresponding row of B...");
		}
		for(int j=0; j<arow.length; j++) {
		    double b = (j<brow.length)? brow[j] : 0;
		    if (b < 0) arow[j] += lambda;
		    else if (b>0) arow[j] -= lambda;
		    else if (arow[j] < - lambda)  arow[j] += lambda;
		    else if (arow[j] > lambda)  arow[j] -= lambda;
		    else  arow[j] = 0;
		}
	    }
	    a = new BetaMatrix(qa);
	}
	return a;
    }

    /** Increments w by a. Special treatment is provided in the
	Laplacian case, when the derivative is not continuous, and
	"steepest descent" beyond a crease makes no sense. There, if,
	for a particular coordinate we hit zero (or would have jumped
	beyond the zero), both the element of w and the corresponding
	element of a are zeroed. */ 
    private void addAndCap( BetaMatrix a) {
	if (prior != null && prior instanceof LaplacePrior) {
	    // addition that can't take a value beyond zero. When that would happen in normal addition,
	    // we instead set the element to w to zero AND the corresponding element of a to zero as well
	    // (to prevent crossing the "crease" on any "bonus" steps)
	    w.addAndCap(a);
	} else {
	    // simple addition
	    w.add(a);
	}
    }

}
