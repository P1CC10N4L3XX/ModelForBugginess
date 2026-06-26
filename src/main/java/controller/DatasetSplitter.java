package controller;

import weka.core.Instances;

public class DatasetSplitter {

    private final Instances datasetA;
    private final int nSmellsIndex;

    public DatasetSplitter(Instances datasetA){
        this.datasetA = datasetA;
        this.nSmellsIndex = datasetA.attribute("smells").index();
    }

    public Instances getBPlus(){
        Instances bPlus = new Instances(datasetA, 0);
        for (int i = 0; i < datasetA.numInstances(); i++){
            if (datasetA.instance(i).value(nSmellsIndex) > 0){
                bPlus.add(datasetA.instance(i));
            }
        }
        return bPlus;
    }

    public Instances getB(Instances bPlus){
        Instances b = new Instances(bPlus);
        for (int i = 0; i < b.numInstances(); i++){
            b.instance(i).setValue(nSmellsIndex, 0);
        }
        return b;
    }

    public Instances getC(){
        Instances c = new Instances(datasetA, 0);
        for (int i=0; i<datasetA.numInstances(); i++){
            if (datasetA.instance(i).value(nSmellsIndex) == 0){
                c.add(datasetA.instance(i));
            }
        }
        return c;
    }
}
