package com.amazing.EverydayWiki.database;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Getter;

@Entity
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Getter
    private Long chatID;
    @Getter
    private String username;
    private String timezone;

    public User() {}

    public User(Long chatID, String username) {
        this.chatID = chatID;
        this.username = username;
    }

    public void setChatId(Long chatID) {
        this.chatID = chatID;
    }

}
