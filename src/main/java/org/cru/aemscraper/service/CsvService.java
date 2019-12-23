package org.cru.aemscraper.service;

import org.cru.aemscraper.model.PageData;

import java.io.File;
import java.io.IOException;
import java.util.Set;

public interface CsvService {
    File createCsvFile(Set<PageData> pageData) throws IOException;

    byte[] createCsvBytes(Set<PageData> pageData) throws IOException;
}
