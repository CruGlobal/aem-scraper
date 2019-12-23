package org.cru.aemscraper.model;

import java.util.Objects;
import java.util.Set;

public class PageData {
    private String url;
    private String htmlBody;
    private String contentScore;
    private Set<String> tags;

    public String getUrl() {
        return url;
    }

    public PageData withUrl(final String url) {
        this.url = url;
        return this;
    }

    public String getHtmlBody() {
        return htmlBody;
    }

    public PageData withHtmlBody(final String htmlBody) {
        this.htmlBody = htmlBody;
        return this;
    }

    public String getContentScore() {
        return contentScore;
    }

    public PageData withContentScore(final String contentScore) {
        this.contentScore = contentScore;
        return this;
    }

    public Set<String> getTags() {
        return tags;
    }

    public PageData withTags(final Set<String> tags) {
        this.tags = tags;
        return this;
    }

    @Override
    public boolean equals(final Object obj) {
        boolean equals = super.equals(obj);

        if (equals) {
            return true;
        }
        if (obj instanceof PageData) {
            PageData other = (PageData) obj;
            return Objects.equals(getUrl(), other.getUrl())
                && Objects.equals(getHtmlBody(), other.getHtmlBody())
                && Objects.equals(getContentScore(), other.getContentScore())
                && Objects.equals(getTags(), other.getTags());
        }
        return false;
    }
}
