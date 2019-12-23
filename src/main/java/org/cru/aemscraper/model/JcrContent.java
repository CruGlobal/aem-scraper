package org.cru.aemscraper.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class JcrContent {
    @JsonProperty("sling:redirect")
    private boolean redirect;

    @JsonFormat(with = JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
    @JsonProperty("sling:vanityPath")
    private List<String> vanityPaths;

    public boolean isRedirect() {
        return redirect;
    }

    public List<String> getVanityPaths() {
        return vanityPaths == null ? new ArrayList<>() : vanityPaths;
    }
}
