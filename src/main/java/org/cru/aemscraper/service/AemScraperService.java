package org.cru.aemscraper.service;

import org.cru.aemscraper.model.PageEntity;

import java.io.IOException;

public interface AemScraperService {
    PageEntity scrape(String pageUrl) throws IOException;
}
