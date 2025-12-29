package com.hansenjc.tempus.data;

public class ZoneInfo {
    public ClassInfo soldier_info;
    public ClassInfo demoman_info;

    public ZoneInfo() {
        this.soldier_info = null;
        this.demoman_info = null;
    }

    public ZoneInfo(ClassInfo soldier_info, ClassInfo demoman_info) {
        this.soldier_info = soldier_info;
        this.demoman_info = demoman_info;
    }

    public ZoneInfo(int soldier_tier, int soldier_rating, int soldier_run_count, int demoman_tier, int demoman_rating, int demoman_run_count) {
        this.soldier_info = new ClassInfo(new Tier(soldier_tier), soldier_rating, soldier_run_count);
        this.demoman_info = new ClassInfo(new Tier(demoman_tier), demoman_rating, demoman_run_count);
    }

    public ZoneInfo(Tier soldier_tier, int soldier_rating, int soldier_run_count, Tier demoman_tier, int demoman_rating, int demoman_run_count) {
        this.soldier_info = new ClassInfo(soldier_tier, soldier_rating, soldier_run_count);
        this.demoman_info = new ClassInfo(demoman_tier, demoman_rating, demoman_run_count);
    }

    public record ClassInfo(Tier tier, int rating, int runCount) {}

    @Override
    public String toString() {
        return "S:" + soldier_info.toString() + " D:"+ demoman_info.toString();
    }
}