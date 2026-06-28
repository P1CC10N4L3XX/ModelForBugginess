package utils;

import models.ClassRecord;
import utils.interfaces.ResultWriter;

import java.io.IOException;

public class ClassMetricsWriter extends ResultWriter<ClassRecord> {

    public ClassMetricsWriter(String fileName) throws IOException {
        super(fileName);
    }

    @Override
    public void writeHeader() {
        writer.println("release,className,smells,smellsDensity,cyclomaticComplexity,nPublicMethods,loc,numberRevision,numberAuthors,locAuthors,maxOverRevisionLOCAdded,averageLOCAddedPerRevision,churn,maxChurn,averageChurn,changeSetSize,maxChangeSet,averageChangeSet,LocTouched,age,weightedAge,buggy");
    }

    @Override
    public void writeResult(ClassRecord classRecord) {
        writer.print(classRecord.getRelease() + ",");
        writer.print(classRecord.getClassName() + ",");
        writer.print(classRecord.getSmells() + ",");
        writer.print(classRecord.getSmellsDensity() + ",");
        writer.print(classRecord.getCyclomaticComplexity() + ",");
        writer.print(classRecord.getNumberPublicMethods() + ",");
        writer.print(classRecord.getLoc() + ",");
        writer.print(classRecord.getNumberRevision() + ",");
        writer.print(classRecord.getNumberAuthors() + ",");
        writer.print(classRecord.getLocAuthors() + ",");
        writer.print(classRecord.getMaxOverRevisionLOCAdded() + ",");
        writer.print(classRecord.getAverageLOCAddedPerRevision() + ",");
        writer.print(classRecord.getChurn() + ",");
        writer.print(classRecord.getMaxChurn() + ",");
        writer.print(classRecord.getAverageChurn() + ",");
        writer.print(classRecord.getChangeSetSize() + ",");
        writer.print(classRecord.getMaxChangeSet() + ",");
        writer.print(classRecord.getAverageChangeSet() + ",");
        writer.print(classRecord.getLocTouched() + ",");
        writer.print(classRecord.getAge() + ",");
        writer.print(classRecord.getWeightedAge() + ",");
        writer.println(classRecord.isBuggy() ? "yes" : "no");
    }
}
