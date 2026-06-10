package client;

import net.sourceforge.pmd.PMDConfiguration;
import net.sourceforge.pmd.PmdAnalysis;
import net.sourceforge.pmd.lang.LanguageRegistry;
import net.sourceforge.pmd.reporting.Report;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class PMDManager {
    private PMDManager(){}

    public static int getNSmells(String classPath, String classContent) throws IOException {
        Path tempFile = Files.createTempFile("pmd_", ".java");
        try (FileWriter fileWriter = new FileWriter(tempFile.toFile())){
            fileWriter.write(classContent);
        }

        PMDConfiguration config = new PMDConfiguration();

        config.setDefaultLanguageVersion(
                LanguageRegistry.PMD.getLanguageById("java").getDefaultVersion()
        );

        config.addRuleSet("category/java/design.xml");
        config.addRuleSet("category/java/bestpractices.xml");
        config.addRuleSet("category/java/errorprone.xml");

        config.addInputPath(tempFile);

        int smellsCount = 0;
        try(PmdAnalysis pmdAnalysis = PmdAnalysis.create(config)){
            Report report = pmdAnalysis.performAnalysisAndCollectReport();
            smellsCount = report.getViolations().size();
        }

        Files.deleteIfExists(tempFile);

        return smellsCount;
    }
}
