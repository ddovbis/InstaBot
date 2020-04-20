package com.instabot.data.repositories.primaryuser

import com.instabot.data.model.primaryuser.PrimaryUser
import com.instabot.data.model.user.User
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface PrimaryUserRepository extends JpaRepository<PrimaryUser, String> {
    List<User> findByUsername(String username);
}