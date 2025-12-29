package com.hansenjc.tempus.data;

public class CountryCode {
    private static final int OFFSET = 0x1F1A5;

    public String code;

    public CountryCode(String code) throws IllegalArgumentException {
        if (code.length() != 2) throw new IllegalArgumentException("Invalid country code: " + code);
        this.code = code;
    }

    public String getFlag() {
        return String.format("&#x%X;&#x%X;", code.charAt(0) + OFFSET, code.charAt(1) + OFFSET);
    }

}
