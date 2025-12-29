package com.hansenjc.tempus;


import com.hansenjc.tempus.data.ZoneId;
import com.hansenjc.tempus.enums.ZoneType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;


@Controller
public class TempusController {
    private final TempusService service;
    private final TempusUtils utils = new TempusUtils();

    public TempusController(TempusService service) {
        this.service = service;
    }

    @GetMapping("/")
    public String root(Model model) {
        model.addAttribute("main", "fragments/root.html");
        return "layout";
    }

    @GetMapping("/maps")
    public String maps(Model model) {
        // this is mostly static
        model.addAttribute("maps", service.getAllMapInfos());
        model.addAttribute("main", "fragments/maps/maps.html");
        model.addAttribute("side_bar", "fragments/maps/maps-side-bar.html");
        return "layout";
    }

    @GetMapping("/maps/{map_id}/{zone_type}/{zone_index}")
    public String map(@PathVariable String map_id, @PathVariable String zone_type, @PathVariable String zone_index, Model model) {
        try {
            int i_map_id = Integer.parseInt(map_id);
            ZoneType e_zone_type = ZoneType.valueOf(zone_type);
            int i_zone_index = Integer.parseInt(zone_index);

            ZoneId zone_id = new ZoneId(i_map_id, e_zone_type, i_zone_index);

            model.addAttribute("map", service.getMapName(i_map_id));
            model.addAttribute("zone", zone_id);
            model.addAttribute("authors", service.getMapAuthors(i_map_id));

            int courseCount = service.getMapCourseCount(i_map_id);
            int bonusCount = service.getMapBonusCount(i_map_id);

            model.addAttribute("course_count", courseCount);
            model.addAttribute("bonus_count", bonusCount);

            model.addAttribute("info", service.getZoneInfo(zone_id));
            model.addAttribute("runs", service.getZoneRuns(zone_id));

            model.addAttribute("utils", utils);

            model.addAttribute("main", "fragments/maps/zone.html");
            if (courseCount > 0 || bonusCount > 0) model.addAttribute("side_bar", "fragments/maps/zone-side-bar.html");
            return "layout";
        } catch (IllegalArgumentException e) {
            System.err.println(e);
            return "error";
        }
    }

    @GetMapping("/players")
    public String players(@RequestParam(value = "type", required = false, defaultValue = "overall") String type, Model model) {
        model.addAttribute("type", type);
        model.addAttribute("leaderboard", service.getLeaderboard(type, 100, 0));
        model.addAttribute("main", "fragments/players/players.html");
        return "layout";
    }

    @GetMapping("/players/{player_id}")
    public String player(@PathVariable String player_id, Model model) {
        try {
            long l_player_id = Long.parseLong(player_id);

            model.addAttribute("player_info", service.getPlayerInfo(l_player_id));
            model.addAttribute("stats", service.getPlayerStats(l_player_id));
            model.addAttribute("zone_runs", service.getPlayerRuns(l_player_id, 100, 0));

            model.addAttribute("utils", utils);

            model.addAttribute("main", "fragments/players/player.html");
            return "layout";
        } catch (IllegalArgumentException e) {
            System.err.println(e);
            return "error";
        }
    }
}
