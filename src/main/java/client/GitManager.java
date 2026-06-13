package client;

import exceptions.CommitOfReleaseNotFoundException;
import exceptions.FirstCommitOfProjectNotFoundException;
import models.Commit;
import models.GitFileChange;
import models.ProjectRelease;
import models.TicketBugRecord;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import utils.ConfigManager;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.*;

import static utils.CommandRunner.runCommand;

public class GitManager {

    private GitManager(){}

    private static final String localRepoPath = ConfigManager.getInstance().getProperty("localRepoPath");
    private static final String isoStrictFormat = "iso-strict";

    public static void cloneRepo() throws IOException, InterruptedException{
        String githubRepoUrl = ConfigManager.getInstance().getProperty("GithubRepoUrl");
        File repoDir = new File(localRepoPath);
        if(!repoDir.exists()){
            System.out.println("Cloning repository...");
            runCommand(".", "git", "clone", githubRepoUrl, localRepoPath);
        }
    }

    public static Map<String, String> getAllFileContentAtCommit(Commit commit) throws IOException, InterruptedException{

        Map<String, String> contentMap = new HashMap<>();



        ProcessBuilder processBuilder = new ProcessBuilder(
                "git",
                "archive",
                commit.getHash(),
                "--format=tar"
        );

        processBuilder.directory(new File(localRepoPath));
        Process process = processBuilder.start();

        try (TarArchiveInputStream tarStream = new TarArchiveInputStream(process.getInputStream())){
            TarArchiveEntry entry;
            while((entry = tarStream.getNextEntry()) != null){
                String name = entry.getName();
                if(!name.endsWith(".java")) continue;
                byte[] bytes = tarStream.readAllBytes();
                String content = new String(bytes);
                contentMap.put(name, content);
            }
        }

        process.waitFor();
        return contentMap;


    }

    public static String getFileContentAtCommit(String classPath, Commit commit) throws IOException, InterruptedException{
        ProcessBuilder processBuilder = new ProcessBuilder(
          "git",
          "show",
          commit.getHash() + ":" + classPath
        );

        processBuilder.directory(new File(localRepoPath));

        Process process = processBuilder.start();

        String content = new String(process.getInputStream().readAllBytes());
        process.waitFor();
        return content;

    }

    public static int getLocAtCommit(String classPath, Commit commit) throws IOException, InterruptedException {

        ProcessBuilder processBuilder = new ProcessBuilder(
                "git",
                "show",
                commit.getHash()+":"+classPath
        );

        processBuilder.directory(new File(localRepoPath));

        Process process = processBuilder.start();

        int loc=0;

        try(BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(process.getInputStream()))){
            while ((bufferedReader.readLine() )!= null){
                loc++;
            }
        }

        process.waitFor();

        return loc;
    }

    public static List<String> getJavaFilesPerCommit(Commit commit) throws IOException, InterruptedException{

        ProcessBuilder pb = new ProcessBuilder(
                "git",
                "ls-tree",
                "-r",
                "--name-only",
                commit.getHash()
        );
        pb.directory(new File(localRepoPath));

        Process process = pb.start();

        List<String> javaFiles = new ArrayList<>();

        try(BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))){
            String line;
            while((line = reader.readLine()) != null){
                if(line.endsWith(".java")){
                    javaFiles.add(line);
                }
            }
        }

        process.waitFor();

        return javaFiles;
    }

    public static Commit getFirstCommitOfProject() throws FirstCommitOfProjectNotFoundException,IOException, InterruptedException{

        ProcessBuilder processBuilder = new ProcessBuilder(
          "git",
          "rev-list",
          "--reverse",
          "--pretty=format:%H|%an|%ad|%s",
          "--date="+isoStrictFormat,
          "HEAD"
        );

        processBuilder.directory(new File(localRepoPath));

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

    public static Commit getLastCommitOfRelease(ProjectRelease release) throws CommitOfReleaseNotFoundException,IOException, InterruptedException {

        String gitDate = release.getReleaseDate().toString().replace("T"," ");

        ProcessBuilder processBuilder = new ProcessBuilder(
                "git",
                "rev-list",
                "-n",
                "1",
                "--before="+gitDate,
                "--pretty=format:%H|%an|%ad|%s",
                "--date="+isoStrictFormat,
                "HEAD"
        );

        processBuilder.directory(new File(localRepoPath));

        Process process = processBuilder.start();

        String output = new String(process.getInputStream().readAllBytes()).trim();

        process.waitFor();

        if(output.isEmpty()){
            throw new CommitOfReleaseNotFoundException();
        }

        String[] parts = output.split("\\|");

        String hash = parts[0].replace("commit", "").trim().split("\\s+")[0];
        String author = parts[1];
        LocalDateTime commitDate = OffsetDateTime.parse(parts[2]).toLocalDateTime();
        String message = parts[3];

        return new Commit(hash,author,commitDate,message);
    }


    public static Map<String, Integer> getAllLocAtCommit(List<String> classPaths,Commit commit) throws IOException, InterruptedException{
        Map<String,Integer> locMap = new HashMap<>();

        for(String classPath: classPaths){
            ProcessBuilder processBuilder = new ProcessBuilder(
                    "git",
                    "show",
                    commit.getHash() + ":" + classPath
            );
            processBuilder.directory(new File(localRepoPath));

            Process process = processBuilder.start();

            long loc = new BufferedReader(new InputStreamReader(process.getInputStream())).lines().count();

            locMap.put(classPath,(int)loc);
        }

        return locMap;
    }

    public static Map<String, List<GitFileChange>> getAllFileHistory(Commit commitPrevRelease, Commit commitActualRelease) throws IOException, InterruptedException{
        Map<String, List<GitFileChange>> historyMap = new HashMap<>();

        ProcessBuilder processBuilder = new ProcessBuilder(
                "git",
                "log",
                commitPrevRelease.getHash() + ".." + commitActualRelease.getHash(),
                "--numstat",
                "--format=%H|%an|%ad",
                "--date="+isoStrictFormat
        );
        processBuilder.directory(new File(localRepoPath));
        Process process = processBuilder.start();

        try(BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(process.getInputStream()))){
            String line;
            String currentHash = null;
            String currenAuthor = null;
            LocalDateTime currentDate = null;

            while ((line = bufferedReader.readLine()) != null){
                line = line.trim();
                if(line.contains("|")){
                    String[] parts = line.split("\\|");
                    currentHash = parts[0];
                    currenAuthor = parts[1];
                    currentDate = OffsetDateTime.parse(parts[2].trim()).toLocalDateTime();
                }else if (line.matches("\\d+\\s+\\d+\\s+.*")){
                    String[] parts = line.split("\\s+",3);
                    String filePath = parts[2].trim();
                    if(!filePath.endsWith(".java")) continue;

                    GitFileChange gitFileChange = new GitFileChange(
                            new Commit(currentHash, currenAuthor, currentDate, null),
                            parse(parts[0]),
                            parse(parts[1])
                    );

                    historyMap.computeIfAbsent(filePath, k -> new ArrayList<>()).add(gitFileChange);
                }
            }
        }
        process.waitFor();
        return historyMap;
    }

    public static List<String> getBuggyClassesFromFix(TicketBugRecord ticket) throws IOException, InterruptedException{

        String ticketKey = ticket.getKey();

        ProcessBuilder processBuilder = new ProcessBuilder(
                "git",
                "log",
                "--all",
                "--format=%H",
                "--grep=" + ticketKey
        );

        processBuilder.directory(new File(localRepoPath));
        Process process = processBuilder.start();

        List<String> buggyClasses = new ArrayList<>();

        try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(process.getInputStream()))){
            String commitHash;
            while ((commitHash = bufferedReader.readLine()) != null){
                commitHash = commitHash.trim();
                if (commitHash.isEmpty()) continue;

                List<String> changedFiles = getChangedJavaFiles(commitHash);
                buggyClasses.addAll(changedFiles);
            }
        }

        return buggyClasses;

    }

    private static List<String> getChangedJavaFiles(String commitHash) throws IOException, InterruptedException{
        ProcessBuilder processBuilder = new ProcessBuilder(
                "git",
                "diff-tree",
                "--no-commit-id",
                "-r",
                "--name-only",
                commitHash
        );

        processBuilder.directory(new File(localRepoPath));
        Process process = processBuilder.start();

        List<String> javaFiles = new ArrayList<>();

        try(BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = bufferedReader.readLine()) != null){
                if (line.endsWith(".java")){
                    javaFiles.add(line.trim());
                }
            }
        }

        process.waitFor();
        return javaFiles;
    }


    private static int parse(String s){
        try{
            return Integer.parseInt(s);
        }catch (Exception _){
            return 0;
        }
    }

}
