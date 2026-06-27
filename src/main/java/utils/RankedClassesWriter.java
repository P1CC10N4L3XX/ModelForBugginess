package utils;

import models.ClassRecord;
import utils.interfaces.ResultWriter;

import java.io.IOException;

public class RankedClassesWriter extends ResultWriter<ClassRecord> {

    public RankedClassesWriter(String fileName) throws IOException {
        super(fileName);
    }

    @Override
    public void writeHeader() {
        writer.println("release,className,smells,CyclomaticComplexity,PublicMethods");
    }

    @Override
    public void writeResult(ClassRecord classRecord) {
        writer.print(classRecord.getRelease() + ",");
        writer.print(classRecord.getClassName() + ",");
        writer.print(classRecord.getSmells() + ",");
        writer.print(classRecord.getCyclomaticComplexity() + ",");
        writer.print(classRecord.getNumberPublicMethods() + "\n");
    }
}
