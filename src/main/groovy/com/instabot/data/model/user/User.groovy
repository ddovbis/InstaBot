package com.instabot.data.model.user

import com.fasterxml.jackson.dataformat.xml.XmlMapper

import javax.persistence.Entity
import javax.persistence.Id
import java.time.LocalDate
import java.time.LocalDateTime

// TODO If the name has changed -> change it on user
// TODO When creating, check if in white list
@Entity
class User {
    @Id
    private String id
    private String masterUsername
    private String username
    private String name
    private boolean isInWhiteList

    private boolean isFollower
    private LocalDateTime isFollowerLastUpdatedAt
    private LocalDateTime unfollowedAt
    private boolean isFollowed
    private LocalDateTime isFollowedLastUpdatedAt
    private LocalDateTime becameUnfollowedAt

    private int nrOfLikes
    private int targetNrOfLikes
    private int nrOfComments
    private int targetNrOfComments

    private LocalDateTime processedAt
    private UserStatus userStatus

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

    String getMasterUsername() {
        return masterUsername
    }

    String getUsername() {
        return username
    }

    String getId() {
        return id
    }

    String getName() {
        return name
    }

    void setName(String name) {
        this.name = name
    }

    boolean getIsInWhiteList() {
        return isInWhiteList
    }

    void setIsInWhiteList(boolean isInWhiteList) {
        this.isInWhiteList = isInWhiteList
    }

    boolean getIsFollower() {
        return isFollower
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

    LocalDateTime getIsFollowerLastUpdatedAt() {
        return isFollowerLastUpdatedAt
    }

    LocalDateTime getUnfollowedAt() {
        return unfollowedAt
    }

    boolean getIsFollowed() {
        return isFollowed
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

    LocalDateTime getIsFollowedLastUpdatedAt() {
        return isFollowedLastUpdatedAt
    }

    LocalDateTime getBecameUnfollowedAt() {
        return becameUnfollowedAt
    }

    int getNrOfLikes() {
        return nrOfLikes
    }

    void setNrOfLikes(int nrOfLikes) {
        this.nrOfLikes = nrOfLikes
    }

    int getTargetNrOfLikes() {
        return targetNrOfLikes
    }

    void setTargetNrOfLikes(int targetNrOfLikes) {
        this.targetNrOfLikes = targetNrOfLikes
    }

    int getNrOfComments() {
        return nrOfComments
    }

    void setNrOfComments(int nrOfComments) {
        this.nrOfComments = nrOfComments
    }

    int getTargetNrOfComments() {
        return targetNrOfComments
    }

    void setTargetNrOfComments(int targetNrOfComments) {
        this.targetNrOfComments = targetNrOfComments
    }

    LocalDateTime getProcessedAt() {
        return processedAt
    }

    void setProcessedAt(LocalDateTime processedAt) {
        this.processedAt = processedAt
    }

    UserStatus getUserStatus() {
        return userStatus
    }

    void setUserStatus(UserStatus userStatus) {
        this.userStatus = userStatus
    }

    @Override
    String toString() {
        XmlMapper xmlMapper = new XmlMapper();
        return xmlMapper.writeValueAsString(this)
    }

    @Override
    boolean equals(Object o) {
        if (o == this) {
            return true
        } else if (!(o instanceof User)) {
            return false;
        } else {
            return ((User) o).id == id
        }
    }
}