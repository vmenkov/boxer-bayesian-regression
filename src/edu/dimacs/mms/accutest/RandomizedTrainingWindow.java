package edu.dimacs.mms.accutest;

import java.util.*;
//import java.io.*;
//import java.text.*;

import edu.dimacs.mms.boxer.*;

/** Support for "buffering" training examples, and feeding them to the
    learner multiple times.

    Presently, this class is used by WindowRepeater.
 */
class RandomizedTrainingWindow extends TrainingWindow {
    final int offset;
    
    private int usedCnt[];
    private int filled = 0;
   
    private Random gen; 


    /** Creates a "window" of a given size, with a given data source
      and a given learner

      @param _w How many examples will be stored in the buffer 
      @param _rep How many times (on average) each stored example will be fed to  the learner
      @param _src Source of examples (e.g., an iterator over a vector
      of DataPoints)
      @param _learner Learner to which examples will be fed
     */
    RandomizedTrainingWindow(int _w, int _rep, Learner _learner, Iterator<DataPoint> _src, long seed) {
	super(_w, _rep, _learner, _src);

	offset = w/rep;
	if (offset * rep != w) throw new IllegalArgumentException("Window size ("+w+") not divisible by repetition factor " + rep);
	usedCnt = new int[w];
	gen = new Random(seed); 
    }
    
    

    /** Feeds one example - a new one, or a previously used "buffered"
	one - to the learner.

       @param canFinalize Controls behavior when there are no more
       examples to read. If the flag is false, we report an error
       right away; if true, we keep feeding stored examples to the
       learner, until each one has been fed rep times.

       @return false if it can't get more examples
    */
    boolean absorbNextExample(boolean canFinalize) {

	if (fedCount() % offset == 0) {
	    if (!src.hasNext()) return false;
	    DataPoint p = src.next();
	    srcCnt ++;
	    if (filled < w) {
		data[filled++] = p;
	    } else {
		data[ gen.nextInt(filled) ] = p;
	    }
	    doAbsorb(p);
	} else {
	    doAbsorb( data[ gen.nextInt(filled)] );
	}
	return true;
    }


    String describe() {
	return "Randomized window, size="  +  w+ ", repeating each example on average " + rep + " times";
    }

  
}
    

