package org.cru.aemscraper.service;

import java.io.File;

public interface S3Service {
    void sendCsvToS3(File csvFile);

    void sendCsvBytesToS3(byte[] csvBytes);
}
