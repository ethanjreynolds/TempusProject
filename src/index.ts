import {Author2, RunInfoEx, ZoneRuns} from "./types";
import postgres, {Row} from "postgres";
import {
    get_map_detail,
    get_map_full_overview,
    get_player_stats,
    get_server_statuses,
    get_zone_runs, HTTP
} from "./tempus_requests";
import {COUNTRY_CODE_3_TO_2} from "./country";
import axios from "axios";
import * as fs from "node:fs";


const sql = postgres('postgres://127.0.0.1:5433/tempus');

async function populate_map_info() {
    const map_details = await get_map_detail();
    if (map_details === undefined) return;
    await Promise.all([...map_details.map(async (map) => {
        try {
            await sql`INSERT INTO map VALUES (${map.id}, ${map.name})`;

        } catch (e) {
            console.error(e)
        }
    })]);
}

async function populate_zone_info() {
    const map_ids_result = await sql`SELECT map_id
                                     FROM map
                                     ORDER BY map_id`;

    // noinspection ES6MissingAwait
    map_ids_result.forEach(async (value: Row) => {
        const map_id = value.map_id;

        console.log(`[${(new Date()).toISOString()}] Inserting map_id: ${map_id} into zone_info`);

        let map_full_overview = await get_map_full_overview(map_id);
        if (map_full_overview === undefined) return;

        const map_info = await get_zone_runs(map_id, "map");
        if (map_info !== undefined) {
            try {
                sql`INSERT INTO zone_info
                    VALUES (${map_id}, 'map', 1, ${map_info.tier_info["3"]}, ${map_info.rating_info["3"]},
                            ${map_info.tier_info["4"]}, ${map_info.rating_info["4"]})`;
            } catch (e) {
                console.error(e)
            }
        }

        for (let c = 1; c <= map_full_overview.zone_counts.course; c++) {
            const bonus_info = await get_zone_runs(map_id, "course", c);
            if (bonus_info === undefined) continue;
            try {
                sql`INSERT INTO zone_info
                    VALUES (${map_id}, 'course', ${c}, ${bonus_info.tier_info["3"]}, ${bonus_info.rating_info["3"]},
                            ${bonus_info.tier_info["4"]}, ${bonus_info.rating_info["4"]})`;
            } catch (e) {
                console.error(e)
            }
        }

        for (let b = 1; b <= map_full_overview.zone_counts.bonus; b++) {
            const bonus_info = await get_zone_runs(map_id, "bonus", b);
            if (bonus_info === undefined) continue;
            try {
                sql`INSERT INTO zone_info
                    VALUES (${map_id}, 'bonus', ${b}, ${bonus_info.tier_info["3"]}, ${bonus_info.rating_info["3"]},
                            ${bonus_info.tier_info["4"]}, ${bonus_info.rating_info["4"]})`;
            } catch (e) {
                console.error(e)
            }
        }

        console.log(`[${(new Date()).toISOString()}] Inserted map_id: ${map_id} into zone_info`);
    })
}

async function populate_zone_runs() {
    async function insert_zone_run(map_id: number, zone_type: string, zone_index: number, zone_run: RunInfoEx, class_: string) {
        while (true) {
            try {
                // just assume that player id exists, more likely to exists as db fills up
                // should be faster than having to check every time first
                await sql`INSERT INTO zone_run
                          VALUES (${map_id}, ${zone_type}, ${zone_index}, ${zone_run.player_info.id}, ${class_},
                                  ${zone_run.demo_info.server_info.id}, ${zone_run.date * 1000}, ${zone_run.duration})`;
                console.log(`[${(new Date()).toISOString()}] ${map_id}-${zone_type}-${zone_index} | ${class_} player_id ${zone_run.player_info.id}`)
                return;
            } catch (e) {
                if (e instanceof postgres.PostgresError && e.code == "23503" && e.constraint_name == "player_id_fk") {
                    console.log(`[${(new Date()).toISOString()}] ${map_id}-${zone_type}-${zone_index} | player_id: ${zone_run.player_info.id} fk violation!`);
                    await populate_player_info(zone_run.player_info.id);
                } else {
                    console.error(e);
                    return;
                }
            }
        }
    }

    const zone_info_result = await sql<{ map_id: number, zone_type: string, zone_index: number }[]>
        `SELECT map_id, zone_type, zone_index
         FROM zone_info
         ORDER BY map_id, zone_type, zone_index`;

    // let row = {map_id: 1, zone_type: "map", zone_index: 1};
    for (const row of zone_info_result) {
        const zone_runs = await get_zone_runs(row.map_id, row.zone_type, row.zone_index, 999999);
        if (zone_runs === undefined) {
            console.log(`[${(new Date()).toISOString()}] zone_run ${row.map_id}_${row.zone_type}_${row.zone_index} returned undefined!`);
            continue;
        }
        console.log(`[${(new Date()).toISOString()}] awaiting zone_run ${row.map_id}_${row.zone_type}_${row.zone_index}`);
        await Promise.all([
            ...zone_runs.results.soldier.map(async (zone_run) => insert_zone_run(row.map_id, row.zone_type, row.zone_index, zone_run, "soldier")),
            ...zone_runs.results.demoman.map(async (zone_run) => insert_zone_run(row.map_id, row.zone_type, row.zone_index, zone_run, "demoman"))
        ]);
    }
}

async function populate_server_info() {
    const server_statuses = await get_server_statuses();
    if (server_statuses === undefined) {
        console.error("[populate_server_info] Server Statuses returned undefined!");
        return;
    }

    // noinspection ES6MissingAwait
    server_statuses.forEach(async (server_status) => {
        let matcher = server_status.server_info.shortname.match(/^([A-Z]{2,3})([0-9]+)$/);
        if (matcher == null) {
            console.error(`[populate_server_info] Failed to match \"${server_status.server_info.shortname}\"!`);
            return;
        }

        let code = matcher[1];
        let index = parseInt(matcher[2]);

        // TODO: XK missing
        if (code.length === 3) code = COUNTRY_CODE_3_TO_2[code];

        try {
            await sql`INSERT INTO server
                      VALUES (${server_status.server_info.id}, ${server_status.server_info.name}, ${index}, ${code})`;
            console.log(`[populate_server_info] Inserted server_id: ${server_status.server_info.id} into server`);
        } catch (e) {
            console.error(e);
        }
    });
}

async function populate_map_author() {
    const map_ids_result = await sql<{ map_id: number }[]>`
        SELECT map_id
        FROM map
        ORDER BY map_id`;

    async function insert_map_author(map_id: number, author: Author2) {
        if (author.user_id === 0) {
            console.error(`[${(new Date()).toISOString()}] map_id: ${map_id} has author ${author.name} with player_id = 0!`);
            return;
        }
        while (true) {
            try {
                await sql`INSERT INTO map_author
                          VALUES (${map_id}, ${author.user_id})`;
                console.log(`[${(new Date()).toISOString()}] Inserted map_id: ${map_id}, player_id: ${author.user_id} into map_author`);
                return;
            } catch (e) {
                console.error(e);
                if (e instanceof postgres.PostgresError && e.code == "23503") {
                    await populate_player_info(author.user_id);
                } else {
                    return;
                }
            }
        }
    }

    await Promise.all([...map_ids_result.map(async (row) => {
        const map_full_overview = await get_map_full_overview(row.map_id);
        if (map_full_overview === undefined) {
            console.error("[populate_map_author] Map Full Overview returned undefined!");
            return;
        }
        await Promise.all([...map_full_overview.authors.map(async (author) => insert_map_author(row.map_id, author))]);
    })]);
}

async function populate_player_info(player_id: number) {
    const player_stats = await get_player_stats(player_id);
    if (player_stats === undefined) return;

    let country = player_stats.player_info.country_code;
    if (country !== null && country.length === 3) country = COUNTRY_CODE_3_TO_2[country];

    try {
        await sql`INSERT INTO player_info
                  VALUES (${player_id}, ${player_stats.player_info.steamid}, ${player_stats.player_info.name},
                          ${player_stats.player_info.first_seen * 1000}, ${player_stats.player_info.last_seen * 1000},
                          ${country})`;
        console.log(`[${(new Date()).toISOString()}] Inserted player_id: ${player_id} into player_info`);
    } catch (e) {
        console.error(e)
    }
}

async function download_map_images() {
    const map_ids_result = await sql<{map_id: number, map_name: string}[]>`SELECT *
                                     FROM map
                                     ORDER BY map_name`;

    await Promise.all(
        ...[map_ids_result.map(async (row) => {
            const response = await HTTP.get( `https://tempusplaza.xyz/map-backgrounds/medium/${row.map_name}.jpg`, {
                responseType: "stream",
            });

            const stream = response.data.pipe(fs.createWriteStream(`${row.map_name}.jpg`));

            stream.on("finish", () => console.log(`Downloaded ${row.map_name}.jpg`));
            stream.on("error", () => console.log(`Failed to download ${row.map_name}.jpg`));
        })
    ]);
}

async function main() {
    // await populate_server_info();
    // await populate_map_info();
    // await populate_zone_info();
    // await populate_zone_runs();
    // await populate_map_author();
}

main();
