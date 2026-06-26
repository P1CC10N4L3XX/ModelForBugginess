package utils;

import java.io.IOException;

import models.ClassifierMetrics;
import utils.interfaces.ResultWriter;

public class ClassifierMetricsWriter extends ResultWriter<ClassifierMetrics> {

    public ClassifierMetricsWriter(String filePath) throws IOException {
        super(filePath);
    }

    @Override
    public void writeHeader() {
        writer.println("Classifier,Balancing,Precision,Recall,AUC,Kappa");
    }

    @Override
    public void writeResult(ClassifierMetrics classifierMetrics) {
        writer.print(classifierMetrics.getClassifier() + ",");
        writer.print(classifierMetrics.getBalancing() + ",");
        writer.print(classifierMetrics.getPrecision() + ",");
        writer.print(classifierMetrics.getRecall() + ",");
        writer.print(classifierMetrics.getAuc() + ",");
        writer.print(classifierMetrics.getKappa() + "\n");
    }
}
