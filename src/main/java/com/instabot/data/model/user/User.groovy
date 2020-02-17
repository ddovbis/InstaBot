package com.instabot.data.model.user

import java.time.LocalDateTime

// TODO If the name has changed -> change it on user
// TODO When creating, check if in white list

class User {
    String masterUsername
    String username
    String id
    String name
    boolean isInWhiteList

    boolean isFollower
    LocalDateTime isFollowerLastUpdatedAt
    LocalDateTime unfollowedAt
    boolean isFollowed
    LocalDateTime isFollowedLastUpdatedAt
    LocalDateTime gotUnfollowedAt

    int nrOfLikes
    int targetNrOfLikes
    int nrOfComments
    int targetNrOfComments

    LocalDateTime processedAt
    UserStatus userStatus

    User(String masterUsername, String username, String name) {
        this.masterUsername = masterUsername
        this.username = username
        this.id = masterUsername + "_" + username
        this.name = name
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
        this.isFollower = isFollower
    }

    LocalDateTime getIsFollowerLastUpdatedAt() {
        return isFollowerLastUpdatedAt
    }

    void setIsFollowerLastUpdatedAt(LocalDateTime isFollowerLastUpdatedAt) {
        this.isFollowerLastUpdatedAt = isFollowerLastUpdatedAt
    }

    LocalDateTime getUnfollowedAt() {
        return unfollowedAt
    }

    void setUnfollowedAt(LocalDateTime unfollowedAt) {
        this.unfollowedAt = unfollowedAt
    }

    boolean getIsFollowed() {
        return isFollowed
    }

    void setIsFollowed(boolean isFollowed) {
        this.isFollowed = isFollowed
    }

    LocalDateTime getIsFollowedLastUpdatedAt() {
        return isFollowedLastUpdatedAt
    }

    void setIsFollowedLastUpdatedAt(LocalDateTime isFollowedLastUpdatedAt) {
        this.isFollowedLastUpdatedAt = isFollowedLastUpdatedAt
    }

    LocalDateTime getGotUnfollowedAt() {
        return gotUnfollowedAt
    }

    void setGotUnfollowedAt(LocalDateTime gotUnfollowedAt) {
        this.gotUnfollowedAt = gotUnfollowedAt
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