package controller;

import models.ClassRecord;
import models.Commit;
import models.GitFileChange;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;


public class MetricsCalculator {

    private MetricsCalculator(){}

    public static ClassRecord calculateMetrics(String classPath, List<GitFileChange> historyFromStart,List<GitFileChange> historyInRelease,int loc, Commit lastCommitActualRelease) throws IOException, InterruptedException {
        ClassRecord classRecord = new ClassRecord();
        classRecord.setLoc(loc);

        LocalDateTime lastCommitDate = lastCommitActualRelease.getCommitDate();


        //====================================================================
        // METRICS FROM RELEASE 0 -> calculated on historyFromStart
        //====================================================================

        Set<String> authorsTotal = new HashSet<>();
        int locTouched = 0;
        int weightedAgeSumTotal = 0;
        int age = 0;

        for (GitFileChange change : historyFromStart){
            authorsTotal.add(change.getCommit().getAuthor());
            locTouched += change.getAdded() + change.getDeleted();


            int daysFromCommit = (int) (lastCommitDate.toLocalDate().toEpochDay() -
                    change.getCommit().getCommitDate().toLocalDate().toEpochDay());
            weightedAgeSumTotal += daysFromCommit;
        }

        if (!historyFromStart.isEmpty()){
            LocalDateTime firstChangeDate = historyFromStart
                    .getLast()
                    .getCommit().getCommitDate();
            age = (int) (
                        lastCommitDate.toLocalDate().toEpochDay() -
                        firstChangeDate.toLocalDate().toEpochDay()
                    );
        }

        int totalRevisions = countDistinctCommits(historyFromStart);
        double weightedAge = totalRevisions == 0 ? 0 : (double) weightedAgeSumTotal / totalRevisions;

        //====================================================================
        // METRICS WITHIN THE RELEASE RELEASE -> calculated on historyInRelease
        //====================================================================

        Set<String> authorsInRelease = new HashSet<>();
        Set<String> commitsInRelease = new HashSet<>();
        Map<String, Integer> locAddedPerCommit = new HashMap<>();
        Map<String, Integer> changeSetPerCommit = new HashMap<>();

        int churn = 0;
        int maxChurn = 0;
        int totalAdded = 0;
        int maxAdded = 0;

        for (GitFileChange change : historyInRelease){
            String hash = change.getCommit().getHash();
            String author = change.getCommit().getAuthor();

            authorsInRelease.add(author);
            commitsInRelease.add(hash);

            locAddedPerCommit.merge(hash, change.getAdded(), Integer::sum);
            changeSetPerCommit.merge(hash, 1, Integer::sum);

            int localChurn = change.getAdded() + change.getDeleted();
            churn += localChurn;
            totalAdded += change.getAdded();

            maxChurn = Math.max(maxChurn, localChurn);
            maxAdded = Math.max(maxAdded, change.getAdded());
        }

        int revisions = commitsInRelease.size();
        int nAuthors = authorsInRelease.size();

        int locAuthors = (nAuthors == 0 || loc == 0) ? 0 : loc/nAuthors;

        int maxLocAdded = locAddedPerCommit.values().stream()
                        .mapToInt(Integer :: intValue).max().orElse(0);
        int changeSetSize = changeSetPerCommit.size();
        int maxChangeSet = changeSetPerCommit.values().stream()
                        .mapToInt(Integer :: intValue).max().orElse(0);
        double avgChangeSet = revisions == 0 ? 0 : (double) changeSetSize / revisions;

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

        //from release 0


        classRecord.setAge(age);
        classRecord.setWeightedAge(weightedAge);
        classRecord.setLocTouched(locTouched);
        classRecord.setBuggy(false);

        return classRecord;
     }

    private static int countDistinctCommits(List<GitFileChange> history) {
        Set<String> hashes = new HashSet<>();
        for (GitFileChange change : history){
            hashes.add(change.getCommit().getHash());
        }
        return hashes.size();
    }
}


