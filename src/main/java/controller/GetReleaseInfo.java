package controller;

import client.JsonClient;
import models.ProjectRelease;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class GetReleaseInfo {

    private static final String projName = "SYNCOPE";

    public static List<ProjectRelease> run() {
        List<ProjectRelease> releases = new ArrayList<>();
        String url = "https://issues.apache.org/jira/rest/api/2/project/" + projName;
        try {
            JSONObject jsonObject = JsonClient.readJSONObjectFromUrl(url);
            JSONArray versions = jsonObject.getJSONArray("versions");
            for (int i = 0; i < versions.length(); i++) {
                JSONObject version = versions.getJSONObject(i);
                if (version.has("releaseDate") && version.has("name") && version.has("id")) {
                    String name = version.getString("name");
                    String id = version.getString("id");
                    String releaseDateStr = version.getString("releaseDate");
                    addRelease(releases, releaseDateStr, name, id);
                }
            }

            // Sort releases by date
            Collections.sort(releases, Comparator.comparing(ProjectRelease::getReleaseDate));

        } catch (Exception e) {
            e.printStackTrace();
        }
        
        writeCsv(releases);
        
        return releases;
    }

    private static void addRelease(List<ProjectRelease> releases, String strDate, String name, String id) {
        LocalDate date = LocalDate.parse(strDate);
        LocalDateTime dateTime = date.atStartOfDay();
        releases.add(new ProjectRelease(id, name, dateTime));
    }
    
    private static void writeCsv(List<ProjectRelease> releases) {
        String fileName = projName + "_Releases.csv";
        try (PrintWriter writer = new PrintWriter(new File(fileName))) {
            StringBuilder sb = new StringBuilder();
            sb.append("Index,ID,Name,ReleaseDate\n");
            for (int i = 0; i < releases.size(); i++) {
                ProjectRelease release = releases.get(i);
                sb.append(i + 1).append(",")
                  .append(release.getId()).append(",")
                  .append(release.getName()).append(",")
                  .append(release.getReleaseDate()).append("\n");
            }
            writer.write(sb.toString());
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }
}
