package ch.sbb.driveradvisorysystem.digitaldrivingorder.eradn;

import ch.sbb.bahninfrastruktur.eradn.Fussnote;
import ch.sbb.bahninfrastruktur.eradn.Fussnote.Referenzen.Referenz;
import ch.sbb.bahninfrastruktur.eradn.RadnDaten;
import ch.sbb.bahninfrastruktur.eradn.Strecke;
import ch.sbb.bahninfrastruktur.eradn.Teilstrecke;
import ch.sbb.bahninfrastruktur.eradn.V;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import lombok.experimental.UtilityClass;

@UtilityClass
public class RadnParser {

    public static RadnDaten unmarshal(String filename) throws JAXBException, IOException {
        final JAXBContext context = JAXBContext.newInstance(RadnDaten.class);
        return (RadnDaten) context.createUnmarshaller()
            .unmarshal(new FileReader(filename));
    }

    public static Strecke findStrecke(RadnDaten radnDaten, String streckenId) {
        return radnDaten.getStrecken().getStrecke().stream()
            .filter(strecke -> streckenId.equals(strecke.getStreckenId()))
            .findFirst()
            .orElse(null);
    }

    public static Teilstrecke findTeilstrecke(Strecke strecke, int teilstreckenNr) {
        return strecke.getTeilstrecken().getTeilstrecke().stream()
            .filter(teilstrecke -> teilstreckenNr == teilstrecke.getTeilstreckenNr())
            .findFirst()
            .orElse(null);
    }

    /**
     * Same {@link Strecke} must contain both!
     *
     * @param teilstreckeId
     * @param fussnoten
     * @return subset of Fussnoten for the given Teilstrecke
     */
    public static List<Fussnote> findFussnoten(String teilstreckeId, String teilstreckenBpId, List<Fussnote> fussnoten) {
        // TODO refactor ugly code
        final List<Fussnote> subset = new ArrayList<>();
        for (Fussnote fussnote : fussnoten) {
            for (Referenz referenz : fussnote.getReferenzen().getReferenz()) {
                if (referenz.getTeilstreckenRef().equals(teilstreckeId) && referenz.getRefId().equals(teilstreckenBpId)) {
                    subset.add(fussnote);
                }
            }
        }
        return subset;
    }

    public static String getR150(List<V> vitesse) {
        return vitesse.stream()
            .filter(v -> "R".equals(v.getZugreihe()) && 150 == v.getBremsverhaeltnis())
            .map(V::getGeschwindigkeit)
            .findFirst()
            .orElse("");
    }
}
