package com.instabot.utils.time

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class TimeUtils {
    public static final DateTimeFormatter LEGIBLE_DATE_TIME_FORMATTER_S = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
    public static final DateTimeFormatter LEGIBLE_DATE_TIME_FORMATTER_MS = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS")

    /**
     * @param localDateTime - {@link LocalDateTime} to be formatted into a legible {@link String}
     * @return - if {@param localDateTime} in a String format, stripping milliseconds if there are not any
     */
    static String getLegibleDateTime(LocalDateTime localDateTime) {
        return localDateTime.nano == 0 ? localDateTime.format(LEGIBLE_DATE_TIME_FORMATTER_S) : localDateTime.format(LEGIBLE_DATE_TIME_FORMATTER_MS)
    }
}
