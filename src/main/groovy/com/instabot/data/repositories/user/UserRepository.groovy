package com.instabot.data.repositories.user

import com.instabot.data.model.user.User
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface UserRepository extends JpaRepository<User, String> {
    List<User> findByUsername(String username);

    List<User> findByMasterUsername(String username);
}