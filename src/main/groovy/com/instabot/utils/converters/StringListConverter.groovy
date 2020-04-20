package com.instabot.utils.converters

import javax.persistence.AttributeConverter
import javax.persistence.Converter

@Converter
class StringListConverter implements AttributeConverter<List<String>, String> {
    private static final String SPLIT_CHAR = ";"

    @Override
    String convertToDatabaseColumn(List<String> stringList) {
        return String.join(SPLIT_CHAR, stringList)
    }

    @Override
    List<String> convertToEntityAttribute(String string) {
        return Arrays.asList(string.split(SPLIT_CHAR))
    }
}