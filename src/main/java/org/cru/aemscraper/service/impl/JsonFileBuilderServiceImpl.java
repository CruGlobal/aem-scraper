package org.cru.aemscraper.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import jersey.repackaged.com.google.common.collect.Lists;
import org.cru.aemscraper.model.CloudSearchAddDocument;
import org.cru.aemscraper.model.CloudSearchDeleteDocument;
import org.cru.aemscraper.model.CloudSearchDocument;
import org.cru.aemscraper.model.PageData;
import org.cru.aemscraper.service.JsonFileBuilderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class JsonFileBuilderServiceImpl implements JsonFileBuilderService {
    private static final Logger LOG = LoggerFactory.getLogger(JsonFileBuilderServiceImpl.class);

    private static final String FILE_PREFIX = "./cloudsearch-data";
    private static final String FILE_TYPE = ".json";
    private static final String START_ARRAY = "[";
    private static final String END_ARRAY = "]";
    private static final String DELIMITER = ",";
    private static final Long FIVE_MB = (long) (5 * 1024 * 1024);

    @Override
    public void buildJsonFiles(final Set<PageData> pageData, final CloudSearchDocument.Type type) {
        writeFiles(0, 0, Lists.newArrayList(pageData), type);
    }

    private void writeFiles(
        final int fileIndex,
        final int dataIndex,
        final List<PageData> pageData,
        final CloudSearchDocument.Type type) {

        try {
            File file = buildFile(fileIndex);
            try (BufferedWriter bufferedWriter = Files.newBufferedWriter(file.toPath(), StandardCharsets.UTF_8)) {
                bufferedWriter.write(START_ARRAY);
                ObjectMapper objectMapper = new ObjectMapper();
                for (int i = dataIndex; i < pageData.size(); i++) {
                    PageData data = pageData.get(i);
                    CloudSearchDocument cloudSearchDocument = buildDocument(type, data);

                    String json = objectMapper.writeValueAsString(cloudSearchDocument);
                    if (hasRoom(bufferedWriter, json)) {
                        if (i > 0) {
                            bufferedWriter.append(DELIMITER);
                        }
                        bufferedWriter.append(json);
                    }
                    else {
                        bufferedWriter.append(END_ARRAY);
                        bufferedWriter.flush();
                        writeFiles(fileIndex + 1, i + 1, pageData, type);
                    }
                }
                bufferedWriter.append(END_ARRAY);
                bufferedWriter.flush();
            }
        } catch (IOException e) {
            LOG.error(e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    private CloudSearchDocument buildDocument(final CloudSearchDocument.Type type, final PageData data) {
        CloudSearchDocument cloudSearchDocument;
        switch (type) {
            case ADD:
                cloudSearchDocument = new CloudSearchAddDocument();
                cloudSearchDocument.setFields(buildFieldsFromData(data));
                break;
            case DELETE:
                cloudSearchDocument = new CloudSearchDeleteDocument();
                break;
            default:
                throw new IllegalArgumentException("Illegal type " + type);
        }

        cloudSearchDocument.setId(data.getUrl());
        return cloudSearchDocument;
    }

    private File buildFile(final int index) throws IOException {
        File file;
        if (index == 0) {
            file = new File(FILE_PREFIX + FILE_TYPE);
        } else {
            file = new File(FILE_PREFIX + "-" + index + FILE_TYPE);
        }
        if (!file.exists()) {
            boolean created = file.createNewFile();
            if (!created) {
                throw new RuntimeException("Couldn't create file at " + FILE_PREFIX + FILE_TYPE);
            }
        }
        return file;
    }

    private boolean hasRoom(final BufferedWriter writer, final String json) {
        return writer.toString().getBytes(StandardCharsets.UTF_8).length
            + json.getBytes(StandardCharsets.UTF_8).length
            + DELIMITER.getBytes(StandardCharsets.UTF_8).length
            + END_ARRAY.getBytes(StandardCharsets.UTF_8).length < FIVE_MB;
    }

    private Map<String, String> buildFieldsFromData(final PageData data) {
        Map<String, String> fields = new HashMap<>();
        if (data.getTags() != null) {
            fields.put("tags", Arrays.toString(data.getTags().toArray()));
        }
        if (data.getTitle() != null) {
            fields.put("title", data.getTitle());
        }
        if (data.getDescription() != null) {
            fields.put("description", data.getDescription());
        }
        if (data.getImageUrl() != null) {
            fields.put("image_url", data.getImageUrl());
        }
        return fields;
    }
}
