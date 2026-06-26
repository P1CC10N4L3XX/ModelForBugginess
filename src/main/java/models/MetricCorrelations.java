package models;

public class MetricCorrelations {
    private String metric;
    private Double meanA;
    private Double meanB;
    private Double meanC;
    private Double corrNSmells;
    private Double corrDefectiveness;

    public String getMetric() {
        return metric;
    }

    public void setMetric(String metric) {
        this.metric = metric;
    }

    public Double getMeanA() {
        return meanA;
    }

    public void setMeanA(Double meanA) {
        this.meanA = meanA;
    }

    public Double getMeanB() {
        return meanB;
    }

    public void setMeanB(Double meanB) {
        this.meanB = meanB;
    }

    public Double getMeanC() {
        return meanC;
    }

    public void setMeanC(Double meanC) {
        this.meanC = meanC;
    }

    public Double getCorrNSmells() {
        return corrNSmells;
    }

    public void setCorrNSmells(Double corrNSmells) {
        this.corrNSmells = corrNSmells;
    }

    public Double getCorrDefectiveness() {
        return corrDefectiveness;
    }

    public void setCorrDefectiveness(Double corrDefectiveness) {
        this.corrDefectiveness = corrDefectiveness;
    }
}
