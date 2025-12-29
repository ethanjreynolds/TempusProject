package com.hansenjc.tempus.data;

public record Country(CountryCode code, String name) {
    public Country(String code, String name) {
        this(new CountryCode(code), name);
    }
}
