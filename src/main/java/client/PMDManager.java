package client;


import net.sourceforge.pmd.PMDConfiguration;
import net.sourceforge.pmd.PmdAnalysis;
import net.sourceforge.pmd.lang.LanguageRegistry;
import net.sourceforge.pmd.reporting.Report;
import net.sourceforge.pmd.reporting.RuleViolation;
import utils.SecureTempDir;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class PMDManager {
    private PMDManager(){}

    public static Map<String, Integer> getAllSmells(Map<String, String> contentMap) throws IOException {
        return runPmdAndCollect(
                contentMap,
                new String[]{
                        "category/java/design.xml",
                        "category/java/bestpractices.xml",
                        "category/java/errorprone.xml"
                },
                _ -> 1
        );
    }

    public static Map<String, Integer> getCyclomaticComplexityPerFile(Map<String, String> contentMap) throws IOException{
        return runPmdAndCollect(
                contentMap,
                new String[]{"category/java/design.xml/CyclomaticComplexity"},
                violation -> {
                    String msg = violation.getDescription();
                    try {
                        String[] tokens = msg.replaceAll("[^0-9 ]", "").trim().split("\\s+");
                        return Integer.parseInt(tokens[tokens.length - 1]);
                    }catch (Exception _){
                        return 1;
                    }
                }
        );
    }

    public static Map<String, Integer> getPublicMethodsCountPerFile(Map<String, String> contentMap){
        Map<String, Integer> result = new HashMap<>();

        for (Map.Entry<String, String> entry : contentMap.entrySet()){
            String classPath = entry.getKey();
            String content = entry.getValue();

            if (content == null || content.isEmpty()){
                result.put(classPath, 0);
                continue;
            }

            int count = countPublicMethods(content);
            result.put(classPath, count);
        }
        return result;
    }

    @FunctionalInterface
    private interface ViolationValueExtractor{
        int extract(RuleViolation violation);
    }

    private static Map<String, Integer> runPmdAndCollect(Map<String, String> contentMap, String[] ruleSets, ViolationValueExtractor extractor) throws IOException {
        Map<String, Integer> resultMap = new HashMap<>();
        Path tempDir = SecureTempDir.createSecureTempDirectory("pmd_analysis_");
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

            for (String classPath : contentMap.keySet()){
                resultMap.put(classPath, 0);
            }
            PMDConfiguration config = new PMDConfiguration();
            config.setDefaultLanguageVersion(
                    LanguageRegistry.PMD.getLanguageById("java").getDefaultVersion()
            );
            for (String ruleSet : ruleSets){
                config.addRuleSet(ruleSet);
            }

            config.addInputPath(tempDir);

            try (PmdAnalysis pmdAnalysis = PmdAnalysis.create(config)){
                Report report = pmdAnalysis.performAnalysisAndCollectReport();

                for (RuleViolation violation : report.getViolations()){
                    String violationPath = violation.getFileId().getAbsolutePath();

                    for (Map.Entry<Path, String> e : tempToOriginal.entrySet()){
                        if(violationPath.equals(e.getKey().toAbsolutePath().toString())){
                            String originalPath = e.getValue();
                            int value = extractor.extract(violation);
                            resultMap.merge(originalPath, value, Integer::sum);
                            break;
                        }
                    }
                }
            }
        }finally {
            deleteDirectory(tempDir);
        }

        return resultMap;
    }

    private static int countPublicMethods(String source){
        String cleaned = source
                .replaceAll("//[^\n]*", "")
                .replaceAll("/\\*.*?\\*/", " ");
        Pattern pattern = Pattern.compile(
                "public\\s+(?!class\\b|interface\\b|enum\\b|@interface\\b)" +
                        "(?:(?:static|final|synchronized|abstract|default|native)\\s+)*" +
                        "[\\w<>\\[\\]]+\\s+\\w+\\s*\\("
        );

        Matcher matcher = pattern.matcher(cleaned);
        int count = 0;
        while (matcher.find()) count++;
        return count;
    }

    private static void deleteDirectory(Path dir){
        try {
            Files.walk(dir)
                    .sorted(Comparator.reverseOrder())
                    .forEach(path -> {
                        try { Files.deleteIfExists(path); }
                        catch (IOException _){
                            //ignore the exception
                        }
                    });
        } catch (IOException _){
            //ignore the exception
        }
    }
}