package org.cru.aemscraper;

import org.cru.aemscraper.model.PageEntity;
import org.cru.aemscraper.service.AemScraperService;
import org.cru.aemscraper.service.impl.AemScraperServiceImpl;

import java.io.IOException;

public class Main {
    public static void main(String args[]) {
        String rootUrl = args[0];

        if (rootUrl == null || rootUrl.isEmpty()) {
            return;
        }

        AemScraperService aemScraperService = new AemScraperServiceImpl();

        try {
            PageEntity rootEntity = aemScraperService.scrape(rootUrl);
            System.out.println(rootEntity);
        } catch (IOException e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
        }
    }
}
