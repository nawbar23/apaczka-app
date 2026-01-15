package com.belamila.backend.pdf;

import com.belamila.model.Package;
import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class PdfPrinter {

    private static final Logger logger = LoggerFactory.getLogger(PdfPrinter.class);

    private static final Font HEADER_FONT;
    private static final Font HEADER_NORMAL_FONT;
    private static final Font NORMAL_FONT;
    private static final Font SMALL_FONT;

    private static final Integer MAX_PAGE_CONTENT_SIZE = 22;

    static {
        try {
            // Użyj wbudowanej czcionki z obsługą polskich znaków
            BaseFont baseFont = BaseFont.createFont("C:/Windows/Fonts/arial.ttf", BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
            HEADER_FONT = new Font(baseFont, 12, Font.BOLD);
            HEADER_NORMAL_FONT = new Font(baseFont, 12, Font.NORMAL);
            NORMAL_FONT = new Font(baseFont, 10, Font.NORMAL);
            SMALL_FONT = new Font(baseFont, 9, Font.NORMAL);
        } catch (Exception e) {
            throw new RuntimeException("Nie udało się załadować czcionki", e);
        }
    }

    // Inner class for page numbering
    private static class PageNumberEvent extends PdfPageEventHelper {
        private final Font font;
        private PdfTemplate total;

        public PageNumberEvent(Font font) {
            this.font = font;
        }

        @Override
        public void onOpenDocument(PdfWriter writer, Document document) {
            total = writer.getDirectContent().createTemplate(30, 16);
        }

        @Override
        public void onEndPage(PdfWriter writer, Document document) {
            PdfContentByte cb = writer.getDirectContent();

            // Calculate center position
            float centerX = (document.right() - document.left()) / 2 + document.leftMargin();
            float footerY = document.bottom() - 10;

            // Add "Strona X z " text
            String pageText = String.format("Strona %d z ", writer.getPageNumber());
            Phrase footer = new Phrase(pageText, font);

            ColumnText.showTextAligned(cb, Element.ALIGN_CENTER,
                    footer,
                    centerX,
                    footerY,
                    0);

            // Add placeholder for total pages (will be filled in onCloseDocument)
            float textWidth = font.getBaseFont().getWidthPoint(pageText, font.getSize());
            cb.addTemplate(total, centerX + textWidth / 2, footerY);
        }

        @Override
        public void onCloseDocument(PdfWriter writer, Document document) {
            // Fill in the total number of pages
            ColumnText.showTextAligned(total, Element.ALIGN_LEFT,
                    new Phrase(String.valueOf(writer.getPageNumber()), font),
                    0, 0, 0);
        }
    }

    public void printSummary(List<Package> packages, String path) {
        Date timestamp = new Date();
        String fileTimeString = new SimpleDateFormat("dd-MM-yyyy HH_mm").format(timestamp);
        String fileName = "Podsumawanie paczek " + fileTimeString + ".pdf";
        String fullPath = Paths.get(path, fileName).toString();

        Document document = new Document(PageSize.A4, 50, 50, 50, 50);

        try {
            PdfWriter writer = PdfWriter.getInstance(document, Files.newOutputStream(Paths.get(fullPath)));
            writer.setPageEvent(new PageNumberEvent(SMALL_FONT));

            document.open();

            int pageContentSize = 4 + packages.get(0).getItems().size();
            for (int i = 0; i < packages.size(); i++) {
                Package pkg = packages.get(i);

                addPackageInfo(document, pkg);
                addItemsTable(document, pkg);

                // last package, not point in computing further
                if (i >= packages.size() - 1) {
                    break;
                }

                Package nextPkg = packages.get(i + 1);
                pageContentSize += 4 + nextPkg.getItems().size();

                // Add new page after every 3 packages (except for the last one)
                if (pageContentSize > MAX_PAGE_CONTENT_SIZE) {
                    pageContentSize = 4 + nextPkg.getItems().size();
                    document.newPage();
                }
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
        packageHeader.setSpacingAfter(2);
        document.add(packageHeader);

        Paragraph label = new Paragraph("Metoda dostawy: " + pkg.getShippingLabel(), HEADER_NORMAL_FONT);
        document.add(label);
        String fourthDigit = pkg.getPhone().substring(pkg.getPhone().length() - 4, pkg.getPhone().length() - 3);
        String lastThree = pkg.getPhone().substring(pkg.getPhone().length() - 3);
        String phoneFormatted = "xxx-xx" + fourthDigit + "-" + lastThree;
        Paragraph phone = new Paragraph("Numer telefonu: " + phoneFormatted, HEADER_NORMAL_FONT);
        phone.setSpacingAfter(8);
        document.add(phone);
    }

    private void addItemsTable(Document document, Package pkg) throws DocumentException {
        PdfPTable table = new PdfPTable(3);
        table.setWidthPercentage(100);
        table.setSpacingAfter(20);
        table.setWidths(new float[]{6, 2, 2});

        // Nagłówki kolumn
        addTableHeader(table, "Nazwa");
        addTableHeader(table, "Ilość");
        addTableHeader(table, "Cena (PLN)");

        // Wiersze z przedmiotami
        for (Package.Item item : pkg.getItems()) {
            // Nazwa
            String itemLabel = item.getLabel() != null ? item.getLabel() : "-";
            // Dodaj adnotację o torbie jeśli wymagana
            if (item.getIsBagRequested() != null && item.getIsBagRequested()) {
                itemLabel += "\nKlient potrzebuje torebeczki";
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
        }

        // Wiersz sumaryczny
        PdfPCell totalLabelCell = new PdfPCell(new Phrase("Suma z wysyłką:", HEADER_FONT));
        totalLabelCell.setBorder(Rectangle.BOX);
        totalLabelCell.setPadding(5);
        totalLabelCell.setColspan(2);
        totalLabelCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        table.addCell(totalLabelCell);

        PdfPCell totalValueCell = new PdfPCell(new Phrase(
                String.format("%.2f", pkg.getAmount()), HEADER_FONT
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