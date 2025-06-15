package ch.sbb.driveradvisorysystem.digitaldrivingorder.model;

import ch.sbb.bahninfrastruktur.eradn.Blocksignal;
import ch.sbb.bahninfrastruktur.eradn.Kurve;
import ch.sbb.bahninfrastruktur.eradn.Schutzstrecke;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;

@Data
public class DigitalDrivingOrderEntry {

    String teilstreckenBpId;
    boolean ausgeblendet = false;
    boolean klammer = false;

    // PDF relevant eRADN Strecke
    Double km;
    Integer gefaelle;
    Integer steigung;
    String funkkanal;
    String abfahrtsErlaubnis;
    String etcs;
    String shortName;
    String name;
    String streckenR150;
    String bahnhofR150;
    List<Blocksignal> blocks = new ArrayList<>();
    List<Kurve> curves = new ArrayList<>();
    List<Schutzstrecke> schutzstrecken = new ArrayList<>();
    List<Footnote> footnotes = new ArrayList<>();

    // PDF relevant fahrplan
    String an;
    String ab;
}
