package com.instabot.data.model.user

/**
 * NEW - a user that has not reached the target nr. of likes and comments
 * PROCESSED - user that has reached the target nr. of likes and comments
 * FINISHED - user that has been processed and unfolled
 */
enum UserStatus {
    NEW, PROCESSED, FINISHED;
}