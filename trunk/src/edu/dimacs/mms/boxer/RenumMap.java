package boxer;

import java.util.Vector;

/** An auxiliary class used to delete data pertaining to a particular
 * discrimination from learners' matrices and arrays when the
 * discirmination is deleted.
 */
class RenumMap {
    /** Maps old column ids to new column ids. If an element is  -1, it means that the column must be deleted.
     */
    int[] renumMap;
    private int[] sizeMap;
    /** The discrimination ID of the discrimination being deleted */
    int did;

    /** Creates a map that describes deletion of the classes belonging
	to Discrimination d from the  vector of classes id2cla	
     */
    RenumMap(Vector<Discrimination.Cla> id2cla, int _did, Discrimination d) {
	did = _did;
	renumMap  = new int[ id2cla.size() ];
	int pos = 0;
	for(int i=0; i<id2cla.size(); i++) {
	    renumMap[i] = (id2cla.elementAt(i).getDisc() == d) ? -1 : (pos++);
	}
	makeSizeMap();
    }

    private void makeSizeMap() {
	sizeMap = new int[renumMap.length+1];
	sizeMap[0] = 0;
	for(int i=0; i<renumMap.length; i++) {
	    sizeMap[i+1] = sizeMap[i] +  (renumMap[i]>=0 ? 1 : 0);
	    if (renumMap[i]>=0) {
		if (sizeMap[i+1] != renumMap[i]+1) throw new AssertionError("renumMap is not ordered as expected");
	    }
	}
    }

    /** Deletes the elements corresponding to a deleted discrimination
	from an array that's aligned with suite.id2cla
     */
    int[] applyTo(int v[]) {
	if (v==null || sizeMap[v.length] == v.length) return v; //nothing to remove
	int w[] = new int[ sizeMap[ v.length]];
	for(int i=0; i<v.length; i++) {
	    if (renumMap[i]>0) w[renumMap[i]] = v[i];
	}
	return w;
    }

    /** Deletes the elements corresponding to a deleted discrimination from an
	array that's aligned with suite.id2cla
     */
    double[] applyTo(double v[]) {
	if (v==null || sizeMap[v.length] == v.length) return v; //nothing to remove
	double w[] = new double[ sizeMap[ v.length]];
	for(int i=0; i<v.length; i++) {
	    if (renumMap[i]>0) w[renumMap[i]] = v[i];
	}
	return w;
    }

    /** deletes an element from the position No. "did" of an array
     * that stores one element per discrimination
     */
    @SuppressWarnings("unchecked")
    <T> T[] deleteDisElement(T[] a) {
	T[] b = (T[]) new Object[a.length-1];
	for(int i=0; i<did; i++) b[i] = a[i];
	for(int i=did; i<b.length; i++) b[i] = a[i+1];
	return b;
    }
	

}
