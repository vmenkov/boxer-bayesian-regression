package blackbook.ejb.server.datamanager;

import java.util.HashSet;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import security.ejb.client.User;
import blackbook.ejb.client.datamanager.AssertionsLevel;
import blackbook.ejb.client.datamanager.URIBean;
import blackbook.ejb.client.exception.BlackbookSystemException;
import blackbook.ejb.client.metadata.DataSourceMetadata;
import blackbook.ejb.server.jena.JenaModelCache;
import blackbook.ejb.server.jena.JenaModelFactory;
import blackbook.ejb.server.metadata.MetadataManagerFactory;
import blackbook.util.JenaUtils;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.ResIterator;
import com.hp.hpl.jena.rdf.model.Resource;

/**
 * In order to use JenaMaterialize the developer must first have a database that
 * contains Jena RDF data. The materialize algorithm takes a list of one or more
 * URIs and returns all of the node's attributes for the specified URIs. The
 * original URIs are usually returned from previously running JenaKeyword or
 * LuceneKeyword.
 * 
 * Default implementation of the Materialize interface. This implementation uses
 * the Jena API to read entities from an RDF model.
 */
public class JenaMaterializeAll extends AbstractAlgorithmURI2Model {
    /** description */
    private static final String DESCRIPTION = "Materialize against Jena";

    /** label */
    private static final String LABEL = "Materialize All";

    /** logger */
    private static Log logger = LogFactory.getLog(JenaMaterializeAll.class);

    /**
     * @see blackbook.ejb.client.datamanager.AlgorithmURI2Model#executeAlgorithm(security.ejb.client.User,
     *      java.lang.String, java.util.Set, java.util.Set)
     */
    public Model executeAlgorithm(User user, String assertionsLevel,
            Set<String> assertionsDataSourceNames, Set<URIBean> uris)
            throws BlackbookSystemException {
        if (user == null) {
            throw new BlackbookSystemException("'user' cannot be null.");
        }

        if (uris == null) {
            throw new BlackbookSystemException("'uris' cannot be null.");
        }

        try {
            long startTime = System.currentTimeMillis();

            AssertionsLevel al = AssertionsLevel.fromString(assertionsLevel);

            Model model = null;
            if (al != AssertionsLevel.ASSERTIONS_LEVEL_NONE) {
                model = materializeWithAssertions(user, uris, al,
                        assertionsDataSourceNames);
            } else {
                model = materializeWithoutAssertions(user, uris);
            }

            long endTime = System.currentTimeMillis();
            double seconds = (endTime - startTime) / 1000.0;
            double secondsPerURI = seconds / uris.size();
            logger.debug("MATERIALIZATION STATS: " + seconds
                    + " total seconds, " + uris.size() + " total URIs, "
                    + secondsPerURI + " seconds per URI, assertions level "
                    + assertionsLevel);

            return model;
        } catch (BlackbookSystemException e) {
            throw e;
        } catch (Exception e) {
            logger.error("Materialization failed.", e);
            throw new BlackbookSystemException("Materialization failed.", e);
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

    /**
     * Get set of data source meta-data from set of URIs
     * 
     * @param uris
     *            URI set
     * @param user
     *            user object
     * @return data sources
     * @throws BlackbookSystemException
     */
    private Set<DataSourceMetadata> getDataSourcesFromURIs(Set<URIBean> uris,
            User user) throws BlackbookSystemException {

        Set<DataSourceMetadata> dsMetaDataSet = new HashSet<DataSourceMetadata>();
        DataSourceMetadata metadata = null;

        for (URIBean uri : uris) {
            metadata = MetadataManagerFactory.getInstance()
                    .getDataSourceMetadataByURI(uri.getUri());
            dsMetaDataSet.add(metadata);
        }
        return dsMetaDataSet;
    }

    /**
     * Materializes including content from assertions.
     * 
     * @param user
     *            user information
     * @param uris
     *            URI set
     * @param al
     *            Assertion Level (advanced, basic, or none)
     * @param assertionsDataSourceNames
     *            set of assertion data source names
     * @return resulting model
     * @throws BlackbookSystemException
     */
    private Model materializeWithAssertions(User user, Set<URIBean> uris,
            AssertionsLevel al, Set<String> assertionsDataSourceNames)
            throws BlackbookSystemException {
        Model model = null;
        try {
            // Create the final model...
            Model finalModel = ModelFactory.createDefaultModel();

            for (String dsName : assertionsDataSourceNames) {
            	
            		finalModel.add(JenaModelFactory
                        .openModelByName(dsName, user));
            }

            return finalModel;
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
     * Materializes without the including content from assertions.
     * 
     * @param user
     *            user information
     * @param uris
     *            URI set to materialize
     * @return resulting model from the materialized URIs.
     * @throws BlackbookSystemException
     */
    private Model materializeWithoutAssertions(User user, Set<URIBean> uris)
            throws BlackbookSystemException {

        if (uris.size() == 0) {
            return ModelFactory.createDefaultModel();
        }

        if (user.getRoles().isEmpty()) {
            return ModelFactory.createDefaultModel();
        }

        JenaModelCache modelCache = new JenaModelCache();

        Model model = null;
        Model submodel = null;
        Model resultModel = ModelFactory.createDefaultModel();
        try {
            // iterate over all data sources.
            for (DataSourceMetadata dataSourceMetadata : getDataSourcesFromURIs(
                    uris, user)) {
                // get current model
                // model = JenaModelFactory.openModelByName(dataSourceMetadata
                // .getName(), user);

                model = modelCache.getModelByName(dataSourceMetadata.getName(),
                        user);

                // sub-model to build
                submodel = ModelFactory.createDefaultModel();

                for (URIBean uri : uris) {
                    Resource r = model.createResource(uri.toString());
                    if (!model.contains(r, null)) {
                        logger.warn("Resource with URI '" + uri
                                + "' has no associated statements.");

                        // nothing added to the model
                        continue;
                    }

                    JenaUtils.materializeResource(r, model, submodel, user,
                            dataSourceMetadata.getDefaultRoles(), null);
                }

                // Make sure we have content other than anonymous node rooted
                // statements...
                boolean hasContent = false;
                for (ResIterator it = submodel.listSubjects(); it.hasNext()
                        && !hasContent;) {
                    Resource r = it.nextResource();
                    if (r.isURIResource()) {
                        hasContent = true;
                    }
                }

                if (!hasContent) {
                    // Empty model, make sure it's really empty...
                    submodel = ModelFactory.createDefaultModel();
                }
                resultModel.add(submodel);
                submodel.close();
                // model.close();
            }

            return resultModel;
        } catch (BlackbookSystemException e) {
            throw e;
        } catch (Exception e) {
            logger.error("Materialization failed.", e);
            throw new BlackbookSystemException("Materialization failed.", e);
        } finally {
            if (model != null) {
                model.close();
            }
            modelCache.closeAll();
        }
    }
}
