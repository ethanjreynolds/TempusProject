package com.hansenjc.tempus.data;

import jakarta.annotation.Nullable;

public record Server(int server_id, @Nullable String server_name, CountryCode country_code) {}
