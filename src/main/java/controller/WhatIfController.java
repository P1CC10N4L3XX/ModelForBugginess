package controller;

import client.WekaManager;
import models.MetricCorrelations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import utils.DatasetLoader;
import utils.WhatIfTableWriter;
import utils.interfaces.ResultWriter;
import weka.classifiers.Classifier;
import weka.core.Instances;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class WhatIfController {

    private static final Logger LOGGER = LoggerFactory.getLogger(WhatIfController.class);
    private static final String DATASET_PATH = "Syncope_classes_metrics.csv";
    private static final String WHAT_IF_TABLE_PATH = "what_if_table.csv";

    public static void run() throws Exception {
        LOGGER.info("Loading dataset A from: {}", DATASET_PATH);
        Instances datasetA = DatasetLoader.loadCsv(DATASET_PATH);
        datasetA.setClassIndex(datasetA.numAttributes() - 1);
        LOGGER.info("Dataset A: {} instances, {} features", datasetA.numInstances(), datasetA.numAttributes());

        LOGGER.info("Splitting dataset...");
        DatasetSplitter splitter = new DatasetSplitter(datasetA);

        Instances bPlus = splitter.getBPlus();
        Instances b = splitter.getB(bPlus);
        Instances c = splitter.getC();

        LOGGER.info("B+ (NSmells > 0): {} instances", bPlus.numInstances());
        LOGGER.info("B (NSmells = 0): {} instances", b.numInstances());
        LOGGER.info("C (NSmells = 0 original): {} instances",c.numInstances());

        LOGGER.info("Training classifier on A...");
        Classifier classifier = WekaManager.train(datasetA);
        LOGGER.info("Training completed");

        LOGGER.info("Predictions on A...");
        List<String> predictionsA = WekaManager.predict(classifier, datasetA);
        LOGGER.info("Predictions on B+...");
        List<String> predictionsBPlus = WekaManager.predict(classifier, bPlus);
        LOGGER.info("Predictions on B...");
        List<String> predictionsB = WekaManager.predict(classifier, b);
        LOGGER.info("Predictions on C...");
        List<String> predictionsC = WekaManager.predict(classifier, c);
        long buggyA = countBuggy(predictionsA);
        long buggyBPlus = countBuggy(predictionsBPlus);
        long buggyB = countBuggy(predictionsB);
        long buggyC = countBuggy(predictionsC);

        LOGGER.info("Buggy predicted - A:{} B+:{} B:{} C:{}",buggyA,buggyB,buggyBPlus,buggyC);


        LOGGER.info("Calculating means e correlations...");
        CorrelationCalculator correlationCalculator = new CorrelationCalculator(datasetA);

        Map<String, Double> meansA = correlationCalculator.computeMeans(datasetA);
        Map<String, Double> meansB = correlationCalculator.computeMeans(b);
        Map<String, Double> meansC = correlationCalculator.computeMeans(c);

        Map<String, Double> corrNSmells = correlationCalculator.computeCorrelationWithNSmells();
        Map<String, Double> corrDefectiveness = correlationCalculator.computeCorrelationWithDefectiveness();

        List<String> featureOrder = buildFeatureOrder(datasetA);

        LOGGER.info("Writing output on {}", WHAT_IF_TABLE_PATH);
        ResultWriter<MetricCorrelations> tableWriter = new WhatIfTableWriter(WHAT_IF_TABLE_PATH);
        tableWriter.writeHeader();

        for (String feature : featureOrder){
            MetricCorrelations metricCorrelations = new MetricCorrelations();
            metricCorrelations.setMetric(feature);
            metricCorrelations.setMeanA(meansA.getOrDefault(feature, 0.0));
            metricCorrelations.setMeanB(meansB.getOrDefault(feature, 0.0));
            metricCorrelations.setMeanC(meansC.getOrDefault(feature, 0.0));
            metricCorrelations.setCorrNSmells(corrNSmells.getOrDefault(feature, null));
            metricCorrelations.setCorrDefectiveness(corrDefectiveness.getOrDefault(feature, 0.0));
            tableWriter.writeResult(metricCorrelations);
        }

        tableWriter.close();

        long prevented = buggyBPlus - buggyB;
        LOGGER.info("--- What-If Analysis ---");
        LOGGER.info("Buggy in A: {}/{}", buggyA, datasetA.numInstances());
        LOGGER.info("Buggy in B+: {}/{}", buggyBPlus, bPlus.numInstances());
        LOGGER.info("Buggy in B: {}/{}", buggyB, b.numInstances());
        LOGGER.info("Buggy in C: {}/{}", buggyC, c.numInstances());

        LOGGER.info("Prevented classes (B+ - B): {}", prevented);
        LOGGER.info("Prevented / total buggy A: {}", buggyA > 0 ? String.format("%.1f%%", prevented * 100.0 / buggyA) : "N/A");

        LOGGER.info("WhatIf analysis completed.");



    }

    private static List<String> buildFeatureOrder(Instances dataset){
        List<String> features = new ArrayList<>();
        for (int j = 0; j < dataset.numAttributes(); j++){
            String name = dataset.attribute(j).name();
            if (name.equalsIgnoreCase("release") || name.equalsIgnoreCase("className") || name.equalsIgnoreCase("buggy")) continue;
            features.add(name);
        }
        return features;
    }

    private static long countBuggy(List<String> predictions){
        return predictions.stream().filter("yes"::equals).count();
    }
}
