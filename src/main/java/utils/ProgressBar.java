package utils;

import org.slf4j.Logger;

public class ProgressBar {
    private ProgressBar(){}

    public static void printProgress(int current, int total, Logger logger){
        if (!logger.isInfoEnabled()){
            return;
        }

        int percent = (int) ((current * 100.0) / total);
        int barLength = 30;
        int filled = (int) (barLength * percent / 100.0);

        StringBuilder bar = new StringBuilder();

        bar.append("\r[");
        for(int i=0; i<barLength; i++){
            bar.append(i < filled ? '■' : ' ');
        }
        bar.append("] ")
                .append(percent).append("% (")
                .append(current).append("/")
                .append(total).append(")");

        logger.info(bar.toString());
    }
}
