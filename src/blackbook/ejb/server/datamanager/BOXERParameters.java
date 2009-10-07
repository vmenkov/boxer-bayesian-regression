package blackbook.ejb.server.datamanager;

import java.io.Serializable;

/**
 * Parameters used for the BOXERParameters algorithm
 */
public class BOXERParameters implements Serializable {

    /** algorithm name */
    private String algorithmName = null;

    /** algorithm id */
    private long algorithmID = 0;

    /**
     * Default constructor.
     */
    public BOXERParameters() {
    }

    /**
     * Constructor that takes a algorithmName.
     * 
     * @param algorithmName
     *            the algorithm name.
     */
    public BOXERParameters(String algorithmName) {
        this.algorithmName = algorithmName;
    }

    /**
     * Constructor that takes a algorithmID.
     * 
     * @param algorithmID
     *            the algorithm ID.
     */
    public BOXERParameters(long algorithmID) {
        this.algorithmID = algorithmID;
    }

    /**
     * Constructor that takes both the algorithmName and algorithmID.
     * 
     * @param algorithmName
     *            the algorithm name.
     * @param algorithmID
     *            the algorithm ID.
     */
    public BOXERParameters(String algorithmName, long algorithmID) {
        this.algorithmName = algorithmName;
        this.algorithmID = algorithmID;
    }

    /**
     * @return algorithm ID
     */
    public long getAlgorithmID() {
        return algorithmID;
    }

    /**
     * @param algorithmID
     *            the algorithm ID to set.
     */
    public void setAlgorithmID(long algorithmID) {
        this.algorithmID = algorithmID;
    }

    /**
     * @return algorithm name
     */
    public String getAlgorithmName() {
        return algorithmName;
    }

    /**
     * @param algorithmName
     *            the name of the algorithm to set.
     */
    public void setAlgorithmName(String algorithmName) {
        this.algorithmName = algorithmName;
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