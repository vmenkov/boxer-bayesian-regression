package blackbook.ejb.server.datamanager;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.ResIterator;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.util.iterator.ExtendedIterator;
import com.hp.hpl.jena.util.iterator.Filter;

import security.ejb.client.User;
import blackbook.ejb.client.exception.BlackbookSystemException;
import blackbook.ejb.server.datamanager.AbstractAlgorithmModel2Model;

public class BOXERFilter extends AbstractAlgorithmModel2Model {
	
    /**
	 * 
	 */
	private static final long serialVersionUID = 3791236414011234099L;

	private static final String DESCRIPTION = "BOXER Filter";

    private static final String LABEL = "BOXER Filter";
    
    private static String REGION = "JAP";
    private static String INDUSTRY = "I00380";
    
    /** logger */
    private static Log logger = LogFactory.getLog(BOXERFilter.class);
	
	private final static int VERBOSE = 0;
	
	/**
	 * @param args
	 * @throws IOException 
	 */
	
	private final static String REGION_RDF = "http://www.w3.org/2001/vcard-rdf/3.0#Region";
	private final static String INDUSTRY_RDF = "http://blackbook.com/terms/INDUSTRY";
	private final static String FEATURES_RDF = "http://blackbook.com/terms/FEATURES";
	private final static String IDENTIFIER_RDF = "http://purl.org/dc/elements/1.1/identifier";
	
	/**
	 * 
	 *  This is the constructor for the Filter class, which inherently has to be
	 *  linked to a model. For now we will only be dealing with region and industry
	 *  filters, but this can easily be generalized to (property,object) pairs.
	 *  
	 * @param m 				-	The Jena Model
	 * @param region_filter		-	The name of the region we're looking for.
	 * @param industry_filter	-	The name of the industry we're looking for.
	 */
	
	// public BOXERFilter (Model m, String region_filter, String industry_filter) {
		// this.region_filter = region_filter;
		// this.industry_filter = industry_filter;
		
		// model = m;
		
		// region_property = model.createProperty(REGION_RDF);
		// industry_property = model.createProperty(INDUSTRY_RDF);
		
		// counter = 1;
	// }
	
	public void setRegion(String region) {
		REGION = region;
	}
	
	public void setIndustry(String industry) {
		INDUSTRY = industry;
	}

	public Model executeAlgorithm(User user, Model model)
			throws BlackbookSystemException {
		
		logger.error("Starting BOXER Filter with filter "+REGION);
	
		/* My vain attempt at ParameterizedModel2Model */
		
//		if (object instanceof BOXERParameters) {
//			return getFilteredModelByRegion(model,REGION);
//		}
//		else {
//			return ModelFactory.createDefaultModel();
//		}

		return getFilteredModelByRegion(model,REGION);

	}

	/* July 06, 2009: Useless now */
	
//	/** getModel()
//	 *  Simple accessor function.
//	 * @return	- 	the model associated with this BOXERFilter.
//	 */
//	
//	public Model getModel() {
//		return model;
//	}
	
	/** listURIResources()
	 * @return	-	All resources in the model that are URIResources.
	 */
	
	public static ExtendedIterator listURIResources(Model model) {
		BOXERURIFilter f = new BOXERURIFilter();
		ResIterator resources = model.listSubjects();
		ExtendedIterator result = resources.filterKeep(f);
		return result;
	}
	
	/** contains
	 *  Goes through the list of statements to see if there is a match.
	 * @param statements	-	List of statements.
	 * @param target		-	Target statement.
	 * @return				-	True if target is in statements.
	 * 							False otherwise.
	 */
	
	private boolean contains (StmtIterator statements, Statement target) {
		List<Statement> l = statements.toList();

		for (Statement cur_statement : l) {
			if (target.equals(cur_statement)) {
				return true;
			}
		}
		
		return false;
	}
	
	/** BFS
	 *  Helper method for Breadth-First Search that keeps track of the current
	 *  nodes to look at, all statements obtained so far, and all marked nodes.
	 * @param current		-	The nodes to look at in this step.
	 * @param statements	-	All statements gathered so far.
	 * @param marked		-	The nodes already looked at.
	 * @return				-	One step of BFS (called recursively). 
	 * 							The new nodes to look at are the ones we found
	 * 							as objects of statements that weren't marked, and
	 * 							we add the new statements to the ArrayList.
	 * 							We update the marked nodes by adding the current ones
	 * 							after they are analyzed.
	 */
	
	private static ArrayList<Statement> BFS (Model model, ArrayList<Resource> current, ArrayList<Statement> statements, ArrayList<Resource> marked) {
			int counter = 0;
			int len = current.size();
			
			ArrayList<Resource> next = new ArrayList<Resource>();
			
			Statement current_statement;
		
			/* We've reached the end of our breadth-first search. */
			if (len == 0) {
				return statements;
			}
			
			/* Debugging */
			logger.debug("ITERATION "+Integer.toString(counter));
			logger.debug("Nodes we are visiting this round:");
			for (Resource cur : current) {
				logger.debug("\t"+cur.toString());
				counter++;
			}
				
			for (Resource cur : current) {
				StmtIterator statements_from_cur = model.listStatements(cur, (Property) null, (RDFNode) null);
				while (statements_from_cur.hasNext()) {
					current_statement = (Statement)statements_from_cur.next();
					/* It will always be a new statement */
					statements.add(current_statement);
					
					/* Now we check the object to see if it's a new node */
					RDFNode object = current_statement.getObject();
					if (!object.isLiteral() && !marked.contains(object)) {
						next.add((Resource) object);
					}
				}
			}
			
			marked.addAll(current);
			
			return BFS(model,next,statements,marked);			
		
	}
	
	/** BFS
	 *  Breadth-First search starting at a resource. For most of our cases, our
	 *  Jena graph will be a collection of disjoint rooted directed trees, and
	 *  we can easily recognize the roots.
	 * @param start	-	The starting node for the BFS.
	 * @return		-	All statements that emanate from this node, using BFS.
	 */
	
	public static ArrayList<Statement> BFS(Model model, Resource start) {
		ArrayList<Resource> current = new ArrayList<Resource>();
		ArrayList<Statement> statements = new ArrayList<Statement>();
		ArrayList<Resource> marked = new ArrayList<Resource>();
		
		current.add(start);
		
		return BFS(model,current,statements,marked);
	}
	
	/** BFS
	 * 	Probably an old version that isn't used.
	 * @param resources
	 * @param old_statements
	 * @return
	 */
	
	public StmtIterator BFS (Model model, ResIterator resources, StmtIterator old_statements) {
		Resource cur_resource;
		Statement cur_statement;
		
		/* Must do the first one first, separately */
		
		if (!resources.hasNext()) {
			return old_statements;
		}
		
		StmtIterator all_statements;
		
		cur_resource = (Resource)resources.next();
		all_statements = model.listStatements(cur_resource,(Property) null, (RDFNode) null);
		while (all_statements.hasNext()) {
			cur_statement = (Statement)all_statements.next();
			if (!contains(old_statements,cur_statement)) {
				all_statements.remove();
			}
		}
		
		/* Now we go through the rest of the resources, selectively removing what's
		 *  already seen.
		 */
		
		while (resources.hasNext()) {
			cur_resource = (Resource)resources.next();
			StmtIterator cur_statements = model.listStatements(cur_resource,(Property) null, (RDFNode) null);
			while (cur_statements.hasNext()) {
				cur_statement = (Statement)cur_statements.next();
				if (!contains(old_statements,cur_statement)) {
					cur_statements.remove();
				}
			}
			all_statements.andThen(cur_statements);
		}
		
		return all_statements;
	}
	
	public static String genericFinder (Model model, Resource res, String RDF) {
		
		ArrayList<Statement> statements = BFS(model, res);
		Property current_predicate;
		
		for (Statement st : statements) {
			current_predicate = st.getPredicate();

			if (current_predicate.toString().equals(RDF)) {
				return st.getObject().toString();
			}
		}
		
		return "";
	}
	
	/** findRegion
	 *  Uses BFS to find the Region node, and returns it.
	 * @param res	-	Starting resource for the BFS.
	 * @return		-	The region associated with this resource, if there is one.
	 * 					Otherwise, returns the empty string.
	 */
	
	public static String findRegion (Model model, Resource res) {
		return genericFinder(model, res,REGION_RDF);
	}
	
	/** replaceRegion
	 *  Uses BFS to find the Region node, and returns it.
	 * @param res	-	Starting resource for the BFS.
	 * @return		-	The region associated with this resource, if there is one.
	 * 					Otherwise, returns the empty string.
	 */
	
	public static void replaceRegion (Model model, Resource res, String new_region) {
		ArrayList<Statement> statements = BFS(model, res);
		Property current_predicate;
		
		for (Statement st : statements) {
			current_predicate = st.getPredicate();

			if (current_predicate.toString().equals(REGION_RDF) && new_region != null) {
				st.changeObject(new_region);
			}
		}
	}
	
	/** findFeatures
	 *  Uses BFS to find the features node, and returns it.
	 * @param res	-	Starting resource for the BFS.
	 * @return		-	The features associated with this resource, if there is one.
	 * 					Otherwise, returns the empty string.
	 */
	
	public static String findFeatures (Model model, Resource res) {
		return genericFinder(model, res,FEATURES_RDF);
	}
	
	public static String findIdentifier (Model model, Resource res) {
		return genericFinder(model,res,IDENTIFIER_RDF);
	}
	
	public static String findIndustry (Model model, Resource res) {
		return genericFinder(model,res,INDUSTRY_RDF);
	}
	
	/** getResourcesWithRegion
	 *  Simple search to find all resources with a given region.
	 * @param region	-	Desired region	
	 * @return			-	An ArrayList of resources that have this region.
	 */
	
	public ArrayList<Resource> getResourcesWithRegion(Model model, String region) {
		ExtendedIterator URIResources = listURIResources(model);
		ArrayList<Resource> end_result = new ArrayList<Resource>();
		
		Resource cur;
		
		while (URIResources.hasNext()) {
			cur = (Resource) URIResources.next();
			if (findRegion(model, cur).equals(region)) {
				end_result.add(cur);
			}
		}
		
		return end_result;
	}
	
	/** getFilteredModelByRegion
	 *  The pinnacle method - takes in a region and returns the filtered model
	 *  by region.
	 * @param region	-	Region desired.
	 * @return			-	Filtered Jena model, only containing the data
	 * 						that has the appropriate region.
	 */
	
	public Model getFilteredModelByRegion(Model model, String region) {
		ArrayList<Statement> cur_statements;
		
		Model result = ModelFactory.createDefaultModel();
		
		ArrayList<Resource> resources = getResourcesWithRegion(model, region);
		
		for (Resource r : resources) {
			cur_statements = BFS(model, r);
			for (Statement s : cur_statements) {
				result.add(s);
			}
		}
		
		return result;
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
	
//	public StmtIterator BFS (Resource start) {
//		return model.listStatements(start,(Property) null, (RDFNode) null);
//	}

}

/** BOXERURIFilter
 *  Helper class for filtering, which is needed to apply some filtering methods
 *  already included in Jena. It definitely seems not worth the trouble, though!
 */

class BOXERURIFilter extends Filter {

	@Override
	public boolean accept(Object r) {
		return ((Resource) r).isURIResource();
	}
	
}
