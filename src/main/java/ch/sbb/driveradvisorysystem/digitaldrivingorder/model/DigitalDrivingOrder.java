package ch.sbb.driveradvisorysystem.digitaldrivingorder.model;

import java.time.LocalDate;
import java.util.List;
import lombok.Builder;
import lombok.Value;

@Builder
@Value
public class DigitalDrivingOrder {

    // VehicleJourney data
    String trainNumber;
    LocalDate operatingDay;

    // RADN data
    List<String> isbs;
    List<DigitalDrivingOrderEntry> entries;
}
