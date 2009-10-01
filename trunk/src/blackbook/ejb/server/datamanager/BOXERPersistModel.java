package blackbook.ejb.server.datamanager;

import security.ejb.client.User;
import blackbook.ejb.client.exception.BlackbookSystemException;
import blackbook.ejb.server.metadata.MetadataManager;

import com.hp.hpl.jena.rdf.model.Literal;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;

import edu.dimacs.mms.tokenizer.RDFNames;

/*
 * BOXERPersistModel does a simple task - it takes in two models and persists
 * the second one. The first model contains information about the name of the 
 * model that we want to persist, which can be stored using BOXERCreateModelName.
 */

public class BOXERPersistModel extends AbstractAlgorithmMultiModel2Model {

	/**
	 * 
	 */
	private static final long serialVersionUID = 2210606390350839244L;

	/* Model 1 will contain the name, and Model 2 will contain the model */
	public Model executeAlgorithm(User user, Model model1, Model model2)
			throws BlackbookSystemException {
		
		String DSname = "";
		try {
			DSname = extractNameFromModel(model1);
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
	
	private String extractNameFromModel(Model m) throws Exception {
		String resource_name = RDFNames.URI_PREFIX + RDFNames.URI_MODELNAME;
		StmtIterator statements = m.listStatements(m.getResource(resource_name),
									  			   m.createProperty(RDFNames.PROP_PREFIX,RDFNames.PROP_IS),
									  			   (RDFNode) null);
		if (statements.hasNext()) {
			RDFNode o = ((Statement) statements.next()).getObject();
			if (o.isLiteral())
				return ((Literal) o).getString();
			else 
				throw new Exception("Object of statement must be a literal.");
		}

		throw new Exception("Model has no statements giving names");
	}

	

}
