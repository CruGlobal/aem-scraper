package org.cru.aemscraper.model;

public class CloudSearchDeleteDocument extends CloudSearchDocument {
    private final String type = "delete";

    @Override
    public String getType() {
        return type;
    }
}
