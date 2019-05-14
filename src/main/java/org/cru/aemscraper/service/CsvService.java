package org.cru.aemscraper.service;

import java.io.File;
import java.io.IOException;
import java.util.Map;

public interface CsvService {
    File createCsvFile(Map<String, String> pageData) throws IOException;

    byte[] createCsvBytes(Map<String, String> pageData) throws IOException;
}
