package models;


public class ClassRecord {
    private String release;
    private String className;
    private int smells;
    private int smellsDensity;
    private int loc;
    private int numberRevision;
    private int numberDefectedVersion;
    private int locAuthors;
    private int numberAuthors;
    private int maxOverRevisionLOCAdded;
    private double averageLOCAddedPerRevision;
    private int churn;
    private int maxChurn;
    private double averageChurn;
    private int changeSetSize;
    private int maxChangeSet;
    private double averageChangeSet;
    private int age;
    private int weightedAge;
    private boolean buggy;

    public ClassRecord(){}

    public ClassRecord(String release,
                       String className,
                       int smells,
                       int smellsDensity,
                       int loc,
                       int numberRevision,
                       int numberDefectedVersion,
                       int numberAuthors,
                       int locAuthors,
                       int maxOverRevisionLOCAdded,
                       double averageLOCAddedPerRevision,
                       int churn,
                       int maxChurn,
                       double averageChurn,
                       int changeSetSize,
                       int maxChangeSet,
                       double averageChangeSet,
                       int age,
                       int weightedAge,
                       boolean buggy
                        ){
        this.release = release;
        this.className = className;
        this.smells = smells;
        this.smellsDensity = smellsDensity;
        this.loc = loc;
        this.numberRevision = numberRevision;
        this.numberDefectedVersion = numberDefectedVersion;
        this.numberAuthors = numberAuthors;
        this.locAuthors = locAuthors;
        this.maxOverRevisionLOCAdded = maxOverRevisionLOCAdded;
        this.churn = churn;
        this.maxChurn = maxChurn;
        this.averageChurn = averageChurn;
        this.changeSetSize = changeSetSize;
        this.maxChangeSet = maxChangeSet;
        this.averageChangeSet = averageChangeSet;
        this.age = age;
        this.weightedAge = weightedAge;
        this.averageLOCAddedPerRevision = averageLOCAddedPerRevision;
        this.buggy = buggy;
    }

    public String getRelease() {
        return release;
    }

    public void setRelease(String release) {
        this.release = release;
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public int getSmells() {
        return smells;
    }

    public void setSmells(int smells) {
        this.smells = smells;
    }

    public int getSmellsDensity() {
        return smellsDensity;
    }

    public void setSmellsDensity(int smellsDensity) {
        this.smellsDensity = smellsDensity;
    }

    public int getLoc() {
        return loc;
    }

    public void setLoc(int loc) {
        this.loc = loc;
    }

    public int getNumberRevision() {
        return numberRevision;
    }

    public void setNumberRevision(int numberRevision) {
        this.numberRevision = numberRevision;
    }

    public int getNumberDefectedVersion() {
        return numberDefectedVersion;
    }

    public void setNumberDefectedVersion(int numberDefectedVersion) {
        this.numberDefectedVersion = numberDefectedVersion;
    }

    public int getMaxOverRevisionLOCAdded() {
        return maxOverRevisionLOCAdded;
    }

    public void setMaxOverRevisionLOCAdded(int maxOverRevisionLOCAdded) {
        this.maxOverRevisionLOCAdded = maxOverRevisionLOCAdded;
    }

    public int getLocAuthors() {
        return locAuthors;
    }

    public void setLocAuthors(int locAuthors) {
        this.locAuthors = locAuthors;
    }

    public int getNumberAuthors() {
        return numberAuthors;
    }

    public void setNumberAuthors(int numberAuthors) {
        this.numberAuthors = numberAuthors;
    }

    public double getAverageLOCAddedPerRevision() {
        return averageLOCAddedPerRevision;
    }

    public void setAverageLOCAddedPerRevision(double averageLOCAddedPerRevision) {
        this.averageLOCAddedPerRevision = averageLOCAddedPerRevision;
    }

    public int getChurn() {
        return churn;
    }

    public void setChurn(int churn) {
        this.churn = churn;
    }

    public int getMaxChurn() {
        return maxChurn;
    }

    public void setMaxChurn(int maxChurn) {
        this.maxChurn = maxChurn;
    }

    public double getAverageChurn() {
        return averageChurn;
    }

    public void setAverageChurn(double averageChurn) {
        this.averageChurn = averageChurn;
    }

    public int getChangeSetSize() {
        return changeSetSize;
    }

    public void setChangeSetSize(int changeSetSize) {
        this.changeSetSize = changeSetSize;
    }

    public int getMaxChangeSet() {
        return maxChangeSet;
    }

    public void setMaxChangeSet(int maxChangeSet) {
        this.maxChangeSet = maxChangeSet;
    }

    public double getAverageChangeSet() {
        return averageChangeSet;
    }

    public void setAverageChangeSet(double averageChangeSet) {
        this.averageChangeSet = averageChangeSet;
    }

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }

    public int getWeightedAge() {
        return weightedAge;
    }

    public void setWeightedAge(int weightedAge) {
        this.weightedAge = weightedAge;
    }

    public boolean isBuggy() {
        return buggy;
    }

    public void setBuggy(boolean buggy) {
        this.buggy = buggy;
    }
}


