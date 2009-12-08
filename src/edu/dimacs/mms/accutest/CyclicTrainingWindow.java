package edu.dimacs.mms.accutest;

import java.util.*;
//import java.io.*;
//import java.text.*;

import edu.dimacs.mms.boxer.*;

/** Support for "buffering" training examples, and feeding them to the
    learner multiple times.

    Presently, this class is used by WindowRepeater.
 */
class CyclicTrainingWindow extends TrainingWindow {
    final int offset;
    
    private int usedCnt[];
    private int next = 0;
    
    /** Creates a "window" of a given size, with a given data source
      and a given learner

      @param _w How many examples will be stored in the buffer 
      @param _rep How many times each stored example will be fed
      @param _src Source of examples (e.g., an iterator over a vector
      of DataPoints)
      @param _learner Learner to which examples will be fed
     */
    CyclicTrainingWindow(int _w, int _rep, Learner _learner, Iterator<DataPoint> _src) {
	super(_w, _rep, _learner, _src);

	offset = w/rep;
	if (offset * rep != w) throw new IllegalArgumentException("Window size ("+w+") not divisible by repetition factor " + rep);
	usedCnt = new int[w];
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
	if (next > 0 && data[next]==null && next%offset == 0) {
	    // early turn-backs done at initial stages, when the buffer
	    // has not been fully loaded yet
	    next = 0;
	}

	// get one more example, if needed
	if (data[next] == null || usedCnt[next] >= rep) {
	    if (src.hasNext()) {
		data[next] = src.next();
		srcCnt ++;
		usedCnt[next] = 0;
	    } else {
		if (!canFinalize) return false; // Can't!
		// finalization stage - looking for loaded examples with 
		// less than "rep" presentations so far
		int h=0;
		while(h < w && 
		      (data[(next+h)%w]==null || usedCnt[(next+h)%w] >= rep)) {
		    h++;
		}
		if (h==w) return false; // nothing to repeat either
		next = (next+h)%w;
	    }

	}
	doAbsorb(data[next]);
	usedCnt[next]++;
	next = (next+1) % w;
	return true;
    }

    String describe() {
	return "Cyclic window, size="  +  w + ", repeating each example exactly " + rep + " times";
    }


}
    

