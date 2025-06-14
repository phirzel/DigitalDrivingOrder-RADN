package ch.sbb.driveradvisorysystem.digitaldrivingorder.nets.model;

import java.time.LocalDate;
import java.util.List;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

/**
 * NeTS alike structure represented in a Transmodel manner.
 */
@Builder
@Value
public class VehicleJourney {

    @NonNull
    LocalDate operatingDay;

    /**
     * Transmodel:: ServiceProduct::number
     */
    @NonNull
    String trainNumber;

    @NonNull
    List<ScheduledStopPoint> stopPoints;

    /**
     * Transmodel like structure.
     */
    @Builder
    @Value
    public static class ScheduledStopPoint {

        /**
         * BP Uic
         */
        String placeShortName;

        /**
         * StopCall::timeAimed, ::timeRt
         */
        String timeAimedDeparture;

        String timeAimedArrival;
    }
}
