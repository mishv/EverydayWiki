package com.amazing.EverydayWiki.database;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class UserService {
    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User saveUser(Long chatID, String username, String systemLanguage) {
        Optional<User> existingUser = Optional.ofNullable(userRepository.findByChatID(chatID));
        if (existingUser.isEmpty()) {
            User newUser = new User(chatID, username, systemLanguage);
            return userRepository.save(newUser);
        }
        return existingUser.get();
    }

    public Optional<User> getUserByChatId(Long chatID) {
        return Optional.ofNullable(userRepository.findByChatID(chatID));
    }

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public void setLanguage(Long chatID, String language) {
        User user = userRepository.findByChatID(chatID);
        if (user != null) {
            user.setLanguage(language); // Обновляем поле language
            userRepository.save(user); // Сохраняем изменения в базе данных
        } else {
            throw new IllegalArgumentException("User with chatID " + chatID + " not found.");
        }
    }

    public String getLanguage(Long chatID) {
        User user = userRepository.findByChatID(chatID);
        if (user != null) {
            return user.getLanguage();
        } else {
            throw new IllegalArgumentException("User with chatID " + chatID + " not found.");
        }
    }

    public void setSystemLanguage(Long chatID, String language) {
        User user = userRepository.findByChatID(chatID);
        if (user != null) {
            user.setSystemLanguage(language); // Обновляем поле systemlanguage
            userRepository.save(user); // Сохраняем изменения в базе данных
        } else {
            throw new IllegalArgumentException("User with chatID " + chatID + " not found.");
        }
    }

    public String getSystemLanguage(Long chatID) {
        User user = userRepository.findByChatID(chatID);
        if (user != null) {
            return user.getSystemLanguage();
        } else {
            throw new IllegalArgumentException("User with chatID " + chatID + " not found.");
        }
    }
}
