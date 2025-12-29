import {MapDetail, MapFullOverview, PlayerStats, ServerStatus, ZoneRuns} from "./types";
import rateLimit from "axios-rate-limit";
import axios from "axios";

export const HTTP = rateLimit(axios.create(), {maxRPS: 1})

export async function get_server_statuses(): Promise<ServerStatus[] | undefined> {
    let data = undefined;
    await HTTP.get("https://tempus2.xyz/api/v0/servers/statusList")
        .then((res) => data = res.data)
        .catch((err) => console.log(err));
    return data;
}

// TODO: really should use https://tempus2.xyz/api/v0/maps/list instead for only id and name
export async function get_map_detail(): Promise<MapDetail[] | undefined> {
    let data = undefined;
    await HTTP.get("https://tempus2.xyz/api/v0/maps/detailedList")
        .then((res) => data = res.data)
        .catch((err) => console.log(err));
    return data;
}

export async function get_map_full_overview(map_id: number): Promise<MapFullOverview | undefined> {
    let data = undefined;
    await HTTP.get(`https://tempus2.xyz/api/v0/maps/id/${map_id}/fullOverview`)
        .then((res) => data = res.data)
        .catch((err) => console.log(err));
    return data;
}

export async function get_zone_runs(map_id: number, zone_type: string, zone_index = 1, limit = 1): Promise<ZoneRuns | undefined> {
    if (map_id < 0) {
        console.info(`[get_zone_runs]: Invalid argument map_id: ${map_id}`);
        return undefined;
    }
    if (!(zone_type === "map" || zone_type === "course" || zone_type === "bonus" || zone_type === "trick")) {
        console.info(`[get_zone_runs]: Invalid argument zone_type: ${zone_type}`);
        return undefined;
    }
    if (zone_index < 0) {
        console.info(`[get_zone_runs]: Invalid argument zone_index: ${zone_index}`);
        return undefined;
    }

    let data = undefined;
    await HTTP.get(`https://tempus2.xyz/api/v0/maps/id/${map_id}/zones/typeindex/${zone_type}/${zone_index}/records/list?limit=${limit}`)
        .then((res) => data = res.data)
        .catch((err) => console.log(err));

    if (data === undefined) console.error(`[get_zone_runs]: Failed with arguments: map_id: ${map_id}, zone_type: ${zone_type}, zone_index: ${zone_index}, limit: ${limit}`);
    return data;
}

export async function get_player_stats(player_id: number): Promise<PlayerStats | undefined> {
    let data = undefined;
    await HTTP.get(`https://tempus2.xyz/api/v0/players/id/${player_id}/stats`)
        .then((res) => data = res.data)
        .catch((err) => console.log(err));
    return data;
}