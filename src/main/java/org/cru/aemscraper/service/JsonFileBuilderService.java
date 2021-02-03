package org.cru.aemscraper.service;

import org.cru.aemscraper.model.PageData;

import java.io.File;
import java.io.IOException;
import java.util.Set;

public interface JsonFileBuilderService {
    File buildJsonFiles(Set<PageData> pageData, boolean enforceFileSizeLimit) throws IOException;
}
