package org.cru.aemscraper;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.cru.aemscraper.model.CloudSearchDocument;
import org.cru.aemscraper.model.PageData;
import org.cru.aemscraper.model.PageEntity;
import org.cru.aemscraper.service.AemScraperService;
import org.cru.aemscraper.service.CsvService;
import org.cru.aemscraper.service.HtmlParserService;
import org.cru.aemscraper.service.JsonFileBuilderService;
import org.cru.aemscraper.service.S3Service;
import org.cru.aemscraper.service.impl.AemScraperServiceImpl;
import org.cru.aemscraper.service.impl.CsvServiceImpl;
import org.cru.aemscraper.service.impl.HtmlParserServiceImpl;
import org.cru.aemscraper.service.impl.JsonFileBuilderServiceImpl;
import org.cru.aemscraper.service.impl.S3ServiceImpl;
import org.cru.aemscraper.util.PageUtil;
import org.cru.aemscraper.util.RunMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.utils.StringUtils;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.Response;
import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class Main {
    private static final Logger LOG = LoggerFactory.getLogger(Main.class);

    private static HtmlParserService htmlParserService;
    private static AemScraperService aemScraperService;
    private static Client client;

    private static final Set<PageData> ALL_PAGE_DATA = new HashSet<>();

    public static void main(String[] args) {
        String rootUrl = args[0];

        if (StringUtils.isEmpty(rootUrl)) {
            return;
        }

        RunMode runMode = RunMode.fromCode(System.getProperty("runMode"));

        aemScraperService = new AemScraperServiceImpl();
        htmlParserService = new HtmlParserServiceImpl();

        try {
            switch (runMode) {
                case S3:
                    sendToS3(args, rootUrl);
                    break;
                case CLOUDSEARCH:
                    prepareForCloudSearch(rootUrl, args);
                    break;
            }
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
        }
    }

    private static void sendToS3(final String[] args, final String rootUrl) throws Exception {
        String bucketName = args[1];
        String keyPrefix = args[2];

        if (StringUtils.isEmpty(bucketName) || StringUtils.isEmpty(keyPrefix)) {
            return;
        }

        String type = "file";
        if (args[3] != null) {
            type = args[3];
        }

        boolean onlySendToS3 = Boolean.parseBoolean(System.getProperty("onlySendToS3"));
        boolean onlyBuildCsv = Boolean.parseBoolean(System.getProperty("onlyBuildCSV"));

        S3Service s3Service = new S3ServiceImpl(bucketName, keyPrefix);
        CsvService csvService = new CsvServiceImpl();

        if (onlySendToS3) {
            if (!type.equals("file")) {
                LOG.error("Must use type \"file\" to only send to S3");
                return;
            }

            File existingFile = Paths.get(CsvServiceImpl.CSV_FILE).toFile();
            s3Service.sendCsvToS3(existingFile);
        } else {
            PageEntity rootEntity = aemScraperService.scrape(rootUrl);
            rootEntity = aemScraperService.removeNonPages(rootEntity);
            LOG.debug(rootEntity.toString());

            parsePages(rootEntity, RunMode.S3);

            if (type.equals("file")) {
                File csvFile = csvService.createCsvFile(ALL_PAGE_DATA);

                if (!onlyBuildCsv) {
                    s3Service.sendCsvToS3(csvFile);
                }
            } else if (type.equals("bytes")) {
                byte[] csvBytes = csvService.createCsvBytes(ALL_PAGE_DATA);

                if (!onlyBuildCsv) {
                    s3Service.sendCsvBytesToS3(csvBytes);
                }
            }
        }
    }

    private static void prepareForCloudSearch(
        final String rootUrl,
        final String[] args) throws Exception {

        CloudSearchDocument.Type type = CloudSearchDocument.Type.fromCode(args[1]);
        PageEntity rootEntity = aemScraperService.scrape(rootUrl);
        rootEntity = aemScraperService.removeNonPages(rootEntity);
        LOG.debug(rootEntity.toString());

        client = ClientBuilder.newBuilder().build();

        parsePages(rootEntity, RunMode.CLOUDSEARCH);

        JsonFileBuilderService jsonFileBuilderService = new JsonFileBuilderServiceImpl();
        jsonFileBuilderService.buildJsonFiles(ALL_PAGE_DATA, type);
    }

    private static void parsePages(final PageEntity pageEntity, final RunMode runMode) throws IOException {
        if (pageEntity.getChildren() != null) {
            for (PageEntity child : pageEntity.getChildren()) {
                parsePages(child, runMode);
            }
        }

        PageData pageData = new PageData()
            .withHtmlBody(htmlParserService.parsePage(pageEntity))
            .withContentScore(getContentScore(pageEntity))
            .withTitle(getBasicStringProperty(pageEntity, "dc:title"))
            .withDescription(getBasicStringProperty(pageEntity, "dc:description"))
            // Since this runs against the publisher, this should be fine
            .withPublishedDate(getBasicStringProperty(pageEntity, "cq:lastModified"))
            .withUrl(pageEntity.getCanonicalUrl());

        if (runMode == RunMode.CLOUDSEARCH) {
            pageData = pageData.withImageUrl(getImageUrl(pageEntity));
        }
        ALL_PAGE_DATA.add(pageData);
    }

    static String getContentScore(final PageEntity pageEntity) {
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

    static String getBasicStringProperty(final PageEntity pageEntity, final String key) {
        if (pageEntity.getProperties() == null) {
            return null;
        }

        Set<Map.Entry<String, Object>> pageProperties = pageEntity.getProperties().entrySet();
        return getProperty(pageProperties, key);
    }

    static String getImageUrl(final PageEntity pageEntity) throws IOException {
        String contentUrl = PageUtil.getContentUrl(pageEntity);

        if (contentUrl != null && contentUrl.endsWith(".json")) {
            Response response = client.target(contentUrl).request().get();
            String contentJson = response.readEntity(String.class);
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode jsonNode = objectMapper.readValue(contentJson, JsonNode.class);

            if (jsonNode != null) {
                JsonNode jcrContent = jsonNode.get("jcr:content");

                if (jcrContent != null) {
                    JsonNode imageNode = jcrContent.get("image");
                    if (imageNode != null) {
                        return imageNode.get("fileReference").asText();
                    }
                }
            }
        }
        return null;
    }

    static String getProperty(final Set<Map.Entry<String, Object>> pageProperties, final String key) {
        for (Map.Entry<String, Object> property : pageProperties) {
            if (property.getKey().equals(key)) {
                return property.getValue().toString();
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    static List<String> getTags(final Set<Map.Entry<String, Object>> pageProperties) {
        for (Map.Entry<String, Object> property : pageProperties) {
            if (property.getKey().equals("cq:tags")) {
                return (List<String>) property.getValue();
            }
        }
        return new ArrayList<>();
    }
}
