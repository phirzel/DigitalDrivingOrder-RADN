package ch.sbb.driveradvisorysystem.digitaldrivingorder.pdf;

import com.itextpdf.text.BaseColor;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Font;
import com.itextpdf.text.FontFactory;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.time.LocalDate;
import java.util.List;
import lombok.experimental.UtilityClass;

@UtilityClass
public class PdfHelper {

    public static final Font TITLE = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16, BaseColor.BLACK);
    public static final Font FOOTNOTE = FontFactory.getFont(FontFactory.HELVETICA, 10, BaseColor.BLACK);
    public static final Font CELL_HEADER = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, BaseColor.BLACK);
    public static final Font CELL_VALUE = FontFactory.getFont(FontFactory.HELVETICA, 10, BaseColor.BLACK);
    public static final Font CELL_VALUE_PINK = FontFactory.getFont(FontFactory.HELVETICA, 10, BaseColor.PINK);
    public static final Font CELL_VALUE_BLUE = FontFactory.getFont(FontFactory.HELVETICA, 10, BaseColor.BLUE);
    public static final Font CELL_VALUE_SMALL = FontFactory.getFont(FontFactory.HELVETICA, 8, BaseColor.BLACK);

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
        columnTitles.stream().forEach(columnTitle -> {
            final PdfPCell cell = new PdfPCell();
            cell.setBackgroundColor(BaseColor.LIGHT_GRAY);
            cell.setBorderWidth(2);
            cell.setPhrase(new Phrase(columnTitle, CELL_HEADER));
            table.addCell(cell);
            });
    }
}
