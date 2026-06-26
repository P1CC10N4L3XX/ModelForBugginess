package client;

import weka.classifiers.Classifier;
import weka.classifiers.meta.FilteredClassifier;
import weka.core.Instance;
import weka.core.Instances;
import weka.filters.supervised.instance.Resample;

import java.util.ArrayList;
import java.util.List;

public class WhatIfClassifier {
    private final String classifierName;
    private final String balancingName;
    private Classifier traindedClassifier;

    public WhatIfClassifier(String classifierName, String balancingName){
        this.classifierName = classifierName;
        this.balancingName = balancingName;
    }

    public void train(Instances dataset) throws Exception {
        dataset.setClassIndex(dataset.numAttributes() - 1);
        Classifier fc = WekaManager.buildClassifier(classifierName, balancingName);
        fc.buildClassifier(dataset);
        this.traindedClassifier = fc;
    }

    public List<String> predict(Instances dataset) throws Exception {
        List<String> predictions = new ArrayList<>();
        for (int i=0; i<dataset.numInstances(); i++){
            Instance instance = dataset.instance(i);
            double predicted = traindedClassifier.classifyInstance(instance);
            predictions.add(dataset.classAttribute().value((int) predicted));
        }
        return predictions;
    }
}
