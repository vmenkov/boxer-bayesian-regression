package edu.dimacs.mms.boxer;

import java.io.*;
import java.util.*;

/** An auxiliary class for {@link NormalizedKnnLearner}, this class stores a specified number
    of the top values from those "offered" to it. It may store   more, if it's needed to break a tie.
     */
 class TopValues {

     /** it's like a Vector of DataPoints, but each recorded point may represent several identical
	 vectors (accessible via dataPointInfo); the sum of their multiplicities is stored.
     */
     static class VectorDP extends  Vector<DataPoint> {
	 int multi=0;
	 VectorDP(int n) { super(n); }
	 public boolean  add(DataPoint o) {
	     throw new UnsupportedOperationException("must use add(p,multi) instead");
	 }
	 boolean add(DataPoint p, int _multi) {
	     super.add(p);
	     multi += _multi;
	     return true;
	 }
     }
     
     /** Each vector contains all DataPoints with the same cos
	 value. They are ordered in the order of ascending cos. */
     SortedMap<Double, VectorDP> map=new TreeMap<Double, VectorDP>();

     final int desiredSize;
     final double mincos;

     /** number of DataPoint objects stored (summing their multiplicities) */
     private int cnt=0;

     int getCnt() { return cnt; }

     TopValues(int n, double _mincos) {
	 desiredSize=n;
	 if (desiredSize<1) throw new IllegalArgumentException();
	 mincos = _mincos;
     }

     /** Insert the value; adjust counts.	*/
     private void insert(DataPoint q, int multi, double cos) {
	 Double key = new Double(cos);
	 VectorDP v = map.get(key);
	 if (v == null)  map.put(key, v= new VectorDP(1));
	 v.add(q, multi);
	 cnt += multi;
     }
     
     /** Removes the values (a vector of them, that is) with the lowest key value, if doing this
	 does not make the stored element count less than the desired size. Adjusts the count.
     */
     private void trimIfCan() {
	 while (cnt > desiredSize) {
	     Double key = map.firstKey();
	     int cnt1 = cnt - map.get(key).multi;
	     if (cnt1 < desiredSize) return;
	     map.remove(key);
	     cnt=cnt1;
	 }
     }
     
     void offer(DataPoint q, int _multi, double cos) {
	 if (cos <= mincos) return;
	 if (cnt<desiredSize || cos >=  map.firstKey().doubleValue()) {
	     insert(q,_multi, cos);
	     trimIfCan();
	 } 
     }
     
 }


