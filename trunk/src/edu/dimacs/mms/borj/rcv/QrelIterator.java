package edu.dimacs.mms.borj.rcv;

import java.util.*;
import java.io.*;

/** Allows Iterator-like access to the content of a QREL file
 */
class QrelIterator implements Iterator<QrelEntry> {
    
    private String fname="";
    private LineNumberReader r = null;
    private boolean closed = false;
    
    private String s=null;
    /** How many lines have been read so far */
    int cnt=0; 

    QrelEntry nextVal=null;

    public QrelIterator(String _fname) throws IOException {
	fname = _fname;
	r = new LineNumberReader( new FileReader(fname));
    }

    /** Checks if there are more data, and if not, closes the file */
    public boolean hasNext() {
	if (nextVal ==null) nextVal = readNext();
	if (nextVal==null && !closed) {
	    try {
		r.close(); 
	    } catch(IOException e) {}
	    closed=true;
	}
	return (nextVal !=  null);
    }

    public QrelEntry next()throws  NoSuchElementException {
	if (nextVal ==null) nextVal = readNext();
	QrelEntry q = nextVal;
	nextVal=null;
	return q;
    }

    private QrelEntry readNext() throws NoSuchElementException {
	String s;
	try {
	    s=r.readLine();
	} catch(IOException e) {
	    throw new NoSuchElementException("IOException: " + e.getMessage());
	}
	if (s==null) return null;
	s=s.trim();
	cnt ++;
	String q[] = s.split("\\s+");
	if (q.length != 3) {
	    throw new  IllegalArgumentException("Reading " + fname + ", line "+cnt+": Split line `"+s+"', and got "+q.length+" tokens!");
	}
	String cat=q[0];
	int docid=Integer.parseInt(q[1]);
	if (!q[2].equals("1")) throw new IllegalArgumentException("Expected all lines of file " + fname + " to end in `1'");
	return new QrelEntry(docid, cat);
    }

    /** Unsupported */
    public void remove()    throws        UnsupportedOperationException {
	throw new UnsupportedOperationException();
    }

}

    

