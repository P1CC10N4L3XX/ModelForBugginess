package org.ModelForBugginess;

import controller.GetReleaseInfo;
import controller.GetTicketInfo;
import client.GitManager;
import controller.MetricsCalculator;
import exceptions.CommitOfReleaseNotFoundException;
import exceptions.FirstCommitOfProjectNotFoundException;
import models.ClassRecord;
import models.Commit;
import models.ProjectRelease;
import models.TicketBugRecord;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

public class Main {
    private static final String METRICS_FILE = "Syncope_classes_metrics.csv";

    public static void main() throws Exception {
        System.out.println("Starting data collection for project SYNCOPE...");

        System.out.println("Collecting releases...");
        List<ProjectRelease> releases = GetReleaseInfo.run();
        System.out.println("Total releases found: " + releases.size());

        System.out.println("Collecting tickets...");
        List<TicketBugRecord> tickets = GetTicketInfo.run();
        System.out.println("Total tickets found: " + tickets.size());

        System.out.println("Collection completed.");
        System.out.println("Results saved to SYNCOPE_Releases.csv and SYNCOPE_Tickets.csv");

        initMetricsFile();

        releases.removeIf(r ->
                r.getName().toLowerCase().contains("incubating") ||
                r.getName().toLowerCase().contains(".*-m\\d+.*") ||
                r.getName().toLowerCase().contains("rc") ||
                r.getName().toLowerCase().contains("snapshot") ||
                r.getName().toLowerCase().contains("ea") ||
                r.getName().toLowerCase().contains("archetype")
        );
        int limit = (int)Math.ceil(releases.size() * 0.34);
        List<ProjectRelease> releasesToProcess = releases.subList(0, limit);
        GitManager.cloneRepo();

        for(int i = 0; i<releasesToProcess.size(); i++){
            printProgress(i,releasesToProcess.size());
            try {
                Commit commitActualRelease = GitManager.getLastCommitOfRelease(releasesToProcess.get(i));
                Commit commitPrevRelease = i > 0 ? GitManager.getLastCommitOfRelease(releasesToProcess.get(i-1)) : GitManager.getFirstCommitOfProject();
                List<String> javaClassPaths = GitManager.getJavaFilesPerCommit(commitActualRelease);
                for(String javaClassPath: javaClassPaths){
                    try {
                        ClassRecord classRecord = MetricsCalculator.calculateMetrics(javaClassPath, commitPrevRelease, commitActualRelease);
                        classRecord.setClassName(javaClassPath);
                        classRecord.setRelease(releasesToProcess.get(i).getName());
                        writeClassRecordToCSV(classRecord);
                    }catch (Exception e){
                        System.out.println(e.getMessage());
                    }
                }
            } catch (CommitOfReleaseNotFoundException | FirstCommitOfProjectNotFoundException e) {
                System.out.println(e.getMessage());
            }


        }

    }

    private static void printProgress(int current, int total){
        int percent = (int) ((current * 100.0) / total);

        int barLength = 30;
        int filled = (int) (barLength * percent / 100.0);

        StringBuilder bar = new StringBuilder();

        bar.append("\r[");
        for(int i=0; i<barLength; i++){
            if(i<filled) bar.append('■');
            else bar.append(" ");
        }

        bar.append(percent).append("% (")
                .append(current).append("/")
                .append(total).append(")");
        System.out.println(bar);
    }

    private static void initMetricsFile() {
        try (PrintWriter writer = new PrintWriter(METRICS_FILE)) {
            writer.println("release,className,smells,smellsDensity,loc,numberRevision,numberDefectedVersion,numberAuthors,locAuthors,maxOverRevisionLOCAdded,averageLOCAddedPerRevision,churn,maxChurn,averageChurn,changeSetSize,maxChangeSet,averageChangeSet,age,weightedAge,buggy");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void writeClassRecordToCSV(ClassRecord classRecord) {
        try (FileWriter fw = new FileWriter(METRICS_FILE, true);
             PrintWriter writer = new PrintWriter(fw)) {

            writer.print(classRecord.getRelease() + ",");
            writer.print(classRecord.getClassName() + ",");
            writer.print(classRecord.getSmells() + ",");
            writer.print(classRecord.getSmellsDensity() + ",");
            writer.print(classRecord.getLoc() + ",");
            writer.print(classRecord.getNumberRevision() + ",");
            writer.print(classRecord.getNumberDefectedVersion() + ",");
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
            writer.print(classRecord.getAge() + ",");
            writer.print(classRecord.getWeightedAge() + ",");
            writer.println(classRecord.isBuggy());

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
