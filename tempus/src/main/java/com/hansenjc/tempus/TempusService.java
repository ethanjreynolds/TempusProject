package com.hansenjc.tempus;

import com.hansenjc.tempus.enums.TFClass;
import com.hansenjc.tempus.data.*;
import com.hansenjc.tempus.enums.ZoneType;
import jakarta.annotation.Nullable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

// for handling data
@Service
public class TempusService {

    private final TempusRepository repository;

    public TempusService(TempusRepository repository) {
        this.repository = repository;
    }

    public List<Country> getCountries() {
        return repository.getCountries();
    }

    public List<Server> getServers() {
        return repository.getServers();
    }

    public MapName getMapName(int map_id) {
        return new MapName(map_id, repository.getMapName(map_id));
    }

    public List<MapName> getMapNames() {
        return repository.getMapNames();
    }

    public List<ZoneCounts> getAllMapZoneCounts() {
        return repository.getAllMapZoneCounts();
    }

    public List<ZoneInfo> getAllMapInfos() {
        return repository.getAllMapInfos();
    }

    public int getMapCourseCount(int map_id) {
        return repository.getMapZoneCount(map_id, ZoneType.course);
    }

    public int getMapBonusCount(int map_id) {
        return repository.getMapZoneCount(map_id, ZoneType.bonus);
    }

    public ZoneInfo getZoneInfo(ZoneId zone_id) {
        return repository.getZoneInfo(zone_id);
    }

    public List<Player> getMapAuthors(int map_id) { return repository.getMapAuthors(map_id); }

    public List<ZoneRunWithPlayer> getZoneRuns(ZoneId zone_id, TFClass tf_class, int offset, int limit) {
        return repository.getZoneRuns(zone_id, tf_class, offset, limit);
    }

    public ZoneRuns getZoneRuns(ZoneId zone_id, int offset, int limit) {
        return new ZoneRuns(repository.getZoneRuns(zone_id, TFClass.soldier, offset, limit), repository.getZoneRuns(zone_id, TFClass.demoman, offset, limit));
    }

    public ZoneRuns getZoneRuns(ZoneId zone_id) {
        return getZoneRuns(zone_id, 0, 50);
    }

    public List<ZoneRunWithZone> getPlayerRuns(long player_id, int limit, int offset) {
        return repository.getPlayerRuns(player_id, limit, offset, null);
    }

    public List<ZoneRunWithZone> getPlayerRuns(long player_id, TempusApiController.PlayerRunsParameters params) {
        return repository.getPlayerRuns(player_id, params.limit(), params.offset(), params.orders());
    }

    /* Returns new player's player_id
     */
    public long createPlayer(String name, @Nullable String steam_id, String code) throws Exception {
        // manually created player_id's will begin from 1,000,000
        long player_id = Math.max(1000000, repository.getMaxPlayerId()) + 1;

        int rows = repository.createPlayer(player_id, steam_id, name, LocalDateTime.now(), code);
        if (rows != 1) throw new Exception(String.format("Creation of player: [%d, %s] failed!", player_id, name));

        System.out.printf("Successfully created player: [%d, %s]!\n", player_id, name);

        return player_id;
    }

    public PlayerInfo getPlayerInfo(long player_id) {
        return repository.getPlayerInfo(player_id);
    }

    public @Nullable PlayerStats getPlayerStats(long player_id) {
        return repository.getPlayerStats(player_id);
    }

    public List<PlayerLeaderboard> getLeaderboard(String type, int limit, int offset) {
        if (Objects.equals(type, "overall")) return repository.getOverallLeaderboard(limit, offset);
        if (Objects.equals(type, "soldier")) return repository.getSoldierLeaderboard(limit, offset);
        if (Objects.equals(type, "demoman")) return repository.getDemomanLeaderboard(limit, offset);

        throw new IllegalArgumentException(String.format("Invalid leaderboard type: %s not found!", type));
    }

    public ZoneRunWithZone createZoneRun(ZoneId zoneId, TFClass tfClass, long playerId, double dTime, int serverId) throws Exception {
        return repository.createZoneRun(zoneId, tfClass, playerId, dTime, serverId);
    }

    public PlayerStats deleteRun(long playerId, int mapId, String zoneType, int zoneIndex, String class_) throws Exception {
        int rows = repository.deleteRun(playerId, mapId, zoneType, zoneIndex, class_);
        if (rows == 0) throw new IllegalArgumentException("Delete failed: run not found");
        return getPlayerStats(playerId);
    }
}
