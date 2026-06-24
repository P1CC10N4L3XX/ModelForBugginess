package org.modelforbugginess;


import controller.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import client.WekaManager;
import weka.core.Instances;
import weka.core.converters.CSVLoader;

import java.io.*;

public class Main {
    private static final String METRICS_FILE = "Syncope_classes_metrics.csv";
    private static final Logger LOGGER = LoggerFactory.getLogger(Main.class);

    private Main(){}

    public static void main(String[] args) throws Exception {

        switch (args[0]){
            case "dataset_creation" -> DatasetCreationController.run();
            case "model_evaluation" -> {
                Instances data = loadDataset(METRICS_FILE);
                data.setClassIndex(data.numAttributes() - 1);
                LOGGER.info("Dataset loaded: {} instances",data.numInstances());
                LOGGER.info("Attributes: {}", data.numAttributes());
                LOGGER.info("Class attributes: {}", data.classAttribute().name());

                WekaManager wekaManager = new WekaManager(data);
                wekaManager.evaluate();
            }
            default -> LOGGER.info("Invalid arg passed to main");
        }
    }

    private static Instances loadDataset(String path) throws IOException {
        CSVLoader loader = new CSVLoader();
        loader.setSource(new File(path));
        return loader.getDataSet();
    }

}
