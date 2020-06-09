package org.cru.aemscraper.model;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.util.Map;

@JsonPropertyOrder({
    "type",
    "id",
    "fields"
})
public abstract class CloudSearchDocument {
    public enum Type {
        ADD, DELETE
    }

    private String id;
    private Map<String, String> fields;

    public abstract String getType();

    public String getId() {
        return id;
    }

    public void setId(final String id) {
        this.id = id;
    }

    public Map<String, String> getFields() {
        return fields;
    }

    public void setFields(final Map<String, String> fields) {
        this.fields = fields;
    }
}
