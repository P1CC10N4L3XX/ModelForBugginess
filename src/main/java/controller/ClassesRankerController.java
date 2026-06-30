package controller;

import client.GitManager;
import client.PMDManager;
import exceptions.CommitOfReleaseNotFoundException;
import models.ClassRecord;
import models.Commit;
import models.ProjectRelease;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import utils.RankedClassesWriter;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ClassesRankerController {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClassesRankerController.class);
    private static final String RANKED_CLASSES_PATH = "ranked_classes.csv";

    private ClassesRankerController(){}

    public static void run() throws IOException, InterruptedException, CommitOfReleaseNotFoundException {

        LOGGER.info("Collecting releases...");
        List<ProjectRelease> releases = GetReleaseInfo.run();
        ProjectRelease lastRelease = releases.getLast();
        LOGGER.info("Last release found: {}", lastRelease.getName());

        GitManager.cloneRepo();

        LOGGER.info("Collecting git history for each file of last release...");
        Map<ProjectRelease, Commit> commitForEachRelease = GitManager.getLastCommitForEachRelease(releases);
        Commit commitLastRelease = commitForEachRelease.get(lastRelease);
        Map<String, String> contentMap = GitManager.getAllFileContentAtCommit(commitLastRelease);
        Map<String, Integer> sortedSmellsMap = PMDManager.getAllSmells(contentMap)
                .entrySet()
                .stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (e1, e2) -> e1,
                        LinkedHashMap::new
                        )

                );
        Map<String, Integer> cyclomaticComplexityMap = PMDManager.getCyclomaticComplexityPerFile(contentMap);
        Map<String, Integer> publicMethodsMap = PMDManager.getPublicMethodsCountPerFile(contentMap);
        Map<ProjectRelease, Map<String, Integer>> locForEachRelease = GitManager.getAllLocForEachRelease(commitForEachRelease);
        Map<String, Integer> locMap = locForEachRelease.get(lastRelease);
        LOGGER.info("Git history collected");

        LOGGER.info("Creating {}", RANKED_CLASSES_PATH);
        RankedClassesWriter rankedClassesWriter = new RankedClassesWriter(RANKED_CLASSES_PATH);
        rankedClassesWriter.writeHeader();

        for (Map.Entry<String, Integer> entry : sortedSmellsMap.entrySet()){
            int cc = cyclomaticComplexityMap.getOrDefault(entry.getKey(), 0);
            int publicMethods = publicMethodsMap.getOrDefault(entry.getKey(), 0);
            int loc = locMap.getOrDefault(entry.getKey(), 0);
            if (cc < 10 || publicMethods < 5 || loc < 200 || entry.getKey().contains("/test/") || entry.getValue() == 0) continue;
            ClassRecord classRecord = new ClassRecord();
            classRecord.setClassName(entry.getKey());
            classRecord.setCyclomaticComplexity(cc);
            classRecord.setNumberPublicMethods(publicMethods);
            classRecord.setRelease(lastRelease.getName());
            classRecord.setSmells(entry.getValue());
            rankedClassesWriter.writeResult(classRecord);
        }
        rankedClassesWriter.close();
        LOGGER.info("{} created", RANKED_CLASSES_PATH);
    }
}
