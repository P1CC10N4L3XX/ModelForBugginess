package client;

import models.ClassifierMetrics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import utils.ClassifierMetricsWriter;

import utils.interfaces.ResultWriter;
import weka.attributeSelection.BestFirst;
import weka.attributeSelection.CfsSubsetEval;
import weka.classifiers.Classifier;
import weka.classifiers.Evaluation;
import weka.classifiers.bayes.NaiveBayes;
import weka.classifiers.lazy.IBk;
import weka.classifiers.meta.FilteredClassifier;


import weka.classifiers.trees.RandomForest;
import weka.core.Instances;
import weka.filters.Filter;
import weka.filters.supervised.attribute.AttributeSelection;
import weka.filters.supervised.instance.Resample;
import weka.filters.supervised.instance.SpreadSubsample;


import java.util.Random;

public class WekaManager {
    private final Instances data;
    private final Logger logger = LoggerFactory.getLogger(WekaManager.class);

    private static final String[] CLASSIFIER_NAMES = {
            "RandomForest",
            "NaiveBayes",
            "IBk"
    };

    private static final String[] BALANCING_NAMES = {
            "None",
            "Oversampling",
            "Undersampling"
    };

    public void evaluate() throws Exception {
        String filePath = "classifier_metrics.csv";
        ResultWriter<ClassifierMetrics> writer = new ClassifierMetricsWriter(filePath);
        writer.writeHeader();

        for (String classifierName : CLASSIFIER_NAMES){
            for (String balancingName : BALANCING_NAMES){
                logger.info("Evaluating: {} with balancing: {}",classifierName, balancingName);

                Classifier classifier = buildClassifier(classifierName, balancingName);
                ClassifierMetrics classifierMetrics = runTenTimesTenFold(classifier);
                classifierMetrics.setClassifier(classifierName);
                classifierMetrics.setBalancing(balancingName);
                writer.writeResult(classifierMetrics);
            }
        }
        writer.close();
        logger.info("Results saved to Milestone2_Results.csv");
    }

    private Classifier buildClassifier(String classifierName, String balancingName){
        Classifier base = getBaseClassifier(classifierName);
        AttributeSelection featureSelection = new AttributeSelection();
        CfsSubsetEval eval = new CfsSubsetEval();
        BestFirst search = new BestFirst();
        featureSelection.setEvaluator(eval);
        featureSelection.setSearch(search);

        FilteredClassifier fcWithFS = new FilteredClassifier();
        fcWithFS.setFilter(featureSelection);
        fcWithFS.setClassifier(base);

        if (balancingName.equals("None")){
            return fcWithFS;
        }

        Filter balancingFilter = getBalancingFilter(balancingName);
        FilteredClassifier fcWithBalancing = new FilteredClassifier();
        fcWithBalancing.setFilter(balancingFilter);
        fcWithBalancing.setClassifier(fcWithFS);

        return fcWithBalancing;
    }

    private Classifier getBaseClassifier(String name){
        return switch (name){
            case "RandomForest" -> new RandomForest();
            case "NaiveBayes" -> new NaiveBayes();
            case "IBk" -> new IBk();
            default -> throw new IllegalArgumentException("Unknown classifier:" + name);
        };
    }

    private Filter getBalancingFilter(String name){
        return switch (name){
            case "Oversampling" -> {
                Resample resample = new Resample();
                resample.setNoReplacement(false);
                resample.setBiasToUniformClass(1.0);
                yield resample;
            }
            case "Undersampling" -> {
                SpreadSubsample undersample = new SpreadSubsample();
                undersample.setDistributionSpread(1.0);
                yield undersample;
            }
            default -> throw new IllegalArgumentException("Unknown balancing: " + name);
        };
    }

    private ClassifierMetrics runTenTimesTenFold(Classifier classifier) throws Exception {
        int numRuns = 10;
        int numFolds = 10;

        double totalPrecision = 0;
        double totalRecall = 0;
        double totalAUC = 0;
        double totalKappa = 0;
        ClassifierMetrics classifierMetrics = new ClassifierMetrics();

        for (int run = 0; run < numRuns; run++){
            Instances shuffled = new Instances(data);
            shuffled.randomize(new Random(run));
            shuffled.stratify(numFolds);

            Evaluation evaluation = new Evaluation(shuffled);
            evaluation.crossValidateModel(
                    classifier,
                    shuffled,
                    numFolds,
                    new Random(run)
            );
            int classIndex = data.classAttribute().indexOfValue("yes");

            totalPrecision += evaluation.precision(classIndex);
            totalRecall += evaluation.recall(classIndex);
            totalAUC += evaluation.areaUnderROC(classIndex);
            totalKappa += evaluation.kappa();
        }

        classifierMetrics.setPrecision(totalPrecision / numRuns);
        classifierMetrics.setRecall(totalRecall / numRuns);
        classifierMetrics.setAuc(totalAUC / numRuns);
        classifierMetrics.setKappa(totalKappa / numRuns);

        return classifierMetrics;
    }



    public WekaManager(Instances data){
        this.data = data;
    }
}
