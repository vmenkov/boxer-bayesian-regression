package edu.dimacs.mms.boxer;

import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Vector;

import org.w3c.dom.Document;
import org.w3c.dom.Element;


/** Using Truncated Gradient for training of Polytomous Logistic
    Regression Models (PLRM).

    <p>
    If the truncation is turned off (trunc.theta=0), the TruncatedGradient
    becomes plain old Stochastic Gradient Descent (CGD).

    <p>
    If the suite for which this learner is created has a {@link
    Priors} object associated with it, then the priors-based "truncation" 
    will be used instead of the regular toward-zero truncation.
 */
public class TruncatedGradient extends PLRMLearner {

    static final boolean lazyT = true;

    /** Algorithm parameters */
    /** The fixed learning rate eta. Default (as per paper) is 0.1. It was
	0.01 in the code used in January-Feb 2009. 

	<p>The parameter is ignored in ASD (adaptive steepest descent), since
	there eta is computed dynamically at each step.
   */
    double  eta=0.1;  
    /** The gravity factor for truncation. in Jan-Feb 2009 we had
       g=0.001 and K=1, i.e. truncation was applied at each step, but
       was very small.

       <p>If indiviudal priors are used, this parameter is completely
       ignored, as the prior-based truncation is carried out
       instead. E.g., if the priors are {@link LaplacePrior}s, then
       the priors' lambdas are used exactly like g would be used if there
       were no priors.     
    */
    double g=0.1;
    /** How often truncation is carried out */
    private int K = 10;
    
    public class TruncatedGradientLearnerBlock extends PLRMLearner.PLRMLearnerBlock {

	/** The truncation object controls the truncation
	    modalities. In particular, if trunc.theta=0, there
	    is no truncation at all.
	 */
	Truncation trunc; 

	/** Creates a new learner block, possibly patterned on an old one
	    @param b An old block. If not null, this constructor will
	    copy all data structures of the old block into this block.
	    This can be used for AssumeDefaultHistory

	 */
	TruncatedGradientLearnerBlock(Discrimination _dis, TruncatedGradientLearnerBlock b) {
	    // synch any not-yet-applied truncation in b's matrix
	    if (b!=null) b.trunc.applyTruncationToAllRows(); 

	    if (Suite.verbosity>1) System.out.println("Creating TG block, based on " + (b==null ? "null" : "a non-null block" ));

	    // copy params
	    dis = _dis;
	    if (b==null) {
		trunc = new Truncation( commonTrunc, new Matrix[]{w}, dis); // ZZ
	    } else {
		// copy b's matrices etc
		//System.out.println("TGLB( b=" + b+")");
		super.initFrom(b);
		// copy truncation and its state
		trunc = b.trunc.liveCopy( new Matrix[]{w}, dis ); // ZZ
	    }

	     if (Suite.verbosity>1) System.out.println("TG Block created for dis=" + dis.getName());
	     if (Suite.verbosity>1) System.out.println("trunc=" + trunc.describe());

	}


	/** Incrementally train this classifier on data points xvec[i], i1&le; i &lt; i2
	    @param xvec A vector of data points. Elements from i1 through i2-1 will be used for this training sessions.
	    
	*/
	public void absorbExample(Vector<DataPoint> xvec, int i1, int i2) {
	    int d =  suite.getDic().getDimension(); // feature count
	    
	    if (Suite.verbosity>1) System.out.println("TruncatedGradient.absorbExample (dis="+dis.name+"), "+(i2-i1)+" examples:\n" +
			       "d="+d+"; eta=" + eta +"; "+ commonTrunc.describe());
       
	    for(int i=i1; i<i2; i++) {

		// 1. Acquire an example
		DataPoint x = xvec.elementAt(i);

		// 2. truncate (or, actually, request lazy truncation)

		trunc.requestTruncation(d, eta);	    

		// 2(a). Actuate any postponed ("lazy evaluation") truncation that
		// needs to be done now because if affects the matrix's rows 
		// that we'll be using now
		for(int h=0; h<x.features.length; h++) {
		    int j = x.features[h];		
		    trunc.applyTruncation(j);
		}

		// 3. compute predictions
		double z[] = adjWeights(x);
		if (z==null) {
		    if (Suite.verbosity>1) System.out.println("Skip example " + x.name + " not labeled for " + dis.getName());
		    continue; // example not labeled for this discr
		}

		if (Suite.verbosity>1) {
		    // 4. output probability
		    System.out.print("z vector=[");
		    for(double q: z) System.out.print(" " + q);
		    System.out.println("]");
		    System.out.println("Multiplied by eta=" + eta);
		}

		// 6. update weights
		for(int h=0; h<x.features.length; h++) {
		    int j = x.features[h];	
		    w.addDenseRow(j, z, eta * x.values[h]);
		}
		// trunc.requestTruncation(d,eta); // moved to bottom
		//System.out.println(describe());
	    }
	    trunc.applyTruncationToAllRows(); 
	}

	/** Like {@link #absorbExample(Vector<DataPoint>,int,int)}, but emulating the SD (Steepest Descent)
	    method. That is, all gradients are computed first, and then
	    applied at once. This method was added for experiments
	    that compare SGD with SD.

	    <p>If the learning rate is <em>very</em> small, the result
	    of calling absorbExamplesSD on a vector of data points
	    should be quite similar to those of calling absorbExample
	    on the same vector of data points. However, learning rates
	    this low are usually not practical.
	 */
	public void absorbExamplesSD(Vector<DataPoint> xvec, int i1, int i2) {
	    int d =  suite.getDic().getDimension(); // feature count
	    
	    if (Suite.verbosity>1) System.out.println("TruncatedGradient.absorbExamplesSD (dis="+dis.name+"), "+(i2-i1)+" examples:\n" +
			       "d="+d+"; eta=" + eta +"; "+ commonTrunc.describe());

	    double [][] zz = new double[i2-i1][];

       
	    // 2. truncate (or, actually, request lazy truncation).
	    // We do it multiple times, since there is no requestTruncation()
	    // call with a multiplier
	    for(int i=i1; i<i2; i++) {
		trunc.requestTruncation(d,eta);	    
	    }

	    // Actuate all truncations. No need to be "lazy" here, as this
	    // is essentially a batch method
	    trunc.applyTruncationToAllRows();


	    for(int i=i1; i<i2; i++) {

		// 1. Acquire an example
		DataPoint x = xvec.elementAt(i);

		// 3. compute predictions
		double [] z = zz[i-i1] = adjWeights(x);
		if (z==null) {
		    if (Suite.verbosity>1) System.out.println("Skip example " + x.name + " not labeled for " + dis.getName());
		    continue; // example not labeled for this discr
		}
	    }

	    for(int i=i1; i<i2; i++) {
		double [] z = zz[i-i1];
		if (z==null) continue;
		DataPoint x = xvec.elementAt(i);
		if (Suite.verbosity>1) {
		    // 4. output probability
		    System.out.print("z vector=[");
		    for(double q: z) System.out.print(" " + q);
		    System.out.println("]");
		    System.out.println("  mult by eta=" + eta);
		}

		// 6. update weights
		for(int h=0; h<x.features.length; h++) {
		    int j = x.features[h];	
		    w.addDenseRow(j, z, eta * x.values[h]);
		}
	    }

	    trunc.applyTruncationToAllRows(); 

	    if (Suite.verbosity>1) {
		// Measure the update (without the eta factor), just for reporting
		BetaMatrix b = new BetaMatrix( w.getNRows());
		for(int i=i1; i<i2; i++) {
		    double [] z = zz[i-i1];
		    if (z==null) continue;
		    DataPoint x = xvec.elementAt(i);
		    for(int h=0; h<x.features.length; h++) {
			int j = x.features[h];	
			b.addDenseRow(j, z, x.values[h]);
		    }
		}
		double grad2 = b.squareOfNorm();
		System.out.println("|grad L|=" + Math.sqrt(grad2));
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
	private double verifyPriorsAndGetInverseVar() {
	    if (trunc.getTheta() == 0) {
		// Truncation (and priors, if any) are disabled
		return 0;
	    }
	    Priors priors = trunc.getPriors();
	    if (priors==null) {
		throw new IllegalArgumentException("The learner is configured for truncation with theta="+ trunc.getTheta() + " and no priors, but truncation is presently not supported in Adaptive SD");
	    } 
	    Prior onlyPrior = priors.getTheOnlyPrior(dis);
	    if (onlyPrior==null) {
		throw new IllegalArgumentException("The learner is configured to use a variaty of priors, but the only type of prior presently supported in Adaptive SD is the uniform Gaussian prior for all matrix elements");
	    }
	    if (onlyPrior.getType() != Prior.Type.g || onlyPrior.skew!=0) {
		throw new IllegalArgumentException("Adaptive SD does not presently support any priors other than Gaussian non-skewed");  
	    }
	    double var= onlyPrior.avar;
	    if (var==0)  {
		throw new IllegalArgumentException("It makes little sense to use variance=0 in Adaptive SD... it sort of already tells us where to converge to, right away!");
	    }
	    return 1/var;
	}



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
	 */
	public void runAdaptiveSD(Vector<DataPoint> xvec, int i1, int i2, double eps) {
	    // the inverse of Gaussian variance, if any
	    double ivar = verifyPriorsAndGetInverseVar();


	    final int n=i2-i1;
	    System.out.println("[SD] Adaptive SD with L-based eps=" + eps);
	    System.out.println("[SD] Maximizing f(B)=L-P, with L=(1/n)*sum_{j=1..n} log(C_{correct(x_j)}|x_j), n="+n+", P=("+ivar+"/2)*|B|^2 ");

	    // compact format for the data
	    DataPointArray dpa = new DataPointArray(xvec, i1, i2, dis);
	    int d =  suite.getDic().getDimension(); // feature count
	    
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
		double logLik = dpa.logLikelihood(this,zz); // zz := Y-P
		if (ivar!=0) {
		    double sumB2 = w.squareOfNorm();
		    logLik -= ivar/2*sumB2;
		}

		// 2. Check termination criterion
		double delta = first? 0:  logLik - prevLogLik;
		if (Suite.verbosity>0) {
		    System.out.println("[SD] t="+t+", sumEta="+sumEta+", L="+logLik +
				       (first? "" :
					" (delta L=" + delta + ")" +
					(delta<0? " [NEGATIVE delta L?]":"")));
		}

		if (!first) {
		    // Converged (within eps)?
		    if (delta < eps) return;
		}

		first = false;
		prevLogLik = logLik;

		// 2. Compute A=grad L, which is also our increment vector
		BetaMatrix a;

		if (ivar==0) {
		    a = new BetaMatrix(d);
		} else {
		    // gradient of the penalty term
		    a = new BetaMatrix(w);
		    a.multiplyBy( -ivar );
		}

		for(int i=0; i< dpa.length(); i++) {
		    double [] z = zz[i];
		    DataPoint x = dpa.points.elementAt(i);
		    for(int h=0; h<x.features.length; h++) {
			int j = x.features[h];	
			a.addDenseRow(j, z, x.values[h]/dpa.sumCnt);
		    }
		}

		// 3. Adaptive safe Eta
		/* eta = n ||A||^2 / sum_i { max_j { (alpha_j * x_i)^2 }},
		   or, if Gaussian penalty is used:
		   eta = n ||A||^2/((n/var)||A||^2 + sum_i { max_j { (alpha_j * x_i)^2 }})
		 */
		double sumA2 = a.squareOfNorm();
		double sumQ2 = 0;
		for(int i=0; i< dpa.length(); i++) {
		    DataPoint p = dpa.points.elementAt(i);
		    double mp = 0;
		    for(double q: p.dotProducts(a, dis)) {
			mp = Math.max(mp, Math.abs(q));
		    }
		    sumQ2 += mp*mp * dpa.las.elementAt(i).sumCnt;
		}
		double eta = (ivar==0) ?
		    dpa.sumCnt * sumA2 / sumQ2 :
		    dpa.sumCnt * sumA2 / (sumQ2 +   dpa.sumCnt * ivar * sumA2);

		if (Suite.verbosity>0) {
		    System.out.println("[SD] |grad L|=" + Math.sqrt(sumA2) +", eta := " + eta);	
		}
		a.multiplyBy(eta);
		w.add(a);
		t++;
		sumEta+=eta;

		// "bonus" steps - try to keep going in the same
		// direction, with increasingly longer steps, as long
		// as log-lik keeps increasing

		double savedLogLik = dpa.logLikelihood(this, null);
		if (ivar!=0) {
		    double sumB2 = w.squareOfNorm();
		    savedLogLik -= ivar/2*sumB2;
		}

		while(true) {
		    BetaMatrix savedW = new BetaMatrix(w);
		    final double f = 2;
		    eta *= f ;
		    a.multiplyBy(f);
		    w.add(a);
		    double newLogLik =  dpa.logLikelihood(this, null);
		    if (ivar!=0) {
			double sumB2 = w.squareOfNorm();
			newLogLik -= ivar/2*sumB2;
		    }
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


	public void describe(PrintWriter out, boolean verbose) {
	    
	    out.println("===TruncatedGradient Classifier["+dis.name+"]===");
	    out.println(   commonTrunc.describe());
	    out.println("--- W --------------");
	    if (verbose) {
		w.describe( out, suite.getDic());
	    } else {
		out.println("(Matrix content skipped)");
	    }
	}

	public Element saveAsXML(Document xmldoc) {	    
 	    Element de =   xmldoc.createElement(XMLUtil.CLASSIFIER);

	    de.appendChild(createParamsElement
	    	 (xmldoc, 
		  new String[] { PARAM.t},
		  new double[] { trunc.t} ));
	    
	    de.setAttribute(XMLUtil.DISCRIMINATION, dis.getName());
	    de.appendChild(w.saveAsXML(xmldoc, dis, suite.getDic(), "W"));
	    return de;
	}

	/** List of names of matrices that we may need to serialize
	 * (and deserialize)
	 */
	HashMap<String, Matrix> listMatrices() {
	    HashMap<String, Matrix> h = new HashMap<String, Matrix>();
	    h.put("W", w);
	    return h;
	}

	public long memoryEstimate() {
	    return Sizeof.OBJ +  Sizeof.OBJREF + w.memoryEstimate();
	}
   
	/** Parses the element for discrimination-specific parameters,
	    which may be supplied in the learner's description. Also,
	    ensures that the block's Truncation instance is in sync with 
	    commonTrunc.

	    Overrides the method in the parent class
	*/    
	void parseDSP(Element e) throws BoxerXMLException  {
	    // Make sure that we actually have a truncation instance
	    // in sync with the learner's "common" params. 2009-10-29
	    trunc = new Truncation( commonTrunc, new Matrix[]{w}, dis); // ZZ

	    if (e==null) return;
	    XMLUtil.assertName(e, PARAMETERS);

	    HashMap<String,Object> h =
		parseParamsElement(e,new String[] {PARAM.t}, new double[] {0});
	    int t = ((Number)(h.get(PARAM.t))).intValue();
	    trunc.setT(t); // part of saved history
	}


    } // end of inner class

    public TruncatedGradient(Suite _suite) throws org.xml.sax.SAXException, BoxerXMLException  {
	this(_suite, null);
    }

    /** Creates an instance of TruncatedGradient learner based on the
      content of an XML "learner" element. The element may be the
      top-level element of an XML file, or more often, an element
      nested within a "learners" element within a "learner complex"
      element.
      
    */
    TruncatedGradient(Suite _suite, Element e) throws org.xml.sax.SAXException,  BoxerXMLException  {
	super.init(_suite, e);
    }

    public void describe(PrintWriter out, boolean verbose) {
	out.println("===" + algoName() + " Classifier===");
	out.println("eta="+ eta +", g=" + g);
	//+ "\n"+		   commonTrunc.describe());
	for(LearnerBlock b: blocks)  b.describe(out, verbose);
	out.println("=====================");
	out.println("[NET] Main tables memory estimate=" + memoryEstimate() + " bytes");
 	out.flush();
    }

    Element saveParamsAsXML(Document xmldoc) {
	return createParamsElement
	    (xmldoc, 
	     new String[] {PARAM.theta,PARAM.eta,PARAM.g, PARAM.K},
	     new Object[] {new Double(commonTrunc.getTheta()),new Double(eta),
			   new Double(commonTrunc.getG()),  new Integer(commonTrunc.getK()) });
    }

    void parseParams(Element e) throws BoxerXMLException  {
	XMLUtil.assertName(e, Learner.PARAMETERS);

	double theta =Double.POSITIVE_INFINITY;
	int t=0; // saved position in the truncation sequence

	HashMap<String,Object> h = makeHashMap
	    ( new String[] { PARAM.theta, PARAM.eta, PARAM.g,PARAM.K, PARAM.t},
	      new Object[] {new Double(theta), new Double(eta), new Double(g), 
			    new Integer(K),  new Integer(0)});
	
	h = parseParamsElement(e,h);

	theta = ((Double)(h.get(PARAM.theta))).doubleValue();

	eta =  ((Double)(h.get(PARAM.eta))).doubleValue();
	g =  ((Double)(h.get(PARAM.g))).doubleValue();
	K = ((Number)(h.get(PARAM.K))).intValue();
	if (K<=0) throw new IllegalArgumentException("K=" + K + " in the XML learner definition. K must be a positive integer");
	t = ((Number)(h.get(PARAM.t))).intValue();
	commonTrunc = defaultCommonTrunc(theta);
	commonTrunc.setT(t); // part of saved history	
    }
    
    Truncation defaultCommonTrunc() {
	return defaultCommonTrunc(Double.POSITIVE_INFINITY);
    }

    /** Creates a truncation object that has correct parameters, but applies
	truncation to no matrix. 

	<p>If the suite has its priors sets, the Truncation priors-based
	object will be based on these priors, rather than on g.
     */
    private Truncation defaultCommonTrunc(double theta) {
	return  new Truncation(theta, g, K, new BetaMatrix[0], lazyT, suite.getPriors(), null); 
    }

       
    TruncatedGradientLearnerBlock createBlock(Discrimination dis, LearnerBlock model) {

	return new TruncatedGradientLearnerBlock(dis, (TruncatedGradientLearnerBlock) model);
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