package com.instabot.utils.converters

import com.instabot.operations.OperationType

import javax.persistence.AttributeConverter
import javax.persistence.Converter
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Converter
class OperationTypeToLocalDateTimeMapConverter implements AttributeConverter<HashMap<OperationType, LocalDateTime>, String> {
    private final String SPLIT_CHAR = ';'
    private final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

    @Override
    String convertToDatabaseColumn(HashMap<OperationType, LocalDateTime> operationTypeToLocalDateTimeMap) {
        if (operationTypeToLocalDateTimeMap == null) {
            return null
        }

        String convertedString = ""
        operationTypeToLocalDateTimeMap.each { operationType, localDateTime ->
            convertedString += operationType.toString() + '=' + localDateTime.format(DATE_TIME_FORMATTER) + SPLIT_CHAR
        }

        return convertedString
    }

    @Override
    HashMap<OperationType, LocalDateTime> convertToEntityAttribute(String string) {
        if (string == null) {
            return null
        } else if (string == "") {
            return new HashMap<OperationType, LocalDateTime>()
        }

        HashMap<OperationType, LocalDateTime> convertedOperationTypeToLocalDateTimeMap = new HashMap<>()
        String[] mapEntries = string.split(SPLIT_CHAR)
        for (String mapEntry : mapEntries) {
            String[] mapEntryComponents = mapEntry.split('=')
            OperationType operationType = OperationType.valueOf(mapEntryComponents[0])
            LocalDateTime localDateTime = LocalDateTime.parse(mapEntryComponents[1], DATE_TIME_FORMATTER)
            convertedOperationTypeToLocalDateTimeMap.put(operationType, localDateTime)
        }

        return convertedOperationTypeToLocalDateTimeMap
    }
}