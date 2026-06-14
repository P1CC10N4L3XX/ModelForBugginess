package org.ModelForBugginess;

import client.PMDManager;
import controller.GetReleaseInfo;
import controller.GetTicketInfo;
import client.GitManager;
import controller.MetricsCalculator;
import controller.SZZ;
import exceptions.CommitOfReleaseNotFoundException;
import exceptions.FirstCommitOfProjectNotFoundException;
import models.*;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

import java.util.Collections;
import java.util.List;
import java.util.Map;

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

        System.out.println("Collecting all git history...");

        Map<Integer, List<String>> buggyMap = SZZ.computeBuggyClasses(releases, tickets);
        System.out.println("Computed buggy classes");
        Map<ProjectRelease, Commit> commitForEachRelease = GitManager.getLastCommitForEachRelease(releasesToProcess);
        System.out.println("Computed commit for each release");
        Map<ProjectRelease, Map<String, List<GitFileChange>>> fullHistoryMap = GitManager.getFullHistoryForEachRelease(commitForEachRelease);
        System.out.println("Computed fullHistoryMap");
        Commit firstCommitOfProject = GitManager.getFirstCommitOfProject();

        System.out.println("All history from git collected");


        for(int i = 0; i<releasesToProcess.size(); i++){
            printProgress(i, releasesToProcess.size());


            Commit commitActualRelease = commitForEachRelease.get(releasesToProcess.get(i));
            Commit commitPrevRelease = i > 0 ? commitForEachRelease.get(releasesToProcess.get(i-1)) : firstCommitOfProject;
            List<String> javaClassPaths = GitManager.getJavaFilesPerCommit(commitActualRelease);
            Map<String, List<GitFileChange>> historyMapFromStart = fullHistoryMap.get(releasesToProcess.get(i));
            //Map<String, Integer> locMap = GitManager.getAllLocAtCommit(javaClassPaths,commitActualRelease);
            //Map<String, String> contentMap = GitManager.getAllFileContentAtCommit(commitActualRelease);
            //Map<String, Integer> smellsMap = PMDManager.getAllSmells(contentMap);

            //TODO locMap, contentMap e smellsMap out from for
            for(String classPath : javaClassPaths){
                try {
                    List<GitFileChange> history = historyMapFromStart.getOrDefault(classPath, Collections.emptyList());
                    //int loc = locMap.getOrDefault(classPath, 0);
                    int loc = 0;
                    //TODO: modify calculateMetrics to calculate the metrics with respect to actuale release and from release 0
                    ClassRecord classRecord = MetricsCalculator.calculateMetrics(classPath, history, loc, commitActualRelease);
                    classRecord.setRelease(releasesToProcess.get(i).getName());

                    //int nSmells = smellsMap.getOrDefault(classPath, 0);
                    //classRecord.setSmells(nSmells);
                    //classRecord.setSmellsDensity(loc == 0 ? 0 : (double)nSmells/loc);

                    List<String> buggyClasses = buggyMap.getOrDefault(i, List.of());
                    classRecord.setBuggy(buggyClasses.contains(classPath));

                    writeClassRecordToCSV(classRecord);
                }catch (Exception e){
                    e.printStackTrace();
                }
            }
        }
        System.out.println("Done! Results saved to "+METRICS_FILE);

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
            writer.println(classRecord.isBuggy() ? "yes" : "no");

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
