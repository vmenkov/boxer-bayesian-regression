package edu.dimacs.mms.boxer;

import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Vector;

import org.w3c.dom.Document;
import org.w3c.dom.Element;


/** Using Exponentiated Gradient for training of Polytomous Logistic
  Regression Models (PLRM).

  As with other learners, the parameters of this learner can be
  controlled via the XML element from which it can be initialized.

  Since there is in fact a completely independent classifier for each
  discrimination, one can generally control the learner's parameters
  on the global level (for all existing and future discriminations at
  once), or for individual discriminations.

  This learner also supports truncation (which, if requested, applies
  to the latent coefficients V). However, the truncation is rather
  expensive, since it does not use lazy evaluation, 
  
 */
public class ExponentiatedGradient extends PLRMLearner {

    /** We cant't do lazy truncation here, because W needs to be
    updated from V after each training vector
    */
    static final boolean lazyT = false;

    /** Algorithm parameters - the "common" fixed values of U and F which
	are used for all discriinations if supplied like this.

	Fixed defaults set as of Ver 0.5.006 (2009-04-21)
     */
    double commonU=10.0, commonF=1.0;

    Truncation defaultCommonTrunc() {
	return new Truncation(lazyT);
    }

    /** If these flags are true, U and f will be computed on each
	training example based on our heuristics. This was default prior to 
	0.5.006 (2009-04-21); false thereafter.
    */
    boolean adjustU=false, adjustF=false;

    /** How many training data points from each class have we seen?
	This matrix serves as a wrapper for an array of class sizes,
	which is discrimination-specific aligned with the list of
	classes in the Discr.  For I/O reasons, the array is "wrapped"
	into a matrix as its only row.
    */
    static class ClassSizesMatrix extends DenseMatrix {

	ClassSizesMatrix() {}

	/** Copy constructor */
	ClassSizesMatrix(ClassSizesMatrix a) {
	    super(a);
	}
	/** Computes the size of the smallest (non-empty) class in the
	    training set. The result can be used in the heuristic for
	    setting the parameter U: "U is some fraction (perhaps 0.1
	    ... 4.0) of the number of training examples in the least
	    frequent class":

	    U  = alpha * min | C_k |,  with  0.1<alpha<4.0     

	*/
	private int minNonZeroClassSize() {
	    int m=0;
	    double classSizes[] = data[0];
	    for(int i=0; i<classSizes.length; i++) {
		if (classSizes[i]>0 && (m==0 || classSizes[i] < m)) {
		    m=(int)classSizes[i];
		}
	    }

	    // FIXME: this really should not be a problem, because if
	    // we have no training examples labeled with respect to a
	    // particular discrimination, we should not do any training there

	    //if (m==0) throw new AssertionError("minNonZeroClassSize() called when no non-empty classes are known in at least one of the discrimination yet!");
	    return m;
	}	
    }


    private Object reportAdj(boolean isAdjustable, double val) {
	return isAdjustable? Param.ADJUST: new Double(val);
    }


    private Object reportCommonU() { 
	return reportAdj(adjustU,commonU);
    }
    
    private Object reportCommonF() {
	return reportAdj(adjustF,commonF);
    }

    class ExponentiatedGradientLearnerBlock extends PLRMLearner.PLRMLearnerBlock {

	/** Discrimination-specific values of U, f */
	double U, f; 

	/** Matrices of latent coefficients. Sizes are [d][r], where d is
	    the number of features, and r is the number of classes. For
	    all-zero rows, null is stored. */
	DenseMatrix vplus=new DenseMatrix(), vminus=new DenseMatrix();

	/** Optional Truncation policy for Vplus and Vminus; by default, none */
	Truncation trunc = new Truncation(lazyT);

	/** How many training data points from each class have we
	    seen?  The array is aligned with the list of classes in
	    the Discr.  It may be reallocated if it needs resizing in
	    later training. For I/O reasons, the array is "wrapped" 
	    into a matrix as its only row.
	*/
	private ClassSizesMatrix classSizesMatrix = new ClassSizesMatrix();
	private double maxInfNorm = 0;
    
	/** Initializes block params based on the common params, or on
	 * the pre-existing fallback discrimination's learner block */
	ExponentiatedGradientLearnerBlock(Discrimination _dis, ExponentiatedGradientLearnerBlock b) {
	    dis = _dis;
	    if (b==null) {
		U=commonU;
		f=commonF;
		classSizesMatrix.enable(0, dis.claCount());
		if (commonTrunc==null) throw new IllegalArgumentException("commonTrunc=null");

		trunc = new Truncation( commonTrunc, new Matrix[]{ vplus, vminus}, dis);
	    } else {
		U=b.U;
		f=b.f;
		classSizesMatrix=new ClassSizesMatrix(b.classSizesMatrix);
		trunc = b.trunc.liveCopy( new Matrix[]{vplus, vminus}, dis);
	    }
	}

	/** Sets the block's truncation parameters */
	public void setTruncation(double theta, double g, int K) {
	    trunc = new Truncation(theta, g, K, new Matrix[] {vplus, vminus}, lazyT, null /* no individual priors in EG */, dis);
	}


	HashMap<String, Matrix> listMatrices() {
	    HashMap<String, Matrix> h = new	HashMap<String, Matrix>();
	    h.put("W", w); // FIXME: this should be optional
	    h.put("Vplus", vplus);
	    h.put("Vminus", vminus);
	    h.put(PARAM.classSizes,  classSizesMatrix);
	    return h;
	}

	/** Builds a BetaMatrix w from the matrix V (vplus and vminus)
	 */
	private BetaMatrix latentToModelAll() {
	    int r = dis.claCount(); // class count
	    double z[] = new double[r];  // individual sum for each class

	    int d =  vplus.data.length;

	    double aw[][] = new double[d][];

	    //  store max val for each column, to reduce chances of under/overflow
	    boolean set[] = new boolean[r]; 
	    double maxv[] = new double[r]; 
	    for(int j=0; j<d; j++) {
		double vp[] = vplus.data[j];
		double vm[] = vminus.data[j];
		if (vp==null && vm==null) continue; // both empty rows
		if (vp==null || vm==null) {
		    throw new AssertionError("We did not expect VP and VM to have mismatched row patterns!");
		} else if (vp.length != vm.length) {
		throw new AssertionError("Rows of VP and VM are of different length");
		}
		for(int k=0; k<vp.length; k++) {		    
		    double q = Math.max(vp[k], vm[k]);
		    if (!set[k] || q>maxv[k]) {
			set[k] = true;
			maxv[k] = q;
		    }
		}
	    }

	    // precompute 
	    double emaxv[] = new double[r]; 	    
	    for(int k=0; k<r; k++) {
		emaxv[k] = Math.exp( - maxv[k]);
	    }

	    for(int j=0; j<d; j++) {
		double vp[] = vplus.data[j];
		double vm[] = vminus.data[j];
		if (vp==null && vm==null) {
		    // both empty rows
		    for(int k=0; k<r; k++)  z[k] += 2 * emaxv[k];
		    continue; 
		}
		aw[j] = new double[r];
		for(int k=0; k<r; k++) {
		    if (k<vp.length) {
			double ep = Math.exp(vp[k] - maxv[k]);
			double em = Math.exp(vm[k] - maxv[k]);
			aw[j][k] = ep - em;
			z[k] += ep + em;
		    } else {
			z[k] += 2 * emaxv[k];
		    }
		}
	    }

	    for(int j=0; j<d; j++) {
		if (aw[j]==null) continue;
		for(int k=0; k<r; k++) {
		    aw[j][k] *= U/z[k];
		}
	    }
	    return new BetaMatrix(aw);
	}


	/** Count class membership */
	private void adjustClassSizes(Vector<DataPoint> xvec, int i1, int i2) {
	    double classSizes[] = classSizesMatrix.data[0];

	    for(int i=i1; i<i2; i++) {
		DataPoint x =  xvec.elementAt(i);
		Discrimination.Cla trueC = x.claForDisc(dis);
		if (trueC != null) {
		    classSizes[trueC.getPos()] ++;
		    maxInfNorm = Math.max(maxInfNorm, x.infNorm());
		}
	    }
	}


	/** Ensures that the algorithm params (such as U and f) are set
	    for all discriminations. Computes them if they arer
	    adjustable, or uses the common values otherwise.

	    Since the algorithm's heuristics provide for
	    discrimination-specific values for the parameters U and f,
	    we use the following procedure for setting them:<ol>
	    
	    <li>If a given param is specified in the XML file as "adjustble",
	    it is set individually for each discrimination for each example

	    // Set U and f according to heuristics in the 2002-0107 paper
	    // f = (beta=2 ... 40) / (U* xmax)^2
	    
	    
	    <li>If the param is not specified as adjustable, then a
	    common (for all discriminations) value, and, optionally,
	    an individual value for each discrimination (or some
	    discriminations only) is expected to be found in the XML
	    file. The parameter is then set appropriately (to the
	    specific or common value) for each discrimination upon
	    reading the XML file. Later on, if a new discrimination is
	    created, it is given the common value.

	    </ol>
	
	*/
	private void setParams(Vector<DataPoint> xvec, int i1, int i2) {
	    int d =  suite.getDic().getDimension(); // feature count

	    if (Suite.verbosity>1) System.out.println("ExponentiatedGradient.absorbExample:\n" +
			       "d="+d+"\n" + 
			       trunc.describe());

	    if (adjustU || adjustF) adjustClassSizes(xvec, i1, i2);

	    if (adjustU) {
		int minClassSize= classSizesMatrix.minNonZeroClassSize();
		U = minClassSize * 0.1;
		if (Suite.verbosity>1) System.out.println("minClassSize={"+  minClassSize +"}; U(adjustable)="+U);
	    } else {
		if (Suite.verbosity>1) System.out.println("U(fixed)="+U);
	    }

	    if (adjustF) {
		double q = U * maxInfNorm;
		f = 40.0 / (q*q);
		if (Suite.verbosity>1) System.out.println("maxInfNorm="+ maxInfNorm+"; f(adjustable)="+f);
	    } else {
		if (Suite.verbosity>1) System.out.println("f(fixed)="+f);
	    }
	}

	public void absorbExample(Vector<DataPoint> xvec, int i1, int i2) {
  
	    int d = suite.getDic().getDimension(); // feature count
	    setParams(xvec,i1,i2);

	    vplus.resize(d);
	    vminus.resize(d);
	    w = latentToModelAll();

	    for(int i=i1; i<i2; i++) {
		trunc.requestTruncation(d, f);//or we can move to bottom
		trunc.applyTruncationToAllRows(); // nothing is done by this
		// call (since lazeT==false), but we keep it here for
		// completeness


		DataPoint x = xvec.elementAt(i);

		double z[] = adjWeights(x); // (t-p)
		if (z==null) {
		    if (Suite.verbosity>1) System.out.println("Skip example " + x.name + " not labeled for " + dis.getName());
		    continue; // example not labeled for this discr
		}

		int r = z.length;
		for(int k=0; k<z.length; k++) {
		    z[k] *= U*f; // (t-p)*U*f
		}

		// update latent coefficients
		for(int h=0; h<x.features.length; h++) {
		    int j = x.features[h];
		    // Resize arrays, if more classes have been introduced
		    vplus.enable(j,r);
		    vminus.enable(j,r);
		    
		    for(int k=0; k<r; k++) {
			double f = z[k] * x.values[h];  // (t-p) * U * f * x
			vplus.data[j][k]  += f;
			vminus.data[j][k] -= f;
		    }
		}
		// convert latent coefficients to the model, to be
		// ready for scoring the next training vector
		w = latentToModelAll();
		//describe( System.out, false);
	    }

	}

	public void describe(PrintWriter out, boolean verbose) {
	    out.println("===ExponentiatedGradient Classifier["+dis.name+"]===");
	    
	    if (adjustU) {
		out.print("U(adjustable)=" + U);
	    } else {
		out.print("U(fixed)=" + U);
	    }

	    if (adjustF) {
		out.println(", f(adjustable)="+f);
	    } else {
		out.println(", f(fixed)="+f);
	    }

	    out.println(trunc.describe());
	    if (verbose) {
		out.println("---Vplus-----------");
		vplus.describe( out, suite.getDic());
		out.println("---Vminus-----------");
		vminus.describe( out, suite.getDic());
		out.println("--- W --------------");
		w.describe(out, suite.getDic());
    } else {
		out.println("---Vplus, Vminus, W: (skipped) ----");
	    }
	    out.println("=====================");
	    //out.println("Main tables memory estimate=" + memoryEstimate() + " bytes");	    
	    out.flush();
	}

	public long memoryEstimate() {
	    return Sizeof.OBJ + 3 * Sizeof.OBJREF + w.memoryEstimate() +
		vplus.memoryEstimate() +    vminus.memoryEstimate();
	}

	public Element saveAsXML(Document xmldoc) {	    
 	    Element de =   xmldoc.createElement(XMLUtil.CLASSIFIER);
	    de.setAttribute(XMLUtil.DISCRIMINATION, dis.getName());

	    de.appendChild(createParamsElement
	    	 (xmldoc, 
		  new String[] {PARAM.maxInfNorm, PARAM.f, PARAM.U, PARAM.t},
		  new double[] {maxInfNorm, f, U, trunc.t}
		  ));
	    
	    de.appendChild(classSizesMatrix.saveAsXML(xmldoc, dis, suite.getDic(), PARAM.classSizes));
	    de.appendChild(vplus.saveAsXML(xmldoc,dis,suite.getDic(),"Vplus"));
	    de.appendChild(vminus.saveAsXML(xmldoc,dis,suite.getDic(),"Vminus"));
	    de.appendChild(w.saveAsXML(xmldoc,dis, suite.getDic(), "W"));


	    return de;
	}


	/** Parses the element for discrimination-specific parameters,
	    which may be supplied in the learner's
	    description. Overrides the method in the parent
	    class. This method is invoked from PLRMLearner.parseDisc.

	    @param e Element to parse. If null, just initialize the
	    params of this block from the "common" values.
	*/    
	void parseDSP(Element e) throws BoxerXMLException{
	    HashMap<String,Object> h = makeHashMap
		(new String[] {PARAM.f, PARAM.U, PARAM.t, PARAM.maxInfNorm },
		 new Object[] { reportCommonF(),reportCommonU(),Zero, Zero});

	    if (e!=null) {
		XMLUtil.assertName(e, PARAMETERS);
		h=parseParamsElement(e,h);
	    }
	    
	    Object o = h.get(PARAM.f);
	    if (!o.equals(Param.ADJUST)) f = ((Double)o).doubleValue();
	    
	    o = h.get(PARAM.U);
	    if (!o.equals(Param.ADJUST))  U =  ((Double)o).doubleValue();

	    int t = ((Number)(h.get(PARAM.t))).intValue();

	    maxInfNorm = ((Double)(h.get(PARAM.maxInfNorm))).doubleValue();
	
	    trunc.setT(t); // part of saved history

	    // report classSizes
	    //if ( classSizesMatrix.data.length>0) {
	    //	double[] classSizes = classSizesMatrix.data[0];
	    //	System.out.print("Read classSizes=(");
	    //	for(int s: classSizes) 	System.out.print(" " + s);
	    //	System.out.println(")");
	    //}

	    //System.out.println("[DEBUG]: done a block's parseDSP; U="+U+", f="+f);
	}

    } // end of inner class


    public ExponentiatedGradient(Suite _suite) throws org.xml.sax.SAXException, BoxerXMLException   {
	this(_suite, null);
    }

    /** Creates an instance of ExponentiatedGradient learner based on the
      content of an XML element (which may be the top-level element of
      an XML file), or more often, an element nested within a
      "learners" element within a "learner complex" element.
      
    */
    public ExponentiatedGradient(Suite _suite, Element e) throws
	org.xml.sax.SAXException, BoxerXMLException {
	super.init(_suite, e);
    }



    /** Enables truncation of the latent coeffient matrix.

	This method does NOT set the truncation parameters for each
	individual block - only for the "common" ones (on which
	blocks' params will be modeled); therefore, this method can
	only be called before starting to train the classifier.
     
	The truncation in EG is currently done using exactly the same
	paradigm as in TG, but is applied to the elements of V (latent
	coeff) rather than W (model). Truncation is carried out on
	each K-th call to absorbExample, before the latentToModel
	procedure.

	@param theta Theta; a negative value means "infinity",
	i.e. "always"; 0 means "never"

	@param to Truncate the elements of V by this much every on
	every K-th step (so this is equivalent to g*K in the
	TruncatedGriadient write-up). More precisely, Truncation will
	be carried out in the t-th call to absorbExample (before
	latentToModel is called) if t is divisible by K; t starts from
	one.

	@param K Truncate after so many steps

     */
    public void setTruncation(double theta, double g, int K) {
	commonTrunc = new Truncation(theta, g, K, new Matrix[0], lazyT, null /** no individual priors in EG */, null);
	//for(LearnerBlock b: blocks)  ((ExponentiatedGradientLearnerBlock)b).setTruncation(otheta, to,  K);
    }

    public void describe(PrintWriter out, boolean verbose) {
	out.println("===" + algoName() +" Classifier===");
	for(LearnerBlock b: blocks)  b.describe(out, verbose);
	out.println("=====================");
	out.println("[NET] Main tables memory estimate=" + memoryEstimate() + " bytes");
 	out.flush();
    }

    Element saveParamsAsXML(Document xmldoc) {
	return createParamsElement
	    (xmldoc, 
	     new String[] { PARAM.theta, PARAM.g, PARAM.K, PARAM.f, PARAM.U},
	     new Object[] {commonTrunc.reportTheta(),
			   new Double(commonTrunc.getG()),
			   new Integer(commonTrunc.getK()), 
			   reportAdj(adjustF, commonF),
			   reportAdj(adjustU, commonU)}
	     );
    }


    /** Parses the "common" (applicable to all blocks) learner
     * parameters. Makes sure that if a param is not supplied, we
     * keep the defaul
     */
    void parseParams(Element e) throws BoxerXMLException  {
	XMLUtil.assertName(e, Learner.PARAMETERS);

	double theta = 0; // no truncation
	double g = 0;
	int K = 1;
	HashMap<String,Object> h = makeHashMap
	    (new String[] { PARAM.theta, PARAM.g, PARAM.K,  PARAM.f, PARAM.U},
	     new Object[]{Zero,Zero, new Double(K), reportCommonF(), reportCommonU()});
	
	h =  parseParamsElement(e,h);

	theta =  ((Double)(h.get(PARAM.theta))).doubleValue();
	g =  ((Double)(h.get(PARAM.g))).doubleValue();
	
	Object o = h.get(PARAM.f);
	if (o!=null) adjustF =  o.equals(Param.ADJUST);
	if (!adjustF) commonF=  ((Double)o).doubleValue();
	
	o = h.get(PARAM.U);
	if (o!=null) adjustU = o.equals(Param.ADJUST);
	if (!adjustU) commonU=  ((Double)o).doubleValue();
	
	K = ((Number)(h.get(PARAM.K))).intValue();
		
	setTruncation(theta, g, K);

	//System.out.println("[DEBUG]: EG.parseParams: commonF=" +  reportCommonF()+
	//			   " commonU=" +  reportCommonU());
    }

    ExponentiatedGradientLearnerBlock createBlock(Discrimination dis, LearnerBlock model) {
	return new  ExponentiatedGradientLearnerBlock(dis, (ExponentiatedGradientLearnerBlock)model);
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