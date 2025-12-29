package com.hansenjc.tempus.data;

import java.sql.ResultSet;
import java.sql.SQLException;

public record Player(long id, String steam_id, String name) {
    public Player(ResultSet rs) throws SQLException {
        this(rs.getLong("player_id"), rs.getString("steam_id"), rs.getString("name"));
    }
}