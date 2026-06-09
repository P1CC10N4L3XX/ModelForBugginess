package client;

import exceptions.CommitOfReleaseNotFoundException;
import exceptions.FirstCommitOfProjectNotFoundException;
import models.Commit;
import models.GitFileChange;
import models.ProjectRelease;
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

    public static List<GitFileChange> getFileHistory(String classPath, Commit commitPrevRelease, Commit commitActualRelease) throws IOException, InterruptedException {

        ProcessBuilder processBuilder = new ProcessBuilder(
                "git",
                "log",
                commitPrevRelease.getHash()+".."+commitActualRelease.getHash(),
                "--numstat",
                "--format=%H|%an|%ad",
                "--date="+isoStrictFormat,
                "--",
                classPath
        );

        processBuilder.directory(new File(localRepoPath));

        Process process = processBuilder.start();

        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(process.getInputStream()));

        List<GitFileChange> changes = new ArrayList<>();

        String line;

        String currentCommit = null;
        String currentAuthor = null;
        LocalDateTime currentDate = null;

        while((line=bufferedReader.readLine()) != null){
            line = line.trim();

            if(line.contains("|")){
                String[] parts = line.split("\\|");
                currentCommit = parts[0];
                currentAuthor = parts[1];
                currentDate = OffsetDateTime.parse(parts[2]).toLocalDateTime();
            }else if(line.matches("\\d+\\s+\\d+\\s+.*")){
                String[] parts = line.split("\\s+");

                GitFileChange change = new GitFileChange(
                        new Commit(currentCommit,currentAuthor,currentDate,null),
                        parse(parts[0]),
                        parse(parts[1])

                );

                changes.add(change);
            }
        }

        process.waitFor();

        return changes;


    }

    private static int parse(String s){
        try{
            return Integer.parseInt(s);
        }catch (Exception _){
            return 0;
        }
    }

}
