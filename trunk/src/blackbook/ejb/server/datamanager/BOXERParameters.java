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