package ch.sbb.driveradvisorysystem.digitaldrivingorder;

import static org.assertj.core.api.Assertions.assertThat;

import ch.sbb.bahninfrastruktur.eradn.RadnDaten;
import ch.sbb.bahninfrastruktur.eradn.Strecke;
import ch.sbb.driveradvisorysystem.digitaldrivingorder.eradn.RadnParser;
import ch.sbb.driveradvisorysystem.digitaldrivingorder.journeyplanner.JourneyPlanner;
import ch.sbb.driveradvisorysystem.digitaldrivingorder.nets.NetsFpsParser;
import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

class DigitalDrivingOrderServiceTest {

    //@Autowired
    private final DigitalDrivingOrderService digitalDrivingOrderService = new DigitalDrivingOrderService();

    @Test
    void generatePDF() throws Exception {
        // contains all de:Strecken TODO get newest from eRADN file
        final RadnDaten radnDaten = RadnParser.unmarshal("src/main/resources/eRADN_280_20250121_083123_000_orig.xml");
        //TODO move to RadnParserTest
        assertThat(radnDaten).isNotNull();
        assertThat(radnDaten.getStrecken().getStrecke()).hasSize(229);
        final Strecke strecke101 = radnDaten.getStrecken().getStrecke().get(0);
        assertThat(strecke101.getStreckenId()).isEqualTo("101");
        assertThat(strecke101.getTeilstrecken().getTeilstrecke()).hasSize(3);

        final Document eRadn = readXML("src/main/resources/eRADN_280_20250121_083123_000_orig.xml");
        assertThat(eRadn).isNotNull();

        final JourneyPlanner journeyPlanner = NetsFpsParser.toJourneyPlanner(/*"src/test/resources/*/ "NeTS-FPS_IC-1-711.csv", LocalDate.of(2024, 12, 17));
        //TODO move to NetsFpsParserTest
        assertThat(journeyPlanner.getTrainNumber()).isEqualTo("711");
        assertThat(journeyPlanner.getStopPoints()).hasSize(167);
        assertThat(journeyPlanner.getStopPoints().get(0).getPlaceShortName()).isEqualTo("GEAP");
        assertThat(journeyPlanner.getStopPoints().get(0).getTimeAimedDeparture()).isEqualTo("07.25");
        assertThat(journeyPlanner.getStopPoints().get(4).getPlaceShortName()).isEqualTo("GE");
        assertThat(journeyPlanner.getStopPoints().get(4).getTimeAimedArrival()).as("underlined resp. arrival").isEqualTo("32");
        assertThat(journeyPlanner.getStopPoints().get(1).getPlaceShortName()).isEqualTo("CHNE");
        assertThat(journeyPlanner.getStopPoints().get(1).getTimeAimedArrival()).as("passtrhough").isEqualTo("(27)");
        assertThat(journeyPlanner.getStopPoints().get(166).getPlaceShortName()).isEqualTo("SG");
        assertThat(journeyPlanner.getStopPoints().get(166).getTimeAimedArrival()).as("last stop formatted as departure but is arrival").isEqualTo("11.52");

        /* com.itextpdf.text.Document ddoPdf =*/
        digitalDrivingOrderService.generatePDF(eRadn, journeyPlanner);
        digitalDrivingOrderService.generatePDF(radnDaten, journeyPlanner);
        //assertThat(ddoPdf).isNotNull();
    }

    @Deprecated
    private Document readXML(String filename) throws ParserConfigurationException, IOException, SAXException {
        final DocumentBuilder builder = DocumentBuilderFactory.newInstance()
            .newDocumentBuilder();
        final Document xmlDocument = builder.parse(new File(filename));
        xmlDocument.getDocumentElement().normalize();
        return xmlDocument;
    }
}
