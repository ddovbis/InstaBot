package com.instabot.data.model.user

import com.fasterxml.jackson.dataformat.xml.XmlMapper
import com.instabot.utils.converters.StringTreeSetConverter

import javax.persistence.Convert
import javax.persistence.Entity
import javax.persistence.Id
import java.time.LocalDateTime

@Entity
class User {
    @Id
    String id
    String masterUsername
    String username
    String name
    boolean isInWhiteList

    boolean isFollower
    LocalDateTime isFollowerLastUpdatedAt
    LocalDateTime unfollowedAt
    boolean isFollowed
    LocalDateTime isFollowedLastUpdatedAt
    LocalDateTime becameUnfollowedAt

    int nrOfLikes
    int targetedNrOfLikes
    LocalDateTime processedLikesAt

    @Convert(converter = StringTreeSetConverter.class)
    TreeSet<UserLabel> labels

    protected User() {
    }

    User(String masterUsername, String username, String name) {
        this.masterUsername = masterUsername
        this.username = username
        this.id = masterUsername + "_" + username
        this.name = name
        this.labels = new TreeSet<UserLabel>()
    }


    void setIsFollower(boolean isFollower) {
        if (!isFollower && this.isFollower) {
            this.unfollowedAt = LocalDateTime.now()
        } else if (isFollower && !this.isFollower) {
            this.unfollowedAt = null
        }

        this.isFollower = isFollower
        this.isFollowerLastUpdatedAt = LocalDateTime.now()
    }

    void setIsFollowed(boolean isFollowed) {
        if (!isFollowed && this.isFollowed) {
            this.becameUnfollowedAt = LocalDateTime.now()
        } else if (isFollowed) {
            removeLabel(UserLabel.TO_FOLLOW)

            if (!this.isFollowed) {
                this.becameUnfollowedAt = null
            }
        }

        this.isFollowed = isFollowed
        this.isFollowedLastUpdatedAt = LocalDateTime.now()
    }

    void incrementNrOfLikes() {
        nrOfLikes++
        if (nrOfLikes == targetedNrOfLikes) {
            addLabel(UserLabel.FULLY_LIKED)
        }
    }

    void setTargetedNrOfLikes(int targetedNrOfLikes) {
        this.targetedNrOfLikes = targetedNrOfLikes
        if (nrOfLikes >= targetedNrOfLikes) {
            addLabel(UserLabel.FULLY_LIKED)
        }
    }

    void addLabel(UserLabel label) {
        if (label == UserLabel.FULLY_LIKED) {
            processedLikesAt = LocalDateTime.now()
        }

        labels.add(label)
    }

    void removeLabel(UserLabel label) {
        if (label == UserLabel.FULLY_LIKED) {
            processedLikesAt = null
        }

        labels.remove(label)
    }

    boolean isFullyLiked() {
        return labels.contains(UserLabel.FULLY_LIKED)
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

        return ((User) o).id == id
    }

    int hashCode() {
        int result
        result = id.hashCode()
        result = 31* result + masterUsername.hashCode()
        result = 31 * result + username.hashCode()
        result = 31 * result + name.hashCode()
        return result
    }


}