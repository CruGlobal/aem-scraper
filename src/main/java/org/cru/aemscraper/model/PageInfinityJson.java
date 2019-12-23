package org.cru.aemscraper.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class PageInfinityJson {
    @JsonProperty("jcr:content")
    private JcrContent jcrContent;

    public JcrContent getJcrContent() {
        return jcrContent;
    }
}
