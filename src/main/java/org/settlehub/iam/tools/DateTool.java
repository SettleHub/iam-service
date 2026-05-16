package org.settlehub.iam.tools;

import org.springframework.stereotype.Component;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

@Component("dateTool")
public class DateTool {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX");

    public static String getCurrentDate() {
        return OffsetDateTime.now(ZoneId.of("Europe/Kiev")).format(FORMATTER);
    }
}
