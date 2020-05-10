package com.instabot.data.model.primaryuser

import com.fasterxml.jackson.dataformat.xml.XmlMapper
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

    int totalLiked
    int totalFollowed
    int totalUnfollowed

    @Convert(converter = StringListConverter.class)
    List<String> whiteList

    protected PrimaryUser() {
        whiteList = new ArrayList<>()
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