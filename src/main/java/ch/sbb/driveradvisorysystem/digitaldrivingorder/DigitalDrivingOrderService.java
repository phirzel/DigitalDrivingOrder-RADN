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
import ch.sbb.driveradvisorysystem.digitaldrivingorder.model.DigitalDrivingOrder;
import ch.sbb.driveradvisorysystem.digitaldrivingorder.model.DigitalDrivingOrderEntry;
import ch.sbb.driveradvisorysystem.digitaldrivingorder.model.Footnote;
import ch.sbb.driveradvisorysystem.digitaldrivingorder.nets.model.VehicleJourney;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.NotImplementedException;
import org.apache.commons.lang3.StringUtils;

@Slf4j
//@Service
public class DigitalDrivingOrderService {

    //TODO make re-entrant
    private int fussnotenIndex = -1;

    DigitalDrivingOrder buildDigitalDrivingOrder(RadnDaten eRADN, VehicleJourney vehicleJourney) {
        final List<DigitalDrivingOrderEntry> entries = buildTimetable(vehicleJourney);
        final DigitalDrivingOrder digitalDrivingOrder = DigitalDrivingOrder.builder()
            .trainNumber(vehicleJourney.getTrainNumber())
            .operatingDay(vehicleJourney.getOperatingDay())
            .isbs(overlayRadn(entries, eRADN).stream().toList())
            .entries(entries)
            .build();

        if (digitalDrivingOrder.getIsbs().size() != 1) {
            throw new NotImplementedException("none or multi ISB unclear yet");
        }
        return digitalDrivingOrder;
    }

    /**
     * Im RADN sind alle möglichen Zug-/Bremsreihen drin. Mit welcher Zug-/Bremsreihe die Fahrt vorgesehen ist (=Regelreihe) ist in NeTS respektive RCS definiert.
     *
     * @see <a href="https://leaprint.sbb.ch/">Regelreihe für eine spezifische Fahrt: Die vorausgewählte Zug-/Bremsreihe ist die Regelreihe.</a>
     */
    private Set<String> overlayRadn(List<DigitalDrivingOrderEntry> digitalDrivingOrderEntries, RadnDaten radnDaten) {
        final Set<String> isbs = new HashSet<>();

        // TODO hardcoded: bezeichnung="Genève-La-Praille - / Genève-Aéroport - Lausanne" bezeichnungKurz="GEPR - / GEAP - LS"
        Strecke strecke = RadnParser.findStrecke(radnDaten, "111");
        isbs.add(strecke.getIsb());
        final List<Fussnote> fussnoten = strecke.getFussnoten().getFussnote();

        int entryIndex = 0;
        fussnotenIndex = 1;
        // GEAP -> NY
        entryIndex = merge(digitalDrivingOrderEntries, entryIndex, RadnParser.findTeilstrecke(strecke, 2 /* TODO or 3?*/), false, fussnoten);
        // NY -> LS, skip NY because last in previous Teilstrecke
        entryIndex = merge(digitalDrivingOrderEntries, entryIndex, RadnParser.findTeilstrecke(strecke, 4), true, fussnoten);

        // Lausanne - Fribourg/Freiburg - / Laupen - Bern
        strecke = RadnParser.findStrecke(radnDaten, "121");
        isbs.add(strecke.getIsb());
        // LS -> FRI
        entryIndex = merge(digitalDrivingOrderEntries, entryIndex, RadnParser.findTeilstrecke(strecke, 1), true, fussnoten);
        // FRI -> THOD
        entryIndex = merge(digitalDrivingOrderEntries, entryIndex, RadnParser.findTeilstrecke(strecke, 2), true, fussnoten);
        // LP -> BN (skip some at beginning until THOD)
        entryIndex = merge(digitalDrivingOrderEntries, entryIndex, RadnParser.findTeilstrecke(strecke, 3), true, fussnoten);

        // Bern - / Solothurn - NBS - Olten
        strecke = RadnParser.findStrecke(radnDaten, "142");
        isbs.add(strecke.getIsb());
        // BN -> WANZ
        entryIndex = merge(digitalDrivingOrderEntries, entryIndex, RadnParser.findTeilstrecke(strecke, 1), true, fussnoten);
        // SO -> OLTEN (skip some at beginning)
        entryIndex = merge(digitalDrivingOrderEntries, entryIndex, RadnParser.findTeilstrecke(strecke, 2 /*TODO or 3?*/), true, fussnoten);

        // Olten - Lenzburg - RBL / - Brugg
        strecke = RadnParser.findStrecke(radnDaten, "151");
        isbs.add(strecke.getIsb());
        // OL -> WOES
        entryIndex = merge(digitalDrivingOrderEntries, entryIndex, RadnParser.findTeilstrecke(strecke, 1 /*TODO or 5?*/), true, fussnoten);
        //
        entryIndex = merge(digitalDrivingOrderEntries, entryIndex, RadnParser.findTeilstrecke(strecke, 2 /*TODO or 6?*/), true, fussnoten);
        entryIndex = merge(digitalDrivingOrderEntries, entryIndex, RadnParser.findTeilstrecke(strecke, 6), true, fussnoten);
        // -> ZH
        entryIndex = merge(digitalDrivingOrderEntries, entryIndex, RadnParser.findTeilstrecke(strecke, 11), true, fussnoten);

        // Zürich Altstetten - / Zürich HB - Zürich Oerlikon
        strecke = RadnParser.findStrecke(radnDaten, "701");
        isbs.add(strecke.getIsb());
        entryIndex = merge(digitalDrivingOrderEntries, entryIndex, RadnParser.findTeilstrecke(strecke, 4), true, fussnoten);

        // Zürich Oerlikon - Winterthur
        strecke = RadnParser.findStrecke(radnDaten, "702");
        isbs.add(strecke.getIsb());
        entryIndex = merge(digitalDrivingOrderEntries, entryIndex, RadnParser.findTeilstrecke(strecke, 1), true, fussnoten);
        entryIndex = merge(digitalDrivingOrderEntries, entryIndex, RadnParser.findTeilstrecke(strecke, 3), true, fussnoten);

        // Winterthur - St. Gallen St. Fiden
        strecke = RadnParser.findStrecke(radnDaten, "711");
        isbs.add(strecke.getIsb());
        entryIndex = merge(digitalDrivingOrderEntries, entryIndex, RadnParser.findTeilstrecke(strecke, 1), true, fussnoten);

        if (entryIndex != -1) {
            throw new NotImplementedException("journey not fully treated");
        }

        return isbs;
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

            entry.setStreckenR150(teilstreckenBp.getStreckenGeschwindigkeit() == null ? StringUtils.EMPTY : RadnParser.getR150(teilstreckenBp.getStreckenGeschwindigkeit().getV()));
            entry.setBahnhofR150(teilstreckenBp.getBahnhofsGeschwindigkeit() == null ? StringUtils.EMPTY : RadnParser.getR150(teilstreckenBp.getBahnhofsGeschwindigkeit().getV()));

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
                                case null, default ->
                                    // TODO ? CabSignal, GenerischesElement
                                    log.warn("untreated Knoten: {}", knoten);
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
