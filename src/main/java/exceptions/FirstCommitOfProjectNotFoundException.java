package exceptions;

public class FirstCommitOfProjectNotFoundException extends Exception{
    public FirstCommitOfProjectNotFoundException(){
        super("First commit of project not found");
    }
}
