package com.hansenjc.tempus.data;


public record PlayerLeaderboard(long player_id, String name, long rank, double points, String rank_title) {}