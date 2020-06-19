package org.cru.aemscraper.util;

public enum RunMode {
    S3, CLOUDSEARCH;

    public static RunMode fromCode(final String code) {
        String upperCase = code.toUpperCase();
        switch (upperCase) {
            case "S3":
                return S3;
            case "CLOUDSEARCH":
                return CLOUDSEARCH;
        }
        throw new IllegalArgumentException("Code " + code + " is invalid");
    }
}
