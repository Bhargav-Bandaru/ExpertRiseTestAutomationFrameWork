package com.expertrise.automation.utils;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.*;
import java.util.*;

/**
 * ExcelUtil — Apache POI helper for reading and writing .xlsx files.
 *
 * Used for data-driven testing where inputs are stored in Excel sheets.
 *
 * Usage:
 *   // Read all rows as List of Maps (header → value)
 *   List&lt;Map&lt;String,String&gt;&gt; rows = ExcelUtil.readSheet("testdata/login.xlsx", "Sheet1");
 *   String email = rows.get(0).get("email");
 *
 *   // Read specific cell by column header name
 *   String pwd = ExcelUtil.getCellValue("testdata/login.xlsx", "Sheet1", 0, "password");
 *
 *   // Get as TestNG @DataProvider 2D array
 *   Object[][] data = ExcelUtil.getSheetAsDataProvider("testdata/login.xlsx", "Sheet1");
 *
 *   // Write result back to Excel
 *   ExcelUtil.setCellValue("testdata/results.xlsx", "Results", 1, 3, "PASS");
 */
public class ExcelUtil {

    private static final Logger log = LogManager.getLogger(ExcelUtil.class);

    // ── READ ───────────────────────────────────────────────────────────────────

    /**
     * Read all data rows from a sheet.
     * Row 0 is treated as the header row.
     *
     * @param filePath  path to .xlsx file (relative to project root)
     * @param sheetName target sheet name
     * @return List of rows, each as Map{columnHeader → cellValue}
     */
    public static List<Map<String, String>> readSheet(String filePath, String sheetName) {
        List<Map<String, String>> data = new ArrayList<>();
        try (FileInputStream fis = new FileInputStream(filePath);
             Workbook wb = new XSSFWorkbook(fis)) {

            Sheet sheet = wb.getSheet(sheetName);
            if (sheet == null)
                throw new RuntimeException("Sheet '" + sheetName + "' not found in: " + filePath);

            Row header = sheet.getRow(0);
            if (header == null) return data;

            List<String> headers = new ArrayList<>();
            for (Cell c : header) headers.add(getCellString(c));

            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;
                Map<String, String> rowMap = new LinkedHashMap<>();
                for (int j = 0; j < headers.size(); j++) {
                    Cell cell = row.getCell(j);
                    rowMap.put(headers.get(j), cell == null ? "" : getCellString(cell));
                }
                data.add(rowMap);
            }
            log.info("Excel: read {} rows from '{}' in '{}'", data.size(), sheetName, filePath);
        } catch (IOException e) {
            throw new RuntimeException("Failed to read Excel: " + filePath, e);
        }
        return data;
    }

    /**
     * Read a specific cell by column header name.
     *
     * @param filePath   path to .xlsx file
     * @param sheetName  sheet name
     * @param rowIndex   0-based data row index (0 = first data row, not header)
     * @param columnName exact column header name
     */
    public static String getCellValue(String filePath, String sheetName,
                                      int rowIndex, String columnName) {
        List<Map<String, String>> rows = readSheet(filePath, sheetName);
        if (rowIndex >= rows.size())
            throw new RuntimeException("Row " + rowIndex + " out of range (" + rows.size() + " rows)");
        String val = rows.get(rowIndex).get(columnName);
        if (val == null)
            throw new RuntimeException("Column '" + columnName + "' not found in sheet '" + sheetName + "'");
        return val;
    }

    /**
     * Read a cell by row and column index (both 0-based, skipping header row).
     */
    public static String getCellValueByIndex(String filePath, String sheetName,
                                              int rowIndex, int colIndex) {
        try (FileInputStream fis = new FileInputStream(filePath);
             Workbook wb = new XSSFWorkbook(fis)) {
            Sheet sheet = wb.getSheet(sheetName);
            if (sheet == null) throw new RuntimeException("Sheet not found: " + sheetName);
            Row row = sheet.getRow(rowIndex + 1); // +1 skips header
            if (row == null) throw new RuntimeException("Row " + rowIndex + " is empty");
            Cell cell = row.getCell(colIndex);
            return cell == null ? "" : getCellString(cell);
        } catch (IOException e) {
            throw new RuntimeException("Failed to read cell: " + filePath, e);
        }
    }

    /**
     * Return sheet data as Object[][] for use with TestNG @DataProvider.
     */
    public static Object[][] getSheetAsDataProvider(String filePath, String sheetName) {
        List<Map<String, String>> rows = readSheet(filePath, sheetName);
        if (rows.isEmpty()) return new Object[0][0];
        List<String> headers = new ArrayList<>(rows.get(0).keySet());
        Object[][] result = new Object[rows.size()][headers.size()];
        for (int i = 0; i < rows.size(); i++) {
            for (int j = 0; j < headers.size(); j++)
                result[i][j] = rows.get(i).getOrDefault(headers.get(j), "");
        }
        log.info("DataProvider: {}x{} from '{}'/'{}'", result.length, headers.size(), filePath, sheetName);
        return result;
    }

    /** Return number of data rows in a sheet (not counting header row). */
    public static int getRowCount(String filePath, String sheetName) {
        try (FileInputStream fis = new FileInputStream(filePath);
             Workbook wb = new XSSFWorkbook(fis)) {
            Sheet sheet = wb.getSheet(sheetName);
            return sheet == null ? 0 : sheet.getLastRowNum();
        } catch (IOException e) {
            throw new RuntimeException("Failed to get row count: " + filePath, e);
        }
    }

    // ── WRITE ──────────────────────────────────────────────────────────────────

    /**
     * Write a string value to a specific cell (0-based row and column).
     * Reads the file, updates the cell, writes back to same path.
     */
    public static void setCellValue(String filePath, String sheetName,
                                    int rowIndex, int colIndex, String value) {
        try (FileInputStream fis = new FileInputStream(filePath);
             Workbook wb = new XSSFWorkbook(fis)) {
            Sheet sheet = wb.getSheet(sheetName);
            if (sheet == null) throw new RuntimeException("Sheet not found: " + sheetName);
            Row row = sheet.getRow(rowIndex);
            if (row == null) row = sheet.createRow(rowIndex);
            Cell cell = row.getCell(colIndex);
            if (cell == null) cell = row.createCell(colIndex);
            cell.setCellValue(value);
            log.info("Excel: set [{},{}]='{}' in sheet '{}'", rowIndex, colIndex, value, sheetName);
            try (FileOutputStream fos = new FileOutputStream(filePath)) {
                wb.write(fos);
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to write Excel: " + filePath, e);
        }
    }

    /**
     * Create a new Excel report file with bold headers and auto-sized columns.
     * Use this to write test execution results back to Excel.
     *
     * @param filePath  output .xlsx path (parent directories created automatically)
     * @param sheetName sheet name
     * @param headers   column header names
     * @param dataRows  each inner List is one row of data values
     */
    public static void createExcelReport(String filePath, String sheetName,
                                         List<String> headers, List<List<String>> dataRows) {
        try (Workbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet(sheetName);

            // Bold style for header row
            CellStyle boldStyle = wb.createCellStyle();
            Font font = wb.createFont();
            font.setBold(true);
            boldStyle.setFont(font);

            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.size(); i++) {
                Cell c = headerRow.createCell(i);
                c.setCellValue(headers.get(i));
                c.setCellStyle(boldStyle);
            }

            for (int i = 0; i < dataRows.size(); i++) {
                Row row = sheet.createRow(i + 1);
                List<String> rowData = dataRows.get(i);
                for (int j = 0; j < rowData.size(); j++)
                    row.createCell(j).setCellValue(rowData.get(j));
            }

            for (int i = 0; i < headers.size(); i++) sheet.autoSizeColumn(i);

            new File(filePath).getParentFile().mkdirs();
            try (FileOutputStream fos = new FileOutputStream(filePath)) {
                wb.write(fos);
                log.info("Excel report created: {} ({} rows)", filePath, dataRows.size());
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to create Excel report: " + filePath, e);
        }
    }

    // ── INTERNAL ───────────────────────────────────────────────────────────────

    /** Convert any cell type to a trimmed String value. */
    private static String getCellString(Cell cell) {
        if (cell == null) return "";
        return switch (cell.getCellType()) {
            case STRING  -> cell.getStringCellValue().trim();
            case NUMERIC -> {
                if (DateUtil.isCellDateFormatted(cell))
                    yield cell.getLocalDateTimeCellValue().toString();
                double v = cell.getNumericCellValue();
                yield (v == Math.floor(v)) ? String.valueOf((long) v) : String.valueOf(v);
            }
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            case FORMULA -> {
                try { yield cell.getStringCellValue(); }
                catch (Exception e) { yield String.valueOf(cell.getNumericCellValue()); }
            }
            default -> "";
        };
    }

    private ExcelUtil() {}
}
