package org.cru.aemscraper.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import jersey.repackaged.com.google.common.collect.Lists;
import org.apache.commons.lang3.StringUtils;
import org.cru.aemscraper.model.PageData;
import org.cru.aemscraper.service.JsonFileBuilderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class JsonFileBuilderServiceImpl implements JsonFileBuilderService {
    public static final String OUTPUT_FILE = "./data.json";
    private static final Logger LOG = LoggerFactory.getLogger(JsonFileBuilderServiceImpl.class);

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public File buildJsonFiles(final Set<PageData> pageData, boolean enforceFileSizeLimit) throws IOException {
        List<PageData> filteredPageData = normalizeSpace(Lists.newArrayList(filterPages(pageData)));
        return enforceFileSizeLimit ? writeFiles(filteredPageData, OUTPUT_FILE) : writeFile(filteredPageData, OUTPUT_FILE);
    }

    private List<PageData> normalizeSpace(final List<PageData> pageDataSet) {
        pageDataSet.forEach(pageData -> pageData.withHtmlBody(StringUtils.normalizeSpace(pageData.getHtmlBody())));

        pageDataSet.forEach(pageData -> pageData.withDescription(StringUtils.normalizeSpace(pageData.getDescription())));

        return pageDataSet;
    }

    private Set<PageData> filterPages(final Set<PageData> pageData) {
        return pageData
                .stream()
                .filter(page -> !page.isExcludeFromSearch())
                .filter(page -> !page.isExcludeFromSearchEngines())
                .collect(Collectors.toSet());
    }

    private File writeFile(final List<PageData> pageDataList, final String fileName) throws IOException {
        File file = new File(fileName);
        objectMapper.writerWithDefaultPrettyPrinter().writeValue(file, pageDataList);
        return file;
    }

    private File writeFiles(final List<PageData> pageDataList, final String fileName) {
        File file = new File(fileName);

        try(BufferedWriter bufferedWriter = Files.newBufferedWriter(file.toPath(), StandardCharsets.UTF_8)) {
            boolean first = true;
            bufferedWriter.write("[");
            for (PageData pageData : pageDataList) {
                try {
                    if(!first) {
                        bufferedWriter.write(",");
                    } else {
                        first = false;
                    }
                    bufferedWriter.newLine();
                    bufferedWriter.write(objectMapper.writer().withDefaultPrettyPrinter().writeValueAsString(pageData));
                }
                catch (IOException e) {
                    LOG.error(e.getMessage(), e);
                    throw new RuntimeException(e);
                }
            }
            bufferedWriter.newLine();
            bufferedWriter.write("]");
        } catch (Exception e) {
            LOG.error("Error writing JSON file.", e);
            throw new RuntimeException(e);
        }

        return file;
    }
}
