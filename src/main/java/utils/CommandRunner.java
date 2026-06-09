package utils;

import java.io.File;
import java.io.IOException;

public class CommandRunner {
    private CommandRunner(){}
    public static int runCommand(String workDir, String... command) throws IOException, InterruptedException{
        ProcessBuilder pb = new ProcessBuilder(command);
        pb.directory(new File(workDir));
        pb.inheritIO();
        Process process = pb.start();
        return process.waitFor();

    }
}
