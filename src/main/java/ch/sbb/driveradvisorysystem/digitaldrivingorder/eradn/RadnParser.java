package ch.sbb.driveradvisorysystem.digitaldrivingorder.eradn;

import ch.sbb.bahninfrastruktur.eradn.RadnDaten;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import java.io.FileReader;
import java.io.IOException;
import lombok.experimental.UtilityClass;

@UtilityClass
public class RadnParser {

    public static RadnDaten unmarshal(String filename) throws JAXBException, IOException {
        final JAXBContext context = JAXBContext.newInstance(RadnDaten.class);
        return (RadnDaten) context.createUnmarshaller()
            .unmarshal(new FileReader(filename));
    }
}
