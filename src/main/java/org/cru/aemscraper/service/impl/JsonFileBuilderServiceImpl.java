package org.cru.aemscraper.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.hash.HashCode;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;
import jersey.repackaged.com.google.common.collect.Lists;
import jersey.repackaged.com.google.common.collect.Sets;
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
import java.util.HashSet;
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
    // Just under 5MB? but 1000 was 5.01MB according to IntelliJ and AWS can only handle batches of up to 5MB
    private static final Long FIVE_MB = (long) (5 * 1000 * 995);
    private static final int ID_MAX_LENGTH = 128; // Maximum characters for IDs being sent to CloudSearch

    private static final Set<String> DESIRED_TEMPLATES = Sets.newHashSet(
        "CruOrgApp/components/page/summermission",
        "CruOrgApp/components/page/article",
        "CruOrgApp/components/page/article-long-form",
        "CruOrgApp/components/page/content",
        "CruOrgApp/components/page/daily-content",
        "CruOrgApp/components/page/marketing-content",
        "CruOrgApp/components/page/editable/landing-page",
        "CruOrgApp/components/page/editable/videoplayer-page",
        "JesusFilmApp/components/page/blogpost"
    );

    @Override
    public void buildJsonFiles(final Set<PageData> pageData, final CloudSearchDocument.Type type) {
        Set<PageData> filtered = removeUndesiredTemplates(pageData, DESIRED_TEMPLATES);
        writeFiles(0, 0, Lists.newArrayList(filtered), type);
    }

    Set<PageData> removeUndesiredTemplates(final Set<PageData> pageData, final Set<String> desiredTemplates) {
        Set<PageData> filtered = new HashSet<>();
        for (PageData data : pageData) {
            if (desiredTemplates.contains(data.getTemplate())) {
                filtered.add(data);
            }
        }

        return filtered;
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
                    if (hasRoom(file, json)) {
                        if (i > dataIndex) {
                            bufferedWriter.append(DELIMITER);
                        }
                        bufferedWriter.append(json);
                    }
                    else {
                        bufferedWriter.flush();
                        writeFiles(fileIndex + 1, i + 1, pageData, type);
                        break;
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

        cloudSearchDocument.setId(hashIdIfNecessary(data.getUrl()));
        return cloudSearchDocument;
    }

    /**
     * Turns the ID into an MD5 hash if it is longer than the maximum valid ID length.
     */
    private String hashIdIfNecessary(final String id) {
        // Only hash IDs that are too long because of content that is already in CloudSearch with URL IDs.
        if (id.length() > ID_MAX_LENGTH) {
            return getMd5Hash(id);
        }
        return id;
    }

    private String getMd5Hash(String id) {
        // We are using MD5 hashing already for this in AEM, so we should continue using it here for consistency.
        @SuppressWarnings("deprecation")
        HashFunction hashFunction = Hashing.md5();
        HashCode hashCode = hashFunction.hashString(id, StandardCharsets.UTF_8);
        return hashCode.toString();
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

    private boolean hasRoom(final File file, final String json) {
        return file.length()
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
        if (data.getHtmlBody() != null) {
            fields.put("body", data.getHtmlBody());
        }
        if (data.getPublishedDate() != null) {
            fields.put("published_date", data.getPublishedDate());
        }
        fields.put("path", data.getUrl());
        return fields;
    }
}
