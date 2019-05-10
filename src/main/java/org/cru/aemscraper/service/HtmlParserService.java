package org.cru.aemscraper.service;

import org.cru.aemscraper.model.PageEntity;

public interface HtmlParserService {
    String parsePage(PageEntity pageEntity);
}
