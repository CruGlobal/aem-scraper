package org.cru.aemscraper;

import org.cru.aemscraper.model.PageData;
import org.cru.aemscraper.model.PageEntity;
import org.cru.aemscraper.service.AemScraperService;
import org.cru.aemscraper.service.CsvService;
import org.cru.aemscraper.service.HtmlParserService;
import org.cru.aemscraper.service.S3Service;
import org.cru.aemscraper.service.impl.AemScraperServiceImpl;
import org.cru.aemscraper.service.impl.CsvServiceImpl;
import org.cru.aemscraper.service.impl.HtmlParserServiceImpl;
import org.cru.aemscraper.service.impl.S3ServiceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.utils.StringUtils;

import java.io.File;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class Main {
    private static final Logger LOG = LoggerFactory.getLogger(Main.class);

    private static HtmlParserService htmlParserService;
    private static Set<PageData> allPageData = new HashSet<>();

    public static void main(String[] args) {
        String rootUrl = args[0];

        if (StringUtils.isEmpty(rootUrl)) {
            return;
        }

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

        AemScraperService aemScraperService = new AemScraperServiceImpl();
        htmlParserService = new HtmlParserServiceImpl();
        CsvService csvService = new CsvServiceImpl();
        S3Service s3Service = new S3ServiceImpl(bucketName, keyPrefix);

        try {
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

                parsePages(rootEntity);

                if (type.equals("file")) {
                    File csvFile = csvService.createCsvFile(allPageData);

                    if (!onlyBuildCsv) {
                        s3Service.sendCsvToS3(csvFile);
                    }
                } else if (type.equals("bytes")) {
                    byte[] csvBytes = csvService.createCsvBytes(allPageData);

                    if (!onlyBuildCsv) {
                        s3Service.sendCsvBytesToS3(csvBytes);
                    }
                }
            }
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
        }
    }

    private static void parsePages(final PageEntity pageEntity) {
        if (pageEntity.getChildren() != null) {
            for (PageEntity child : pageEntity.getChildren()) {
                parsePages(child);
            }
        }

        allPageData.add(
            new PageData()
                .withHtmlBody(htmlParserService.parsePage(pageEntity))
                .withContentScore(getContentScore(pageEntity)));
    }

    static String getContentScore(final PageEntity pageEntity) {
        if (pageEntity.getProperties() == null) {
            return "NONE";
        }

        Set<Map.Entry<String, Object>> pageProperties = pageEntity.getProperties().entrySet();

        String score = getScoreProperty(pageProperties);

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

    static String getScoreProperty(final Set<Map.Entry<String, Object>> pageProperties) {
        for (Map.Entry<String, Object> property : pageProperties) {
            if (property.getKey().equals("score")) {
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
