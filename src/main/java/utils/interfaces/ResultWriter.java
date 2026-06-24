package utils.interfaces;

import models.ClassifierMetrics;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

public abstract class ResultWriter<T> {

    protected PrintWriter writer;

    public ResultWriter(String fileName) throws IOException {
        this.writer = new PrintWriter(new FileWriter(fileName));
    }

    public abstract void writeHeader();
    public abstract void writeResult(T result);
    public void close(){
        writer.close();
    }

}
