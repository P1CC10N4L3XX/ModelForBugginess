package org.modelforbugginess;


import controller.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import client.WekaManager;
import utils.DatasetLoader;
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
                Instances data = DatasetLoader.loadCsv(METRICS_FILE);
                data.setClassIndex(data.numAttributes() - 1);
                LOGGER.info("Dataset loaded: {} instances",data.numInstances());
                LOGGER.info("Attributes: {}", data.numAttributes());
                LOGGER.info("Class attributes: {}", data.classAttribute().name());

                WekaManager wekaManager = new WekaManager(data);
                wekaManager.evaluate();
            }
            case "what_if_analysis" -> WhatIfController.run();
            default -> LOGGER.info("Invalid arg passed to main");
        }
    }

}
