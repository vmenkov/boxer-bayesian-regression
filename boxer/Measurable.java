package boxer;

/** Data structures implementing this interface can estimate their
 * memory use */
public interface Measurable {
    /** Returns an estimate of the memory use of this object in bytes. */
   public long memoryEstimate();
}

 