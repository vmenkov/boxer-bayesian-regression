package borj.rcv;

import boxer.*;

import java.util.*;
import java.io.*;

/** Reads an id list file with one integer doc id per line */
class RcvUtil {

    static int[] readIds(String fname, int from, int to) throws IOException {
	LineNumberReader r = new LineNumberReader( new FileReader(fname));
	String s=null;
	int cnt=0;
	Vector<Integer> v = new	Vector<Integer>();
	while( (s=r.readLine()) != null) {
	    s=s.trim();
	    if (s.equals("")) continue;
	    cnt++;
	    if (from > 0 && cnt < from) continue;
	    if (to > 0 && cnt > to) continue;
	    v.add( new Integer(s));
	}
	int a[] = new int[v.size()];
	for(int i=0; i<a.length; i++) {
	    a[i] = v.elementAt(i).intValue();
	}
	return a;
    }

}