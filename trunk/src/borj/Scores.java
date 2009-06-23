package borj;

import java.util.*;
import boxer.*;

/** A Scores instance contains scoring data (obtained with a
 * particular learner) for the entire suite
 */
class Scores extends Vector<ScoreEntry[]> {

    double logLik[];
    int logLikCnt[];
  
    Scores(Suite suite) {
	super();	
	int ndis =suite.disCnt();
	logLik = new double[suite.disCnt()];
	logLikCnt = new int[suite.disCnt()];  	    
	//System.out.println("DEBUG: logLik["+ndis+"]");
    }

    /** ... And it also resizes the logLik arrays */
    void evalScores(DataPoint x, Suite suite, double[][] prob) {

	// resize as needed
	int ndis =suite.disCnt();
	if (logLik.length < ndis) logLik = Arrays.copyOf(logLik, ndis);
	if (logLikCnt.length < ndis) logLikCnt = Arrays.copyOf(logLikCnt, ndis);

	for(int i=0; i<ndis; i++) {
	    int r = suite.getDisc(i).claCount();
	    if (i==size()) {
		ScoreEntry[] q= new ScoreEntry[r];
		for(int j=0; j<r; j++) q[j] = new ScoreEntry();		
		add(q);
	    } else if (elementAt(i).length<r) {
		int r0 = elementAt(i).length;
		ScoreEntry[] q= Arrays.copyOf(elementAt(i),r);
		for(int j=r0; j<r; j++) q[j] = new ScoreEntry();
		set(i,q);
	    }
	}

	Discrimination.Cla[] chosen=x.interpretScores(prob,suite);

	if (Suite.verbosity>1) System.out.print("Classifier assigned "+x.getName()+" to:");
	for(int did=0; did<chosen.length; did++) {
	    ScoreEntry[] q = elementAt(did);
	    if (chosen[did]==null) continue; // empty discr
	    int cpos = chosen[did].getPos();
	    //System.out.println("se["+did+"]["+cpos+"].chosenCnt++");
	    q[cpos].chosenCnt ++;
	    Discrimination dis = suite.getDisc(did);
	    Discrimination.Cla trueC = x.claForDisc(dis);

	    boolean correct = (chosen[did] == trueC);

	    if (correct) {
		// our classifier was "correct" - it matched the oracle
		q[cpos].tpCnt ++;
	    }

	    if (Suite.verbosity>1) System.out.print(" " + chosen[did] +  (correct? " (C)" :" (I);"));
	}

	for(Discrimination.Cla c: x.getClasses(suite)) {
	    elementAt( suite.getDid(c) )[ c.getPos() ].oracleCnt ++;
	}

	if (Suite.verbosity>1) System.out.println();

    }


    /** Print a report on the quality of our classifier's scoring so far 
     @param prefix Append it to each line, for easy retrieval with 'grep'*/
    String scoringReport( Suite suite, String prefix) {

	int disCnt =suite.disCnt();
  
	StringBuffer b = new StringBuffer();
	// order by dicscrimination
	for(int k=0; k<disCnt; k++) {
	    ScoreEntry[] q = elementAt(k);
	    Discrimination dis = suite.getDisc(k);

	    for(int i=0; i<q.length; i++) {
		b.append(prefix + dis.getClaById(i) + ": " +q[i].report()+"\n");
	    }
	}
	return b.toString();
    }

    String loglikReport( Suite suite, String prefix) {
	StringBuffer b = new StringBuffer();
	for(int j=0;j< logLik.length; j++) {
	    if (logLikCnt[j]==0) continue; // empty discr
	    double w = logLik[j]/logLikCnt[j];
	    b.append(prefix+ "[" +suite.getDisc(j).getName()+"] " + w + "\n"); 
	}
	return b.toString();
    }

    void deleteDiscr(int delDid) {
	removeElementAt(delDid);
	logLik = deleteElement(logLik, delDid);
	logLikCnt = deleteElement(logLikCnt, delDid);	
    }

    /** Removes an element from an array */
    static double[] deleteElement(double a[], int del) {
	if (del >= a.length) throw new IllegalArgumentException("Del index "+del+" out of range");
	double b[] = Arrays.copyOf(a, a.length-1);
	for(int i=del; i<b.length; i++) b[i] = a[i+1];
	return  b;
    }
 
    static int[] deleteElement(int a[], int del) {
	if (del >= a.length) throw new IllegalArgumentException("Del index "+del+" out of range");
	int b[] = Arrays.copyOf(a, a.length-1);
	for(int i=del; i<b.length; i++) b[i] = a[i+1];
	return  b;
    }


}
 