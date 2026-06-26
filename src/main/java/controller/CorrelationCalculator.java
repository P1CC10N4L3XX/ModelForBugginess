package controller;

import weka.core.Instances;

import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

public class CorrelationCalculator {
    private final Instances datasetA;
    private final int nSmellsIndex;
    private final int buggyIndex;

    private static final String[] EXCLUDED_ATTRIBUTES = {"release", "className", "buggy"};

    public CorrelationCalculator(Instances datasetA) {
        this.datasetA = datasetA;
        this.nSmellsIndex = datasetA.attribute("smells").index();
        this.buggyIndex = datasetA.attribute("buggy").index();
    }

    public Map<String, Double> computeMeans(Instances dataset){
        Map<String, Double> means = new HashMap<>();
        for (int j=0; j<dataset.numAttributes(); j++){
            if (isExcluded(dataset.attribute(j).name()) || !dataset.attribute(j).isNumeric()) continue;
            double sum = 0;
            int count = 0;
            for (int i=0; i<dataset.numInstances(); i++){
                if (!dataset.instance(i).isMissing(j)){
                    sum += dataset.instance(i).value(j);
                    count++;
                }
            }
            means.put(dataset.attribute(j).name(), count > 0 ? sum/count : 0.0);
        }

        return means;
    }

    public Map<String, Double> computeCorrelationWithNSmells(){
        return computeCorrelationsWithTarget(datasetA, nSmellsIndex);
    }

    public Map<String, Double> computeCorrelationWithDefectiveness(){
        double[] buggyValues = new double[datasetA.numInstances()];
        for (int i=0; i<datasetA.numInstances(); i++){
            String val = datasetA.instance(i).stringValue(buggyIndex);
            buggyValues[i] = val.equals("yes") ? 1.0 : 0.0;
        }
        return computeCorrelationsWithTargetArray(datasetA, buggyValues, false);
    }

    private Map<String, Double> computeCorrelationsWithTarget(Instances dataset, int targetIndex){
        double[] targetValues = extractColumn(dataset, targetIndex);
        return computeCorrelationsWithTargetArray(dataset, targetValues, true);
    }

    private Map<String, Double> computeCorrelationsWithTargetArray(Instances dataset, double[] targetValues, boolean skipNSmells){
        Map<String, Double> correlations = new HashMap<>();
        double[] rankedTarget = rank(targetValues);

        for (int j=0; j<dataset.numAttributes(); j++){
            if (isExcluded(dataset.attribute(j).name()) || !dataset.attribute(j).isNumeric() || (skipNSmells && j==nSmellsIndex)) continue;
            double[] featureValues = extractColumn(dataset, j);
            double[] rankedFeature = rank(featureValues);
            double rho = pearson(rankedFeature, rankedTarget);
            correlations.put(dataset.attribute(j).name(), rho);
        }

        return correlations;
    }

    private double pearson(double[] x, double[] y) {
        int n = x.length;
        double meanX = 0, meanY = 0;
        for (int i=0; i<n; i++){
            meanX += x[i];
            meanY += y[i];
        }
        meanX /= n;
        meanY /= n;

        double num=0, denX = 0, denY = 0;
        for (int i=0; i<n; i++){
            double dx = x[i] - meanX;
            double dy = y[i] - meanY;
            num += dx * dy;
            denX += dx * dx;
            denY += dy * dy;
        }
        if (denX == 0 || denY == 0) return 0.0;
        return num / Math.sqrt(denX * denY);
    }

    private double[] extractColumn(Instances dataset, int colIndex){
        double[] values = new double[dataset.numInstances()];
        for (int i = 0; i<dataset.numInstances(); i++){
            values[i] = dataset.instance(i).value(colIndex);
        }
        return values;
    }

    private double[] rank(double[] values){
        int n = values.length;
        Integer[] indices = new Integer[n];
        for (int i=0; i<n; i++) indices[i] = i;
        Arrays.sort(indices, Comparator.comparingDouble(a -> values[a]));

        double[] ranks = new double[n];
        int i=0;
        while (i < n){
            int j=i;
            while (j < n && values[indices[j]] == values[indices[i]]) j++;
            double avgRank = (i + j - 1) / 2.0 +1;
            for (int k=i; k<j; k++) ranks[indices[k]] = avgRank;
            i = j;
        }
        return ranks;
    }

    private boolean isExcluded(String name){
        for (String ex : EXCLUDED_ATTRIBUTES){
            if (ex.equalsIgnoreCase(name)) return true;
        }
        return false;
    }
}
