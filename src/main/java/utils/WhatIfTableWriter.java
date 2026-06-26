package utils;

import models.MetricCorrelations;
import utils.interfaces.ResultWriter;

import java.io.IOException;
import java.util.Locale;

public class WhatIfTableWriter extends ResultWriter<MetricCorrelations> {


    public WhatIfTableWriter(String fileName) throws IOException {
        super(fileName);
    }

    @Override
    public void writeHeader() {
        writer.println("Variable,Mean_A,Mean_B,Mean_C,Corr_NSmells,Corr_Defectiveness");
    }

    @Override
    public void writeResult(MetricCorrelations metricCorrelations) {
        String corrNSmells = metricCorrelations.getCorrNSmells() != null
                ? String.format(Locale.US,"%.2f", metricCorrelations.getCorrNSmells())
                : "-";

        writer.printf(Locale.US,
                "%s,%.2f,%.2f,%.2f,%s,%.2f%n",
                metricCorrelations.getMetric(),
                metricCorrelations.getMeanA(),
                metricCorrelations.getMeanB(),
                metricCorrelations.getMeanC(),
                corrNSmells,
                metricCorrelations.getCorrDefectiveness()
        );

    }
}
