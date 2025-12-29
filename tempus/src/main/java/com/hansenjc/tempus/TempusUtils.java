package com.hansenjc.tempus;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TempusUtils {

    public static double parseDuration(String duration) throws IllegalArgumentException {
        double time = 0;

        Matcher matcher = Pattern.compile("(\\d?\\d):([0-5]\\d):([0-5]\\d)(\\.\\d{1,3})?").matcher(duration);
        if (matcher.matches()) {
            time += Double.parseDouble(matcher.group(1)) * 3600;
            time += Double.parseDouble(matcher.group(2)) * 60;
            time += Double.parseDouble(matcher.group(3));
            String sss = matcher.group(4);
            if (sss != null) {
                String sub = sss.substring(1);
                double denominator = Math.pow(10, sub.length());
                time += Double.parseDouble(sub) / denominator;
            }
            return time;
        }

        matcher = Pattern.compile("(\\d?\\d):([0-5]\\d)(\\.\\d{1,3})?").matcher(duration);
        if (matcher.matches()) {
            time += Double.parseDouble(matcher.group(1)) * 60;
            time += Double.parseDouble(matcher.group(2));
            String sss = matcher.group(3);
            if (sss != null) {
                String sub = sss.substring(1);
                double denominator = Math.pow(10, sub.length());
                time += Double.parseDouble(sub) / denominator;
            }
            return time;
        }

        matcher = Pattern.compile("([0-5]?\\d)(\\.\\d{1,3})?").matcher(duration);
        if (matcher.matches()) {
            time += Double.parseDouble(matcher.group(1));
            String sss = matcher.group(2);
            if (sss != null) {
                String sub = sss.substring(1);
                double denominator = Math.pow(10, sub.length());
                time += Double.parseDouble(sub) / denominator;
            }
            return time;
        }

        throw new IllegalArgumentException("Invalid duration: " + duration + "!");
    }

    public static String formatDuration(double duration) {
        long HH = (long) duration / 3600;
        long MM = (long) duration % 3600 / 60;
        long SS = (long) duration % 60;
        long mmm = (long) (duration * 1000) % 1000;
        if (duration > 3600) return String.format("%d:%02d:%02d.%03d", HH, MM, SS, mmm);
        if (duration > 60) return String.format("%02d:%02d.%03d", MM, SS, mmm);
        if (duration > 0) return String.format("%02d.%03d", SS, mmm);
        return String.format("0.%03d", mmm);
    }

}
