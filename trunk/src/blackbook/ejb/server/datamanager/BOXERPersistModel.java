package blackbook.ejb.server.datamanager;

import security.ejb.client.User;
import blackbook.ejb.client.exception.BlackbookSystemException;
import blackbook.ejb.server.metadata.MetadataManager;

import com.hp.hpl.jena.rdf.model.Literal;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;

import edu.dimacs.mms.tokenizer.RDFNames;

/**
 * BOXERPersistModel does a simple task - it takes in two models and persists
 * the second one. The first model contains information about the name of the 
 * model that we want to persist, which can be stored using BOXERCreateModelName.
 * @author praff
 */

public class BOXERPersistModel extends AbstractAlgorithmMultiModel2Model {
	
	private static final String DESCRIPTION = "General method to persist models in Assertions Datasources.";

    private static final String LABEL = "BOXER Persist Model";
    
    /* For testing purposes only */
    public static void main (String[] args) {
    	BOXERPersistModel test_persist = new BOXERPersistModel();
    	BOXERCreateModelName test_create = new BOXERCreateModelName();
    	
    	Model m_name = null;
    	
    	try {
			m_name = test_create.executeAlgorithm(new User(), " " , "BAM");
		} catch (BlackbookSystemException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    	Model m = ModelFactory.createDefaultModel();
    	
    	try {
			test_persist.executeAlgorithm(new User(), m_name, m);
		} catch (BlackbookSystemException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    	
    	
    }

	/**
	 * 
	 */
	private static final long serialVersionUID = 2210606390350839244L;

	/** 
	 * Model 1 will contain the name, and Model 2 will contain the model. It then
	 * creates a datasource in blackbook with the given name containing the
	 * given information.
	 * @param	user	The user
	 * @param	m_name	a dumb model containing the name.
	 * @param	model2	The model containing the information to be stored.
	 * @return	This really only returns model2, but in the process it creates a datasource
	 * with the name specified in m_name and stores model2 in that datasource.
	 */
	public Model executeAlgorithm(User user, Model m_name, Model model2)
			throws BlackbookSystemException {
		
		String DSname = "";
		try {
			DSname = extractNameFromModel(m_name);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		/* Now we have the entire model in Document form. Perfect for XMLtoRDF! */
		MetadataManager meta = new MetadataManager();
		meta.createNewAssertionsDS(DSname);
		/* Don't just create it, PERSIST it! */
		JenaAndLuceneReplaceOrAdd j = new JenaAndLuceneReplaceOrAdd();
		j.executeAlgorithm(user,DSname,model2);
		return model2;
	}
	
	/**
	 * Method giving the appropriate syntax for a model containing the name, 
	 * and extracts the name.
	 * @param	m	The model containing the name.
	 * @return	The name specified in the model, if it can be found.
	 * @throws BOXERBlackbookException if the model is ill-formed.
	 */
	private String extractNameFromModel(Model m) throws BOXERBlackbookException {
		String resource_name = RDFNames.URI_PREFIX + RDFNames.URI_MODELNAME;
		StmtIterator statements = m.listStatements(m.getResource(resource_name),
									  			   m.createProperty(RDFNames.PROP_PREFIX,RDFNames.PROP_IS),
									  			   (RDFNode) null);
		if (statements.hasNext()) {
			RDFNode o = ((Statement) statements.next()).getObject();
			if (o.isLiteral())
				return ((Literal) o).getString();
			else 
				throw new BOXERBlackbookException("Object of statement must be a literal.");
		}

		throw new BOXERBlackbookException("Model has no statements giving names");
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
