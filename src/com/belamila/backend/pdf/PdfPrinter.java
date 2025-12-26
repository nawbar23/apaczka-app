package com.belamila.backend.pdf;

import com.belamila.model.Package;
import com.itextpdf.text.*;
import com.itextpdf.text.pdf.BaseFont;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import com.itextpdf.text.pdf.draw.LineSeparator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class PdfPrinter {

    private static final Logger logger = LoggerFactory.getLogger(PdfPrinter.class);

    private static final Font TITLE_FONT;
    private static final Font HEADER_FONT;
    private static final Font NORMAL_FONT;
    private static final Font BOLD_FONT;
    private static final Font SMALL_FONT;
    private static final Font ITALIC_FONT;

    static {
        try {
            // Użyj wbudowanej czcionki z obsługą polskich znaków
            BaseFont baseFont = BaseFont.createFont("C:/Windows/Fonts/arial.ttf", BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
            TITLE_FONT = new Font(baseFont, 18, Font.BOLD);
            HEADER_FONT = new Font(baseFont, 12, Font.BOLD);
            NORMAL_FONT = new Font(baseFont, 10, Font.NORMAL);
            BOLD_FONT = new Font(baseFont, 10, Font.BOLD);
            SMALL_FONT = new Font(baseFont, 9, Font.NORMAL);
            ITALIC_FONT = new Font(baseFont, 9, Font.ITALIC);
        } catch (Exception e) {
            throw new RuntimeException("Nie udało się załadować czcionki", e);
        }
    }

    public void printSummary(List<Package> packages, String path) {
        String timestamp = new SimpleDateFormat("dd-MM-yyyy HH_mm").format(new Date());
        String fileName = "Podsumawanie paczek " + timestamp + ".pdf";
        String fullPath = Paths.get(path, fileName).toString();

        Document document = new Document(PageSize.A4, 50, 50, 50, 50);

        try {
            PdfWriter.getInstance(document, Files.newOutputStream(Paths.get(fullPath)));
            document.open();

            Paragraph date = new Paragraph("Wygenerowano: " +
                    new SimpleDateFormat("dd.MM.yyyy HH:mm").format(new Date()), SMALL_FONT);
            date.setAlignment(Element.ALIGN_RIGHT);
            date.setSpacingAfter(20);
            document.add(date);

            for (int i = 0; i < packages.size(); i++) {
                Package pkg = packages.get(i);

                if (i > 0) {
                    document.add(Chunk.NEWLINE);
                    document.add(new LineSeparator());
                    document.add(Chunk.NEWLINE);
                }

                addPackageInfo(document, pkg);
                addItemsTable(document, pkg);
            }

            logger.info("PDF generated: {}", fullPath);

        } catch (Exception e) {
            logger.error("Error during PDF: {}", e.getMessage(), e);
            throw new RuntimeException("Nie udało się wygenerować podsumowania PDF", e);
        } finally {
            document.close();
        }
    }

    private void addPackageInfo(Document document, Package pkg) throws DocumentException {
        Paragraph packageHeader = new Paragraph("Numer zamówienia: " + pkg.getId(), HEADER_FONT);
        packageHeader.setSpacingAfter(10);
        document.add(packageHeader);

        Paragraph label = new Paragraph("Metoda dostawy: " + pkg.getShippingLabel(), BOLD_FONT);
        label.setSpacingAfter(10);
        document.add(label);
    }

    private void addItemsTable(Document document, Package pkg) throws DocumentException {
        PdfPTable table = new PdfPTable(3);
        table.setWidthPercentage(100);
        table.setSpacingAfter(10);
        table.setWidths(new float[]{6, 2, 2});

        // Nagłówki kolumn
        addTableHeader(table, "Nazwa");
        addTableHeader(table, "Ilość");
        addTableHeader(table, "Cena (PLN)");

        // Wiersze z przedmiotami
        double totalSum = 0.0;
        for (Package.Item item : pkg.getItems()) {
            // Nazwa
            String itemLabel = item.getLabel() != null ? item.getLabel() : "-";
            // Dodaj adnotację o torbie jeśli wymagana
            if (item.getIsBagRequested() != null && item.getIsBagRequested()) {
                itemLabel += " (wymagana torba)";
            }

            PdfPCell nameCell = new PdfPCell(new Phrase(itemLabel, NORMAL_FONT));
            nameCell.setBorder(Rectangle.BOX);
            nameCell.setPadding(5);
            table.addCell(nameCell);

            // Ilość
            PdfPCell qtyCell = new PdfPCell(new Phrase(
                    item.getQuantity() != null ? item.getQuantity().toString() : "0", NORMAL_FONT
            ));
            qtyCell.setBorder(Rectangle.BOX);
            qtyCell.setPadding(5);
            qtyCell.setHorizontalAlignment(Element.ALIGN_CENTER);
            table.addCell(qtyCell);

            // Cena
            String priceText = item.getTotal() != null ?
                    String.format("%.2f", item.getTotal()) : "0.00";
            PdfPCell priceCell = new PdfPCell(new Phrase(priceText, NORMAL_FONT));
            priceCell.setBorder(Rectangle.BOX);
            priceCell.setPadding(5);
            priceCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
            table.addCell(priceCell);

            if (item.getTotal() != null) {
                totalSum += item.getTotal();
            }
        }

        // Wiersz sumaryczny
        PdfPCell totalLabelCell = new PdfPCell(new Phrase("SUMA:", HEADER_FONT));
        totalLabelCell.setBorder(Rectangle.BOX);
        totalLabelCell.setPadding(5);
        totalLabelCell.setColspan(2);
        totalLabelCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        table.addCell(totalLabelCell);

        PdfPCell totalValueCell = new PdfPCell(new Phrase(
                String.format("%.2f", totalSum), HEADER_FONT
        ));
        totalValueCell.setBorder(Rectangle.BOX);
        totalValueCell.setPadding(5);
        totalValueCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        table.addCell(totalValueCell);

        document.add(table);
    }

    private void addTableHeader(PdfPTable table, String text) {
        PdfPCell header = new PdfPCell(new Phrase(text, HEADER_FONT));
        header.setBackgroundColor(BaseColor.LIGHT_GRAY);
        header.setBorder(Rectangle.BOX);
        header.setPadding(5);
        header.setHorizontalAlignment(Element.ALIGN_CENTER);
        table.addCell(header);
    }
}