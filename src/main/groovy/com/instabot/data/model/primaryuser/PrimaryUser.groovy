package com.instabot.data.model.primaryuser

import com.fasterxml.jackson.dataformat.xml.XmlMapper
import com.instabot.operations.OperationType
import com.instabot.utils.converters.OperationTypeToLocalDateTimeMapConverter
import com.instabot.utils.converters.StringListConverter

import javax.persistence.Convert
import javax.persistence.Entity
import javax.persistence.Id
import java.time.LocalDateTime

@Entity
class PrimaryUser {
    @Id
    String username

    int followers
    int following
    LocalDateTime relatedUsersUpdatedAt

    int totalFollowed
    int totalUnfollowed

    @Convert(converter = StringListConverter.class)
    List<String> whiteList

    @Convert(converter = OperationTypeToLocalDateTimeMapConverter.class)
    HashMap<OperationType, LocalDateTime> operationLockedUntilMap

    protected PrimaryUser() {
        whiteList = new ArrayList<>()
        operationLockedUntilMap = new HashMap<>()
    }

    PrimaryUser(String username) {
        this()
        this.username = username
    }

    @Override
    String toString() {
        XmlMapper xmlMapper = new XmlMapper()
        return xmlMapper.writeValueAsString(this)
    }

    @Override
    boolean equals(Object o) {
        if (o == this) {
            return true
        } else if (!(o instanceof PrimaryUser)) {
            return false
        } else {
            return ((PrimaryUser) o).username == username
        }
    }
}