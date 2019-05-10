package org.cru.aemscraper.service;

import java.io.IOException;
import java.util.Map;

public interface CsvService {
    void createCsvFile(Map<String, String> pageData) throws IOException;

    byte[] createCsvBytes(Map<String, String> pageData) throws IOException;
}
