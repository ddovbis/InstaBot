package com.instabot.data.model.user

import com.fasterxml.jackson.dataformat.xml.XmlMapper

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
    int targetNrOfLikes
    int nrOfComments
    int targetNrOfComments

    LocalDateTime processedAt
    UserStatus userStatus

    protected User() {

    }

    User(String masterUsername, String username, String name) {
        this.masterUsername = masterUsername
        this.username = username
        this.id = masterUsername + "_" + username
        this.name = name

        this.isFollower = false
        this.isFollowed = false
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
        } else {
            if (isFollowed && !this.isFollowed) {
                this.becameUnfollowedAt = null
            }
        }
        this.isFollowed = isFollowed
        this.isFollowedLastUpdatedAt = LocalDateTime.now()
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
        } else if (!(o instanceof User)) {
            return false
        } else {
            return ((User) o).id == id
        }
    }
}