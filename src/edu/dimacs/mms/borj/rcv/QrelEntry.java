package borj.rcv;

import boxer.*;

import java.util.*;
import java.io.*;

/** An auxiliary class for storing an entry from a RCV1-v2 QREL file 
 */
class QrelEntry {
    int docid;
    String cat;
    public QrelEntry(    int _docid,    String _cat) {
	docid = _docid;
	cat = _cat;
    }

}
    

