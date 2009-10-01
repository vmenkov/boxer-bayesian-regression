package blackbook.ejb.server.datamanager;

import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import security.ejb.client.User;
import blackbook.ejb.client.datamanager.URIBean;
import blackbook.ejb.client.exception.BlackbookSystemException;
import blackbook.ejb.server.jena.JenaModelFactory;

import com.hp.hpl.jena.rdf.model.Literal;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.util.iterator.ExtendedIterator;

/** This will scan through the assertions datasources specified and look
 *  to see where the Analyst has given certification of region for the 
 *  documents. It will then remove those documents (since we are memoryless 
 *  and leaving them in is an inconsistency) and then spit out everything 
 *  as an RDF model containing the new data, which can then be fed into 
 *  BOXERDriverUpdate.
 *  
 *  NOTICE: This can only be used in the following setting:
 *  Void Keyword -> BOXERAnalystUpdate -> BOXERDriverUpdate
 * @author raff
 *
 */

public class BOXERAnalystUpdate extends AbstractAlgorithmURI2Model {
	
	private static final String DESCRIPTION = "Scans the assertions to see where the analyst has made statements, and returns a model asserting as such.";

    private static final String LABEL = "BOXER Analyst Update";
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 2473225868591524839L;

	/* Ontology */
    private static final String REGION_PROPERTY_ANALYST = "http://www.blackbook.com/analyst/assertion/region";
	
    /** logger */
    private static Log logger = LogFactory.getLog(JenaMaterializeAll.class);

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

	public Model executeAlgorithm(User user, String assertionsLevel,
			Set<String> assertionsDataSourceNames, Set<URIBean> uris)
			throws BlackbookSystemException {
		
		Model model = null;
        try {
            // Create the final model...
            model = ModelFactory.createDefaultModel();

            for (String dsName : assertionsDataSourceNames) {
            	
            		model.add(JenaModelFactory
                        .openModelByName(dsName, user));
            }
            
            ExtendedIterator URI_Resources = BOXERFilter.listURIResources(model);
            Model filtered_model = ModelFactory.createDefaultModel();
            
            while (URI_Resources.hasNext()) {
            	Statement s = getAnalystAssertion(model,(Resource) URI_Resources.next());
            	if (s != null) {
            		filtered_model.add(s);
            	}
            }

            return filtered_model;
        } catch (BlackbookSystemException e) {
            throw e;
        } catch (Exception e) {
            logger.error("Materialization failed.", e);
            throw new BlackbookSystemException("Materialization failed.", e);
        } finally {
            if (model != null) {
                model.close();
            }
        }
	}
	
	/**
	 * This looks in the resource in the model for the analyst assertion,
	 * and returns the whole statement if an assertion has been made.
	 * Otherwise, it returns null, which will indicate that this statement
	 * does not have an analyst assertion made yet. 
	 * @param m
	 * @param r
	 * @return
	 */
	
	private Statement getAnalystAssertion(Model m, Resource r) {
		Property p = m.createProperty(REGION_PROPERTY_ANALYST);
		
		StmtIterator statements = m.listStatements(r,p, (RDFNode) null);
		if (statements.hasNext()) {
			Statement s = (Statement) statements.next();
			Literal l = s.getLiteral();
			if (l.getString().isEmpty())
				return null;
			else
				return s; 
		}
		else {
			return null;
		}
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
