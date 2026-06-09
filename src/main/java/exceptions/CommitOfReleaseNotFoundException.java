package exceptions;

public class CommitOfReleaseNotFoundException extends Exception{
    public CommitOfReleaseNotFoundException(){
        super("Commit of release not found exception");
    }
}
