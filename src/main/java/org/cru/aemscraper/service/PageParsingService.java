package org.cru.aemscraper.service;

import org.cru.aemscraper.model.PageData;
import org.cru.aemscraper.model.PageEntity;

import java.net.URISyntaxException;
import java.util.Set;

public interface PageParsingService {
    void parsePages(PageEntity pageEntity, Set<PageData> allPageData) throws URISyntaxException;
}
