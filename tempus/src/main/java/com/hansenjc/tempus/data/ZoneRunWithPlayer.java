package com.hansenjc.tempus.data;

import java.sql.Timestamp;

public record ZoneRunWithPlayer(Player player, String class_, Timestamp time_submitted, double duration, long rank, String group) {}
