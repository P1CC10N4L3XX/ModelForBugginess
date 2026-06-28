package controller;

import client.GitManager;
import client.PMDManager;
import exceptions.CommitOfReleaseNotFoundException;
import exceptions.FirstCommitOfProjectNotFoundException;
import models.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import utils.ClassMetricsWriter;
import utils.ProgressBar;
import utils.interfaces.ResultWriter;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class DatasetCreationController {

    private static final String METRICS_FILE = "Syncope_classes_metrics.csv";
    private static final Logger LOGGER = LoggerFactory.getLogger(DatasetCreationController.class);

    private DatasetCreationController(){}
    public static void run() throws IOException, InterruptedException, FirstCommitOfProjectNotFoundException, CommitOfReleaseNotFoundException {
        LOGGER.info("Starting data collection for project SYNCOPE...");

        LOGGER.info("Collecting releases...");
        List<ProjectRelease> releases = GetReleaseInfo.run();
        LOGGER.info("Total releases found: {}", releases.size());

        LOGGER.info("Collecting tickets...");
        List<TicketBugRecord> tickets = GetTicketInfo.run();
        LOGGER.info("Total tickets found: {}", tickets.size());

        LOGGER.info("Collection completed.");
        LOGGER.info("Results saved to SYNCOPE_Releases.csv and SYNCOPE_Tickets.csv");

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

        LOGGER.info("Collecting all git history...");

        Map<Integer, List<String>> buggyMap = SZZ.computeBuggyClasses(releases, tickets);
        LOGGER.info("Computed buggy classes");
        Map<ProjectRelease, Commit> commitForEachRelease = GitManager.getLastCommitForEachRelease(releasesToProcess);
        LOGGER.info("Computed commit for each release");
        Map<ProjectRelease, Map<String, List<GitFileChange>>> fullHistoryMap = GitManager.getFullHistoryForEachRelease(commitForEachRelease);
        LOGGER.info("Computed fullHistoryMap");
        Map<ProjectRelease, Map<String, Integer>> locForEachRelease = GitManager.getAllLocForEachRelease(commitForEachRelease);
        LOGGER.info("Computed all locs");

        Commit firstCommitOfProject = GitManager.getFirstCommitOfProject();


        LOGGER.info("All history from git collected");

        LOGGER.info("Number of releases to process: {}", releasesToProcess.size());

        ResultWriter<ClassRecord> writer = new ClassMetricsWriter(METRICS_FILE);
        writer.writeHeader();



        for(int i = 0; i<releasesToProcess.size(); i++){
            ProgressBar.printProgress(i, releasesToProcess.size(), LOGGER);


            Commit commitActualRelease = commitForEachRelease.get(releasesToProcess.get(i));
            Commit commitPrevRelease = i > 0 ? commitForEachRelease.get(releasesToProcess.get(i-1)) : firstCommitOfProject;
            List<String> javaClassPaths = GitManager.getJavaFilesPerCommit(commitActualRelease);
            Map<String, List<GitFileChange>> historyMapFromStart = fullHistoryMap.get(releasesToProcess.get(i));
            Map<String, List<GitFileChange>> historyMapInRelease = GitManager.getHistoryInRelease(commitPrevRelease, commitActualRelease, historyMapFromStart);
            Map<String, Integer> locMap = locForEachRelease.get(releasesToProcess.get(i));
            Map<String, String> contentMap = GitManager.getAllFileContentAtCommit(commitActualRelease);
            Map<String, Integer> smellsMap = PMDManager.getAllSmells(contentMap);
            Map<String, Integer> cyclomaticComplexityMap = PMDManager.getCyclomaticComplexityPerFile(contentMap);
            Map<String, Integer> publicMethodsMap = PMDManager.getPublicMethodsCountPerFile(contentMap);


            for(String classPath : javaClassPaths){
                List<GitFileChange> historyFromStart = historyMapFromStart.getOrDefault(classPath, Collections.emptyList());
                List<GitFileChange> historyInRelease = historyMapInRelease.getOrDefault(classPath, Collections.emptyList());
                int loc = locMap.getOrDefault(classPath, 0);

                ClassRecord classRecord = MetricsCalculator.calculateMetrics(classPath, historyFromStart, historyInRelease, loc, commitActualRelease);
                classRecord.setRelease(releasesToProcess.get(i).getName());

                int nSmells = smellsMap.getOrDefault(classPath, 0);
                int cyclomaticComplexity = cyclomaticComplexityMap.getOrDefault(classPath, 0);
                int nPublicMethods = publicMethodsMap.getOrDefault(classPath, 0);
                classRecord.setSmells(nSmells);
                classRecord.setSmellsDensity(loc == 0 ? 0 : (double)nSmells/loc);
                classRecord.setNumberPublicMethods(nPublicMethods);
                classRecord.setCyclomaticComplexity(cyclomaticComplexity);
                List<String> buggyClasses = buggyMap.getOrDefault(i, List.of());
                classRecord.setBuggy(buggyClasses.contains(classPath));

                writer.writeResult(classRecord);
            }
        }

        writer.close();

    }

}
