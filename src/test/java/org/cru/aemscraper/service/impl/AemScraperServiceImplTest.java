package org.cru.aemscraper.service.impl;

import jersey.repackaged.com.google.common.collect.Iterables;
import jersey.repackaged.com.google.common.collect.Lists;
import org.cru.aemscraper.model.Link;
import org.cru.aemscraper.model.PageEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AemScraperServiceImplTest {
    private static final String BASE_URL = "http://localhost:4503/api/content/sites/mine/us/en";
    private static final String ROOT_URL = BASE_URL + ".json";

    private AemScraperServiceImpl aemScraperService;
    private Client mockClient;

    @BeforeEach
    void setup() {
        mockClient = mock(Client.class);
        aemScraperService = new AemScraperServiceImpl(mockClient);

        Response mockResponse = mock(Response.class);
        when(mockResponse.readEntity(String.class)).thenReturn(buildRootJson());
        mockClient(ROOT_URL, mockResponse);
    }

    @Test
    public void testGetPageEntity() throws Exception {
        PageEntity rootEntity = aemScraperService.getPageEntity(ROOT_URL);

        assertNotNull(rootEntity);
        assertEquals(1, rootEntity.getChildren().size());
    }

    private String buildRootJson() {
        return "{" +
            "\"entities\": [" +
                "{" +
                    "\"links\": [" +
                        "{" +
                            "\"rel\": [" +
                                "\"self\"" +
                            "]," +
                            "\"href\": \"" + BASE_URL + "/child.json\"" +
                        "}," +
                        "{" +
                            "\"rel\": [" +
                                "\"content\"" +
                            "]," +
                            "\"href\": \"" + BASE_URL + "/child.html\"" +
                        "}" +
                    "]," +
                    "\"class\": [" +
                        "\"content/page\"" +
                    "]," +
                    "\"properties\": {" +
                        "\"dc:description\": \"Child description here\"," +
                        "\"dc:title\": \"Child Title\"," +
                        "\"name\": \"child\"" +
                    "}" +
                "}" +
            "]," +
            "\"links\": [" +
                "{" +
                    "\"rel\": [" +
                        "\"self\"" +
                    "]," +
                    "\"href\": \"" + ROOT_URL + "\"" +
                "}," +
                "{" +
                    "\"rel\": [" +
                        "\"content\"" +
                    "]," +
                    "\"href\": \"" + BASE_URL + ".html\"" +
                "}" +
            "]," +
            "\"class\": [" +
                "\"content/page\"" +
            "]," +
            "\"properties\": {" +
                "\"dc:description\": \"Root description here\"," +
                "\"dc:title\": \"Root Title\"," +
                "\"name\": \"en\"," +
                "\"srn:paging\": {" +
                    "\"total\": 1," +
                    "\"offset\": 0," +
                    "\"limit\": 20" +
                "}" +
            "}" +
        "}";
    }

    @Test
    public void testHasMorePages() {
        Link nextPageLink = new Link()
            .withHref("http://some-page/api/content/next.json")
            .withRel(Lists.newArrayList("next"));
        PageEntity pageEntity = new PageEntity()
            .withLinks(Lists.newArrayList(nextPageLink));

        assertTrue(aemScraperService.hasMorePages(pageEntity));
    }

    @Test
    public void testDoesNotHaveMorePages() {
        Link selfPageLink = new Link()
            .withHref("http://some-page/api/content/self.json")
            .withRel(Lists.newArrayList("self"));
        PageEntity pageEntity = new PageEntity()
            .withLinks(Lists.newArrayList(selfPageLink));

        assertFalse(aemScraperService.hasMorePages(pageEntity));
    }

    @Test
    public void testScrape() throws Exception {
        String childUrl = BASE_URL + "/child.json";
        String grandchildUrl = BASE_URL + "/child/grandchild.json";

        Response mockResponse = mock(Response.class);
        when(mockResponse.readEntity(String.class)).thenReturn(buildChildJson());
        mockClient(childUrl, mockResponse);

        Response mockGrandchildResponse = mock(Response.class);
        when(mockGrandchildResponse.readEntity(String.class)).thenReturn(buildGrandchildJson());
        mockClient(grandchildUrl, mockGrandchildResponse);

        PageEntity rootEntity = aemScraperService.scrape(ROOT_URL);
        assertNotNull(rootEntity);
        assertEquals(1, rootEntity.getChildren().size());
        PageEntity child = Iterables.getOnlyElement(rootEntity.getChildren());
        assertEquals(1, child.getChildren().size());
    }

    private String buildChildJson() {
        return "{" +
            "\"entities\": [" +
                "{" +
                    "\"links\": [" +
                        "{" +
                            "\"rel\": [" +
                                "\"self\"" +
                            "]," +
                            "\"href\": \"" + BASE_URL + "/child/grandchild.json\"" +
                        "}," +
                        "{" +
                            "\"rel\": [" +
                                "\"content\"" +
                            "]," +
                            "\"href\": \"" + BASE_URL + "/child/grandchild.html\"" +
                        "}" +
                    "]," +
                    "\"class\": [" +
                        "\"content/page\"" +
                    "]," +
                    "\"properties\": {" +
                        "\"dc:description\": \"Grandchild description here\"," +
                        "\"dc:title\": \"Grandchild Title\"," +
                        "\"name\": \"grandchild\"" +
                    "}" +
                "}" +
            "]," +
            "\"links\": [" +
                "{" +
                    "\"rel\": [" +
                        "\"self\"" +
                    "]," +
                    "\"href\": \"" + BASE_URL + "/child.json\"" +
                "}," +
                "{" +
                    "\"rel\": [" +
                        "\"content\"" +
                    "]," +
                    "\"href\": \"" + BASE_URL + "/child.html\"" +
                "}," +
                "{" +
                    "\"rel\": [" +
                        "\"parent\"" +
                    "]," +
                    "\"href\": \"" + ROOT_URL + "\"" +
                "}" +
            "]," +
            "\"class\": [" +
                "\"content/page\"" +
            "]," +
            "\"properties\": {" +
                "\"dc:description\": \"Child description here\"," +
                "\"dc:title\": \"Child Title\"," +
                "\"name\": \"child\"," +
                "\"srn:paging\": {" +
                    "\"total\": 1," +
                    "\"offset\": 0," +
                    "\"limit\": 20" +
                "}" +
            "}" +
        "}";
    }

    private String buildGrandchildJson() {
        return "{" +
            "\"links\": [" +
                "{" +
                    "\"rel\": [" +
                        "\"self\"" +
                    "]," +
                    "\"href\": \"" + BASE_URL + "/child/grandchild.json\"" +
                "}," +
                "{" +
                    "\"rel\": [" +
                        "\"content\"" +
                    "]," +
                    "\"href\": \"" + BASE_URL + "/child/grandchild.html\"" +
                "}," +
                "{" +
                    "\"rel\": [" +
                        "\"parent\"" +
                    "]," +
                    "\"href\": \"" + BASE_URL + "/child.json\"" +
                "}" +
            "]," +
            "\"class\": [" +
                "\"content/page\"" +
            "]," +
            "\"properties\": {" +
                "\"dc:description\": \"Grandchild description here\"," +
                "\"dc:title\": \"Grandchild Title\"," +
                "\"name\": \"grandchild\"," +
                "\"srn:paging\": {" +
                    "\"total\": 0," +
                    "\"offset\": 0," +
                    "\"limit\": 20" +
                "}" +
            "}" +
        "}";
    }

    private void mockClient(final String url, final Response response) {
        Invocation.Builder mockBuilder = mock(Invocation.Builder.class);
        when(mockBuilder.get()).thenReturn(response);

        WebTarget mockTarget = mock(WebTarget.class);
        when(mockTarget.request()).thenReturn(mockBuilder);

        when(mockClient.target(url)).thenReturn(mockTarget);
    }
}
