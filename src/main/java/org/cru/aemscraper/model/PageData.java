package org.cru.aemscraper.model;

import com.fasterxml.jackson.annotation.JsonIgnore;

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
    private String publishedDate;
    private String siteSection; // In the form of {first,second,third}


    @JsonIgnore
    private String template;
    @JsonIgnore
    private boolean excludeFromSearch;

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

    public String getPublishedDate() {
        return publishedDate;
    }

    public PageData withPublishedDate(final String publishedDate) {
        this.publishedDate = publishedDate;
        return this;
    }

    public String getTemplate() {
        return template;
    }

    public PageData withTemplate(final String template) {
        this.template = template;
        return this;
    }

    public boolean shouldExcludeFromSearch() {
        return excludeFromSearch;
    }

    public PageData shouldExcludeFromSearch(final boolean excludeFromSearch) {
        this.excludeFromSearch = excludeFromSearch;
        return this;
    }

    public String getSiteSection() {
        return siteSection;
    }

    public PageData withSiteSection(final String siteSection) {
        this.siteSection = siteSection;
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
                && Objects.equals(getImageUrl(), other.getImageUrl())
                && Objects.equals(getPublishedDate(), other.getPublishedDate())
                && Objects.equals(getTemplate(), other.getTemplate())
                && Objects.equals(shouldExcludeFromSearch(), other.shouldExcludeFromSearch())
                && Objects.equals(getSiteSection(), other.getSiteSection());
        }
        return false;
    }
}
