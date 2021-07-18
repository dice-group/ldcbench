package org.dice_research.ldcbench;

import org.dice_research.ldcbench.benchmark.BenchmarkController;

public class Constants {

    public static String GIT_USERNAME = "ldcbench";
    public static String GIT_REPO_PATH = "git.project-hobbit.eu:4567/"+GIT_USERNAME+"/";
    //public static String GIT_REPO_PATH = "";

    public static String PROJECT_NAME = "ldcbench";

    //use these constants within BenchmarkController
    public static final String BENCHMARK_IMAGE_NAME = GIT_REPO_PATH+PROJECT_NAME +"/ldcbench.controller";
    public static final String DATAGEN_IMAGE_NAME = GIT_REPO_PATH+PROJECT_NAME +"/ldcbench.data-generator";
    public static final String EVAL_STORAGE_IMAGE_NAME = BenchmarkController.DEFAULT_EVAL_STORAGE_IMAGE;
    public static final String EVALMODULE_IMAGE_NAME = GIT_REPO_PATH+PROJECT_NAME +"/ldcbench.eval-module";
    public static final String SYSTEM_IMAGE_NAME = GIT_REPO_PATH+PROJECT_NAME +"/ldcbench.system";
    public static final String HTTPNODE_IMAGE_NAME = GIT_REPO_PATH+PROJECT_NAME +"/ldcbench.http-node";
    public static final String CKANNODE_IMAGE_NAME = GIT_REPO_PATH+PROJECT_NAME +"/ldcbench.ckan-node";
    public static final String HENODE_IMAGE_NAME = GIT_REPO_PATH+PROJECT_NAME +"/ldcbench.html-embd-node";
    public static final String RDFADATAGEN_IMAGE_NAME = GIT_REPO_PATH+PROJECT_NAME +"/ldcbench.rdfa-gen";
    public static final String JSONLDDATAGEN_IMAGE_NAME = GIT_REPO_PATH+PROJECT_NAME +"/ldcbench.jsonld-gen";
    public static final String SPARQLNODE_IMAGE_NAME = GIT_REPO_PATH+PROJECT_NAME +"/ldcbench.sparql-node";
    public static final String EMPTY_SERVER_IMAGE_NAME = GIT_REPO_PATH+PROJECT_NAME +"/ldcbench.empty-server";

    public static final String BENCHMARK_URI = "http://project-hobbit.eu/"+PROJECT_NAME;
    public static final String SYSTEM_URI = "http://project-hobbit.eu/"+PROJECT_NAME+"/system";

    public static final String SDK_BUILD_DIR_PATH = ".";  //build directory, temp docker file will be created there
    public static final String SDK_WORK_DIR_PATH = "/usr/src/"+PROJECT_NAME;

    public static final String VOS_PASSWORD = "ldcbench";

}
