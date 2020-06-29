package org.cru.aemscraper.service.impl;

import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import org.cru.aemscraper.model.CloudSearchAddDocument;
import org.cru.aemscraper.model.CloudSearchDocument;
import org.cru.aemscraper.model.PageData;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

public class JsonFileBuilderServiceImplTest {
    private final JsonFileBuilderServiceImpl jsonFileBuilderService = new JsonFileBuilderServiceImpl();

    @Test
    public void testBuildSingleObjectFile() throws Exception {
        PageData pageData = buildSinglePageData();

        Set<PageData> allData = new HashSet<>();
        allData.add(pageData);

        jsonFileBuilderService.buildJsonFiles(allData, CloudSearchDocument.Type.ADD);

        MappingIterator<CloudSearchAddDocument> readData = verifyAndReadFile();

        CloudSearchAddDocument onlyData = readData.next();
        assertThat(onlyData.getType(), is(equalTo("add")));
        assertThat(onlyData.getId(), is(equalTo(pageData.getUrl())));

        Map<String, String> fields = onlyData.getFields();
        assertThat(fields.get("tags"), is(equalTo(Arrays.toString(pageData.getTags().toArray()))));
        assertThat(fields.get("title"), is(equalTo(pageData.getTitle())));
        assertThat(fields.get("description"), is(equalTo(pageData.getDescription())));
        assertThat(fields.get("image_url"), is(equalTo(pageData.getImageUrl())));
        assertThat(fields.get("published_date"), is(equalTo(pageData.getPublishedDate())));
        assertThat(fields.get("path"), is(equalTo(pageData.getUrl())));
    }

    @Test
    public void testBuildMultiObjectFile() throws Exception {
        List<String> tags = new ArrayList<>();
        tags.add("tag1");
        tags.add("tag2");

        String url1 = "http://localhost:4503/content/app/us/en/category/article.html";
        PageData pageData = new PageData()
            .withHtmlBody("Lorem Ipsum...")
            .withTags(tags)
            .withUrl(url1);

        String url2 = "http://localhost:4503/content/app/us/en/category/article2.html";
        PageData anotherPage = new PageData()
            .withHtmlBody("Foo Ipsum...")
            .withUrl(url2);

        Set<PageData> allData = new HashSet<>();
        allData.add(pageData);
        allData.add(anotherPage);

        jsonFileBuilderService.buildJsonFiles(allData, CloudSearchDocument.Type.ADD);

        MappingIterator<CloudSearchAddDocument> readData = verifyAndReadFile();

        int numberOfDocuments = 0;
        while(readData.hasNext()) {
            numberOfDocuments++;
            CloudSearchAddDocument document = readData.next();
            assertThat(document.getType(), is(equalTo("add")));

            Map<String, String> fields = document.getFields();
            if (document.getId().equals(url1)) {
                assertThat(fields.get("tags"), is(equalTo(Arrays.toString(tags.toArray()))));
            } else if (document.getId().equals(url2)) {
                assertThat(fields.get("tags"), is(nullValue()));
            }
        }
        assertThat(numberOfDocuments, is(equalTo(2)));
    }

    @Test
    public void testManyDocuments() throws Exception {
        Set<PageData> allData = new HashSet<>();

        for (int i = 0; i < 14_000; i++) {
            allData.add(buildSinglePageData());
        }

        jsonFileBuilderService.buildJsonFiles(allData, CloudSearchDocument.Type.ADD);

        verifyAndReadFile();
        verifyAndReadFile("./cloudsearch-data-1.json");
    }

    private PageData buildSinglePageData() {
        List<String> tags = new ArrayList<>();
        tags.add("tag1");
        tags.add("tag2");

        String url = "http://localhost:4503/content/app/us/en/category/article.html";
        String title = "Some Content";
        String description = "Some description";
        String imageUrl = "http://localhost:4503/content/dam/app/image.png";
        String publishedDate = "2020-05-30T15:46:03.720Z";

        return new PageData()
            .withHtmlBody("Lorem Ipsum...")
            .withTags(tags)
            .withTitle(title)
            .withDescription(description)
            .withImageUrl(imageUrl)
            .withPublishedDate(publishedDate)
            .withUrl(url);
    }

    private MappingIterator<CloudSearchAddDocument> verifyAndReadFile() throws Exception {
        return verifyAndReadFile("./cloudsearch-data.json");
    }

    private MappingIterator<CloudSearchAddDocument> verifyAndReadFile(final String fileName) throws Exception {
        File outputFile = new File(fileName);
        assertThat(outputFile, is(not(nullValue())));

        ObjectReader objectReader = new ObjectMapper().readerFor(CloudSearchAddDocument.class);
        MappingIterator<CloudSearchAddDocument> readData = objectReader.readValues(outputFile);
        assertThat(readData, is(not(nullValue())));

        return readData;
    }
}
