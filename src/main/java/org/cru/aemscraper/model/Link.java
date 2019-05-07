package org.cru.aemscraper.model;

import java.util.List;

public class Link
{
    private List<String> rel;
    private String href;

    public List<String> getRel()
    {
        return rel;
    }

    public Link withRel(List<String> rel)
    {
        this.rel = rel;
        return this;
    }

    public String getHref()
    {
        return href;
    }

    public Link withHref(String href)
    {
        this.href = href;
        return this;
    }
}
