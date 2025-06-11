package ch.sbb.driveradvisorysystem.digitaldrivingorder.nets;

import ch.sbb.driveradvisorysystem.digitaldrivingorder.journeyplanner.JourneyPlanner;
import ch.sbb.driveradvisorysystem.digitaldrivingorder.journeyplanner.JourneyPlanner.ScheduledStopPoint;
import com.opencsv.CSVReader;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Date;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import lombok.experimental.UtilityClass;

@UtilityClass
public class NetsFpsParser {

    public static JourneyPlanner toJourneyPlanner(String filename, Date operatingDay) throws Exception {

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
    private JourneyPlanner mapToJourneyPlanner(List<String[]> journeyPlanner, Date operatingDay) {

        //TODO find cells => hardcoded line 6..182 for operatingDay
        final List<ScheduledStopPoint> stopPoints = new ArrayList<>();
        final int lastLineIndex = 183;
        for (int i = 6; i < lastLineIndex; i++) {
            final String line = extractLine(journeyPlanner, i);
            final String stopPlaceNameShort = extractStopPlaceShortName(line);
            if (!"".equals(stopPlaceNameShort)) {
                // might parse 1 or 2 lines
                stopPoints.add(mapToScheduledStopPoint(journeyPlanner, i, lastLineIndex, stopPlaceNameShort, extractValue(line)));
            }
        }

        return JourneyPlanner.builder()
            .operatingDay(operatingDay)
            .trainNumber(extractValue(extractLine(journeyPlanner, 0)))
            .stopPoints(stopPoints)
            .build();
    }

    /**
     * arrival: format like "=T(""08(00)"")" or "=T(""(37)"")"
     */
    private ScheduledStopPoint mapToScheduledStopPoint(List<String[]> journeyPlanner, int lineIndex, int lastLineIndex, String stopPlaceNameShort, String timeAimed) {
        String arrival = "";
        String departure = "";

        if (timeAimed.contains("(" /*passthrough*/)) {
            arrival = timeAimed;
        } else {
            if (timeAimed.contains("style")) {
                // real arrival timeAimed
                arrival = timeAimed.replace("<style isUnderline=\"true\">", "");
                arrival = arrival.replace("</style>", "");

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
