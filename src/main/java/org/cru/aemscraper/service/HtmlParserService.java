package org.cru.aemscraper.service;

import org.cru.aemscraper.model.PageEntity;

import java.io.IOException;

public interface HtmlParserService {
    String parsePage(PageEntity pageEntity) throws IOException;
}
