package controller;

import client.GitManager;
import models.GitFileChange;
import models.ProjectRelease;
import models.TicketBugRecord;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SZZ {

    public static Map<Integer, List<String>> computeBuggyClasses (List<ProjectRelease> releases, List<TicketBugRecord> tickets) throws Exception {
        double p = Proportion.computeP(tickets, releases);
        System.out.println("Proportion P = " + p);

        Proportion.assignMissingIV(releases,tickets, p);

        Map<Integer, List<String>> buggyMap = new HashMap<>();

        for (TicketBugRecord ticket : tickets){
            int iv = getIV(releases, ticket);
            int fv = getFV(releases,ticket);

            if (iv < 0 || fv < 0 || iv >= fv) continue;

            List<String> buggyClasses = GitManager.getBuggyClassesFromFix(ticket);

            for (int r = iv; r<fv; r++){
                buggyMap.computeIfAbsent(r, k -> new ArrayList<>()).addAll(buggyClasses);
            }
        }

        return buggyMap;
    }

    private static int getFV(List<ProjectRelease> releases, TicketBugRecord ticket) {
        if (ticket.getFixVersion() != null){
            Integer idx = Proportion.getReleaseIndex(releases, ticket.getFixVersion());
            return idx != null ? idx : ticket.getComputedFVIndex();
        }
        return ticket.getComputedFVIndex();
    }

    private static int getIV(List<ProjectRelease> releases,TicketBugRecord ticket){
        if(ticket.getInjectedVersion() != null){
            Integer idx = Proportion.getReleaseIndex(releases, ticket.getInjectedVersion());
            return idx != null ? idx : ticket.getComputedIVIndex();
        }
        return ticket.getComputedIVIndex();
    }
}
