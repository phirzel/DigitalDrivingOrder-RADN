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
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.pdf.PdfPTable;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

@Slf4j
//@Service
public class DigitalDrivingOrderService {

    private static final String TODO = "<TODO>";
    //TODO make re-entrant
    private int fussnotenIndex = -1;

    //TODO detach data building from visualisation
    public void generatePDF(RadnDaten radnDaten, VehicleJourney vehicleJourney) throws DocumentException, FileNotFoundException {
        final com.itextpdf.text.Document ddoPdf = PdfHelper.createDocumentPDF(vehicleJourney.getTrainNumber(), vehicleJourney.getOperatingDay());
        final PdfPTable table = new PdfPTable(11);
        PdfHelper.addTableHeader(table);
        Chunk chunk = new Chunk(TODO /*strecke.getIsb()*/ + "     " +
            "Zug " + vehicleJourney.getTrainNumber() + "     " + vehicleJourney.getOperatingDay());
        ddoPdf.add(chunk);
        ddoPdf.add(new Paragraph(" "));

        //TODO parseStrecke(table, strecke, journeyPlanner);
        final List<String> footnoteTexts = new ArrayList<>();
        for (DigitalDrivingOrderEntry entry : buildDigitalDrivingOrder(radnDaten, vehicleJourney)) {
            footnoteTexts.addAll(addTableRow(table, entry));
        }
        ddoPdf.add(table);

        for (String footnoteText : footnoteTexts) {
            // TODO add after each page instead of once at the end
            chunk = new Chunk(footnoteText);
            ddoPdf.add(chunk);
            ddoPdf.add(new Paragraph(" "));
        }

        ddoPdf.close();
    }

    List<DigitalDrivingOrderEntry> buildDigitalDrivingOrder(RadnDaten eRADN, VehicleJourney vehicleJourney) {
        final List<DigitalDrivingOrderEntry> digitalDrivingOrderEntries = buildTimetable(vehicleJourney);
        overlayRadn(digitalDrivingOrderEntries, eRADN);
        return digitalDrivingOrderEntries;
    }

    /**
     * Im RADN sind alle möglichen Zug-/Bremsreihen drin. Mit welcher Zug-/Bremsreihe die Fahrt vorgesehen ist (=Regelreihe) ist in NeTS respektive RCS definiert.
     * @see <a href="https://leaprint.sbb.ch/">Regelreihe für eine spezifische Fahrt: Die vorausgewählte Zug-/Bremsreihe ist die Regelreihe.</a>
     */
    private void overlayRadn(List<DigitalDrivingOrderEntry> digitalDrivingOrderEntries, RadnDaten radnDaten) {
        // TODO hardcoded: bezeichnung="Genève-La-Praille - / Genève-Aéroport - Lausanne" bezeichnungKurz="GEPR - / GEAP - LS"
        final Strecke strecke = RadnParser.findStrecke(radnDaten, "111");
        final List<Fussnote> fussnoten = strecke.getFussnoten().getFussnote();

        int entryIndex = 0;
        fussnotenIndex = 1;
        // GEAP -> NY
        entryIndex = merge(digitalDrivingOrderEntries, entryIndex, RadnParser.findTeilstrecke(strecke, 2 /* TODO or 3?*/), false, fussnoten);
        // NY -> LS, skip NY because last in previous Teilstrecke
        entryIndex = merge(digitalDrivingOrderEntries, entryIndex, RadnParser.findTeilstrecke(strecke, 4), true, fussnoten);
        //TODO Strecke LS->SG
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
        for (; i < teilstrecke.getTeilstreckenBPe().getTeilstreckenBp().size(); i++) {
            final DigitalDrivingOrderEntry entry = digitalDrivingOrderEntries.get(count);

            final TeilstreckenBp teilstreckenBp = teilstrecke.getTeilstreckenBPe().getTeilstreckenBp().get(i);
            if (!entry.getShortName().equals(teilstreckenBp.getBpAbkuerzung())) {
                //TODO RADN Teilstrecke 4::teilstreckenBp[26]="LS", ::teilstreckenBp[27]="LSPA"; NeTS "LS", "PUN" -> mismatch PUN<>LSPA
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

            count++;
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
                                case Blocksignal blocksignal -> {
                                    // TODO ::text like "Frontier" ?
                                    entry.getBlocks().add("Block " + blocksignal.getBezeichnung() + "@" + knoten.getKm());
                                }
                                case Kurve kurve -> {
                                    entry.getCurves().add("Kurve " + kurve.getStandardText() + "@" + knoten.getKm());
                                }
                                case Schutzstrecke schutzstrecke -> {
                                    entry.getSchutzstrecken().add("Schutzstrecke " + schutzstrecke.getStandardText() + "@" + schutzstrecke.getKm());
                                }
                                case null, default -> {
                                    log.warn("untreated Knoten: {}", knoten);
                                }
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

    private List<String> addTableRow(PdfPTable table, DigitalDrivingOrderEntry entry) {
        String footnoteRef = "";
        final List<String> footnoteTexts = new ArrayList<>();
        for (Footnote footnote : entry.getFootnotes()) {
            final String index = footnote.getIndex() + ")";
            footnoteRef += index;
            footnoteTexts.add(index + " " + footnote.getText());
        }
        table.addCell(footnoteRef);

        table.addCell(entry.getKm() == null ? "" : entry.getKm().toString());
        table.addCell(entry.getGefaelle() == null ? "" : "" + entry.getGefaelle());
        table.addCell(entry.getSteigung() == null ? "" : "" + entry.getSteigung());
        table.addCell(entry.getFunkkanal());
        table.addCell(entry.getAbfahrtsErlaubnis());
        table.addCell(entry.getEtcs());
        table.addCell(toPlaceName(entry));
        table.addCell(entry.getStreckenR150());
        table.addCell(entry.getAn());
        table.addCell(entry.getAb());

        return footnoteTexts;
    }

    /**
     * @return formatted Betriebspunkt (Bp)
     */
    private String toPlaceName(DigitalDrivingOrderEntry entry) {
        String text = "";
        if (entry.isAusgeblendet()) {
            text += "H";
        }
        if (entry.isKlammer()) {
            text += "K";
        }
        if (!text.isEmpty()) {
            text = "<" + text + ">";
        }
        text += entry.getName() == null ? entry.getShortName() : entry.getName();

        if (entry.getBahnhofR150() != null) {
            text += " - " + entry.getBahnhofR150();
        }

        for (String block : entry.getBlocks()) {
            text += " " + block;
        }
        for (String curve : entry.getCurves()) {
            text += " " + curve;
        }
        for (String schutzstrecke : entry.getSchutzstrecken()) {
            text += " " + schutzstrecke;
        }
        return text;
    }
}
