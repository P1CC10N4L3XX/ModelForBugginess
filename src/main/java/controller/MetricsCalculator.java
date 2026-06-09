package controller;

import client.GitManager;
import models.ClassRecord;
import models.Commit;
import models.GitFileChange;

import java.io.IOException;
import java.util.*;


public class MetricsCalculator {

    private MetricsCalculator(){}

    public static ClassRecord calculateMetrics(String classPath, Commit commitPrevRelease, Commit commitActualRelease) throws IOException, InterruptedException {
        List<GitFileChange> history = GitManager.getFileHistory(classPath, commitPrevRelease, commitActualRelease);

        ClassRecord classRecord = new ClassRecord();

        Set<String> authors = new HashSet<>();
        Set<String> commits = new HashSet<>();

        Map<String, Integer> revisionsLOCAdded = new HashMap<>();
        Map<String, Integer> changeSetPerCommit = new HashMap<>();

        int churn = 0;
        int maxChurn = 0;

        int totalAdded = 0;
        int maxAdded = 0;

        int locAuthors = 0;

        int weightedAgeSum = 0;

        for(GitFileChange gitFileChange : history){
            authors.add(gitFileChange.getCommit().getAuthor());
            commits.add(gitFileChange.getCommit().getHash());

            revisionsLOCAdded.merge(gitFileChange.getCommit().getHash(), gitFileChange.getAdded(), Integer::sum);
            changeSetPerCommit.merge(gitFileChange.getCommit().getHash(), 1, Integer::sum);

            int localChurn = gitFileChange.getAdded() + gitFileChange.getDeleted();

            churn += localChurn;
            totalAdded += gitFileChange.getAdded();

            maxChurn = Math.max(maxChurn, localChurn);
            maxAdded = Math.max(maxAdded, gitFileChange.getAdded());

            int ageWeight = (int) (commitActualRelease.getCommitDate().toLocalDate().toEpochDay() -
                    gitFileChange.getCommit().getCommitDate().toLocalDate().toEpochDay());
            weightedAgeSum += ageWeight;
        }

        int revisions = commits.size();


        int loc = GitManager.getLocAtCommit(classPath, commitActualRelease);
        locAuthors = revisions == 0 ? 0 : loc/ authors.size();

        int changeSetSize = changeSetPerCommit.size();
        int maxChangeSet = changeSetPerCommit.values()
                        .stream().mapToInt(i -> i).max().orElse(0);

        int age = history.isEmpty() ? 0 : (int) (commitActualRelease.getCommitDate().toLocalDate().toEpochDay() - history.getFirst().getCommit().getCommitDate().toLocalDate().toEpochDay());

        classRecord.setLoc(loc);
        classRecord.setNumberRevision(revisions);
        classRecord.setNumberAuthors(authors.size());

        classRecord.setChurn(churn);
        classRecord.setMaxChurn(maxChurn);
        classRecord.setAverageChurn(revisions == 0 ? 0 : (double) churn / revisions);

        classRecord.setMaxOverRevisionLOCAdded(maxAdded);
        classRecord.setAverageLOCAddedPerRevision(revisions == 0 ? 0 : (double) totalAdded / revisions);

        classRecord.setChangeSetSize(changeSetSize);
        classRecord.setMaxChangeSet(maxChangeSet);
        classRecord.setAverageChangeSet(revisions == 0 ? 0 : (double) changeSetSize / revisions);

        classRecord.setLocAuthors(locAuthors);

        classRecord.setAge(age);
        classRecord.setWeightedAge(revisions == 0 ? 0 : weightedAgeSum / revisions);

        return classRecord;
    }
}


