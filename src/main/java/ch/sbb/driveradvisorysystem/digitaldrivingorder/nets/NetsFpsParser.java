package ch.sbb.driveradvisorysystem.digitaldrivingorder.nets;

import ch.sbb.driveradvisorysystem.digitaldrivingorder.nets.model.VehicleJourney;
import ch.sbb.driveradvisorysystem.digitaldrivingorder.nets.model.VehicleJourney.ScheduledStopPoint;
import com.opencsv.CSVReader;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import lombok.experimental.UtilityClass;
import org.apache.commons.lang3.StringUtils;

@UtilityClass
public class NetsFpsParser {

    public static VehicleJourney toJourneyPlanner(String filename, LocalDate operatingDay) throws Exception {

        /*
         * NeTS Export für den Zug 711.
         * Da für diesen Zug an verschiedenen Daten unterschiedliche Fahrordnungen existieren ist es natürlich etwas unübersichtlich für den Endbenutzer.
         */
        final Path path = Paths.get(ClassLoader.getSystemResource(filename).toURI());

        final List<String[]> journeyPlannerCsv = readAllLines(path);

        return mapToJourneyPlanner(journeyPlannerCsv, operatingDay);
    }

    private List<String[]> readAllLines(Path filePath) throws Exception {
        try (Reader reader = Files.newBufferedReader(filePath)) {
            try (CSVReader csvReader = new CSVReader(reader)) {
                return csvReader.readAll();
            }
        }
    }

    /**
     * @param journeyPlanner the list contains multiple schedules according to operatingDay
     * @param operatingDay
     * @return extract of one Train on a specific operatingDay
     */
    private VehicleJourney mapToJourneyPlanner(List<String[]> journeyPlanner, LocalDate operatingDay) {

        //TODO find cells => hardcoded line 6..182 for operatingDay
        final List<ScheduledStopPoint> stopPoints = new ArrayList<>();
        final int lastLineIndex = 183;
        for (int i = 6; i < lastLineIndex; i++) {
            final String line = extractLine(journeyPlanner, i);
            final String stopPlaceNameShort = extractStopPlaceShortName(line);
            if (!stopPlaceNameShort.isEmpty()) {
                // might parse 1 or 2 lines
                stopPoints.add(mapToScheduledStopPoint(journeyPlanner, i, lastLineIndex, stopPlaceNameShort, extractValue(line)));
            }
        }

        return VehicleJourney.builder()
            .operatingDay(operatingDay)
            .trainNumber(extractValue(extractLine(journeyPlanner, 0)))
            .stopPoints(stopPoints)
            .build();
    }

    /**
     * arrival: format like "=T(""08(00)"")" or "=T(""(37)"")"
     */
    private ScheduledStopPoint mapToScheduledStopPoint(List<String[]> journeyPlanner, int lineIndex, int lastLineIndex, String stopPlaceNameShort, String timeAimed) {
        String arrival = StringUtils.EMPTY;
        String departure = StringUtils.EMPTY;

        if (timeAimed.contains("(" /*passthrough*/)) {
            arrival = timeAimed;
        } else {
            if (timeAimed.contains("style")) {
                // real arrival timeAimed
                arrival = timeAimed.replace("<style isUnderline=\"true\">", StringUtils.EMPTY);
                arrival = arrival.replace("</style>", StringUtils.EMPTY);

                if (journeyPlanner.size() > lineIndex + 1) {
                    // next line contains departure for same stop-place
                    departure = extractTimeAimed(journeyPlanner, lineIndex + 1);
                } // EOF journey
            } else {
                // real departure timeAimed
                if (lineIndex == lastLineIndex - 1) {
                    // last stop-point becomes arrival always
                    arrival = timeAimed;
                } else {
                    departure = timeAimed;
                }
            }
        }

        return ScheduledStopPoint.builder()
            .placeShortName(stopPlaceNameShort)
            .timeAimedDeparture(departure)
            .timeAimedArrival(arrival)
            .build();
    }

    private String extractLine(List<String[]> journeyPlanner, int lineIndex) {
        return Arrays.stream(journeyPlanner.get(lineIndex)).toArray()[0].toString();
    }

    private String extractStopPlaceShortName(String line) {
        final int separator = line.indexOf(";");
        return line.substring(0, separator);
    }

    private String extractTimeAimed(List<String[]> journeyPlanner, int lineIndex) {
        final String stopPlaceShortName = extractLine(journeyPlanner, lineIndex);
        return extractValue(stopPlaceShortName);
    }

    /**
     * @param netsLine format like "=T(""711"")" or "=T(""07.25"")";
     * @return 711 or 07:25
     */
    private String extractValue(String netsLine) {
        int start = netsLine.indexOf("(\"");
        int end = netsLine.indexOf("\")", start);
        return netsLine.substring(start + 2, end);
    }
}
