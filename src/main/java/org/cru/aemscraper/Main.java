package org.cru.aemscraper;

import org.cru.aemscraper.model.PageEntity;
import org.cru.aemscraper.service.AemScraperService;
import org.cru.aemscraper.service.CsvService;
import org.cru.aemscraper.service.HtmlParserService;
import org.cru.aemscraper.service.impl.AemScraperServiceImpl;
import org.cru.aemscraper.service.impl.CsvServiceImpl;
import org.cru.aemscraper.service.impl.HtmlParserServiceImpl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Main {
    private static HtmlParserService htmlParserService;
//    private static List<String> pagesText = new ArrayList<>();
    private static Map<String, String> pageData = new HashMap<>();

    public static void main(String args[]) {
        String rootUrl = args[0];

        if (rootUrl == null || rootUrl.isEmpty()) {
            return;
        }

        String type = "file";
        if (args[1] != null) {
            type = args[1];
        }

        AemScraperService aemScraperService = new AemScraperServiceImpl();
        htmlParserService = new HtmlParserServiceImpl();
        CsvService csvService = new CsvServiceImpl();

        try {
            PageEntity rootEntity = aemScraperService.scrape(rootUrl);
            rootEntity = aemScraperService.removeNonPages(rootEntity);
            System.out.println(rootEntity);

            parsePages(rootEntity);
//            pagesText.forEach(System.out::println);

            if (type.equals("file")) {
                csvService.createCsvFile(pageData);
            } else if (type.equals("bytes")) {
                byte[] csvBytes = csvService.createCsvBytes(pageData);
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

//        pagesText.add(htmlParserService.parsePage(pageEntity));
        pageData.put(htmlParserService.parsePage(pageEntity), getContentScore(pageEntity));
    }

    private static String getContentScore(final PageEntity pageEntity) {
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
                        return tag.substring(tag.lastIndexOf("/"));
                    }
                }
            }
        }
        return "NONE";
    }
}
