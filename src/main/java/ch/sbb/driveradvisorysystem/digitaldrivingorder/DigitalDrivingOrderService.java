package ch.sbb.driveradvisorysystem.digitaldrivingorder;

import ch.sbb.bahninfrastruktur.eradn.Blocksignal;
import ch.sbb.bahninfrastruktur.eradn.Fussnote;
import ch.sbb.bahninfrastruktur.eradn.Knoten;
import ch.sbb.bahninfrastruktur.eradn.Kurve;
import ch.sbb.bahninfrastruktur.eradn.RadnDaten;
import ch.sbb.bahninfrastruktur.eradn.Schutzstrecke;
import ch.sbb.bahninfrastruktur.eradn.Strecke;
import ch.sbb.bahninfrastruktur.eradn.Teilstrecke;
import ch.sbb.bahninfrastruktur.eradn.TeilstreckenBp;
import ch.sbb.bahninfrastruktur.eradn.TeilstreckenBpVerbindung;
import ch.sbb.bahninfrastruktur.eradn.TeilstreckenBpVerbindungen;
import ch.sbb.bahninfrastruktur.eradn.TsBpvElemente;
import ch.sbb.driveradvisorysystem.digitaldrivingorder.eradn.RadnParser;
import ch.sbb.driveradvisorysystem.digitaldrivingorder.model.DigitalDrivingOrderEntry;
import ch.sbb.driveradvisorysystem.digitaldrivingorder.model.Footnote;
import ch.sbb.driveradvisorysystem.digitaldrivingorder.nets.model.VehicleJourney;
import ch.sbb.driveradvisorysystem.digitaldrivingorder.pdf.PdfHelper;
import com.itextpdf.text.Chunk;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Font;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.Rectangle;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.NotImplementedException;

@Slf4j
//@Service
public class DigitalDrivingOrderService {

    //TODO make re-entrant
    private int fussnotenIndex = -1;

    //TODO detach data building from visualisation
    public void generatePDF(RadnDaten radnDaten, VehicleJourney vehicleJourney) throws DocumentException, FileNotFoundException {
        final com.itextpdf.text.Document ddoPdf = PdfHelper.createDocumentPDF(vehicleJourney.getTrainNumber(), vehicleJourney.getOperatingDay());

        Chunk chunk = new Chunk("<strecke::ISB>     " +
            "Zug " + vehicleJourney.getTrainNumber() + "     " + vehicleJourney.getOperatingDay(), PdfHelper.TITLE);
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
        for (DigitalDrivingOrderEntry entry : buildDigitalDrivingOrder(radnDaten, vehicleJourney)) {
            footnoteTexts.addAll(addTableRow(table, entry));
        }
        ddoPdf.add(table);

        for (String footnoteText : footnoteTexts) {
            // TODO add after each page instead of once at the end
            ddoPdf.add(new Paragraph(footnoteText, PdfHelper.FOOTNOTE));
        }

        ddoPdf.close();
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

    List<DigitalDrivingOrderEntry> buildDigitalDrivingOrder(RadnDaten eRADN, VehicleJourney vehicleJourney) {
        final List<DigitalDrivingOrderEntry> digitalDrivingOrderEntries = buildTimetable(vehicleJourney);
        overlayRadn(digitalDrivingOrderEntries, eRADN);
        return digitalDrivingOrderEntries;
    }

    /**
     * Im RADN sind alle möglichen Zug-/Bremsreihen drin. Mit welcher Zug-/Bremsreihe die Fahrt vorgesehen ist (=Regelreihe) ist in NeTS respektive RCS definiert.
     *
     * @see <a href="https://leaprint.sbb.ch/">Regelreihe für eine spezifische Fahrt: Die vorausgewählte Zug-/Bremsreihe ist die Regelreihe.</a>
     */
    private void overlayRadn(List<DigitalDrivingOrderEntry> digitalDrivingOrderEntries, RadnDaten radnDaten) {
        // TODO hardcoded: bezeichnung="Genève-La-Praille - / Genève-Aéroport - Lausanne" bezeichnungKurz="GEPR - / GEAP - LS"
        Strecke strecke = RadnParser.findStrecke(radnDaten, "111");
        final List<Fussnote> fussnoten = strecke.getFussnoten().getFussnote();

        int entryIndex = 0;
        fussnotenIndex = 1;
        // GEAP -> NY
        entryIndex = merge(digitalDrivingOrderEntries, entryIndex, RadnParser.findTeilstrecke(strecke, 2 /* TODO or 3?*/), false, fussnoten);
        // NY -> LS, skip NY because last in previous Teilstrecke
        entryIndex = merge(digitalDrivingOrderEntries, entryIndex, RadnParser.findTeilstrecke(strecke, 4), true, fussnoten);
        //TODO Strecke LS->SG

        // Lausanne - Fribourg/Freiburg - / Laupen - Bern
        strecke = RadnParser.findStrecke(radnDaten, "121");
        // LS -> FRI
        entryIndex = merge(digitalDrivingOrderEntries, entryIndex, RadnParser.findTeilstrecke(strecke, 1), true, fussnoten);
        // FRI -> THOD
        entryIndex = merge(digitalDrivingOrderEntries, entryIndex, RadnParser.findTeilstrecke(strecke, 2), true, fussnoten);
        // LP -> BN (skip some at beginning until THOD)
        entryIndex = merge(digitalDrivingOrderEntries, entryIndex, RadnParser.findTeilstrecke(strecke, 3), true, fussnoten);

        // Bern - / Solothurn - NBS - Olten
        strecke = RadnParser.findStrecke(radnDaten, "142");
        // BN -> WANZ
        entryIndex = merge(digitalDrivingOrderEntries, entryIndex, RadnParser.findTeilstrecke(strecke, 1), true, fussnoten);
        // SO -> OLTEN (skip some at beginning)
        entryIndex = merge(digitalDrivingOrderEntries, entryIndex, RadnParser.findTeilstrecke(strecke, 2 /*TODO or 3?*/), true, fussnoten);

        // Olten - Lenzburg - RBL / - Brugg
        strecke = RadnParser.findStrecke(radnDaten, "151");
        // OL -> WOES
        entryIndex = merge(digitalDrivingOrderEntries, entryIndex, RadnParser.findTeilstrecke(strecke, 1 /*TODO or 5?*/), true, fussnoten);
        //
        entryIndex = merge(digitalDrivingOrderEntries, entryIndex, RadnParser.findTeilstrecke(strecke, 2 /*TODO or 6?*/), true, fussnoten);
        entryIndex = merge(digitalDrivingOrderEntries, entryIndex, RadnParser.findTeilstrecke(strecke, 6), true, fussnoten);
        // -> ZH
        entryIndex = merge(digitalDrivingOrderEntries, entryIndex, RadnParser.findTeilstrecke(strecke, 11), true, fussnoten);

        // Zürich Altstetten - / Zürich HB - Zürich Oerlikon
        strecke = RadnParser.findStrecke(radnDaten, "701");
        entryIndex = merge(digitalDrivingOrderEntries, entryIndex, RadnParser.findTeilstrecke(strecke, 4), true, fussnoten);

        // Zürich Oerlikon - Winterthur
        strecke = RadnParser.findStrecke(radnDaten, "702");
        entryIndex = merge(digitalDrivingOrderEntries, entryIndex, RadnParser.findTeilstrecke(strecke, 1), true, fussnoten);
        entryIndex = merge(digitalDrivingOrderEntries, entryIndex, RadnParser.findTeilstrecke(strecke, 3), true, fussnoten);

        // Winterthur - St. Gallen St. Fiden
        strecke = RadnParser.findStrecke(radnDaten, "711");
        entryIndex = merge(digitalDrivingOrderEntries, entryIndex, RadnParser.findTeilstrecke(strecke, 1), true, fussnoten);

        if (entryIndex != -1) {
            throw new NotImplementedException("journey not fully treated");
        }
    }

    /**
     * @param digitalDrivingOrderEntries
     * @param index
     * @param teilstrecke
     * @param skipOrigin for follow-up Teilstrecken the origin is treated in previous Teilstrecke
     * @return
     */
    private int merge(List<DigitalDrivingOrderEntry> digitalDrivingOrderEntries, int index, Teilstrecke teilstrecke, boolean skipOrigin, List<Fussnote> fussnoten) {
        int count = index;
        int i = skipOrigin ? 1 : 0;
        int usedBps = 0;
        for (; i < teilstrecke.getTeilstreckenBPe().getTeilstreckenBp().size(); i++) {
            if (count >= digitalDrivingOrderEntries.size()) {
                log.info("end of journey reached, skip farther Bp's in Teilstrecke");
                return -1;
            }
            final DigitalDrivingOrderEntry entry = digitalDrivingOrderEntries.get(count);

            final TeilstreckenBp teilstreckenBp = teilstrecke.getTeilstreckenBPe().getTeilstreckenBp().get(i);
            if (!entry.getShortName().equals(teilstreckenBp.getBpAbkuerzung())) {
                //TODO for e.g. RADN Teilstrecke 4::teilstreckenBp[26]="LS", ::teilstreckenBp[27]="LSPA"; NeTS "LS", "PUN" -> mismatch PUN<>LSPA
                log.warn("no match for Teilstrecke={}, teilstreckeBp[{}], DigitalDrivingOrderEntry[{}]", teilstrecke.getTeilstreckenNr(), i, count);
                continue;
            }

            entry.setTeilstreckenBpId(teilstreckenBp.getId());
            entry.setName(teilstreckenBp.getText());
            entry.setShortName(teilstreckenBp.getBpAbkuerzung());
            entry.setAusgeblendet(teilstreckenBp.isAusgeblendet());
            entry.setKlammer(teilstreckenBp.isInKlammern());
            entry.setKm(teilstreckenBp.getKm1() == null ? null : teilstreckenBp.getKm1().doubleValue());
            entry.setFunkkanal(teilstreckenBp.getFunkkanal());
            entry.setAbfahrtsErlaubnis(teilstreckenBp.getAbfahrerlaubnisText());

            entry.setStreckenR150(teilstreckenBp.getStreckenGeschwindigkeit() == null ? "" : RadnParser.getR150(teilstreckenBp.getStreckenGeschwindigkeit().getV()));
            entry.setBahnhofR150(teilstreckenBp.getBahnhofsGeschwindigkeit() == null ? "" : RadnParser.getR150(teilstreckenBp.getBahnhofsGeschwindigkeit().getV()));

            for (Fussnote fussnoteForBp : RadnParser.findFussnoten(teilstrecke.getId(), entry.getTeilstreckenBpId(), fussnoten)) {
                entry.getFootnotes().add(Footnote.builder()
                    .index(fussnotenIndex++)
                    .text(fussnoteForBp.getText() + "Zugreihen=" + fussnoteForBp.getZugreihen())
                    .build());
            }

            usedBps++;
            count++;
        }

        if (usedBps == 0) {
            throw new NotImplementedException("Developer fault: no Bp used out of Teilstrecke");
        }

        mergeTeilstreckenBpVerbindungen(digitalDrivingOrderEntries, index, count - 1, teilstrecke.getTeilstreckenBpVerbindungen());

        return count;
    }

    private void mergeTeilstreckenBpVerbindungen(List<DigitalDrivingOrderEntry> digitalDrivingOrderEntries, int from, int to, TeilstreckenBpVerbindungen teilstreckenBpVerbindungen) {
        for (TeilstreckenBpVerbindung teilstreckenBpVerbindung : teilstreckenBpVerbindungen.getTeilstreckenBpVerbindung()) {
            final String tsBpVon = teilstreckenBpVerbindung.getTsBpVon();
            final String tsBpBis = teilstreckenBpVerbindung.getTsBpBis();

            boolean override = false;
            for (int i = from; i < to; i++) {
                final DigitalDrivingOrderEntry entry = digitalDrivingOrderEntries.get(i);
                if (override || tsBpVon.equals(entry.getTeilstreckenBpId())) {
                    override = true;
                    entry.setGefaelle(teilstreckenBpVerbindung.getGefaelle() == null ? null : Integer.valueOf(teilstreckenBpVerbindung.getGefaelle()));
                    entry.setSteigung(teilstreckenBpVerbindung.getSteigung() == null ? null : Integer.valueOf(teilstreckenBpVerbindung.getSteigung()));

                    final TsBpvElemente elemente = teilstreckenBpVerbindung.getTsBpvElemente();
                    if (elemente != null) {
                        for (Knoten knoten : elemente.getBlocksignalOrSchutzstreckeOrZugsicherungsgeraet()) {
                            switch (knoten) {
                                case Blocksignal blocksignal -> entry.getBlocks().add(blocksignal);
                                case Kurve kurve -> entry.getCurves().add(kurve);
                                case Schutzstrecke schutzstrecke -> entry.getSchutzstrecken().add(schutzstrecke);
                                case null, default -> log.warn("untreated Knoten: {}", knoten);
                            }
                        }
                    }
                } else if (tsBpBis.equals(entry.getTeilstreckenBpId())) {
                    override = false;
                    //TODO tune: continue indices range after last value of i to update more efficiently
                    break;
                }
            }
        }
    }

    /**
     * @param vehicleJourney NeTS-FPS based vehicle-journey
     * @return part itinery of public transportation only
     */
    private List<DigitalDrivingOrderEntry> buildTimetable(VehicleJourney vehicleJourney) {
        return vehicleJourney.getStopPoints().stream()
            .map(stopPoint -> {
                final DigitalDrivingOrderEntry entry = new DigitalDrivingOrderEntry();
                entry.setShortName(stopPoint.getPlaceShortName());
                entry.setAb(stopPoint.getTimeAimedDeparture());
                entry.setAn(stopPoint.getTimeAimedArrival());
                return entry;
            })
            .toList();
    }
}
