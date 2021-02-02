package org.cru.aemscraper;

import com.google.common.collect.ImmutableSet;
import org.cru.aemscraper.model.CloudSearchDocument;
import org.cru.aemscraper.model.PageData;
import org.cru.aemscraper.model.PageEntity;
import org.cru.aemscraper.service.AemScraperService;
import org.cru.aemscraper.service.CsvService;
import org.cru.aemscraper.service.HtmlParserService;
import org.cru.aemscraper.service.JsonCloudSearchFileBuilderService;
import org.cru.aemscraper.service.JsonFileBuilderService;
import org.cru.aemscraper.service.PageParsingService;
import org.cru.aemscraper.service.S3Service;
import org.cru.aemscraper.service.impl.AemScraperServiceImpl;
import org.cru.aemscraper.service.impl.CsvServiceImpl;
import org.cru.aemscraper.service.impl.HtmlParserServiceImpl;
import org.cru.aemscraper.service.impl.JsonCloudSearchFileBuilderServiceImpl;
import org.cru.aemscraper.service.impl.JsonFileBuilderServiceImpl;
import org.cru.aemscraper.service.impl.PageParsingServiceImpl;
import org.cru.aemscraper.service.impl.S3ServiceImpl;
import org.cru.aemscraper.util.PageUtil;
import org.cru.aemscraper.util.RunMode;
import org.cru.aemscraper.util.Template;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.utils.StringUtils;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import java.io.File;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Set;

import static org.cru.aemscraper.util.Template.ARTICLE_LONG_FORM;
import static org.cru.aemscraper.util.Template.BLOG_POST;
import static org.cru.aemscraper.util.Template.CONTENT;
import static org.cru.aemscraper.util.Template.DAILY_CONTENT;
import static org.cru.aemscraper.util.Template.DYNAMIC_ARTICLE;
import static org.cru.aemscraper.util.Template.INTERNATIONAL_INTERNSHIP;
import static org.cru.aemscraper.util.Template.LANDING;
import static org.cru.aemscraper.util.Template.MARKETING_CONTENT;
import static org.cru.aemscraper.util.Template.STATIC_ARTICLE;
import static org.cru.aemscraper.util.Template.SUMMER_MISSION;
import static org.cru.aemscraper.util.Template.VIDEO_PLAYER;

public class Main {
    private static final Logger LOG = LoggerFactory.getLogger(Main.class);

    private static AemScraperService aemScraperService;
    private static PageParsingService pageParsingService;
    private static Client client;

    private static final Set<PageData> ALL_PAGE_DATA = new HashSet<>();

    public static void main(String[] args) {
        String rootUrl = args[0];

        if (StringUtils.isEmpty(rootUrl)) {
            return;
        }

        RunMode runMode = RunMode.fromCode(System.getProperty("runMode"));

        aemScraperService = new AemScraperServiceImpl();
        HtmlParserService htmlParserService = new HtmlParserServiceImpl();
        pageParsingService = new PageParsingServiceImpl(htmlParserService);

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

        String type = "";
        if (args[3] != null) {
            type = args[3];
        }

        boolean onlySendToS3 = Boolean.parseBoolean(System.getProperty("onlySendToS3"));
        boolean onlyBuildOutput = Boolean.parseBoolean(System.getProperty("onlyBuildOutput"));

        S3Service s3Service = new S3ServiceImpl(bucketName, keyPrefix);
        CsvService csvService = new CsvServiceImpl();
        JsonFileBuilderService jsonFileBuilderService = new JsonFileBuilderServiceImpl();

        if (onlySendToS3) {
            if (!type.equals("csvfile") && !type.equals("jsonfile")) {
                LOG.error("Must use type \"[csv,json]file\" to only send to S3");
            } else if (type.equals("csvfile")) {
                File existingFile = Paths.get(CsvServiceImpl.CSV_FILE).toFile();
                s3Service.sendToS3(existingFile);
            } else {
                File existingFile = Paths.get(JsonFileBuilderServiceImpl.OUTPUT_FILE).toFile();
                s3Service.sendToS3(existingFile);
            }
        } else {
            LOG.info("Scraping pages ...");
            PageEntity rootEntity = aemScraperService.scrape(rootUrl);
            LOG.info("Scraping pages ... done {}", rootEntity.getJcrContent());

            LOG.info("Removing non pages ...");
            rootEntity = aemScraperService.removeNonPages(rootEntity);
            LOG.info("Removing non pages ... done {}", rootEntity.getJcrContent());
            LOG.info(rootEntity.toString());

            LOG.info("Parsing pages ...");
            pageParsingService.parsePages(rootEntity, ALL_PAGE_DATA);
            LOG.info("Parsing pages ... done {}", rootEntity.getJcrContent());
            Set<Template> desiredTemplates = ImmutableSet.of(
                STATIC_ARTICLE,
                ARTICLE_LONG_FORM,
                DAILY_CONTENT,
                DYNAMIC_ARTICLE,
                SUMMER_MISSION,
                INTERNATIONAL_INTERNSHIP
            );

            LOG.info("Remove undesired templates ...");
            Set<PageData> filteredData = aemScraperService.removeUndesiredTemplates(ALL_PAGE_DATA, desiredTemplates);
            LOG.info("Remove undesired templates ... done. Filtered data set size {}", filteredData.size());

            if (type.equals("csvfile")) {
                LOG.info("Create csv file ...");
                File csvFile = csvService.createCsvFile(filteredData);
                LOG.info("Create csv file ... done");

                if (!onlyBuildOutput) {
                    LOG.info("Send csv file to S3 ...");
                    s3Service.sendToS3(csvFile);
                    LOG.info("Send csv file to S3 ... done");
                }
            } else if (type.equals("jsonfile")) {
                LOG.info("Create json file ...");
                File jsonFile = jsonFileBuilderService.buildJsonFiles(filteredData);
                LOG.info("Create json file ... done");

                if (!onlyBuildOutput) {
                    LOG.info("Send json file to S3 ...");
                    s3Service.sendToS3(jsonFile);
                    LOG.info("Send json file to S3 ... done");
                }
            } else if (type.equals("bytes")) {
                byte[] csvBytes = csvService.createCsvBytes(filteredData);

                if (!onlyBuildOutput) {
                    LOG.info("Send bytes to S3 ...");
                    s3Service.sendBytesToS3(csvBytes);
                    LOG.info("Send bytes to S3 ... done");
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

        pageParsingService.parsePages(rootEntity, ALL_PAGE_DATA);

        Set<Template> desiredTemplates = ImmutableSet.of(
            SUMMER_MISSION,
            STATIC_ARTICLE,
            ARTICLE_LONG_FORM,
            CONTENT,
            DAILY_CONTENT,
            MARKETING_CONTENT,
            LANDING,
            VIDEO_PLAYER,
            BLOG_POST
        );
        Set<PageData> filteredPages = aemScraperService.removeUndesiredTemplates(ALL_PAGE_DATA, desiredTemplates);

        JsonCloudSearchFileBuilderService jsonCloudSearchFileBuilderService = new JsonCloudSearchFileBuilderServiceImpl();
        jsonCloudSearchFileBuilderService.buildJsonFiles(filteredPages, type);
    }
}
