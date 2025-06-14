package ch.sbb.driveradvisorysystem.digitaldrivingorder.model;

import lombok.Builder;
import lombok.Value;

@Builder
@Value
public class Footnote {

    int index;
    String text;
}
