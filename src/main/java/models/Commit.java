package models;

import java.time.LocalDateTime;

public class Commit {
    private String hash;
    private String author;
    private LocalDateTime commitDate;
    private String message;

    public Commit(){}

    public Commit(String hash, String author, LocalDateTime commitDate, String message){
        this.hash = hash;
        this.author = author;
        this.commitDate = commitDate;
        this.message = message;
    }

    public String getHash() {
        return hash;
    }

    public void setHash(String hash) {
        this.hash = hash;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public LocalDateTime getCommitDate() {
        return commitDate;
    }

    public void setCommitDate(LocalDateTime commitDate) {
        this.commitDate = commitDate;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
