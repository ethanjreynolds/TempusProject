package com.hansenjc.tempus.data;

import java.sql.ResultSet;
import java.sql.SQLException;

public class MapName {
    public int id;
    public String name;

    public MapName(int id, String name) {
        this.id = id;
        this.name = name;
    }

    public MapName(ResultSet rs) throws SQLException {
        this.id = rs.getInt("map_id");
        this.name = rs.getString("map_name");
    }
}
