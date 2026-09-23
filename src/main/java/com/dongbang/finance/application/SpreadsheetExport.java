package com.dongbang.finance.application;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public final class SpreadsheetExport {
    private SpreadsheetExport() {}

    public static byte[] csv(List<List<String>> rows) {
        StringBuilder csv = new StringBuilder("\uFEFF");
        for (List<String> row : rows) {
            for (int i = 0; i < row.size(); i++) {
                if (i > 0) csv.append(',');
                String value = safeCsvValue(row.get(i)).replace("\"", "\"\"");
                csv.append('"').append(value).append('"');
            }
            csv.append("\r\n");
        }
        return csv.toString().getBytes(StandardCharsets.UTF_8);
    }

    public static byte[] xlsx(List<List<String>> rows) {
        try {
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            try (ZipOutputStream zip = new ZipOutputStream(output)) {
                entry(zip, "[Content_Types].xml", """
                        <?xml version="1.0" encoding="UTF-8"?>
                        <Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
                          <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
                          <Default Extension="xml" ContentType="application/xml"/>
                          <Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>
                          <Override PartName="/xl/worksheets/sheet1.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
                        </Types>""");
                entry(zip, "_rels/.rels", """
                        <?xml version="1.0" encoding="UTF-8"?>
                        <Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
                          <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/>
                        </Relationships>""");
                entry(zip, "xl/workbook.xml", """
                        <?xml version="1.0" encoding="UTF-8"?>
                        <workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
                          <sheets><sheet name="DongBang" sheetId="1" r:id="rId1"/></sheets>
                        </workbook>""");
                entry(zip, "xl/_rels/workbook.xml.rels", """
                        <?xml version="1.0" encoding="UTF-8"?>
                        <Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
                          <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet1.xml"/>
                        </Relationships>""");
                StringBuilder sheet = new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\"?><worksheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\"><sheetData>");
                for (int rowIndex = 0; rowIndex < rows.size(); rowIndex++) {
                    sheet.append("<row r=\"").append(rowIndex + 1).append("\">");
                    List<String> row = rows.get(rowIndex);
                    for (int col = 0; col < row.size(); col++) {
                        sheet.append("<c r=\"").append(column(col)).append(rowIndex + 1)
                                .append("\" t=\"inlineStr\"><is><t>")
                                .append(xml(row.get(col))).append("</t></is></c>");
                    }
                    sheet.append("</row>");
                }
                sheet.append("</sheetData></worksheet>");
                entry(zip, "xl/worksheets/sheet1.xml", sheet.toString());
            }
            return output.toByteArray();
        } catch (Exception ex) {
            throw new IllegalStateException("엑셀 파일 생성에 실패했습니다.", ex);
        }
    }

    private static void entry(ZipOutputStream zip, String name, String value) throws Exception {
        zip.putNextEntry(new ZipEntry(name));
        zip.write(value.getBytes(StandardCharsets.UTF_8));
        zip.closeEntry();
    }

    private static String column(int index) {
        StringBuilder result = new StringBuilder();
        int current = index;
        do { result.insert(0, (char) ('A' + current % 26)); current = current / 26 - 1; } while (current >= 0);
        return result.toString();
    }

    private static String xml(String value) {
        if (value == null) return "";
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&apos;");
    }

    private static String safeCsvValue(String value) {
        if (value == null) return "";
        String stripped = value.stripLeading();
        if (stripped.startsWith("=") || stripped.startsWith("+") || stripped.startsWith("-") || stripped.startsWith("@")) {
            return "'" + value;
        }
        return value;
    }
}
