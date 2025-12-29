package com.hansenjc.tempus.data;

import jakarta.annotation.Nullable;

public record Tier(int tier, @Nullable String name) {
    public Tier(int tier) {
        this(tier, null);
    }

    @Override
    public String toString() {
        return name == null ? Integer.toString(this.tier) : String.format("%d (%s)", this.tier, name);
    }
}
