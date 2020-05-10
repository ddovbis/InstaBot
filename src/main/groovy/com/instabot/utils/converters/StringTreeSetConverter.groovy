package com.instabot.utils.converters

import com.instabot.data.model.user.UserLabel

import javax.persistence.AttributeConverter
import javax.persistence.Converter

@Converter

class StringTreeSetConverter implements AttributeConverter<TreeSet<UserLabel>, String> {
    private static final String SPLIT_CHAR = ";"

    @Override
    String convertToDatabaseColumn(TreeSet<UserLabel> stringTreeSet) {
        String dataBaseColumn = ""
        stringTreeSet.each { userLabel -> dataBaseColumn += (userLabel.toString()) }
        return dataBaseColumn
    }

    @Override
    TreeSet<UserLabel> convertToEntityAttribute(String string) {
        TreeSet<UserLabel> entityAttribute = new TreeSet<>()
        string.split(SPLIT_CHAR).each { userLabelStr ->
            if (!userLabelStr.empty) {
                entityAttribute.add(UserLabel.valueOf(userLabelStr))
            }
        }
        return entityAttribute
    }
}
