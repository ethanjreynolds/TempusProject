package com.hansenjc.tempus.data;

import java.util.List;

public class ZoneInfoEx extends ZoneInfo {
    public List<String> authors;
    public ZoneRuns zoneRuns;

    public ZoneInfoEx(ZoneInfo zone_info, List<String> authors, ZoneRuns zoneRuns) {
        super(zone_info.soldier_info, zone_info.demoman_info);
        this.authors = authors;
        this.zoneRuns = zoneRuns;
    }

    public ZoneInfoEx(ZoneId zone_id, ClassInfo soldier_info, ClassInfo demoman_info, List<String> authors, ZoneRuns zone_runs) {
        super(soldier_info, demoman_info);
        this.authors = authors;
        this.zoneRuns = zone_runs;
    }
}
