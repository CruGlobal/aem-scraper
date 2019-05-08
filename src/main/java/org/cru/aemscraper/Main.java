package org.cru.aemscraper;

import org.cru.aemscraper.model.PageEntity;
import org.cru.aemscraper.service.AemScraperService;
import org.cru.aemscraper.service.HtmlParserService;
import org.cru.aemscraper.service.impl.AemScraperServiceImpl;
import org.cru.aemscraper.service.impl.HtmlParserServiceImpl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Main {
    private static HtmlParserService htmlParserService;
    private static List<String> pagesText = new ArrayList<>();

    public static void main(String args[]) {
        String rootUrl = args[0];

        if (rootUrl == null || rootUrl.isEmpty()) {
            return;
        }

        AemScraperService aemScraperService = new AemScraperServiceImpl();
        htmlParserService = new HtmlParserServiceImpl();

        try {
            PageEntity rootEntity = aemScraperService.scrape(rootUrl);
            rootEntity = aemScraperService.removeNonPages(rootEntity);
            System.out.println(rootEntity);

            parsePages(rootEntity);
            pagesText.forEach(System.out::println);
        } catch (IOException e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
        }
    }

    private static void parsePages(final PageEntity pageEntity) throws IOException {
        if (pageEntity.getChildren() != null) {
            for (PageEntity child : pageEntity.getChildren()) {
                parsePages(child);
            }
        }

        pagesText.add(htmlParserService.parsePage(pageEntity));
    }
}
