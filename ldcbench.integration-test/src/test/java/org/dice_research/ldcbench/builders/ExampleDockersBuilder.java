package org.dice_research.ldcbench.builders;

import org.hobbit.core.run.ComponentStarter;
import org.hobbit.sdk.docker.builders.DynamicDockerFileBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.dice_research.ldcbench.Constants.*;

/**
 * @author Pavel Smirnov
 */

public class ExampleDockersBuilder extends DynamicDockerFileBuilder {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExampleDockersBuilder.class);

    @SuppressWarnings("rawtypes")
    public ExampleDockersBuilder(Class runnerClass, String imageName) throws Exception {
        super("ExampleDockersBuilder");
        imageName(imageName);
        // name for searching in logs
        containerName(runnerClass.getSimpleName());
        // temp docker file will be created there
        buildDirectory(SDK_BUILD_DIR_PATH);
        // should be packaged will all dependencies (via 'mvn package -DskipTests=true'
        // command)
        String jarPath = System.getProperty("sdkJarFilePath");
        if (jarPath == null) {
            jarPath = "target/ldcbench.integration-test-shaded.jar";
            LOGGER.info("Couldn't get SDK jar path from properties. I will assume \"{}\".", jarPath);
        }
        jarFilePath(jarPath);
        // will be placed in temp dockerFile
        dockerWorkDir(SDK_WORK_DIR_PATH);
        // will be placed in temp dockerFile
        runnerClass(ComponentStarter.class, runnerClass);
    }

}
