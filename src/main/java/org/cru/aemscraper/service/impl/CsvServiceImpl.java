package org.cru.aemscraper.service.impl;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.cru.aemscraper.service.CsvService;

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;

public class CsvServiceImpl implements CsvService {
    private static final String CSV_FILE = "./data.csv";
    @Override
    public void createCsvFile(final Map<String, String> pageData) throws IOException {
        BufferedWriter writer = Files.newBufferedWriter(Paths.get(CSV_FILE));
        CSVPrinter csvPrinter = new CSVPrinter(writer, CSVFormat.DEFAULT);

        for (Map.Entry<String, String> entry : pageData.entrySet()) {
            csvPrinter.printRecord(entry.getValue(), entry.getKey());
        }

        csvPrinter.flush();
    }

    @Override
    public byte[] createCsvBytes(final Map<String, String> pageData) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(out));
        CSVPrinter csvPrinter = new CSVPrinter(writer, CSVFormat.DEFAULT);

        for (Map.Entry<String, String> entry : pageData.entrySet()) {
            csvPrinter.printRecord(entry.getValue(), entry.getKey());
        }

        csvPrinter.flush();
        return out.toByteArray();
    }
}
