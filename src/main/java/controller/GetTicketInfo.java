package controller;

import client.JsonClient;
import models.TicketBugRecord;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import static client.JsonClient.readJSONObjectFromUrl;

public class GetTicketInfo {

    private static final String projName = "SYNCOPE";

    private GetTicketInfo(){}

    public static List<TicketBugRecord> run() {
        List<TicketBugRecord> tickets = new ArrayList<>();
        int j = 0;
        int i = 0;
        int total = 1;
        
        // JQL query for Syncope defects that are fixed and closed/resolved
        String jql = "PROJECT=%22" + projName + "%22" +
                "%20AND%20issuetype=%22Bug%22" +
                "%20AND%20(status=%22closed%22%20OR%20status=%22resolved%22)" +
                "%20AND%20resolution=%22Fixed%22";
        
        do {
            int startAt = i * 1000;
            String url = "https://issues.apache.org/jira/rest/api/2/search?jql=" + jql +
                    "&startAt=" + startAt +
                    "&maxResults=1000"+
                    "&fields=key,resolutiondate,created,versions,fixVersions";
            
            try {
                JSONObject json = JsonClient.readJSONObjectFromUrl(url);
                JSONArray issues = json.getJSONArray("issues");
                total = json.getInt("total");
                for (int k = 0; k < issues.length(); k++) {
                    JSONObject issue = issues.getJSONObject(k);
                    String key = issue.getString("key");
                    String id = issue.getString("id");
                    JSONObject fields = issue.getJSONObject("fields");
                    
                    String createdStr = fields.getString("created");
                    String resolutionDateStr = fields.getString("resolutiondate");
                    LocalDateTime created = LocalDateTime.parse(createdStr.substring(0, 19), DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"));
                    LocalDateTime resolutionDate = LocalDateTime.parse(resolutionDateStr.substring(0, 19), DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"));

                    String fixVersion = null;
                    JSONArray fixVersions = fields.getJSONArray("fixVersions");
                    if(!fixVersions.isEmpty()){
                        fixVersion = fixVersions.getJSONObject(0).getString("name");
                    }

                    String injectedVersion = null;
                    JSONArray affectedVersions = fields.getJSONArray("versions");
                    if(!affectedVersions.isEmpty()){
                        injectedVersion = getEarliestVersion(affectedVersions);
                    }

                    if (fixVersion == null){
                        j++;
                        continue;
                    }

                    tickets.add(new TicketBugRecord(id, key, projName, created, resolutionDate,injectedVersion,fixVersion));
                    j++;
                }
                i++;
            } catch (Exception e) {
                e.printStackTrace();
                break;
            }
        } while (j < total);
        
        writeCsv(tickets);
        
        return tickets;
    }

    private static String getEarliestVersion(JSONArray versions) {
        String earliest = null;
        for (int i=0 ; i<versions.length(); i++){
            try {
                String name = versions.getJSONObject(i).getString("name");
                if(earliest == null || name.compareTo(earliest) < 0){
                    earliest = name;
                }
            }catch (Exception _){}
        }
        return earliest;
    }

    private static void writeCsv(List<TicketBugRecord> tickets) {
        String fileName = projName + "_Tickets.csv";
        try (PrintWriter writer = new PrintWriter(new File(fileName))) {
            StringBuilder sb = new StringBuilder();
            sb.append("ID,Key,Project,CreationDate,FixedDate,InjectedVersion,FixVersion\n");
            for (TicketBugRecord ticket : tickets) {
                sb.append(ticket.getId()).append(",")
                        .append(ticket.getKey()).append(",")
                        .append(ticket.getProjectKey()).append(",")
                        .append(ticket.getCreationDate()).append(",")
                        .append(ticket.getFixedDate()).append(",")
                        .append(ticket.getInjectedVersion() != null ? ticket.getInjectedVersion() : "").append(",")
                        .append(ticket.getFixVersion() != null ? ticket.getFixVersion() : "").append(";").append("\n");
            }
            writer.write(sb.toString());
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }
}
