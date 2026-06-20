package client;

import exceptions.CommitOfReleaseNotFoundException;
import exceptions.FirstCommitOfProjectNotFoundException;
import models.Commit;
import models.GitFileChange;
import models.ProjectRelease;
import models.TicketBugRecord;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import utils.ConfigManager;

import java.io.*;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static utils.CommandRunner.runCommand;

public class GitManager {

    private GitManager(){
        //empty constructor
    }

    private static final String LOCAL_REPO_PATH = ConfigManager.getInstance().getProperty("localRepoPath");
    private static final String ISO_STRICT_FORMAT = "iso-strict";
    private static final String JAVA_EXTENSION = ".java";
    private static final String DATE_OPTION = "--date=";
    private static final String GIT = "git";
    private static final Logger LOGGER = LoggerFactory.getLogger(GitManager.class);

    public static void cloneRepo() throws IOException, InterruptedException{
        String githubRepoUrl = ConfigManager.getInstance().getProperty("GithubRepoUrl");
        File repoDir = new File(LOCAL_REPO_PATH);
        if(!repoDir.exists()){
            LOGGER.info("Cloning repository...");
            runCommand(".", GIT, "clone", githubRepoUrl, LOCAL_REPO_PATH);
        }
    }

    public static Map<String, String> getAllFileContentAtCommit(Commit commit) throws IOException, InterruptedException{

        Map<String, String> contentMap = new HashMap<>();



        ProcessBuilder processBuilder = new ProcessBuilder(
                GIT,
                "archive",
                commit.getHash(),
                "--format=tar"
        );

        processBuilder.directory(new File(LOCAL_REPO_PATH));
        Process process = processBuilder.start();

        try (TarArchiveInputStream tarStream = new TarArchiveInputStream(process.getInputStream())){
            TarArchiveEntry entry;
            while((entry = tarStream.getNextEntry()) != null){
                String name = entry.getName();
                if(!name.endsWith(JAVA_EXTENSION)) continue;
                byte[] bytes = tarStream.readAllBytes();
                String content = new String(bytes);
                contentMap.put(name, content);
            }
        }

        process.waitFor();
        return contentMap;


    }

    public static List<String> getJavaFilesPerCommit(Commit commit) throws IOException, InterruptedException{

        ProcessBuilder pb = new ProcessBuilder(
                GIT,
                "ls-tree",
                "-r",
                "--name-only",
                commit.getHash()
        );
        pb.directory(new File(LOCAL_REPO_PATH));

        Process process = pb.start();

        List<String> javaFiles = new ArrayList<>();

        try(BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))){
            String line;
            while((line = reader.readLine()) != null){
                if(line.endsWith(JAVA_EXTENSION)){
                    javaFiles.add(line);
                }
            }
        }

        process.waitFor();

        return javaFiles;
    }

    public static Commit getFirstCommitOfProject() throws FirstCommitOfProjectNotFoundException,IOException, InterruptedException{

        ProcessBuilder processBuilder = new ProcessBuilder(
                GIT,
                "rev-list",
                "--reverse",
                "--pretty=format:%H|%an|%ad|%s",
                DATE_OPTION + ISO_STRICT_FORMAT,
                "HEAD"
        );

        processBuilder.directory(new File(LOCAL_REPO_PATH));

        Process process = processBuilder.start();

        String firstLine = null;

        try(BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(process.getInputStream()))){
            String line;
            while ((line = bufferedReader.readLine())!=null){
                if (line.contains("|")){
                    firstLine = line;
                    break;
                }
            }
        }
        process.waitFor();
        if (firstLine == null){
            throw new FirstCommitOfProjectNotFoundException();
        }

        String[] parts = firstLine.split("\\|");

        String hash = parts[0].replace("commit", "").trim().split("\\s+")[0];
        String author = parts[1];
        LocalDateTime commitDate = OffsetDateTime.parse(parts[2]).toLocalDateTime();
        String message = parts[3];

        return new Commit(hash,author,commitDate,message);


    }

    public static Map<ProjectRelease, Commit> getLastCommitForEachRelease(List<ProjectRelease> releases) throws IOException, InterruptedException, CommitOfReleaseNotFoundException {
        Map<ProjectRelease, Commit> releaseCommitMap = new HashMap<>();

        ProcessBuilder processBuilder = new ProcessBuilder(
                GIT,
                "log",
                "--format=%H|%an|%ad|%s",
                DATE_OPTION + ISO_STRICT_FORMAT,
                "HEAD"
        );

        processBuilder.directory(new File(LOCAL_REPO_PATH));
        Process process = processBuilder.start();

        List<Commit> allCommits = new ArrayList<>();

        try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(process.getInputStream()))){
            String line;

            while ((line = bufferedReader.readLine()) != null){

                line = line.trim();
                String[] parts;
                if(line.isEmpty() || (parts=line.split("\\|",4)).length < 4) continue;

                try {
                    String hash = parts[0].trim();
                    String author = parts[1].trim();
                    LocalDateTime date = OffsetDateTime.parse(parts[2].trim()).toLocalDateTime();
                    String message = parts[3].trim();
                    allCommits.add(new Commit(hash,author,date,message));
                }catch (Exception _){
                    //ignore all the commits that are not formattable
                }
            }
        }

        process.waitFor();

        for (ProjectRelease release : releases){
            LocalDateTime releaseDate = release.getReleaseDate();

            Commit lastCommitOfRelease = null;
            for (Commit commit : allCommits){
                if(!commit.getCommitDate().isAfter(releaseDate)){
                    lastCommitOfRelease = commit;
                    break;
                }
            }
            if (lastCommitOfRelease != null){
                releaseCommitMap.put(release, lastCommitOfRelease);
            }else {
                throw new CommitOfReleaseNotFoundException();
            }
        }

        return releaseCommitMap;
    }

    public static Map<String, List<GitFileChange>> getHistoryInRelease(Commit commitPrevRelease, Commit commitActualRelease, Map<String, List<GitFileChange>> historyFromStart){
        Map<String, List<GitFileChange>> historyInRelease = new HashMap<>();
        LocalDateTime dateFrom = commitPrevRelease.getCommitDate();
        LocalDateTime dateTo = commitActualRelease.getCommitDate();

        for (Map.Entry<String, List<GitFileChange>> entry : historyFromStart.entrySet()){
            List<GitFileChange> filtered = entry.getValue().stream()
                    .filter(c->c.getCommit().getCommitDate().isAfter(dateFrom) &&
                            !c.getCommit().getCommitDate().isAfter(dateTo))
                    .toList();
            if (!filtered.isEmpty()){
                historyInRelease.put(entry.getKey(), filtered);
            }
        }

        return historyInRelease;
    }

    public static Map<ProjectRelease, Map<String, List<GitFileChange>>> getFullHistoryForEachRelease(Map<ProjectRelease, Commit> releaseCommitMap) throws IOException, InterruptedException{
        Map<ProjectRelease, Map<String, List<GitFileChange>>> result = new LinkedHashMap<>();

        for (ProjectRelease release : releaseCommitMap.keySet()){
            result.put(release, new HashMap<>());
        }

        List<ProjectRelease> sortedReleases = releaseCommitMap.keySet().stream()
                .sorted(Comparator.comparing(ProjectRelease::getReleaseDate))
                .toList();

        ProjectRelease lastRelease = sortedReleases.getLast();
        Commit lastCommit = releaseCommitMap.get(lastRelease);


        Map<String, List<GitFileChange>> fullHistory = buildFullHistory(lastCommit);


        for (ProjectRelease release : sortedReleases){
            LocalDateTime releaseDate = releaseCommitMap.get(release).getCommitDate();
            Map<String, List<GitFileChange>> historyForRelease = result.get(release);

            for (Map.Entry<String, List<GitFileChange>> entry : fullHistory.entrySet()){
                String classPath = entry.getKey();
                List<GitFileChange> allChanges = entry.getValue();

                List<GitFileChange> filteredChanges = allChanges.stream()
                        .filter(c -> !c.getCommit().getCommitDate().isAfter(releaseDate))
                        .toList();
                if (!filteredChanges.isEmpty()){
                    historyForRelease.put(classPath, filteredChanges);
                }
            }
        }
        return result;
    }

    private static Map<String, List<GitFileChange>> buildFullHistory(Commit lastCommit) throws IOException, InterruptedException{
        ProcessBuilder processBuilder = new ProcessBuilder(
                GIT,
                "log",
                lastCommit.getHash(),
                "--numstat",
                "--format=%H|%an|%ad",
                DATE_OPTION + ISO_STRICT_FORMAT
        );

        processBuilder.directory(new File(LOCAL_REPO_PATH));
        Process process = processBuilder.start();

        Map<String, List<GitFileChange>> fullHistory = new LinkedHashMap<>();

        try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(process.getInputStream()))){
            String line;
            String currentHash = null;
            String currenAuthor = null;
            LocalDateTime currentDate = null;

            while ((line = bufferedReader.readLine())!=null){
                line = line.trim();
                if(line.isEmpty()) continue;

                if (line.contains("|")){
                    String[] parts = line.split("\\|", 3);
                    if (parts.length < 3) continue;
                    currentHash = parts[0].trim();
                    currenAuthor = parts[1].trim();
                    try{
                        currentDate = OffsetDateTime.parse(parts[2].trim()).toLocalDateTime();
                    }catch (Exception _){
                        currentDate = null;
                    }
                    continue;
                }

                FileChangeEntry entry = parseFileChange(line, currentHash, currenAuthor, currentDate);
                if (entry != null)
                    fullHistory.computeIfAbsent(entry.filePath(), k -> new ArrayList<>()).add(entry.change());
            }
        }
        process.waitFor();
        return fullHistory;
    }

    private static FileChangeEntry parseFileChange(String line, String hash, String author, LocalDateTime date){
        if (date == null || !line.matches("\\d+\\s+\\d+\\s+.*")) return null;
        String[] parts = line.split("\\s+", 3);
        String filePath = parts[2].trim();
        if (!filePath.endsWith(JAVA_EXTENSION)) return null;

        GitFileChange change = new GitFileChange(
                new Commit(hash, author, date, null),
                parse(parts[0]),
                parse(parts[1])
        );
        return new FileChangeEntry(filePath, change);
    }

    private record FileChangeEntry(String filePath, GitFileChange change) {}




    public static Map<ProjectRelease, Map<String, Integer>> getAllLocForEachRelease(Map<ProjectRelease, Commit> releaseCommitMap) throws IOException, InterruptedException{
        Map<ProjectRelease, Map<String, Integer>> result = new LinkedHashMap<>();

        List<ProjectRelease> sortedReleases = releaseCommitMap.keySet().stream()
                .sorted(Comparator.comparing(ProjectRelease :: getReleaseDate))
                .toList();

        Map<ProjectRelease, Map<String, String>> releasesBlobToFile = new LinkedHashMap<>();
        Set<String> allBlobs = new LinkedHashSet<>();

        for (ProjectRelease release : sortedReleases){
            Commit commit = releaseCommitMap.get(release);
            Map<String, String> blobToFile = new LinkedHashMap<>();

            ProcessBuilder processBuilder = new ProcessBuilder(
                    GIT,
                    "ls-tree",
                    "-r",
                    commit.getHash()
            );

            processBuilder.directory(new File(LOCAL_REPO_PATH));
            Process process = processBuilder.start();

            try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(process.getInputStream()))){

                String line;
                while ((line = bufferedReader.readLine())!=null){
                    line = line.trim();
                    if (!line.endsWith(JAVA_EXTENSION)) continue;

                    String[] parts = line.split("\\s+", 4);
                    if (parts.length < 4) continue;

                    String blobHash = parts[2].trim();
                    String filePath = parts[3].trim();
                    blobToFile.put(blobHash, filePath);
                    allBlobs.add(blobHash);
                }
            }

            process.waitFor();
            releasesBlobToFile.put(release, blobToFile);
        }



        Map<String, Integer> blobToLoc = new HashMap<>();

        if (!allBlobs.isEmpty()) {
            ProcessBuilder processBuilder2 = new ProcessBuilder(
                    GIT,
                    "cat-file",
                    "--batch"
            );

            processBuilder2.directory(new File(LOCAL_REPO_PATH));
            Process process2 = processBuilder2.start();

            Thread writerThread = new Thread(()->{
                try (PrintWriter stdin = new PrintWriter(process2.getOutputStream())) {
                    for (String blobHash : allBlobs) {
                        stdin.println(blobHash);
                    }

                }
            });

            writerThread.start();

            try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(process2.getInputStream()))) {
                String line;
                String currentBlob = null;
                int remainingBytes = 0;
                int loc = 0;

                while ((line = bufferedReader.readLine()) != null) {
                    if (remainingBytes > 0) {
                        loc++;
                        remainingBytes -= (line.length() + 1);

                        if (remainingBytes <= 0) {
                            blobToLoc.put(currentBlob, loc);
                            currentBlob = null;
                            loc = 0;
                            remainingBytes = 0;
                        }
                    } else {
                        String[] parts = line.trim().split("\\s+");
                        if (parts.length >= 3 && parts[1].equals("blob")) {
                            currentBlob = parts[0].trim();
                            remainingBytes = Integer.parseInt(parts[2].trim());
                            loc = 0;

                            if (remainingBytes == 0) {
                                blobToLoc.put(currentBlob, 0);
                                currentBlob = null;
                            }
                        }
                    }
                }
            }

            writerThread.join();
            process2.waitFor();
        }

        for (ProjectRelease release : sortedReleases){
            Map<String, String> blobToFile = releasesBlobToFile.get(release);
            Map<String, Integer> locMap = new HashMap<>();

            for (Map.Entry<String, String> entry : blobToFile.entrySet()){
                Integer loc = blobToLoc.get(entry.getKey());
                if (loc != null){
                    locMap.put(entry.getValue(), loc);
                }
            }

            result.put(release, locMap);
        }

        return result;
    }

    public static Map<String, List<String>> getAllBugFixCommits(List<TicketBugRecord> tickets) throws IOException, InterruptedException{
        Map<String, List<String>> messageToFiles = new HashMap<>();

        String grepPattern = tickets.stream()
                .map(TicketBugRecord::getKey)
                .collect(Collectors.joining("\\|"));

        if (grepPattern.isEmpty()) return messageToFiles;

        ProcessBuilder processBuilder = new ProcessBuilder(
                GIT,
                "log",
                "--all",
                "--name-only",
                "--format=%H|%s",
                "--grep=" + grepPattern
        );

        processBuilder.directory(new File(LOCAL_REPO_PATH));
        Process process = processBuilder.start();

        Map<String, String> hashToMessage = new HashMap<>();
        String currentHash = null;

        try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(process.getInputStream()))){
            String line;
            while ((line = bufferedReader.readLine())!=null){
                line = line.trim();
                if (line.isEmpty()) continue;

                if (line.matches("[0-9a-f]{40}\\|.*")){
                    String[] parts = line.split("\\|",2);
                    currentHash = parts[0].trim();
                    String message = parts[1].trim();
                    hashToMessage.put(currentHash, message);
                    messageToFiles.computeIfAbsent(message, k->new ArrayList<>());
                }else if (currentHash != null && line.endsWith(JAVA_EXTENSION)){
                    String message = hashToMessage.get(currentHash);
                    if (message != null){
                        messageToFiles.get(message).add(line);
                    }
                }
            }
        }
        process.waitFor();

        return messageToFiles;
    }


    private static int parse(String s){
        try{
            return Integer.parseInt(s);
        }catch (Exception _){
            return 0;
        }
    }


}