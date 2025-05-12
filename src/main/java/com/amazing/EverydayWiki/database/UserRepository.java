package com.amazing.EverydayWiki.database;
import jakarta.persistence.Column;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {
    User findByChatID(Long chatID);
}
