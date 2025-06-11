package ch.sbb.driveradvisorysystem.digitaldrivingorder;

import ch.sbb.bahninfrastruktur.eradn.RadnDaten;
import ch.sbb.driveradvisorysystem.digitaldrivingorder.eradn.DigitalDrivingOrderEntry;
import ch.sbb.driveradvisorysystem.digitaldrivingorder.journeyplanner.JourneyPlanner;
import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Chunk;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Font;
import com.itextpdf.text.FontFactory;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

@Slf4j
//@Service
public class DigitalDrivingOrderService {

    private static final String TODO = "<TODO>";
    private static final Font font = FontFactory.getFont(FontFactory.COURIER, 16, BaseColor.BLACK);

    public void generatePDF(RadnDaten radnDaten, JourneyPlanner journeyPlanner) throws DocumentException, FileNotFoundException {
        final com.itextpdf.text.Document ddoPdf = createDocumentPDF(/*streckenId,*/ journeyPlanner.getTrainNumber(), journeyPlanner.getOperatingDay());
        final PdfPTable table = new PdfPTable(11);
        addTableHeader(table);
        Chunk chunk = new Chunk(TODO /*isb*/ + "     " +
            "Zug " + journeyPlanner.getTrainNumber() + "     " + journeyPlanner.getOperatingDay());
        ddoPdf.add(chunk);
        ddoPdf.add(new Paragraph(" "));

        //TODO parseStrecke(table, strecke, journeyPlanner);
        for (DigitalDrivingOrderEntry place : buildDigitalDrivingOrder(radnDaten, journeyPlanner)) {
            addTableRow(table, place, journeyPlanner);
        }

        ddoPdf.add(table);
        ddoPdf.close();
    }

    public List<DigitalDrivingOrderEntry> buildDigitalDrivingOrder(RadnDaten eRADN, JourneyPlanner journeyPlanner) {
        return journeyPlanner.getStopPoints().stream()
            .map(stopPoint -> {
                final DigitalDrivingOrderEntry entry = new DigitalDrivingOrderEntry();
                entry.setShortName(stopPoint.getPlaceShortName());
                entry.setAb(stopPoint.getTimeAimedDeparture());
                entry.setAn(stopPoint.getTimeAimedArrival());
                return entry;
            })
            .toList();
    }

    @Deprecated
    public void generatePDF(Document eRADN, JourneyPlanner journeyPlanner) throws DocumentException, FileNotFoundException {

        final NodeList streckenList = eRADN.getElementsByTagName("strecken");
        if (streckenList.getLength() != 1) {
            throw new IllegalArgumentException("1 'strecken' assumed");
        }

        final Node strecken = streckenList.item(0);
        final NodeList streckeList = strecken.getChildNodes();
        for (int s = 0; s < streckeList.getLength(); s++) {
            // one eRADN Streckentabelle.xml file contains always all Strecken (~229 for SBB rail-network)
            final Node strecke = streckeList.item(s);
            if (!isNodeByName(strecke, "strecke")) {
                log.warn("unexpected Node: {}", strecke);
                continue;
            }

            final NamedNodeMap attrList = strecke.getAttributes();
            final String streckenId = getNamedNodeValue(attrList, "streckenId");
            if (!"711".equals(streckenId)) {
                log.info("Skip streckenId={}", streckenId);
                continue;
            }

            print("Strecke: " + attrList.getNamedItem("streckenId") + ", " + attrList.getNamedItem("bezeichnung") + ", " + attrList.getNamedItem("gueltigVon"));

            final com.itextpdf.text.Document ddoPdf = createDocumentPDF(/*streckenId,*/"old_" + journeyPlanner.getTrainNumber(), journeyPlanner.getOperatingDay());
            final PdfPTable table = new PdfPTable(11);
            addTableHeader(table);
            Chunk chunk = new Chunk(attrList.getNamedItem("isb").getNodeValue() + "     " +
                "Zug " + journeyPlanner.getTrainNumber() + "     " + journeyPlanner.getOperatingDay());
            ddoPdf.add(chunk);
            ddoPdf.add(new Paragraph(" "));

            parseStrecke(table, strecke, journeyPlanner);

            ddoPdf.add(table);
            ddoPdf.close();
            break;
        }

        //TODO <topologie>
    }

    // skip "#text"
    private boolean isNodeByName(Node node, String nodeName) {
        return nodeName.equals(node.getNodeName());
    }

    private String getNamedNodeValue(NamedNodeMap attributeList, String attributeName) {
        if (attributeList.getNamedItem(attributeName) == null) {
            return "";
        } else {
            return attributeList.getNamedItem(attributeName).getNodeValue();
        }
    }

    @Deprecated
    private void parseStrecke(PdfPTable table, Node strecke, JourneyPlanner journeyPlanner) {
        final NodeList streckeChildren = strecke.getChildNodes();
        for (int streckeChildIndex = 0; streckeChildIndex < streckeChildren.getLength(); streckeChildIndex++) {

            final Node streckeChild = streckeChildren.item(streckeChildIndex);
            if (isNodeByName(streckeChild, "teilstrecken")) {
                final NodeList teilstreckeList = streckeChild.getChildNodes();
                for (int ts = 0; ts < teilstreckeList.getLength(); ts++) {
                    final Node teilstrecke = teilstreckeList.item(ts);
                    if (!isNodeByName(teilstrecke, "teilstrecke")) {
                        log.warn("unexpected Node: {}", teilstrecke);
                        continue;
                    }

                    parseTeilstrecke(teilstrecke, table, journeyPlanner);
                }
            } else if (isNodeByName(streckeChild, "fussnoten")) {
                //TODO
                log.info("fussnote: {}", streckeChild);
            } else if (isNodeByName(streckeChild, "v-konfigs")) {
                //TODO
                log.info("v-konfigs: {}", streckeChild);
            } else {
                log.warn("non expected Node: {}", streckeChild);
                continue;
            }
        }
    }

    private void parseTeilstrecke(Node teilstrecke, PdfPTable table, JourneyPlanner journeyPlanner) {
        final NamedNodeMap attrList = teilstrecke.getAttributes();
        print("  Teilstrecke: " + attrList.getNamedItem("id") + ", " + attrList.getNamedItem("bezeichnung") + ", " + attrList.getNamedItem("teilstreckenNr"));

        final List<DigitalDrivingOrderEntry> places = new ArrayList<>();

        final NodeList teilstreckeChildren = teilstrecke.getChildNodes();
        for (int tsBPe = 0; tsBPe < teilstreckeChildren.getLength(); tsBPe++) {
            final Node teilstreckenBPeOrVerbindungen = teilstreckeChildren.item(tsBPe);
            if (isNodeByName(teilstreckenBPeOrVerbindungen, "teilstreckenBPe")) {
                final NodeList teilstreckenBPList = teilstreckenBPeOrVerbindungen.getChildNodes();
                for (int tsBP = 0; tsBP < teilstreckenBPList.getLength(); tsBP++) {
                    final Node teilstreckenBp = teilstreckenBPList.item(tsBP);
                    if (!isNodeByName(teilstreckenBp, "teilstreckenBp")) {
                        log.warn("unexpected Node: {}", teilstreckenBp);
                        continue;
                    }

                    final DigitalDrivingOrderEntry place = parseTeilstreckenBp(teilstreckenBp);
                    if (place != null) {
                        places.add(place);
                    }
                }
            } else if (isNodeByName(teilstreckenBPeOrVerbindungen, "teilstreckenBpVerbindungen")) {
                final NodeList teilstreckenBpVerbindungList = teilstreckenBPeOrVerbindungen.getChildNodes();
                for (int tsBpV = 0; tsBpV < teilstreckenBpVerbindungList.getLength(); tsBpV++) {
                    final Node teilstreckenBpVerbindung = teilstreckenBpVerbindungList.item(tsBpV);
                    if (!isNodeByName(teilstreckenBpVerbindung, "teilstreckenBpVerbindung")) {
                        log.warn("unexpected Node: {}", teilstreckenBpVerbindung);
                        continue;
                    }

                    parseTeilstreckenBpVerbindung(teilstreckenBpVerbindung, places);
                }
            } else {
                // skip
                log.warn("unexpected Node: {}", teilstreckenBPeOrVerbindungen);
                continue;
            }
        }

        for (DigitalDrivingOrderEntry place : places) {
            addTableRow(table, place, journeyPlanner);
        }
    }

    private DigitalDrivingOrderEntry parseTeilstreckenBp(Node teilstreckenBp) {
        final NamedNodeMap attrList = teilstreckenBp.getAttributes();
        print("    teilstreckenBp: " + attrList.getNamedItem("id") + ", " + attrList.getNamedItem("funkkanal") + "," + attrList.getNamedItem("km1"));

        final DigitalDrivingOrderEntry place = new DigitalDrivingOrderEntry();

        place.setTeilstreckenBpId(getNamedNodeValue(attrList, "id"));
        place.setName(getNamedNodeValue(attrList, "text"));
        place.setShortName(getNamedNodeValue(attrList, "bpAbkuerzung"));
        place.setAusgeblendet("true".equals(getNamedNodeValue(attrList, "ausgeblendet")));
        place.setKlammer("true".equals(getNamedNodeValue(attrList, "inKlammern")));
        place.setKm(getNamedNodeValue(attrList, "km1"));
        place.setFunkkanal(getNamedNodeValue(attrList, "funkkanal"));
        place.setAbfahrtsErlaubnis(getNamedNodeValue(attrList, "abfahrerlaubnisText"));

        final NodeList streckenOrBahnhofsGeschwindigkeitList = teilstreckenBp.getChildNodes();
        for (int sg = 0; sg < streckenOrBahnhofsGeschwindigkeitList.getLength(); sg++) {
            final Node streckenOrBahnhofsGeschwindigkeit = streckenOrBahnhofsGeschwindigkeitList.item(sg);
            if (isNodeByName(streckenOrBahnhofsGeschwindigkeit, "streckenGeschwindigkeit")) {
                place.setR150(getR150(streckenOrBahnhofsGeschwindigkeit));
            } else if (isNodeByName(streckenOrBahnhofsGeschwindigkeit, "bahnhofsGeschwindigkeit")) {
                final String stopSpeed = getR150(streckenOrBahnhofsGeschwindigkeit);
                if (!"".equals(stopSpeed)) {
                    place.setShortName(place.getShortName() + " " + stopSpeed);
                }
            } else {
                log.warn("unexpected Node: {}", streckenOrBahnhofsGeschwindigkeit);
                continue;
            }
        }

        return place;
    }

    private void parseTeilstreckenBpVerbindung(Node teilstreckenBpVerbindung, List<DigitalDrivingOrderEntry> entries) {
        NamedNodeMap attrList = teilstreckenBpVerbindung.getAttributes();
        print("    teilstreckenBpVerbindung: " + attrList.getNamedItem("tsBpVon") + ", " + attrList.getNamedItem("tsBpBis") + ", " + attrList.getNamedItem("steigung") + ", "
            + attrList.getNamedItem("gefaelle"));

        String tsBpVon = getNamedNodeValue(attrList, "tsBpVon");
        String tsBpBis = getNamedNodeValue(attrList, "tsBpBis");

        boolean override = false;
        for (DigitalDrivingOrderEntry entry : entries) {
            if (override || tsBpVon.equals(entry.getTeilstreckenBpId())) {
                override = true;
                entry.setGefaelle(getNamedNodeValue(attrList, "gefaelle"));
                entry.setSteigung(getNamedNodeValue(attrList, "steigung"));
            } else if (tsBpBis.equals(entry.getTeilstreckenBpId())) {
                override = false;
            }

            final NodeList tsBpvElementeList = teilstreckenBpVerbindung.getChildNodes();
            for (int tsBpvE = 0; tsBpvE < tsBpvElementeList.getLength(); tsBpvE++) {
                final Node tsBpvElemente = tsBpvElementeList.item(tsBpvE);
                if (!isNodeByName(tsBpvElemente, "tsBpvElemente")) {
                    log.warn("unexpected Node: {}", tsBpvElemente);
                    continue;
                }

                final NodeList tsBpvElementKurveOrBlocksignal = tsBpvElemente.getChildNodes();
                for (int tsBpvEKB = 0; tsBpvEKB < tsBpvElementKurveOrBlocksignal.getLength(); tsBpvEKB++) {
                    final Node kurveOrBlocksignal = tsBpvElementKurveOrBlocksignal.item(tsBpvEKB);
                    if (isNodeByName(kurveOrBlocksignal, "kurve")) {
                        attrList = kurveOrBlocksignal.getAttributes();
                        print("      kurve:" + attrList.getNamedItem("kmBis"));
                        //TODO geschwindigkeiten
                    } else if (isNodeByName(kurveOrBlocksignal, "blocksignal")) {
                        attrList = kurveOrBlocksignal.getAttributes();
                        print("      blocksignal:" + attrList.getNamedItem("km"));
                    } else {
                        log.warn("unexpected Node: {}", kurveOrBlocksignal);
                        continue;
                    }
                }
            }
        }
    }

    private String getR150(Node streckenOrBahnhofsGeschwindigkeit) {
        final NodeList vList = streckenOrBahnhofsGeschwindigkeit.getChildNodes();
        for (int vi = 0; vi < vList.getLength(); vi++) {
            final Node v = vList.item(vi);
            if (!isNodeByName(v, "v")) {
                log.warn("unexpected Node: {}", v);
                continue;
            }

            final NamedNodeMap attrList = v.getAttributes();
            print("      " + streckenOrBahnhofsGeschwindigkeit.getNodeName() + ": " + attrList.getNamedItem("zugreihe") + ", " + attrList.getNamedItem("bremsverhaeltnis")
                + "," + attrList.getNamedItem("geschwindigkeit"));
            if ("R".equals(getNamedNodeValue(attrList, "zugreihe")) && "150".equals(getNamedNodeValue(attrList, "bremsverhaeltnis"))) {
                return getNamedNodeValue(attrList, "geschwindigkeit");
            }
        }
        return "";
    }

    private com.itextpdf.text.Document createDocumentPDF(/*String streckenId,*/ String trainNumber, LocalDate date) throws FileNotFoundException, DocumentException {
        com.itextpdf.text.Document document = new com.itextpdf.text.Document();
        PdfWriter.getInstance(document, new FileOutputStream("target/DDO_" + /*streckenId + "_" +*/ trainNumber + "_" + date.toString() + ".pdf"));
        //document.setMargins(0,0,0,0);
        document.open();
        //document.left(0);
        //document.right(0);
        return document;
    }

    private void addTableHeader(PdfPTable table) {
        Stream.of("" /*de:fussnote Ref*/, "km", "-" /*de:Gefälle*/, "+" /*de:Steigung*/, "Funkkanal", "AE" /*de:Abfahrt-Erlaubnis*/, "ETCS", "", "R150", "An", "Ab")
            .forEach(columnTitle -> {
                PdfPCell header = new PdfPCell();
                header.setBackgroundColor(BaseColor.LIGHT_GRAY);
                header.setBorderWidth(2);
                header.setPhrase(new Phrase(columnTitle));
                table.addCell(header);
            });
    }

    private void addTableRow(PdfPTable table, DigitalDrivingOrderEntry entry, JourneyPlanner journeyPlanner) {
        table.addCell(entry.getFussnote());
        table.addCell(entry.getKm());
        table.addCell(entry.getGefaelle());
        table.addCell(entry.getSteigung());
        table.addCell(entry.getFunkkanal());
        table.addCell(entry.getAbfahrtsErlaubnis());
        table.addCell(entry.getEtcs());
        table.addCell(toPlaceName(entry));
        table.addCell(entry.getR150());
        //table.addCell(getTimeAimed(journeyPlanner, place.getShortName()));
        table.addCell(entry.getAn());
        table.addCell(entry.getAb());
    }

    private String toPlaceName(DigitalDrivingOrderEntry place) {
        String text = "";
        if (place.isAusgeblendet()) {
            text += "H";
        }
        if (place.isKlammer()) {
            text += "K";
        }
        if (!"".equals(text)) {
            text = "<" + text + ">";
        }
        text += place.getShortName();
        if (!"".equals(place.getName())) {
            text += "(" + place.getName() + ")";
        }
        return text;
    }

    private String getTimeAimed(JourneyPlanner journeyPlanner, String placeShortName) {
        return journeyPlanner.getStopPoints().stream()
            .filter(stopPoint -> stopPoint.getPlaceShortName().equals(placeShortName))
            .findFirst()
            .map(stopPoint -> stopPoint.getTimeAimedDeparture())
            .orElse(null);
    }

    private void print(String text) {
        System.out.println(text);
    }
}
