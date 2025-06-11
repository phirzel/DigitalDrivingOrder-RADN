package ch.sbb.driveradvisorysystem.digitaldrivingorder.eradn;

import lombok.Data;

@Data
public class DigitalDrivingOrderEntry {

    String teilstreckenBpId;
    boolean ausgeblendet = false;
    boolean klammer = false;

    // PDF relevant eRADN Strecke
    String fussnote;
    String km;
    String gefaelle;
    String steigung;
    String funkkanal;
    String abfahrtsErlaubnis;
    String etcs;
    String shortName;
    String name;
    String r150;

    // PDF relevant fahrplan
    String an;
    String ab;
}
