package controller;

import models.ProjectRelease;
import models.TicketBugRecord;

import java.util.ArrayList;
import java.util.List;

public class Proportion {
    private Proportion(){}

    public static double computeP(List<TicketBugRecord> tickets, List<ProjectRelease> releases){
        List<Double> proportions = new ArrayList<>();
        for (TicketBugRecord ticket : tickets){
            Integer iv = getReleaseIndex(releases,ticket.getInjectedVersion());
            Integer fv = getReleaseIndex(releases, ticket.getFixVersion());
            Integer ov = getOpeningVersion(releases, ticket);

            if(iv == null || fv == null || ov == null) continue;
            if(fv.equals(ov)) continue;
            if(iv >= fv) continue;

            double p = (double) (fv - iv) / (fv - ov);
            if (p > 0) proportions.add(p);
        }

        if (proportions.isEmpty()) return 1.0;

        return (proportions.stream().mapToDouble(Double :: doubleValue).average().orElse(1.0));

    }

    public static void assignMissingIV(List<ProjectRelease> releases,List<TicketBugRecord> tickets, double p){
        for (TicketBugRecord ticket : tickets){
            if(ticket.getInjectedVersion() != null) continue;

            Integer fv = getReleaseIndex(releases,ticket.getFixVersion());
            Integer ov = getOpeningVersion(releases,ticket);

            if (fv == null || ov == null) continue;

            int ivIndex = (int) Math.max(0, Math.floor(fv - p * (fv - ov)));

            if (ivIndex < releases.size()){
                ticket.setComputedIVIndex(ivIndex);
            }else {
                ticket.setComputedIVIndex(0);
            }

            ticket.setComputedFVIndex(fv);
        }
    }

    private static Integer getOpeningVersion(List<ProjectRelease> releases, TicketBugRecord ticket) {
        for (int i=0 ; i<releases.size(); i++){
            if(!releases.get(i).getReleaseDate().isAfter(ticket.getCreationDate())) return i;
        }
        return 0;
    }

    public static Integer getReleaseIndex(List<ProjectRelease> releases, String versionName){
        if(versionName == null) return null;
        for(int i=0 ; i<releases.size(); i++){
            if(releases.get(i).getName().equals(versionName)){
                return i;
            }
        }
        return null;
    }
}
