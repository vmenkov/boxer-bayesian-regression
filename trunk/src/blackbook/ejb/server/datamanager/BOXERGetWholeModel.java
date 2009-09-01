package blackbook.ejb.server.datamanager;

/**
 * This will not work well because the model sizes will, for the most part,
 * be ginormous.
 */

import security.ejb.client.User;
import blackbook.ejb.client.exception.BlackbookSystemException;
import blackbook.ejb.server.jena.JenaModelFactory;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;

public class BOXERGetWholeModel extends AbstractAlgorithmKeyword2Model {
	
	private static final String DESCRIPTION = "Gets a whole model from its name";

    private static final String LABEL = "BOXER Get Whole Model";


	/**
	 * 
	 */
	private static final long serialVersionUID = 3945624489360025034L;

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

	public Model executeAlgorithm(User user, String dataSource, String keyword)
			throws BlackbookSystemException {
		
		Model finalModel = ModelFactory.createDefaultModel();
		
		finalModel.add(JenaModelFactory
                .openModelByName(dataSource, user));
		// TODO Auto-generated method stub
		return finalModel;
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
