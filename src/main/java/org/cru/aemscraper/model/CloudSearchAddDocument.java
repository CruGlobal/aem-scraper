package org.cru.aemscraper.model;

public class CloudSearchAddDocument extends CloudSearchDocument {
    private final String type = "add";

    @Override
    public String getType() {
        return type;
    }
}
