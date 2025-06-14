package ch.sbb.driveradvisorysystem.digitaldrivingorder.pdf;

import com.itextpdf.text.BaseColor;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Font;
import com.itextpdf.text.FontFactory;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.time.LocalDate;
import java.util.stream.Stream;
import lombok.experimental.UtilityClass;

@UtilityClass
public class PdfHelper {

    private static final Font font = FontFactory.getFont(FontFactory.COURIER, 16, BaseColor.BLACK);

    public static com.itextpdf.text.Document createDocumentPDF(String trainNumber, LocalDate date) throws FileNotFoundException, DocumentException {
        com.itextpdf.text.Document document = new com.itextpdf.text.Document();
        PdfWriter.getInstance(document, new FileOutputStream("target/DDO_" + trainNumber + "_" + date.toString() + ".pdf"));
        //document.setMargins(0,0,0,0);
        document.open();
        //document.left(0);
        //document.right(0);
        return document;
    }

    public static void addTableHeader(PdfPTable table) {
        Stream.of("" /*de:fussnote Ref*/, "km", "-" /*de:Gefälle*/, "+" /*de:Steigung*/, "Funkkanal", "AE" /*de:Abfahrt-Erlaubnis*/, "ETCS", "", "R150", "An", "Ab")
            .forEach(columnTitle -> {
                PdfPCell header = new PdfPCell();
                header.setBackgroundColor(BaseColor.LIGHT_GRAY);
                header.setBorderWidth(2);
                header.setPhrase(new Phrase(columnTitle));
                table.addCell(header);
            });
    }
}
