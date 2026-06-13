package controller;

import models.ClassRecord;
import models.Commit;
import models.GitFileChange;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;


public class MetricsCalculator {

    private MetricsCalculator(){}

    public static ClassRecord calculateMetrics(String classPath, List<GitFileChange> history, int loc, Commit lastCommitActualRelease) throws IOException, InterruptedException {
        ClassRecord classRecord = new ClassRecord();
        classRecord.setLoc(loc);

        Set<String> authors = new HashSet<>();
        Set<String> commits = new HashSet<>();
        Map<String, Integer> locAddedPerCommit = new HashMap<>();
        Map<String, Integer> changeSetPerCommit = new HashMap<>();

        int churn = 0;
        int maxChurn = 0;
        int totalAdded = 0;
        int maxAdded = 0;
        int weightedAgeSum = 0;

        LocalDateTime lastCommitOfReleaseDate = lastCommitActualRelease.getCommitDate();

        for (GitFileChange change : history){
            String hash = change.getCommit().getHash();
            String author = change.getCommit().getAuthor();
            LocalDateTime changeDate = change.getCommit().getCommitDate();

            authors.add(author);
            commits.add(hash);

            locAddedPerCommit.merge(hash, change.getAdded(), Integer::sum);

            changeSetPerCommit.merge(hash, 1, Integer::sum);

            int localChurn = change.getAdded() + change.getDeleted();
            churn += localChurn;
            totalAdded += change.getAdded();

            maxChurn = Math.max(maxChurn, churn);
            maxAdded = Math.max(maxAdded, change.getAdded());

            int daysFromCommit = (int) (lastCommitOfReleaseDate.toLocalDate().toEpochDay() - changeDate.toLocalDate().toEpochDay());
            weightedAgeSum += daysFromCommit;
        }

        int revisions = commits.size();
        int nAuthors = authors.size();

        int locAuthors = ((nAuthors == 0 || loc == 0) ? 0 : loc/nAuthors);
        int maxLocAdded = locAddedPerCommit.values().stream().mapToInt(Integer::intValue).max().orElse(0);

        int changeSetSize = changeSetPerCommit.size();
        int maxChangeSet = changeSetPerCommit.values().stream().mapToInt(Integer::intValue).max().orElse(0);
        double avgChangeSet = (revisions == 0 ? 0 : (double) changeSetSize / revisions);

        int age = 0;

        if(!history.isEmpty()){
            LocalDateTime firstChangeDate = history.getFirst().getCommit().getCommitDate();
            age = (int) (lastCommitOfReleaseDate.toLocalDate().toEpochDay() - firstChangeDate.toLocalDate().toEpochDay());
        }

        double weightedAge = (revisions == 0 ? 0 : (double) weightedAgeSum / revisions);

        classRecord.setClassName(classPath);
        classRecord.setNumberRevision(revisions);
        classRecord.setNumberAuthors(nAuthors);
        classRecord.setLocAuthors(locAuthors);
        classRecord.setMaxOverRevisionLOCAdded(maxLocAdded);
        classRecord.setAverageLOCAddedPerRevision(revisions == 0 ? 0 : (double) totalAdded / revisions);
        classRecord.setChurn(churn);
        classRecord.setMaxChurn(maxChurn);
        classRecord.setAverageChurn(revisions == 0 ? 0 : (double) churn / revisions);
        classRecord.setChangeSetSize(changeSetSize);
        classRecord.setMaxChangeSet(maxChangeSet);
        classRecord.setAverageChangeSet(avgChangeSet);
        classRecord.setAge(age);
        classRecord.setWeightedAge(weightedAge);
        classRecord.setBuggy(false);

        return classRecord;
     }
}


