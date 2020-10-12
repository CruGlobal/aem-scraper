package org.cru.aemscraper.service;

import org.cru.aemscraper.model.PageData;
import org.cru.aemscraper.model.PageEntity;

import java.io.IOException;
import java.util.Set;

public interface AemScraperService {
    PageEntity scrape(String pageUrl) throws IOException;

    PageEntity removeNonPages(PageEntity pageEntity);

    Set<PageData> removeUndesiredTemplates(Set<PageData> pageData, Set<String> desiredTemplates);
}
