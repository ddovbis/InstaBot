package com.instabot.data.repositories.interaction.like


import com.instabot.data.model.interaction.like.LikeInteraction
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

import java.time.LocalDateTime

@Repository
interface LikeInteractionRepository extends JpaRepository<LikeInteraction, String> {
    List<LikeInteraction> findByPrimaryUsername(String primaryUsername);

    @Query("SELECT COUNT(*) FROM LikeInteraction il WHERE il.primaryUsername = ?1 and il.timestamp > ?2 and il.timestamp < ?3")
    int countByPrimaryUsernameBetween(String primaryUsername, LocalDateTime start, LocalDateTime end);
}