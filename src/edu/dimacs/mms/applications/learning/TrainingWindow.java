package edu.dimacs.mms.applications.learning;

import java.util.*;
//import java.io.*;
//import java.text.*;

import edu.dimacs.mms.boxer.*;

/** Support for "buffering" training examples, and feeding them to the
    learner multiple times.

    Presently, this class is used by WindowRepeater.
 */
abstract class TrainingWindow {
    /** Window size */
    final int w;
    final Learner algo;
    /** Source of new training examples: e.g., an iterator on a Vector
	of DataPoint objects, or some kind of data reader
     */
    final Iterator<DataPoint> src;
    final int rep;
    
    protected DataPoint data[];
    protected int srcCnt = 0;
    private int fedCnt=0;
    
    /** Creates a "window" of a given size, with a given data source
      and a given learner

      @param _w How many examples will be stored in the buffer 
      @param _rep How many times each stored example will be fed
      @param _src Source of examples (e.g., an iterator over a vector
      of DataPoints)
      @param _learner Learner to which examples will be fed
     */
    TrainingWindow(int _w, int _rep, Learner _learner, Iterator<DataPoint> _src) {
	w = _w;
	rep = _rep;
	if (w < 1) throw new IllegalArgumentException("Illegal window size w="+w);
	if (rep < 1) throw new IllegalArgumentException("Illegal example repetition count rep="+rep);
	algo = _learner;
	src=_src;
	data = new DataPoint[w];
    }
    
    /** Feeds one example - a new one, or a previously used "buffered"
	one - to the learner.

       @param canFinalize Controls behavior when there are no more
       examples to read. If the flag is false, we report an error
       right away; if true, we keep feeding stored examples to the
       learner, until each one has been fed rep times.

       @return false if it can't get more examples
    */
    abstract boolean absorbNextExample(boolean canFinalize);

    /** How many examples have been fed to the learner so far
     * (counting each "absorbExample()" call, i.e. counting each
     * example as many times as it has been presented to the learner).
     */
    int fedCount() { return fedCnt; }
    int srcCount() { return srcCnt; }

    protected void doAbsorb(DataPoint p) {
	Vector<DataPoint> v = new Vector<DataPoint>(1);
	v.addElement(p);
	algo.absorbExample(v);
	fedCnt++;
    }
  
    abstract String describe();

}
    

