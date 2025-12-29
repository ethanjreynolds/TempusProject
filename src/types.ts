export type Author = {
    name: string,
    id: number, // seems to be an author_id which is not the same as tempus player_id
    map_id: number
}

export type Author2 = {
    name: string,
    id: number,
    user_id: number,
    steamid: string,
    user_name: string,
    map_count: number
}

export type PlayerInfo = {
    id: number,
    steamid: string,
    name: string
}

export type PlayerStats = {
    player_info: {
        id: number,
        steamid: string,
        name: string,
        first_seen: number,
        last_seen: number,
        country_code: string | null
    },
    rank_info: {
        points: number,
        rank: number,
        total_ranked: number
    },
    class_rank_info: {
        3: {
            points: number,
            rank: number,
            total_ranked: number,
            title: string | null
        },
        4: {
            points: number,
            rank: number,
            total_ranked: number,
            title: string | null
        }
    },
    country_rank_info: {
        rank: number,
        total_ranked: number
    },
    country_class_rank_info: {
        3: {
            rank: number,
            total_ranked: number
        },
        4: {
            rank: number,
            total_ranked: number
        }
    },
    pr_stats: any,
    wr_stats: any,
    top_stats: any,
    zone_count: any
}

export type TierInfo = {
    // solider
    3: number,
    // demoman
    4: number
}

export type RatingInfo = {
    // solider
    3: number,
    // demoman
    4: number
}

export type ServerStatus = {
    game_info: {
        hostname: string,
        currentMap: string,
        nextMap: string | null,
        users: string[],
        playerCount: number,
        maxPlayers: number,
        tempusVersion: number,
        spVersion: number,
        gameVersion: number,
        appID: number,
        freeDisk: number
    },
    server_info: {
        id: number,
        name: string,
        shortname: string, // 2 or 3 ISO 3166 code with index
        country: string,
        addr: string,
        ipAddr: string
        port: number,
        hidden: boolean
    }
}

export type RunInfo = {
    id: number,
    duration: number,
    date: number,
    name: string,
    steamid: string,
    player_info: PlayerInfo
}

export type RunInfoEx = {
    id: number,
    zone_id: number,
    duration: number,
    class: number,
    date: number,
    demo_info: {
        id: number,
        start_tick: number,
        end_tick: number,
        url: string,
        server_info: {
            id: number,
            name: string
        }
    },
    user_id: number,
    name: string,
    steamid: string,
    rank: number, // map rank
    placement: number, // pretty sure this is the group but as a number
    player_info: PlayerInfo
}

export type ZoneInfo = {
    id: number,
    map_id: number,
    type: string,
    zoneindex: number,
    custom_name: string | null
}

export type ZoneRuns = {
    zone_info: ZoneInfo,
    tier_info: TierInfo,
    rating_info: RatingInfo
    completion_info: {
        soldier: number,
        demoman: number
    },
    results: {
        soldier: RunInfoEx[],
        demoman: RunInfoEx[]
    }
}

export type MapDetail = {
    id: number,
    name: string,
    zone_counts: {
        checkpoint: number
        linear: number,
        map_end: number,
        map: number
    },
    authors: Author[],
    tier_info: TierInfo,
    rating_info: RatingInfo,
    videos: {
        soldier: string,
        demoman: string
    }
};

export type MapFullOverview = {
    map_info: {
        id: number,
        name: string,
        date_added: number // might be a string, seems to be unix time but has a decimal
    },
    tier_info: {
        // these are for map runs only
        soldier: number,
        demoman: number
    },
    rating_info: {
        // these are for map runs only
        soldier: number,
        demoman: number
    },
    videos: {
        soldier: string | null,
        demoman: string | null
    },
    authors: Author2[],
    soldier_runs: RunInfo[],
    demoman_runs: RunInfo[],
    zone_counts: {
        // no idea what _end's are for
        checkpoint: number,
        bonus_end: number,
        bonus: number,
        course: number,
        course_end: number,
        map_end: number,
        map: number
    },
}

export type MapFullOverview2 =
    MapFullOverview & {
    zones: {
        checkpoint: ZoneInfo[],
        bonus_end: ZoneInfo[],
        bonus: ZoneInfo[],
        course: ZoneInfo[],
        course_end: ZoneInfo[],
        map_end: ZoneInfo[],
        map: ZoneInfo[]
    }
}