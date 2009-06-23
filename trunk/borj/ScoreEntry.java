package borj;

/** A ScoreEntry instance contains scoring data for one class of one
  discrimination */
class ScoreEntry {
    /** How many data points are in this class according to the oracle? */
    int oracleCnt;
    /** How many data points have been assigned to this class by our classifier? */
    int chosenCnt;
    /** True positives - data points that both the oracle and our classifier assign to this class */
    int tpCnt;
    String report() {
	return "Recall=" + tpCnt + "/" + oracleCnt + 
	    (oracleCnt>0? String.format("=%4.3f", (double)tpCnt/(double)oracleCnt) : "") +
	    ", Precision=" + tpCnt + "/" + chosenCnt +
	    (chosenCnt>0? String.format("=%4.3f", (double)tpCnt/(double)chosenCnt) : "");
    }
}
