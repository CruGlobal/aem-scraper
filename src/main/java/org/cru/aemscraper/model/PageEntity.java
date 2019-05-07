package org.cru.aemscraper.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public class PageEntity
{
    @JsonProperty("entities")
    private List<PageEntity> children;

    private List<Link> links;

    @JsonProperty("class")
    private List<String> classType;

    private Map<String, Object> properties;

    public List<PageEntity> getChildren()
    {
        return children;
    }

    public PageEntity withChildren(List<PageEntity> children)
    {
        this.children = children;
        return this;
    }

    public List<Link> getLinks()
    {
        return links;
    }

    public PageEntity withLinks(List<Link> links)
    {
        this.links = links;
        return this;
    }

    public List<String> getClassType()
    {
        return classType;
    }

    public PageEntity withClassType(List<String> classType)
    {
        this.classType = classType;
        return this;
    }

    public Map<String, Object> getProperties()
    {
        return properties;
    }

    public PageEntity withProperties(Map<String, Object> properties)
    {
        this.properties = properties;
        return this;
    }

    @Override
    public String toString() {
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            return objectMapper.writeValueAsString(this);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return super.toString();
    }
}
