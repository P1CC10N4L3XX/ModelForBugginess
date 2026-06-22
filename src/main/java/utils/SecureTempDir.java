package utils;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Set;

public class SecureTempDir {

    private SecureTempDir(){}

    public static Path createSecureTempDirectory(String prefix) throws IOException{
        FileSystem fs = FileSystems.getDefault();

        if (fs.supportedFileAttributeViews().contains("posix")){
            FileAttribute<Set<PosixFilePermission>> attrs = PosixFilePermissions.asFileAttribute(
                    PosixFilePermissions.fromString("rwx------")
            );
            return Files.createTempDirectory(prefix, attrs);
        }

        Path dir = Files.createTempDirectory(prefix);
        if (!dir.toFile().setReadable(true, true) || !dir.toFile().setWritable(true, true) || !dir.toFile().setExecutable(true, true)) return null;

        return dir;
    }
}
