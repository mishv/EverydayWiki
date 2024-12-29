package com.amazing.EverydayWiki.database;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.Setter;

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
    @Setter
    @Getter
    private String language;
    @Setter
    @Getter
    private String systemLanguage;

    public User() {}

    public User(Long chatID, String username, String systemLanguage) {
        this.chatID = chatID;
        this.username = username;
        this.systemLanguage = systemLanguage;
    }
}
