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
        List<String> tags = new ArrayList<>();
        tags.add("tag1");
        tags.add("tag2");

        String url = "http://localhost:4503/content/app/us/en/category/article.html";

        PageData pageData = new PageData()
            .withHtmlBody("<html><head><title>Wee</title></head><body>Lorem Ipsum...</body></html>")
            .withTags(tags)
            .withUrl(url);

        Set<PageData> allData = new HashSet<>();
        allData.add(pageData);

        jsonFileBuilderService.buildJsonFiles(allData, CloudSearchDocument.Type.ADD);

        MappingIterator<CloudSearchAddDocument> readData = verifyAndReadFile();

        CloudSearchAddDocument onlyData = readData.next();
        assertThat(onlyData.getType(), is(equalTo("add")));
        assertThat(onlyData.getId(), is(equalTo(url)));

        Map<String, String> fields = onlyData.getFields();
        assertThat(fields.get("tags"), is(equalTo(Arrays.toString(tags.toArray()))));
    }

    @Test
    public void testBuildMultiObjectFile() throws Exception {
        List<String> tags = new ArrayList<>();
        tags.add("tag1");
        tags.add("tag2");

        String url1 = "http://localhost:4503/content/app/us/en/category/article.html";
        PageData pageData = new PageData()
            .withHtmlBody("<html><head><title>Wee</title></head><body>Lorem Ipsum...</body></html>")
            .withTags(tags)
            .withUrl(url1);

        String url2 = "http://localhost:4503/content/app/us/en/category/article2.html";
        PageData anotherPage = new PageData()
            .withHtmlBody("<html><head><title>Foo</title></head><body>Lorem Ipsum...</body></html>")
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

    private MappingIterator<CloudSearchAddDocument> verifyAndReadFile() throws Exception {
        File outputFile = new File("./cloudsearch-data.json");
        assertThat(outputFile, is(not(nullValue())));

        ObjectReader objectReader = new ObjectMapper().readerFor(CloudSearchAddDocument.class);
        MappingIterator<CloudSearchAddDocument> readData = objectReader.readValues(outputFile);
        assertThat(readData, is(not(nullValue())));

        return readData;
    }
}
