package com.hansenjc.tempus.data;

import org.springframework.lang.Nullable;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;

public record ZoneRunWithZone(@Nullable String map_name, ZoneId zone_id, long player_id, String class_, int server_id, Timestamp time_submitted, double duration, long rank, String group, int tier, int rating, double points) {
    public ZoneRunWithZone(ResultSet rs) throws SQLException {
        this(rs.getString("map_name"), new ZoneId(rs), rs.getLong("player_id"),
                rs.getString("class"), rs.getInt("server_id"),
                rs.getTimestamp("time_submitted"), rs.getDouble("duration"),
                rs.getLong("rank"), rs.getString("group"),
                rs.getInt("tier"), rs.getInt("rating"),
                rs.getDouble("points"));
    }
}
