package blackbook.ejb.server.datamanager;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import security.ejb.client.User;
import blackbook.ejb.client.datamanager.URIBean;
import blackbook.ejb.client.exception.BlackbookSystemException;

import com.hp.hpl.jena.rdf.model.Literal;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.util.iterator.ExtendedIterator;

/* The first model is going to be the whole world.
 * The second model is a model with partial information about some URI
 * resources. Our goal now is to fill in this second model with the new
 * information from the first model.
 * This is different than Combine Models since we are picky about what is
 * included.
 * 
 * At this point, we don't know what to do if both models share some information.
 */

public class BOXERFillIn extends AbstractAlgorithmModel2Model {
	
	private static final String DESCRIPTION = "Takes in two models - one with full information and one with partial - and fills in the partial information with more gleaned from the world.";

    private static final String LABEL = "BOXER Fill In";

	/**
	 * 
	 */
	private static final long serialVersionUID = -6299969867860886465L;

	private final static String REGION_RDF = "http://www.w3.org/2001/vcard-rdf/3.0#Region";
    private static final String REGION_PROPERTY_ANALYST = "http://www.blackbook.com/analyst/assertion/region";

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}
	
	/**
	 * model2 is the partial information we want filled in.
	 * We will take the URIs from model2 and materialize to get
	 * the rest of the information.
	 * As a consequence, we will get information from ALL data sources . . . grrrrr.
	 * @throws URISyntaxException 
	 */

	public Model executeAlgorithm(User user, Model model)
			throws BlackbookSystemException {
		
		/* We will have to remember what the analyst said, since
		 * we will have to modify things later.
		 */
		
		HashMap<String,String> analyst_assertions = new HashMap<String,String>();
		
		ExtendedIterator model_URIResources = BOXERFilter.listURIResources(model);
		
		Set<URIBean> URIs = new HashSet<URIBean>(); 
		/* We only want to get URIs that have analyst assertions made */
		while (model_URIResources.hasNext()) {
			Resource r = (Resource) model_URIResources.next();
			String analyst_assertion = BOXERFilter.genericFinder(model,r,REGION_PROPERTY_ANALYST);
			/* In this case, the analyst has made an assertion, and we include */
			if (!( analyst_assertion.isEmpty() )) {
				try {
					URIs.add(new URIBean(r.getURI()));
				} catch (URISyntaxException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				analyst_assertions.put(r.getURI(),analyst_assertion);
			}
		}
		
		JenaMaterialize materializer = new JenaMaterialize();
		Model the_world = materializer.executeAlgorithm(user,null,null,URIs);
		
		/* Now we have to go back and replace the region information with 
		 * what the analyst said.
		 */
		
		ExtendedIterator new_URIResources = BOXERFilter.listURIResources(the_world);
		while (new_URIResources.hasNext()) {
			Resource r = (Resource) new_URIResources.next();
			BOXERFilter.replaceRegion(the_world,r,analyst_assertions.get(r.getURI()));
		}
		return the_world;
	}
	
	 /**
     * @see blackbook.ejb.server.datamanager.AbstractAlgorithmImpl#getDescription()
     */
    @Override
    public String getDescription() {
        return DESCRIPTION;
    }

    /**
     * @see blackbook.ejb.server.datamanager.AbstractAlgorithmImpl#getLabel()
     */
    @Override
    public String getLabel() {
        return LABEL;
    }

}
