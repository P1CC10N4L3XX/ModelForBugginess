package models;

import java.time.LocalDateTime;

public class TicketBugRecord {

    private String id;
    private String key;
    private String projectKey;
    private LocalDateTime creationDate;
    private LocalDateTime fixedDate;

    public TicketBugRecord(String id,
                           String key,
                           String projectKey,
                           LocalDateTime creationDate,
                           LocalDateTime fixedDate){
        this.id = id;
        this.key = key;
        this.projectKey = projectKey;
        this.creationDate = creationDate;
        this.fixedDate = fixedDate;
    }

    public String getId() {
        return id;
    }

    public String getKey() {
        return key;
    }

    public String getProjectKey() {
        return projectKey;
    }

    public LocalDateTime getCreationDate() {
        return creationDate;
    }

    public LocalDateTime getFixedDate() {
        return fixedDate;
    }
}
