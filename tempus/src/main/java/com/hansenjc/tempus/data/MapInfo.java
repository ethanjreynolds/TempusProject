package com.hansenjc.tempus.data;

public class MapInfo extends ZoneInfo {
    public int id;
    public String name;

    public MapInfo(int id, String name, int soldier_tier, int soldier_rating, int soldier_run_count, int demoman_tier, int demoman_rating, int demoman_run_count) {
        super(soldier_tier, soldier_rating, soldier_run_count, demoman_tier, demoman_rating, demoman_run_count);
        this.id = id;
        this.name = name;
    }
}
