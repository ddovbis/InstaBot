package com.instabot.data.repository.user

import com.instabot.data.model.user.User
import com.instabot.tmptest.Customer
import com.sun.xml.bind.v2.model.core.ID
import org.springframework.context.annotation.Bean
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository

//@Repository
interface UserRepository extends CrudRepository<User, String> {
    List<User> findByUsername(String username);

    Optional<User> findById(String id);
}