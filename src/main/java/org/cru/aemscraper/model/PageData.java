package org.cru.aemscraper.model;

import java.util.List;
import java.util.Objects;

public class PageData {
    private String url;
    private String htmlBody;
    private String contentScore;
    private List<String> tags;
    private String title;
    private String description;
    private String imageUrl;

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

    public List<String> getTags() {
        return tags;
    }

    public PageData withTags(final List<String> tags) {
        this.tags = tags;
        return this;
    }

    public String getTitle() {
        return title;
    }

    public PageData withTitle(final String title) {
        this.title = title;
        return this;
    }

    public String getDescription() {
        return description;
    }

    public PageData withDescription(final String description) {
        this.description = description;
        return this;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public PageData withImageUrl(final String imageUrl) {
        this.imageUrl = imageUrl;
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
                && Objects.equals(getTags(), other.getTags())
                && Objects.equals(getTitle(), other.getTitle())
                && Objects.equals(getDescription(), other.getDescription())
                && Objects.equals(getImageUrl(), other.getImageUrl());
        }
        return false;
    }
}
