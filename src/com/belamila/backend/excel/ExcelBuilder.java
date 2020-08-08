package com.belamila.backend.excel;

import com.belamila.model.Package;
import org.apache.poi.ss.usermodel.CellCopyPolicy;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.xssf.usermodel.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by: Bartosz Nawrot
 * Date: 19.05.2020
 * Description:
 */
public class ExcelBuilder {

    private static final Logger logger = LoggerFactory.getLogger(ExcelBuilder.class);

    private void setCell(XSSFCell cell, CellStyle style, String value) {
        cell.setCellStyle(style);
        cell.setCellValue(value);
    }

    private void fillTemplate(XSSFWorkbook workbook, XSSFSheet sheet, List<Package> packages) {
        CellCopyPolicy.Builder b = new CellCopyPolicy.Builder();
        b.cellFormula(true);
        b.cellStyle(true);
        b.cellValue(true);
        for (int i = 1; i < packages.size(); i++) {
            sheet.copyRows(3, 3, 3 + i, b.build());
        }

        CellStyle style = workbook.createCellStyle();
        style.setFillForegroundColor(IndexedColors.LIGHT_CORNFLOWER_BLUE.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        AtomicInteger rowCounter = new AtomicInteger(3);
        packages.forEach(p -> {
            XSSFRow row = sheet.getRow(rowCounter.getAndIncrement());
            setCell(row.createCell(0), style, p.getService());
            setCell(row.createCell(16), style, p.getReceiver());
            setCell(row.createCell(17), style, p.getAddress());
            setCell(row.createCell(19), style, p.getZip());
            setCell(row.createCell(20), style, p.getCity());
            setCell(row.createCell(21), style, p.getReceiver().split(" ", 2)[0]);
            setCell(row.createCell(22), style, p.getEmail());
            setCell(row.createCell(23), style, p.getPhone());
        });
    }

    public int buildAndSafe(List<Package> packages, File input) throws IOException {
        String name = input.getName();
        File output = new File(input.getParent()
                + "\\" + name.substring(0, name.lastIndexOf('.')) + "-apaczka.xlsx");
        logger.info("Building excel file at {}, input: {}", output, packages);

        FileInputStream file = new FileInputStream(new File("template.xlsx"));
        XSSFWorkbook workbook = new XSSFWorkbook(file);
        XSSFSheet sheet = workbook.getSheetAt(0);

        fillTemplate(workbook, sheet, packages);

        FileOutputStream out = new FileOutputStream(output);
        workbook.write(out);
        out.close();

        return packages.size();
    }
}
