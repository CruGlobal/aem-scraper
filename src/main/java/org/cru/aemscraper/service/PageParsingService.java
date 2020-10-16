package org.cru.aemscraper.service;

import org.cru.aemscraper.model.PageData;
import org.cru.aemscraper.model.PageEntity;
import org.cru.aemscraper.util.RunMode;

import java.net.URISyntaxException;
import java.util.Set;

public interface PageParsingService {
    void parsePages(PageEntity pageEntity, RunMode runMode, Set<PageData> allPageData) throws URISyntaxException;
}
