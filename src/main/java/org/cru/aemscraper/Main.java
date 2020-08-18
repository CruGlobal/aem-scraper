package org.cru.aemscraper;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.base.Strings;
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
import javax.ws.rs.core.UriBuilder;
import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
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

        PageUtil.populateJcrContent(rootEntity, client);
        LOG.debug(rootEntity.toString());

        client = ClientBuilder.newBuilder().build();

        parsePages(rootEntity, RunMode.CLOUDSEARCH);

        JsonFileBuilderService jsonFileBuilderService = new JsonFileBuilderServiceImpl();
        jsonFileBuilderService.buildJsonFiles(ALL_PAGE_DATA, type);
    }

    private static void parsePages(final PageEntity pageEntity, final RunMode runMode) throws URISyntaxException {
        if (pageEntity.getChildren() != null) {
            for (PageEntity child : pageEntity.getChildren()) {
                parsePages(child, runMode);
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

    static String getDateProperty(final PageEntity pageEntity, final String key) {
        String dateString = getBasicStringProperty(pageEntity, key);
        if (dateString == null) {
            return null;
        }

        DateTimeFormatter formatter = DateTimeFormatter.ISO_ZONED_DATE_TIME;
        ZonedDateTime zoned = ZonedDateTime.from(formatter.parse(dateString)).withZoneSameInstant(ZoneId.of("UTC"));
        return DateTimeFormatter.ISO_INSTANT.format(zoned);
    }

    static String getBasicStringProperty(final PageEntity pageEntity, final String key) {
        if (pageEntity.getProperties() == null) {
            return null;
        }

        Set<Map.Entry<String, Object>> pageProperties = pageEntity.getProperties().entrySet();
        return getProperty(pageProperties, key);
    }

    static boolean getBooleanProperty(final PageEntity pageEntity, final String key) {
        if (pageEntity.getProperties() == null) {
            return false;
        }

        Set<Map.Entry<String, Object>> pageProperties = pageEntity.getProperties().entrySet();
        for (Map.Entry<String, Object> property : pageProperties) {
            if (property.getKey().equals(key)) {
                return (Boolean) property.getValue();
            }
        }
        return false;
    }

    static String getImageUrl(final PageEntity pageEntity, final String pageUrl) throws URISyntaxException {
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

    private static String buildImageUrl(final String imagePath, final String pageUrl) throws URISyntaxException {
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

    static String determineUrl(final PageEntity pageEntity) {
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

    static String getTemplate(final PageEntity pageEntity) {
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
