package edu.dimacs.mms.borj.rcv;

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