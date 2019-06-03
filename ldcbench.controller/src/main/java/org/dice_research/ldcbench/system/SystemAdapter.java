package org.dice_research.ldcbench.system;

import org.apache.jena.rdf.model.NodeIterator;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.ResIterator;
import org.apache.jena.vocabulary.RDF;
import org.hobbit.core.components.AbstractSystemAdapter;
import org.hobbit.core.rabbit.RabbitMQUtils;
import org.hobbit.vocab.HOBBIT;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;

public class SystemAdapter extends AbstractSystemAdapter {
    private static final Logger logger = LoggerFactory.getLogger(SystemAdapter.class);
    private Map<String, String> parameters = new HashMap<>();

    @Override
    public void init() throws Exception {
        super.init();
        logger.debug("Init()");

        //Getting default values from system.ttl
        Property parameter;
        NodeIterator objIterator;
        ResIterator iterator = systemParamModel.listResourcesWithProperty(RDF.type, HOBBIT.Parameter);
        Property defaultValProperty = systemParamModel.getProperty("http://w3id.org/hobbit/vocab#defaultValue");
        while (iterator.hasNext()) {
            parameter = systemParamModel.getProperty(iterator.next().getURI());
            objIterator = systemParamModel.listObjectsOfProperty(parameter, defaultValProperty);
            while (objIterator.hasNext()) {
                String value = objIterator.next().asLiteral().getString();
                parameters.put(parameter.getLocalName(), value);
            }
        }
    }

    @Override
    public void receiveGeneratedData(byte[] data) {
    }

    @Override
    public void receiveGeneratedTask(String taskId, byte[] data) {
        String[] seedURIs = RabbitMQUtils.readString(data).split("\n");
        logger.info("Seed URIs: {}", Arrays.toString(seedURIs));
        terminate(null);
    }

    @Override
    public void close() throws IOException {
        // Free the resources you requested here
        logger.debug("close()");

        // Always close the super class after yours!
        super.close();
    }
}
