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
    int totalUnfollowed

    LocalDateTime likesProcessingBlockedUntil

    @Convert(converter = StringListConverter.class)
    List<String> whiteList

    protected PrimaryUser() {
        whiteList = new ArrayList<>()
    }

    PrimaryUser(String username) {
        this()
        this.username = username
    }

    PrimaryUser incrementNrOfLikes() {
        totalLiked++
        return this
    }

    PrimaryUser incrementNrOfUnfollowed() {
        totalUnfollowed++
        return this
    }

    PrimaryUser setLikesProcessingBlockedUntil(LocalDateTime likesLimitReachedAt) {
        this.likesProcessingBlockedUntil = likesLimitReachedAt
        return this
    }

    @Override
    String toString() {
        XmlMapper xmlMapper = new XmlMapper()
        return xmlMapper.writeValueAsString(this)
    }

    @Override
    boolean equals(Object o) {
        if (this.is(o)) return true
        if (this.getClass() != o.class) return false

        return ((PrimaryUser) o).username == username
    }

    @Override
    int hashCode() {
        return username.hashCode()
    }
}