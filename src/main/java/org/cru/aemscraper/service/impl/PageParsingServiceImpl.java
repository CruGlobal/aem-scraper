package org.cru.aemscraper.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.base.Strings;
import org.cru.aemscraper.model.PageData;
import org.cru.aemscraper.model.PageEntity;
import org.cru.aemscraper.service.HtmlParserService;
import org.cru.aemscraper.service.PageParsingService;
import org.cru.aemscraper.util.PageUtil;
import org.cru.aemscraper.util.RunMode;

import javax.ws.rs.core.UriBuilder;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class PageParsingServiceImpl implements PageParsingService {
    HtmlParserService htmlParserService;

    public PageParsingServiceImpl(final HtmlParserService htmlParserService) {
        this.htmlParserService = htmlParserService;
    }

    @Override
    public void parsePages(final PageEntity pageEntity, final RunMode runMode, final Set<PageData> allPageData)
        throws URISyntaxException {

        if (pageEntity.getChildren() != null) {
            for (PageEntity child : pageEntity.getChildren()) {
                parsePages(child, runMode, allPageData);
            }
        }

        String pageUrl = determineUrl(pageEntity);
        PageData pageData = new PageData()
            .withHtmlBody(htmlParserService.parsePage(pageEntity))
            .withContentScore(getContentScore(pageEntity))
            .withTitle(getBasicStringProperty(pageEntity, "dc:title"))
            .withDescription(getBasicStringProperty(pageEntity, "dc:description"))
            // Since this runs against the publisher, this should be fine
            .withPublishedDate(getDateProperty(pageEntity, "cq:lastModified"))
            .withUrl(pageUrl)
            .withTemplate(getTemplate(pageEntity))
            .shouldExcludeFromSearch(getBooleanProperty(pageEntity, "excludeFromSearch"))
            .withTags(getTags(pageEntity.getProperties().entrySet()));

        if (runMode == RunMode.CLOUDSEARCH) {
            pageData = pageData.withImageUrl(getImageUrl(pageEntity, pageUrl));
        }
        allPageData.add(pageData);
    }

    String determineUrl(final PageEntity pageEntity) {
        if (!Strings.isNullOrEmpty(pageEntity.getCanonicalUrl())) {
            return pageEntity.getCanonicalUrl();
        }

        Map<String, String> externalizerMap = new HashMap<>();
        externalizerMap.put("http://uatpub1.aws.cru.org:4503/content/jf/us/en", "https://stage.jesusfilm.org");
        externalizerMap.put("http://uatpub2.aws.cru.org:4503/content/jf/us/en", "https://stage.jesusfilm.org");
        externalizerMap.put("http://prodpub1.aws.cru.org:4503/content/jf/us/en", "https://jesusfilm.org");
        externalizerMap.put("http://prodpub2.aws.cru.org:4503/content/jf/us/en", "https://jesusfilm.org");
        externalizerMap.put("http://uatpub1.aws.cru.org:4503/content/cru/us/en", "https://stage.cru.org/us/en");
        externalizerMap.put("http://uatpub2.aws.cru.org:4503/content/cru/us/en", "https://stage.cru.org/us/en");
        externalizerMap.put("http://prodpub1.aws.cru.org:4503/content/cru/us/en", "https://www.cru.org/us/en");
        externalizerMap.put("http://prodpub2.aws.cru.org:4503/content/cru/us/en", "https://www.cru.org/us/en");

        String contentUrl = PageUtil.getContentUrl(pageEntity);
        if (contentUrl == null) {
            return null;
        }
        contentUrl = contentUrl.replace(".infinity.json", ".html");
        for (Map.Entry<String, String> entry : externalizerMap.entrySet()) {
            if (contentUrl.startsWith(entry.getKey())) {
                return contentUrl.replace(entry.getKey(), entry.getValue());
            }
        }
        return null;
    }

    String getContentScore(final PageEntity pageEntity) {
        if (pageEntity.getProperties() == null) {
            return "NONE";
        }

        Set<Map.Entry<String, Object>> pageProperties = pageEntity.getProperties().entrySet();

        String score = getProperty(pageProperties, "score");

        if (score != null && score.trim().length() > 0) {
            return score;
        }
        List<String> tags = getTags(pageProperties);
        for (String tag : tags) {
            if (tag.startsWith("target-audience:scale-of-belief/")) {
                return tag.substring(tag.lastIndexOf("/") + 1);
            }
        }
        return "NONE";
    }

    String getDateProperty(final PageEntity pageEntity, final String key) {
        String dateString = getBasicStringProperty(pageEntity, key);
        if (dateString == null) {
            return null;
        }

        DateTimeFormatter formatter = DateTimeFormatter.ISO_ZONED_DATE_TIME;
        ZonedDateTime zoned = ZonedDateTime.from(formatter.parse(dateString)).withZoneSameInstant(ZoneId.of("UTC"));
        return DateTimeFormatter.ISO_INSTANT.format(zoned);
    }

    String getBasicStringProperty(final PageEntity pageEntity, final String key) {
        if (pageEntity.getProperties() == null) {
            return null;
        }

        Set<Map.Entry<String, Object>> pageProperties = pageEntity.getProperties().entrySet();
        return getProperty(pageProperties, key);
    }

    boolean getBooleanProperty(final PageEntity pageEntity, final String key) {
        if (pageEntity.getProperties() == null) {
            return false;
        }

        Set<Map.Entry<String, Object>> pageProperties = pageEntity.getProperties().entrySet();
        for (Map.Entry<String, Object> property : pageProperties) {
            if (property.getKey().equals(key)) {
                if (property.getValue() instanceof Boolean) {
                    return (Boolean) property.getValue();
                } else {
                    return Boolean.parseBoolean((String) property.getValue());
                }
            }
        }
        return false;
    }

    String getImageUrl(final PageEntity pageEntity, final String pageUrl) throws URISyntaxException {
        JsonNode jcrContent = pageEntity.getJcrContent();
        if (jcrContent != null) {
            JsonNode imageNode = jcrContent.get("image");
            if (imageNode != null) {
                JsonNode fileReference = imageNode.get("fileReference");
                if (fileReference != null && fileReference.asText() != null) {
                    return buildImageUrl(fileReference.asText(), pageUrl);
                }
            }
        }
        return null;
    }

    private String buildImageUrl(final String imagePath, final String pageUrl) throws URISyntaxException {
        if (pageUrl != null) {
            URI canonicalUrl = new URI(pageUrl);
            return UriBuilder.fromPath(imagePath)
                .scheme(canonicalUrl.getScheme())
                .host(canonicalUrl.getHost())
                .port(canonicalUrl.getPort())
                .build().toString();
        }
        return imagePath;
    }

    String getTemplate(final PageEntity pageEntity) {
        String template = null;
        JsonNode jcrContent = pageEntity.getJcrContent();
        if (jcrContent != null) {
            JsonNode resourceTypeNode = jcrContent.get("sling:resourceType");
            if (resourceTypeNode != null) {
                template = resourceTypeNode.asText();
            }
        }
        return template;
    }

    String getProperty(final Set<Map.Entry<String, Object>> pageProperties, final String key) {
        for (Map.Entry<String, Object> property : pageProperties) {
            if (property.getKey().equals(key)) {
                return property.getValue().toString();
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    List<String> getTags(final Set<Map.Entry<String, Object>> pageProperties) {
        for (Map.Entry<String, Object> property : pageProperties) {
            if (property.getKey().equals("cq:tags")) {
                return (List<String>) property.getValue();
            }
        }
        return new ArrayList<>();
    }
}
