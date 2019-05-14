package org.cru.aemscraper.service.impl;

import org.cru.aemscraper.service.S3Service;
import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class S3ServiceImpl implements S3Service {
    private static final Region REGION = Region.US_EAST_1;
    private String bucketName;
    private String keyPrefix;

    private S3Client s3Client;

    public S3ServiceImpl(final String bucketName, final String keyPrefix) {
        this.bucketName = bucketName;
        this.keyPrefix = keyPrefix;
    }

    @Override
    public void sendCsvToS3(final File csvFile) {
        RequestBody requestBody = RequestBody.fromFile(csvFile);
        sendDataToS3(requestBody);
    }

    @Override
    public void sendCsvBytesToS3(final byte[] csvBytes) {
        RequestBody requestBody = RequestBody.fromBytes(csvBytes);
        sendDataToS3(requestBody);
    }

    private void sendDataToS3(final RequestBody requestBody) {
        initClient();

        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
            .bucket(bucketName)
            .key(keyPrefix + "/" + buildFileName())
            .build();

        s3Client.putObject(putObjectRequest, requestBody);
    }

    private void initClient() {
        s3Client = S3Client.builder()
            .region(REGION)
            .credentialsProvider(ProfileCredentialsProvider.create("research"))
            .build();
    }

    private String buildFileName() {
        LocalDateTime now = LocalDateTime.now();
        return now.format(DateTimeFormatter.ISO_DATE_TIME);
    }
}
