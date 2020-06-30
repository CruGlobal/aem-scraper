package org.cru.aemscraper;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import org.cru.aemscraper.model.PageEntity;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

public class MainTest {
    @Test
    public void testGetContentScoreWithScoreProperty() {
        Map<String, Object> properties = new HashMap<>();
        properties.put("score", 4);

        PageEntity page = new PageEntity()
            .withProperties(properties);

        String score = Main.getContentScore(page);
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

        String score = Main.getContentScore(page);
        assertThat(score, is(equalTo("6")));
    }

    @Test
    public void testGetImageUrlWithPort() throws Exception {
        String canonicalUrl = "http://localhost:4503/us/en/page.html";
        String expectedResult = "http://localhost:4503/content/dam/some/image.jpg";
        testGetImageUrl(canonicalUrl, expectedResult);
    }

    @Test
    public void testGetImageUrlWithoutPort() throws Exception {
        String canonicalUrl = "https://app.com/us/en/page.html";
        String expectedResult = "https://app.com/content/dam/some/image.jpg";
        testGetImageUrl(canonicalUrl, expectedResult);
    }

    private void testGetImageUrl(final String canonicalUrl, final String expectedResult) throws Exception {
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
        pageEntity.setCanonicalUrl(canonicalUrl);

        String imageUrl = Main.getImageUrl(pageEntity);
        assertThat(imageUrl, is(equalTo(expectedResult)));
    }
}
