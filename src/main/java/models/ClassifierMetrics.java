package models;

public class ClassifierMetrics {
    private String classifier;
    private String balancing;
    private double precision;
    private double recall;
    private double auc;
    private double kappa;

    public String getClassifier() {
        return classifier;
    }

    public void setClassifier(String classifier) {
        this.classifier = classifier;
    }

    public String getBalancing() {
        return balancing;
    }

    public void setBalancing(String balancing) {
        this.balancing = balancing;
    }

    public double getPrecision() {
        return precision;
    }

    public void setPrecision(double precision) {
        this.precision = precision;
    }

    public double getRecall() {
        return recall;
    }

    public void setRecall(double recall) {
        this.recall = recall;
    }

    public double getAuc() {
        return auc;
    }

    public void setAuc(double auc) {
        this.auc = auc;
    }

    public double getKappa() {
        return kappa;
    }

    public void setKappa(double kappa) {
        this.kappa = kappa;
    }
}
