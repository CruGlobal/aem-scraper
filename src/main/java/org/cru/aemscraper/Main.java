package org.cru.aemscraper;

import com.google.common.collect.ImmutableSet;
import org.cru.aemscraper.model.CloudSearchDocument;
import org.cru.aemscraper.model.PageData;
import org.cru.aemscraper.model.PageEntity;
import org.cru.aemscraper.service.AemScraperService;
import org.cru.aemscraper.service.CsvService;
import org.cru.aemscraper.service.HtmlParserService;
import org.cru.aemscraper.service.JsonFileBuilderService;
import org.cru.aemscraper.service.PageParsingService;
import org.cru.aemscraper.service.S3Service;
import org.cru.aemscraper.service.impl.AemScraperServiceImpl;
import org.cru.aemscraper.service.impl.CsvServiceImpl;
import org.cru.aemscraper.service.impl.HtmlParserServiceImpl;
import org.cru.aemscraper.service.impl.JsonFileBuilderServiceImpl;
import org.cru.aemscraper.service.impl.PageParsingServiceImpl;
import org.cru.aemscraper.service.impl.S3ServiceImpl;
import org.cru.aemscraper.util.PageUtil;
import org.cru.aemscraper.util.RunMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.utils.StringUtils;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import java.io.File;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Set;

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

            pageParsingService.parsePages(rootEntity, RunMode.S3, ALL_PAGE_DATA);
            Set<String> desiredTemplates = ImmutableSet.of(
                "CruOrgApp/components/page/article",
                "CruOrgApp/components/page/article-long-form",
                "CruOrgApp/components/page/daily-content",
                "CruOrgApp/components/page/editable/article",
                "CruOrgApp/components/page/summermission",
                "CruOrgApp/components/page/internationalinternship"
            );
            Set<PageData> filteredData = aemScraperService.removeUndesiredTemplates(ALL_PAGE_DATA, desiredTemplates);

            if (type.equals("file")) {
                File csvFile = csvService.createCsvFile(filteredData);

                if (!onlyBuildCsv) {
                    s3Service.sendCsvToS3(csvFile);
                }
            } else if (type.equals("bytes")) {
                byte[] csvBytes = csvService.createCsvBytes(filteredData);

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

        pageParsingService.parsePages(rootEntity, RunMode.CLOUDSEARCH, ALL_PAGE_DATA);

        Set<String> desiredTemplates = ImmutableSet.of(
            "CruOrgApp/components/page/summermission",
            "CruOrgApp/components/page/article",
            "CruOrgApp/components/page/article-long-form",
            "CruOrgApp/components/page/content",
            "CruOrgApp/components/page/daily-content",
            "CruOrgApp/components/page/marketing-content",
            "CruOrgApp/components/page/editable/landing-page",
            "CruOrgApp/components/page/editable/videoplayer-page",
            "JesusFilmApp/components/page/blogpost"
        );
        Set<PageData> filteredPages = aemScraperService.removeUndesiredTemplates(ALL_PAGE_DATA, desiredTemplates);

        JsonFileBuilderService jsonFileBuilderService = new JsonFileBuilderServiceImpl();
        jsonFileBuilderService.buildJsonFiles(filteredPages, type);
    }
}
