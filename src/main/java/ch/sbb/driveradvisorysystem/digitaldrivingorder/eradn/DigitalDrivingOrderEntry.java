package ch.sbb.driveradvisorysystem.digitaldrivingorder.eradn;

import lombok.Data;

@Data
public class DigitalDrivingOrderEntry {

    String teilstreckenBpId;
    boolean ausgeblendet = false;
    boolean klammer = false;

    // PDF relevant eRADN Strecke
    String fussnote;
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

    // PDF relevant fahrplan
    String an;
    String ab;
}
