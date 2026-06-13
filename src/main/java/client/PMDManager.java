package client;

import net.sourceforge.pmd.PMDConfiguration;
import net.sourceforge.pmd.PmdAnalysis;
import net.sourceforge.pmd.lang.LanguageRegistry;
import net.sourceforge.pmd.reporting.Report;
import net.sourceforge.pmd.reporting.RuleViolation;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;



public class PMDManager {
    private PMDManager(){}

    public static Map<String, Integer> getAllSmells(Map<String, String> contentMap) throws IOException {
        Map<String, Integer> smellsMap = new HashMap<>();

        Path tempDir = Files.createTempDirectory("pmd_analysis_");

        Map<Path, String> tempToOriginal = new HashMap<>();
        try {
            for (Map.Entry<String, String> entry : contentMap.entrySet()){
                String classPath = entry.getKey();
                String content = entry.getValue();

                if (content == null || content.isEmpty()) continue;

                Path tempFile = tempDir.resolve(classPath);
                Files.createDirectories(tempFile.getParent());
                try (FileWriter fileWriter = new FileWriter(tempFile.toFile())){
                    fileWriter.write(content);
                }
                tempToOriginal.put(tempFile, classPath);
            }

            PMDConfiguration config = new PMDConfiguration();
            config.setDefaultLanguageVersion(
                    LanguageRegistry.PMD.getLanguageById("java").getDefaultVersion()
            );

            config.addRuleSet("category/java/design.xml");
            config.addRuleSet("category/java/bestpractices.xml");
            config.addRuleSet("category/java/errorprone.xml");

            config.addInputPath(tempDir);

            for (String classPath : contentMap.keySet()){
                smellsMap.put(classPath, 0);
            }

            try (PmdAnalysis pmdAnalysis = PmdAnalysis.create(config)){
                Report report = pmdAnalysis.performAnalysisAndCollectReport();

                for (RuleViolation violation : report.getViolations()){
                    String violationPath = violation.getFileId().getAbsolutePath();

                    for (Map.Entry<Path, String> entry : tempToOriginal.entrySet()){
                        if (violationPath.equals(entry.getKey().toAbsolutePath().toString())){
                            String originalPath = entry.getValue();
                            smellsMap.merge(originalPath, 1, Integer::sum);
                            break;
                        }
                    }
                }
            }
        }finally {
            deleteDirectory(tempDir);
        }

        return smellsMap;


    }

    private static void deleteDirectory(Path dir){
        try {
            Files.walk(dir)
                    .sorted((a,b) -> b.compareTo(a))
                    .forEach(path -> {
                        try { Files.deleteIfExists(path); }
                        catch (IOException _){}
                    });
        } catch (IOException _){}
    }
}
