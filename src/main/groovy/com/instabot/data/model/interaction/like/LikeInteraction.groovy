package com.instabot.data.model.interaction.like

import com.fasterxml.jackson.dataformat.xml.XmlMapper

import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import java.time.LocalDateTime

@Entity
class LikeInteraction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    String id

    String primaryUsername
    String targetUsername
    LocalDateTime timestamp

    protected LikeInteraction() {
    }

    LikeInteraction(String primaryUsername, String targetUsername) {
        this.primaryUsername = primaryUsername
        this.targetUsername = targetUsername
        timestamp = LocalDateTime.now()
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

        return this.id == ((LikeInteraction) o).id
    }

    @Override
    int hashCode() {
        int result
        result = id.hashCode()
        result = 31 * result + primaryUsername.hashCode()
        result = 31 * result + targetUsername.hashCode()
        result = 31 * result + timestamp.hashCode()
        return result
    }
}
