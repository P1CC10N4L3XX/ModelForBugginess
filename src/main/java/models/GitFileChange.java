package models;

public class GitFileChange {
    private Commit commit;
    private int added;
    private int deleted;

    public GitFileChange(Commit commit, int added, int deleted){
        this.commit = commit;
        this.added = added;
        this.deleted = deleted;
    }

    public Commit getCommit() {
        return commit;
    }

    public void setCommit(Commit commit) {
        this.commit = commit;
    }

    public int getAdded() {
        return added;
    }

    public void setAdded(int added) {
        this.added = added;
    }

    public int getDeleted() {
        return deleted;
    }

    public void setDeleted(int deleted) {
        this.deleted = deleted;
    }
}
