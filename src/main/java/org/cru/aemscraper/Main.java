package org.cru.aemscraper;

import org.cru.aemscraper.model.PageEntity;
import org.cru.aemscraper.service.AemScraperService;
import org.cru.aemscraper.service.CsvService;
import org.cru.aemscraper.service.HtmlParserService;
import org.cru.aemscraper.service.S3Service;
import org.cru.aemscraper.service.impl.AemScraperServiceImpl;
import org.cru.aemscraper.service.impl.CsvServiceImpl;
import org.cru.aemscraper.service.impl.HtmlParserServiceImpl;
import org.cru.aemscraper.service.impl.S3ServiceImpl;
import software.amazon.awssdk.utils.StringUtils;

import java.io.File;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Main {
    private static HtmlParserService htmlParserService;
    private static Map<String, String> pageData = new HashMap<>();

    public static void main(String args[]) {
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

        boolean onlySendToS3 = Boolean.valueOf(System.getProperty("onlySendToS3"));

        AemScraperService aemScraperService = new AemScraperServiceImpl();
        htmlParserService = new HtmlParserServiceImpl();
        CsvService csvService = new CsvServiceImpl();
        S3Service s3Service = new S3ServiceImpl(bucketName, keyPrefix);

        try {
            if (onlySendToS3) {
                if (!type.equals("file")) {
                    System.out.println("Must use type \"file\" to only send to S3");
                    return;
                }

                File existingFile = Paths.get(CsvServiceImpl.CSV_FILE).toFile();
                if (existingFile == null) {
                    System.out.println("File does not exist!");
                    return;
                }
                s3Service.sendCsvToS3(existingFile);
            } else {
                PageEntity rootEntity = aemScraperService.scrape(rootUrl);
                rootEntity = aemScraperService.removeNonPages(rootEntity);
                System.out.println(rootEntity);

                parsePages(rootEntity);

                if (type.equals("file")) {
                    File csvFile = csvService.createCsvFile(pageData);
                    s3Service.sendCsvToS3(csvFile);
                } else if (type.equals("bytes")) {
                    byte[] csvBytes = csvService.createCsvBytes(pageData);
                    s3Service.sendCsvBytesToS3(csvBytes);
                }
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
        }
    }

    private static void parsePages(final PageEntity pageEntity) {
        if (pageEntity.getChildren() != null) {
            for (PageEntity child : pageEntity.getChildren()) {
                parsePages(child);
            }
        }

        pageData.put(htmlParserService.parsePage(pageEntity), getContentScore(pageEntity));
    }

    static String getContentScore(final PageEntity pageEntity) {
        if (pageEntity.getProperties() == null) {
            return "NONE";
        }

        for (Map.Entry<String, Object> entry : pageEntity.getProperties().entrySet()) {
            if (entry.getKey().equals("score")) {
                return entry.getValue().toString();
            } else if (entry.getKey().equals("cq:tags")) {
                List<String> tags = (List<String>) entry.getValue();
                for (String tag : tags) {
                    if (tag.startsWith("target-audience:scale-of-belief/")) {
                        return tag.substring(tag.lastIndexOf("/") + 1);
                    }
                }
            }
        }
        return "NONE";
    }
}
