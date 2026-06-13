package models;

import java.time.LocalDateTime;

public class TicketBugRecord {

    private String id;
    private String key;
    private String projectKey;
    private LocalDateTime creationDate;
    private LocalDateTime fixedDate;
    private String injectedVersion;
    private String fixVersion;
    private int computedIVIndex = -1;
    private int computedFVIndex = -1;

    public TicketBugRecord(String id,
                           String key,
                           String projectKey,
                           LocalDateTime creationDate,
                           LocalDateTime fixedDate,
                           String injectedVersion,
                           String fixVersion){
        this.id = id;
        this.key = key;
        this.projectKey = projectKey;
        this.creationDate = creationDate;
        this.fixedDate = fixedDate;
        this.injectedVersion = injectedVersion;
        this.fixVersion = fixVersion;
    }

    public String getInjectedVersion() {
        return injectedVersion;
    }

    public void setInjectedVersion(String injectedVersion) {
        this.injectedVersion = injectedVersion;
    }

    public String getFixVersion() {
        return fixVersion;
    }

    public void setFixVersion(String fixVersion) {
        this.fixVersion = fixVersion;
    }

    public int getComputedIVIndex() {
        return computedIVIndex;
    }

    public void setComputedIVIndex(int computedIVIndex) {
        this.computedIVIndex = computedIVIndex;
    }

    public int getComputedFVIndex() {
        return computedFVIndex;
    }

    public void setComputedFVIndex(int computedFVIndex) {
        this.computedFVIndex = computedFVIndex;
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
