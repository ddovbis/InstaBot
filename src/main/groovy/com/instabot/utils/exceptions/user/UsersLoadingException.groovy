package com.instabot.utils.exceptions.user

class UsersLoadingException extends Exception {
    UsersLoadingException(String errorMessage) {
        super(errorMessage)
    }
}
