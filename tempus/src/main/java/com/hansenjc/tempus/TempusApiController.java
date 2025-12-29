package com.hansenjc.tempus;

import com.hansenjc.tempus.data.*;
import com.hansenjc.tempus.enums.TFClass;
import com.hansenjc.tempus.enums.ZoneType;
import jakarta.annotation.Nullable;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

@RestController
public class TempusApiController {
    private final TempusService service;

    public TempusApiController(TempusService service) {
        this.service = service;
    }

    @GetMapping("/api/countries")
    public ResponseEntity<List<Country>> apiCountries() {
        try {
            List<Country> countries = service.getCountries();
            HttpHeaders headers = new HttpHeaders();
            headers.setCacheControl(CacheControl.maxAge(365, TimeUnit.DAYS));
            return new ResponseEntity<>(countries, headers, HttpStatus.OK);
        } catch (Exception e) {
            System.err.println(e);
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/api/servers")
    public ResponseEntity<List<Server>> apiServers() {
        try {
            List<Server> servers = service.getServers();
            HttpHeaders headers = new HttpHeaders();
            headers.setCacheControl(CacheControl.maxAge(365, TimeUnit.DAYS));
            return new ResponseEntity<>(servers, headers, HttpStatus.OK);
        } catch (Exception e) {
            System.err.println(e);
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/api/maps/{map_id}/{zone_type}/{zone_index}/runs")
    public ResponseEntity<List<ZoneRunWithPlayer>> apiMapRuns(@PathVariable String map_id, @PathVariable String zone_type, @PathVariable String zone_index, @RequestParam String tfclass, @RequestParam String offset, @RequestParam(required = false) String limit) {
        try {
            final int i_map_id = Integer.parseInt(map_id);
            final ZoneType e_zone_type = ZoneType.valueOf(zone_type);
            final int i_zone_index = Integer.parseInt(zone_index);

            final TFClass tfClass = TFClass.valueOf(tfclass);
            final int i_offset = Integer.parseInt(offset);

            final ZoneId zone_id = new ZoneId(i_map_id, e_zone_type, i_zone_index);

            List<ZoneRunWithPlayer> map_runs = service.getZoneRuns(zone_id, tfClass, i_offset, limit != null ? Integer.parseInt(limit) : 999999);

            if (map_runs.isEmpty()) return new ResponseEntity<>(HttpStatus.NO_CONTENT);
            return ResponseEntity.ok(map_runs);
        } catch (IllegalArgumentException e) {
            System.err.println(e);
            return new ResponseEntity<>(HttpStatus.UNPROCESSABLE_ENTITY);
        }
    }

    public record PlayerRunsParameters(int limit, int offset, OrderBy[] orders) {}
    public record OrderBy(String column, int direction) {}

    @PostMapping(value = "/api/player/{player_id}/runs", consumes = "application/json")
    public ResponseEntity<List<ZoneRunWithZone>> apiPlayerRuns(@PathVariable String player_id, @RequestBody PlayerRunsParameters params) {
        try {
            final long l_player_id = Long.parseLong(player_id);

            List<ZoneRunWithZone> player_runs = service.getPlayerRuns(l_player_id, params);

            if (player_runs.isEmpty())
                return new ResponseEntity<>(HttpStatus.NO_CONTENT);
            return ResponseEntity.ok(player_runs);
        } catch (IllegalArgumentException e) {
            System.err.println(e);
            return new ResponseEntity<>(HttpStatus.UNPROCESSABLE_ENTITY);
        }
    }

    @GetMapping("/api/player/{player_id}/stats")
    public ResponseEntity<PlayerStats> apiPlayerRuns(@PathVariable String player_id) {
        try {
            final long l_player_id = Long.parseLong(player_id);
            return ResponseEntity.ok(service.getPlayerStats(l_player_id));
        } catch (IllegalArgumentException e) {
            System.err.println(e);
            return new ResponseEntity<>(HttpStatus.UNPROCESSABLE_ENTITY);
        }
    }

    @PostMapping("/api/run/create")
    public ResponseEntity<CreateRunResponse> apiCreateRun(@RequestParam String player_id, @RequestParam String map_id, @RequestParam String zone_type, @RequestParam String zone_index, @RequestParam String tf_class, @RequestParam String time, @RequestParam String server_id) {
        try {
            final long l_player_id = Long.parseLong(player_id);
            final ZoneId zone_id = new ZoneId(map_id, zone_type, zone_index);
            final TFClass e_tf_class = TFClass.valueOf(tf_class);
            final double d_time = TempusUtils.parseDuration(time);
            final int i_server_id = Integer.parseInt(server_id);

            ZoneRunWithZone run = service.createZoneRun(zone_id, e_tf_class, l_player_id, d_time, i_server_id);
            PlayerStats stats = service.getPlayerStats(l_player_id);

            return ResponseEntity.ok(new CreateRunResponse("OK: Inserted run ranked " + run.rank(), run, stats));
        } catch (Exception e) {
            e.printStackTrace();
            if (e instanceof DuplicateKeyException)
                return ResponseEntity.unprocessableEntity().body(new CreateRunResponse(e.getCause().toString()));
            if (e instanceof IllegalArgumentException)
                return ResponseEntity.unprocessableEntity().body(new CreateRunResponse(e.toString()));

            return ResponseEntity.internalServerError().body(new CreateRunResponse(e.toString()));
        }
    }

    public record CreateRunResponse(String message, @Nullable ZoneRunWithZone run, @Nullable PlayerStats stats) {
        public CreateRunResponse(String message) {
            this(message, null, null);
        }
    }

    
    public record DeleteRunResponse(String message, PlayerStats stats) {
        public DeleteRunResponse(String message) { 
            this(message, null); 
        }
    }

    @PostMapping("/api/run/delete")
    public ResponseEntity<DeleteRunResponse> apiDeleteRun(@RequestParam String player_id,@RequestParam String map_id,
            @RequestParam String zone_type, @RequestParam String zone_index, @RequestParam String tf_class) {
        try {
            final long l_player_id = Long.parseLong(player_id);
            final int i_map_id = Integer.parseInt(map_id);
            final String s_zone_type = zone_type;
            final int i_zone_index = Integer.parseInt(zone_index);
            final String s_class = tf_class;

            // delete and fetch updated stats
            PlayerStats stats = service.deleteRun(l_player_id, i_map_id, s_zone_type, i_zone_index, s_class);

            return ResponseEntity.ok(new DeleteRunResponse("OK: Deleted run", stats));
        } catch (Exception e) {
            e.printStackTrace();
            if (e instanceof IllegalArgumentException)
                return ResponseEntity.unprocessableEntity().body(new DeleteRunResponse(e.toString()));
            return ResponseEntity.internalServerError().body(new DeleteRunResponse(e.toString()));
        }
    }

    @PostMapping("/api/player/create")
    public ResponseEntity<String> apiCreatePlayer(@RequestParam String name, @RequestParam(required = false) String steam_id, @RequestParam String code) {
        try {
            if (name == null || name.length() > 64)
                throw new IllegalArgumentException("name can not exceed 64 characters!");
            if (steam_id != null && !Pattern.compile("STEAM_\\d:[01]:\\d+").matcher(steam_id).matches())
                throw new IllegalArgumentException("Invalid steamID!");
            if (code == null || code.length() != 2)
                throw new IllegalArgumentException(String.format("Invalid country code: %s!", code));

            return ResponseEntity.ok(String.valueOf(service.createPlayer(name, steam_id, code)));
        } catch (Exception e) {
            System.err.println(e);
            if (e instanceof DuplicateKeyException)
                return ResponseEntity.unprocessableEntity().body(e.getCause().toString());
            if (e instanceof IllegalArgumentException)
                return ResponseEntity.unprocessableEntity().body(e.toString());

            return ResponseEntity.internalServerError().body(e.toString());
        }
    }

    @GetMapping("/api/leaderboard")
    public ResponseEntity<List<PlayerLeaderboard>> apiLeaderboard(@RequestParam String type, @RequestParam int limit, @RequestParam int offset) {
        try {
            List<PlayerLeaderboard> players = service.getLeaderboard(type, limit, offset);
            if (players.isEmpty())
                return new ResponseEntity<>(HttpStatus.NO_CONTENT);
            return ResponseEntity.ok(players);
        } catch (Exception e) {
            System.err.println(e);
            return new ResponseEntity<>(HttpStatus.UNPROCESSABLE_ENTITY);
        }
    }

    @GetMapping("/api/zone_counts")
    public ResponseEntity<List<ZoneCounts>> apiZones() {
        try {
            List<ZoneCounts> maps = service.getAllMapZoneCounts();

            if (maps.isEmpty()) return new ResponseEntity<>(HttpStatus.NO_CONTENT);

            return ResponseEntity.ok(maps);
        } catch (Exception e) {
            System.err.println(e);
            return new ResponseEntity<>(HttpStatus.UNPROCESSABLE_ENTITY);
        }
    }
}
