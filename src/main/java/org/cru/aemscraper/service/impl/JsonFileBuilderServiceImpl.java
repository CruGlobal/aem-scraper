package org.cru.aemscraper.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import jersey.repackaged.com.google.common.collect.Lists;
import org.cru.aemscraper.model.PageData;
import org.cru.aemscraper.service.JsonFileBuilderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class JsonFileBuilderServiceImpl implements JsonFileBuilderService {
    public static final String OUTPUT_FILE = "./data.json";
    private static final Logger LOG = LoggerFactory.getLogger(JsonFileBuilderServiceImpl.class);

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public File buildJsonFiles(final Set<PageData> pageData) throws IOException {
        Set<PageData> filtered = filterPages(pageData);
        return writeFile(Lists.newArrayList(filtered), OUTPUT_FILE);
    }

    Set<PageData> filterPages(final Set<PageData> pageData) {
        return pageData
                .stream()
                .filter(page -> !page.shouldExcludeFromSearch())
                .collect(Collectors.toSet());
    }

    private File writeFile(final List<PageData> pageDataList, final String fileName) {
        FileWriter fileWriter = null;
        BufferedWriter bufferedWriter = null;
        try {
            fileWriter = new FileWriter(fileName);
            bufferedWriter = new BufferedWriter(fileWriter);
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
        } finally {
            try {
                if(bufferedWriter != null) {
                    bufferedWriter.flush();
                    bufferedWriter.close();
                }
            }
            catch (IOException e) {
                e.printStackTrace();
            }
            try {
                if(fileWriter != null) {
                    fileWriter.close();
                }
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        }

        return new File(fileName);
    }
}
