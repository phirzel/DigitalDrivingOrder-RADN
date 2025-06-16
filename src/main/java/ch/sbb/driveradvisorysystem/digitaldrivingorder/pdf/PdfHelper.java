package ch.sbb.driveradvisorysystem.digitaldrivingorder.pdf;

import ch.sbb.bahninfrastruktur.eradn.Blocksignal;
import ch.sbb.bahninfrastruktur.eradn.Kurve;
import ch.sbb.bahninfrastruktur.eradn.Schutzstrecke;
import ch.sbb.driveradvisorysystem.digitaldrivingorder.model.DigitalDrivingOrder;
import ch.sbb.driveradvisorysystem.digitaldrivingorder.model.DigitalDrivingOrderEntry;
import ch.sbb.driveradvisorysystem.digitaldrivingorder.model.Footnote;
import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Chunk;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Font;
import com.itextpdf.text.FontFactory;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.Rectangle;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Pretty print {@link DigitalDrivingOrder} to PDF.
 */
public class PdfHelper {

    private static final Font TITLE = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16, BaseColor.BLACK);
    private static final Font FOOTNOTE = FontFactory.getFont(FontFactory.HELVETICA, 10, BaseColor.BLACK);
    private static final Font CELL_HEADER = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, BaseColor.BLACK);
    private static final Font CELL_VALUE = FontFactory.getFont(FontFactory.HELVETICA, 10, BaseColor.BLACK);
    private static final Font CELL_VALUE_PINK = FontFactory.getFont(FontFactory.HELVETICA, 10, BaseColor.PINK);
    private static final Font CELL_VALUE_BLUE = FontFactory.getFont(FontFactory.HELVETICA, 10, BaseColor.BLUE);
    private static final Font CELL_VALUE_SMALL = FontFactory.getFont(FontFactory.HELVETICA, 8, BaseColor.BLACK);

    public static com.itextpdf.text.Document createDocumentPDF(String trainNumber, LocalDate date) throws FileNotFoundException, DocumentException {
        final com.itextpdf.text.Document document = new com.itextpdf.text.Document();
        /*final PdfWriter writer =*/
        PdfWriter.getInstance(document, new FileOutputStream("target/DDO_" + trainNumber + "_" + date.toString() + ".pdf"));
        /*rotates the text
        MyPage event = new MyPage();
        writer.setPageEvent(event);
        event.setOrientation(PdfPage.LANDSCAPE);
         */
        document.setPageSize(PageSize.A4.rotate());
        document.open();

        return document;
    }

    public static void addTableHeader(PdfPTable table, List<String> columnTitles) {
        columnTitles.forEach(columnTitle -> {
            final PdfPCell cell = new PdfPCell();
            cell.setBackgroundColor(BaseColor.LIGHT_GRAY);
            cell.setBorderWidth(2);
            cell.setPhrase(new Phrase(columnTitle, CELL_HEADER));
            table.addCell(cell);
            });
    }

    private PdfHelper(DigitalDrivingOrder digitalDrivingOrder) throws DocumentException, FileNotFoundException {
        final com.itextpdf.text.Document ddoPdf = createDocumentPDF(digitalDrivingOrder.getTrainNumber(), digitalDrivingOrder.getOperatingDay());

        final Chunk chunk = new Chunk(digitalDrivingOrder.getIsbs().toString() + "     " +
            "Zug " + digitalDrivingOrder.getTrainNumber() + "     " + digitalDrivingOrder.getOperatingDay(), PdfHelper.TITLE);
        ddoPdf.add(chunk);
        ddoPdf.add(new Paragraph(" "));

        final PdfPTable table = new PdfPTable(11);
        table.setWidthPercentage(100);
        table.setSpacingBefore(0f);
        table.setSpacingAfter(0f);
        table.setTotalWidth(new float[]{10, 10, 10, 10, 30, 20, 20, 100, 20, 15, 15});
        //table.setLockedWidth(true);
        PdfHelper.addTableHeader(table, List.of("" /*de:fussnote Ref*/, "km", "-" /*de:Gefälle*/, "+" /*de:Steigung*/, "Funkkanal", "AE" /*de:Abfahrt-Erlaubnis*/, "ETCS", "", "R150", "An", "Ab"));

        //TODO parseStrecke(table, strecke, journeyPlanner);
        final List<String> footnoteTexts = new ArrayList<>();
        for (DigitalDrivingOrderEntry entry : digitalDrivingOrder.getEntries()) {
            footnoteTexts.addAll(addTableRow(table, entry));
        }
        ddoPdf.add(table);

        for (String footnoteText : footnoteTexts) {
            // TODO add after each page instead of once at the end
            ddoPdf.add(new Paragraph(footnoteText, PdfHelper.FOOTNOTE));
        }

        ddoPdf.close();
    }

    public static void generatePDF(DigitalDrivingOrder digitalDrivingOrder) throws DocumentException, FileNotFoundException {
        new PdfHelper(digitalDrivingOrder);
    }

    private List<String> addTableRow(PdfPTable table, DigitalDrivingOrderEntry entry) throws DocumentException {
        String footnoteRef = "";
        final List<String> footnoteTexts = new ArrayList<>();
        for (Footnote footnote : entry.getFootnotes()) {
            final String index = footnote.getIndex() + ")";
            footnoteRef += index;
            footnoteTexts.add(index + " " + footnote.getText());
        }
        addCell(table, footnoteRef);

        addCell(table, entry.getKm() == null ? "" : entry.getKm().toString());
        addCell(table, entry.getGefaelle() == null ? "" : "" + entry.getGefaelle());
        addCell(table, entry.getSteigung() == null ? "" : "" + entry.getSteigung());
        addCell(table, entry.getFunkkanal());
        addCell(table, entry.getAbfahrtsErlaubnis());
        addCell(table, entry.getEtcs());
        table.addCell(toPlaceData(entry));
        addCell(table, entry.getStreckenR150());
        addCell(table, entry.getAn());
        addCell(table, entry.getAb());

        return footnoteTexts;
    }

    private void addCell(PdfPTable table, String cellValue) {
        final PdfPCell cell = new PdfPCell(new Phrase(cellValue, PdfHelper.CELL_VALUE));
        //cell.setBorder(Rectangle.BOX);
        cell.setBorderWidth(1);
        cell.setRowspan(1);
        table.addCell(cell);
    }

    /**
     * @return formatted Betriebspunkt (Bp) with additional infos
     */
    private PdfPTable toPlaceData(DigitalDrivingOrderEntry entry) throws DocumentException {
        final PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);
        table.setSpacingBefore(0f);
        table.setSpacingAfter(0f);
        table.setTotalWidth(new float[]{60, 40});

        addInCell(table, entry.getName() == null ? entry.getShortName() : entry.getName() + " (" + entry.getShortName() + ")", entry.getBahnhofR150() == null ? "" : entry.getBahnhofR150(),
            (!entry.getAb().isEmpty() | (entry.getAn() != null && !entry.getAn().contains("(")) ? PdfHelper.TITLE : PdfHelper.CELL_VALUE));

        // TODO check format with Biz
        String text = "";
        if (entry.isAusgeblendet()) {
            text += "H";
        }
        if (entry.isKlammer()) {
            text += "K";
        }
        if (!text.isEmpty()) {
            addInCell(table, "<" + text + ">", "", PdfHelper.CELL_VALUE_SMALL);
        }

        // Knoten
        for (Blocksignal block : entry.getBlocks()) {
            addInCell(table, "Block: " + block.getBezeichnung() + (block.getText() == null ? "" : " (" + block.getText() + ")"), "" + block.getKm(), PdfHelper.CELL_VALUE_PINK);
        }
        for (Kurve curve : entry.getCurves()) {
            addInCell(table, "Kurve: " + curve.getStandardText(), "" + curve.getKm(), PdfHelper.CELL_VALUE_BLUE);
        }
        for (Schutzstrecke schutzstrecke : entry.getSchutzstrecken()) {
            addInCell(table, "Schutztstrecke: " + schutzstrecke.getStandardText(), "" + schutzstrecke.getKm(), PdfHelper.CELL_VALUE);
        }
        return table;
    }

    private void addInCell(PdfPTable table, String column0Text, String column1Text, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(column0Text, font));
        cell.setBorder(Rectangle.TOP);
        //cell.setBorderWidth(1);
        cell.setRowspan(1);
        table.addCell(cell);

        cell = new PdfPCell(new Phrase(column1Text, font));
        cell.setBorder(Rectangle.TOP);
        //cell.setBorderWidth(1);
        cell.setRowspan(1);
        table.addCell(cell);
    }
}
