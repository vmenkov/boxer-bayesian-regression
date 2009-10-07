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

/**
 * Simple workflow algorithm that will retrieve ALL contents of a specified model.
 * @author praff
 *
 */
public class BOXERGetWholeModel extends AbstractAlgorithmKeyword2Model {
	
	private static final String DESCRIPTION = "Gets a whole model from its name";

    private static final String LABEL = "BOXER Get Whole Model";


	/**
	 * 
	 */
	private static final long serialVersionUID = 3945624489360025034L;

	/**
	 * For testing purposes only
	 * @param args	The arguments.
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

	/**
	 * Takes in a datasource and retrieves everything. May be slow or 
	 * even impossible with mySQL restrictions if the model is too big.
	 * @param	user		The user
	 * @param	dataSource	The datasource requested
	 * @param	keyword		A keyword (ignored)	
	 */
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