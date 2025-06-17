package ch.sbb.driveradvisorysystem.digitaldrivingorder;

import static org.assertj.core.api.Assertions.assertThat;

import ch.sbb.bahninfrastruktur.eradn.RadnDaten;
import ch.sbb.bahninfrastruktur.eradn.Strecke;
import ch.sbb.driveradvisorysystem.digitaldrivingorder.eradn.RadnParser;
import ch.sbb.driveradvisorysystem.digitaldrivingorder.model.DigitalDrivingOrder;
import ch.sbb.driveradvisorysystem.digitaldrivingorder.nets.NetsFpsParser;
import ch.sbb.driveradvisorysystem.digitaldrivingorder.nets.model.VehicleJourney;
import ch.sbb.driveradvisorysystem.digitaldrivingorder.pdf.PdfHelper;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class DigitalDrivingOrderServiceTest {

    //@Autowired
    private final DigitalDrivingOrderService digitalDrivingOrderService = new DigitalDrivingOrderService();

    @Test
    void buildDigitalDrivingOrderAndGeneratePDF() throws Exception {
        // contains all de:Strecken TODO get newest from eRADN file
        final RadnDaten radnDaten = RadnParser.unmarshal("src/main/resources/eRADN_280_20250121_083123_000.xml");
        //TODO move to RadnParserTest
        assertThat(radnDaten).isNotNull();
        assertThat(radnDaten.getStrecken().getStrecke()).hasSize(229);
        final Strecke strecke101 = radnDaten.getStrecken().getStrecke().getFirst();
        assertThat(strecke101.getStreckenId()).isEqualTo("101");
        assertThat(strecke101.getTeilstrecken().getTeilstrecke()).hasSize(3);

        //TODO iterate overall trains for next X hours
        final VehicleJourney vehicleJourney = NetsFpsParser.toJourneyPlanner(/*"src/test/resources/*/ "NeTS-FPS_IC-1-711.csv", LocalDate.of(2024, 12, 17));
        //TODO move to NetsFpsParserTest
        assertThat(vehicleJourney.getTrainNumber()).isEqualTo("711");
        assertThat(vehicleJourney.getStopPoints()).hasSize(167);
        assertThat(vehicleJourney.getStopPoints().get(0).getPlaceShortName()).isEqualTo("GEAP");
        assertThat(vehicleJourney.getStopPoints().get(0).getTimeAimedDeparture()).isEqualTo("07.25");
        assertThat(vehicleJourney.getStopPoints().get(4).getPlaceShortName()).isEqualTo("GE");
        assertThat(vehicleJourney.getStopPoints().get(4).getTimeAimedArrival()).as("underlined resp. arrival").isEqualTo("32");
        assertThat(vehicleJourney.getStopPoints().get(1).getPlaceShortName()).isEqualTo("CHNE");
        assertThat(vehicleJourney.getStopPoints().get(1).getTimeAimedArrival()).as("passtrhough").isEqualTo("(27)");
        assertThat(vehicleJourney.getStopPoints().get(166).getPlaceShortName()).isEqualTo("SG");
        assertThat(vehicleJourney.getStopPoints().get(166).getTimeAimedArrival()).as("last stop formatted as departure but is arrival").isEqualTo("11.52");

        final DigitalDrivingOrder digitalDrivingOrder = digitalDrivingOrderService.buildDigitalDrivingOrder(radnDaten, vehicleJourney);
        assertThat(digitalDrivingOrder).isNotNull();
        assertThat(digitalDrivingOrder.getIsbs()).hasSizeGreaterThanOrEqualTo(1);
        assertThat(digitalDrivingOrder.getTrainNumber()).isNotBlank();
        assertThat(digitalDrivingOrder.getOperatingDay()).isNotNull();
        assertThat(digitalDrivingOrder.getEntries()).hasSizeGreaterThanOrEqualTo(vehicleJourney.getStopPoints().size());

        PdfHelper.generatePDF(digitalDrivingOrder);
        //TODO assert PDF created
    }
}
