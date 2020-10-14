package org.cru.aemscraper.service.impl;

import jersey.repackaged.com.google.common.collect.ImmutableList;
import jersey.repackaged.com.google.common.collect.Iterables;
import jersey.repackaged.com.google.common.collect.Lists;
import jersey.repackaged.com.google.common.collect.Sets;
import org.cru.aemscraper.model.Link;
import org.cru.aemscraper.model.PageData;
import org.cru.aemscraper.model.PageEntity;
import org.cru.aemscraper.util.Template;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.cru.aemscraper.util.Template.CONTENT;
import static org.cru.aemscraper.util.Template.DYNAMIC_ARTICLE;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasEntry;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AemScraperServiceImplTest {
    private static final String BASE_URL = "http://localhost:4503/api/content/sites/mine/us/en";
    private static final String ROOT_URL = BASE_URL + ".json";

    private static final List<String> PAGE_TYPE = ImmutableList.of("content/page");

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
    void testGetPageEntity() throws Exception {
        PageEntity rootEntity = aemScraperService.getPageEntity(ROOT_URL);

        assertThat(rootEntity, is(not(nullValue())));
        assertThat(rootEntity.getChildren().size(), is(equalTo(1)));
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
    void testHasMorePages() {
        Link nextPageLink = new Link()
            .withHref("http://some-page/api/content/next.json")
            .withRel(Lists.newArrayList("next"));
        PageEntity pageEntity = new PageEntity()
            .withLinks(Lists.newArrayList(nextPageLink));

        assertThat(aemScraperService.hasMorePages(pageEntity), is(equalTo(true)));
    }

    @Test
    void testDoesNotHaveMorePages() {
        Link selfPageLink = new Link()
            .withHref("http://some-page/api/content/self.json")
            .withRel(Lists.newArrayList("self"));
        PageEntity pageEntity = new PageEntity()
            .withLinks(Lists.newArrayList(selfPageLink));

        assertThat(aemScraperService.hasMorePages(pageEntity), is(equalTo(false)));
    }

    @Test
    void testScrape() throws Exception {
        String childUrl = BASE_URL + "/child.json";
        String grandchildUrl = BASE_URL + "/child/grandchild.json";

        Response mockResponse = mock(Response.class);
        when(mockResponse.readEntity(String.class)).thenReturn(buildChildJson());
        mockClient(childUrl, mockResponse);

        Response mockGrandchildResponse = mock(Response.class);
        when(mockGrandchildResponse.readEntity(String.class)).thenReturn(buildGrandchildJson());
        mockClient(grandchildUrl, mockGrandchildResponse);

        PageEntity rootEntity = aemScraperService.scrape(ROOT_URL);
        assertThat(rootEntity, is(not(nullValue())));
        assertThat(rootEntity.getChildren().size(), is(equalTo(1)));
        PageEntity child = Iterables.getOnlyElement(rootEntity.getChildren());
        assertThat(child.getChildren().size(), is(equalTo(1)));
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

    @Test
    void testRemoveNonPages() {
        PageEntity pageGrandchild = new PageEntity().withClassType(PAGE_TYPE);
        PageEntity nonPageGrandchild = new PageEntity().withClassType(ImmutableList.of("core/services"));

        List<PageEntity> allGrandchildren = Lists.newArrayList(pageGrandchild, nonPageGrandchild);

        PageEntity pageChildWithChildren = new PageEntity()
            .withClassType(PAGE_TYPE)
            .withChildren(allGrandchildren);

        PageEntity pageChild = new PageEntity().withClassType(PAGE_TYPE);
        PageEntity nonPageChild = new PageEntity().withClassType(ImmutableList.of("core/services"));

        List<PageEntity> allChildren = Lists.newArrayList(pageChild, pageChildWithChildren, nonPageChild);

        PageEntity rootEntity = new PageEntity()
            .withChildren(allChildren)
            .withClassType(PAGE_TYPE);

        List<PageEntity> expectedGrandchildren = Lists.newArrayList(pageGrandchild);
        PageEntity expectedChildWithChildren = new PageEntity()
            .withClassType(PAGE_TYPE)
            .withChildren(expectedGrandchildren);

        List<PageEntity> expectedChildren = Lists.newArrayList(pageChild, expectedChildWithChildren);

        PageEntity expected = new PageEntity()
            .withChildren(expectedChildren)
            .withClassType(PAGE_TYPE);

        PageEntity filteredEntity = aemScraperService.removeNonPages(rootEntity);

        assertDeepEquals(expected, filteredEntity);
    }

    @Test
    public void testRemoveUndesiredPages() {
        Template desiredTemplate = DYNAMIC_ARTICLE;

        PageData desiredPage = new PageData().withTemplate(desiredTemplate.getPath());
        PageData undesiredPage = new PageData().withTemplate(CONTENT.getPath());

        Set<PageData> allPages = Sets.newHashSet(desiredPage, undesiredPage);
        Set<PageData> expectedFiltered = Sets.newHashSet(desiredPage);

        Set<Template> desiredTemplates = Sets.newHashSet(desiredTemplate);
        Set<PageData> filtered = aemScraperService.removeUndesiredTemplates(allPages, desiredTemplates);

        assertThat(expectedFiltered, is(equalTo(filtered)));
    }

    private void assertDeepEquals(final PageEntity expected, final PageEntity actual) {
        if (expected.getClassType() == null) {
            assertThat(actual.getClassType() == null, is(equalTo(true)));
        } else {
            assertThat(expected.getClassType(), hasItems(actual.getClassType().toArray(new String[0])));
        }

        if (expected.getLinks() == null) {
            assertThat(actual.getLinks() == null, is(equalTo(true)));
        } else {
            assertThat(expected.getLinks(), hasItems(actual.getLinks().toArray(new Link[0])));
        }

        if (expected.getProperties() == null) {
            assertThat(actual.getProperties() == null, is(equalTo(true)));
        } else {
            for (Map.Entry<String, Object> entry : expected.getProperties().entrySet()) {
                assertThat(actual.getProperties(), hasEntry(entry.getKey(), entry.getValue()));
            }
        }

        if (expected.getChildren() == null) {
            assertThat(actual.getChildren() == null, is(equalTo(true)));
        } else {
            int i = 0;
            for (PageEntity child : expected.getChildren()) {
                assertDeepEquals(child, actual.getChildren().get(i));
                i++;
            }
        }
    }
}
