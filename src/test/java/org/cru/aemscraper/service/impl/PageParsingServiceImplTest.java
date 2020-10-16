package org.cru.aemscraper.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import jersey.repackaged.com.google.common.collect.Lists;
import org.cru.aemscraper.Main;
import org.cru.aemscraper.model.Link;
import org.cru.aemscraper.model.PageEntity;
import org.cru.aemscraper.service.HtmlParserService;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

public class PageParsingServiceImplTest {
    @Mock
    private HtmlParserService htmlParserService;

    private PageParsingServiceImpl pageParsingService = new PageParsingServiceImpl(htmlParserService);

    @Test
    public void testGetContentScoreWithScoreProperty() {
        Map<String, Object> properties = new HashMap<>();
        properties.put("score", 4);

        PageEntity page = new PageEntity()
            .withProperties(properties);

        String score = pageParsingService.getContentScore(page);
        assertThat(score, is(equalTo("4")));
    }

    @Test
    public void testGetContentScoreWithTag() {
        List<String> tagList = new ArrayList<>();
        tagList.add("secular-topics:holidays/mothers-day");
        tagList.add("target-audience:scale-of-belief/6");

        Map<String, Object> properties = new HashMap<>();
        properties.put("cq:tags", tagList);

        PageEntity page = new PageEntity()
            .withProperties(properties);

        String score = pageParsingService.getContentScore(page);
        assertThat(score, is(equalTo("6")));

    }

    @Test
    public void testGetImageUrlWithPort() throws Exception {
        String nonCanonicalUrl = "http://localhost:4503/us/en/page.html";
        String expectedResult = "http://localhost:4503/content/dam/some/image.jpg";
        testGetImageUrl(nonCanonicalUrl, expectedResult, false);
    }

    @Test
    public void testGetImageUrlWithoutPort() throws Exception {
        String canonicalUrl = "https://app.com/us/en/page.html";
        String expectedResult = "https://app.com/content/dam/some/image.jpg";
        testGetImageUrl(canonicalUrl, expectedResult, true);
    }

    private void testGetImageUrl(final String pageUrl, final String expectedResult, final boolean isCanonical)
        throws Exception {

        JsonNodeFactory nodeFactory = JsonNodeFactory.instance;
        JsonNode jcrContent = nodeFactory.objectNode()
            .set(
                "image",
                nodeFactory.objectNode()
                    .put(
                        "fileReference",
                        "/content/dam/some/image.jpg")
            );
        PageEntity pageEntity = new PageEntity();
        pageEntity.setJcrContent(jcrContent);

        if (isCanonical) {
            pageEntity.setCanonicalUrl(pageUrl);
        }

        String imageUrl = pageParsingService.getImageUrl(pageEntity, pageUrl);
        assertThat(imageUrl, is(equalTo(expectedResult)));
    }

    @Test
    public void testDetermineUrlReturnsCanonicalUrl() {
        String canonicalUrl = "https://some-site.org/page.html";
        PageEntity pageEntity = new PageEntity();
        pageEntity.setCanonicalUrl(canonicalUrl);

        assertThat(pageParsingService.determineUrl(pageEntity), is(equalTo(canonicalUrl)));
    }

    @Test
    public void testDetermineUrlReturnsFallbackUrl() {
        String contentUrl = "http://uatpub1.aws.cru.org:4503/content/cru/us/en/page.infinity.json";
        String canonicalUrl = "https://stage.cru.org/us/en/page.html";

        Link contentLink = new Link().withRel(Lists.newArrayList("content")).withHref(contentUrl);
        PageEntity pageEntity = new PageEntity().withLinks(Lists.newArrayList(contentLink));

        assertThat(pageParsingService.determineUrl(pageEntity), is(equalTo(canonicalUrl)));
    }

    @Test
    public void testGetDateProperty() {
        String originalDate = "2015-09-27T17:34:24.007-04:00";
        Map<String, Object> properties = new HashMap<>();
        properties.put("someDate", originalDate);
        PageEntity pageEntity = new PageEntity().withProperties(properties);

        String expectedDate = "2015-09-27T21:34:24.007Z";
        assertThat(pageParsingService.getDateProperty(pageEntity, "someDate"), is(equalTo(expectedDate)));
    }
}
