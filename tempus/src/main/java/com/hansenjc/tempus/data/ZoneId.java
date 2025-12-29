package com.hansenjc.tempus.data;

import com.hansenjc.tempus.enums.ZoneType;

import java.sql.ResultSet;
import java.sql.SQLException;

public record ZoneId(int map_id, ZoneType zone_type, int zone_index) {
    public ZoneId(String map_id, String zone_type, String zone_index) throws IllegalArgumentException {
        this(Integer.parseInt(map_id), ZoneType.valueOf(zone_type), Integer.parseInt(zone_index));
    }

    public ZoneId(ResultSet rs) throws SQLException {
        this(rs.getInt("map_id"), ZoneType.valueOf(rs.getString("zone_type")), rs.getInt("zone_index"));
    }
}

