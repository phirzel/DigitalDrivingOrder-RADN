package ch.sbb.driveradvisorysystem.digitaldrivingorder.journeyplanner;

import java.util.Date;
import java.util.List;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

/**
 * NeTS alike structure
 */
@Builder
@Value
public class JourneyPlanner {

    @NonNull
    Date operatingDay;

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
