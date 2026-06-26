package client;

import models.ClassifierMetrics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import utils.ClassifierMetricsWriter;

import utils.interfaces.ResultWriter;
import weka.attributeSelection.CfsSubsetEval;
import weka.attributeSelection.GreedyStepwise;
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
        logger.info("Applying feature selection once on full dataset...");
        Instances reducedData = applyFeatureSelection(data);
        logger.info("Feature selection done. Attributes reduced: {} -> {}", data.numAttributes(), reducedData.numAttributes());

        for (String classifierName : CLASSIFIER_NAMES){
            for (String balancingName : BALANCING_NAMES){
                logger.info("Evaluating: {} with balancing: {}",classifierName, balancingName);

                Classifier classifier = buildClassifier(classifierName, balancingName);
                ClassifierMetrics classifierMetrics = runTenTimesTenFold(classifier, reducedData);
                classifierMetrics.setClassifier(classifierName);
                classifierMetrics.setBalancing(balancingName);
                writer.writeResult(classifierMetrics);
            }
        }
        writer.close();
        logger.info("Results saved to {}", filePath);
    }

    private Instances applyFeatureSelection(Instances instances) throws Exception {
        AttributeSelection fs = new AttributeSelection();
        CfsSubsetEval eval = new CfsSubsetEval();
        GreedyStepwise search = new GreedyStepwise();
        search.setSearchBackwards(false);

        fs.setEvaluator(eval);
        fs.setSearch(search);
        fs.setInputFormat(instances);

        return Filter.useFilter(instances, fs);
    }

    public static Classifier buildClassifier(String classifierName, String balancingName){
        Classifier base = getBaseClassifier(classifierName);

        if (balancingName.equals("None")){
            return base;
        }

        Filter balancingFilter = getBalancingFilter(balancingName);
        FilteredClassifier fcWithBalancing = new FilteredClassifier();

        fcWithBalancing.setFilter(balancingFilter);
        fcWithBalancing.setClassifier(base);

        return fcWithBalancing;
    }

    private static Classifier getBaseClassifier(String name){
        return switch (name){
            case "RandomForest" -> {
                RandomForest randomForest = new RandomForest();
                randomForest.setNumIterations(20);
                randomForest.setMaxDepth(8);
                randomForest.setNumExecutionSlots(1);
                randomForest.setBagSizePercent(50);
                yield randomForest;
            }
            case "NaiveBayes" -> new NaiveBayes();
            case "IBk" -> new IBk();
            default -> throw new IllegalArgumentException("Unknown classifier:" + name);
        };
    }

    private static Filter getBalancingFilter(String name){
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

    private ClassifierMetrics runTenTimesTenFold(Classifier classifier, Instances instances) throws Exception {
        int numRuns = 10;
        int numFolds = 10;

        double totalPrecision = 0;
        double totalRecall = 0;
        double totalAUC = 0;
        double totalKappa = 0;

        int classIndex = instances.classAttribute().indexOfValue("yes");

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

            totalPrecision += evaluation.precision(classIndex);
            totalRecall += evaluation.recall(classIndex);
            totalAUC += evaluation.areaUnderROC(classIndex);
            totalKappa += evaluation.kappa();
        }

        ClassifierMetrics classifierMetrics = new ClassifierMetrics();

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
