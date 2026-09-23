package com.dongbang.finance.application;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.assertj.core.api.Assertions.assertThat;

class SpreadsheetExportTest {
    private final List<List<String>> rows = List.of(
            List.of("항목", "금액"),
            List.of("정기 납부", "40000")
    );

    @Test
    void createsUtf8Csv() {
        String csv = new String(SpreadsheetExport.csv(rows), StandardCharsets.UTF_8);
        assertThat(csv).startsWith("\uFEFF");
        assertThat(csv).contains("\"정기 납부\",\"40000\"");
    }

    @Test
    void createsMinimalValidXlsxPackage() throws Exception {
        Set<String> entries = new HashSet<>();
        try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(SpreadsheetExport.xlsx(rows)))) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) entries.add(entry.getName());
        }
        assertThat(entries).contains("[Content_Types].xml", "xl/workbook.xml", "xl/worksheets/sheet1.xml");
    }
}
