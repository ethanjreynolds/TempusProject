package com.hansenjc.tempus;

import com.hansenjc.tempus.data.*;
import com.hansenjc.tempus.enums.TFClass;
import com.hansenjc.tempus.enums.ZoneType;
import jakarta.annotation.Nullable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

// for database logic
@Repository
public class TempusRepository {
    private final JdbcTemplate jdbc;

    public TempusRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<Country> getCountries() {
        return jdbc.query("""
                        SELECT *
                        FROM country
                        ORDER BY name""",
                (rs, n) -> new Country(rs.getString("country_code"), rs.getString("name"))
        );
    }

    public List<Server> getServers() {
        return jdbc.query("""
                        SELECT server_id, server_name, country_code
                        FROM server
                        ORDER BY country_code, server_name""",
                (rs, n) -> new Server(rs.getInt("server_id"), rs.getString("server_name"), new CountryCode(rs.getString("country_code")))
        );
    }

    public List<ZoneInfo> getAllMapInfos() {
        return jdbc.query("""
                        SELECT m.map_name, m.map_id, soldier_tier, soldier_rating, soldier_run_count, demoman_tier, demoman_rating, demoman_run_count
                        FROM map m JOIN zone_info zi
                            ON m.map_id = zi.map_id
                        WHERE zone_type = 'map' AND zone_index = 1
                        ORDER BY map_name""",
                (rs, n) -> new MapInfo(
                        rs.getInt("map_id"), rs.getString("map_name"),
                        rs.getInt("soldier_tier"), rs.getInt("soldier_rating"), rs.getInt("soldier_run_count"),
                        rs.getInt("demoman_tier"), rs.getInt("demoman_rating"), rs.getInt("demoman_run_count")
                )
        );
    }

    public String getMapName(int map_id) {
        return jdbc.queryForObject("SELECT map_name FROM map WHERE map_id = ? LIMIT 1", String.class, map_id);
    }

    public List<MapName> getMapNames() {
        return jdbc.query("SELECT * FROM map", (rs, n) -> new MapName(rs));
    }

    public List<ZoneCounts> getAllMapZoneCounts() {
        return jdbc.query("""
                         SELECT m.map_name, m.map_id,
                            (SELECT COALESCE(COUNT(zone_index), 0) FROM zone_info WHERE map_id = m.map_id AND zone_type = 'course') AS courses,
                            (SELECT COALESCE(COUNT(zone_index), 0) FROM zone_info WHERE map_id = m.map_id AND zone_type = 'bonus') AS bonuses
                         FROM map m
                         ORDER BY map_name""",
                (rs, n) -> new ZoneCounts(new MapName(rs), rs.getInt("courses"), rs.getInt("bonuses"))
        );
    }

    public int getMapZoneCount(int map_id, ZoneType zone_type) {
        //noinspection DataFlowIssue
        return jdbc.queryForObject("""
                        SELECT COALESCE(COUNT(zone_index), 0)
                        FROM zone_info WHERE map_id = ? AND zone_type = ?""",
                Integer.class,
                map_id, zone_type.name()
        );
    }

    public ZoneInfo getZoneInfo(ZoneId zone_id) {
        return jdbc.queryForObject("""
                        SELECT *,
                               (SELECT name FROM tier_name WHERE tier = soldier_tier) AS soldier_tier_name,
                               (SELECT name FROM tier_name WHERE tier = demoman_tier) AS demoman_tier_name
                        FROM zone_info
                        WHERE map_id = ? AND zone_type = ? AND zone_index = ?""",
                (rs, n) -> new ZoneInfo(
                        new Tier(rs.getInt("soldier_tier"), rs.getString("soldier_tier_name")), rs.getInt("soldier_rating"), rs.getInt("soldier_run_count"),
                        new Tier(rs.getInt("demoman_tier"), rs.getString("demoman_tier_name")), rs.getInt("demoman_rating"), rs.getInt("demoman_run_count")
                ),
                zone_id.map_id(), zone_id.zone_type().name(), zone_id.zone_index()
        );
    }

    public List<Player> getMapAuthors(int map_id) {
        return jdbc.query(
                "SELECT pi.player_id, pi.steam_id, pi.name " +
                        "FROM map_author ma JOIN player_info pi ON ma.author_id = pi.player_id " +
                        "WHERE map_id = ? " +
                        "ORDER BY pi.name",
                (rs, n) -> new Player(rs),
                map_id
        );
    }

    public List<ZoneRunWithPlayer> getZoneRuns(ZoneId zone_id, TFClass class_, int limit, int offset) {
        return jdbc.query("""
                        WITH runs AS (SELECT player_id, class, time_submitted, duration, rank, zrs.group
                        FROM zone_run_scored zrs
                        WHERE map_id = ? AND zone_type = ? AND zone_index = ? AND class = ?
                        ORDER BY rank
                        LIMIT ?
                        OFFSET ?)
                        SELECT r.*, pi.steam_id, pi.name
                        FROM player_info pi JOIN runs r ON pi.player_id = r.player_id
                        ORDER BY r.rank""",
                (rs, n) -> new ZoneRunWithPlayer(
                        new Player(rs),
                        rs.getString("class"), rs.getTimestamp("time_submitted"), rs.getDouble("duration"), rs.getLong("rank"), rs.getString("group")
                ),
                zone_id.map_id(), zone_id.zone_type().name(), zone_id.zone_index(), class_.name(), offset, limit
        );
    }

    public List<ZoneRunWithZone> getPlayerRuns(long player_id, int limit, int offset, @Nullable TempusApiController.OrderBy[] orders) {
        String order = "points DESC, tier DESC, rating ASC, rank ASC, time_submitted ASC, duration DESC";

        if (orders != null) {
            order = Arrays.stream(orders)
                    .map(o -> {
                        if (!Pattern.compile("[a-zA-Z_]+").matcher(o.column()).matches())
                            throw new IllegalArgumentException("Invalid ORDER BY column: " + o.column() + "!");
                        return o.column() + " " + (o.direction() >= 1 ? "ASC" : "DESC");
                    })
                    .collect(Collectors.joining(", "));
        }

        // yeah yeah sql injection but this should be validated
        String sql = String.format("""
                SELECT m.map_name, zrs.*
                FROM map m JOIN zone_run_scored zrs ON zrs.map_id = m.map_id
                WHERE player_id = ?
                ORDER BY %s, m.map_name, zone_type DESC, zone_index, class
                LIMIT ?
                OFFSET ?""", order);
        return jdbc.query(sql, (rs, n) -> new ZoneRunWithZone(rs), player_id, limit, offset);
    }

    public long getMaxPlayerId() {
        //noinspection DataFlowIssue
        return jdbc.queryForObject("SELECT MAX(player_id) FROM player_info", Long.class);
    }

    public int createPlayer(long player_id, @Nullable String steam_id, String name, LocalDateTime now, String country_code) {
        return jdbc.update("""
                        INSERT INTO player_info(player_id, steam_id, name, first_seen, last_seen, country_code)
                        VALUES (?, ?, ?, ?, ?, ?)""",
                player_id, steam_id, name, now, now, country_code
        );
    }

    public PlayerInfo getPlayerInfo(long player_id) {
        return jdbc.queryForObject("""
                        SELECT *
                        FROM player_info
                        WHERE player_id = ?""",
                (rs, n) -> new PlayerInfo(rs.getLong("player_id"), rs.getString("steam_id"), rs.getString("name"), rs.getTimestamp("first_seen"), rs.getTimestamp("last_seen"), rs.getString("country_code")),
                player_id);
    }

    public @Nullable PlayerStats getPlayerStats(long player_id) {
        List<PlayerStats> query = jdbc.query("""
                        WITH stat AS (SELECT overall_rank, overall_points, soldier_rank, soldier_points, demoman_rank, demoman_points
                                      FROM player_stats
                                      WHERE player_id = ?)
                        SELECT *,
                               (SELECT title FROM rank_info WHERE rank @> s.overall_rank::int4) as overall_title,
                               (SELECT title FROM rank_info WHERE rank @> s.soldier_rank::int4) as soldier_title,
                               (SELECT title FROM rank_info WHERE rank @> s.demoman_rank::int4) as demoman_title
                        FROM stat s""",
                (rs, n) -> new PlayerStats(
                                rs.getString("overall_title"), rs.getLong("overall_rank"), rs.getDouble("overall_points"),
                                rs.getString("soldier_title"), rs.getLong("soldier_rank"), rs.getDouble("soldier_points"),
                                rs.getString("demoman_title"), rs.getLong("demoman_rank"), rs.getDouble("demoman_points")
                ), player_id);
        if (query.isEmpty()) return null;
        return query.getFirst();
    }

    public List<PlayerLeaderboard> getOverallLeaderboard(int limit, int offset) {
        return jdbc.query("""
                        SELECT pi.player_id, pi.name, ps.overall_rank, ps.overall_points, ri.title
                        FROM player_info pi
                        JOIN player_stats ps ON pi.player_id = ps.player_id JOIN rank_info ri ON ri.rank @> (ps.overall_rank::int4)
                        ORDER BY ps.overall_rank
                        LIMIT ?
                        OFFSET ?""",
                (rs, n) -> new PlayerLeaderboard(rs.getLong("player_id"), rs.getString("name"), rs.getLong("overall_rank"), rs.getDouble("overall_points"), rs.getString("title")),
                limit, offset
        );
    }

    public List<PlayerLeaderboard> getSoldierLeaderboard(int limit, int offset) {
        return jdbc.query("""
                        SELECT pi.player_id, pi.name, ps.soldier_rank, ps.soldier_points, ri.title
                        FROM player_info pi
                        JOIN player_stats ps ON pi.player_id = ps.player_id JOIN rank_info ri ON ri.rank @> (ps.soldier_rank::int4)
                        ORDER BY ps.soldier_rank
                        LIMIT ?
                        OFFSET ?""",
                (rs, n) -> new PlayerLeaderboard(rs.getLong("player_id"), rs.getString("name"), rs.getLong("soldier_rank"), rs.getDouble("soldier_points"), rs.getString("title")),
                limit, offset
        );
    }

    public List<PlayerLeaderboard> getDemomanLeaderboard(int limit, int offset) {
        return jdbc.query("""
                        SELECT pi.player_id, pi.name, ps.demoman_rank, ps.demoman_points, ri.title
                        FROM player_info pi
                        JOIN player_stats ps ON pi.player_id = ps.player_id JOIN rank_info ri ON ri.rank @> (ps.demoman_rank::int4)
                        ORDER BY ps.demoman_rank
                        LIMIT ?
                        OFFSET ?""",
                (rs, n) -> new PlayerLeaderboard(rs.getLong("player_id"), rs.getString("name"), rs.getLong("demoman_rank"), rs.getDouble("demoman_points"), rs.getString("title")),
                limit, offset
        );
    }

    public ZoneRunWithZone createZoneRun(ZoneId zoneId, TFClass tfClass, long playerId, double dTime, int serverId) throws Exception {
        if (jdbc.update("""
                        INSERT INTO zone_run(map_id, zone_type, zone_index, player_id, class, server_id, time_submitted, duration)
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                        ON CONFLICT (map_id, zone_type, zone_index, player_id, class)
                        DO UPDATE SET server_id = EXCLUDED.server_id,
                                      time_submitted = EXCLUDED.time_submitted,
                                      duration = EXCLUDED.duration
                        WHERE zone_run.duration > EXCLUDED.duration""",
                zoneId.map_id(), zoneId.zone_type().name(), zoneId.zone_index(),
                playerId, tfClass.name(),
                serverId, LocalDateTime.now(), dTime) != 1) {
            throw new Exception("Failed to insert/update run!");
        }

        return jdbc.queryForObject("""
                        SELECT m.map_name, zrs.*
                        FROM map m JOIN zone_run_scored zrs ON zrs.map_id = m.map_id
                        WHERE zrs.map_id = ? AND zone_type = ? AND zone_index = ? AND player_id = ? AND class = ?""",
                (rs, n) -> new ZoneRunWithZone(rs),
                zoneId.map_id(), zoneId.zone_type().name(), zoneId.zone_index(), playerId, tfClass.name()
        );
    }

    public int deleteRun(long playerId, long mapId, String zoneType, int zoneIndex, String class_) {
        return jdbc.update("""
                DELETE FROM zone_run
                WHERE player_id = ? AND map_id = ? AND zone_type = ? AND zone_index = ? AND class = ?""",
            playerId, mapId, zoneType, zoneIndex, class_);
    }

}

