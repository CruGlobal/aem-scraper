package org.cru.aemscraper.service;

import java.io.File;

public interface S3Service {
    void sendToS3(File file);

    void sendBytesToS3(byte[] csvBytes);
}
