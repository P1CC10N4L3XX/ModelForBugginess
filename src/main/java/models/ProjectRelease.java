package models;

import java.time.LocalDateTime;

public class ProjectRelease {
    private String id;
    private String name;
    private LocalDateTime releaseDate;

    public ProjectRelease(String id, String name, LocalDateTime releaseDate){
        this.id = id;
        this.name = name;
        this.releaseDate = releaseDate;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setReleaseDate(LocalDateTime releaseDate) {
        this.releaseDate = releaseDate;
    }

    public LocalDateTime getReleaseDate() {
        return releaseDate;
    }

    public String getName() {
        return name;
    }
}
