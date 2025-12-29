package com.hansenjc.tempus.data;

import io.micrometer.common.lang.Nullable;

import java.sql.Timestamp;

public record PlayerInfo(long player_id, @Nullable String steam_id, String name, Timestamp first_seen, Timestamp last_seen, CountryCode country_code) {
    public PlayerInfo(long player_id, @Nullable String steam_id, String name, Timestamp first_seen, Timestamp last_seen, String country_code) {
        this(player_id, steam_id, name, first_seen, last_seen, new CountryCode(country_code));
    }
}
