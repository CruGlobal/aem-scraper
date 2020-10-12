package org.cru.aemscraper.service.impl;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.cru.aemscraper.model.PageData;
import org.cru.aemscraper.service.CsvService;

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Set;

public class CsvServiceImpl implements CsvService {
    public static final String CSV_FILE = "./data.csv";
    @Override
    public File createCsvFile(final Set<PageData> allPageData) throws IOException {
        BufferedWriter writer = Files.newBufferedWriter(Paths.get(CSV_FILE));
        CSVPrinter csvPrinter = new CSVPrinter(writer, CSVFormat.DEFAULT);

        for (PageData entry : allPageData) {
            if (!entry.getHtmlBody().isEmpty()) {
                csvPrinter.printRecord(
                    entry.getContentScore(),
                    entry.getHtmlBody(),
                    entry.getTitle(),
                    entry.getDescription(),
                    entry.getImageUrl(),
                    entry.getTags(),
                    entry.getUrl());
            }
        }

        csvPrinter.flush();
        csvPrinter.close();

        return Paths.get(CSV_FILE).toFile();
    }

    @Override
    public byte[] createCsvBytes(final Set<PageData> pageData) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(out));
        CSVPrinter csvPrinter = new CSVPrinter(writer, CSVFormat.DEFAULT);

        for (PageData entry : pageData) {
            csvPrinter.printRecord(entry.getContentScore(), entry.getHtmlBody());
        }

        csvPrinter.flush();
        return out.toByteArray();
    }
}
