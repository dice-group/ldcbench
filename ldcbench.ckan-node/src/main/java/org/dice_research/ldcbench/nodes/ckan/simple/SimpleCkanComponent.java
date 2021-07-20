package org.dice_research.ldcbench.nodes.ckan.simple;

import static org.hobbit.core.Constants.CONTAINER_TYPE_BENCHMARK;

import java.io.IOException;
import java.io.InputStream;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.stream.Stream;

import org.dice_research.ldcbench.graph.Graph;
import org.dice_research.ldcbench.nodes.ckan.Constants;
import org.dice_research.ldcbench.nodes.ckan.dao.CkanDAO;
import org.dice_research.ldcbench.nodes.components.NodeComponent;
import org.dice_research.ldcbench.rdf.SimpleTripleCreator;
import org.dice_research.ldcbench.utils.CloseableHelper;
import org.hobbit.core.components.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.trentorise.opendata.jackan.CheckedCkanClient;
import eu.trentorise.opendata.jackan.exceptions.CkanException;
import eu.trentorise.opendata.jackan.model.CkanDataset;
import eu.trentorise.opendata.jackan.model.CkanDatasetBase;
import eu.trentorise.opendata.jackan.model.CkanResource;

/**
 *
 * Ckan Node
 *
 * @author Geraldo de Souza Junior
 *
 */

public class SimpleCkanComponent extends NodeComponent implements Component {

    private static final Logger LOGGER = LoggerFactory.getLogger(SimpleCkanComponent.class);

    private static final String POSTGRES_USER = "ckan";
    private static final String POSTGRES_PASSWORD = "ckan";
    private static final String POSTGRES_DB = "ckan";

    protected String postGresContainer = null;
    protected String solrContainer = null;
    protected String redisContainer = null;
    protected String ckanContainer = null;

    private CkanDAO ckanDao;
    private List<CkanDataset> ckanDataSets = new ArrayList<CkanDataset>();

    public static void main(String[] args) {

//      new PostgresCkanDAO("localhost").insertData();

        CkanDAO ckanDao = new CkanDAO(new CheckedCkanClient("http://localhost:80", Constants.CKAN_CLIENT_TOKEN));

        CkanDatasetBase ds = new CkanDatasetBase();
        ds.setName("dataset-test");
        ds.setTitle("dataset-test");
        ds.setOwnerOrg(Constants.ORGANIZATION);

//      ckanDao.insertDataSource(ds);
        ckanDao.deleteDataSource("dataset-test");

    }

    private void waitForSqlConnection(String url) throws InterruptedException {
        LOGGER.info("Waiting for {} to be ready...", url);
        Properties props = new Properties();
        props.put("user", POSTGRES_USER);
        props.put("password", POSTGRES_PASSWORD);
        while (true) {
            try {
                DriverManager.getConnection(url, props);
                break;
            } catch (SQLException e) {
                LOGGER.info("Cannot connect to the database {}, will try again...\nError: {}", url, e.getMessage());
                Thread.sleep(5000);
            }
        }
        LOGGER.info("Database {} is ready.", url);
    }

    @Override
    public void initBeforeDataGeneration() throws Exception {
        postGresContainer = createContainer(Constants.POSTGRES, CONTAINER_TYPE_BENCHMARK,
                new String[] { "POSTGRES_USER=ckan" + POSTGRES_USER, "POSTGRES_PASSWORD=" + POSTGRES_PASSWORD,
                        "POSTGRES_DB=" + POSTGRES_DB, "PGDATA=/var/postgresql/data",
                        "HOBBIT_SDK_PUBLISH_PORTS=5432", });
        waitForSqlConnection(
                "jdbc:postgresql://" + (dockerized ? postGresContainer : "localhost") + ":5432/" + POSTGRES_DB);
        String sqlUrl = "postgresql://" + POSTGRES_USER + ":" + POSTGRES_PASSWORD + "@" + postGresContainer + ":5432/"
                + POSTGRES_DB;

        solrContainer = createContainer(Constants.SOLR, CONTAINER_TYPE_BENCHMARK, null);
        redisContainer = createContainer(Constants.REDIS, CONTAINER_TYPE_BENCHMARK, null);

        String recaptchaPublicKey = "";
        String recaptchaPrivateKey = "";
        try (InputStream ps = getClass().getClassLoader().getResourceAsStream("recaptcha.properties")) {
            Properties p = new Properties();
            p.load(ps);
            recaptchaPublicKey = p.getProperty("publicKey", "");
            recaptchaPrivateKey = p.getProperty("privateKey", "");
        } catch (NullPointerException e) {
            LOGGER.warn("Cannot load recaptcha.properties.");
        }

        LOGGER.debug("Starting CKAN service: {}...", Constants.CKAN);
        ckanContainer = createContainer(Constants.CKAN, CONTAINER_TYPE_BENCHMARK, new String[] {
                "CKAN_SOLR_URL=http://" + solrContainer + ":8983/solr/ckan", "CKAN_SQLALCHEMY_URL=" + sqlUrl,
                "CKAN_REDIS_URL=redis://" + redisContainer + ":6379/0", "CKAN_SITE_URL=http://localhost",
                "CKAN_SITE_TITLE=CKAN NODE", "CKAN_SITE_DESCRIPTION=LDCBench Benchmark node",
                "CKAN_RECAPTCHA_PUBLICKEY=" + recaptchaPublicKey, "CKAN_RECAPTCHA_PRIVATEKEY=" + recaptchaPrivateKey,
                "REDIS_HOSTNAME=" + redisContainer, "HOBBIT_SDK_PUBLISH_PORTS=5000", });

        accessUriTemplate = "http://" + (dockerized ? ckanContainer : "localhost") + ":5000/";
        resourceUriTemplate = accessUriTemplate;
        CheckedCkanClient client = new CheckedCkanClient(accessUriTemplate, Constants.CKAN_CLIENT_TOKEN);
        ckanDao = new CkanDAO(client);
    }

    private void addDataSource(String uri) {
        LOGGER.info("Adding {} to CKAN...", uri.replaceAll("[^A-Za-z0-9]", "").toLowerCase());
        CkanDatasetBase dataset = new CkanDatasetBase();
        dataset.setTitle("Dataset " + uri);
        dataset.setName(uri.replaceAll("[^A-Za-z0-9]", "").toLowerCase());
        dataset.setOwnerOrg(Constants.ORGANIZATION);
        dataset.setAuthor(Constants.AUTHOR);

        List<CkanResource> listResources = new ArrayList<CkanResource>();
        CkanResource ckanRes = new CkanResource();
        ckanRes.setName("dist" + uri.replaceAll("[^A-Za-z0-9]", "").toLowerCase());
        LOGGER.info("DS Name: " + "dist" + uri.replaceAll("[^A-Za-z0-9]", "").toLowerCase());
        ckanRes.setUrl(uri);
        listResources.add(ckanRes);

        dataset.setResources(listResources);

        CkanDataset insertedDataset = ckanDao.insertDataSource(dataset);
        LOGGER.info("Inserted CKAN dataset ID: {}", insertedDataset.getId());
        ckanDataSets.add(insertedDataset);

    }

    private void addCloudNode(int node) throws Exception {
        SimpleTripleCreator tripleCreator = new SimpleTripleCreator(node,
                Stream.of(nodeMetadata).map(nm -> nm.getResourceUriTemplate()).toArray(String[]::new),
                Stream.of(nodeMetadata).map(nm -> nm.getAccessUriTemplate()).toArray(String[]::new));

        boolean success = false;
        while (!success) {
            try {
                addDataSource(tripleCreator.createNode(0, -1, -2, false).toString());
                success = true;
            } catch (CkanException ce) {
                if (ce.getMessage().contains("Solr returned an error"))
                    LOGGER.info("Solr is not ready yet. Trying again in 5 seconds");
                else
                    LOGGER.info("Could not add Data set. Trying again in 5 seconds");

                Thread.sleep(5000);

            }
        }
    }

    @Override
    public void initAfterDataGeneration() throws Exception {
        for (Graph graph : graphs) {
            int graphNodes = graph.getNumberOfNodes();
            for (int graphNode = 0; graphNode < graphNodes; graphNode++) {
                int targetCloudNode = graph.getGraphId(graphNode);
                if (targetCloudNode != Graph.INTERNAL_NODE_GRAPH_ID) {
                    addCloudNode(targetCloudNode);
                }

            }
        }
    }

    @Override
    public void close() throws IOException {

        if (postGresContainer != null) {
            LOGGER.debug("Stopping Postgres {}", postGresContainer);
            stopContainer(postGresContainer);
        } else {
            LOGGER.debug("There is no Postgres to stop.");
        }

        if (solrContainer != null) {
            LOGGER.debug("Stopping Solr {}", solrContainer);
            stopContainer(solrContainer);
        } else {
            LOGGER.debug("There is no Solr to stop.");
        }

        if (redisContainer != null) {
            LOGGER.debug("Stopping Redis {}", redisContainer);
            stopContainer(redisContainer);
        } else {
            LOGGER.debug("There is no Redis to stop.");
        }

        if (ckanContainer != null) {
            LOGGER.debug("Stopping Ckan {}", ckanContainer);
            stopContainer(ckanContainer);
        } else {
            LOGGER.debug("There is no Ckan to stop.");
        }

        // delete all the datasets

        for (CkanDataset dataset : ckanDataSets) {
            ckanDao.deleteDataSource(dataset.getName());
        }

        CloseableHelper.closeQuietly(receiver);
        if (bcBroadcastConsumer != null) {
            // bcBroadcastConsumer.close();
        }

        super.close();
    }

}
